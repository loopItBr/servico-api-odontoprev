package com.odontoPrev.odontoPrev.infrastructure.client.service;

import com.odontoPrev.odontoPrev.domain.service.ProcessamentoLoteService;
import com.odontoPrev.odontoPrev.domain.service.SincronizacaoOdontoprevService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

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
    public void executarSincronizacao() {
        LocalDateTime inicioSincronizacao = LocalDateTime.now();
        log.info("Iniciando sincronização com OdontoPrev - {}", inicioSincronizacao);
        log.info("Configurações: Batch size = {}, Max threads = {}", tamanhoBatch, maxThreads);

        try {
            long totalEmpresas = contarTotalEmpresasUmaVez();
            
            if (totalEmpresas == 0) {
                log.info("Nenhuma empresa encontrada para sincronização");
                return;
            }
            
            log.info("Total de {} empresas serão processadas em lotes", totalEmpresas);
            processamentoLoteService.processarEmpresasEmLotes(tamanhoBatch, maxThreads, totalEmpresas);
            
        } catch (Exception e) {
            log.error("Erro durante a sincronização com OdontoPrev: {}", e.getMessage(), e);
            throw new RuntimeException("Falha na execução da sincronização", e);
        } finally {
            LocalDateTime fimSincronizacao = LocalDateTime.now();
            long duracao = java.time.Duration.between(inicioSincronizacao, fimSincronizacao).toMillis();
            log.info("Sincronização com OdontoPrev finalizada - {} (duração: {}ms)", 
                    fimSincronizacao, duracao);
        }
    }

    private long contarTotalEmpresasUmaVez() {
        log.debug("Contando total de empresas disponíveis - execução única");
        return processamentoLoteService.contarTotalEmpresas();
    }
}