package com.odontoPrev.odontoPrev.infrastructure.client.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.odontoPrev.odontoPrev.domain.entity.BeneficiarioOdontoprev;
import com.odontoPrev.odontoPrev.domain.entity.ControleSyncBeneficiario;
import com.odontoPrev.odontoPrev.domain.repository.BeneficiarioOdontoprevRepository;
import com.odontoPrev.odontoPrev.domain.repository.ControleSyncBeneficiarioRepository;
import com.odontoPrev.odontoPrev.domain.service.ProcessamentoBeneficiarioExclusaoService;
import com.odontoPrev.odontoPrev.infrastructure.aop.MonitorarOperacao;
import com.odontoPrev.odontoPrev.infrastructure.client.adapter.out.BeneficiarioOdontoprevFeignClient;
import com.odontoPrev.odontoPrev.infrastructure.client.adapter.out.dto.AssociadoInativacao;
import com.odontoPrev.odontoPrev.infrastructure.client.adapter.out.dto.EmpresarialModelInativacao;
import com.odontoPrev.odontoPrev.infrastructure.client.domain.service.TokenService;
import com.odontoPrev.odontoPrev.infrastructure.exception.ProcessamentoBeneficiarioException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
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
    private final BeneficiarioOdontoprevRepository beneficiarioRepository;
    private final ControleSyncBeneficiarioRepository controleSyncRepository;
    private final TokenService tokenService;
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

            // Etapa 3: Criar registro de controle
            controleSync = criarRegistroControle(beneficiario, empresarialModel);

            // Etapa 4: Serializar EmpresarialModel para JSON string
            String empresarialModelJson = objectMapper.writeValueAsString(empresarialModel);

            // Etapa 5: Obter token de autenticação
            String token = tokenService.obterTokenValido();

            // Etapa 6: Chamada para API da OdontoPrev
            log.info("Enviando inativação do beneficiário {} (cdAssociado: {}) para OdontoPrev",
                    codigoMatricula, cdAssociado);

            odontoprevClient.inativarBeneficiario(
                    token,
                    token,
                    empresarialModelJson
            );

            // Etapa 7: Atualização do status no banco
            atualizarStatusSucesso(beneficiario, controleSync);

            log.info("Inativação do beneficiário {} processada com sucesso", codigoMatricula);

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
     *     "idMotivo": "7"
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
                .idMotivo(String.valueOf(beneficiario.getIdMotivoInativacao()))
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
     * CRIA REGISTRO DE CONTROLE DE SINCRONIZAÇÃO
     */
    @MonitorarOperacao(
            operacao = "CRIAR_REGISTRO_CONTROLE_INATIVACAO",
            excecaoEmErro = PROCESSAMENTO_BENEFICIARIO
    )
    private ControleSyncBeneficiario criarRegistroControle(
            BeneficiarioOdontoprev beneficiario,
            Object payload) {

        try {
            String payloadJson = objectMapper.writeValueAsString(payload);

            ControleSyncBeneficiario controle = ControleSyncBeneficiario.builder()
                    .codigoEmpresa(beneficiario.getCodigoEmpresa())
                    .codigoBeneficiario(beneficiario.getCodigoMatricula())
                    .tipoLog("E")
                    .tipoOperacao("EXCLUSAO")
                    .endpointDestino("/inativarAssociadoEmpresarial")
                    .dadosJson(payloadJson)
                    .statusSync("PENDING")
                    .tentativas(1)
                    .maxTentativas(3)
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
     * ATUALIZA STATUS DO BENEFICIÁRIO PARA SUCESSO
     */
    private void atualizarStatusSucesso(BeneficiarioOdontoprev beneficiario, ControleSyncBeneficiario controle) {
        try {

            if (controle != null) {
                controleSyncRepository.marcarComoSucesso(
                        controle.getId(),
                        LocalDateTime.now(),
                        "Inativação realizada com sucesso"
                );
            }

            log.debug("Status atualizado para SINCRONIZADO - Beneficiário: {}",
                    beneficiario.getCodigoMatricula());
        } catch (Exception e) {
            log.error("Erro ao atualizar status de sucesso: {}", e.getMessage(), e);
        }
    }

    /**
     * ATUALIZA STATUS DO BENEFICIÁRIO PARA ERRO
     */
    private void atualizarStatusErro(BeneficiarioOdontoprev beneficiario, String mensagemErro, ControleSyncBeneficiario controle) {
        try {
            beneficiarioRepository.atualizarStatusErro(
                    beneficiario.getId(),
                    "ERRO",
                    mensagemErro,
                    LocalDateTime.now()
            );

            if (controle != null) {
                controleSyncRepository.atualizarAposTentativa(
                        controle.getId(),
                        "ERROR",
                        controle.getTentativas() + 1,
                        LocalDateTime.now(),
                        mensagemErro,
                        null
                );
            }

            log.debug("Status atualizado para ERRO - Beneficiário: {}, Mensagem: {}",
                    beneficiario.getCodigoMatricula(), mensagemErro);
        } catch (Exception e) {
            log.error("Erro ao atualizar status de erro: {}", e.getMessage(), e);
        }
    }
}
