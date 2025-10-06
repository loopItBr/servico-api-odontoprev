package com.odontoPrev.odontoPrev.infrastructure.client.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.odontoPrev.odontoPrev.domain.entity.BeneficiarioOdontoprev;
import com.odontoPrev.odontoPrev.domain.entity.ControleSyncBeneficiario;
import com.odontoPrev.odontoPrev.domain.repository.BeneficiarioOdontoprevRepository;
import com.odontoPrev.odontoPrev.domain.repository.ControleSyncBeneficiarioRepository;
import com.odontoPrev.odontoPrev.domain.service.ProcessamentoBeneficiarioAlteracaoService;
import com.odontoPrev.odontoPrev.infrastructure.aop.MonitorarOperacao;
import com.odontoPrev.odontoPrev.infrastructure.client.adapter.out.BeneficiarioOdontoprevFeignClient;
import com.odontoPrev.odontoPrev.infrastructure.client.adapter.out.dto.BeneficiarioAlteracaoRequest;
import com.odontoPrev.odontoPrev.infrastructure.client.domain.service.TokenService;
import com.odontoPrev.odontoPrev.infrastructure.exception.ProcessamentoBeneficiarioException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;

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
    private final BeneficiarioOdontoprevRepository beneficiarioRepository;
    private final ControleSyncBeneficiarioRepository controleSyncRepository;
    private final OdontoprevApiHeaderService headerService;
    private final TokenService tokenService;
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
            BeneficiarioAlteracaoRequest request = converterParaAlteracaoRequest(beneficiario);

            // Etapa 3: Criar registro de controle
            controleSync = criarRegistroControle(beneficiario, request);

            // Etapa 4: Chamada para API da OdontoPrev
            log.info("Enviando alteração do beneficiário {} (cdAssociado: {}) para OdontoPrev",
                    codigoMatricula, cdAssociado);

            String token = tokenService.obterTokenValido();
            String authorization = "Bearer " + token;

            odontoprevClient.alterarBeneficiario(
                    authorization,
                    headerService.getEmpresa(),
                    headerService.getUsuario(),
                    headerService.getSenha(),
                    headerService.getAppId(),
                    request
            );

            // Etapa 5: Atualização do status no banco
            atualizarStatusSucesso(beneficiario, controleSync);

            log.info("Alteração do beneficiário {} processada com sucesso", codigoMatricula);

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
     * CONVERTE ENTIDADE PARA DTO DE ALTERAÇÃO
     *
     * Mapeia campos da entidade para formato esperado pela API de alteração.
     * Inclui todos os campos que podem ser alterados.
     */
    private BeneficiarioAlteracaoRequest converterParaAlteracaoRequest(BeneficiarioOdontoprev beneficiario) {
        return BeneficiarioAlteracaoRequest.builder()
                // Campos obrigatórios para alteração
                .cdEmpresa(beneficiario.getCodigoEmpresa())
                .cdAssociado(beneficiario.getCdAssociado())
                .codigoPlano(beneficiario.getCodigoPlano())
                .departamento(beneficiario.getDepartamento())
                // Campos opcionais que podem ser alterados
                .dtVigenciaRetroativa(beneficiario.getDtVigenciaRetroativa() != null ?
                        beneficiario.getDtVigenciaRetroativa().toString() : null)
                .dataNascimento(beneficiario.getDataNascimento() != null ?
                        beneficiario.getDataNascimento().toString() : null)
                .telefoneCelular(beneficiario.getTelefoneCelular())
                .telefoneResidencial(beneficiario.getTelefoneResidencial())
                .rg(beneficiario.getRg())
                .estadoCivil(beneficiario.getEstadoCivil())
                .nmCargo(beneficiario.getNmCargo())
                .cpf(beneficiario.getCpf())
                .sexo(beneficiario.getSexo())
                .nomeDaMae(beneficiario.getNomeMae())
                .pisPasep(beneficiario.getPisPasep())
                .bairro(beneficiario.getBairro())
                .email(beneficiario.getEmail())
                .rgEmissor(beneficiario.getRgEmissor())
                .nomeBeneficiario(beneficiario.getNomeBeneficiario())
                .cns(beneficiario.getCns())
                .cep(beneficiario.getCep())
                .cidade(beneficiario.getCidade())
                .logradouro(beneficiario.getLogradouro())
                .numero(beneficiario.getNumero())
                .complemento(beneficiario.getComplemento())
                .uf(beneficiario.getUf())
                .build();
    }

    /**
     * CRIA REGISTRO DE CONTROLE DE SINCRONIZAÇÃO
     */
    @MonitorarOperacao(
            operacao = "CRIAR_REGISTRO_CONTROLE_ALTERACAO",
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
                    .tipoLog("A")
                    .tipoOperacao("ALTERACAO")
                    .endpointDestino("/alterar")
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
            beneficiarioRepository.atualizarStatusErro(
                    beneficiario.getId(),
                    "SINCRONIZADO",
                    null,
                    LocalDateTime.now()
            );

            if (controle != null) {
                controleSyncRepository.marcarComoSucesso(
                        controle.getId(),
                        LocalDateTime.now(),
                        "Alteração realizada com sucesso"
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
