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
import org.springframework.jdbc.core.CallableStatementCallback;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

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
        EmpresaAtivacaoPlanoRequest request = EmpresaAtivacaoPlanoRequest.builder()
                .sistema("SabinSinai")
                .tipoPessoa("J")
                .emiteCarteirinhaPlastica("N")
                .codigoEmpresaGestora(1)
                .codigoFilialEmpresaGestora(1)
                .codigoGrupoGerencial("787392")
                .codigoNaturezaJuridica("6550-2")
                .nomeNaturezaJuridica("Planos de saúde")
                .situacaoCadastral("ATIVO")
                .inscricaoMunicipal("997.179.737.204")
                .inscricaoEstadual("997.179.737.204")
                .dataConstituicao("2025-10-01T00:00:00.000Z")
                .renovacaoAutomatica("S")
                .codigoClausulaReajusteDiferenciado("1")
                .departamento("SEM DEPARTAMENTO")
                .dependentePaga("N")
                .permissaoCadastroDep(true)
                .modeloCobrancaVarejo(false)
                .numeroMinimoAssociados(3)
                .numeroFuncionarios(0)
                .numeroDepedentes(0)
                .idadeLimiteDependente(21)
                .valorFator(1)
                .tipoRetornoCritica("T")
                .codigoLayoutCarteirinha("B")
                .codigoOrdemCarteira(3)
                .codigoDocumentoContrato(0)
                .codigoCelula(9)
                .codigoMarca(1)
                .codigoDescricaoNF(0)
                .diaVencimentoAg(19)
                .codigoPerfilClienteFatura(3)
                .codigoBancoFatura("085 ")
                .multaFatura(0)
                .descontaIR("N")
                .retencaoIss("N")
                .liberaSenhaInternet("S")
                .faturamentoNotaCorte("N")
                .proRata("N")
                .custoFamiliar("S")
                .planoFamiliar("S")
                .percSinistroContrato(60)
                .idadeLimiteUniversitaria(24)
                .percentualINSSAutoGestao(0)
                .percentualMateriaisAutoGestao(0)
                .valorSinistroContrato(60.0)
                .percentualAssociado(0)
                .codigoRegiao(0)
                .codigoImagemFatura(1)
                .codigoMoeda("7")
                .codigoParceriaEstrategica(0)
                .sinistralidade(60)
                .posicaoIniTIT(1)
                .posicaoFimTIT(7)
                .regraDowngrade(0)
                .mesCompetenciaProximoFaturamento("09")
                .codigoUsuarioFaturamento("")
                .codigoUsuarioCadastro("")
                .ramo("Massificado")
                .cgc(dadosEmpresa.getCnpj())
                .razaoSocial(dadosEmpresa.getNomeFantasia())
                .nomeFantasia(dadosEmpresa.getNomeFantasia())
                .diaInicioFaturamento(20)
                .codigoUsuarioConsultor("FEODPV01583")
                .mesAniversarioReajuste(7)
                .dataInicioContrato("2025-07-17T03:00:00.000")
                .dataVigencia("2025-07-17T03:00:00.000")
                .descricaoRamoAtividade("Saúde Suplementar")
                .diaVencimento(15)
                .cnae("6550-2/00")
                .codigoManual("1 ")
                .diaLimiteConsumoAg(19)
                .email("diretoria@sabinjf.com.br")
                .diaMovAssociadoEmpresa(15)
                .build();

        // Planos padrão (exemplo)
        List<EmpresaAtivacaoPlanoRequest.Plano> planos = new ArrayList<>();
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

        // CONTATOS - Campo obrigatório
        request.setContatos(List.of(
                EmpresaAtivacaoPlanoRequest.Contato.builder()
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
                        .build()
        ));

        // CONTATOS DA FATURA - Campo obrigatório
        request.setContatosDaFatura(List.of(
                EmpresaAtivacaoPlanoRequest.ContatoFatura.builder()
                        .codSequencial(1)
                        .email("fatura@empresa.com")
                        .nomeContato("Contato Fatura")
                        .relatorio(true)
                        .build()
        ));

        return request;
    }
}


