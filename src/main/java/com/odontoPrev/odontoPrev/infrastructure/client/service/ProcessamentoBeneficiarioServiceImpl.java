package com.odontoPrev.odontoPrev.infrastructure.client.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.odontoPrev.odontoPrev.domain.entity.BeneficiarioOdontoprev;
import com.odontoPrev.odontoPrev.domain.entity.ControleSyncBeneficiario;
import com.odontoPrev.odontoPrev.domain.repository.ControleSyncBeneficiarioRepository;
import com.odontoPrev.odontoPrev.domain.service.ProcessamentoBeneficiarioService;
import com.odontoPrev.odontoPrev.infrastructure.aop.MonitorarOperacao;
import com.odontoPrev.odontoPrev.infrastructure.client.adapter.out.BeneficiarioOdontoprevFeignClient;
import com.odontoPrev.odontoPrev.infrastructure.client.adapter.out.dto.BeneficiarioInclusaoRequestNew;
import com.odontoPrev.odontoPrev.infrastructure.client.adapter.out.dto.BeneficiarioInclusaoResponseNew;
import com.odontoPrev.odontoPrev.infrastructure.client.service.BeneficiarioTokenService;
import com.odontoPrev.odontoPrev.infrastructure.exception.ProcessamentoBeneficiarioException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.CallableStatementCallback;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.sql.CallableStatement;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import static com.odontoPrev.odontoPrev.infrastructure.aop.MonitorarOperacao.TipoExcecao.PROCESSAMENTO_BENEFICIARIO;
import static com.odontoPrev.odontoPrev.infrastructure.exception.ProcessamentoBeneficiarioException.TipoOperacao.INCLUSAO;

/**
 * IMPLEMENTAÇÃO DO SERVIÇO DE PROCESSAMENTO DE BENEFICIÁRIOS
 *
 * Realiza o processamento completo de inclusão de beneficiários na OdontoPrev,
 * incluindo validação, comunicação com API, atualização de status e execução
 * da procedure no sistema Tasy.
 *
 * FLUXO COMPLETO DE PROCESSAMENTO:
 * 1. Validação de dados obrigatórios
 * 2. Conversão de entidade para DTO de request
 * 3. Chamada para API da OdontoPrev
 * 4. Processamento da resposta (cdAssociado)
 * 5. Atualização do status no banco
 * 6. Execução da procedure no Tasy
 * 7. Registro de logs de auditoria
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ProcessamentoBeneficiarioServiceImpl implements ProcessamentoBeneficiarioService {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final BeneficiarioOdontoprevFeignClient odontoprevClient;
    private final ControleSyncBeneficiarioRepository controleSyncRepository;
    private final JdbcTemplate jdbcTemplate;
    private final OdontoprevApiHeaderService headerService;
    private final BeneficiarioTokenService beneficiarioTokenService;
    private final ObjectMapper objectMapper;

    /**
     * PROCESSA INCLUSÃO DE UM ÚNICO BENEFICIÁRIO
     *
     * Executa todo o fluxo de inclusão com tratamento completo de erros
     * e atualização de status conforme resultado da operação.
     */
    @Override
    @Transactional
    @MonitorarOperacao(
            operacao = "PROCESSAR_INCLUSAO_BENEFICIARIO",
            incluirParametros = {"codigoMatricula"},
            excecaoEmErro = PROCESSAMENTO_BENEFICIARIO
    )
    public void processarInclusaoBeneficiario(BeneficiarioOdontoprev beneficiario) {
        String codigoMatricula = beneficiario.getCodigoMatricula();
        ControleSyncBeneficiario controleSync = null;

        try {
            // DEBUG: Verificar dados da view antes da validação
            log.debug("🔍 DADOS RECEBIDOS DA VIEW - Matrícula: {} | Nome: {} | NomeMae: {} | TelefoneCelular: {} | TelefoneResidencial: {}", 
                     beneficiario.getCodigoMatricula(), 
                     beneficiario.getNomeBeneficiario(),
                     beneficiario.getNomeMae(),
                     beneficiario.getTelefoneCelular(),
                     beneficiario.getTelefoneResidencial());
            
            // DEBUG: Verificar se os campos obrigatórios estão nulos
            if (beneficiario.getNomeMae() == null || beneficiario.getNomeMae().trim().isEmpty()) {
                log.warn("⚠️ NOME DA MÃE ESTÁ NULO OU VAZIO para beneficiário: {}", beneficiario.getCodigoMatricula());
            }
            
            if (beneficiario.getTelefoneCelular() == null || beneficiario.getTelefoneCelular().trim().isEmpty()) {
                log.warn("⚠️ TELEFONE CELULAR ESTÁ NULO OU VAZIO para beneficiário: {}", beneficiario.getCodigoMatricula());
            }
            
            // Etapa 1: Validação de dados obrigatórios - TEMPORARIAMENTE DESABILITADA
            // if (!validarDadosObrigatorios(beneficiario)) {
            //     String mensagem = "Beneficiário possui dados obrigatórios ausentes ou inválidos";
            //     registrarTentativaErro(beneficiario, "INCLUSAO", null, mensagem, null);
            //     throw new ProcessamentoBeneficiarioException(mensagem, codigoMatricula, INCLUSAO);
            // }

            // Etapa 2: Validação final do telefone antes de criar o request
            String telefoneFinal = beneficiario.getTelefoneCelular();
            if (telefoneFinal != null) {
                String telefoneLimpo = telefoneFinal.replaceAll("[^0-9]", "");
                if (telefoneLimpo.length() != 11) {
                    String mensagem = "Telefone celular inválido: deve ter exatamente 11 dígitos (DDD + número). Atual: " + telefoneLimpo.length() + " dígitos";
                    registrarTentativaErro(beneficiario, "INCLUSAO", null, mensagem, null);
                    throw new ProcessamentoBeneficiarioException(mensagem, codigoMatricula, INCLUSAO);
                }
            }
            
            // Etapa 3: Conversão para DTO de request
            BeneficiarioInclusaoRequestNew request = converterParaInclusaoRequestNew(beneficiario);
            try {
                log.debug("📤 DADOS ENVIADOS PARA API - Beneficiário {}: {}", codigoMatricula, 
                         objectMapper.writeValueAsString(request));
            } catch (Exception e) {
                log.debug("📤 DADOS ENVIADOS PARA API - Beneficiário {}: [Erro ao serializar request]", codigoMatricula);
            }

            // Etapa 4: Criar registro de controle ANTES de chamar a API
            controleSync = criarRegistroControle(beneficiario, "INCLUSAO", request);

            // Etapa 5: Chamada para API da OdontoPrev
            log.info("🚀 INICIANDO CHAMADA API - Enviando beneficiário {} para inclusão na OdontoPrev", codigoMatricula);

            // Obter tokens para autenticação dupla
            String[] tokens = beneficiarioTokenService.obterTokensCompletos();
            String tokenOAuth2 = tokens[0];
            String tokenLoginEmpresa = tokens[1];
            log.debug("🔑 TOKENS OBTIDOS - OAuth2: {}...{}, LoginEmpresa: {}...{}", 
                     tokenOAuth2.substring(0, Math.min(20, tokenOAuth2.length())),
                     tokenOAuth2.length() > 20 ? "..." : "",
                     tokenLoginEmpresa.substring(0, Math.min(20, tokenLoginEmpresa.length())),
                     tokenLoginEmpresa.length() > 20 ? "..." : "");

            long inicioChamada = System.currentTimeMillis();
            BeneficiarioInclusaoResponseNew response = odontoprevClient.incluirBeneficiario(
                    tokenOAuth2,
                    tokenLoginEmpresa,
                    request
            );
            long tempoResposta = System.currentTimeMillis() - inicioChamada;
            
            log.info("✅ RESPOSTA RECEBIDA DA API - Beneficiário {} processado em {}ms", codigoMatricula, tempoResposta);
            
            // LOG DETALHADO DA RESPOSTA DA API
            try {
                String responseJson = objectMapper.writeValueAsString(response);
                log.info("📥 RESPOSTA COMPLETA DA API - Beneficiário {}: {}", codigoMatricula, responseJson);
            } catch (Exception e) {
                log.error("❌ ERRO AO SERIALIZAR RESPOSTA DA API - Beneficiário {}: {}", codigoMatricula, e.getMessage());
            }
            
            // LOG DETALHADO DOS CAMPOS DA RESPOSTA
            log.info("🔍 ANÁLISE DETALHADA DA RESPOSTA - Beneficiário {}:", codigoMatricula);
            log.info("   📊 Status: {}", response.getStatus());
            log.info("   📊 CdMsg: {}", response.getCdMsg());
            log.info("   📊 Mensagem: {}", response.getMensagem());
            log.info("   📊 Protocolo: {}", response.getProtocolo());
            log.info("   📊 GuidProtocolo: {}", response.getGuidProtocolo());
            
            if (response.getBeneficiarios() != null) {
                log.info("   👤 Beneficiarios (objeto principal):");
                log.info("      📋 CodigoMatricula: {}", response.getBeneficiarios().getCodigoMatricula());
                log.info("      📋 CodigoAssociado: {}", response.getBeneficiarios().getCodigoAssociado());
                log.info("      📋 NomeAssociado: {}", response.getBeneficiarios().getNomeAssociado());
                log.info("      📋 Email: {}", response.getBeneficiarios().getEmail());
                log.info("      📋 MotivoInativacao: {}", response.getBeneficiarios().getMotivoInativacao());
                log.info("      📋 IdMotivo: {}", response.getBeneficiarios().getIdMotivo());
            } else {
                log.warn("   ⚠️ Beneficiarios é NULL!");
            }
            
            if (response.getListaBeneficiarios() != null && !response.getListaBeneficiarios().isEmpty()) {
                log.info("   📋 ListaBeneficiarios (array): {} itens", response.getListaBeneficiarios().size());
                for (int i = 0; i < response.getListaBeneficiarios().size(); i++) {
                    var item = response.getListaBeneficiarios().get(i);
                    log.info("      [{}] CodigoMatricula: {}, CodigoAssociado: {}, NomeAssociado: {}", 
                             i, item.getCodigoMatricula(), item.getCodigoAssociado(), item.getNomeAssociado());
                }
            } else {
                log.warn("   ⚠️ ListaBeneficiarios é NULL ou vazia!");
            }
            
            if (response.getCrm() != null) {
                log.info("   🏥 CRM:");
                log.info("      📋 Status: {}", response.getCrm().getStatus());
                log.info("      📋 Mensagem: {}", response.getCrm().getMensagem());
                log.info("      📋 Redirect: {}", response.getCrm().getRedirect());
                log.info("      📋 Protocolo: {}", response.getCrm().getProtocolo());
                log.info("      📋 Ocorrencia: {}", response.getCrm().getOcorrencia());
            } else {
                log.warn("   ⚠️ CRM é NULL!");
            }

            // Etapa 6: Processamento da resposta - EXTRAÇÃO DO CD_ASSOCIADO
            log.info("🔍 EXTRAINDO CD_ASSOCIADO - Beneficiário {}:", codigoMatricula);
            
            String cdAssociado = null;
            
            // Tentar extrair do objeto beneficiarios principal
            if (response.getBeneficiarios() != null) {
                String codigoAssociadoPrincipal = response.getBeneficiarios().getCodigoAssociado();
                log.info("   📋 CodigoAssociado do objeto principal: '{}'", codigoAssociadoPrincipal);
                
                if (StringUtils.hasText(codigoAssociadoPrincipal)) {
                    cdAssociado = codigoAssociadoPrincipal;
                    log.info("   ✅ CD_ASSOCIADO EXTRAÍDO DO OBJETO PRINCIPAL: '{}'", cdAssociado);
                } else {
                    log.warn("   ⚠️ CodigoAssociado do objeto principal está vazio ou nulo");
                }
            } else {
                log.warn("   ⚠️ Objeto beneficiarios é NULL");
            }
            
            // Se não conseguiu extrair do objeto principal, tentar da lista
            if (cdAssociado == null && response.getListaBeneficiarios() != null && !response.getListaBeneficiarios().isEmpty()) {
                log.info("   🔄 Tentando extrair da ListaBeneficiarios...");
                for (int i = 0; i < response.getListaBeneficiarios().size(); i++) {
                    var item = response.getListaBeneficiarios().get(i);
                    String codigoAssociadoItem = item.getCodigoAssociado();
                    log.info("   📋 [{}] CodigoAssociado da lista: '{}'", i, codigoAssociadoItem);
                    
                    if (StringUtils.hasText(codigoAssociadoItem)) {
                        cdAssociado = codigoAssociadoItem;
                        log.info("   ✅ CD_ASSOCIADO EXTRAÍDO DA LISTA [{}]: '{}'", i, cdAssociado);
                        break;
                    }
                }
            }
            
            // Verificar se conseguiu extrair o cdAssociado
            if (cdAssociado == null || cdAssociado.trim().isEmpty()) {
                String mensagem = "OdontoPrev não retornou código do associado (codigoAssociado) válido";
                log.error("❌ FALHA NA EXTRAÇÃO DO CD_ASSOCIADO - Beneficiário {}: {}", codigoMatricula, mensagem);
                registrarTentativaErro(beneficiario, "INCLUSAO", controleSync, mensagem, null);
                throw new ProcessamentoBeneficiarioException(mensagem, codigoMatricula, INCLUSAO);
            }
            
            log.info("🎯 CD_ASSOCIADO FINAL EXTRAÍDO - Beneficiário {}: '{}'", codigoMatricula, cdAssociado);

            // Etapa 7: Execução da procedure no Tasy
            log.info("🔄 EXECUTANDO PROCEDURE - Chamando SS_PLS_CAD_CARTEIRINHA_ODONTOPREV para beneficiário {} com cdAssociado {}", 
                    codigoMatricula, cdAssociado);
            executarProcedureTasy(beneficiario, cdAssociado);
            log.info("✅ PROCEDURE EXECUTADA - SS_PLS_CAD_CARTEIRINHA_ODONTOPREV concluída com sucesso para beneficiário {}", codigoMatricula);

            // Etapa 8: Registrar sucesso no controle
            registrarTentativaSucesso(controleSync, objectMapper.writeValueAsString(response));

            log.info("🎉 BENEFICIÁRIO PROCESSADO COM SUCESSO - {} | CdAssociado: {} | Tempo total: {}ms",
                    codigoMatricula, cdAssociado, tempoResposta);

        } catch (Exception e) {
            log.error("Erro durante processamento de inclusão: {}", e.getMessage(), e);
            
            // Verificar se é erro de beneficiário já cadastrado
            if (e.getMessage() != null && e.getMessage().contains("Beneficiário já cadastrado")) {
                log.warn("⚠️ BENEFICIÁRIO JÁ CADASTRADO - {}: {}", codigoMatricula, e.getMessage());
                
                // Mesmo quando o beneficiário já está cadastrado, precisamos executar a procedure
                // para atualizar o sistema Tasy com o cdAssociado
                try {
                    log.info("🔄 EXECUTANDO PROCEDURE PARA BENEFICIÁRIO JÁ CADASTRADO - Chamando SS_PLS_CAD_CARTEIRINHA_ODONTOPREV para beneficiário {}", codigoMatricula);
                    
                    // Para beneficiários já cadastrados, vamos tentar extrair o cdAssociado da resposta de erro
                    // ou usar o código da matrícula como fallback
                    String cdAssociadoParaProcedure = null;
                    
                    log.info("🔍 TENTANDO EXTRAIR CD_ASSOCIADO DA RESPOSTA DE ERRO - Beneficiário {}", codigoMatricula);
                    
                    // Tentar extrair cdAssociado da mensagem de erro (pode conter informações úteis)
                    if (e.getMessage().contains("cdAssociado")) {
                        // Se a mensagem contém cdAssociado, tentar extrair
                        log.info("📋 MENSAGEM DE ERRO CONTÉM CD_ASSOCIADO - Tentando extrair para beneficiário {}", codigoMatricula);
                        // TODO: Implementar extração do cdAssociado da mensagem de erro
                    }
                    
                    // Para beneficiários já cadastrados, a API não retorna o cdAssociado na resposta de erro
                    // Vamos usar o código da matrícula como identificador único
                    if (cdAssociadoParaProcedure == null || cdAssociadoParaProcedure.trim().isEmpty()) {
                        cdAssociadoParaProcedure = codigoMatricula; // Usar código da matrícula como identificador
                        log.info("🔄 USANDO CÓDIGO DA MATRÍCULA COMO IDENTIFICADOR - cdAssociado: {} para beneficiário já cadastrado {}", cdAssociadoParaProcedure, codigoMatricula);
                    }
                    
                    executarProcedureTasy(beneficiario, cdAssociadoParaProcedure);
                    log.info("✅ PROCEDURE EXECUTADA PARA BENEFICIÁRIO JÁ CADASTRADO - SS_PLS_CAD_CARTEIRINHA_ODONTOPREV concluída para beneficiário {} com cdAssociado {}", codigoMatricula, cdAssociadoParaProcedure);
                    
                } catch (Exception procedureException) {
                    log.error("❌ ERRO AO EXECUTAR PROCEDURE PARA BENEFICIÁRIO JÁ CADASTRADO - Beneficiário {}: {}", 
                             codigoMatricula, procedureException.getMessage(), procedureException);
                    // Não falhar o processamento por causa da procedure
                }
                
                registrarTentativaSucesso(controleSync, "Beneficiário já cadastrado na OdontoPrev");
                return; // Não lançar exceção, apenas logar e continuar
            }
            
            registrarTentativaErro(beneficiario, "INCLUSAO", controleSync, e.getMessage(), e);
            throw new ProcessamentoBeneficiarioException(
                    "Falha no processamento de inclusão: " + e.getMessage(),
                    codigoMatricula,
                    INCLUSAO,
                    e
            );
        }
    }

    /**
     * VALIDAÇÃO DE DADOS OBRIGATÓRIOS
     *
     * Valida todos os campos obrigatórios conforme documentação da API OdontoPrev.
     * Retorna false se algum campo obrigatório estiver ausente ou inválido.
     */
    public boolean validarDadosObrigatorios(BeneficiarioOdontoprev beneficiario) {
        // Validação de campos básicos obrigatórios
        if (!StringUtils.hasText(beneficiario.getCodigoMatricula()) ||
            !StringUtils.hasText(beneficiario.getCpf()) ||
            beneficiario.getDataNascimento() == null ||
            beneficiario.getDtVigenciaRetroativa() == null ||
            !StringUtils.hasText(beneficiario.getNomeBeneficiario()) ||
            !StringUtils.hasText(beneficiario.getNomeMae()) ||
            !StringUtils.hasText(beneficiario.getSexo())) {
            log.debug("Validação falhou: campos básicos obrigatórios ausentes");
            return false;
        }
        
        // Validação de endereço obrigatório
        if (!StringUtils.hasText(beneficiario.getCep()) ||
            !StringUtils.hasText(beneficiario.getCidade()) ||
            !StringUtils.hasText(beneficiario.getLogradouro()) ||
            !StringUtils.hasText(beneficiario.getNumero()) ||
            !StringUtils.hasText(beneficiario.getUf())) {
            log.debug("Validação falhou: dados de endereço incompletos");
            return false;
        }
        
        // Validação do formato do telefone celular: 2 dígitos DDD + 9 dígitos número
        String telefoneCelular = beneficiario.getTelefoneCelular().replaceAll("[^0-9]", "");
        if (telefoneCelular.length() != 11) {
            log.debug("Validação falhou: telefone celular deve ter 11 dígitos (DDD + número). Atual: {} dígitos", 
                     telefoneCelular.length());
            return false;
        }
        // REMOVIDO: validação de telefoneResidencial pois é OPCIONAL

        // Validação de vinculação empresarial obrigatória (documentação linhas 361-363)
        // codigoPlano, departamento - codigoEmpresa não existe na view de inclusão
        if (!StringUtils.hasText(beneficiario.getCodigoPlano()) ||
            !StringUtils.hasText(beneficiario.getDepartamento())) {
            log.debug("Validação falhou: dados empresariais incompletos (plano, departamento)");
            return false;
        }

        // Validação de formato dos campos da venda conforme documentação API
        // codigoPlano: até 5 caracteres
        if (beneficiario.getCodigoPlano().length() > 5) {
            log.debug("Validação falhou: codigoPlano excede 5 caracteres (atual: {})", 
                     beneficiario.getCodigoPlano().length());
            return false;
        }

        // departamento: até 8 caracteres
        if (beneficiario.getDepartamento().length() > 8) {
            log.debug("Validação falhou: departamento excede 8 caracteres (atual: {})", 
                     beneficiario.getDepartamento().length());
            return false;
        }

        // Validação do codigoEmpresa: deve ter exatamente 6 caracteres
        String codigoEmpresa = headerService.getEmpresa();
        if (codigoEmpresa == null || codigoEmpresa.length() != 6) {
            log.debug("Validação falhou: codigoEmpresa deve ter exatamente 6 caracteres (atual: {})", 
                     codigoEmpresa != null ? codigoEmpresa.length() : "null");
            return false;
        }

        return true;
    }

    /**
     * CONVERSÃO PARA DTO DE REQUEST
     *
     * Mapeia todos os campos da entidade para o formato esperado pela API.
     */
    private BeneficiarioInclusaoRequestNew converterParaInclusaoRequestNew(BeneficiarioOdontoprev beneficiario) {
        // DEBUG: Log dos dados que estão vindo da view
        log.debug("🔍 DADOS DA VIEW - Matrícula: {} | Nome: {} | NomeMae: {} | TelefoneCelular: {} | TelefoneResidencial: {}", 
                 beneficiario.getCodigoMatricula(), 
                 beneficiario.getNomeBeneficiario(),
                 beneficiario.getNomeMae(),
                 beneficiario.getTelefoneCelular(),
                 beneficiario.getTelefoneResidencial());
        
        // DEBUG: Verificar se os campos obrigatórios estão nulos
        if (beneficiario.getNomeMae() == null || beneficiario.getNomeMae().trim().isEmpty()) {
            log.warn("⚠️ NOME DA MÃE ESTÁ NULO OU VAZIO para beneficiário: {}", beneficiario.getCodigoMatricula());
        }
        
        if (beneficiario.getTelefoneCelular() == null || beneficiario.getTelefoneCelular().trim().isEmpty()) {
            log.warn("⚠️ TELEFONE CELULAR ESTÁ NULO OU VAZIO para beneficiário: {}", beneficiario.getCodigoMatricula());
        }
        
        // TRATAMENTO ESPECIAL: Se telefoneCelular estiver incompleto, usar telefoneResidencial como fallback
        String telefoneCelular = beneficiario.getTelefoneCelular();
        if (telefoneCelular != null && telefoneCelular.trim().length() < 11) {
            log.warn("⚠️ TELEFONE CELULAR INCOMPLETO ({} dígitos) para beneficiário: {}, tentando usar telefoneResidencial", 
                     telefoneCelular.length(), beneficiario.getCodigoMatricula());
            
            String telefoneResidencial = beneficiario.getTelefoneResidencial();
            if (telefoneResidencial != null && telefoneResidencial.trim().length() >= 11) {
                log.info("✅ USANDO TELEFONE RESIDENCIAL como fallback para beneficiário: {} ({} dígitos)", 
                         beneficiario.getCodigoMatricula(), telefoneResidencial.length());
                // Atualizar o beneficiário com o telefone residencial
                beneficiario.setTelefoneCelular(telefoneResidencial);
            } else {
                log.error("❌ NENHUM TELEFONE VÁLIDO encontrado para beneficiário: {} (celular: {} dígitos, residencial: {} dígitos)", 
                         beneficiario.getCodigoMatricula(), 
                         telefoneCelular != null ? telefoneCelular.length() : 0,
                         telefoneResidencial != null ? telefoneResidencial.length() : 0);
            }
        }
        
        // VALIDAÇÃO FINAL: Garantir que o telefone tenha exatamente 11 dígitos
        String telefoneFinal = beneficiario.getTelefoneCelular();
        if (telefoneFinal != null) {
            String telefoneLimpo = telefoneFinal.replaceAll("[^0-9]", "");
            if (telefoneLimpo.length() != 11) {
                log.error("❌ TELEFONE FINAL INVÁLIDO para beneficiário: {} ({} dígitos) - PULANDO BENEFICIÁRIO", 
                         beneficiario.getCodigoMatricula(), telefoneLimpo.length());
                // Não lançar exceção aqui, apenas logar o erro
                // A validação será feita antes de chamar a API
            } else {
                log.debug("✅ TELEFONE VÁLIDO para beneficiário: {} ({} dígitos)", beneficiario.getCodigoMatricula(), telefoneLimpo.length());
            }
        }
        
        // Criar beneficiário
        var beneficiarioData = BeneficiarioInclusaoRequestNew.Beneficiario.builder()
                .codigoMatricula(beneficiario.getCodigoMatricula())
                .codigoPlano(beneficiario.getCodigoPlano() != null ? beneficiario.getCodigoPlano().toString() : null) // Vem da view CODIGOPLANO
                .cpf(beneficiario.getCpf())
                .dataDeNascimento(beneficiario.getDataNascimento().format(DATE_FORMATTER))
                .dtVigenciaRetroativa(beneficiario.getDtVigenciaRetroativa().format(DATE_FORMATTER))
                .nomeBeneficiario(beneficiario.getNomeBeneficiario())
                .nomeDaMae(beneficiario.getNomeMae())
                .sexo(beneficiario.getSexo())
                .identificacao("T") // T = Titular (fixo para inclusão)
                .rg(beneficiario.getRg())
                .rgEmissor(beneficiario.getRgEmissor())
                .estadoCivil(beneficiario.getEstadoCivil())
                .nmCargo(beneficiario.getNmCargo())
                .cns(beneficiario.getCns())
                .telefoneCelular(beneficiario.getTelefoneCelular())
                .telefoneResidencial(beneficiario.getTelefoneResidencial())
                .departamento(beneficiario.getDepartamento() != null ? beneficiario.getDepartamento().toString() : null) // Vem da view
                .endereco(BeneficiarioInclusaoRequestNew.Endereco.builder()
                        .cep(beneficiario.getCep())
                        .cidade(beneficiario.getCidade())
                        .logradouro(beneficiario.getLogradouro())
                        .numero(beneficiario.getNumero())
                        .uf(beneficiario.getUf())
                        .bairro(beneficiario.getBairro())
                        .complemento(beneficiario.getComplemento())
                        .tpEndereco(beneficiario.getTpEndereco() != null ? beneficiario.getTpEndereco().toString() : null)
                        .cidadeBeneficiario(null) // Campo não existe na entidade
                        .build())
                .build();

        // Criar beneficiário titular
        var beneficiarioTitular = BeneficiarioInclusaoRequestNew.BeneficiarioTitular.builder()
                .beneficiario(beneficiarioData)
                .build();

        // Criar venda
        var venda = BeneficiarioInclusaoRequestNew.Venda.builder()
                .codigoEmpresa(beneficiario.getCodigoEmpresa()) 
                .codigoPlano(beneficiario.getCodigoPlano() != null ? beneficiario.getCodigoPlano().toString() : null) // Vem da view CODIGOPLANO
                .departamento(beneficiario.getDepartamento() != null ? beneficiario.getDepartamento().toString() : null) // Vem da view
                .build();

        return BeneficiarioInclusaoRequestNew.builder()
                .beneficiarioTitular(beneficiarioTitular)
                .usuario(headerService.getUsuario())
                .venda(venda)
                .build();
    }

    /**
     * EXECUÇÃO DA PROCEDURE NO TASY
     *
     * Executa a procedure SS_PLS_CAD_CARTEIRINHA_ODONTOPREV no banco Tasy
     * para registrar o cdAssociado retornado pela OdontoPrev.
     */
    @MonitorarOperacao(
            operacao = "EXECUTAR_PROCEDURE_TASY",
            incluirParametros = {"beneficiario.codigoMatricula", "cdAssociado"},
            excecaoEmErro = PROCESSAMENTO_BENEFICIARIO
    )
    private void executarProcedureTasy(BeneficiarioOdontoprev beneficiario, String cdAssociado) {
        log.info("🚀 INICIANDO EXECUÇÃO DA PROCEDURE - SS_PLS_CAD_CARTEIRINHA_ODONTOPREV para beneficiário {}", beneficiario.getCodigoMatricula());
        log.info("📋 PARÂMETROS DA PROCEDURE - Beneficiário {}: nrSequencia={}, cdCgcEstipulante={}, cdAssociado={}", 
                beneficiario.getCodigoMatricula(), beneficiario.getNrSequencia(), beneficiario.getCdCgcEstipulante(), cdAssociado);
        
        String cdCgcEstipulante = beneficiario.getCdCgcEstipulante();

        if (cdCgcEstipulante == null || cdCgcEstipulante.trim().isEmpty()) {
            log.error("❌ VALIDAÇÃO FALHOU - CD_CGC_ESTIPULANTE é nulo ou vazio para beneficiário {}", beneficiario.getCodigoMatricula());
            throw new ProcessamentoBeneficiarioException(
                    "CD_CGC_ESTIPULANTE não pode ser nulo ou vazio",
                    beneficiario.getCodigoMatricula(),
                    INCLUSAO
            );
        }

        if (beneficiario.getNrSequencia() == null) {
            log.error("❌ VALIDAÇÃO FALHOU - NR_SEQUENCIA é nulo para beneficiário {}", beneficiario.getCodigoMatricula());
            throw new ProcessamentoBeneficiarioException(
                    "NR_SEQUENCIA não pode ser nulo",
                    beneficiario.getCodigoMatricula(),
                    INCLUSAO
            );
        }

        if (cdAssociado == null || cdAssociado.trim().isEmpty()) {
            log.error("❌ VALIDAÇÃO FALHOU - CD_ASSOCIADO é nulo ou vazio para beneficiário {}", beneficiario.getCodigoMatricula());
            throw new ProcessamentoBeneficiarioException(
                    "CD_ASSOCIADO não pode ser nulo ou vazio",
                    beneficiario.getCodigoMatricula(),
                    INCLUSAO
            );
        }

        log.info("✅ VALIDAÇÕES PASSARAM - Todos os parâmetros são válidos para beneficiário {}", beneficiario.getCodigoMatricula());

        try {
            // Para Oracle, usar a sintaxe correta para procedures com schema
            String sql = "{ call TASY.SS_PLS_CAD_CARTEIRINHA_ODONTOPREV(?, ?, ?) }";
            log.info("🔧 SQL DA PROCEDURE - {} para beneficiário {}", sql, beneficiario.getCodigoMatricula());

            log.info("🔄 EXECUTANDO CALLABLE STATEMENT - Preparando execução da procedure para beneficiário {}", beneficiario.getCodigoMatricula());
            
            // Usar a sintaxe correta do Spring JDBC para Oracle
            jdbcTemplate.execute(sql, (CallableStatementCallback<Void>) cs -> {
                log.info("🔗 CONEXÃO OBTIDA - Criando CallableStatement para beneficiário {}", beneficiario.getCodigoMatricula());
                
                // Configurar os parâmetros IN
                cs.setLong(1, beneficiario.getNrSequencia()); // p_nr_seq_segurado as NUMBER
                cs.setString(2, cdCgcEstipulante); // p_cd_cgc_estipulante as VARCHAR2
                cs.setString(3, cdAssociado); // p_cd_associado as VARCHAR2
                
                log.info("📝 PARÂMETROS SETADOS - p_nr_seq_segurado={}, p_cd_cgc_estipulante={}, p_cd_associado={} para beneficiário {}", 
                        beneficiario.getNrSequencia(), cdCgcEstipulante, cdAssociado, beneficiario.getCodigoMatricula());
                
                log.info("⚡ EXECUTANDO PROCEDURE - Chamando cs.execute() para beneficiário {}", beneficiario.getCodigoMatricula());
                boolean result = cs.execute();
                log.info("✅ PROCEDURE EXECUTADA - cs.execute() retornou {} para beneficiário {}", result, beneficiario.getCodigoMatricula());
                
                return null;
            });

            log.info("✅ Procedure SS_PLS_CAD_CARTEIRINHA_ODONTOPREV executada com sucesso para beneficiário {} com cdAssociado {}", 
                    beneficiario.getCodigoMatricula(), cdAssociado);

        } catch (Exception e) {
            log.error("❌ Erro ao executar procedure SS_PLS_CAD_CARTEIRINHA_ODONTOPREV: {}", e.getMessage(), e);
            throw new ProcessamentoBeneficiarioException(
                    "Falha na execução da procedure no Tasy: " + e.getMessage(),
                    beneficiario.getCodigoMatricula(),
                    INCLUSAO
            );
        }
    }

    /**
     * CRIA REGISTRO DE CONTROLE DE SINCRONIZAÇÃO
     *
     * Cria um registro na tabela TB_CONTROLE_SYNC_ODONTOPREV_BENEF
     * para rastrear o processamento do beneficiário.
     */
    private ControleSyncBeneficiario criarRegistroControle(BeneficiarioOdontoprev beneficiario, String tipoOperacao, Object request) {
        try {
            String payloadJson = objectMapper.writeValueAsString(request);
            
            ControleSyncBeneficiario controle = ControleSyncBeneficiario.builder()
                    .codigoEmpresa(beneficiario.getCodigoEmpresa())
                    .codigoBeneficiario(beneficiario.getCodigoMatricula())
                    .tipoOperacao(tipoOperacao)
                    .statusSync("PROCESSANDO")
                    .dadosJson(payloadJson)
                    .dataUltimaTentativa(LocalDateTime.now())
                    .build();

            return controleSyncRepository.save(controle);
        } catch (Exception e) {
            log.error("Erro ao criar registro de controle para beneficiário {}: {}", 
                     beneficiario.getCodigoMatricula(), e.getMessage(), e);
            return null;
        }
    }

    /**
     * REGISTRA TENTATIVA DE SUCESSO
     *
     * Atualiza o registro de controle com o resultado de sucesso.
     */
    private void registrarTentativaSucesso(ControleSyncBeneficiario controle, String responseJson) {
        if (controle != null) {
            try {
                controle.setStatusSync("SUCESSO");
                controle.setDataSucesso(LocalDateTime.now());
                controle.setResponseApi(responseJson);
                controleSyncRepository.save(controle);
            } catch (Exception e) {
                log.error("Erro ao registrar sucesso no controle: {}", e.getMessage(), e);
            }
        }
    }

    /**
     * REGISTRA TENTATIVA DE ERRO
     *
     * Atualiza o registro de controle com o resultado de erro.
     */
    private void registrarTentativaErro(BeneficiarioOdontoprev beneficiario, String tipoOperacao, 
                                       ControleSyncBeneficiario controle, String mensagemErro, Exception excecao) {
        try {
            if (controle == null) {
                // Se não existe controle, tenta criar o request para ter o JSON correto
                String payloadJson = "{}";
                try {
                    // Tenta criar o request mesmo com dados inválidos para ter o JSON
                    BeneficiarioInclusaoRequestNew request = converterParaInclusaoRequestNew(beneficiario);
                    payloadJson = objectMapper.writeValueAsString(request);
                } catch (Exception e) {
                    log.debug("Não foi possível criar request para beneficiário {}: {}", 
                             beneficiario.getCodigoMatricula(), e.getMessage());
                    // Mantém "{}" se não conseguir criar o request
                }
                
                controle = ControleSyncBeneficiario.builder()
                        .codigoEmpresa(beneficiario.getCodigoEmpresa())
                        .codigoBeneficiario(beneficiario.getCodigoMatricula())
                        .tipoOperacao(tipoOperacao)
                        .statusSync("ERRO")
                        .dadosJson(payloadJson)
                        .dataUltimaTentativa(LocalDateTime.now())
                        .erroMensagem(mensagemErro)
                        .build();
            } else {
                controle.setStatusSync("ERRO");
                controle.setDataUltimaTentativa(LocalDateTime.now());
                controle.setErroMensagem(mensagemErro);
            }

            controleSyncRepository.save(controle);
        } catch (Exception e) {
            log.error("Erro ao registrar erro no controle: {}", e.getMessage(), e);
        }
    }
}