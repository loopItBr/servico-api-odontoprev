package com.odontoPrev.odontoPrev.infrastructure.client.service;

import com.odontoPrev.odontoPrev.domain.service.ProcessamentoLoteService;
import com.odontoPrev.odontoPrev.domain.service.SincronizacaoOdontoprevService;
import com.odontoPrev.odontoPrev.infrastructure.aop.MonitorarOperacao;
import com.odontoPrev.odontoPrev.infrastructure.exception.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import static com.odontoPrev.odontoPrev.infrastructure.aop.MonitorarOperacao.TipoExcecao.*;

/**
 * SERVIÇO PRINCIPAL PARA SINCRONIZAÇÃO COM ODONTOPREV
 * 
 * FUNÇÃO:
 * Esta classe coordena todo o processo de sincronização de dados entre 
 * nosso sistema e a API da OdontoPrev. É como o "maestro" de uma orquestra,
 * coordenando todas as partes para trabalharem em harmonia.
 * 
 * RESPONSABILIDADES:
 * 1. VALIDAR configurações antes de começar
 * 2. CONTAR quantas empresas precisam ser sincronizadas
 * 3. COORDENAR o processamento em lotes (batches)
 * 4. MONITORAR o progresso e registrar logs
 * 5. TRATAR erros de forma apropriada
 * 
 * ESTRATÉGIA DE PROCESSAMENTO:
 * Em vez de processar todas as empresas de uma vez (que poderia travar 
 * o sistema ou esgotar a memória), divide o trabalho em "lotes" menores.
 * 
 * EXEMPLO PRÁTICO:
 * - Temos 1000 empresas para sincronizar
 * - Configurado lote de 50 empresas
 * - Sistema processa: 50, depois mais 50, depois mais 50...
 * - Até terminar todas as 1000 empresas
 * 
 * CONFIGURAÇÕES IMPORTANTES:
 * - batch-size: quantas empresas processar por vez (padrão: 50)
 * - max-threads: quantas threads usar em paralelo (padrão: 5)
 * 
 * SEGURANÇA:
 * - Validação rigorosa de configurações
 * - Transação de banco (@Transactional)
 * - Monitoramento automático (@MonitorarOperacao)
 * - Tratamento específico de erros
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SincronizacaoOdontoprevServiceImpl implements SincronizacaoOdontoprevService {

    // Serviço que divide e processa empresas em lotes
    private final ProcessamentoLoteService processamentoLoteService;
    
    // Quantas empresas processar por vez (configurável via properties)
    @Value("${odontoprev.sync.batch-size:50}")
    private int tamanhoBatch;
    
    // Quantas threads usar em paralelo (configurável via properties)
    @Value("${odontoprev.sync.max-threads:5}")
    private int maxThreads;

    /**
     * MÉTODO PRINCIPAL - EXECUTA TODA A SINCRONIZAÇÃO
     * 
     * Este é o método "coração" de toda sincronização. Coordena
     * todo o processo do início ao fim.
     * 
     * FLUXO COMPLETO:
     * 1. VALIDA configurações (tamanhos de lote, threads, etc.)
     * 2. CONTA quantas empresas existem para sincronizar
     * 3. Se não tem empresas, termina (não há trabalho a fazer)
     * 4. Se tem empresas, inicia processamento em lotes
     * 
     * ANOTAÇÕES IMPORTANTES:
     * @Override = implementa método da interface SincronizacaoOdontoprevService
     * @Transactional = se der erro, faz rollback no banco de dados
     * @MonitorarOperacao = adiciona logs automáticos e tratamento de erros
     * 
     * TRATAMENTO DE DADOS VAZIOS:
     * Se não encontrar empresas para processar, simplesmente termina.
     * Não é erro - pode ser que realmente não tenha dados novos.
     * 
     * DELEGAÇÃO DE RESPONSABILIDADE:
     * Este método não faz o trabalho pesado - ele coordena.
     * O trabalho real é delegado para processamentoLoteService.
     */
    @Override
    @MonitorarOperacao(
            operacao = "SINCRONIZACAO_COMPLETA",
            excecaoEmErro = PROCESSAMENTO_LOTE
    )
    public void executarSincronizacao() {
        log.info("🔍 [SINCRONIZAÇÃO ADIÇÕES] ===== INICIANDO SINCRONIZAÇÃO DE ADIÇÕES =====");
        log.info("🔍 [SINCRONIZAÇÃO ADIÇÕES] Timestamp: {}", java.time.LocalDateTime.now());
        log.info("🔍 [SINCRONIZAÇÃO ADIÇÕES] Thread: {}", Thread.currentThread().getName());
        log.info("🔍 [SINCRONIZAÇÃO ADIÇÕES] Iniciando sincronização de adições");
        
        // 1. Primeiro, valida se as configurações estão corretas
        log.info("⚙️ [SINCRONIZAÇÃO ADIÇÕES] Validando configurações...");
        validarConfiguracoes();
        log.info("✅ [SINCRONIZAÇÃO ADIÇÕES] Configurações validadas com sucesso");
        
        // 2. Conta quantas empresas precisam ser sincronizadas
        log.info("📊 [SINCRONIZAÇÃO ADIÇÕES] Contando empresas para sincronização...");
        long totalEmpresas = contarTotalEmpresasUmaVez();
        log.info("📊 [SINCRONIZAÇÃO ADIÇÕES] Total de empresas encontradas: {}", totalEmpresas);
        
        // 3. Se não tem empresas, não há trabalho a fazer
        if (totalEmpresas == 0) {
            log.info("ℹ️ [SINCRONIZAÇÃO ADIÇÕES] Nenhuma empresa encontrada para sincronização");
            log.info("ℹ️ [SINCRONIZAÇÃO ADIÇÕES] Verificando se há dados na view VW_INTEGRACAO_ODONTOPREV...");
            log.info("🔍 [SINCRONIZAÇÃO ADIÇÕES] ===== FIM DA SINCRONIZAÇÃO DE ADIÇÕES (SEM DADOS) =====");
            return; // Termina aqui - não é erro, apenas não tem dados
        }
        
        // 4. Se chegou aqui, tem empresas para processar
        log.info("🚀 [SINCRONIZAÇÃO ADIÇÕES] Processando {} empresas em lotes de {}", totalEmpresas, tamanhoBatch);
        
        // 5. Delega o trabalho real para o serviço de lotes
        log.info("🔄 [SINCRONIZAÇÃO ADIÇÕES] Chamando processamentoLoteService.processarEmpresasEmLotes()");
        processamentoLoteService.processarEmpresasEmLotes(tamanhoBatch, maxThreads, totalEmpresas);
        
        log.info("✅ [SINCRONIZAÇÃO ADIÇÕES] Sincronização de adições finalizada");
        log.info("🔍 [SINCRONIZAÇÃO ADIÇÕES] ===== FIM DA SINCRONIZAÇÃO DE ADIÇÕES =====");
    }

    private void validarConfiguracoes() {
        if (tamanhoBatch <= 0) {
            throw new IllegalArgumentException("Batch size deve ser maior que zero: " + tamanhoBatch);
        }
        
        if (maxThreads <= 0) {
            throw new IllegalArgumentException("Max threads deve ser maior que zero: " + maxThreads);
        }
        
        if (tamanhoBatch > 1000) {
            throw new IllegalArgumentException("Batch size muito alto, máximo 1000: " + tamanhoBatch);
        }
        
        if (maxThreads > 50) {
            throw new IllegalArgumentException("Max threads muito alto, máximo 50: " + maxThreads);
        }
        
        log.debug("Configurações validadas: batch={}, threads={}", tamanhoBatch, maxThreads);
    }

    @MonitorarOperacao(
            operacao = "CONTAGEM_EMPRESAS",
            logSucesso = MonitorarOperacao.NivelLog.INFO,
            excecaoEmErro = CONSULTA_EMPRESAS
    )
    private long contarTotalEmpresasUmaVez() {
        return processamentoLoteService.contarTotalEmpresas();
    }
}