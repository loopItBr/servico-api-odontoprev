package com.odontoPrev.odontoPrev.infrastructure.scheduler;

import com.odontoPrev.odontoPrev.infrastructure.service.SyncOdontoprevService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.concurrent.ExecutorService;

@Slf4j
@Component
@RequiredArgsConstructor
public class SyncOdontoprevScheduler {

    private final SyncOdontoprevService syncOdontoprevService;
    private final ExecutorService executorService;

    @Scheduled(cron = "0 * * * * *")
    public void executarSincronizacaoOdontoprev() {
        log.info("Iniciando scheduler de sincronização OdontoPrev - {}", 
            java.time.LocalDateTime.now());

        executorService.submit(() -> {
            try {
                syncOdontoprevService.executarSincronizacao();
            } catch (Exception e) {
                log.error("Erro durante a execução do scheduler de sincronização OdontoPrev", e);
            }
        });
    }
}