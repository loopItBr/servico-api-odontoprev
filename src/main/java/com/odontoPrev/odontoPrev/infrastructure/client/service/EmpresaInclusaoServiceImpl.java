package com.odontoPrev.odontoPrev.infrastructure.client.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.odontoPrev.odontoPrev.infrastructure.client.adapter.out.BeneficiarioOdontoprevFeignClient;
import com.odontoPrev.odontoPrev.infrastructure.client.adapter.out.dto.EmpresaAtivacaoPlanoRequest;
import com.odontoPrev.odontoPrev.infrastructure.client.adapter.out.dto.EmpresaAtivacaoPlanoResponse;
import com.odontoPrev.odontoPrev.infrastructure.client.domain.service.TokenService;
import com.odontoPrev.odontoPrev.infrastructure.repository.IntegracaoOdontoprevRepository;
import com.odontoPrev.odontoPrev.infrastructure.repository.ControleSyncRepository;
import com.odontoPrev.odontoPrev.infrastructure.repository.entity.ControleSync;
import com.odontoPrev.odontoPrev.infrastructure.repository.entity.IntegracaoOdontoprev;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.CallableStatementCallback;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmpresaInclusaoServiceImpl {

    private final IntegracaoOdontoprevRepository integracaoRepository;
    private final BeneficiarioOdontoprevFeignClient feignClient;
    private final TokenService tokenService;
    private final ObjectMapper objectMapper;
    private final ControleSyncRepository controleSyncRepository;
    private final JdbcTemplate jdbcTemplate;

    @Value("${odontoprev.api.codigo-grupo-gerencial:787392}")
    private String codigoGrupoGerencialPadrao;

    @PostConstruct
    public void init() {
        log.info("✅ [INICIALIZAÇÃO] EmpresaInclusaoServiceImpl - codigoGrupoGerencialPadrao configurado: {}", codigoGrupoGerencialPadrao);
    }

    /**
     * Fluxo de inclusão de empresa:
     * 1) Ler dados da view (VW_INTEGRACAO_ODONTOPREV)
     * 2) Montar e enviar POST /empresa/2.0/empresas/contrato/empresarial
     * 3) Receber codigoEmpresa e senha da resposta
     * 4) Executar procedure TASY.SS_PLS_CAD_CODEMPRESA_ODONTOPREV(nrSequencia, codigoEmpresa)
     * 5) Registrar controle com codigoEmpresa retornado
     * 6) O GET-API deve ocorrer em ação separada posterior
     */
    public EmpresaAtivacaoPlanoResponse incluirEmpresa(String codigoEmpresaOrigem, Long nrSequencia) {
        log.info("🚀 [INCLUSAO EMPRESA] Iniciando inclusão empresarial para '{}', nrSequencia={}", codigoEmpresaOrigem, nrSequencia);

        // 1) Buscar dados na view
        // Converte String para Long (nrSeqContrato)
        Long nrSeqContrato = Long.valueOf(codigoEmpresaOrigem);
        Optional<IntegracaoOdontoprev> opt = integracaoRepository.buscarPrimeiroDadoPorCodigoEmpresa(nrSeqContrato);
        if (opt.isEmpty()) {
            throw new IllegalStateException("Dados da empresa não encontrados na view para código: " + codigoEmpresaOrigem);
        }
        IntegracaoOdontoprev dadosEmpresa = opt.get();

        // 2) Converter para request do endpoint empresarial
        EmpresaAtivacaoPlanoRequest request = converterParaRequestEmpresarial(dadosEmpresa);

        // 3) Criar controle PENDING com payload (usando codigoEmpresa de origem)
        ControleSync controle = criarControleInclusaoPendente(codigoEmpresaOrigem, request);

        try {
            // 4) Token OAuth2
            log.info("🔑 [INCLUSAO EMPRESA] Obtendo token OAuth2 para empresa: {}", codigoEmpresaOrigem);
            String token = tokenService.obterTokenValido();
            String authorization = "Bearer " + token;
            log.info("✅ [INCLUSAO EMPRESA] Token OAuth2 obtido com sucesso para empresa: {}", codigoEmpresaOrigem);

            // 5) POST empresarial - LOGS DETALHADOS
            log.info("📤 [INCLUSAO EMPRESA] ===== INICIANDO CHAMADA POST =====");
            log.info("📤 [INCLUSAO EMPRESA] Endpoint: POST {{baseUrl}}/empresa/2.0/empresas/contrato/empresarial");
            log.info("📤 [INCLUSAO EMPRESA] Authorization: {}", authorization.substring(0, Math.min(20, authorization.length())) + "...");
            log.info("📤 [INCLUSAO EMPRESA] Request payload: {}", objectMapper.writeValueAsString(request));
            log.info("📤 [INCLUSAO EMPRESA] Empresa origem: {}", codigoEmpresaOrigem);
            log.info("📤 [INCLUSAO EMPRESA] NR_SEQUENCIA: {}", nrSequencia);
            
            long startTime = System.currentTimeMillis();
            log.info("⏰ [INCLUSAO EMPRESA] Iniciando chamada POST às {}", java.time.LocalDateTime.now());
            
            EmpresaAtivacaoPlanoResponse response = feignClient.ativarPlanoEmpresa(authorization, request);
            
            long endTime = System.currentTimeMillis();
            long duration = endTime - startTime;
            log.info("⏰ [INCLUSAO EMPRESA] Chamada POST finalizada às {} (duração: {}ms)", java.time.LocalDateTime.now(), duration);
            log.info("📥 [INCLUSAO EMPRESA] ===== RESPOSTA DO POST =====");
            log.info("📥 [INCLUSAO EMPRESA] Status da resposta: {}", response != null ? "SUCESSO" : "NULL");
            
            if (response != null) {
                log.info("📥 [INCLUSAO EMPRESA] Código da empresa retornado: '{}'", response.getCodigoEmpresa());
                log.info("📥 [INCLUSAO EMPRESA] Senha retornada: '{}'", response.getSenha());
                log.info("📥 [INCLUSAO EMPRESA] Response completa: {}", objectMapper.writeValueAsString(response));
            } else {
                log.warn("⚠️ [INCLUSAO EMPRESA] ATENÇÃO: Response é NULL!");
            }
            log.info("📥 [INCLUSAO EMPRESA] ===== FIM DA RESPOSTA =====");

            // 6) Executar procedure com NR_SEQUENCIA e codigoEmpresa retornado
            if (nrSequencia == null) {
                log.warn("⚠️ [INCLUSAO EMPRESA] NR_SEQUENCIA não informado; pulando execução da procedure");
            } else if (response != null && response.getCodigoEmpresa() != null) {
                log.info("🔧 [INCLUSAO EMPRESA] Condições atendidas para executar procedure - nrSequencia: {}, codigoEmpresa: '{}'", 
                        nrSequencia, response.getCodigoEmpresa());
                
                log.info("🚀 [INCLUSAO EMPRESA] Chamando procedure SS_PLS_CAD_CODEMPRESA_ODONTOPREV para empresa {}", codigoEmpresaOrigem);
                executarProcedureAtualizarCodigoEmpresa(nrSequencia, response.getCodigoEmpresa());
                log.info("✅ [INCLUSAO EMPRESA] Procedure executada com sucesso para empresa {}", codigoEmpresaOrigem);
            } else {
                log.warn("⚠️ [INCLUSAO EMPRESA] Condições NÃO atendidas para executar procedure");
                log.warn("📊 [INCLUSAO EMPRESA] nrSequencia: {}, response: {}, codigoEmpresa: '{}'", 
                        nrSequencia, response, response != null ? response.getCodigoEmpresa() : null);
                
                if (response == null) {
                    log.warn("⚠️ [INCLUSAO EMPRESA] Response é nulo - procedure não será executada");
                } else if (response.getCodigoEmpresa() == null) {
                    log.warn("⚠️ [INCLUSAO EMPRESA] codigoEmpresa é nulo na response - procedure não será executada");
                }
            }

            // 7) Atualizar controle como SUCCESS usando codigoEmpresa retornado
            processarSucessoControle(controle, response);

            log.info("✅ [INCLUSAO EMPRESA] Inclusão empresarial concluída para '{}', codigoEmpresaRetornado={}",
                    codigoEmpresaOrigem, response != null ? response.getCodigoEmpresa() : null);
            return response;

        } catch (Exception e) {
            // 8) Atualizar controle como ERROR - LOGS DETALHADOS DO ERRO
            log.error("❌ [INCLUSAO EMPRESA] ===== ERRO NA CHAMADA POST =====");
            log.error("❌ [INCLUSAO EMPRESA] Empresa: {}", codigoEmpresaOrigem);
            log.error("❌ [INCLUSAO EMPRESA] NR_SEQUENCIA: {}", nrSequencia);
            log.error("❌ [INCLUSAO EMPRESA] Tipo do erro: {}", e.getClass().getSimpleName());
            log.error("❌ [INCLUSAO EMPRESA] Mensagem do erro: {}", e.getMessage());
            log.error("❌ [INCLUSAO EMPRESA] Stack trace completo:", e);
            log.error("❌ [INCLUSAO EMPRESA] ===== FIM DO ERRO =====");
            
            // Verificar se é erro de autenticação
            if (e.getMessage() != null && e.getMessage().contains("401")) {
                log.error("🔐 [INCLUSAO EMPRESA] ERRO DE AUTENTICAÇÃO (401) - Token pode estar inválido ou expirado");
            } else if (e.getMessage() != null && e.getMessage().contains("403")) {
                log.error("🚫 [INCLUSAO EMPRESA] ERRO DE AUTORIZAÇÃO (403) - Sem permissão para acessar o endpoint");
            } else if (e.getMessage() != null && e.getMessage().contains("404")) {
                log.error("🔍 [INCLUSAO EMPRESA] ERRO DE ENDPOINT (404) - Endpoint não encontrado");
            } else if (e.getMessage() != null && e.getMessage().contains("500")) {
                log.error("💥 [INCLUSAO EMPRESA] ERRO INTERNO DO SERVIDOR (500) - Problema na OdontoPrev");
            } else if (e.getMessage() != null && e.getMessage().contains("timeout")) {
                log.error("⏰ [INCLUSAO EMPRESA] ERRO DE TIMEOUT - Chamada demorou muito para responder");
            }
            
            // Atualizar controle como ERROR
            processarErroControle(controle, e.getMessage());
            throw new RuntimeException("Falha na inclusão empresarial: " + e.getMessage(), e);
        }
    }

    /**
     * EXECUÇÃO DA PROCEDURE SS_PLS_CAD_CODEMPRESA_ODONTOPREV
     * 
     * Executa a procedure no banco Tasy para registrar o codigoEmpresa retornado pela OdontoPrev.
     * Esta procedure é responsável por atualizar o campo codigoEmpresa na tabela base da view.
     */
    private void executarProcedureAtualizarCodigoEmpresa(Long nrSequencia, String codigoEmpresaApi) {
        log.info("🚀 [INCLUSAO EMPRESA] Iniciando execução da procedure SS_PLS_CAD_CODEMPRESA_ODONTOPREV");
        log.info("📋 [INCLUSAO EMPRESA] Parâmetros - nrSequencia: {}, codigoEmpresaApi: '{}'", 
                nrSequencia, codigoEmpresaApi);
        
        // Validações dos parâmetros
        if (nrSequencia == null) {
            log.error("❌ [INCLUSAO EMPRESA] VALIDAÇÃO FALHOU - nrSequencia é nulo");
            throw new IllegalArgumentException("nrSequencia não pode ser nulo");
        }
        
        if (codigoEmpresaApi == null || codigoEmpresaApi.trim().isEmpty()) {
            log.error("❌ [INCLUSAO EMPRESA] VALIDAÇÃO FALHOU - codigoEmpresaApi é nulo ou vazio");
            throw new IllegalArgumentException("codigoEmpresaApi não pode ser nulo ou vazio");
        }
        
        log.info("✅ [INCLUSAO EMPRESA] Validações passaram - todos os parâmetros são válidos");
        
        try {
            String sql = "{ call TASY.SS_PLS_CAD_CODEMPRESA_ODONTOPREV(?, ?) }";
            log.info("🔧 [INCLUSAO EMPRESA] SQL da procedure: {}", sql);
            
            log.info("🔄 [INCLUSAO EMPRESA] Executando CallableStatement...");
            
            jdbcTemplate.execute(sql, (CallableStatementCallback<Void>) cs -> {
                log.info("🔗 [INCLUSAO EMPRESA] Conexão obtida - criando CallableStatement");
                
                // Configurar os parâmetros IN
                cs.setLong(1, nrSequencia); // p_nr_sequencia as NUMBER
                cs.setString(2, codigoEmpresaApi); // p_codigo_empresa as VARCHAR2
                
                log.info("📝 [INCLUSAO EMPRESA] Parâmetros setados - p_nr_sequencia={}, p_codigo_empresa='{}'", 
                        nrSequencia, codigoEmpresaApi);
                
                log.info("⚡ [INCLUSAO EMPRESA] Executando cs.execute()...");
                boolean result = cs.execute();
                log.info("✅ [INCLUSAO EMPRESA] cs.execute() retornou: {}", result);
                
                return null;
            });
            
            log.info("✅ [INCLUSAO EMPRESA] Procedure SS_PLS_CAD_CODEMPRESA_ODONTOPREV executada com sucesso");
            log.info("🎯 [INCLUSAO EMPRESA] Empresa {} agora deve ter codigoEmpresa atualizado na view", nrSequencia);
            
        } catch (Exception e) {
            log.error("❌ [INCLUSAO EMPRESA] Erro ao executar procedure SS_PLS_CAD_CODEMPRESA_ODONTOPREV: {}", e.getMessage(), e);
            log.error("📊 [INCLUSAO EMPRESA] Detalhes do erro - nrSequencia: {}, codigoEmpresaApi: '{}'", 
                    nrSequencia, codigoEmpresaApi);
            
            // Cadastrar erro na TBSYNC (tabela de controle)
            cadastrarErroProcedureTBSync(nrSequencia, codigoEmpresaApi, e.getMessage());
            
            throw new RuntimeException("Falha na execução da procedure SS_PLS_CAD_CODEMPRESA_ODONTOPREV: " + e.getMessage(), e);
        }
    }
    
    /**
     * CADASTRA ERRO DA PROCEDURE NA TBSYNC
     * 
     * Registra o erro da procedure na tabela de controle para auditoria e rastreamento.
     */
    private void cadastrarErroProcedureTBSync(Long nrSequencia, String codigoEmpresaApi, String mensagemErro) {
        try {
            log.info("📝 [TBSYNC] Cadastrando erro da procedure na tabela de controle");
            
            // Criar registro de controle com erro
            ControleSync controleErro = ControleSync.builder()
                    .codigoEmpresa(String.valueOf(nrSequencia))
                    .tipoOperacao(ControleSync.TipoOperacao.CREATE)
                    .tipoControle(ControleSync.TipoControle.ADICAO.getCodigo())
                    .endpointDestino("PROCEDURE_SS_PLS_CAD_CODEMPRESA_ODONTOPREV")
                    .dadosJson(String.format("{\"nrSequencia\":%d,\"codigoEmpresaApi\":\"%s\"}", 
                            nrSequencia, codigoEmpresaApi))
                    .statusSync(ControleSync.StatusSync.ERROR)
                    .erroMensagem("ERRO_PROCEDURE: " + mensagemErro)
                    .dataCriacao(java.time.LocalDateTime.now())
                    .build();
            
            // Salvar na tabela de controle
            ControleSync controleSalvo = controleSyncRepository.save(controleErro);
            log.info("💾 [TBSYNC] Erro da procedure cadastrado na TBSYNC com ID: {} para empresa {}", 
                    controleSalvo.getId(), nrSequencia);
            
        } catch (Exception e) {
            log.error("❌ [TBSYNC] Erro ao cadastrar erro da procedure na TBSYNC: {}", e.getMessage(), e);
        }
    }

    private ControleSync criarControleInclusaoPendente(String codigoEmpresaOrigem, EmpresaAtivacaoPlanoRequest request) {
        try {
            String payloadJson = objectMapper.writeValueAsString(request);
            ControleSync controle = ControleSync.builder()
                    .codigoEmpresa(codigoEmpresaOrigem)
                    .tipoOperacao(ControleSync.TipoOperacao.CREATE)
                    .tipoControle(ControleSync.TipoControle.ADICAO.getCodigo())
                    .endpointDestino("/empresa/2.0/empresas/contrato/empresarial")
                    .dadosJson(payloadJson)
                    .statusSync(ControleSync.StatusSync.PENDING)
                    .dataCriacao(LocalDateTime.now())
                    .build();
            return controleSyncRepository.save(controle);
        } catch (Exception e) {
            log.error("❌ [CONTROLE] Erro ao criar controle de inclusão: {}", e.getMessage(), e);
            return null;
        }
    }

    private void processarSucessoControle(ControleSync controle, EmpresaAtivacaoPlanoResponse response) {
        if (controle == null) return;
        try {
            String responseJson = objectMapper.writeValueAsString(response);
            // Ajustar o codigoEmpresa do controle para o retornado pela API, se existir
            if (response != null && response.getCodigoEmpresa() != null) {
                controle.setCodigoEmpresa(response.getCodigoEmpresa());
            }
            controle.setStatusSync(ControleSync.StatusSync.SUCCESS);
            controle.setResponseApi(responseJson);
            controle.setDataSucesso(LocalDateTime.now());
            controleSyncRepository.save(controle);
        } catch (Exception e) {
            log.error("❌ [CONTROLE] Erro ao processar sucesso da inclusão: {}", e.getMessage(), e);
        }
    }

    private void processarErroControle(ControleSync controle, String mensagemErro) {
        if (controle == null) return;
        try {
            controle.setStatusSync(ControleSync.StatusSync.ERROR);
            controle.setErroMensagem(mensagemErro);
            controleSyncRepository.save(controle);
        } catch (Exception e) {
            log.error("❌ [CONTROLE] Erro ao registrar erro da inclusão: {}", e.getMessage(), e);
        }
    }

    // Conversão baseada no fluxo já existente de ativação
    private EmpresaAtivacaoPlanoRequest converterParaRequestEmpresarial(IntegracaoOdontoprev dadosEmpresa) {
        // Determinar codigoGrupoGerencial: usar da view se disponível, senão usar o padrão configurado
        String codigoGrupoGerencial = dadosEmpresa.getCodigoGrupoGerencial() != null 
                ? dadosEmpresa.getCodigoGrupoGerencial().toString() 
                : codigoGrupoGerencialPadrao;
        
        log.debug("📋 [INCLUSAO EMPRESA] codigoGrupoGerencial - View: {}, Usando: {}, Padrão configurado: {}", 
                dadosEmpresa.getCodigoGrupoGerencial(), codigoGrupoGerencial, codigoGrupoGerencialPadrao);
        
        EmpresaAtivacaoPlanoRequest request = EmpresaAtivacaoPlanoRequest.builder()
                .sistema(dadosEmpresa.getSistema() != null ? dadosEmpresa.getSistema() : "SabinSinai")
                .tipoPessoa(dadosEmpresa.getTipoPessoa() != null ? dadosEmpresa.getTipoPessoa() : "J")
                .emiteCarteirinhaPlastica(dadosEmpresa.getEmiteCarteirinhaPlastica() != null ? dadosEmpresa.getEmiteCarteirinhaPlastica() : "N")
                .codigoEmpresaGestora(dadosEmpresa.getCodigoEmpresaGestora() != null ? dadosEmpresa.getCodigoEmpresaGestora().intValue() : 1)
                .codigoFilialEmpresaGestora(dadosEmpresa.getCodigoFilialEmpresaGestora() != null ? dadosEmpresa.getCodigoFilialEmpresaGestora().intValue() : 1)
                .codigoGrupoGerencial(codigoGrupoGerencial)
                .codigoNaturezaJuridica(dadosEmpresa.getCodigoNaturezaJuridica() != null ? dadosEmpresa.getCodigoNaturezaJuridica() : "6550-2")
                .nomeNaturezaJuridica(dadosEmpresa.getNomeNaturezaJuridica() != null ? dadosEmpresa.getNomeNaturezaJuridica() : "Planos de saúde")
                .situacaoCadastral(dadosEmpresa.getSituacaoCadastral() != null ? dadosEmpresa.getSituacaoCadastral() : "ATIVO")
                .inscricaoMunicipal(dadosEmpresa.getInscricaoMunicipal() != null ? dadosEmpresa.getInscricaoMunicipal() : "997.179.737.204")
                .inscricaoEstadual(dadosEmpresa.getInscricaoEstadual() != null ? dadosEmpresa.getInscricaoEstadual() : "997.179.737.204")
                .dataConstituicao(dadosEmpresa.getDataConstituicao() != null ? dadosEmpresa.getDataConstituicao() : "2025-10-01T00:00:00.000Z")
                .renovacaoAutomatica(dadosEmpresa.getRenovacaoAutomatica() != null ? dadosEmpresa.getRenovacaoAutomatica() : "S")
                .codigoClausulaReajusteDiferenciado(dadosEmpresa.getCodigoClausulaReajusteDiferenciado() != null ? dadosEmpresa.getCodigoClausulaReajusteDiferenciado().toString() : "1")
                .departamento(dadosEmpresa.getDepartamento() != null ? dadosEmpresa.getDepartamento().toString() : "SEM DEPARTAMENTO")
                .dependentePaga(dadosEmpresa.getDependentePaga() != null ? dadosEmpresa.getDependentePaga() : "N")
                .permissaoCadastroDep(dadosEmpresa.getPermissaoCadastroDep() != null ? dadosEmpresa.getPermissaoCadastroDep().equals("S") : true)
                .modeloCobrancaVarejo(false)
                .numeroMinimoAssociados(dadosEmpresa.getNumeroMinimoAssociados() != null ? dadosEmpresa.getNumeroMinimoAssociados().intValue() : 3)
                .numeroFuncionarios(dadosEmpresa.getNumeroFuncionarios() != null ? dadosEmpresa.getNumeroFuncionarios().intValue() : 0)
                .numeroDepedentes(dadosEmpresa.getNumeroDependentes() != null ? dadosEmpresa.getNumeroDependentes().intValue() : 0)
                .idadeLimiteDependente(dadosEmpresa.getIdadeLimiteDependente() != null ? dadosEmpresa.getIdadeLimiteDependente().intValue() : 21)
                .valorFator(dadosEmpresa.getValorFator() != null ? dadosEmpresa.getValorFator().intValue() : 1)
                .tipoRetornoCritica("T")
                .codigoLayoutCarteirinha(dadosEmpresa.getCodigoLayoutCarteirinha() != null ? dadosEmpresa.getCodigoLayoutCarteirinha() : "B")
                .codigoOrdemCarteira(dadosEmpresa.getCodigoOrdemCarteira() != null ? dadosEmpresa.getCodigoOrdemCarteira().intValue() : 3)
                .codigoDocumentoContrato(0)
                .codigoCelula(dadosEmpresa.getCodigoCelula() != null ? dadosEmpresa.getCodigoCelula().intValue() : 9)
                .codigoMarca(dadosEmpresa.getCodigoMarca() != null ? dadosEmpresa.getCodigoMarca().intValue() : 1)
                .codigoDescricaoNF(0)
                .diaVencimentoAg(dadosEmpresa.getDiaVencimentoAg() != null ? dadosEmpresa.getDiaVencimentoAg().intValue() : 19)
                .codigoPerfilClienteFatura(dadosEmpresa.getCodigoPerfilClienteFatura() != null ? dadosEmpresa.getCodigoPerfilClienteFatura().intValue() : 3)
                .codigoBancoFatura(dadosEmpresa.getCodigoBancoFatura() != null ? dadosEmpresa.getCodigoBancoFatura().toString().trim() + " " : "085 ")
                .multaFatura(dadosEmpresa.getMultaFatura() != null ? dadosEmpresa.getMultaFatura().intValue() : 0)
                .descontaIR(dadosEmpresa.getDescontaIr() != null ? dadosEmpresa.getDescontaIr() : "N")
                .retencaoIss(dadosEmpresa.getRetencaoIss() != null ? dadosEmpresa.getRetencaoIss() : "N")
                .liberaSenhaInternet(dadosEmpresa.getLiberaSenhaInternet() != null ? dadosEmpresa.getLiberaSenhaInternet() : "S")
                .faturamentoNotaCorte(dadosEmpresa.getFaturamentoNotaCorte() != null ? dadosEmpresa.getFaturamentoNotaCorte() : "N")
                .proRata(dadosEmpresa.getProrata() != null ? dadosEmpresa.getProrata() : "N")
                .custoFamiliar(dadosEmpresa.getCustoFamiliar() != null ? dadosEmpresa.getCustoFamiliar() : "S")
                .planoFamiliar(dadosEmpresa.getPlanoFamiliar() != null ? dadosEmpresa.getPlanoFamiliar() : "S")
                .percSinistroContrato(dadosEmpresa.getValorSinistroContrato() != null ? dadosEmpresa.getValorSinistroContrato().intValue() : 60)
                .idadeLimiteUniversitaria(dadosEmpresa.getIdadeLimiteUniversitario() != null ? dadosEmpresa.getIdadeLimiteUniversitario().intValue() : 24)
                .percentualINSSAutoGestao(0)
                .percentualMateriaisAutoGestao(0)
                .valorSinistroContrato(dadosEmpresa.getValorSinistroContrato() != null ? dadosEmpresa.getValorSinistroContrato().doubleValue() : 60.0)
                .percentualAssociado(0)
                .codigoRegiao(dadosEmpresa.getCodigoRegiao() != null ? dadosEmpresa.getCodigoRegiao().intValue() : 0)
                .codigoImagemFatura(dadosEmpresa.getCodigoImagemFatura() != null ? dadosEmpresa.getCodigoImagemFatura().intValue() : 1)
                .codigoMoeda(dadosEmpresa.getCodigoMoeda() != null ? dadosEmpresa.getCodigoMoeda().toString() : "7")
                .codigoParceriaEstrategica(0)
                .sinistralidade(60)
                .posicaoIniTIT(dadosEmpresa.getPosicaoInitTit() != null ? dadosEmpresa.getPosicaoInitTit().intValue() : 1)
                .posicaoFimTIT(dadosEmpresa.getPosicaoFimTit() != null ? dadosEmpresa.getPosicaoFimTit().intValue() : 7)
                .regraDowngrade(0)
                .mesCompetenciaProximoFaturamento("09")
                .codigoUsuarioFaturamento("")
                .codigoUsuarioCadastro("")
                .ramo("2")
                .cgc(dadosEmpresa.getCnpj())
                .razaoSocial(dadosEmpresa.getRazaoSocial() != null ? dadosEmpresa.getRazaoSocial() : dadosEmpresa.getNomeFantasia())
                .nomeFantasia(dadosEmpresa.getNomeFantasia())
                .diaInicioFaturamento(dadosEmpresa.getDiaInicioFaturamento() != null ? dadosEmpresa.getDiaInicioFaturamento().intValue() : 20)
                .codigoUsuarioConsultor(dadosEmpresa.getCodigoUsuarioConsultor() != null ? dadosEmpresa.getCodigoUsuarioConsultor().toString() : "0")
                .mesAniversarioReajuste(dadosEmpresa.getMesAniversarioReajuste() != null ? dadosEmpresa.getMesAniversarioReajuste().intValue() : 7)
                .dataInicioContrato(formatarDataInicioContrato(dadosEmpresa.getDataInicioContrato()))
                .dataVigencia(formatarDataVigencia(dadosEmpresa.getDataVigencia()))
                .descricaoRamoAtividade(dadosEmpresa.getDescricaoRamoAtividade() != null ? dadosEmpresa.getDescricaoRamoAtividade() : "Saúde Suplementar")
                .diaVencimento(dadosEmpresa.getDiaVencimento() != null ? dadosEmpresa.getDiaVencimento().intValue() : 15)
                .cnae(dadosEmpresa.getCnae() != null ? dadosEmpresa.getCnae() : "6550-2/00")
                .codigoManual(dadosEmpresa.getCodigoManual() != null ? dadosEmpresa.getCodigoManual().toString().trim() + " " : "1 ")
                .diaLimiteConsumoAg(19)
                .email(dadosEmpresa.getEmail() != null ? dadosEmpresa.getEmail() : "diretoria@sabinjf.com.br")
                .diaMovAssociadoEmpresa(dadosEmpresa.getDiaMovAssociadoEmpresa() != null ? dadosEmpresa.getDiaMovAssociadoEmpresa().intValue() : 15)
                .build();

        // Planos usando dados da view
        List<EmpresaAtivacaoPlanoRequest.Plano> planos = criarPlanos(dadosEmpresa);
        request.setPlanos(planos);

        // Endereço e cobrança (exemplo mínimo)
        EmpresaAtivacaoPlanoRequest.Endereco endereco = EmpresaAtivacaoPlanoRequest.Endereco.builder()
                .cep("36033318")
                .descricao("Av. Presidente Itamar Franco")
                .complemento("loja 202 E")
                .tipoLogradouro("2")
                .logradouro("Av. Presidente Itamar Franco")
                .numero("4001")
                .bairro("Cascatinha")
                .cidade(EmpresaAtivacaoPlanoRequest.Cidade.builder()
                        .codigo(3670)
                        .nome("Juiz de Fora")
                        .siglaUf("MG")
                        .codigoPais(1)
                        .build())
                .build();
        request.setEndereco(endereco);
        request.setCobranca(EmpresaAtivacaoPlanoRequest.Cobranca.builder()
                .nome(dadosEmpresa.getNomeFantasia())
                .cgc(dadosEmpresa.getCnpj())
                .endereco(endereco)
                .build());

        // Graus de parentesco mínimos
        request.setGrausParentesco(List.of(
                EmpresaAtivacaoPlanoRequest.GrauParentesco.builder().codigoGrauParentesco("1").build()
        ));

        // GRUPOS - Incluir grupo com codigoGrupo 109 conforme especificação
        request.setGrupos(List.of(
                EmpresaAtivacaoPlanoRequest.Grupo.builder()
                        .codigoGrupo(109)
                        .build()
        ));

        // CONTATOS - Usar dados da view
        request.setContatos(criarContatos(dadosEmpresa));

        // CONTATOS DA FATURA - Usar dados da view
        request.setContatosDaFatura(criarContatosDaFatura(dadosEmpresa));

        // COMISSIONAMENTOS - Usar dados da view
        List<EmpresaAtivacaoPlanoRequest.Comissionamento> comissionamentos = criarComissionamentos(dadosEmpresa);
        request.setComissionamentos(comissionamentos);

        return request;
    }

    /**
     * FORMATA DATA DE INÍCIO DO CONTRATO
     */
    private String formatarDataInicioContrato(String dataInicioContrato) {
        if (dataInicioContrato == null || dataInicioContrato.trim().isEmpty()) {
            return java.time.ZonedDateTime.now(java.time.ZoneId.of("America/Sao_Paulo"))
                    .format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS"));
        }
        try {
            // Se já está no formato correto, retorna como está
            if (dataInicioContrato.contains("T")) {
                return dataInicioContrato;
            }
            // Se é apenas data, adiciona horário
            return dataInicioContrato + "T00:00:00.000";
        } catch (Exception e) {
            log.warn("⚠️ [CONVERSÃO] Erro ao formatar dataInicioContrato '{}': {}", dataInicioContrato, e.getMessage());
            return java.time.ZonedDateTime.now(java.time.ZoneId.of("America/Sao_Paulo"))
                    .format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS"));
        }
    }

    /**
     * FORMATA DATA DE VIGÊNCIA
     */
    private String formatarDataVigencia(String dataVigencia) {
        if (dataVigencia == null || dataVigencia.trim().isEmpty()) {
            return java.time.ZonedDateTime.now(java.time.ZoneId.of("America/Sao_Paulo"))
                    .format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS"));
        }
        try {
            // Se já está no formato correto, retorna como está
            if (dataVigencia.contains("T")) {
                return dataVigencia;
            }
            // Se é apenas data, adiciona horário
            return dataVigencia + "T00:00:00.000";
        } catch (Exception e) {
            log.warn("⚠️ [CONVERSÃO] Erro ao formatar dataVigencia '{}': {}", dataVigencia, e.getMessage());
            return java.time.ZonedDateTime.now(java.time.ZoneId.of("America/Sao_Paulo"))
                    .format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS"));
        }
    }

    /**
     * CRIA COMISSIONAMENTOS USANDO DADOS DA VIEW
     */
    private List<EmpresaAtivacaoPlanoRequest.Comissionamento> criarComissionamentos(IntegracaoOdontoprev dadosEmpresa) {
        List<EmpresaAtivacaoPlanoRequest.Comissionamento> comissionamentos = new ArrayList<>();
        
        // Verificar se há dados de comissionamento na view
        if (dadosEmpresa.getCnpjCorretor() != null && !dadosEmpresa.getCnpjCorretor().trim().isEmpty()) {
            comissionamentos.add(EmpresaAtivacaoPlanoRequest.Comissionamento.builder()
                    .cnpjCorretor(dadosEmpresa.getCnpjCorretor())
                    .codigoRegra(dadosEmpresa.getCodigoRegra() != null ? dadosEmpresa.getCodigoRegra().intValue() : 1)
                    .numeroParcelaDe(dadosEmpresa.getNumeroParcelaDe() != null ? dadosEmpresa.getNumeroParcelaDe().intValue() : 1)
                    .numeroParcelaAte(dadosEmpresa.getNumeroParcelaAte() != null ? dadosEmpresa.getNumeroParcelaAte().intValue() : 12)
                    .porcentagem(dadosEmpresa.getPorcentagem() != null ? dadosEmpresa.getPorcentagem().intValue() : 0)
                    .build());
        } else {
            // Comissionamento padrão se não houver dados na view
            comissionamentos.add(EmpresaAtivacaoPlanoRequest.Comissionamento.builder()
                    .cnpjCorretor("00000000000000")
                    .codigoRegra(1)
                    .numeroParcelaDe(1)
                    .numeroParcelaAte(12)
                    .porcentagem(0)
                    .build());
        }
        
        return comissionamentos;
    }

    /**
     * CRIA CONTATOS USANDO DADOS DA VIEW
     */
    private List<EmpresaAtivacaoPlanoRequest.Contato> criarContatos(IntegracaoOdontoprev dadosEmpresa) {
        List<EmpresaAtivacaoPlanoRequest.Contato> contatos = new ArrayList<>();
        
        // Verificar se há dados de contato na view
        if (dadosEmpresa.getNomeContato() != null && !dadosEmpresa.getNomeContato().trim().isEmpty()) {
            contatos.add(EmpresaAtivacaoPlanoRequest.Contato.builder()
                    .cargo(dadosEmpresa.getCargoContato() != null ? dadosEmpresa.getCargoContato() : "Gerente")
                    .nome(dadosEmpresa.getNomeContato())
                    .email(dadosEmpresa.getEmailContato() != null ? dadosEmpresa.getEmailContato() : dadosEmpresa.getEmail())
                    .idCorretor("N")
                    .telefone(EmpresaAtivacaoPlanoRequest.Telefone.builder()
                            .telefone1("(32) 99999-9999")
                            .celular("(32) 99999-9999")
                            .build())
                    .listaTipoComunicacao(List.of(
                            EmpresaAtivacaoPlanoRequest.TipoComunicacao.builder()
                                    .id("1")
                                    .descricao("E-mail")
                                    .build()
                    ))
                    .build());
        } else {
            // Contato padrão se não houver dados na view
            contatos.add(EmpresaAtivacaoPlanoRequest.Contato.builder()
                    .cargo("Gerente")
                    .nome("Contato Principal")
                    .email("contato@empresa.com")
                    .idCorretor("N")
                    .telefone(EmpresaAtivacaoPlanoRequest.Telefone.builder()
                            .telefone1("(32) 99999-9999")
                            .celular("(32) 99999-9999")
                            .build())
                    .listaTipoComunicacao(List.of(
                            EmpresaAtivacaoPlanoRequest.TipoComunicacao.builder()
                                    .id("1")
                                    .descricao("E-mail")
                                    .build()
                    ))
                    .build());
        }
        
        return contatos;
    }

    /**
     * CRIA CONTATOS DA FATURA USANDO DADOS DA VIEW
     */
    private List<EmpresaAtivacaoPlanoRequest.ContatoFatura> criarContatosDaFatura(IntegracaoOdontoprev dadosEmpresa) {
        List<EmpresaAtivacaoPlanoRequest.ContatoFatura> contatosFatura = new ArrayList<>();
        
        // Verificar se há dados de contato da fatura na view
        if (dadosEmpresa.getNomeContatoFatura() != null && !dadosEmpresa.getNomeContatoFatura().trim().isEmpty()) {
            contatosFatura.add(EmpresaAtivacaoPlanoRequest.ContatoFatura.builder()
                    .codSequencial(dadosEmpresa.getCodSequencial() != null ? dadosEmpresa.getCodSequencial().intValue() : 1)
                    .email(dadosEmpresa.getEmailContatoFatura() != null ? dadosEmpresa.getEmailContatoFatura() : dadosEmpresa.getEmail())
                    .nomeContato(dadosEmpresa.getNomeContatoFatura())
                    .relatorio(true)
                    .build());
        } else {
            // Contato da fatura padrão se não houver dados na view
            contatosFatura.add(EmpresaAtivacaoPlanoRequest.ContatoFatura.builder()
                    .codSequencial(1)
                    .email("fatura@empresa.com")
                    .nomeContato("Contato Fatura")
                    .relatorio(true)
                    .build());
        }
        
        return contatosFatura;
    }

    /**
     * CRIA PLANOS USANDO DADOS DA VIEW
     */
    private List<EmpresaAtivacaoPlanoRequest.Plano> criarPlanos(IntegracaoOdontoprev dadosEmpresa) {
        List<EmpresaAtivacaoPlanoRequest.Plano> planos = new ArrayList<>();
        
        // Plano 1
        if (dadosEmpresa.getCodigoPlano1() != null) {
            planos.add(criarPlano(
                    dadosEmpresa.getCodigoPlano1().toString(),
                    dadosEmpresa.getDataInicioPlano1(),
                    dadosEmpresa.getValorTitular1(),
                    dadosEmpresa.getValorDependente1(),
                    dadosEmpresa.getPeriodicidade1(),
                    dadosEmpresa.getCodigoRede1()
            ));
        }
        
        // Plano 2
        if (dadosEmpresa.getCodigoPlano2() != null) {
            planos.add(criarPlano(
                    dadosEmpresa.getCodigoPlano2().toString(),
                    dadosEmpresa.getDataInicioPlano2(),
                    dadosEmpresa.getValorTitular2(),
                    dadosEmpresa.getValorDependente2(),
                    dadosEmpresa.getPeriodicidade2(),
                    dadosEmpresa.getCodigoRede2()
            ));
        }
        
        // Plano 3
        if (dadosEmpresa.getCodigoPlano3() != null) {
            planos.add(criarPlano(
                    dadosEmpresa.getCodigoPlano3().toString(),
                    dadosEmpresa.getDataInicioPlano3(),
                    dadosEmpresa.getValorTitular3(),
                    dadosEmpresa.getValorDependente3(),
                    dadosEmpresa.getPeriodicidade3(),
                    dadosEmpresa.getCodigoRede3()
            ));
        }
        
        // Se não há planos na view, adiciona um plano padrão
        if (planos.isEmpty()) {
            planos.add(EmpresaAtivacaoPlanoRequest.Plano.builder()
                    .codigoPlano("9972")
                    .dataInicioPlano("2025-01-01T03:00:00.000")
                    .valorDependente(27.42)
                    .valorReembolsoUO(0.0)
                    .valorTitular(27.42)
                    .periodicidade("N")
                    .percentualAssociado(0.0)
                    .percentualDependenteRedeGenerica(0.0)
                    .percentualAgregadoRedeGenerica(0.0)
                    .redes(List.of(EmpresaAtivacaoPlanoRequest.Rede.builder().codigoRede("1").build()))
                    .build());
        }
        
        return planos;
    }

    /**
     * CRIA UM PLANO INDIVIDUAL
     */
    private EmpresaAtivacaoPlanoRequest.Plano criarPlano(String codigoPlano, String dataInicioPlano, 
                                                         Long valorTitular, Long valorDependente, 
                                                         String periodicidade, String codigoRede) {
        return EmpresaAtivacaoPlanoRequest.Plano.builder()
                .codigoPlano(codigoPlano)
                .dataInicioPlano(formatarDataInicioPlano(dataInicioPlano))
                .valorTitular(converterLongParaDouble(valorTitular))
                .valorDependente(converterLongParaDouble(valorDependente))
                .valorReembolsoUO(0.0)
                .periodicidade(periodicidade != null && !periodicidade.trim().isEmpty() ? periodicidade : "N")
                .percentualAssociado(0.0)
                .percentualDependenteRedeGenerica(0.0)
                .percentualAgregadoRedeGenerica(0.0)
                .redes(criarRedes(codigoRede))
                .build();
    }

    /**
     * CRIA REDES PARA O PLANO
     */
    private List<EmpresaAtivacaoPlanoRequest.Rede> criarRedes(String codigoRede) {
        if (codigoRede != null && !codigoRede.trim().isEmpty()) {
            return List.of(EmpresaAtivacaoPlanoRequest.Rede.builder().codigoRede(codigoRede).build());
        }
        return List.of(EmpresaAtivacaoPlanoRequest.Rede.builder().codigoRede("1").build());
    }

    /**
     * FORMATA DATA DE INÍCIO DO PLANO
     */
    private String formatarDataInicioPlano(String dataInicioPlano) {
        if (dataInicioPlano == null || dataInicioPlano.trim().isEmpty()) {
            return "2025-01-01T03:00:00.000";
        }
        try {
            // Se já está no formato correto, retorna como está
            if (dataInicioPlano.contains("T")) {
                return dataInicioPlano;
            }
            // Se é apenas data, adiciona horário
            return dataInicioPlano + "T03:00:00.000";
        } catch (Exception e) {
            log.warn("⚠️ [CONVERSÃO] Erro ao formatar dataInicioPlano '{}': {}", dataInicioPlano, e.getMessage());
            return "2025-01-01T03:00:00.000";
        }
    }

    /**
     * CONVERTE LONG PARA DOUBLE
     */
    private Double converterLongParaDouble(Long valor) {
        if (valor == null) {
            return 0.0;
        }
        return valor.doubleValue();
    }
}


