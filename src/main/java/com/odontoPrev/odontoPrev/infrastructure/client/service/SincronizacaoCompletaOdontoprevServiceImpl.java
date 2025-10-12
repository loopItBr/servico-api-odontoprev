package com.odontoPrev.odontoPrev.infrastructure.client.service;

import com.odontoPrev.odontoPrev.domain.service.*;
import com.odontoPrev.odontoPrev.infrastructure.aop.MonitorarOperacao;
import com.odontoPrev.odontoPrev.infrastructure.repository.IntegracaoOdontoprevAlteracaoRepository;
import com.odontoPrev.odontoPrev.infrastructure.repository.IntegracaoOdontoprevExclusaoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import static com.odontoPrev.odontoPrev.infrastructure.aop.MonitorarOperacao.TipoExcecao.*;

/**
 * SERVIÇO PARA SINCRONIZAÇÃO COMPLETA COM ODONTOPREV
 * 
 * FUNÇÃO PRINCIPAL:
 * Esta classe coordena todo o processo de sincronização incluindo:
 * 1. ADIÇÃO: empresas novas (view padrão)
 * 2. ALTERAÇÃO: empresas modificadas (VW_INTEGRACAO_ODONTOPREV_ALT)
 * 3. EXCLUSÃO: empresas inativadas (VW_INTEGRACAO_ODONTOPREV_EXC)
 * 
 * ESTRATÉGIA DE PROCESSAMENTO:
 * - Processa cada tipo de operação em sequência
 * - Usa os mesmos parâmetros de lote e threads para consistência
 * - Monitora progresso de cada tipo de operação
 * - Trata erros sem interromper outros tipos
 * 
 * CONFIGURAÇÕES:
 * - batch-size: quantas empresas processar por vez
 * - max-threads: quantas threads usar em paralelo
 * 
 * ORDEM DE PROCESSAMENTO:
 * 1. Exclusões (para remover dados obsoletos primeiro)
 * 2. Alterações (para atualizar dados existentes)
 * 3. Adições (para incluir novos dados)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SincronizacaoCompletaOdontoprevServiceImpl implements SincronizacaoCompletaOdontoprevService {

    // Serviços de sincronização específicos
    private final SincronizacaoOdontoprevService sincronizacaoAdicoes;
    private final ProcessamentoLoteService processamentoLoteService;
    private final ProcessamentoEmpresaAlteracaoService processamentoAlteracoes;
    private final ProcessamentoEmpresaExclusaoService processamentoExclusoes;
    
    // Repositórios para contagem
    private final IntegracaoOdontoprevAlteracaoRepository alteracaoRepository;
    private final IntegracaoOdontoprevExclusaoRepository exclusaoRepository;
    
    // Configurações
    @Value("${odontoprev.sync.batch-size:50}")
    private int tamanhoBatch;
    
    @Value("${odontoprev.sync.max-threads:5}")
    private int maxThreads;

    /**
     * MÉTODO PRINCIPAL - EXECUTA SINCRONIZAÇÃO COMPLETA
     * 
     * Coordena todo o processo de sincronização incluindo adições, alterações e exclusões.
     * Processa em ordem: exclusões → alterações → adições
     * 
     * NOTA: Não usa @Transactional pois é executado de forma assíncrona.
     * Cada serviço individual gerencia suas próprias transações.
     */
    @Override
    @MonitorarOperacao(
            operacao = "SINCRONIZACAO_COMPLETA",
            excecaoEmErro = PROCESSAMENTO_LOTE
    )
    public void executarSincronizacaoCompleta() {
        log.info("Iniciando sincronização completa com OdontoPrev");
        
        // 1. Processa exclusões primeiro (remove dados obsoletos)
        executarSincronizacaoExclusoes();
        
        // 2. Processa alterações (atualiza dados existentes)
        executarSincronizacaoAlteracoes();
        
        // 3. Processa adições (inclui novos dados)
        sincronizacaoAdicoes.executarSincronizacao();
        
        log.info("Sincronização completa finalizada com sucesso");
    }

    /**
     * EXECUTA SINCRONIZAÇÃO APENAS DE ALTERAÇÕES
     * 
     * Processa empresas que tiveram dados modificados e precisam ser atualizadas.
     * 
     * NOTA: Não usa @Transactional pois pode ser executado de forma assíncrona.
     * Cada serviço individual gerencia suas próprias transações.
     */
    @Override
    @MonitorarOperacao(
            operacao = "SINCRONIZACAO_ALTERACOES",
            excecaoEmErro = PROCESSAMENTO_LOTE
    )
    public void executarSincronizacaoAlteracoes() {
        log.info("Iniciando sincronização de alterações");
        
        // Conta total de empresas alteradas
        long totalAlteracoes = contarTotalAlteracoes();
        
        if (totalAlteracoes == 0) {
            log.info("Nenhuma empresa alterada encontrada para sincronização");
            return;
        }
        
        log.info("Processando {} empresas alteradas em lotes de {}", totalAlteracoes, tamanhoBatch);
        
        // Processa alterações em lotes
        processarAlteracoesEmLotes(totalAlteracoes);
        
        log.info("Sincronização de alterações finalizada");
    }

    /**
     * EXECUTA SINCRONIZAÇÃO APENAS DE EXCLUSÕES
     * 
     * Processa empresas que foram inativadas/excluídas e precisam ser removidas.
     * 
     * NOTA: Não usa @Transactional pois pode ser executado de forma assíncrona.
     * Cada serviço individual gerencia suas próprias transações.
     */
    @Override
    @MonitorarOperacao(
            operacao = "SINCRONIZACAO_EXCLUSOES",
            excecaoEmErro = PROCESSAMENTO_LOTE
    )
    public void executarSincronizacaoExclusoes() {
        log.info("Iniciando sincronização de exclusões");
        
        // Conta total de empresas excluídas
        long totalExclusoes = contarTotalExclusoes();
        
        if (totalExclusoes == 0) {
            log.info("Nenhuma empresa excluída encontrada para sincronização");
            return;
        }
        
        log.info("Processando {} empresas excluídas em lotes de {}", totalExclusoes, tamanhoBatch);
        
        // Processa exclusões em lotes
        processarExclusoesEmLotes(totalExclusoes);
        
        log.info("Sincronização de exclusões finalizada");
    }

    /**
     * CONTA TOTAL DE EMPRESAS ALTERADAS
     */
    @MonitorarOperacao(
            operacao = "CONTAGEM_ALTERACOES",
            logSucesso = MonitorarOperacao.NivelLog.INFO,
            excecaoEmErro = CONSULTA_EMPRESAS
    )
    private long contarTotalAlteracoes() {
        return alteracaoRepository.contarTotalEmpresasAlteradas();
    }

    /**
     * CONTA TOTAL DE EMPRESAS EXCLUÍDAS
     */
    @MonitorarOperacao(
            operacao = "CONTAGEM_EXCLUSOES",
            logSucesso = MonitorarOperacao.NivelLog.INFO,
            excecaoEmErro = CONSULTA_EMPRESAS
    )
    private long contarTotalExclusoes() {
        return exclusaoRepository.contarTotalEmpresasExcluidas();
    }

    /**
     * PROCESSA ALTERAÇÕES EM LOTES
     * 
     * Implementa processamento em lotes para empresas alteradas.
     */
    private void processarAlteracoesEmLotes(long totalAlteracoes) {
        int numeroPagina = 0;
        long empresasProcessadas = 0;
        
        while (true) {
            // Busca próxima página de empresas alteradas
            var loteAtual = alteracaoRepository.buscarCodigosEmpresasAlteradasPaginado(
                org.springframework.data.domain.PageRequest.of(numeroPagina, tamanhoBatch)
            );
            
            if (loteAtual.isEmpty()) {
                break; // Não há mais empresas
            }
            
            log.info("Processando lote de alterações {} - {} empresas", 
                    numeroPagina + 1, loteAtual.size());
            
            // Processa cada empresa do lote
            long processadasNoLote = processarLoteAlteracoes(loteAtual);
            empresasProcessadas += processadasNoLote;
            
            numeroPagina++;
            
            log.info("Lote de alterações {} concluído - Total processadas: {}/{}", 
                    numeroPagina, empresasProcessadas, totalAlteracoes);
        }
    }

    /**
     * PROCESSA EXCLUSÕES EM LOTES
     * 
     * Implementa processamento em lotes para empresas excluídas.
     */
    private void processarExclusoesEmLotes(long totalExclusoes) {
        int numeroPagina = 0;
        long empresasProcessadas = 0;
        
        while (true) {
            // Busca próxima página de empresas excluídas
            var loteAtual = exclusaoRepository.buscarCodigosEmpresasExcluidasPaginado(
                org.springframework.data.domain.PageRequest.of(numeroPagina, tamanhoBatch)
            );
            
            if (loteAtual.isEmpty()) {
                break; // Não há mais empresas
            }
            
            log.info("Processando lote de exclusões {} - {} empresas", 
                    numeroPagina + 1, loteAtual.size());
            
            // Processa cada empresa do lote
            long processadasNoLote = processarLoteExclusoes(loteAtual);
            empresasProcessadas += processadasNoLote;
            
            numeroPagina++;
            
            log.info("Lote de exclusões {} concluído - Total processadas: {}/{}", 
                    numeroPagina, empresasProcessadas, totalExclusoes);
        }
    }

    /**
     * PROCESSA LOTE DE ALTERAÇÕES
     * 
     * Processa cada empresa alterada do lote atual.
     */
    private long processarLoteAlteracoes(java.util.List<String> codigosEmpresas) {
        long processadasNoLote = 0;
        
        for (String codigoEmpresa : codigosEmpresas) {
            try {
                // Chama o serviço de processamento de alterações
                processamentoAlteracoes.processar(codigoEmpresa);
                processadasNoLote++;
            } catch (Exception e) {
                log.error("Erro ao processar alteração da empresa {}: {}", codigoEmpresa, e.getMessage());
                // Continua processando outras empresas
            }
        }
        
        return processadasNoLote;
    }

    /**
     * PROCESSA LOTE DE EXCLUSÕES
     * 
     * Processa cada empresa excluída do lote atual.
     */
    private long processarLoteExclusoes(java.util.List<String> codigosEmpresas) {
        long processadasNoLote = 0;
        
        for (String codigoEmpresa : codigosEmpresas) {
            try {
                // Chama o serviço de processamento de exclusões
                processamentoExclusoes.processar(codigoEmpresa);
                processadasNoLote++;
            } catch (Exception e) {
                log.error("Erro ao processar exclusão da empresa {}: {}", codigoEmpresa, e.getMessage());
                // Continua processando outras empresas
            }
        }
        
        return processadasNoLote;
    }
}
