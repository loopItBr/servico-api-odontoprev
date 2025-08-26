package com.odontoPrev.odontoPrev.infrastructure.scheduler;

import com.odontoPrev.odontoPrev.domain.service.SincronizacaoOdontoprevService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Scheduler responsável por executar a sincronização periódica com OdontoPrev.
 * 
 * Utiliza controle de concorrência para evitar execuções simultâneas.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SyncOdontoprevScheduler {

    private final SincronizacaoOdontoprevService sincronizacaoService;
    private final ExecutorService executorService;
    private final AtomicBoolean sincronizacaoEmExecucao = new AtomicBoolean(false);

    @Scheduled(cron = "${odontoprev.sync.cron:0 * * * * *}")
    //@Scheduled(cron = "${odontoprev.sync.cron:0 */30 * * * *}")
    public void executarSincronizacaoOdontoprev() {

        if (sincronizacaoJaEstaEmExecucao()) {
            log.warn("Sincronização já está em execução, pulando esta execução agendada");
            return;
        }

        try {
            log.info("Sincronização iniciada");
            sincronizacaoService.executarSincronizacao();
            log.info("Sincronização concluída com sucesso");
        } catch (Exception e) {
            log.error("Erro durante a execução da sincronização OdontoPrev: {}", 
                    e.getMessage(), e);
        } finally {
            liberarControleExecucao();
        }

        /*LocalDateTime inicioScheduler = LocalDateTime.now();
        log.info("Iniciando scheduler de sincronização OdontoPrev - {}", inicioScheduler);

        CompletableFuture
                .runAsync(this::executarSincronizacaoComControle, executorService)
                .whenComplete((result, throwable) -> {
                    if (throwable != null) {
                        log.error("Erro durante execução assíncrona do scheduler: {}", 
                                throwable.getMessage(), throwable);
                    } else {
                        log.debug("Scheduler executado com sucesso");
                    }
                });*/
    }

    private boolean sincronizacaoJaEstaEmExecucao() {
        return !sincronizacaoEmExecucao.compareAndSet(false, true);
    }

    private void executarSincronizacaoComControle() {
        try {
            log.info("Sincronização iniciada");
            sincronizacaoService.executarSincronizacao();
            log.info("Sincronização concluída com sucesso");
        } catch (Exception e) {
            log.error("Erro durante a execução da sincronização OdontoPrev: {}", 
                    e.getMessage(), e);
        } finally {
            liberarControleExecucao();
        }
    }

    private void liberarControleExecucao() {
        sincronizacaoEmExecucao.set(false);
        log.debug("Controle de execução liberado");
    }
}