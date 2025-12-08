package com.odontoPrev.odontoPrev.infrastructure.client.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.odontoPrev.odontoPrev.domain.service.ConsultaEmpresaOdontoprevService;
import com.odontoPrev.odontoPrev.domain.service.GerenciadorControleSyncService;
import com.odontoPrev.odontoPrev.domain.service.ProcessamentoEmpresaService;
import com.odontoPrev.odontoPrev.infrastructure.aop.MonitorarOperacao;
import com.odontoPrev.odontoPrev.infrastructure.client.adapter.out.dto.EmpresaResponse;
import com.odontoPrev.odontoPrev.infrastructure.client.adapter.out.dto.EmpresaAtivacaoPlanoResponse;
import com.odontoPrev.odontoPrev.infrastructure.client.adapter.out.dto.EmpresaPmeRequest;
import com.odontoPrev.odontoPrev.infrastructure.client.adapter.out.dto.PlanoCriarRequest;
import com.odontoPrev.odontoPrev.infrastructure.client.BeneficiarioOdontoprevFeignClient;
import com.odontoPrev.odontoPrev.infrastructure.client.domain.service.TokenService;
import com.odontoPrev.odontoPrev.infrastructure.repository.ControleSyncRepository;
import com.odontoPrev.odontoPrev.infrastructure.repository.IntegracaoOdontoprevRepository;
import com.odontoPrev.odontoPrev.infrastructure.repository.entity.ControleSync;
import com.odontoPrev.odontoPrev.infrastructure.repository.entity.IntegracaoOdontoprev;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import static com.odontoPrev.odontoPrev.infrastructure.aop.MonitorarOperacao.TipoExcecao.*;

import java.util.ArrayList;
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
    
    // Repository para acesso direto aos controles
    private final ControleSyncRepository controleSyncRepository;
    
    // Serviço para chamar API da OdontoPrev
    private final ConsultaEmpresaOdontoprevService consultaEmpresaService;
    
    // Serviço para inclusão de empresa
    private final EmpresaInclusaoServiceImpl empresaInclusaoService;
    
    // Serviço para conversão PME
    private final EmpresaPmeService empresaPmeService;
    
    // Feign client para chamadas à API OdontoPrev
    private final BeneficiarioOdontoprevFeignClient feignClient;
    
    // Serviço para obter tokens de autenticação
    private final TokenService tokenService;
    
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
            
            // VALIDAÇÃO 1: Verificar se empresa já possui codigoEmpresa (já foi sincronizada)
            if (dadosCompletos.getCodigoEmpresa() != null && !dadosCompletos.getCodigoEmpresa().trim().isEmpty()) {
                log.warn("⚠️ [PROCESSAMENTO EMPRESA] Empresa {} JÁ POSSUI codigoEmpresa: {} - PULANDO processamento para evitar duplicação", 
                        codigoEmpresa, dadosCompletos.getCodigoEmpresa());
                log.info("🔍 [PROCESSAMENTO EMPRESA] Empresa já foi sincronizada anteriormente. Para reprocessar, limpe o codigoEmpresa na view.");
                return;
            }
            
            // VALIDAÇÃO 2: Verificar se já existe registro PENDING ou SUCCESS na tabela de controle
            log.info("🔍 [PROCESSAMENTO EMPRESA] VALIDAÇÃO 2 - Verificando se já existe registro de controle para empresa {}", codigoEmpresa);
            Optional<ControleSync> controleExistente = controleSyncRepository
                    .findFirstByCodigoEmpresaAndTipoControleOrderByDataCriacaoDesc(
                            codigoEmpresa, ControleSync.TipoControle.ADICAO.getCodigo());
            
            if (controleExistente.isPresent()) {
                ControleSync controle = controleExistente.get();
                ControleSync.StatusSync status = controle.getStatusSync();
                
                if (status == ControleSync.StatusSync.SUCCESS) {
                    log.warn("⚠️ [PROCESSAMENTO EMPRESA] Empresa {} JÁ FOI PROCESSADA COM SUCESSO (ID: {}) - PULANDO para evitar duplicação", 
                            codigoEmpresa, controle.getId());
                    log.info("🔍 [PROCESSAMENTO EMPRESA] Registro de sucesso encontrado - Data: {}, Endpoint: {}", 
                            controle.getDataSucesso(), controle.getEndpointDestino());
                    return;
                }
                
                if (status == ControleSync.StatusSync.PENDING) {
                    log.warn("⚠️ [PROCESSAMENTO EMPRESA] Empresa {} JÁ TEM PROCESSAMENTO PENDENTE (ID: {}) - PULANDO para evitar duplicação", 
                            codigoEmpresa, controle.getId());
                    log.info("🔍 [PROCESSAMENTO EMPRESA] Registro pendente encontrado - Data: {}, Endpoint: {}", 
                            controle.getDataCriacao(), controle.getEndpointDestino());
                    return;
                }
                
                // Se está em ERROR, permite reprocessar (atualizará o registro existente)
                log.info("🔄 [PROCESSAMENTO EMPRESA] Empresa {} tem registro em ERROR (ID: {}) - Permitindo reprocessamento", 
                        codigoEmpresa, controle.getId());
            }
            
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
            
            // PASSO 2: Procedure - Cadastrar código da empresa (com verificação de duplicação)
            log.info("🔧 [FLUXO INCLUSÃO] PASSO 2 - Verificando se procedure já foi executada para empresa {}", codigoEmpresa);
            String codigoEmpresaApi = responsePost.getCodigoEmpresa();
            log.info("📋 [FLUXO INCLUSÃO] ANTES da procedure - codigoEmpresaApi: '{}'", codigoEmpresaApi);
            
            // Verificar se já existe registro SUCCESS para esta empresa
            // Se já foi processada com sucesso e a response contém o mesmo codigoEmpresaApi, a procedure já foi executada
            Optional<ControleSync> controleExistente = controleSyncRepository
                    .findFirstByCodigoEmpresaAndTipoControleOrderByDataCriacaoDesc(
                            codigoEmpresa, ControleSync.TipoControle.ADICAO.getCodigo());
            
            if (controleExistente.isPresent() && 
                controleExistente.get().getStatusSync() == ControleSync.StatusSync.SUCCESS &&
                controleExistente.get().getResponseApi() != null &&
                controleExistente.get().getResponseApi().contains(codigoEmpresaApi)) {
                log.warn("⚠️ [FLUXO INCLUSÃO] Procedure JÁ FOI EXECUTADA para empresa '{}' (ID: {}) - PULANDO para evitar duplicação", 
                        codigoEmpresa, controleExistente.get().getId());
                log.info("🔍 [FLUXO INCLUSÃO] Registro de sucesso encontrado - Data: {}, codigoEmpresaApi: '{}'", 
                        controleExistente.get().getDataSucesso(), codigoEmpresaApi);
            } else {
                log.info("🔧 [FLUXO INCLUSÃO] Executando procedure para empresa {}", codigoEmpresa);
                executarProcedureAtualizarCodigoEmpresa(dadosCompletos.getNrSeqContrato(), codigoEmpresaApi);
                log.info("✅ [FLUXO INCLUSÃO] DEPOIS da procedure - procedure executada com sucesso para empresa {}", codigoEmpresa);
            }
            
            // PASSO 3: GET - Buscar dados da empresa na API
            log.info("📥 [FLUXO INCLUSÃO] PASSO 3 - Executando GET para buscar dados da empresa {}", codigoEmpresa);
            EmpresaResponse responseGet = consultaEmpresaService.buscarEmpresa(codigoEmpresaApi);
            log.info("✅ [FLUXO INCLUSÃO] GET executado com sucesso para empresa {}", codigoEmpresa);
            
            // PASSO 4: TBSYNC - Cadastrar sucesso na tabela de controle
            log.info("💾 [FLUXO INCLUSÃO] PASSO 4 - Cadastrando sucesso na TBSYNC para empresa {}", codigoEmpresa);
            processarSucesso(controleSync, responseGet, System.currentTimeMillis());
            log.info("✅ [FLUXO INCLUSÃO] Sucesso cadastrado na TBSYNC para empresa {}", codigoEmpresa);
            
            // PASSO 5: PLANOS - Criar planos via endpoint /plano/criar
            log.info("📋 [FLUXO INCLUSÃO] PASSO 5 - Executando criação de planos para empresa {}", codigoEmpresa);
            executarCriacaoPlanos(codigoEmpresaApi, dadosCompletos);
            log.info("✅ [FLUXO INCLUSÃO] Planos criados com sucesso para empresa {}", codigoEmpresa);
            
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
     * EXECUTA CRIAÇÃO DE PLANOS
     * 
     * Cria planos para a empresa usando o endpoint /plano/criar
     * e registra na TBSYNC com tipo PLANOS.
     */
    private void executarCriacaoPlanos(String codigoEmpresaApi, IntegracaoOdontoprev dadosCompletos) {
        log.info("📋 [CRIAÇÃO PLANOS] Iniciando criação de planos para empresa: {}", codigoEmpresaApi);
        
        try {
            // PASSO 1: Preparar dados da view
            log.info("📋 [CRIAÇÃO PLANOS] Dados da view - CODIGO_PLANO_1={}, CODIGO_PLANO_2={}, CODIGO_PLANO_3={}", 
                    dadosCompletos.getCodigoPlano1(), 
                    dadosCompletos.getCodigoPlano2(), 
                    dadosCompletos.getCodigoPlano3());
            log.info("📋 [CRIAÇÃO PLANOS] Valores - TITULAR_1={}, DEPENDENTE_1={}, TITULAR_2={}, DEPENDENTE_2={}, TITULAR_3={}, DEPENDENTE_3={}", 
                    dadosCompletos.getValorTitular1(), 
                    dadosCompletos.getValorDependente1(),
                    dadosCompletos.getValorTitular2(), 
                    dadosCompletos.getValorDependente2(),
                    dadosCompletos.getValorTitular3(), 
                    dadosCompletos.getValorDependente3());
            log.info("📋 [CRIAÇÃO PLANOS] Datas - INICIO_PLANO_1={}, INICIO_PLANO_2={}, INICIO_PLANO_3={}", 
                    dadosCompletos.getDataInicioPlano1(), 
                    dadosCompletos.getDataInicioPlano2(), 
                    dadosCompletos.getDataInicioPlano3());
            
            // PASSO 2: Criar request de planos
            log.info("📋 [CRIAÇÃO PLANOS] Criando request de planos...");
            PlanoCriarRequest request = criarRequestPlanos(codigoEmpresaApi, dadosCompletos);
            log.info("✅ [CRIAÇÃO PLANOS] Request criado com {} planos", 
                    request.getListaPlano() != null ? request.getListaPlano().size() : 0);
            
            // Log detalhado dos planos criados
            if (request.getListaPlano() != null && !request.getListaPlano().isEmpty()) {
                for (int i = 0; i < request.getListaPlano().size(); i++) {
                    PlanoCriarRequest.PlanoItem plano = request.getListaPlano().get(i);
                    log.info("📋 [CRIAÇÃO PLANOS] PLANO {}: codigoPlano={}, valorTitular={}, valorDependente={}, dataInicio={}", 
                            i + 1, 
                            plano.getCodigoPlano(), 
                            plano.getValorTitular(), 
                            plano.getValorDependente(), 
                            plano.getDataInicioPlano());
                }
            }
            
            // Log do JSON completo
            try {
                String requestJson = objectMapper.writeValueAsString(request);
                log.info("📤 [CRIAÇÃO PLANOS] JSON completo que será enviado:");
                log.info("{}", requestJson);
                log.info("📤 [CRIAÇÃO PLANOS] Tamanho do JSON: {} caracteres", requestJson.length());
            } catch (Exception e) {
                log.warn("⚠️ [CRIAÇÃO PLANOS] Erro ao converter request para JSON: {}", e.getMessage());
            }
            
            // PASSO 3: Chamar endpoint /plano/criar
            log.info("📤 [CRIAÇÃO PLANOS] Enviando request para endpoint /empresa/2.0/plano/criar");
            long startTime = System.currentTimeMillis();
            
            String authorization = "Bearer " + obterTokenAutorizacao();
            String response = feignClient.criarPlano(authorization, request);
            
            long endTime = System.currentTimeMillis();
            long duration = endTime - startTime;
            log.info("⏰ [CRIAÇÃO PLANOS] Chamada finalizada (duração: {}ms)", duration);
            log.info("📄 [CRIAÇÃO PLANOS] Resposta da API: {}", response);
            log.info("✅ [CRIAÇÃO PLANOS] Planos criados com sucesso para empresa {}", codigoEmpresaApi);
            
            // PASSO 4: Cadastrar sucesso na TBSYNC com tipo PLANOS
            log.info("💾 [CRIAÇÃO PLANOS] Cadastrando sucesso na TBSYNC com tipo PLANOS");
            cadastrarSucessoPlanosTBSync(codigoEmpresaApi, request, response);
            log.info("✅ [CRIAÇÃO PLANOS] Sucesso registrado na TBSYNC para empresa {}", codigoEmpresaApi);
            
        } catch (Exception e) {
            log.error("❌ [CRIAÇÃO PLANOS] Erro na criação de planos para empresa {}: {}", 
                    codigoEmpresaApi, e.getMessage(), e);
            cadastrarErroPlanosTBSync(codigoEmpresaApi, e.getMessage());
            throw new RuntimeException("Falha na criação de planos: " + e.getMessage(), e);
        }
    }
    
    /**
     * CRIA REQUEST DE PLANOS
     */
    private PlanoCriarRequest criarRequestPlanos(String codigoEmpresaApi, IntegracaoOdontoprev dadosCompletos) {
        log.info("🔧 [CRIAÇÃO PLANOS] Criando request de planos para empresa: {}", codigoEmpresaApi);
        
        List<PlanoCriarRequest.PlanoItem> listaPlano = new ArrayList<>();
        
        // PLANO 1 - Se presente
        if (dadosCompletos.getCodigoPlano1() != null) {
            PlanoCriarRequest.PlanoItem plano1 = criarPlanoItem(
                dadosCompletos.getCodigoPlano1(),
                dadosCompletos.getDataInicioPlano1(),
                dadosCompletos.getValorTitular1(),
                dadosCompletos.getValorDependente1(),
                dadosCompletos.getPeriodicidade1()
            );
            listaPlano.add(plano1);
            log.info("📋 [CRIAÇÃO PLANOS] Plano 1 adicionado: {}", dadosCompletos.getCodigoPlano1());
        }
        
        // PLANO 2 - Se presente
        if (dadosCompletos.getCodigoPlano2() != null) {
            PlanoCriarRequest.PlanoItem plano2 = criarPlanoItem(
                dadosCompletos.getCodigoPlano2(),
                dadosCompletos.getDataInicioPlano2(),
                dadosCompletos.getValorTitular2(),
                dadosCompletos.getValorDependente2(),
                dadosCompletos.getPeriodicidade2()
            );
            listaPlano.add(plano2);
            log.info("📋 [CRIAÇÃO PLANOS] Plano 2 adicionado: {}", dadosCompletos.getCodigoPlano2());
        }
        
        // PLANO 3 - Se presente
        if (dadosCompletos.getCodigoPlano3() != null) {
            PlanoCriarRequest.PlanoItem plano3 = criarPlanoItem(
                dadosCompletos.getCodigoPlano3(),
                dadosCompletos.getDataInicioPlano3(),
                dadosCompletos.getValorTitular3(),
                dadosCompletos.getValorDependente3(),
                dadosCompletos.getPeriodicidade3()
            );
            listaPlano.add(plano3);
            log.info("📋 [CRIAÇÃO PLANOS] Plano 3 adicionado: {}", dadosCompletos.getCodigoPlano3());
        }
        
        // Construir request (SEM codigoEmpresa - API rejeita)
        String codigoGrupoGerencial = dadosCompletos.getCodigoGrupoGerencial() != null 
                ? dadosCompletos.getCodigoGrupoGerencial().toString() 
                : "";
        
        PlanoCriarRequest request = PlanoCriarRequest.builder()
                .codigoGrupoGerencial(codigoGrupoGerencial)
                // .codigoEmpresa(List.of(codigoEmpresaApi)) // REMOVIDO - API rejeita
                .sistema("Sabin Sinai")
                .codigoUsuario("0")
                .listaPlano(listaPlano)
                .build();
        
        log.info("✅ [CRIAÇÃO PLANOS] Request criado com {} planos", listaPlano.size());
        return request;
    }
    
    /**
     * CRIA ITEM DE PLANO INDIVIDUAL
     */
    private PlanoCriarRequest.PlanoItem criarPlanoItem(Long codigoPlano, String dataInicio, 
                                                       Long valorTitular, Long valorDependente, String periodicidade) {
        
        // Redes padrão
        List<PlanoCriarRequest.Rede> redes = List.of(
            PlanoCriarRequest.Rede.builder().codigoRede("1").build(),
            PlanoCriarRequest.Rede.builder().codigoRede("31").build(),
            PlanoCriarRequest.Rede.builder().codigoRede("32").build(),
            PlanoCriarRequest.Rede.builder().codigoRede("33").build(),
            PlanoCriarRequest.Rede.builder().codigoRede("35").build(),
            PlanoCriarRequest.Rede.builder().codigoRede("36").build(),
            PlanoCriarRequest.Rede.builder().codigoRede("37").build(),
            PlanoCriarRequest.Rede.builder().codigoRede("38").build()
        );
        
        return PlanoCriarRequest.PlanoItem.builder()
                .valorTitular(converterLongParaDouble(valorTitular))
                .codigoPlano(codigoPlano.intValue())
                .dataInicioPlano(converterStringParaLocalDateTime(dataInicio))
                .valorDependente(converterLongParaDouble(valorDependente))
                .valorReembolsoUO(0.0)
                .percentualAgregadoRedeGenerica(0.0)
                .percentualDependenteRedeGenerica(0.0)
                .idSegmentacaoGrupoRede(0)
                .idNomeFantasia(0)
                .redes(redes)
                .percentualAssociado(0.0)
                .planoFamiliar("")
                .periodicidade(periodicidade != null && !periodicidade.trim().isEmpty() ? periodicidade : "N")
                .build();
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

    /**
     * CONVERTE STRING PARA LOCALDATETIME
     */
    private java.time.LocalDateTime converterStringParaLocalDateTime(String data) {
        if (data == null || data.trim().isEmpty()) {
            return java.time.LocalDateTime.now();
        }
        try {
            // Se já está no formato correto, converte diretamente
            if (data.contains("T")) {
                return java.time.LocalDateTime.parse(data, java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'"));
            }
            // Se é apenas data, adiciona horário
            return java.time.LocalDateTime.parse(data + "T00:00:00.000Z", java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'"));
        } catch (Exception e) {
            log.warn("⚠️ [CONVERSÃO] Erro ao converter data '{}' para LocalDateTime: {}", data, e.getMessage());
            return java.time.LocalDateTime.now();
        }
    }

    /**
     * FORMATA STRING DE DATA PARA FORMATO ISO
     */
    private String formatarDataString(String data) {
        if (data == null || data.trim().isEmpty()) {
            return java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'"));
        }
        try {
            // Se já está no formato correto, retorna como está
            if (data.contains("T")) {
                return data;
            }
            // Se é apenas data, adiciona horário
            return data + "T00:00:00.000Z";
        } catch (Exception e) {
            log.warn("⚠️ [CONVERSÃO] Erro ao formatar data '{}': {}", data, e.getMessage());
            return java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'"));
        }
    }

    /**
     * CONVERTE STRING PARA DOUBLE
     */
    private Double converterStringParaDouble(String valor) {
        if (valor == null || valor.trim().isEmpty()) {
            return 0.0;
        }
        try {
            return Double.parseDouble(valor.replace(",", "."));
        } catch (NumberFormatException e) {
            log.warn("⚠️ [CONVERSÃO] Erro ao converter valor '{}' para double: {}", valor, e.getMessage());
            return 0.0;
        }
    }
    
    /**
     * CADASTRA SUCESSO DE PLANOS NA TBSYNC
     */
    private void cadastrarSucessoPlanosTBSync(String codigoEmpresaApi, PlanoCriarRequest request, String response) {
        try {
            log.info("📝 [TBSYNC PLANOS] Cadastrando sucesso de planos na tabela de controle");
            log.info("📝 [TBSYNC PLANOS] Criando registro com tipo PLANOS (4) para empresa {}", codigoEmpresaApi);
            
            // Converter request para JSON
            String dadosJson = objectMapper.writeValueAsString(request);
            log.info("📝 [TBSYNC PLANOS] JSON convertido - tamanho: {} caracteres", dadosJson.length());
            
            // Criar registro de controle com sucesso de planos
            ControleSync controlePlanos = ControleSync.builder()
                    .codigoEmpresa(codigoEmpresaApi)
                    .tipoOperacao(ControleSync.TipoOperacao.CREATE)
                    .tipoControle(ControleSync.TipoControle.PLANOS.getCodigo()) // Tipo PLANOS (4)
                    .endpointDestino("POST_EMPRESA_PLANO_CRIAR")
                    .dadosJson(dadosJson)
                    .responseApi(response)
                    .statusSync(ControleSync.StatusSync.SUCCESS)
                    .erroMensagem(null)
                    .dataCriacao(java.time.LocalDateTime.now())
                    .dataSucesso(java.time.LocalDateTime.now())
                    .build();
            
            log.info("📝 [TBSYNC PLANOS] Controle criado - tipoControle: {}, codigoEmpresa: {}, status: {}", 
                    controlePlanos.getTipoControle(), 
                    controlePlanos.getCodigoEmpresa(), 
                    controlePlanos.getStatusSync());
            
            // Salvar na tabela de controle
            ControleSync controleSalvo = gerenciadorControleSync.salvar(controlePlanos);
            log.info("💾 [TBSYNC PLANOS] Sucesso! Registro PLANOS cadastrado na TBSYNC:");
            log.info("   - ID: {}", controleSalvo.getId());
            log.info("   - codigoEmpresa: {}", controleSalvo.getCodigoEmpresa());
            log.info("   - tipoControle: PLANOS ({})", controleSalvo.getTipoControle());
            log.info("   - status: {}", controleSalvo.getStatusSync());
            log.info("   - endpoint: {}", controleSalvo.getEndpointDestino());
            log.info("   - dataCriacao: {}", controleSalvo.getDataCriacao());
            log.info("   - dataSucesso: {}", controleSalvo.getDataSucesso());
            
        } catch (Exception e) {
            log.error("❌ [TBSYNC PLANOS] Erro ao cadastrar sucesso de planos na TBSYNC: {}", e.getMessage(), e);
        }
    }
    
    /**
     * CADASTRA ERRO DE PLANOS NA TBSYNC
     */
    private void cadastrarErroPlanosTBSync(String codigoEmpresaApi, String mensagemErro) {
        try {
            log.info("📝 [TBSYNC PLANOS] Cadastrando erro de planos na tabela de controle");
            
            ControleSync controlePlanos = ControleSync.builder()
                    .codigoEmpresa(codigoEmpresaApi)
                    .tipoOperacao(ControleSync.TipoOperacao.CREATE)
                    .tipoControle(ControleSync.TipoControle.PLANOS.getCodigo()) // Tipo PLANOS (4)
                    .endpointDestino("POST_EMPRESA_PLANO_CRIAR")
                    .dadosJson(String.format("{\"codigoEmpresa\":\"%s\"}", codigoEmpresaApi))
                    .statusSync(ControleSync.StatusSync.ERROR)
                    .erroMensagem("ERRO_PLANOS: " + mensagemErro)
                    .dataCriacao(java.time.LocalDateTime.now())
                    .build();
            
            ControleSync controleSalvo = gerenciadorControleSync.salvar(controlePlanos);
            log.info("💾 [TBSYNC PLANOS] Erro cadastrado na TBSYNC com ID: {} para empresa {}", 
                    controleSalvo.getId(), codigoEmpresaApi);
            
        } catch (Exception e) {
            log.error("❌ [TBSYNC PLANOS] Erro ao cadastrar erro de planos na TBSYNC: {}", e.getMessage(), e);
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

    /**
     * EXECUTA CADASTRO PME DA EMPRESA
     * 
     * Esta é a etapa final do fluxo de inclusão empresarial.
     * Após o sucesso da inclusão, procedure e consulta, cadastra
     * a empresa no endpoint PME da OdontoPrev.
     * 
     * FLUXO:
     * 1. Converte dados da view para request PME
     * 2. Chama endpoint PME da OdontoPrev
     * 3. Cadastra resultado na TBSYNC com tipo PLANOS
     * 
     * @param controleSync Controle da empresa
     * @param dadosCompletos Dados completos da empresa da view
     */
    @MonitorarOperacao(
            operacao = "CADASTRO_PME_EMPRESA",
            incluirParametros = {"dadosCompletos"},
            excecaoEmErro = PROCESSAMENTO_EMPRESA
    )
    private void executarCadastroPme(ControleSync controleSync, IntegracaoOdontoprev dadosCompletos) {
        log.info("🏢 [CADASTRO PME] Iniciando cadastro PME para empresa: {}", dadosCompletos.getNomeFantasia());
        
        try {
            // PASSO 1: Converter dados da view para request PME
            log.info("🔄 [CADASTRO PME] PASSO 1 - Convertendo dados da view para request PME");
            log.info("📋 [CADASTRO PME] Dados da view: CODIGO_PLANO_1={}, CODIGO_PLANO_2={}, CODIGO_PLANO_3={}", 
                    dadosCompletos.getCodigoPlano1(), 
                    dadosCompletos.getCodigoPlano2(), 
                    dadosCompletos.getCodigoPlano3());
            log.info("📋 [CADASTRO PME] Valores - TITULAR_1={}, DEPENDENTE_1={}, TITULAR_2={}, DEPENDENTE_2={}, TITULAR_3={}, DEPENDENTE_3={}", 
                    dadosCompletos.getValorTitular1(), 
                    dadosCompletos.getValorDependente1(),
                    dadosCompletos.getValorTitular2(), 
                    dadosCompletos.getValorDependente2(),
                    dadosCompletos.getValorTitular3(), 
                    dadosCompletos.getValorDependente3());
            log.info("📋 [CADASTRO PME] Datas - INICIO_PLANO_1={}, INICIO_PLANO_2={}, INICIO_PLANO_3={}", 
                    dadosCompletos.getDataInicioPlano1(), 
                    dadosCompletos.getDataInicioPlano2(), 
                    dadosCompletos.getDataInicioPlano3());
            
            EmpresaPmeRequest requestPme = empresaPmeService.converterParaRequestPme(dadosCompletos);
            log.info("✅ [CADASTRO PME] Request PME convertido com sucesso - {} planos", 
                    requestPme.getPlanos() != null ? requestPme.getPlanos().size() : 0);
            
            // Log detalhado dos planos criados
            if (requestPme.getPlanos() != null && !requestPme.getPlanos().isEmpty()) {
                for (int i = 0; i < requestPme.getPlanos().size(); i++) {
                    var plano = requestPme.getPlanos().get(i);
                    log.info("📋 [CADASTRO PME] PLANO {}: codigoPlano={}, valorTitular={}, valorDependente={}, dataInicio={}", 
                            i + 1, 
                            plano.getCodigoPlano(), 
                            plano.getValorTitular(), 
                            plano.getValorDependente(), 
                            plano.getDataInicioPlano());
                }
            }
            
            // PASSO 2: Chamar endpoint PME da OdontoPrev
            log.info("📤 [CADASTRO PME] PASSO 2 - Enviando request para endpoint PME");
            log.info("📤 [CADASTRO PME] Endpoint: POST {{baseUrl}}/empresa/2.0/empresas/pme");
            log.info("📤 [CADASTRO PME] Empresa: {}", dadosCompletos.getNomeFantasia());
            log.info("📤 [CADASTRO PME] CNPJ: {}", dadosCompletos.getCnpj());
            
            long startTime = System.currentTimeMillis();
            log.info("⏰ [CADASTRO PME] Iniciando chamada PME às {}", java.time.LocalDateTime.now());
            
            // Obter token de autorização (reutilizar do serviço de inclusão)
            String authorization = "Bearer " + obterTokenAutorizacao();
            
            // Log do JSON completo que será enviado
            try {
                String requestJson = objectMapper.writeValueAsString(requestPme);
                log.info("📤 [CADASTRO PME] JSON completo que será enviado:");
                log.info("{}", requestJson);
                log.info("📤 [CADASTRO PME] Tamanho do JSON: {} caracteres", requestJson.length());
            } catch (Exception e) {
                log.warn("⚠️ [CADASTRO PME] Erro ao converter request para JSON: {}", e.getMessage());
            }
            
            feignClient.cadastrarEmpresaPme(authorization, requestPme);
            
            long endTime = System.currentTimeMillis();
            long duration = endTime - startTime;
            log.info("⏰ [CADASTRO PME] Chamada PME finalizada às {} (duração: {}ms)", 
                    java.time.LocalDateTime.now(), duration);
            log.info("✅ [CADASTRO PME] Cadastro PME executado com sucesso para empresa {}", 
                    dadosCompletos.getNomeFantasia());
            
            // PASSO 3: Cadastrar sucesso na TBSYNC com tipo PLANOS
            log.info("💾 [CADASTRO PME] PASSO 3 - Cadastrando sucesso PME na TBSYNC com tipo PLANOS");
            log.info("💾 [CADASTRO PME] Registro TBSYNC será criado com:");
            log.info("   - tipoControle: PLANOS (4)");
            log.info("   - codigoEmpresa: {}", controleSync.getCodigoEmpresa());
            log.info("   - statusSync: SUCCESS");
            cadastrarSucessoPmeTBSync(controleSync, requestPme);
            log.info("✅ [CADASTRO PME] Sucesso PME cadastrado na TBSYNC com tipo PLANOS para empresa {}", 
                    dadosCompletos.getNomeFantasia());
            
        } catch (Exception e) {
            log.error("❌ [CADASTRO PME] Erro no cadastro PME para empresa {}: {}", 
                    dadosCompletos.getNomeFantasia(), e.getMessage(), e);
            
            // Cadastrar erro PME na TBSYNC
            cadastrarErroPmeTBSync(controleSync, e.getMessage());
            throw new RuntimeException("Falha no cadastro PME: " + e.getMessage(), e);
        }
    }

    /**
     * OBTÉM TOKEN DE AUTORIZAÇÃO
     * 
     * Obtém token válido usando o mesmo TokenService usado no POST inicial.
     * Garante que o token seja válido e não expirado.
     */
    private String obterTokenAutorizacao() {
        try {
            log.info("🔑 [TOKEN PME] Obtendo token de autorização para cadastro PME");
            String token = tokenService.obterTokenValido();
            String authorization = "Bearer " + token;
            log.info("✅ [TOKEN PME] Token obtido com sucesso para cadastro PME");
            return authorization;
        } catch (Exception e) {
            log.error("❌ [TOKEN PME] Erro ao obter token de autorização para PME: {}", e.getMessage(), e);
            throw new RuntimeException("Falha ao obter token de autorização para cadastro PME", e);
        }
    }

    /**
     * CADASTRA SUCESSO PME NA TBSYNC
     * 
     * Registra o sucesso do cadastro PME na tabela de controle
     * com tipo PLANOS para auditoria.
     */
    private void cadastrarSucessoPmeTBSync(ControleSync controleSync, EmpresaPmeRequest requestPme) {
        try {
            log.info("📝 [TBSYNC PME] Cadastrando sucesso PME na tabela de controle");
            log.info("📝 [TBSYNC PME] Criando registro com tipo PLANOS (4) para empresa {}", 
                    controleSync.getCodigoEmpresa());
            
            // Converter request para JSON
            String dadosJson = objectMapper.writeValueAsString(requestPme);
            log.info("📝 [TBSYNC PME] JSON convertido - tamanho: {} caracteres", dadosJson.length());
            
            // Criar registro de controle com sucesso PME
            ControleSync controlePme = ControleSync.builder()
                    .codigoEmpresa(controleSync.getCodigoEmpresa())
                    .tipoOperacao(ControleSync.TipoOperacao.CREATE)
                    .tipoControle(ControleSync.TipoControle.PLANOS.getCodigo()) // Tipo PLANOS (4)
                    .endpointDestino("POST_EMPRESA_PME")
                    .dadosJson(dadosJson)
                    .statusSync(ControleSync.StatusSync.SUCCESS)
                    .erroMensagem(null)
                    .dataCriacao(java.time.LocalDateTime.now())
                    .dataSucesso(java.time.LocalDateTime.now())
                    .build();
            
            log.info("📝 [TBSYNC PME] Controle criado - tipoControle: {}, codigoEmpresa: {}, status: {}", 
                    controlePme.getTipoControle(), 
                    controlePme.getCodigoEmpresa(), 
                    controlePme.getStatusSync());
            
            // Salvar na tabela de controle
            ControleSync controleSalvo = gerenciadorControleSync.salvar(controlePme);
            log.info("💾 [TBSYNC PME] Sucesso! Registro PLANOS cadastrado na TBSYNC:");
            log.info("   - ID: {}", controleSalvo.getId());
            log.info("   - codigoEmpresa: {}", controleSalvo.getCodigoEmpresa());
            log.info("   - tipoControle: PLANOS ({})", controleSalvo.getTipoControle());
            log.info("   - status: {}", controleSalvo.getStatusSync());
            log.info("   - endpoint: {}", controleSalvo.getEndpointDestino());
            log.info("   - dataCriacao: {}", controleSalvo.getDataCriacao());
            log.info("   - dataSucesso: {}", controleSalvo.getDataSucesso());
            
        } catch (Exception e) {
            log.error("❌ [TBSYNC PME] Erro ao cadastrar sucesso PME na TBSYNC: {}", e.getMessage(), e);
        }
    }

    /**
     * CADASTRA ERRO PME NA TBSYNC
     * 
     * Registra o erro do cadastro PME na tabela de controle
     * com tipo PLANOS para auditoria.
     */
    private void cadastrarErroPmeTBSync(ControleSync controleSync, String mensagemErro) {
        try {
            log.info("📝 [TBSYNC PME] Cadastrando erro PME na tabela de controle");
            
            // Criar registro de controle com erro PME
            ControleSync controlePme = ControleSync.builder()
                    .codigoEmpresa(controleSync.getCodigoEmpresa())
                    .tipoOperacao(ControleSync.TipoOperacao.CREATE)
                    .tipoControle(ControleSync.TipoControle.PLANOS.getCodigo()) // Tipo PLANOS
                    .endpointDestino("POST_EMPRESA_PME")
                    .dadosJson(String.format("{\"codigoEmpresa\":\"%s\"}", controleSync.getCodigoEmpresa()))
                    .statusSync(ControleSync.StatusSync.ERROR)
                    .erroMensagem("ERRO_PME: " + mensagemErro)
                    .dataCriacao(java.time.LocalDateTime.now())
                    .build();
            
            // Salvar na tabela de controle
            ControleSync controleSalvo = gerenciadorControleSync.salvar(controlePme);
            log.info("💾 [TBSYNC PME] Erro PME cadastrado na TBSYNC com ID: {} para empresa {}", 
                    controleSalvo.getId(), controleSync.getCodigoEmpresa());
            
        } catch (Exception e) {
            log.error("❌ [TBSYNC PME] Erro ao cadastrar erro PME na TBSYNC: {}", e.getMessage(), e);
        }
    }
    
    /**
     * REPROCESSA EMPRESAS QUE FALHARAM NA CRIAÇÃO DE PLANOS
     * 
     * Busca empresas que tiveram erro (status ERROR) na criação de planos
     * (tipo PLANOS = 4) e tenta criar os planos novamente.
     * 
     * FLUXO:
     * 1. Busca empresas com erro no tipo PLANOS
     * 2. Para cada empresa, busca dados da view
     * 3. Tenta criar os planos novamente
     */
    public void reprocessarPlanosComErro() {
        log.info("🔄 [REPROCESSAMENTO PLANOS] Iniciando reprocessamento de empresas com erro na criação de planos");
        
        try {
            // Buscar empresas com erro no tipo PLANOS
            List<ControleSync> empresasComErro = controleSyncRepository
                    .findByTipoControleAndStatusSyncOrderByDataCriacaoDesc(
                            ControleSync.TipoControle.PLANOS.getCodigo(), 
                            ControleSync.StatusSync.ERROR);
            
            log.info("📊 [REPROCESSAMENTO PLANOS] Encontradas {} empresas com erro na criação de planos", 
                    empresasComErro.size());
            
            if (empresasComErro.isEmpty()) {
                log.info("✅ [REPROCESSAMENTO PLANOS] Nenhuma empresa com erro encontrada");
                return;
            }
            
            // Processar cada empresa
            int sucesso = 0;
            int erro = 0;
            
            for (ControleSync controleErro : empresasComErro) {
                String codigoEmpresa = controleErro.getCodigoEmpresa();
                log.info("🔄 [REPROCESSAMENTO PLANOS] Reprocessando empresa: {}", codigoEmpresa);
                
                try {
                    // Buscar dados da empresa na view
                    IntegracaoOdontoprev dadosEmpresa = buscarDadosEmpresaOuSair(codigoEmpresa);
                    
                    if (dadosEmpresa == null) {
                        log.warn("⚠️ [REPROCESSAMENTO PLANOS] Dados não encontrados para empresa {}", codigoEmpresa);
                        erro++;
                        continue;
                    }
                    
                    // Verificar se empresa possui codigoEmpresa (já foi sincronizada)
                    if (dadosEmpresa.getCodigoEmpresa() == null || dadosEmpresa.getCodigoEmpresa().trim().isEmpty()) {
                        log.warn("⚠️ [REPROCESSAMENTO PLANOS] Empresa {} ainda não foi sincronizada (não possui codigoEmpresa)", 
                                codigoEmpresa);
                        erro++;
                        continue;
                    }
                    
                    // Criar planos
                    executarCriacaoPlanos(dadosEmpresa.getCodigoEmpresa(), dadosEmpresa);
                    sucesso++;
                    log.info("✅ [REPROCESSAMENTO PLANOS] Empresa {} reprocessada com sucesso", codigoEmpresa);
                    
                } catch (Exception e) {
                    erro++;
                    log.error("❌ [REPROCESSAMENTO PLANOS] Erro ao reprocessar empresa {}: {}", 
                            codigoEmpresa, e.getMessage());
                }
            }
            
            log.info("🎉 [REPROCESSAMENTO PLANOS] Concluído - Sucesso: {}, Erro: {}", sucesso, erro);
            
        } catch (Exception e) {
            log.error("❌ [REPROCESSAMENTO PLANOS] Erro no reprocessamento de planos: {}", e.getMessage(), e);
        }
    }

}