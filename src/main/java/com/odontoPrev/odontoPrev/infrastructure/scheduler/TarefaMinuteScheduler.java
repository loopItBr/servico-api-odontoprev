package com.odontoPrev.odontoPrev.infrastructure.scheduler;

import com.odontoPrev.odontoPrev.application.port.in.ExecutarTarefaUseCase;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class TarefaMinuteScheduler {

    private final ExecutarTarefaUseCase executarTarefaUseCase;

    public TarefaMinuteScheduler(ExecutarTarefaUseCase executarTarefaUseCase) {
        this.executarTarefaUseCase = executarTarefaUseCase;
    }

    //cron job para executar de minuto em minuto
    @Scheduled(cron = "0 * * * * *")
    public void executarTarefa(){
        executarTarefaUseCase.executar();
    }
}
