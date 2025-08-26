package com.odontoPrev.odontoPrev.infrastructure.scheduler;

import com.odontoPrev.odontoPrev.domain.service.SincronizacaoOdontoprevService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;

/**
 * Scheduler responsável por executar a sincronização periódica com OdontoPrev.
 * 
 * Utiliza ExecutorService para execução assíncrona evitando bloqueio do scheduler.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SyncOdontoprevScheduler {

    private final SincronizacaoOdontoprevService sincronizacaoService;
    private final ExecutorService executorService;

    @Scheduled(cron = "${odontoprev.sync.cron:0 * * * * *}")
    public void executarSincronizacaoOdontoprev() {
        LocalDateTime inicioScheduler = LocalDateTime.now();
        log.info("Iniciando scheduler de sincronização OdontoPrev - {}", inicioScheduler);

        sincronizacaoService.executarSincronizacao();

       /* CompletableFuture
                .runAsync(this::executarSincronizacao, executorService)
                .whenComplete((result, throwable) -> {
                    if (throwable != null) {
                        log.error("Erro durante execução assíncrona do scheduler: {}", 
                                throwable.getMessage(), throwable);
                    } else {
                        log.debug("Scheduler executado com sucesso");
                    }
                });*/
    }

    private void executarSincronizacao() {
        try {
            sincronizacaoService.executarSincronizacao();
        } catch (Exception e) {
            log.error("Erro durante a execução da sincronização OdontoPrev: {}", 
                    e.getMessage(), e);
        }
    }
}