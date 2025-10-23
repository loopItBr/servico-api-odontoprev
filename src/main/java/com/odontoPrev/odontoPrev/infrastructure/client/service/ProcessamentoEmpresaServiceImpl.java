package com.odontoPrev.odontoPrev.infrastructure.client.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.odontoPrev.odontoPrev.domain.service.ConsultaEmpresaOdontoprevService;
import com.odontoPrev.odontoPrev.domain.service.GerenciadorControleSyncService;
import com.odontoPrev.odontoPrev.domain.service.ProcessamentoEmpresaService;
import com.odontoPrev.odontoPrev.infrastructure.aop.MonitorarOperacao;
import com.odontoPrev.odontoPrev.infrastructure.client.adapter.out.dto.EmpresaResponse;
import com.odontoPrev.odontoPrev.infrastructure.client.adapter.out.dto.EmpresaAtivacaoPlanoResponse;
import com.odontoPrev.odontoPrev.infrastructure.repository.IntegracaoOdontoprevRepository;
import com.odontoPrev.odontoPrev.infrastructure.repository.entity.ControleSync;
import com.odontoPrev.odontoPrev.infrastructure.repository.entity.IntegracaoOdontoprev;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import static com.odontoPrev.odontoPrev.infrastructure.aop.MonitorarOperacao.TipoExcecao.*;

import java.util.List;
import java.util.Optional;

/**
 * SERVIÇO PARA PROCESSAMENTO INDIVIDUAL DE EMPRESAS
 * 
 * FUNÇÃO PRINCIPAL:
 * Esta classe é responsável por processar uma única empresa por vez,
 * executando todo o fluxo necessário para sincronização com a OdontoPrev.
 * 
 * FLUXO COMPLETO DE PROCESSAMENTO:
 * 1. BUSCAR dados completos da empresa no banco local
 * 2. CRIAR registro de controle para auditoria
 * 3. CHAMAR API da OdontoPrev para buscar dados da empresa
 * 4. SALVAR resposta e resultado no controle de sincronização
 * 
 * ANALOGIA SIMPLES:
 * É como processar um pedido individual numa loja:
 * 1. Pega informações do cliente (buscar dados da empresa)
 * 2. Abre ficha do pedido (criar controle)
 * 3. Consulta estoque/fornecedor (chama API OdontoPrev) 
 * 4. Anota resultado na ficha (salva controle com resposta)
 * 
 * RESPONSABILIDADES:
 * - Processar UMA empresa por vez (não lotes)
 * - Gerenciar registros de controle e auditoria
 * - Integrar com API externa da OdontoPrev
 * - Tratar erros sem interromper processamento de outras empresas
 * - Medir tempo de resposta para métricas de performance
 * 
 * TRATAMENTO DE ERROS:
 * Se qualquer passo der erro, salva o erro no controle e continua.
 * Outras empresas não são afetadas por erro de uma empresa específica.
 * 
 * AUDITORIA E CONTROLE:
 * Cada processamento gera um registro na tabela de controle com:
 * - Dados enviados para OdontoPrev
 * - Resposta recebida (sucesso/erro)
 * - Timestamp e tempo de processamento
 * - Status final (sucesso/erro)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ProcessamentoEmpresaServiceImpl implements ProcessamentoEmpresaService {

    // Repositório para buscar dados completos da empresa no banco
    private final IntegracaoOdontoprevRepository integracaoRepository;
    
    // Serviço para gerenciar registros de controle e auditoria
    private final GerenciadorControleSyncService gerenciadorControleSync;
    
    // Serviço para chamar API da OdontoPrev
    private final ConsultaEmpresaOdontoprevService consultaEmpresaService;
    
    // Serviço para ativação do plano da empresa
    private final AtivacaoPlanoEmpresaService ativacaoPlanoEmpresaService;
    
    // Serviço para inclusão de empresa
    private final EmpresaInclusaoServiceImpl empresaInclusaoService;
    
    // Conversor JSON para serializar respostas da API
    private final ObjectMapper objectMapper;

    /**
     * MÉTODO PRINCIPAL - PROCESSA UMA EMPRESA INDIVIDUAL
     * 
     * Este é o ponto de entrada para processamento de uma única empresa.
     * Executa todo o fluxo desde busca de dados até salvamento do resultado.
     * 
     * FLUXO DETALHADO:
     * 1. Busca dados completos da empresa no banco local
     * 2. Se não encontrou dados, registra warning e termina
     * 3. Se encontrou, cria registro de controle de sincronização
     * 4. Chama API da OdontoPrev para buscar dados da empresa
     * 5. Processa resposta (sucesso ou erro) e salva no controle
     * 
     * TRATAMENTO DE ERROS:
     * Qualquer erro é capturado, registrado no controle, e não interrompe
     * o processamento de outras empresas. Sistema é resiliente.
     * 
     * PARÂMETRO:
     * - codigoEmpresa: código único da empresa a ser processada (ex: "A001")
     */
    @Override
    public void processar(String codigoEmpresa) {
        log.info("🚀 [PROCESSAMENTO EMPRESA] Iniciando processamento da empresa: {}", codigoEmpresa);

        try {
            // PASSO 1: Busca dados completos da empresa no banco (ÚNICA BUSCA)
            log.info("🔍 [PROCESSAMENTO EMPRESA] PASSO 1 - Buscando dados da empresa {} na view", codigoEmpresa);
            IntegracaoOdontoprev dadosCompletos = buscarDadosEmpresaOuSair(codigoEmpresa);
            if (dadosCompletos == null) {
                log.warn("⚠️ [PROCESSAMENTO EMPRESA] Dados não encontrados para empresa {} - cadastrando erro na TBSYNC", codigoEmpresa);
                cadastrarErroProcessamentoTBSync(codigoEmpresa, "Dados da empresa não encontrados na view VW_INTEGRACAO_ODONTOPREV");
                return; // Se não encontrou dados, para aqui após cadastrar erro
            }
            log.info("✅ [PROCESSAMENTO EMPRESA] Dados encontrados para empresa {}: CNPJ={}, Nome={}", 
                    codigoEmpresa, dadosCompletos.getCnpj(), dadosCompletos.getNomeFantasia());
            
            // PASSO 2: Cria ou atualiza registro de controle para auditoria
            log.info("🔍 [PROCESSAMENTO EMPRESA] PASSO 2 - Criando/atualizando registro de controle para empresa {}", codigoEmpresa);
            ControleSync controleSync = criarEMSalvarControleSync(codigoEmpresa, dadosCompletos);
            log.info("✅ [PROCESSAMENTO EMPRESA] Controle criado/atualizado com ID: {}", controleSync.getId());
            
            // PASSO 3: Fluxo de inclusão: POST → Procedure → GET → TBSYNC sucesso
            log.info("🔍 [PROCESSAMENTO EMPRESA] PASSO 3 - Iniciando fluxo de inclusão para empresa {}", codigoEmpresa);
            executarFluxoInclusaoCompleto(controleSync, codigoEmpresa, dadosCompletos);
            log.info("✅ [PROCESSAMENTO EMPRESA] Processamento concluído para empresa {}", codigoEmpresa);
            
        } catch (Exception e) {
            log.error("❌ [PROCESSAMENTO EMPRESA] Erro ao processar empresa {}: {}", codigoEmpresa, e.getMessage(), e);
            cadastrarErroProcessamentoTBSync(codigoEmpresa, e.getMessage());
        }
    }

    /**
     * EXECUTA FLUXO COMPLETO DE INCLUSÃO
     * 
     * Fluxo correto: POST → Procedure → GET → TBSYNC sucesso
     * 
     * 1. POST: Chama API para incluir empresa
     * 2. Procedure: Executa procedure para cadastrar código da empresa
     * 3. GET: Busca dados da empresa na API
     * 4. TBSYNC: Cadastra sucesso na tabela de controle
     */
    private void executarFluxoInclusaoCompleto(ControleSync controleSync, String codigoEmpresa, IntegracaoOdontoprev dadosCompletos) {
        log.info("🚀 [FLUXO INCLUSÃO] Iniciando fluxo completo para empresa: {}", codigoEmpresa);
        
        try {
            // PASSO 1: POST - Incluir empresa na API
            log.info("📤 [FLUXO INCLUSÃO] PASSO 1 - Enviando POST para incluir empresa {}", codigoEmpresa);
            EmpresaAtivacaoPlanoResponse responsePost = empresaInclusaoService.incluirEmpresa(codigoEmpresa, dadosCompletos.getNrSeqContrato());
            log.info("✅ [FLUXO INCLUSÃO] POST executado com sucesso para empresa {}", codigoEmpresa);
            
            // PASSO 2: Procedure - Cadastrar código da empresa
            log.info("🔧 [FLUXO INCLUSÃO] PASSO 2 - Executando procedure para empresa {}", codigoEmpresa);
            String codigoEmpresaApi = responsePost.getCodigoEmpresa();
            log.info("📋 [FLUXO INCLUSÃO] ANTES da procedure - codigoEmpresaApi: '{}'", codigoEmpresaApi);
            
            executarProcedureAtualizarCodigoEmpresa(dadosCompletos.getNrSeqContrato(), codigoEmpresaApi);
            
            log.info("✅ [FLUXO INCLUSÃO] DEPOIS da procedure - procedure executada com sucesso para empresa {}", codigoEmpresa);
            
            // PASSO 3: GET - Buscar dados da empresa na API
            log.info("📥 [FLUXO INCLUSÃO] PASSO 3 - Executando GET para buscar dados da empresa {}", codigoEmpresa);
            EmpresaResponse responseGet = consultaEmpresaService.buscarEmpresa(codigoEmpresaApi);
            log.info("✅ [FLUXO INCLUSÃO] GET executado com sucesso para empresa {}", codigoEmpresa);
            
            // PASSO 4: TBSYNC - Cadastrar sucesso na tabela de controle
            log.info("💾 [FLUXO INCLUSÃO] PASSO 4 - Cadastrando sucesso na TBSYNC para empresa {}", codigoEmpresa);
            processarSucesso(controleSync, responseGet, System.currentTimeMillis());
            log.info("✅ [FLUXO INCLUSÃO] Sucesso cadastrado na TBSYNC para empresa {}", codigoEmpresa);
            
            log.info("🎉 [FLUXO INCLUSÃO] Fluxo completo executado com sucesso para empresa {}", codigoEmpresa);
            
        } catch (Exception e) {
            log.error("❌ [FLUXO INCLUSÃO] Erro no fluxo de inclusão para empresa {}: {}", codigoEmpresa, e.getMessage(), e);
            gerenciadorControleSync.atualizarErro(controleSync, e.getMessage());
            gerenciadorControleSync.salvar(controleSync);
            throw e;
        }
    }

    /**
     * BUSCA DADOS COMPLETOS DA EMPRESA OU TERMINA PROCESSAMENTO
     * 
     * Este método consulta o banco para obter todos os dados necessários
     * da empresa (planos, contratos, valores, etc.) que serão enviados
     * para a OdontoPrev.
     * 
     * ESTRATÉGIA:
     * - Busca apenas o primeiro registro da empresa (método do repository)
     * - Se não encontrou, registra warning e retorna null
     * - Se encontrou, retorna objeto completo com todos os dados
     * 
     * TRATAMENTO DE DADOS AUSENTES:
     * É normal algumas empresas não terem dados completos ainda.
     * Não é erro, apenas significa que não estão prontas para sincronização.
     */
    private IntegracaoOdontoprev buscarDadosEmpresaOuSair(String codigoEmpresa) {
        log.info("🔍 [BUSCA DADOS] Buscando dados para empresa: '{}'", codigoEmpresa);
        
        // Converte String para Long (nrSeqContrato)
        Long nrSeqContrato = Long.valueOf(codigoEmpresa);
        log.info("🔄 [BUSCA DADOS] Conversão String->Long: '{}' -> {}", codigoEmpresa, nrSeqContrato);
        
        // BUSCA ROBUSTA: tentar diferentes abordagens
        Optional<IntegracaoOdontoprev> dadosEmpresaOpt = null;
        
        // Tentativa 1: Busca direta por NR_SEQ_CONTRATO
        try {
            dadosEmpresaOpt = integracaoRepository.buscarPrimeiroDadoPorCodigoEmpresa(nrSeqContrato);
            log.info("🔍 [BUSCA DADOS] Tentativa 1 - Busca por NR_SEQ_CONTRATO: {}", dadosEmpresaOpt.isPresent() ? "SUCESSO" : "FALHOU");
        } catch (Exception e) {
            log.warn("⚠️ [BUSCA DADOS] Tentativa 1 falhou: {}", e.getMessage());
        }
        
        // Se não encontrou dados da empresa
        if (dadosEmpresaOpt == null || dadosEmpresaOpt.isEmpty()) {
            log.warn("⚠️ [BUSCA DADOS] Nenhum dado encontrado para a empresa: '{}' (nrSeqContrato: {})", codigoEmpresa, nrSeqContrato);
            log.warn("⚠️ [BUSCA DADOS] Query executada: SELECT * FROM TASY.VW_INTEGRACAO_ODONTOPREV WHERE NR_SEQ_CONTRATO = {} AND ROWNUM = 1", nrSeqContrato);
            log.warn("⚠️ [BUSCA DADOS] Verificando se há dados na view...");
            
            // Verificar se há dados na view
            try {
                List<IntegracaoOdontoprev> todasEmpresas = integracaoRepository.buscarEmpresasCompletasParaInclusao();
                log.info("🔍 [BUSCA DADOS] Total de empresas na view: {}", todasEmpresas.size());
                
                if (!todasEmpresas.isEmpty()) {
                    log.info("🔍 [BUSCA DADOS] Primeiras 3 empresas na view:");
                    for (int i = 0; i < Math.min(3, todasEmpresas.size()); i++) {
                        IntegracaoOdontoprev emp = todasEmpresas.get(i);
                        if (emp != null) {
                            log.info("🔍 [BUSCA DADOS] Empresa {}: NR_SEQ_CONTRATO={}, CNPJ={}, Nome={}", 
                                    i+1, emp.getNrSeqContrato(), emp.getCnpj(), emp.getNomeFantasia());
                        } else {
                            log.warn("⚠️ [BUSCA DADOS] Empresa {} é null", i+1);
                        }
                    }
                }
            } catch (Exception e) {
                log.warn("⚠️ [BUSCA DADOS] Erro ao verificar view: {}", e.getMessage());
            }
            
            return null; // Indica que não há dados para processar
        }
        
        // Se encontrou, retorna dados completos
        IntegracaoOdontoprev dados = dadosEmpresaOpt.get();
        log.info("✅ [BUSCA DADOS] Dados encontrados para empresa '{}': CNPJ={}, Nome={}, NR_SEQ_CONTRATO={}", 
                codigoEmpresa, dados.getCnpj(), dados.getNomeFantasia(), dados.getNrSeqContrato());
        return dados;
    }

    /**
     * CRIA E SALVA REGISTRO DE CONTROLE PARA AUDITORIA
     * 
     * Este método cria um registro na tabela de controle que serve para:
     * 1. AUDITORIA: registrar que empresa foi processada
     * 2. RASTREAMENTO: saber quando foi processada
     * 3. DADOS: guardar quais dados foram enviados para OdontoPrev
     * 4. RESULTADO: registrar se deu certo ou erro
     * 
     * O controle é salvo ANTES da chamada da API para garantir que sempre
     * temos registro do que foi tentado, mesmo se der erro na API.
     */
    private ControleSync criarEMSalvarControleSync(String codigoEmpresa, IntegracaoOdontoprev dadosCompletos) {
        log.info("🔧 [CRIAR CONTROLE] Iniciando criação de controle para empresa: {}", codigoEmpresa);
        
        // Cria ou atualiza objeto de controle com dados da empresa
        ControleSync controleSync = gerenciadorControleSync.criarControle(codigoEmpresa, dadosCompletos);
        log.info("📋 [CRIAR CONTROLE] Controle criado - Status: {}, Tipo: {}", 
                controleSync.getStatusSync(), controleSync.getTipoControle());
        
        // Salva no banco e retorna com ID gerado
        ControleSync controleSalvo = gerenciadorControleSync.salvar(controleSync);
        log.info("💾 [CRIAR CONTROLE] Controle salvo com ID: {} para empresa: {}", 
                controleSalvo.getId(), controleSalvo.getCodigoEmpresa());
        
        log.info("📝 [EMPRESA] Registro de controle processado - ID: {}, Status: {}, Tipo: {}", 
                controleSalvo.getId(), controleSalvo.getStatusSync(),
                controleSalvo.getId() != null ? "ATUALIZAÇÃO" : "CRIAÇÃO");
        
        return controleSalvo;
    }

    /**
     * CHAMA API DA ODONTOPREV E PROCESSA RESULTADO
     * 
     * Este é o "coração" da integração. Aqui acontece a comunicação real
     * com a API externa da OdontoPrev e o tratamento do resultado.
     * 
     * FLUXO:
     * 1. Mede tempo de início da chamada
     * 2. Chama API da OdontoPrev
     * 3. Calcula tempo de resposta 
     * 4. Se deu certo: processa resposta de sucesso
     * 5. Se deu erro: captura erro e salva no controle
     * 
     * MEDIÇÃO DE PERFORMANCE:
     * Registra tempo de resposta para monitorar performance da API externa.
     * Importante para identificar lentidão ou problemas na integração.
     */
    private void buscarEProcessarResposta(ControleSync controleSync, String codigoEmpresa, IntegracaoOdontoprev dadosEmpresa) {
        try {
            // Registra momento do início da chamada para medir performance
            long inicioTempo = System.currentTimeMillis();
            
            // 1) Inclusão empresarial: POST /empresa/2.0/empresas/contrato/empresarial
            String codigoEmpresaApi = null;
            try {
                var resp = ativacaoPlanoEmpresaService.ativarPlanoEmpresa(dadosEmpresa);
                codigoEmpresaApi = (resp != null) ? resp.getCodigoEmpresa() : null;
            } catch (Exception e) {
                log.error("❌ [INCLUSAO EMPRESA] Erro ao enviar POST empresarial para empresa {}: {}", codigoEmpresa, e.getMessage());
            }

            // 1.1) Executa procedure com NR_SEQ_CONTRATO (equivale ao NR_SEQUENCIA) e codigoEmpresa retornado
            if (dadosEmpresa.getNrSeqContrato() != null && codigoEmpresaApi != null) {
                log.info("🔧 [FLUXO INCLUSAO] Condições atendidas para executar procedure - nrSeqContrato: {}, codigoEmpresaApi: '{}'", 
                        dadosEmpresa.getNrSeqContrato(), codigoEmpresaApi);
                
                try {
                    log.info("🚀 [FLUXO INCLUSAO] Chamando procedure SS_PLS_CAD_CODEMPRESA_ODONTOPREV para empresa {}", codigoEmpresa);
                    executarProcedureAtualizarCodigoEmpresa(dadosEmpresa.getNrSeqContrato(), codigoEmpresaApi);
                    
                    // Atualiza o controle com o codigoEmpresa real da API
                    controleSync.setCodigoEmpresa(codigoEmpresaApi);
                    log.info("🔄 [CONTROLE] Atualizado codigoEmpresa do controle: {} -> {}", codigoEmpresa, codigoEmpresaApi);
                    log.info("✅ [FLUXO INCLUSAO] Procedure executada com sucesso para empresa {}", codigoEmpresa);
                    
                } catch (Exception e) {
                    log.error("❌ [PROCEDURE] Erro ao executar SS_PLS_CAD_CODEMPRESA_ODONTOPREV para empresa {}: {}", codigoEmpresa, e.getMessage());
                    log.error("📊 [PROCEDURE] Detalhes do erro - nrSeqContrato: {}, codigoEmpresaApi: '{}'", 
                            dadosEmpresa.getNrSeqContrato(), codigoEmpresaApi);
                }
            } else {
                log.warn("⚠️ [FLUXO INCLUSAO] Condições NÃO atendidas para executar procedure");
                log.warn("📊 [FLUXO INCLUSAO] nrSeqContrato: {}, codigoEmpresaApi: '{}'", 
                        dadosEmpresa.getNrSeqContrato(), codigoEmpresaApi);
                
                if (dadosEmpresa.getNrSeqContrato() == null) {
                    log.warn("⚠️ [FLUXO INCLUSAO] nrSeqContrato é nulo - procedure não será executada");
                }
                if (codigoEmpresaApi == null) {
                    log.warn("⚠️ [FLUXO INCLUSAO] codigoEmpresaApi é nulo - procedure não será executada");
                }
            }

            // 2) GET-API: Consulta empresa após inclusão (usa o codigoEmpresa da API)
            String codigoEmpresaParaConsulta = (codigoEmpresaApi != null) ? codigoEmpresaApi : codigoEmpresa;
            EmpresaResponse response = consultaEmpresaService.buscarEmpresa(codigoEmpresaParaConsulta);
            
            // Calcula tempo total que a API demorou para responder
            long tempoResposta = System.currentTimeMillis() - inicioTempo;
            
            // Se chegou aqui, API respondeu com sucesso
            processarSucesso(controleSync, response, tempoResposta);
            
        } catch (Exception e) {
            // Se deu qualquer erro na chamada da API
            log.error("Erro ao buscar empresa {}: {}", codigoEmpresa, e.getMessage());
            
            // Atualiza controle com informações do erro
            gerenciadorControleSync.atualizarErro(controleSync, e.getMessage());
            
            // Salva controle atualizado no banco
            gerenciadorControleSync.salvar(controleSync);
        }
    }

    @org.springframework.beans.factory.annotation.Autowired
    private org.springframework.jdbc.core.JdbcTemplate jdbcTemplate;

    /**
     * EXECUÇÃO DA PROCEDURE SS_PLS_CAD_CODEMPRESA_ODONTOPREV
     * 
     * Executa a procedure no banco Tasy para registrar o codigoEmpresa retornado pela OdontoPrev.
     * Esta procedure é responsável por atualizar o campo codigoEmpresa na tabela base da view.
     */
    @MonitorarOperacao(
            operacao = "EXECUTAR_PROCEDURE_EMPRESA",
            incluirParametros = {"nrSequenciaContrato", "codigoEmpresaApi"},
            excecaoEmErro = PROCESSAMENTO_EMPRESA
    )
    private void executarProcedureAtualizarCodigoEmpresa(Long nrSequenciaContrato, String codigoEmpresaApi) {
        log.info("🚀 [PROCEDURE EMPRESA] Iniciando execução da procedure SS_PLS_CAD_CODEMPRESA_ODONTOPREV");
        log.info("📋 [PROCEDURE EMPRESA] Parâmetros - nrSequenciaContrato: {}, codigoEmpresaApi: '{}'", 
                nrSequenciaContrato, codigoEmpresaApi);
        
        // Validações dos parâmetros
        if (nrSequenciaContrato == null) {
            log.error("❌ [PROCEDURE EMPRESA] VALIDAÇÃO FALHOU - nrSequenciaContrato é nulo");
            throw new IllegalArgumentException("nrSequenciaContrato não pode ser nulo");
        }
        
        if (codigoEmpresaApi == null || codigoEmpresaApi.trim().isEmpty()) {
            log.error("❌ [PROCEDURE EMPRESA] VALIDAÇÃO FALHOU - codigoEmpresaApi é nulo ou vazio");
            throw new IllegalArgumentException("codigoEmpresaApi não pode ser nulo ou vazio");
        }
        
        log.info("✅ [PROCEDURE EMPRESA] Validações passaram - todos os parâmetros são válidos");
        
        try {
            String sql = "{ call TASY.SS_PLS_CAD_CODEMPRESA_ODONTOPREV(?, ?) }";
            log.info("🔧 [PROCEDURE EMPRESA] ANTES da procedure - SQL: {}", sql);
            log.info("🔧 [PROCEDURE EMPRESA] ANTES da procedure - Parâmetros: nrSequenciaContrato={}, codigoEmpresaApi='{}'", 
                    nrSequenciaContrato, codigoEmpresaApi);
            
            log.info("🔄 [PROCEDURE EMPRESA] ANTES da procedure - Executando CallableStatement...");
            
            jdbcTemplate.execute(sql, (org.springframework.jdbc.core.CallableStatementCallback<Void>) cs -> {
                log.info("🔗 [PROCEDURE EMPRESA] ANTES da procedure - Conexão obtida - criando CallableStatement");
                
                // Configurar os parâmetros IN
                cs.setLong(1, nrSequenciaContrato); // p_nr_sequencia as NUMBER
                cs.setString(2, codigoEmpresaApi); // p_codigo_empresa as VARCHAR2
                
                log.info("📝 [PROCEDURE EMPRESA] ANTES da procedure - Parâmetros setados - p_nr_sequencia={}, p_codigo_empresa='{}'", 
                        nrSequenciaContrato, codigoEmpresaApi);
                
                log.info("⚡ [PROCEDURE EMPRESA] ANTES da procedure - Executando cs.execute()...");
                boolean result = cs.execute();
                log.info("✅ [PROCEDURE EMPRESA] DEPOIS da procedure - cs.execute() retornou: {}", result);
                
                return null;
            });
            
            log.info("✅ [PROCEDURE EMPRESA] DEPOIS da procedure - Procedure SS_PLS_CAD_CODEMPRESA_ODONTOPREV executada com sucesso!");
            log.info("✅ [PROCEDURE EMPRESA] DEPOIS da procedure - Empresa {} agora deve ter codigoEmpresa atualizado na view", nrSequenciaContrato);
            log.info("✅ [PROCEDURE EMPRESA] DEPOIS da procedure - Código da empresa '{}' cadastrado no banco local", codigoEmpresaApi);
            
        } catch (Exception e) {
            log.error("❌ [PROCEDURE EMPRESA] Erro ao executar procedure SS_PLS_CAD_CODEMPRESA_ODONTOPREV: {}", e.getMessage(), e);
            log.error("📊 [PROCEDURE EMPRESA] Detalhes do erro - nrSequenciaContrato: {}, codigoEmpresaApi: '{}'", 
                    nrSequenciaContrato, codigoEmpresaApi);
            
            // Cadastrar erro na TBSYNC (tabela de controle)
            cadastrarErroProcedureTBSync(nrSequenciaContrato, codigoEmpresaApi, e.getMessage());
            
            throw new RuntimeException("Falha na execução da procedure SS_PLS_CAD_CODEMPRESA_ODONTOPREV: " + e.getMessage(), e);
        }
    }
    
    /**
     * CADASTRA ERRO DA PROCEDURE NA TBSYNC
     * 
     * Registra o erro da procedure na tabela de controle para auditoria e rastreamento.
     */
    private void cadastrarErroProcedureTBSync(Long nrSequenciaContrato, String codigoEmpresaApi, String mensagemErro) {
        try {
            log.info("📝 [TBSYNC] Cadastrando erro da procedure na tabela de controle");
            
            // Buscar dados da empresa para criar registro de controle
            IntegracaoOdontoprev dadosEmpresa = buscarDadosEmpresaOuSair(String.valueOf(nrSequenciaContrato));
            if (dadosEmpresa == null) {
                log.warn("⚠️ [TBSYNC] Não foi possível obter dados da empresa {} para cadastrar erro", nrSequenciaContrato);
                return;
            }
            
            // Criar registro de controle com erro
            ControleSync controleErro = ControleSync.builder()
                    .codigoEmpresa(String.valueOf(nrSequenciaContrato))
                    .tipoOperacao(ControleSync.TipoOperacao.CREATE)
                    .tipoControle(ControleSync.TipoControle.ADICAO.getCodigo())
                    .endpointDestino("PROCEDURE_SS_PLS_CAD_CODEMPRESA_ODONTOPREV")
                    .dadosJson(String.format("{\"nrSequenciaContrato\":%d,\"codigoEmpresaApi\":\"%s\"}", 
                            nrSequenciaContrato, codigoEmpresaApi))
                    .statusSync(ControleSync.StatusSync.ERROR)
                    .erroMensagem("ERRO_PROCEDURE: " + mensagemErro)
                    .dataCriacao(java.time.LocalDateTime.now())
                    .build();
            
            // Salvar na tabela de controle
            ControleSync controleSalvo = gerenciadorControleSync.salvar(controleErro);
            log.info("💾 [TBSYNC] Erro da procedure cadastrado na TBSYNC com ID: {} para empresa {}", 
                    controleSalvo.getId(), nrSequenciaContrato);
            
        } catch (Exception e) {
            log.error("❌ [TBSYNC] Erro ao cadastrar erro da procedure na TBSYNC: {}", e.getMessage(), e);
        }
    }

    /**
     * PROCESSA RESPOSTA DE SUCESSO DA API
     * 
     * Quando a API da OdontoPrev responde com sucesso, este método:
     * 1. Converte resposta para JSON (para armazenamento)
     * 2. Atualiza controle com dados de sucesso
     * 3. Salva controle atualizado no banco
     * 4. ATIVA O PLANO DA EMPRESA automaticamente
     * 
     * TRATAMENTO DE ERRO NA SERIALIZAÇÃO:
     * Mesmo que a API tenha dado certo, pode dar erro na conversão para JSON.
     * Neste caso, registra como erro no controle.
     */
    private void processarSucesso(ControleSync controleSync, EmpresaResponse response, long tempoResposta) {
        try {
            log.info("🔄 [PROCESSAR SUCESSO] Iniciando processamento de sucesso para empresa: {}", controleSync.getCodigoEmpresa());
            
            // Converte objeto de resposta para JSON (String)
            String responseJson = objectMapper.writeValueAsString(response);
            log.debug("📄 [PROCESSAR SUCESSO] Response JSON gerado: {} caracteres", responseJson.length());
            
            // Atualiza controle com dados de sucesso
            gerenciadorControleSync.atualizarSucesso(controleSync, responseJson, tempoResposta);
            log.info("✅ [PROCESSAR SUCESSO] Controle atualizado com sucesso");
            
            // Salva controle com informações de sucesso
            ControleSync controleSalvo = gerenciadorControleSync.salvar(controleSync);
            log.info("💾 [PROCESSAR SUCESSO] Controle salvo com ID: {} para empresa: {}", 
                    controleSalvo.getId(), controleSalvo.getCodigoEmpresa());
            
            // 🚀 NOVA FUNCIONALIDADE: Ativar plano da empresa após sincronização bem-sucedida
            log.info("🎯 [SINCRONIZAÇÃO] Empresa {} sincronizada com sucesso, iniciando ativação do plano", 
                    controleSync.getCodigoEmpresa());
            
            // Buscar dados completos da empresa para ativação
            IntegracaoOdontoprev dadosEmpresa = buscarDadosEmpresaOuSair(controleSync.getCodigoEmpresa());
            if (dadosEmpresa != null) {
                ativacaoPlanoEmpresaService.ativarPlanoEmpresa(dadosEmpresa);
            } else {
                log.warn("⚠️ [ATIVAÇÃO PLANO] Não foi possível obter dados da empresa {} para ativação do plano", 
                        controleSync.getCodigoEmpresa());
            }
            
        } catch (Exception e) {
            // Erro na conversão para JSON (raro, mas pode acontecer)
            log.error("Erro ao processar resposta da empresa {}: {}", 
                    controleSync.getCodigoEmpresa(), e.getMessage());
            
            // Mesmo tendo recebido resposta da API, registra como erro 
            // porque não conseguimos armazenar adequadamente
            gerenciadorControleSync.atualizarErro(controleSync, 
                    "Erro ao serializar resposta: " + e.getMessage());
            
            gerenciadorControleSync.salvar(controleSync);
        }
    }

    /**
     * CADASTRA ERRO DE PROCESSAMENTO NA TBSYNC
     * 
     * Registra erros gerais de processamento na tabela de controle para auditoria.
     */
    private void cadastrarErroProcessamentoTBSync(String codigoEmpresa, String mensagemErro) {
        try {
            log.info("📝 [TBSYNC] Cadastrando erro de processamento na tabela de controle");
            ControleSync controleErro = ControleSync.builder()
                    .codigoEmpresa(codigoEmpresa)
                    .tipoOperacao(ControleSync.TipoOperacao.CREATE)
                    .tipoControle(ControleSync.TipoControle.ADICAO.getCodigo())
                    .endpointDestino("PROCESSAMENTO_EMPRESA")
                    .dadosJson(String.format("{\"codigoEmpresa\":\"%s\"}", codigoEmpresa))
                    .statusSync(ControleSync.StatusSync.ERROR)
                    .erroMensagem("ERRO_PROCESSAMENTO: " + mensagemErro)
                    .dataCriacao(java.time.LocalDateTime.now())
                    .build();
            ControleSync controleSalvo = gerenciadorControleSync.salvar(controleErro);
            log.info("💾 [TBSYNC] Erro de processamento cadastrado na TBSYNC com ID: {} para empresa {}",
                    controleSalvo.getId(), codigoEmpresa);
        } catch (Exception e) {
            log.error("❌ [TBSYNC] Erro ao cadastrar erro de processamento na TBSYNC: {}", e.getMessage(), e);
        }
    }

}