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
 * Implementação principal do serviço de sincronização com OdontoPrev.
 * Responsável por orquestrar o processo de sincronização usando processamento em lotes.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SincronizacaoOdontoprevServiceImpl implements SincronizacaoOdontoprevService {

    private final ProcessamentoLoteService processamentoLoteService;
    
    @Value("${odontoprev.sync.batch-size:50}")
    private int tamanhoBatch;
    
    @Value("${odontoprev.sync.max-threads:5}")
    private int maxThreads;

    @Override
    @Transactional
    @MonitorarOperacao(
            operacao = "SINCRONIZACAO_COMPLETA",
            excecaoEmErro = PROCESSAMENTO_LOTE
    )
    public void executarSincronizacao() {
        validarConfiguracoes();
        
        long totalEmpresas = contarTotalEmpresasUmaVez();
        
        if (totalEmpresas == 0) {
            log.info("Nenhuma empresa encontrada para sincronização");
            return;
        }
        
        log.info("Processando {} empresas em lotes de {}", totalEmpresas, tamanhoBatch);
        processamentoLoteService.processarEmpresasEmLotes(tamanhoBatch, maxThreads, totalEmpresas);
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