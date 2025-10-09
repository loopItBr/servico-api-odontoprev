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

            // Etapa 3: Criar registro de controle
            controleSync = criarRegistroControle(beneficiario, request);
            if (controleSync == null) {
                String mensagem = "Falha ao criar registro de controle para beneficiário " + codigoMatricula;
                log.error("❌ [ALTERAÇÃO] {}", mensagem);
                throw new ProcessamentoBeneficiarioException(mensagem, codigoMatricula, ALTERACAO);
            }
            log.info("📝 [CONTROLE] Registro de controle criado - ID: {}, Status: {}", 
                    controleSync.getId(), controleSync.getStatusSync());

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
                    request
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
     * Inclui todos os campos que podem ser alterados.
     * 
     * IMPORTANTE: Busca CPF e tpEndereco da view de inclusão, pois a view de alteração não tem esses campos.
     */
    private BeneficiarioAlteracaoRequestNew converterParaAlteracaoRequestNew(BeneficiarioOdontoprev beneficiario) {
        // Buscar CPF e tpEndereco da view de inclusão usando o cdAssociado
        String cpf = null;
        String tpEndereco = null;
        
        if (beneficiario.getCdAssociado() != null) {
            try {
                // Buscar na view de inclusão usando cdAssociado como codigoMatricula
                IntegracaoOdontoprevBeneficiario beneficiarioInclusao = 
                    integracaoOdontoprevBeneficiarioRepository.findByCodigoMatricula(beneficiario.getCdAssociado());
                
                if (beneficiarioInclusao != null) {
                    cpf = beneficiarioInclusao.getCpf();
                    tpEndereco = beneficiarioInclusao.getTpEndereco();
                    log.debug("✅ CPF e tpEndereco obtidos da view de inclusão - CPF: {}, tpEndereco: {}", cpf, tpEndereco);
                } else {
                    log.warn("⚠️ Beneficiário não encontrado na view de inclusão para cdAssociado: {}", beneficiario.getCdAssociado());
                }
            } catch (Exception e) {
                log.error("❌ Erro ao buscar CPF e tpEndereco da view de inclusão: {}", e.getMessage());
            }
        }
        // Criar objeto Endereco com os dados
        BeneficiarioAlteracaoRequestNew.Endereco endereco = BeneficiarioAlteracaoRequestNew.Endereco.builder()
                .cep(beneficiario.getCep())
                .logradouro(beneficiario.getLogradouro())
                .numero(beneficiario.getNumero())
                .complemento(beneficiario.getComplemento())
                .bairro(beneficiario.getBairro())
                .cidade(beneficiario.getCidade())
                .uf(beneficiario.getUf())
                .tpEndereco(tpEndereco) // Usar tpEndereco obtido da view de inclusão
                .build();

        // Criar objeto Beneficiario com os dados
        BeneficiarioAlteracaoRequestNew.Beneficiario beneficiarioData = BeneficiarioAlteracaoRequestNew.Beneficiario.builder()
                .codigoMatricula(beneficiario.getCodigoMatricula())
                .codigoPlano(beneficiario.getCodigoPlano())
                .cpf(cpf) // Usar CPF obtido da view de inclusão
                .dataDeNascimento(beneficiario.getDataNascimento() != null ?
                        beneficiario.getDataNascimento().toString() : null)
                .dtVigenciaRetroativa(beneficiario.getDtVigenciaRetroativa() != null ?
                        beneficiario.getDtVigenciaRetroativa().toString() : null)
                .nomeBeneficiario(beneficiario.getNomeBeneficiario())
                .nomeDaMae(beneficiario.getNomeMae())
                .sexo(beneficiario.getSexo())
                .telefoneCelular(beneficiario.getTelefoneCelular())
                .telefoneResidencial(beneficiario.getTelefoneResidencial())
                .rg(beneficiario.getRg())
                .rgEmissor(beneficiario.getRgEmissor())
                .estadoCivil(beneficiario.getEstadoCivil())
                .nmCargo(beneficiario.getNmCargo())
                .pisPasep(beneficiario.getPisPasep())
                .email(beneficiario.getEmail())
                .endereco(endereco)
                .build();

        return BeneficiarioAlteracaoRequestNew.builder()
                // Campos obrigatórios para alteração
                .cdEmpresa(beneficiario.getCodigoEmpresa())
                .codigoAssociado(beneficiario.getCdAssociado())
                .codigoPlano(beneficiario.getCodigoPlano())
                .departamento(beneficiario.getDepartamento())
                // Dados do beneficiário
                .beneficiario(beneficiarioData)
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
                    .endpointDestino("/cadastroonline-pj/1.0/alterar")
                    .dadosJson(payloadJson)
                    .statusSync("PROCESSANDO")
                    .tentativas(0)
                    .maxTentativas(3)
                    .dataUltimaTentativa(LocalDateTime.now())
                    .build();

            ControleSyncBeneficiario controleSalvo = controleSyncRepository.save(controle);
            log.info("✅ [CONTROLE] Registro de controle criado com sucesso para beneficiário {} - ID: {}", 
                    beneficiario.getCodigoMatricula(), controleSalvo.getId());
            return controleSalvo;

        } catch (Exception e) {
            log.error("❌ [CONTROLE] Erro ao criar registro de controle para beneficiário {}: {}",
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
