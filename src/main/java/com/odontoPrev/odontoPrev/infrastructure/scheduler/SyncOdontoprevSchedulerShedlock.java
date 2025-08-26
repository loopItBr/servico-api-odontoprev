package com.odontoPrev.odontoPrev.infrastructure.scheduler;

import com.odontoPrev.odontoPrev.domain.service.SincronizacaoOdontoprevService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * Scheduler alternativo usando ShedLock para ambientes distribuídos.
 * 
 * Para ativar, adicione as propriedades:
 * - odontoprev.sync.shedlock.enabled=true
 * - Dependência do ShedLock no pom.xml
 * 
 * Exemplo de uso:
 * @SchedulerLock(name = "syncOdontoprevScheduler", lockAtMostFor = "25m", lockAtLeastFor = "5m")
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(value = "odontoprev.sync.shedlock.enabled", havingValue = "true")
public class SyncOdontoprevSchedulerShedlock {

    private final SincronizacaoOdontoprevService sincronizacaoService;

    @Scheduled(cron = "${odontoprev.sync.cron:0 */30 * * * *}")
    // @SchedulerLock(name = "syncOdontoprevScheduler", lockAtMostFor = "25m", lockAtLeastFor = "5m")
    public void executarSincronizacaoOdontoprev() {
        LocalDateTime inicioScheduler = LocalDateTime.now();
        log.info("Iniciando scheduler de sincronização OdontoPrev com ShedLock - {}", inicioScheduler);

        try {
            sincronizacaoService.executarSincronizacao();
            log.info("Scheduler ShedLock executado com sucesso");
        } catch (Exception e) {
            log.error("Erro durante a execução da sincronização OdontoPrev com ShedLock: {}", 
                    e.getMessage(), e);
        }
    }
}