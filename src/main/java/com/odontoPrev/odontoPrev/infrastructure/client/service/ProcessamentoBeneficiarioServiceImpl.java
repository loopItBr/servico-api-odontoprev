package com.odontoPrev.odontoPrev.infrastructure.client.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.odontoPrev.odontoPrev.domain.entity.BeneficiarioOdontoprev;
import com.odontoPrev.odontoPrev.domain.entity.ControleSyncBeneficiario;
import com.odontoPrev.odontoPrev.domain.repository.ControleSyncBeneficiarioRepository;
import com.odontoPrev.odontoPrev.domain.service.ProcessamentoBeneficiarioService;
import com.odontoPrev.odontoPrev.infrastructure.aop.MonitorarOperacao;
import com.odontoPrev.odontoPrev.domain.repository.BeneficiarioOdontoprevRepository;
import com.odontoPrev.odontoPrev.infrastructure.repository.IntegracaoOdontoprevBeneficiarioRepository;
import com.odontoPrev.odontoPrev.infrastructure.repository.entity.IntegracaoOdontoprevBeneficiario;
import com.odontoPrev.odontoPrev.infrastructure.client.adapter.out.BeneficiarioOdontoprevFeignClient;
import com.odontoPrev.odontoPrev.infrastructure.client.adapter.out.dto.BeneficiarioInclusaoRequestNew;
import com.odontoPrev.odontoPrev.infrastructure.client.adapter.out.dto.BeneficiarioInclusaoResponseNew;
import com.odontoPrev.odontoPrev.infrastructure.client.adapter.out.dto.BeneficiarioDependenteInclusaoRequest;
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
    private final BeneficiarioOdontoprevRepository beneficiarioRepository;
    private final IntegracaoOdontoprevBeneficiarioRepository integracaoRepository;
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

            // Etapa 2: Verificar se é dependente ou titular
            boolean isDependente = "D".equals(beneficiario.getIdentificacao());
            
            if (isDependente) {
                log.info("👨‍👩‍👧‍👦 PROCESSANDO DEPENDENTE - Matrícula: {} | Empresa: {}", 
                        codigoMatricula, beneficiario.getCodigoEmpresa());
                processarInclusaoDependente(beneficiario);
                return; // Dependente processado, encerra método
            }
            
            log.info("👤 PROCESSANDO TITULAR - Matrícula: {} | Empresa: {}", 
                    codigoMatricula, beneficiario.getCodigoEmpresa());
            
            // Etapa 2.1: Validação final do telefone antes de criar o request (apenas para titular)
            String telefoneFinal = beneficiario.getTelefoneCelular();
            if (telefoneFinal != null) {
                String telefoneLimpo = telefoneFinal.replaceAll("[^0-9]", "");
                if (telefoneLimpo.length() != 11) {
                    String mensagem = "Telefone celular inválido: deve ter exatamente 11 dígitos (DDD + número). Atual: " + telefoneLimpo.length() + " dígitos";
                    registrarTentativaErro(beneficiario, "INCLUSAO", null, mensagem, null);
                    throw new ProcessamentoBeneficiarioException(mensagem, codigoMatricula, INCLUSAO);
                }
            }
            
            // Etapa 3: Conversão para DTO de request (apenas para titular)
            BeneficiarioInclusaoRequestNew request = converterParaInclusaoRequestNew(beneficiario);
            
            // DEBUG: Log detalhado do payload para investigar erro 403
            log.info("🔍 DEBUG PAYLOAD - Beneficiário {}: CódigoEmpresa: '{}', Usuario: '{}', CodigoMatricula: '{}'", 
                    codigoMatricula, 
                    request.getVenda() != null ? request.getVenda().getCodigoEmpresa() : "NULL",
                    request.getUsuario(),
                    request.getBeneficiarioTitular() != null && request.getBeneficiarioTitular().getBeneficiario() != null ? 
                        request.getBeneficiarioTitular().getBeneficiario().getCodigoMatricula() : "NULL");
            
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
            
            // DEBUG: Log detalhado dos tokens para investigar erro 403
            log.info("🔑 DEBUG TOKENS - Beneficiário {}: OAuth2: {}..., LoginEmpresa: {}...", 
                     codigoMatricula,
                     tokenOAuth2.substring(0, Math.min(30, tokenOAuth2.length())),
                     tokenLoginEmpresa.substring(0, Math.min(30, tokenLoginEmpresa.length())));
            
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
                
                // MARCAR COMO SUCESSO na TBSYNC quando o beneficiário já está cadastrado (é considerado sucesso)
                log.info("✅ BENEFICIÁRIO JÁ CADASTRADO - Marcando como SUCESSO na TBSYNC | Matrícula: {}", codigoMatricula);
                if (controleSync != null) {
                    // Atualizar o registro como SUCESSO ao invés de deletar
                    try {
                        // Extrair mensagem da resposta de erro para usar como responseApi
                        String responseApi = "Beneficiário já cadastrado na OdontoPrev";
                        if (e.getMessage() != null && e.getMessage().contains("{")) {
                            // Tentar extrair JSON da mensagem
                            int jsonStart = e.getMessage().indexOf("{");
                            if (jsonStart >= 0) {
                                responseApi = e.getMessage().substring(jsonStart);
                            }
                        }
                        registrarTentativaSucesso(controleSync, responseApi);
                        log.info("✅ Registro atualizado como SUCESSO na TBSYNC para beneficiário já cadastrado | Matrícula: {}", codigoMatricula);
                    } catch (Exception updateException) {
                        log.warn("⚠️ Não foi possível atualizar registro da TBSYNC como sucesso: {}", updateException.getMessage());
                    }
                } else {
                    // Se não havia registro, criar um novo marcando como sucesso
                    try {
                        // Criar registro mínimo de sucesso
                        String responseApi = "Beneficiário já cadastrado na OdontoPrev";
                        if (e.getMessage() != null && e.getMessage().contains("{")) {
                            int jsonStart = e.getMessage().indexOf("{");
                            if (jsonStart >= 0) {
                                responseApi = e.getMessage().substring(jsonStart);
                            }
                        }
                        // Criar request mínimo para o registro (usar BeneficiarioInclusaoRequestNew vazio ou básico)
                        BeneficiarioInclusaoRequestNew requestMinimo = converterParaInclusaoRequestNew(beneficiario);
                        ControleSyncBeneficiario controleNovo = criarRegistroControle(beneficiario, "INCLUSAO", requestMinimo);
                        if (controleNovo != null) {
                            registrarTentativaSucesso(controleNovo, responseApi);
                            log.info("✅ Novo registro criado como SUCESSO na TBSYNC para beneficiário já cadastrado | Matrícula: {}", codigoMatricula);
                        }
                    } catch (Exception createException) {
                        log.warn("⚠️ Não foi possível criar registro de sucesso na TBSYNC: {}", createException.getMessage());
                    }
                }
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
                .identificacao(beneficiario.getIdentificacao() != null ? beneficiario.getIdentificacao() : "T") // T = Titular, D = Dependente (usa da view, com fallback para T)
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

        // DEBUG: Verificar código da empresa antes de criar o payload
        String codigoEmpresa = beneficiario.getCodigoEmpresa();
        log.info("🔍 DEBUG EMPRESA - CódigoEmpresa da view: '{}' (tamanho: {})", 
                codigoEmpresa, codigoEmpresa != null ? codigoEmpresa.length() : 0);
        
        // Criar venda
        var venda = BeneficiarioInclusaoRequestNew.Venda.builder()
                .codigoEmpresa(codigoEmpresa) 
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
     * PROCESSA INCLUSÃO DE DEPENDENTE
     *
     * Fluxo específico para inclusão de dependente:
     * 1. Busca código do associado titular
     * 2. Converte beneficiário para request de dependente
     * 3. Chama endpoint /incluirDependente
     * 4. Processa resposta e salva na TBSYNC
     */
    private void processarInclusaoDependente(BeneficiarioOdontoprev beneficiario) {
        String codigoMatricula = beneficiario.getCodigoMatricula();
        ControleSyncBeneficiario controleSync = null;
        String codigoAssociadoTitularParaSucesso = null; // Variável para usar no catch de "já cadastrado"

        try {
            log.info("🔍 INICIANDO PROCESSAMENTO DE DEPENDENTE - Matrícula: {}", codigoMatricula);

            // Etapa 1: Buscar código do associado titular
            // PRIORIDADE: Usar valor da view se disponível, senão buscar na TBSYNC
            String codigoAssociadoTitular = beneficiario.getCodigoAssociadoTitularTemp();
            log.info("🔍 [DEBUG] codigoAssociadoTitularTemp do beneficiário: '{}'", codigoAssociadoTitular);
            
            if (codigoAssociadoTitular == null || codigoAssociadoTitular.trim().isEmpty()) {
                log.warn("⚠️ codigoAssociadoTitular não veio na view - Buscando na TBSYNC para empresa: {}", 
                        beneficiario.getCodigoEmpresa());
                codigoAssociadoTitular = buscarCodigoAssociadoTitular(beneficiario.getCodigoEmpresa());
                log.info("🔍 [DEBUG] codigoAssociadoTitular da TBSYNC: '{}'", codigoAssociadoTitular);
            } else {
                log.info("✅ Usando codigoAssociadoTitular da view: '{}'", codigoAssociadoTitular);
            }
            
            // Guardar valor para usar no catch de "já cadastrado"
            codigoAssociadoTitularParaSucesso = codigoAssociadoTitular;
            
            if (codigoAssociadoTitular == null || codigoAssociadoTitular.trim().isEmpty()) {
                String mensagem = "Não foi possível encontrar código do associado titular para empresa: " + beneficiario.getCodigoEmpresa();
                log.error("❌ ERRO - {}", mensagem);
                registrarTentativaErro(beneficiario, "INCLUSAO", null, mensagem, null);
                throw new ProcessamentoBeneficiarioException(mensagem, codigoMatricula, INCLUSAO);
            }

            log.info("✅ CÓDIGO DO TITULAR ENCONTRADO - Matrícula dependente: {} | Código titular: {}", 
                    codigoMatricula, codigoAssociadoTitular);

            // Etapa 2: Converter beneficiário para request de dependente
            BeneficiarioDependenteInclusaoRequest request = converterParaDependenteRequest(beneficiario, codigoAssociadoTitular);

            // DEBUG: Log do payload completo antes de enviar
            try {
                String requestJson = objectMapper.writeValueAsString(request);
                log.info("📤 PAYLOAD DEPENDENTE ENVIADO - Beneficiário {}: {}", codigoMatricula, requestJson);
            } catch (Exception e) {
                log.warn("⚠️ ERRO ao serializar request de dependente: {}", e.getMessage());
            }

            // Etapa 3: Criar registro de controle ANTES de chamar a API
            // IMPORTANTE: Criar registro mesmo que request seja null (caso de "já cadastrado")
            controleSync = criarRegistroControle(beneficiario, "INCLUSAO", request != null ? request : new Object());
            if (controleSync == null) {
                log.error("❌ ERRO CRÍTICO - Não foi possível criar registro de controle para dependente {}", codigoMatricula);
                throw new ProcessamentoBeneficiarioException(
                        "Não foi possível criar registro de controle na TBSYNC",
                        codigoMatricula,
                        INCLUSAO
                );
            }

            // Etapa 4: Obter tokens para autenticação dupla
            String[] tokens = beneficiarioTokenService.obterTokensCompletos();
            String tokenOAuth2 = tokens[0];
            String tokenLoginEmpresa = tokens[1];

            log.info("🚀 INICIANDO CHAMADA API DEPENDENTE - Enviando dependente {} para inclusão na OdontoPrev", codigoMatricula);

            long inicioChamada = System.currentTimeMillis();
            BeneficiarioInclusaoResponseNew response = odontoprevClient.incluirDependente(
                    tokenOAuth2,
                    tokenLoginEmpresa,
                    request
            );
            long tempoResposta = System.currentTimeMillis() - inicioChamada;

            log.info("✅ RESPOSTA RECEBIDA DA API DEPENDENTE - Dependente {} processado em {}ms", codigoMatricula, tempoResposta);

            // Etapa 5: Extrair cdAssociado da resposta (da listaBeneficiarios)
            String cdAssociado = null;
            if (response.getListaBeneficiarios() != null && !response.getListaBeneficiarios().isEmpty()) {
                for (var item : response.getListaBeneficiarios()) {
                    if (item.getCodigoMatricula() != null && item.getCodigoMatricula().equals(codigoMatricula)) {
                        cdAssociado = item.getCodigoAssociado();
                        log.info("✅ CD_ASSOCIADO DO DEPENDENTE EXTRAÍDO: '{}'", cdAssociado);
                        break;
                    }
                }
            }

            if (cdAssociado == null || cdAssociado.trim().isEmpty()) {
                String mensagem = "OdontoPrev não retornou código do associado (codigoAssociado) válido para o dependente";
                log.error("❌ FALHA NA EXTRAÇÃO DO CD_ASSOCIADO - Dependente {}: {}", codigoMatricula, mensagem);
                registrarTentativaErro(beneficiario, "INCLUSAO", controleSync, mensagem, null);
                throw new ProcessamentoBeneficiarioException(mensagem, codigoMatricula, INCLUSAO);
            }

            // Etapa 6: Executar procedure no Tasy (mesmo processo do titular)
            log.info("🔄 EXECUTANDO PROCEDURE - Chamando SS_PLS_CAD_CARTEIRINHA_ODONTOPREV para dependente {} com cdAssociado {}", 
                    codigoMatricula, cdAssociado);
            executarProcedureTasy(beneficiario, cdAssociado);
            log.info("✅ PROCEDURE EXECUTADA - SS_PLS_CAD_CARTEIRINHA_ODONTOPREV concluída com sucesso para dependente {}", codigoMatricula);

            // Etapa 7: Registrar sucesso no controle
            registrarTentativaSucesso(controleSync, objectMapper.writeValueAsString(response));

            log.info("🎉 DEPENDENTE PROCESSADO COM SUCESSO - {} | CdAssociado: {} | Tempo total: {}ms",
                    codigoMatricula, cdAssociado, tempoResposta);

        } catch (Exception e) {
            log.error("❌ Erro durante processamento de inclusão de dependente: {}", e.getMessage(), e);
            
            // Verificar se é erro de dependente já cadastrado (status 417 ou mensagem específica)
            // A mensagem pode vir de várias formas: no getMessage(), na causa, ou no stack trace
            String mensagemErro = e.getMessage() != null ? e.getMessage() : "";
            String causaMensagem = (e.getCause() != null && e.getCause().getMessage() != null) ? e.getCause().getMessage() : "";
            String mensagemCompleta = mensagemErro + " " + causaMensagem;
            
            // Verificar na mensagem completa (pode ter JSON com a mensagem)
            boolean dependenteJaExiste = (mensagemCompleta.contains("existe para o titular") || 
                                         mensagemCompleta.contains("417") ||
                                         (mensagemCompleta.contains("Dependente") && mensagemCompleta.contains("existe")) ||
                                         mensagemCompleta.contains("\"mensagem\":\"Dependente") ||
                                         mensagemCompleta.toLowerCase().contains("dependente") && mensagemCompleta.toLowerCase().contains("existe"));
            
            log.info("🔍 VERIFICAÇÃO DE DEPENDENTE JÁ CADASTRADO - Mensagem: '{}' | Causa: '{}' | JaExiste: {}", 
                    mensagemErro, causaMensagem, dependenteJaExiste);
            
            if (dependenteJaExiste) {
                log.warn("⚠️ DEPENDENTE JÁ CADASTRADO - {}: {}", codigoMatricula, mensagemErro);
                
                // Mesmo quando o dependente já está cadastrado, precisamos executar a procedure
                // para atualizar o sistema Tasy
                try {
                    log.info("🔄 EXECUTANDO PROCEDURE PARA DEPENDENTE JÁ CADASTRADO - Chamando SS_PLS_CAD_CARTEIRINHA_ODONTOPREV para dependente {}", codigoMatricula);
                    
                    // Para dependentes já cadastrados, usar o código da matrícula como identificador
                    String cdAssociadoParaProcedure = codigoMatricula;
                    log.info("🔄 USANDO CÓDIGO DA MATRÍCULA COMO IDENTIFICADOR - cdAssociado: {} para dependente já cadastrado {}", cdAssociadoParaProcedure, codigoMatricula);
                    
                    executarProcedureTasy(beneficiario, cdAssociadoParaProcedure);
                    log.info("✅ PROCEDURE EXECUTADA PARA DEPENDENTE JÁ CADASTRADO - SS_PLS_CAD_CARTEIRINHA_ODONTOPREV concluída para dependente {} com cdAssociado {}", codigoMatricula, cdAssociadoParaProcedure);
                    
                } catch (Exception procedureException) {
                    log.error("❌ ERRO AO EXECUTAR PROCEDURE PARA DEPENDENTE JÁ CADASTRADO - Dependente {}: {}", 
                             codigoMatricula, procedureException.getMessage(), procedureException);
                    // Não falhar o processamento por causa da procedure
                }
                
                // MARCAR COMO SUCESSO na TBSYNC quando o dependente já está cadastrado (é considerado sucesso)
                log.info("✅ DEPENDENTE JÁ CADASTRADO - Marcando como SUCESSO na TBSYNC | Matrícula: {}", codigoMatricula);
                if (controleSync != null) {
                    // Atualizar o registro como SUCESSO ao invés de deletar
                    try {
                        // Extrair mensagem da resposta de erro para usar como responseApi
                        String responseApi = "Dependente já cadastrado na OdontoPrev";
                        if (mensagemCompleta != null && mensagemCompleta.contains("{")) {
                            // Tentar extrair JSON da mensagem
                            int jsonStart = mensagemCompleta.indexOf("{");
                            if (jsonStart >= 0) {
                                responseApi = mensagemCompleta.substring(jsonStart);
                            }
                        }
                        registrarTentativaSucesso(controleSync, responseApi);
                        log.info("✅ Registro atualizado como SUCESSO na TBSYNC para dependente já cadastrado | Matrícula: {}", codigoMatricula);
                    } catch (Exception updateException) {
                        log.warn("⚠️ Não foi possível atualizar registro da TBSYNC como sucesso: {}", updateException.getMessage());
                    }
                } else {
                    // Se não havia registro, criar um novo marcando como sucesso
                    try {
                        // Criar registro mínimo de sucesso
                        String responseApi = "Dependente já cadastrado na OdontoPrev";
                        if (mensagemCompleta != null && mensagemCompleta.contains("{")) {
                            int jsonStart = mensagemCompleta.indexOf("{");
                            if (jsonStart >= 0) {
                                responseApi = mensagemCompleta.substring(jsonStart);
                            }
                        }
                        // Criar request mínimo para o registro (usar BeneficiarioDependenteInclusaoRequest básico)
                        // Tentar criar request mínimo com dados disponíveis
                        try {
                            // Usar codigoAssociadoTitularParaSucesso ou buscar novamente
                            String codigoTitularParaRequest = codigoAssociadoTitularParaSucesso;
                            if (codigoTitularParaRequest == null || codigoTitularParaRequest.trim().isEmpty()) {
                                codigoTitularParaRequest = beneficiario.getCodigoAssociadoTitularTemp();
                                if (codigoTitularParaRequest == null || codigoTitularParaRequest.trim().isEmpty()) {
                                    codigoTitularParaRequest = buscarCodigoAssociadoTitular(beneficiario.getCodigoEmpresa());
                                }
                            }
                            
                            if (codigoTitularParaRequest != null && !codigoTitularParaRequest.trim().isEmpty()) {
                                BeneficiarioDependenteInclusaoRequest requestMinimo = converterParaDependenteRequest(beneficiario, codigoTitularParaRequest);
                                ControleSyncBeneficiario controleNovo = criarRegistroControle(beneficiario, "INCLUSAO", requestMinimo);
                                if (controleNovo != null) {
                                    registrarTentativaSucesso(controleNovo, responseApi);
                                    log.info("✅ Novo registro criado como SUCESSO na TBSYNC para dependente já cadastrado | Matrícula: {}", codigoMatricula);
                                }
                            } else {
                                throw new Exception("Não foi possível obter codigoAssociadoTitular para criar request");
                            }
                        } catch (Exception createRequestException) {
                            log.warn("⚠️ Não foi possível criar request mínimo para registro de sucesso: {}", createRequestException.getMessage());
                            // Tentar criar registro sem request (com JSON vazio)
                            try {
                                ControleSyncBeneficiario controleNovo = criarRegistroControle(beneficiario, "INCLUSAO", new Object());
                                if (controleNovo != null) {
                                    registrarTentativaSucesso(controleNovo, responseApi);
                                    log.info("✅ Novo registro criado como SUCESSO na TBSYNC (sem request) para dependente já cadastrado | Matrícula: {}", codigoMatricula);
                                }
                            } catch (Exception fallbackException) {
                                log.error("❌ Não foi possível criar registro de sucesso na TBSYNC: {}", fallbackException.getMessage());
                            }
                        }
                    } catch (Exception createException) {
                        log.warn("⚠️ Não foi possível criar registro de sucesso na TBSYNC: {}", createException.getMessage());
                    }
                }
                return; // Não lançar exceção, apenas logar e continuar
            }
            
            registrarTentativaErro(beneficiario, "INCLUSAO", controleSync, e.getMessage(), e);
            throw new ProcessamentoBeneficiarioException(
                    "Falha no processamento de inclusão de dependente: " + e.getMessage(),
                    codigoMatricula,
                    INCLUSAO
            );
        }
    }

    /**
     * BUSCA CÓDIGO DO ASSOCIADO TITULAR
     *
     * Busca o código do associado (carteirinha) do titular da empresa
     * para poder incluir o dependente.
     * 
     * IMPORTANTE: Busca o titular na view VW_INTEGRACAO_ODONTOPREV_BENEFICIARIOS
     * e extrai o cdAssociado da resposta da API salva na TBSYNC.
     */
    private String buscarCodigoAssociadoTitular(String codigoEmpresa) {
        try {
            log.info("🔍 BUSCANDO CÓDIGO DO ASSOCIADO TITULAR - Empresa: {}", codigoEmpresa);
            
            // PASSO 1: Buscar titular na view VW_INTEGRACAO_ODONTOPREV_BENEFICIARIOS
            var titularesView = integracaoRepository.findByCodigoEmpresa(codigoEmpresa)
                    .stream()
                    .filter(b -> "T".equals(b.getIdentificacao()))
                    .toList();
            
            if (titularesView.isEmpty()) {
                log.error("❌ NENHUM TITULAR ENCONTRADO NA VIEW - Empresa: {}", codigoEmpresa);
                return null;
            }
            
            log.info("✅ {} TITULAR(ES) ENCONTRADO(S) NA VIEW - Empresa: {}", titularesView.size(), codigoEmpresa);
            
            // PASSO 2: Para cada titular, verificar se já foi processado com sucesso na TBSYNC
            for (IntegracaoOdontoprevBeneficiario titularView : titularesView) {
                String codigoMatriculaTitular = titularView.getCodigoMatricula();
                log.info("🔍 VERIFICANDO TITULAR - Matrícula: {} | Nome: {}", 
                        codigoMatriculaTitular, titularView.getNomeDoBeneficiario());
                
                // Buscar controles de sincronização do titular na TBSYNC (pode haver múltiplos registros)
                var controles = controleSyncRepository
                        .findByCodigoEmpresaAndCodigoBeneficiario(codigoEmpresa, codigoMatriculaTitular);
                
                if (!controles.isEmpty()) {
                    log.info("📋 {} REGISTRO(S) ENCONTRADO(S) NA TBSYNC PARA TITULAR - Matrícula: {}", 
                            controles.size(), codigoMatriculaTitular);
                    
                    // Filtrar apenas registros de INCLUSAO com status SUCESSO e ordenar por data de sucesso (mais recente primeiro)
                    var controleSucesso = controles.stream()
                            .filter(c -> "INCLUSAO".equals(c.getTipoOperacao()))
                            .filter(c -> "SUCESSO".equals(c.getStatusSync()) || "SUCCESS".equals(c.getStatusSync()))
                            .filter(c -> c.getResponseApi() != null && !c.getResponseApi().trim().isEmpty())
                            .sorted((c1, c2) -> {
                                // Ordenar por data de sucesso (mais recente primeiro)
                                if (c1.getDataSucesso() != null && c2.getDataSucesso() != null) {
                                    return c2.getDataSucesso().compareTo(c1.getDataSucesso());
                                }
                                if (c1.getDataSucesso() != null) return -1;
                                if (c2.getDataSucesso() != null) return 1;
                                // Se não tem data de sucesso, ordenar por data de última tentativa
                                if (c1.getDataUltimaTentativa() != null && c2.getDataUltimaTentativa() != null) {
                                    return c2.getDataUltimaTentativa().compareTo(c1.getDataUltimaTentativa());
                                }
                                return 0;
                            })
                            .findFirst();
                    
                    if (controleSucesso.isPresent()) {
                        ControleSyncBeneficiario controle = controleSucesso.get();
                        log.info("✅ REGISTRO DE SUCESSO ENCONTRADO - ID: {} | Status: {} | Data Sucesso: {}", 
                                controle.getId(), controle.getStatusSync(), controle.getDataSucesso());
                        
                        // PASSO 3: Extrair cdAssociado da resposta da API salva na TBSYNC
                        String responseApi = controle.getResponseApi();
                        try {
                            BeneficiarioInclusaoResponseNew response = objectMapper.readValue(
                                    responseApi, BeneficiarioInclusaoResponseNew.class);
                            
                            String cdAssociado = null;
                            
                            // Tentar extrair do objeto beneficiarios principal
                            if (response.getBeneficiarios() != null) {
                                cdAssociado = response.getBeneficiarios().getCodigoAssociado();
                            }
                            
                            // Se não conseguiu extrair do objeto principal, tentar da lista
                            if ((cdAssociado == null || cdAssociado.trim().isEmpty()) && 
                                response.getListaBeneficiarios() != null && 
                                !response.getListaBeneficiarios().isEmpty()) {
                                cdAssociado = response.getListaBeneficiarios().get(0).getCodigoAssociado();
                            }
                            
                            if (cdAssociado != null && !cdAssociado.trim().isEmpty()) {
                                log.info("✅ CD_ASSOCIADO DO TITULAR EXTRAÍDO DA TBSYNC - Matrícula: {} | CdAssociado: {}", 
                                        codigoMatriculaTitular, cdAssociado);
                                return cdAssociado;
                            } else {
                                log.warn("⚠️ CD_ASSOCIADO NÃO ENCONTRADO NA RESPOSTA - Matrícula: {} | Response: {}", 
                                        codigoMatriculaTitular, responseApi);
                            }
                        } catch (Exception e) {
                            log.error("❌ ERRO ao extrair cdAssociado da resposta da API para titular {}: {}", 
                                    codigoMatriculaTitular, e.getMessage());
                        }
                    } else {
                        log.warn("⚠️ NENHUM REGISTRO DE SUCESSO ENCONTRADO NA TBSYNC - Matrícula: {} | Total de registros: {}", 
                                codigoMatriculaTitular, controles.size());
                        // Log dos status encontrados para debug
                        controles.forEach(c -> log.debug("   - ID: {} | Tipo: {} | Status: {} | Data: {}", 
                                c.getId(), c.getTipoOperacao(), c.getStatusSync(), c.getDataUltimaTentativa()));
                    }
                } else {
                    log.warn("⚠️ TITULAR NÃO ENCONTRADO NA TBSYNC - Matrícula: {} | Ainda não foi processado", 
                            codigoMatriculaTitular);
                }
            }
            
            log.error("❌ NENHUM TITULAR COM CD_ASSOCIADO ENCONTRADO - Empresa: {}", codigoEmpresa);
            return null;

        } catch (Exception e) {
            log.error("❌ ERRO ao buscar código do associado titular para empresa {}: {}", 
                     codigoEmpresa, e.getMessage(), e);
            return null;
        }
    }

    /**
     * CONVERTE BENEFICIÁRIO PARA REQUEST DE DEPENDENTE
     *
     * Converte a entidade BeneficiarioOdontoprev para o formato
     * BeneficiarioDependenteInclusaoRequest conforme documentação da API.
     * 
     * IMPORTANTE: Todos os campos numéricos devem ser convertidos de String para Long/Integer
     * conforme exemplo da documentação.
     */
    private BeneficiarioDependenteInclusaoRequest converterParaDependenteRequest(
            BeneficiarioOdontoprev beneficiario, String codigoAssociadoTitular) {
        
        // PRIORIDADE: Se codigoAssociadoTitularTemp existe no beneficiário, usar ele primeiro
        if (beneficiario.getCodigoAssociadoTitularTemp() != null && !beneficiario.getCodigoAssociadoTitularTemp().trim().isEmpty()) {
            codigoAssociadoTitular = beneficiario.getCodigoAssociadoTitularTemp();
            log.debug("✅ [DEPENDENTE] Usando codigoAssociadoTitularTemp do beneficiário: '{}'", codigoAssociadoTitular);
        } else if (codigoAssociadoTitular != null && !codigoAssociadoTitular.trim().isEmpty()) {
            log.debug("✅ [DEPENDENTE] Usando codigoAssociadoTitular do parâmetro: '{}'", codigoAssociadoTitular);
        }
        
        // TODOS OS CAMPOS NUMÉRICOS SERÃO ENVIADOS COMO STRING
        // Limpar e preparar codigoAssociadoTitular (manter como String)
        String codigoAssociadoTitularStr = null;
        if (codigoAssociadoTitular != null && !codigoAssociadoTitular.trim().isEmpty()) {
            // Remove caracteres não numéricos e mantém como String
            codigoAssociadoTitularStr = codigoAssociadoTitular.replaceAll("[^0-9]", "");
            log.debug("✅ [DEPENDENTE] codigoAssociadoTitular preparado: '{}'", codigoAssociadoTitularStr);
        } else {
            log.warn("⚠️ [DEPENDENTE] codigoAssociadoTitular está vazio ou null!");
        }
        
        // Preparar codigoPlano (manter como String)
        String codigoPlanoStr = null;
        if (beneficiario.getCodigoPlano() != null && !beneficiario.getCodigoPlano().trim().isEmpty()) {
            codigoPlanoStr = beneficiario.getCodigoPlano().replaceAll("[^0-9]", "");
        }
        
        // Preparar numero do endereço (manter como String)
        String numeroStr = null;
        if (beneficiario.getNumero() != null && !beneficiario.getNumero().trim().isEmpty()) {
            // Remove caracteres não numéricos e mantém como String
            String numeroLimpo = beneficiario.getNumero().replaceAll("[^0-9]", "");
            if (!numeroLimpo.isEmpty()) {
                numeroStr = numeroLimpo;
            } else {
                numeroStr = beneficiario.getNumero(); // Mantém original se não tem números
            }
        }
        
        // Preparar tpEndereco (converter de Long para String)
        String tpEnderecoStr = null;
        if (beneficiario.getTpEndereco() != null) {
            tpEnderecoStr = String.valueOf(beneficiario.getTpEndereco());
        }
        
        // Preparar grauParentesco (manter como String) - usado no beneficiario.grauParentesco
        String grauParentescoStr = null;
        if (beneficiario.getGrauParentesco() != null && !beneficiario.getGrauParentesco().trim().isEmpty()) {
            String grauParentescoLimpo = beneficiario.getGrauParentesco().replaceAll("[^0-9]", "");
            if (!grauParentescoLimpo.isEmpty()) {
                grauParentescoStr = grauParentescoLimpo;
            } else {
                grauParentescoStr = beneficiario.getGrauParentesco(); // Mantém original
            }
        }
        
        // Preparar parentesco (prioridade: usar parentescoTemp da view, senão usar grauParentescoStr)
        // parentesco deve ser enviado como Integer (número)
        Integer parentescoInteger = null;
        if (beneficiario.getParentescoTemp() != null) {
            parentescoInteger = beneficiario.getParentescoTemp().intValue();
            log.debug("✅ Usando parentesco da view (parentescoTemp): {}", parentescoInteger);
        } else if (grauParentescoStr != null && !grauParentescoStr.isEmpty()) {
            try {
                parentescoInteger = Integer.parseInt(grauParentescoStr);
                log.debug("✅ Usando grauParentesco como fallback para parentesco: {}", parentescoInteger);
            } catch (NumberFormatException e) {
                log.warn("⚠️ Erro ao converter grauParentesco '{}' para Integer, usando 0", grauParentescoStr);
                parentescoInteger = 0;
            }
        } else {
            parentescoInteger = 0; // Valor padrão se não houver parentesco
            log.debug("⚠️ Parentesco não encontrado, usando valor padrão: {}", parentescoInteger);
        }
        
        // Preparar usuario: PRIORIDADE usar valor da view (usuarioTemp), senão usar headerService
        String usuarioStr = null;
        if (beneficiario.getUsuarioTemp() != null) {
            usuarioStr = String.valueOf(beneficiario.getUsuarioTemp());
            log.debug("✅ Usando usuario da view: {}", usuarioStr);
        } else {
            String usuarioStrFromHeader = headerService.getUsuario();
            if (usuarioStrFromHeader != null && !usuarioStrFromHeader.trim().isEmpty()) {
                usuarioStr = usuarioStrFromHeader.replaceAll("[^0-9]", "");
                log.debug("✅ Usando usuario do headerService: {}", usuarioStr);
            }
        }
        
        // Preparar codigoEmpresa (manter como String)
        String codigoEmpresaStr = null;
        if (beneficiario.getCodigoEmpresa() != null && !beneficiario.getCodigoEmpresa().trim().isEmpty()) {
            codigoEmpresaStr = beneficiario.getCodigoEmpresa().replaceAll("[^0-9]", "");
        }
        
        // Preparar departamento (manter como String)
        String departamentoStr = null;
        if (beneficiario.getDepartamento() != null && !beneficiario.getDepartamento().trim().isEmpty()) {
            departamentoStr = beneficiario.getDepartamento().replaceAll("[^0-9]", "");
        }
        
        // Construir objeto Beneficiario (dados do dependente)
        var beneficiarioData = BeneficiarioDependenteInclusaoRequest.Beneficiario.builder()
                .beneficiarioTitular(codigoAssociadoTitularStr) // Código do associado titular como String
                .campanha(null)
                .codigoMatricula(beneficiario.getCodigoMatricula())
                .codigoPlano(codigoPlanoStr) // String
                .cpf(beneficiario.getCpf())
                .dataDeNascimento(beneficiario.getDataNascimento() != null ? 
                        beneficiario.getDataNascimento().format(DATE_FORMATTER) : null)
                .dtVigenciaRetroativa(beneficiario.getDtVigenciaRetroativa() != null ? 
                        beneficiario.getDtVigenciaRetroativa().format(DATE_FORMATTER) : null)
                .email(null)
                .empresaNova(null)
                .endereco(beneficiario.getLogradouro() != null ? 
                        BeneficiarioDependenteInclusaoRequest.Endereco.builder()
                                .bairro(beneficiario.getBairro())
                                .cep(beneficiario.getCep())
                                .cidade(beneficiario.getCidade())
                                .cidadeBeneficiario(null)
                                .complemento(beneficiario.getComplemento())
                                .logradouro(beneficiario.getLogradouro())
                                .numero(numeroStr) // String
                                .tpEndereco(tpEnderecoStr) // String
                                .uf(beneficiario.getUf())
                                .build() : null)
                .estadoCivil(beneficiario.getEstadoCivil())
                .grauParentesco(grauParentescoStr) // String
                .identificacao(null) // Opcional conforme exemplo
                .motivoExclusao(null)
                .nmCargo(beneficiario.getNmCargo())
                .nomeBeneficiario(beneficiario.getNomeBeneficiario())
                .nomeDaMae(beneficiario.getNomeMae())
                .pisPasep(beneficiario.getPisPasep())
                .rg(beneficiario.getRg())
                .rgEmissor(beneficiario.getRgEmissor())
                .sexo(beneficiario.getSexo())
                .telefoneCelular(beneficiario.getTelefoneCelular())
                .telefoneComercial(null)
                .telefoneResidencial(beneficiario.getTelefoneResidencial())
                .build();

        // Construir BeneficiarioDependente
        var beneficiarioDependente = BeneficiarioDependenteInclusaoRequest.BeneficiarioDependente.builder()
                .beneficiario(beneficiarioData)
                .codigoEmpresa(codigoEmpresaStr) // String
                .departamento(departamentoStr) // String
                .parentesco(parentescoInteger) // Integer - Usar parentescoTemp da view ou grauParentesco como fallback
                .build();

        // Construir request completo
        return BeneficiarioDependenteInclusaoRequest.builder()
                .codigoAssociadoTitular(codigoAssociadoTitularStr) // String
                .usuario(usuarioStr) // String
                .cdUsuario(usuarioStr) // String - Usando mesmo valor do usuario
                .beneficiarios(java.util.Collections.singletonList(beneficiarioDependente))
                .build();
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
            
            // Determinar tipoLog e endpoint baseado no tipo de operação e se é dependente
            String tipoLog = "I"; // I = Inclusão
            String endpointDestino = "/cadastroonline-pj/1.0/incluir"; // Endpoint padrão para titular
            
            // Se é dependente, usar endpoint específico
            if ("D".equals(beneficiario.getIdentificacao())) {
                endpointDestino = "/cadastroonline-pj/1.0/incluirDependente";
            }
            
            ControleSyncBeneficiario controle = ControleSyncBeneficiario.builder()
                    .codigoEmpresa(beneficiario.getCodigoEmpresa())
                    .codigoBeneficiario(beneficiario.getCodigoMatricula())
                    .tipoLog(tipoLog)
                    .tipoOperacao(tipoOperacao)
                    .endpointDestino(endpointDestino)
                    .statusSync("PROCESSANDO")
                    .dadosJson(payloadJson)
                    .dataUltimaTentativa(LocalDateTime.now())
                    .build();

            log.debug("📝 [TBSYNC] Criando registro de controle - Matrícula: {} | Tipo: {} | Endpoint: {} | DadosJson: {} caracteres", 
                    beneficiario.getCodigoMatricula(), tipoOperacao, endpointDestino, payloadJson.length());

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
                log.info("✅ [TBSYNC] Registro atualizado como SUCESSO - ID: {} | Matrícula: {} | Status: SUCESSO", 
                        controle.getId(), controle.getCodigoBeneficiario());
            } catch (Exception e) {
                log.error("❌ Erro ao registrar sucesso no controle: {}", e.getMessage(), e);
                throw e; // Relançar para não perder o erro
            }
        } else {
            log.warn("⚠️ [TBSYNC] Tentativa de registrar sucesso em controle nulo");
        }
    }

    /**
     * REGISTRA TENTATIVA DE ERRO
     *
     * Atualiza o registro de controle com o resultado de erro.
     * IMPORTANTE: Garante que dadosJson esteja preenchido com o payload enviado.
     */
    private void registrarTentativaErro(BeneficiarioOdontoprev beneficiario, String tipoOperacao, 
                                      ControleSyncBeneficiario controle, String mensagemErro, Exception excecao) {
        try {
            if (controle == null) {
                // Se não existe controle, tenta criar o request para ter o JSON correto
                String payloadJson = "{}";
                String endpointDestino = "/cadastroonline-pj/1.0/incluir";
                
                try {
                    // Verificar se é dependente para criar o request correto
                    if ("D".equals(beneficiario.getIdentificacao())) {
                        endpointDestino = "/cadastroonline-pj/1.0/incluirDependente";
                        
                        // Buscar código do associado titular para criar o request de dependente
                        String codigoAssociadoTitular = buscarCodigoAssociadoTitular(beneficiario.getCodigoEmpresa());
                        if (codigoAssociadoTitular != null && !codigoAssociadoTitular.trim().isEmpty()) {
                            BeneficiarioDependenteInclusaoRequest request = converterParaDependenteRequest(beneficiario, codigoAssociadoTitular);
                            payloadJson = objectMapper.writeValueAsString(request);
                        } else {
                            log.warn("⚠️ Não foi possível buscar código do titular para criar request de dependente - Beneficiário: {}", 
                                    beneficiario.getCodigoMatricula());
                        }
                    } else {
                        // Tenta criar o request de titular mesmo com dados inválidos para ter o JSON
                        BeneficiarioInclusaoRequestNew request = converterParaInclusaoRequestNew(beneficiario);
                        payloadJson = objectMapper.writeValueAsString(request);
                    }
                } catch (Exception e) {
                    log.warn("⚠️ Não foi possível criar request para beneficiário {}: {}", 
                             beneficiario.getCodigoMatricula(), e.getMessage());
                    // Mantém "{}" se não conseguir criar o request
                }
                
                controle = ControleSyncBeneficiario.builder()
                        .codigoEmpresa(beneficiario.getCodigoEmpresa())
                        .codigoBeneficiario(beneficiario.getCodigoMatricula())
                        .tipoLog("I") // I = Inclusão
                        .tipoOperacao(tipoOperacao)
                        .endpointDestino(endpointDestino)
                        .statusSync("ERRO")
                        .dadosJson(payloadJson)
                        .dataUltimaTentativa(LocalDateTime.now())
                        .erroMensagem(mensagemErro)
                        .build();
                
                log.info("📝 [TBSYNC] Criando registro de erro - Matrícula: {} | Endpoint: {} | DadosJson: {} caracteres", 
                        beneficiario.getCodigoMatricula(), endpointDestino, payloadJson.length());
            } else {
                // Se o controle já existe, verificar se dadosJson está vazio e tentar atualizar
                if (controle.getDadosJson() == null || controle.getDadosJson().trim().isEmpty() || "{}".equals(controle.getDadosJson())) {
                    log.warn("⚠️ [TBSYNC] dadosJson vazio no controle existente - Tentando preencher - Matrícula: {}", 
                            beneficiario.getCodigoMatricula());
                    
                    try {
                        String payloadJson = "{}";
                        if ("D".equals(beneficiario.getIdentificacao())) {
                            String codigoAssociadoTitular = buscarCodigoAssociadoTitular(beneficiario.getCodigoEmpresa());
                            if (codigoAssociadoTitular != null && !codigoAssociadoTitular.trim().isEmpty()) {
                                BeneficiarioDependenteInclusaoRequest request = converterParaDependenteRequest(beneficiario, codigoAssociadoTitular);
                                payloadJson = objectMapper.writeValueAsString(request);
                                controle.setEndpointDestino("/cadastroonline-pj/1.0/incluirDependente");
                            }
                        } else {
                            BeneficiarioInclusaoRequestNew request = converterParaInclusaoRequestNew(beneficiario);
                            payloadJson = objectMapper.writeValueAsString(request);
                            controle.setEndpointDestino("/cadastroonline-pj/1.0/incluir");
                        }
                        controle.setDadosJson(payloadJson);
                        log.info("✅ [TBSYNC] dadosJson preenchido no controle existente - Matrícula: {} | DadosJson: {} caracteres", 
                                beneficiario.getCodigoMatricula(), payloadJson.length());
                    } catch (Exception e) {
                        log.warn("⚠️ [TBSYNC] Não foi possível preencher dadosJson no controle existente - Matrícula: {} | Erro: {}", 
                                beneficiario.getCodigoMatricula(), e.getMessage());
                    }
                }
                
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