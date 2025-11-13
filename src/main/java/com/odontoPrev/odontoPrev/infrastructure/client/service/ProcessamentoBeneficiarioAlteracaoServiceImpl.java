package com.odontoPrev.odontoPrev.infrastructure.client.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.odontoPrev.odontoPrev.domain.entity.BeneficiarioOdontoprev;
import com.odontoPrev.odontoPrev.domain.entity.ControleSyncBeneficiario;
import com.odontoPrev.odontoPrev.domain.repository.ControleSyncBeneficiarioRepository;
import com.odontoPrev.odontoPrev.domain.service.ProcessamentoBeneficiarioAlteracaoService;
import com.odontoPrev.odontoPrev.infrastructure.aop.MonitorarOperacao;
import com.odontoPrev.odontoPrev.infrastructure.client.adapter.out.BeneficiarioOdontoprevFeignClient;
import com.odontoPrev.odontoPrev.infrastructure.client.adapter.out.dto.BeneficiarioAlteracaoRequestNew;
import com.odontoPrev.odontoPrev.infrastructure.exception.ProcessamentoBeneficiarioException;
import com.odontoPrev.odontoPrev.infrastructure.repository.IntegracaoOdontoprevBeneficiarioRepository;
import com.odontoPrev.odontoPrev.infrastructure.repository.entity.IntegracaoOdontoprevBeneficiario;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static com.odontoPrev.odontoPrev.infrastructure.aop.MonitorarOperacao.TipoExcecao.PROCESSAMENTO_BENEFICIARIO;
import static com.odontoPrev.odontoPrev.infrastructure.exception.ProcessamentoBeneficiarioException.TipoOperacao.ALTERACAO;

/**
 * IMPLEMENTAÇÃO DO SERVIÇO DE PROCESSAMENTO DE ALTERAÇÕES DE BENEFICIÁRIOS
 *
 * Realiza o processamento completo de alteração de dados cadastrais de
 * beneficiários já existentes na OdontoPrev.
 *
 * FLUXO COMPLETO DE PROCESSAMENTO:
 * 1. Validação de pré-requisitos (beneficiário deve existir na OdontoPrev)
 * 2. Validação de campos obrigatórios para alteração
 * 3. Conversão de entidade para DTO de alteração
 * 4. Chamada para API da OdontoPrev (PUT /alterar)
 * 5. Atualização do status no banco
 * 6. Registro de logs de auditoria
 *
 * CARACTERÍSTICAS DA ALTERAÇÃO:
 * - Beneficiário deve ter cdAssociado preenchido
 * - Apenas campos alterados no dia corrente são enviados
 * - Não há retorno específico da API (void response)
 * - Operação é idempotente (pode ser executada múltiplas vezes)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ProcessamentoBeneficiarioAlteracaoServiceImpl implements ProcessamentoBeneficiarioAlteracaoService {

    private final BeneficiarioOdontoprevFeignClient odontoprevClient;
    private final ControleSyncBeneficiarioRepository controleSyncRepository;
    private final IntegracaoOdontoprevBeneficiarioRepository integracaoOdontoprevBeneficiarioRepository;
    private final BeneficiarioTokenService beneficiarioTokenService;
    private final ObjectMapper objectMapper;

    /**
     * PROCESSA ALTERAÇÃO DE UM ÚNICO BENEFICIÁRIO
     *
     * Executa todo o fluxo de alteração com validação e tratamento de erros.
     */
    @Override
    @Transactional
    @MonitorarOperacao(
            operacao = "PROCESSAR_ALTERACAO_BENEFICIARIO",
            incluirParametros = {"codigoMatricula", "cdAssociado"},
            excecaoEmErro = PROCESSAMENTO_BENEFICIARIO
    )
    public void processarAlteracaoBeneficiario(BeneficiarioOdontoprev beneficiario) {
        String codigoMatricula = beneficiario.getCodigoMatricula();
        String cdAssociado = beneficiario.getCdAssociado();
        ControleSyncBeneficiario controleSync = null;

        try {
            // Etapa 1: Validação de pré-requisitos
            if (!validarBeneficiarioParaAlteracao(beneficiario)) {
                String mensagem = "Beneficiário não atende pré-requisitos para alteração";
                atualizarStatusErro(beneficiario, mensagem, null);
                throw new ProcessamentoBeneficiarioException(mensagem, codigoMatricula, ALTERACAO);
            }

            // Etapa 2: Conversão para DTO de alteração
            BeneficiarioAlteracaoRequestNew request = converterParaAlteracaoRequestNew(beneficiario);

            // Etapa 3: Criar ou atualizar registro de controle
            log.info("🔍 [ALTERAÇÃO] Verificando se já existe registro de controle para beneficiário {}", codigoMatricula);
            controleSync = criarOuAtualizarRegistroControle(beneficiario, request);
            if (controleSync == null) {
                String mensagem = "Falha ao criar registro de controle para beneficiário " + codigoMatricula;
                log.error("❌ [ALTERAÇÃO] {}", mensagem);
                throw new ProcessamentoBeneficiarioException(mensagem, codigoMatricula, ALTERACAO);
            }
            log.info("📝 [CONTROLE] Registro de controle processado - ID: {}, Status: {}, Tipo: {}", 
                    controleSync.getId(), controleSync.getStatusSync(), 
                    controleSync.getTentativas() > 1 ? "ATUALIZAÇÃO" : "CRIAÇÃO");

            // Etapa 4: Chamada para API da OdontoPrev
            log.info("🚀 [ALTERAÇÃO] Enviando alteração do beneficiário {} (cdAssociado: {}) para OdontoPrev",
                    codigoMatricula, cdAssociado);

            // Obter tokens para autenticação dupla
            String[] tokens = beneficiarioTokenService.obterTokensCompletos();
            String tokenOAuth2 = tokens[0];
            String tokenLoginEmpresa = tokens[1];
            
            log.info("🔑 [ALTERAÇÃO] Tokens obtidos - OAuth2: {}..., LoginEmpresa: {}...",
                    tokenOAuth2.substring(0, Math.min(20, tokenOAuth2.length())),
                    tokenLoginEmpresa.substring(0, Math.min(20, tokenLoginEmpresa.length())));

            long inicioChamada = System.currentTimeMillis();
            odontoprevClient.alterarBeneficiarioNew(
                    tokenOAuth2,
                    tokenLoginEmpresa,
                    List.of(request) // Enviar como array conforme documentação da API
            );
            long tempoResposta = System.currentTimeMillis() - inicioChamada;
            
            log.info("✅ [ALTERAÇÃO] Alteração do beneficiário {} processada com sucesso em {}ms", 
                    codigoMatricula, tempoResposta);

            // Etapa 5: Atualização do status no banco
            atualizarStatusSucesso(beneficiario, controleSync);

        } catch (Exception e) {
            // Tratamento de erro abrangente
            String mensagem = "Erro durante processamento de alteração: " + e.getMessage();
            atualizarStatusErro(beneficiario, mensagem, controleSync);

            if (e instanceof ProcessamentoBeneficiarioException) {
                throw e; // Re-lança exceção específica
            } else {
                throw new ProcessamentoBeneficiarioException(mensagem, codigoMatricula, ALTERACAO, e);
            }
        }
    }

    /**
     * VALIDA SE BENEFICIÁRIO PODE SER ALTERADO
     *
     * Verifica pré-requisitos e campos obrigatórios para alteração.
     */
    @Override
    @MonitorarOperacao(
            operacao = "VALIDAR_BENEFICIARIO_PARA_ALTERACAO",
            incluirParametros = {"codigoMatricula"},
            excecaoEmErro = PROCESSAMENTO_BENEFICIARIO
    )
    public boolean validarBeneficiarioParaAlteracao(BeneficiarioOdontoprev beneficiario) {
        // Pré-requisito 1: Deve ter cdAssociado (já existe na OdontoPrev)
        if (!StringUtils.hasText(beneficiario.getCdAssociado())) {
            log.warn("Beneficiário {} não pode ser alterado: cdAssociado não informado",
                    beneficiario.getCodigoMatricula());
            return false;
        }

        // Pré-requisito 2: Campos obrigatórios para alteração
        if (!StringUtils.hasText(beneficiario.getCodigoEmpresa()) ||
            !StringUtils.hasText(beneficiario.getCodigoPlano()) ||
            !StringUtils.hasText(beneficiario.getDepartamento())) {
            log.warn("Beneficiário {} não pode ser alterado: campos obrigatórios ausentes",
                    beneficiario.getCodigoMatricula());
            return false;
        }

        return true;
    }

    /**
     * CONVERTE ENTIDADE PARA DTO DE ALTERAÇÃO (NOVA API)
     *
     * Mapeia campos da entidade para formato esperado pela nova API de alteração.
     * Implementa lógica de merge: busca dados completos na view de inclusão e 
     * atualiza apenas os campos não nulos da view de alteração.
     * 
     * ESTRATÉGIA DE MERGE:
     * 1. Busca dados completos na view de inclusão (VW_INTEGRACAO_ODONTOPREV_BENEFICIARIOS)
     * 2. Atualiza apenas campos não nulos da view de alteração (VW_INTEGRACAO_ODONTOPREV_BENEFICIARIOS_ALT)
     * 3. Formata datas no padrão dd/mm/yyyy conforme API
     * 4. Valida campos obrigatórios
     */
    private BeneficiarioAlteracaoRequestNew converterParaAlteracaoRequestNew(BeneficiarioOdontoprev beneficiario) {
        // PASSO 1: Verificar formato da matrícula vinda da view
        String codigoMatricula = beneficiario.getCodigoMatricula();
        log.info("🔍 MATRÍCULA DA VIEW - Código: '{}', Tamanho: {} dígitos", 
                codigoMatricula, codigoMatricula != null ? codigoMatricula.length() : 0);
        
        if (codigoMatricula != null && codigoMatricula.length() == 6) {
            log.info("✅ MATRÍCULA CORRETA - View retornou matrícula com 6 dígitos: '{}'", codigoMatricula);
        } else {
            log.warn("⚠️ MATRÍCULA INCORRETA - View retornou matrícula com {} dígitos: '{}'", 
                    codigoMatricula != null ? codigoMatricula.length() : 0, codigoMatricula);
        }

        // PASSO 2: Buscar dados completos na view de inclusão
        IntegracaoOdontoprevBeneficiario dadosCompletos = null;
        if (codigoMatricula != null) {
            try {
                dadosCompletos = integracaoOdontoprevBeneficiarioRepository.findByCodigoMatricula(codigoMatricula);
                if (dadosCompletos != null) {
                    log.debug("✅ Dados completos obtidos da view de inclusão para codigoMatricula: {}", codigoMatricula);
                } else {
                    log.warn("⚠️ Beneficiário não encontrado na view de inclusão para codigoMatricula: {}", codigoMatricula);
                }
            } catch (Exception e) {
                log.error("❌ Erro ao buscar dados completos da view de inclusão: {}", e.getMessage());
            }
        }

        // PASSO 2: Merge dos dados - usar dados completos como base e atualizar com alterações
        String cpf = dadosCompletos != null ? dadosCompletos.getCpf() : null;
        String tpEndereco = dadosCompletos != null ? dadosCompletos.getTpEndereco() : null;
        
        // Dados de endereço - priorizar alterações se não nulas
        String cep = beneficiario.getCep() != null ? beneficiario.getCep() : 
                    (dadosCompletos != null ? dadosCompletos.getCep() : null);
        String logradouro = beneficiario.getLogradouro() != null ? beneficiario.getLogradouro() : 
                           (dadosCompletos != null ? dadosCompletos.getLogradouro() : null);
        String numero = beneficiario.getNumero() != null ? beneficiario.getNumero() : 
                       (dadosCompletos != null ? dadosCompletos.getNumero() : null);
        String complemento = beneficiario.getComplemento() != null ? beneficiario.getComplemento() : 
                            (dadosCompletos != null ? dadosCompletos.getComplemento() : null);
        String bairro = beneficiario.getBairro() != null ? beneficiario.getBairro() : 
                       (dadosCompletos != null ? dadosCompletos.getBairro() : null);
        String cidade = beneficiario.getCidade() != null ? beneficiario.getCidade() : 
                       (dadosCompletos != null ? dadosCompletos.getCidade() : null);
        String uf = beneficiario.getUf() != null ? beneficiario.getUf() : 
                   (dadosCompletos != null ? dadosCompletos.getUf() : null);

        // PASSO 3: Criar objeto Endereco com dados mesclados
        BeneficiarioAlteracaoRequestNew.Endereco endereco = BeneficiarioAlteracaoRequestNew.Endereco.builder()
                .cep(cep)
                .logradouro(logradouro)
                .numero(numero)
                .complemento(complemento)
                .bairro(bairro)
                .cidade(cidade)
                .uf(uf)
                .tpEndereco(tpEndereco)
                .build();

        // PASSO 4: Merge dos dados do beneficiário
        String nomeBeneficiario = beneficiario.getNomeBeneficiario() != null ? beneficiario.getNomeBeneficiario() : 
                                 (dadosCompletos != null ? dadosCompletos.getNomeDoBeneficiario() : null);
        String nomeMae = beneficiario.getNomeMae() != null ? beneficiario.getNomeMae() : 
                        (dadosCompletos != null ? dadosCompletos.getNomeDaMae() : null);
        String sexo = beneficiario.getSexo() != null ? beneficiario.getSexo() : 
                     (dadosCompletos != null ? dadosCompletos.getSexo() : null);
        String telefoneCelular = beneficiario.getTelefoneCelular() != null ? beneficiario.getTelefoneCelular() : 
                                (dadosCompletos != null ? dadosCompletos.getTelefoneCelular() : null);
        String telefoneResidencial = beneficiario.getTelefoneResidencial() != null ? beneficiario.getTelefoneResidencial() : 
                                    (dadosCompletos != null ? dadosCompletos.getTelefoneResidencial() : null);
        String rg = beneficiario.getRg() != null ? beneficiario.getRg() : 
                   (dadosCompletos != null ? dadosCompletos.getRg() : null);
        String rgEmissor = beneficiario.getRgEmissor() != null ? beneficiario.getRgEmissor() : 
                          (dadosCompletos != null ? dadosCompletos.getRgEmissor() : null);
        String estadoCivil = beneficiario.getEstadoCivil() != null ? beneficiario.getEstadoCivil() : 
                            (dadosCompletos != null ? dadosCompletos.getEstadoCivil() : null);
        String nmCargo = beneficiario.getNmCargo() != null ? beneficiario.getNmCargo() : 
                        (dadosCompletos != null ? dadosCompletos.getNmCargo() : null);
        String pisPasep = beneficiario.getPisPasep() != null ? beneficiario.getPisPasep() : null;
        String email = beneficiario.getEmail() != null ? beneficiario.getEmail() : null;

        // PASSO 5: Formatar datas no padrão dd/mm/yyyy
        String dataNascimento = null;
        if (beneficiario.getDataNascimento() != null) {
            dataNascimento = beneficiario.getDataNascimento().format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy"));
        } else if (dadosCompletos != null && dadosCompletos.getDataDeNascimento() != null) {
            dataNascimento = dadosCompletos.getDataDeNascimento(); // Já está no formato correto
        }

        String dtVigenciaRetroativa = null;
        if (beneficiario.getDtVigenciaRetroativa() != null) {
            dtVigenciaRetroativa = beneficiario.getDtVigenciaRetroativa().format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy"));
        } else if (dadosCompletos != null && dadosCompletos.getDtVigenciaRetroativa() != null) {
            dtVigenciaRetroativa = dadosCompletos.getDtVigenciaRetroativa(); // Já está no formato correto
        }

        // PASSO 6: Criar objeto Beneficiario com dados mesclados
        BeneficiarioAlteracaoRequestNew.Beneficiario beneficiarioData = BeneficiarioAlteracaoRequestNew.Beneficiario.builder()
                .codigoMatricula(beneficiario.getCodigoMatricula())
                .codigoPlano(beneficiario.getCodigoPlano() != null ? beneficiario.getCodigoPlano().toString() : null)
                .cpf(cpf)
                .dataDeNascimento(dataNascimento)
                .dtVigenciaRetroativa(dtVigenciaRetroativa)
                .nomeBeneficiario(nomeBeneficiario)
                .nomeDaMae(nomeMae)
                .sexo(sexo)
                .telefoneCelular(telefoneCelular)
                .telefoneResidencial(telefoneResidencial)
                .rg(rg)
                .rgEmissor(rgEmissor)
                .estadoCivil(estadoCivil)
                .nmCargo(nmCargo)
                .pisPasep(pisPasep)
                .email(email)
                .endereco(endereco)
                .build();

        // PASSO 7: Criar request com campos obrigatórios
        return BeneficiarioAlteracaoRequestNew.builder()
                .cdEmpresa(beneficiario.getCodigoEmpresa())
                .codigoAssociado(beneficiario.getCdAssociado())
                .codigoPlano(beneficiario.getCodigoPlano() != null ? beneficiario.getCodigoPlano().toString() : null)
                .departamento(beneficiario.getDepartamento() != null ? beneficiario.getDepartamento().toString() : null)
                .beneficiario(beneficiarioData)
                .build();
    }


    /**
     * CRIA OU ATUALIZA REGISTRO DE CONTROLE DE SINCRONIZAÇÃO
     * 
     * Verifica se já existe um registro de controle para este beneficiário.
     * Se existir, atualiza o registro existente.
     * Se não existir, cria um novo registro.
     */
    @MonitorarOperacao(
            operacao = "CRIAR_OU_ATUALIZAR_REGISTRO_CONTROLE_ALTERACAO",
            excecaoEmErro = PROCESSAMENTO_BENEFICIARIO
    )
    private ControleSyncBeneficiario criarOuAtualizarRegistroControle(
            BeneficiarioOdontoprev beneficiario,
            Object payload) {

        try {
            String payloadJson = objectMapper.writeValueAsString(payload);
            String codigoMatricula = beneficiario.getCodigoMatricula();
            String codigoEmpresa = beneficiario.getCodigoEmpresa();

            // Verificar se já existe um registro de controle para este beneficiário
            Optional<ControleSyncBeneficiario> controleExistente = controleSyncRepository
                    .findByCodigoEmpresaAndCodigoBeneficiarioAndTipoOperacao(
                            codigoEmpresa, codigoMatricula, "ALTERACAO");

            ControleSyncBeneficiario controle;

            if (controleExistente.isPresent()) {
                // Atualizar registro existente
                controle = controleExistente.get();
                controle.setDadosJson(payloadJson);
                controle.setStatusSync("PROCESSING"); // PROCESSING = 10 caracteres (máximo permitido)
                controle.setTentativas(controle.getTentativas() + 1);
                controle.setDataUltimaTentativa(LocalDateTime.now());
                controle.setResponseApi(null); // Limpar resposta anterior
                controle.setErroMensagem(null); // Limpar erro anterior
                
                log.info("🔄 [CONTROLE] Atualizando registro existente para beneficiário {} - ID: {}, Tentativa: {}", 
                        codigoMatricula, controle.getId(), controle.getTentativas());
            } else {
                // Criar novo registro
                controle = ControleSyncBeneficiario.builder()
                        .codigoEmpresa(codigoEmpresa)
                        .codigoBeneficiario(codigoMatricula)
                        .tipoLog("A")
                        .tipoOperacao("ALTERACAO")
                        .endpointDestino("/cadastroonline-pj/1.0/alterar")
                        .dadosJson(payloadJson)
                        .statusSync("PROCESSING") // PROCESSING = 10 caracteres (máximo permitido)
                        .tentativas(1)
                        .maxTentativas(3)
                        .dataUltimaTentativa(LocalDateTime.now())
                        .build();
                
                log.info("🆕 [CONTROLE] Criando novo registro de controle para beneficiário {}", codigoMatricula);
            }

            ControleSyncBeneficiario controleSalvo = controleSyncRepository.save(controle);
            log.info("✅ [CONTROLE] Registro de controle salvo com sucesso - ID: {}, Status: {}", 
                    controleSalvo.getId(), controleSalvo.getStatusSync());
            return controleSalvo;

        } catch (Exception e) {
            log.error("❌ [CONTROLE] Erro ao criar/atualizar registro de controle para beneficiário {}: {}",
                    beneficiario.getCodigoMatricula(), e.getMessage(), e);
            return null;
        }
    }

    /**
     * REGISTRA TENTATIVA DE SUCESSO
     *
     * Atualiza o registro de controle com o resultado de sucesso.
     */
    private void atualizarStatusSucesso(BeneficiarioOdontoprev beneficiario, ControleSyncBeneficiario controle) {
        if (controle != null) {
            try {
                controle.setStatusSync("SUCESSO");
                controle.setDataSucesso(LocalDateTime.now());
                controle.setResponseApi("Alteração realizada com sucesso");
                controleSyncRepository.save(controle);
                log.info("Status do beneficiário {} atualizado para SUCESSO no controle de sincronização", beneficiario.getCodigoMatricula());
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
    private void atualizarStatusErro(BeneficiarioOdontoprev beneficiario, String mensagemErro, ControleSyncBeneficiario controle) {
        log.info("🔄 [CONTROLE] Atualizando status de erro para beneficiário {} - Controle existe: {}", 
                beneficiario.getCodigoMatricula(), controle != null);
        try {
            if (controle == null) {
                // Se não existe controle, tenta criar o request para ter o JSON correto
                String payloadJson = "{}";
                try {
                    // Tenta criar o request mesmo com dados inválidos para ter o JSON
                    BeneficiarioAlteracaoRequestNew request = converterParaAlteracaoRequestNew(beneficiario);
                    payloadJson = objectMapper.writeValueAsString(request);
                } catch (Exception e) {
                    log.debug("Não foi possível criar request para beneficiário {}: {}", 
                             beneficiario.getCodigoMatricula(), e.getMessage());
                    // Mantém "{}" se não conseguir criar o request
                }
                
                controle = ControleSyncBeneficiario.builder()
                        .codigoEmpresa(beneficiario.getCodigoEmpresa())
                        .codigoBeneficiario(beneficiario.getCodigoMatricula())
                        .tipoLog("A") // A = Alteração
                        .tipoOperacao("ALTERACAO")
                        .endpointDestino("/cadastroonline-pj/1.0/alterar")
                        .dadosJson(payloadJson)
                        .statusSync("ERRO")
                        .tentativas(1)
                        .erroMensagem(mensagemErro)
                        .dataUltimaTentativa(LocalDateTime.now())
                        .build();
            } else {
                controle.setStatusSync("ERRO");
                controle.setDataUltimaTentativa(LocalDateTime.now());
                controle.setErroMensagem(mensagemErro);
            }

            controleSyncRepository.save(controle);
            log.info("Status do beneficiário {} atualizado para ERRO no controle de sincronização: {}", beneficiario.getCodigoMatricula(), mensagemErro);
        } catch (Exception e) {
            log.error("Erro ao registrar erro no controle: {}", e.getMessage(), e);
        }
    }
}
