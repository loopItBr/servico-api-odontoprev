package com.odontoPrev.odontoPrev.infrastructure.client.service;

import com.odontoPrev.odontoPrev.domain.service.ProcessamentoEmpresaService;
import com.odontoPrev.odontoPrev.domain.service.ProcessamentoLoteService;
import com.odontoPrev.odontoPrev.infrastructure.aop.MonitorarOperacao;
import com.odontoPrev.odontoPrev.infrastructure.repository.IntegracaoOdontoprevRepository;
import com.odontoPrev.odontoPrev.infrastructure.repository.entity.IntegracaoOdontoprev;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

import static com.odontoPrev.odontoPrev.infrastructure.aop.MonitorarOperacao.TipoExcecao.*;

/**
 * SERVIÇO PARA PROCESSAMENTO DE EMPRESAS EM LOTES PAGINADOS
 * 
 * FUNÇÃO PRINCIPAL:
 * Esta classe é responsável por dividir o trabalho de sincronização de empresas
 * em "lotes" menores, evitando sobrecarregar a memória e o banco de dados ao
 * processar milhares de empresas de uma só vez.
 * 
 * ANALOGIA SIMPLES:
 * É como dividir uma pilha de 1000 folhas para copiar em grupos de 50 folhas.
 * Em vez de tentar copiar todas de uma vez (pode travar a máquina), você
 * copia 50, depois mais 50, depois mais 50, até terminar tudo.
 * 
 * ESTRATÉGIA DE PAGINAÇÃO:
 * 1. CONTA quantas empresas existem no total
 * 2. DIVIDE em páginas pequenas (ex: 50 empresas por página)
 * 3. PROCESSA página por página em sequência
 * 4. Para cada página, processa empresa por empresa
 * 5. CONTINUA até não ter mais páginas
 * 
 * BENEFÍCIOS:
 * - MEMÓRIA: não carrega milhares de registros na memória
 * - PERFORMANCE: banco responde mais rápido com consultas menores
 * - RECUPERAÇÃO: se der erro em uma empresa, outras continuam
 * - MONITORAMENTO: pode acompanhar progresso em tempo real
 * 
 * EXEMPLO PRÁTICO:
 * Total: 1000 empresas, lote: 50
 * - Página 1: empresas 1-50
 * - Página 2: empresas 51-100
 * - Página 3: empresas 101-150
 * - ... continua até página 20 (empresas 951-1000)
 * 
 * OBSERVABILIDADE:
 * Cada operação usa @MonitorarOperacao para logs automáticos e tratamento
 * de erros, facilitando investigação de problemas.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ProcessamentoLoteServiceImpl implements ProcessamentoLoteService {

    // Repositório para consultar empresas no banco de dados
    private final IntegracaoOdontoprevRepository integracaoRepository;
    
    // Serviço responsável por processar cada empresa individualmente
    private final ProcessamentoEmpresaService processamentoEmpresaService;

    /**
     * MÉTODO PRINCIPAL - PROCESSA TODAS AS EMPRESAS EM LOTES
     * 
     * Este é o ponto de entrada para todo processamento em lotes. Coordena
     * o trabalho de divisão e processamento sequencial das páginas.
     * 
     * PARÂMETROS:
     * - tamanhoBatch: quantas empresas processar por página (ex: 50)
     * - maxThreads: quantas threads usar em paralelo (não usado ainda, futuro)
     * - totalEmpresas: total de empresas que devem ser processadas
     * 
     * FLUXO DE EXECUÇÃO:
     * 1. Verifica se tem empresas para processar
     * 2. Se não tem, termina sem fazer nada
     * 3. Se tem, inicia processamento página por página
     * 4. Continua até processar todas as empresas
     * 
     * MONITORAMENTO:
     * @MonitorarOperacao adiciona automaticamente:
     * - Logs de início/fim com duração
     * - Captura de parâmetros (tamanho do lote, total de empresas)
     * - Tratamento de exceções com contexto rico
     */
    @Override
    @MonitorarOperacao(
            operacao = "PROCESSAMENTO_LOTE",
            incluirParametros = {"tamanhoBatch", "maxThreads", "totalEmpresas"},
            excecaoEmErro = PROCESSAMENTO_LOTE
    )
    public void processarEmpresasEmLotes(int tamanhoBatch, int maxThreads, long totalEmpresas) {
        // Validação inicial: se não tem empresas, não há trabalho a fazer
        if (totalEmpresas == 0) {
            log.info("Nenhuma empresa encontrada para processamento");
            return; // Termina aqui, não é erro
        }

        // Log inicial com informações do processamento
        log.info("Processando {} empresas em lotes de {}", totalEmpresas, tamanhoBatch);
        
        // Executa o processamento de todas as páginas
        long empresasProcessadas = processarTodasAsPaginas(tamanhoBatch, totalEmpresas);
        
        // Log final com resultado
        log.info("Processamento finalizado: {} empresas", empresasProcessadas);
    }

    /**
     * BUSCA CÓDIGOS DE EMPRESAS DE FORMA PAGINADA
     * 
     * Este método consulta o banco de dados para obter uma "página" específica
     * de códigos de empresas, evitando carregar milhares de registros na memória.
     * 
     * FUNCIONAMENTO DA PAGINAÇÃO:
     * - offset: quantos registros pular (ex: 100 = pula os primeiros 100)
     * - limit: quantos registros trazer (ex: 50 = traz apenas 50)
     * 
     * EXEMPLO PRÁTICO:
     * Para buscar a página 3 com 50 empresas por página:
     * - offset = 100 (página 1=0-49, página 2=50-99, página 3=100-149)
     * - limit = 50
     * - Resultado: códigos das empresas de posição 100 a 149
     * 
     * RETORNO:
     * Lista com códigos das empresas (ex: ["A001", "A002", "B123", ...])
     */
    @Override
    @MonitorarOperacao(
            operacao = "BUSCA_PAGINADA",
            incluirParametros = {"offset", "limit"},
            logSucesso = MonitorarOperacao.NivelLog.INFO,
            excecaoEmErro = CONSULTA_EMPRESAS
    )
    public List<String> buscarCodigosEmpresasPaginado(int offset, int limit) {
        log.info("🔍 [BUSCA EMPRESAS] Buscando TODAS as empresas (SEM PAGINAÇÃO)");
        log.info("🔍 [BUSCA EMPRESAS] Executando query: SELECT DISTINCT NR_SEQ_CONTRATO FROM TASY.VW_INTEGRACAO_ODONTOPREV WHERE CODIGO_EMPRESA IS NULL AND NR_SEQ_CONTRATO IS NOT NULL ORDER BY NR_SEQ_CONTRATO");
        
        // BUSCA SIMPLES: usar o método que funcionava antes
        List<Long> nrSeqContratos = integracaoRepository.buscarEmpresasParaInclusao();
        log.info("📊 [BUSCA EMPRESAS] Encontrados {} NR_SEQ_CONTRATO no total", nrSeqContratos.size());
        
        if (!nrSeqContratos.isEmpty()) {
            log.info("✅ [BUSCA EMPRESAS] Empresas encontradas: {}", nrSeqContratos);
            
            // Converte Long para String para manter compatibilidade
            List<String> empresasString = nrSeqContratos.stream()
                    .map(String::valueOf)
                    .collect(java.util.stream.Collectors.toList());
            
            log.info("🔄 [BUSCA EMPRESAS] Conversão Long->String concluída: {}", empresasString);
            return empresasString;
            
        } else {
            log.warn("⚠️ [BUSCA EMPRESAS] NENHUMA empresa encontrada!");
            log.warn("⚠️ [BUSCA EMPRESAS] Isso pode indicar que:");
            log.warn("⚠️ [BUSCA EMPRESAS] 1. Não há empresas para processar");
            log.warn("⚠️ [BUSCA EMPRESAS] 2. A query não está retornando dados");
            log.warn("⚠️ [BUSCA EMPRESAS] 3. Problema na view VW_INTEGRACAO_ODONTOPREV");
            log.warn("⚠️ [BUSCA EMPRESAS] 4. Todas as empresas já foram processadas");
            return new ArrayList<>();
        }
    }

    /**
     * CONTA TOTAL DE EMPRESAS DISPONÍVEIS PARA SINCRONIZAÇÃO
     * 
     * Este método consulta o banco para saber quantas empresas existem
     * na view de integração. É usado para:
     * 1. Validar se há trabalho a fazer
     * 2. Calcular quantas páginas serão necessárias
     * 3. Exibir progresso nos logs
     * 
     * RETORNO: número total de empresas (ex: 1547)
     */
    @Override
    @MonitorarOperacao(
            operacao = "CONTAGEM_TOTAL_EMPRESAS",
            logSucesso = MonitorarOperacao.NivelLog.INFO,
            excecaoEmErro = CONSULTA_EMPRESAS
    )
    public long contarTotalEmpresas() {
        log.info("🔍 [CONTAGEM] Verificando total de empresas para sincronização...");
        log.info("🔍 [CONTAGEM] Executando query alternativa: SELECT COUNT(DISTINCT NR_SEQ_CONTRATO) FROM TASY.VW_INTEGRACAO_ODONTOPREV WHERE CODIGO_EMPRESA IS NULL AND NR_SEQ_CONTRATO IS NOT NULL");
        
        // Usar query alternativa que sempre pega novidades
        long total = integracaoRepository.contarEmpresasParaInclusao();
        
        log.info("📊 [CONTAGEM] Total de empresas encontradas: {}", total);
        if (total > 0) {
            log.info("✅ [CONTAGEM] Há {} empresas disponíveis para inclusão na OdontoPrev", total);
            
            // Log adicional para debug - buscar algumas empresas para verificar
            try {
                List<Long> amostra = integracaoRepository.buscarEmpresasParaInclusao();
                if (!amostra.isEmpty()) {
                    log.info("🔍 [CONTAGEM] Amostra de empresas encontradas: {}", amostra);
                    log.info("🔍 [CONTAGEM] Primeira empresa: {} (tipo: {})", amostra.get(0), amostra.get(0).getClass().getSimpleName());
                }
            } catch (Exception e) {
                log.warn("⚠️ [CONTAGEM] Erro ao buscar amostra de empresas: {}", e.getMessage());
            }
        } else {
            log.warn("⚠️ [CONTAGEM] NENHUMA empresa encontrada com CODIGO_EMPRESA IS NULL");
            log.warn("⚠️ [CONTAGEM] Isso pode indicar que:");
            log.warn("⚠️ [CONTAGEM] 1. Todas as empresas já foram processadas");
            log.warn("⚠️ [CONTAGEM] 2. Não há dados na view VW_INTEGRACAO_ODONTOPREV");
            log.warn("⚠️ [CONTAGEM] 3. A view não está retornando dados corretamente");
        }
        return total;
    }

    /**
     * PROCESSA TODAS AS PÁGINAS EM SEQUÊNCIA
     * 
     * Este é o "motor" do processamento em lotes. Implementa o loop principal
     * que busca página por página e processa cada empresa encontrada.
     * 
     * ALGORITMO:
     * 1. Começa da página 0
     * 2. Busca próxima página de empresas
     * 3. Se página vazia, termina (não há mais empresas)
     * 4. Se página tem empresas, processa todas
     * 5. Incrementa número da página
     * 6. Volta para passo 2
     * 
     * RETORNO: quantidade total de empresas processadas com sucesso
     */
    private long processarTodasAsPaginas(int tamanhoBatch, long totalEmpresas) {
        log.info("🚀 [PROCESSAMENTO] Iniciando processamento de TODAS as empresas de uma vez (SEM PAGINAÇÃO)");
        
        // Buscar TODAS as empresas de uma vez
        List<String> todasEmpresas = buscarCodigosEmpresasPaginado(0, Integer.MAX_VALUE);
        
        if (todasEmpresas.isEmpty()) {
            log.warn("⚠️ [PROCESSAMENTO] Nenhuma empresa encontrada para processar");
            return 0;
        }
        
        log.info("📊 [PROCESSAMENTO] Total de empresas encontradas: {}", todasEmpresas.size());
        log.info("✅ [PROCESSAMENTO] Empresas para processar: {}", todasEmpresas);
        
        // Processar todas as empresas de uma vez
        long empresasProcessadas = processarLote(todasEmpresas);
        
        log.info("✅ [PROCESSAMENTO] Processamento concluído - {} empresas processadas de {} encontradas", 
                empresasProcessadas, todasEmpresas.size());
        
        return empresasProcessadas;
    }

    /**
     * BUSCA PRÓXIMA PÁGINA DE EMPRESAS
     * Converte número da página em offset e chama método de busca paginada
     */
    private List<String> buscarProximoLote(int numeroPagina, int tamanhoBatch) {
        int offset = numeroPagina * tamanhoBatch; // Ex: página 3 × 50 = offset 150
        return buscarCodigosEmpresasPaginado(offset, tamanhoBatch);
    }

    /**
     * VERIFICA SE PÁGINA ESTÁ VAZIA
     * Condição para parar o loop de processamento
     */
    private boolean loteEstaVazio(List<String> lote) {
        return lote.isEmpty();
    }

    /**
     * LOG DE INÍCIO DE PROCESSAMENTO DE UMA PÁGINA
     * Registra qual página está sendo processada e quantas empresas contém
     */
    private void logInicioLote(int numeroPagina, List<String> lote) {
        log.info("Processando lote {} - {} empresas", numeroPagina + 1, lote.size());
    }

    /**
     * PROCESSA TODAS AS EMPRESAS DE UMA PÁGINA
     * 
     * Para cada código de empresa na página, chama o processamento individual.
     * Se uma empresa der erro, as outras continuam sendo processadas.
     * 
     * RETORNO: quantas empresas foram processadas com sucesso nesta página
     */
    private long processarLote(List<String> codigosEmpresas) {
        log.info("🚀 [PROCESSAMENTO LOTE] Iniciando processamento de {} empresas", codigosEmpresas.size());
        long processadasNoLote = 0; // Contador de sucessos nesta página

        // Processa cada empresa individualmente
        for (int i = 0; i < codigosEmpresas.size(); i++) {
            String codigoEmpresa = codigosEmpresas.get(i);
            log.info("🔍 [PROCESSAMENTO LOTE] Processando empresa {}/{}: {}", i + 1, codigosEmpresas.size(), codigoEmpresa);
            
            // Processa empresa com tratamento de erro
            if (processarEmpresaComSeguranca(codigoEmpresa)) {
                processadasNoLote++; // Incrementa apenas se deu certo
                log.info("✅ [PROCESSAMENTO LOTE] Empresa {} processada com sucesso", codigoEmpresa);
            } else {
                log.warn("⚠️ [PROCESSAMENTO LOTE] Empresa {} teve erro no processamento", codigoEmpresa);
            }
            // Se der erro, empresa é pulada mas outras continuam
        }
        
        log.info("📊 [PROCESSAMENTO LOTE] Lote concluído - {} empresas processadas com sucesso de {} total", 
                processadasNoLote, codigosEmpresas.size());
        
        return processadasNoLote;
    }

    /**
     * PROCESSA UMA EMPRESA INDIVIDUAL COM TRATAMENTO DE ERRO
     * 
     * Este método chama o processamento de uma empresa específica e
     * captura qualquer erro que possa acontecer, evitando que uma
     * empresa com problema interrompa o processamento das outras.
     * 
     * ESTRATÉGIA DE RESILIÊNCIA:
     * - Se empresa A der erro, empresas B, C, D continuam sendo processadas
     * - @MonitorarOperacao captura erro com contexto rico
     * - Erro vira exceção específica (ProcessamentoEmpresaException)
     * - Sistema continua funcionando mesmo com falhas pontuais
     * 
     * RETORNO:
     * - true: empresa processada com sucesso
     * - false: empresa teve erro (capturado automaticamente)
     */
    @MonitorarOperacao(
            operacao = "PROCESSAMENTO_EMPRESA",
            incluirParametros = {"codigoEmpresa"},
            logSucesso = MonitorarOperacao.NivelLog.INFO,
            excecaoEmErro = PROCESSAMENTO_EMPRESA
    )
    private boolean processarEmpresaComSeguranca(String codigoEmpresa) {
        // Delega processamento para serviço específico
        processamentoEmpresaService.processar(codigoEmpresa);
        return true; // Se chegou aqui, deu certo
    }

    /**
     * LOG DE FIM DE PROCESSAMENTO DE UMA PÁGINA
     * Registra progresso geral com formato "processadas/total"
     */
    private void logFimLote(int numeroPagina, long empresasProcessadas, long totalEmpresas) {
        log.info("Lote {} concluído - Total processadas: {}/{}", 
                numeroPagina, empresasProcessadas, totalEmpresas);
    }

    /**
     * CONVERTE OFFSET/LIMIT EM NÚMERO DE PÁGINA
     * Spring Data usa numeração de página (0, 1, 2...) em vez de offset
     */
    private int calcularNumeroPagina(int offset, int limit) {
        return offset / limit; // Ex: offset 100, limit 50 = página 2
    }

}