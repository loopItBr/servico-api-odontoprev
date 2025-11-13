package com.odontoPrev.odontoPrev.infrastructure.client.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.odontoPrev.odontoPrev.domain.entity.BeneficiarioOdontoprev;
import com.odontoPrev.odontoPrev.domain.entity.ControleSyncBeneficiario;
import com.odontoPrev.odontoPrev.domain.repository.ControleSyncBeneficiarioRepository;
import com.odontoPrev.odontoPrev.domain.service.ProcessamentoBeneficiarioExclusaoService;
import com.odontoPrev.odontoPrev.infrastructure.aop.MonitorarOperacao;
import com.odontoPrev.odontoPrev.infrastructure.client.adapter.out.BeneficiarioOdontoprevFeignClient;
import com.odontoPrev.odontoPrev.infrastructure.client.adapter.out.dto.AssociadoInativacao;
import com.odontoPrev.odontoPrev.infrastructure.client.adapter.out.dto.EmpresarialModelInativacao;
import com.odontoPrev.odontoPrev.infrastructure.exception.ProcessamentoBeneficiarioException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Collections;

import static com.odontoPrev.odontoPrev.infrastructure.aop.MonitorarOperacao.TipoExcecao.PROCESSAMENTO_BENEFICIARIO;
import static com.odontoPrev.odontoPrev.infrastructure.exception.ProcessamentoBeneficiarioException.TipoOperacao.EXCLUSAO;

/**
 * IMPLEMENTAÇÃO DO SERVIÇO DE PROCESSAMENTO DE EXCLUSÃO/INATIVAÇÃO DE BENEFICIÁRIOS
 *
 * Realiza o processamento completo de inativação de beneficiários na OdontoPrev
 * quando têm status Rescindido/Suspenso no sistema Tasy.
 *
 * FLUXO COMPLETO DE PROCESSAMENTO:
 * 1. Validação de pré-requisitos (beneficiário deve existir na OdontoPrev)
 * 2. Validação de campos obrigatórios para inativação
 * 3. Conversão de entidade para DTO de inativação
 * 4. Chamada para API da OdontoPrev (POST /inativarAssociadoEmpresarial)
 * 5. Atualização do status no banco
 * 6. Registro de logs de auditoria
 *
 * CARACTERÍSTICAS DA INATIVAÇÃO:
 * - Beneficiário deve ter cdAssociado preenchido
 * - Pode ser executada múltiplas vezes (idempotente)
 * - OdontoPrev trata duplicação de registros
 * - Não há retorno específico da API (void response)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ProcessamentoBeneficiarioExclusaoServiceImpl implements ProcessamentoBeneficiarioExclusaoService {

    private final BeneficiarioOdontoprevFeignClient odontoprevClient;
    private final ControleSyncBeneficiarioRepository controleSyncRepository;
    private final BeneficiarioTokenService beneficiarioTokenService;
    private final ObjectMapper objectMapper;

    @Value("${odontoprev.api.login.usuario}")
    private String cdUsuario;

    /**
     * PROCESSA INATIVAÇÃO DE UM ÚNICO BENEFICIÁRIO
     *
     * Executa todo o fluxo de inativação com validação e tratamento de erros.
     */
    @Override
    @Transactional
    @MonitorarOperacao(
            operacao = "PROCESSAR_INATIVACAO_BENEFICIARIO",
            incluirParametros = {"codigoMatricula", "cdAssociado"},
            excecaoEmErro = PROCESSAMENTO_BENEFICIARIO
    )
    public void processarInativacaoBeneficiario(BeneficiarioOdontoprev beneficiario) {
        String codigoMatricula = beneficiario.getCodigoMatricula();
        String cdAssociado = beneficiario.getCdAssociado();
        ControleSyncBeneficiario controleSync = null;

        try {
            // Etapa 1: Validação de pré-requisitos
            if (!validarBeneficiarioParaInativacao(beneficiario)) {
                String mensagem = "Beneficiário não atende pré-requisitos para inativação";
                atualizarStatusErro(beneficiario, mensagem, null);
                throw new ProcessamentoBeneficiarioException(mensagem, codigoMatricula, EXCLUSAO);
            }

            // Etapa 2: Conversão para EmpresarialModel
            EmpresarialModelInativacao empresarialModel = converterParaEmpresarialModel(beneficiario);

            // Etapa 3: Criar ou atualizar registro de controle
            log.info("🔍 [EXCLUSÃO] Verificando se já existe registro de controle para beneficiário {}", codigoMatricula);
            controleSync = criarOuAtualizarRegistroControle(beneficiario, empresarialModel);

            // Etapa 4: Serializar EmpresarialModel para JSON string
            String empresarialModelJson = objectMapper.writeValueAsString(empresarialModel);

            // Etapa 5: Obter tokens para autenticação dupla
            String[] tokens = beneficiarioTokenService.obterTokensCompletos();
            String tokenOAuth2 = tokens[0];
            String tokenLoginEmpresa = tokens[1];
            
            log.info("🔑 [EXCLUSÃO] Tokens obtidos - OAuth2: {}..., LoginEmpresa: {}...",
                    tokenOAuth2.substring(0, Math.min(20, tokenOAuth2.length())),
                    tokenLoginEmpresa.substring(0, Math.min(20, tokenLoginEmpresa.length())));

            // Etapa 6: Chamada para API da OdontoPrev
            log.info("🚀 [EXCLUSÃO] Enviando inativação do beneficiário {} (cdAssociado: {}) para OdontoPrev",
                    codigoMatricula, cdAssociado);

            long inicioChamada = System.currentTimeMillis();
            odontoprevClient.inativarBeneficiario(
                    tokenOAuth2,
                    tokenLoginEmpresa,
                    empresarialModelJson
            );
            long tempoResposta = System.currentTimeMillis() - inicioChamada;
            
            log.info("✅ [EXCLUSÃO] Inativação do beneficiário {} processada com sucesso em {}ms", 
                    codigoMatricula, tempoResposta);

            // Etapa 7: Atualização do status no banco
            atualizarStatusSucesso(beneficiario, controleSync);

        } catch (Exception e) {
            // Tratamento de erro abrangente
            String mensagem = "Erro durante processamento de inativação: " + e.getMessage();
            atualizarStatusErro(beneficiario, mensagem, controleSync);

            // Relança exceção para ser tratada pelo Global Exception Handler
            throw new ProcessamentoBeneficiarioException(
                    mensagem,
                    codigoMatricula,
                    EXCLUSAO,
                    e
            );
        }
    }

    /**
     * VALIDA SE BENEFICIÁRIO PODE SER INATIVADO
     *
     * Verifica pré-requisitos e campos obrigatórios para inativação.
     */
    @Override
    @MonitorarOperacao(
            operacao = "VALIDAR_BENEFICIARIO_PARA_INATIVACAO",
            incluirParametros = {"codigoMatricula"},
            excecaoEmErro = PROCESSAMENTO_BENEFICIARIO
    )
    public boolean validarBeneficiarioParaInativacao(BeneficiarioOdontoprev beneficiario) {
        // Pré-requisito 1: Deve ter cdAssociado (já existe na OdontoPrev)
        if (!StringUtils.hasText(beneficiario.getCdAssociado())) {
            log.warn("Beneficiário {} não pode ser inativado: cdAssociado não informado",
                    beneficiario.getCodigoMatricula());
            return false;
        }

        // Pré-requisito 2: Campos obrigatórios para inativação
        if (!StringUtils.hasText(beneficiario.getCodigoEmpresa()) ||
            !StringUtils.hasText(beneficiario.getCodigoMatricula()) ||
            !StringUtils.hasText(beneficiario.getNomeBeneficiario())) {
            log.warn("Beneficiário {} não pode ser inativado: campos obrigatórios ausentes",
                    beneficiario.getCodigoMatricula());
            return false;
        }

        // Pré-requisito 3: Deve ter motivo de inativação
        if (beneficiario.getIdMotivoInativacao() == null || beneficiario.getIdMotivoInativacao() <= 0) {
            log.warn("Beneficiário {} não pode ser inativado: motivo de inativação não informado",
                    beneficiario.getCodigoMatricula());
            return false;
        }

        return true;
    }

    /**
     * CONVERTE ENTIDADE PARA EMPRESARIAL MODEL DE INATIVAÇÃO
     *
     * Mapeia campos da entidade para o formato EmpresarialModel esperado
     * pela API de inativação (multipart/form-data).
     *
     * ESTRUTURA CRIADA:
     * {
     *   "cdEmpresa": "787392",
     *   "cdUsuario": "13433638",
     *   "associado": [{
     *     "cdMatricula": "...",
     *     "cdAssociado": "...",
     *     "nome": "...",
     *     "email": "...",
     *     "idMotivo": "25"
     *   }],
     *   "dataInativacao": "2024-12-29"
     * }
     */
    private EmpresarialModelInativacao converterParaEmpresarialModel(BeneficiarioOdontoprev beneficiario) {
        // Cria o objeto AssociadoInativacao
        AssociadoInativacao associado = AssociadoInativacao.builder()
                .cdMatricula(beneficiario.getCodigoMatricula())
                .cdAssociado(beneficiario.getCdAssociado())
                .nome(beneficiario.getNomeBeneficiario())
                .email(beneficiario.getEmail())
                .idMotivo("25") // Sempre 25 (numérico como string) - Iniciativa do beneficiário
                .build();

        // Determina data de inativação (usa data específica ou data atual)
        String dataInativacao = beneficiario.getDataInativacao() != null
                ? beneficiario.getDataInativacao().toString()
                : LocalDate.now().toString();

        // Cria o EmpresarialModel completo
        return EmpresarialModelInativacao.builder()
                .cdEmpresa(beneficiario.getCodigoEmpresa())
                .cdUsuario(cdUsuario) // Vem da configuração (ex: "13433638")
                .associado(Collections.singletonList(associado))
                .dataInativacao(dataInativacao)
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
            operacao = "CRIAR_OU_ATUALIZAR_REGISTRO_CONTROLE_INATIVACAO",
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
                            codigoEmpresa, codigoMatricula, "EXCLUSAO");

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
                        .tipoLog("E")
                        .tipoOperacao("EXCLUSAO")
                        .endpointDestino("/cadastroonline-pj/1.0/inativar")
                        .dadosJson(payloadJson)
                        .statusSync("PROCESSING") // PROCESSING = 10 caracteres (máximo permitido)
                        .tentativas(1)
                        .maxTentativas(3)
                        .dataUltimaTentativa(LocalDateTime.now())
                        .build();
                
                log.info("🆕 [CONTROLE] Criando novo registro de controle para beneficiário {}", codigoMatricula);
            }

            ControleSyncBeneficiario controleSalvo = controleSyncRepository.save(controle);
            log.info("✅ [CONTROLE] Registro de controle processado - ID: {}, Status: {}, Tipo: {}", 
                    controleSalvo.getId(), controleSalvo.getStatusSync(),
                    controleSalvo.getTentativas() > 1 ? "ATUALIZAÇÃO" : "CRIAÇÃO");
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
                controle.setResponseApi("Inativação realizada com sucesso");
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
        try {
            if (controle == null) {
                // Se não existe controle, tenta criar o request para ter o JSON correto
                String payloadJson = "{}";
                try {
                    // Tenta criar o request mesmo com dados inválidos para ter o JSON
                    EmpresarialModelInativacao request = converterParaEmpresarialModel(beneficiario);
                    payloadJson = objectMapper.writeValueAsString(request);
                } catch (Exception e) {
                    log.debug("Não foi possível criar request para beneficiário {}: {}", 
                             beneficiario.getCodigoMatricula(), e.getMessage());
                    // Mantém "{}" se não conseguir criar o request
                }
                
                controle = ControleSyncBeneficiario.builder()
                        .codigoEmpresa(beneficiario.getCodigoEmpresa())
                        .codigoBeneficiario(beneficiario.getCodigoMatricula())
                        .tipoLog("E") // E = Exclusão
                        .tipoOperacao("EXCLUSAO")
                        .endpointDestino("/cadastroonline-pj/1.0/inativar")
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
