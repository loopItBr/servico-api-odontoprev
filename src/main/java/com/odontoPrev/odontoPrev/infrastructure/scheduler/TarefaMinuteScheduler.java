package com.odontoPrev.odontoPrev.infrastructure.scheduler;

import com.odontoPrev.odontoPrev.application.port.in.ExecutarTarefaUseCase;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Component
public class TarefaMinuteScheduler {

    private final ExecutarTarefaUseCase executarTarefaUseCase;
    private final ExecutorService executorService;  //o ExecutorService é uma interface para execução de tarefas assíncronas

    public TarefaMinuteScheduler(ExecutarTarefaUseCase executarTarefaUseCase, ExecutorService executorService) {
        this.executarTarefaUseCase = executarTarefaUseCase;
        this.executorService = executorService;
    }

    //cron job para executar de minuto em minuto
    //o @Scheduled permite agendar métodos de execução automática
    @Scheduled(cron = "0 * * * * *")
    public void executarTarefa(){
        System.out.println("Agendamento de tarefa iniciado: " + java.time.LocalDateTime.now());

        executorService.submit(() -> {
            try {
                executarTarefaUseCase.executar();
            }catch (Exception e){
                System.out.println("Ouve um erro ao executar esta tarefa: " + e.getMessage());
                e.printStackTrace();
            }
        });
    }
}
