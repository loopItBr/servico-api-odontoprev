package com.odontoPrev.odontoPrev.application.usecase;

import com.odontoPrev.odontoPrev.application.port.in.ExecutarTarefaUseCase;
import com.odontoPrev.odontoPrev.domain.service.Tarefa;
import org.springframework.stereotype.Service;


@Service
public class TarefaService implements ExecutarTarefaUseCase {

    private final Tarefa tarefa;

    public TarefaService(Tarefa tarefa) {
        this.tarefa = tarefa;
    }

    @Override
    public void executar() {
        tarefa.executarLogica();
    }
}
