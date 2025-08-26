package com.odontoPrev.odontoPrev.infrastructure.scheduler;

import com.odontoPrev.odontoPrev.domain.service.SincronizacaoOdontoprevService;
import com.odontoPrev.odontoPrev.infrastructure.aop.MonitorarOperacao;
import com.odontoPrev.odontoPrev.infrastructure.exception.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicBoolean;

import static com.odontoPrev.odontoPrev.infrastructure.aop.MonitorarOperacao.TipoExcecao.*;

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
    @MonitorarOperacao(
            operacao = "INICIALIZACAO_SCHEDULER",
            incluirThread = true,
            excecaoEmErro = INICIALIZACAO_SCHEDULER
    )
    public void executarSincronizacaoOdontoprev() {
        if (sincronizacaoJaEstaEmExecucao()) {
            log.warn("Sincronização já está em execução, pulando esta execução");
            return;
        }

        CompletableFuture
                .runAsync(this::executarSincronizacaoComControle, executorService)
                .whenComplete((result, throwable) -> {
                    if (throwable != null) {
                        log.error("Erro durante execução assíncrona", throwable);
                    }
                });
    }

    private boolean sincronizacaoJaEstaEmExecucao() {
        boolean jaEmExecucao = !sincronizacaoEmExecucao.compareAndSet(false, true);
        
        if (jaEmExecucao) {
            log.warn("Tentativa de execução concorrente detectada - Já em execução");
        }
        
        return jaEmExecucao;
    }

    @MonitorarOperacao(
            operacao = "EXECUCAO_SINCRONIZACAO",
            incluirThread = true,
            excecaoEmErro = PROCESSAMENTO_LOTE
    )
    private void executarSincronizacaoComControle() {
        try {
            sincronizacaoService.executarSincronizacao();
        } finally {
            liberarControleExecucao();
        }
    }

    private void liberarControleExecucao() {
        boolean liberado = sincronizacaoEmExecucao.compareAndSet(true, false);
        
        if (!liberado) {
            log.error("Estado inconsistente do controle de execução detectado");
            throw new IllegalStateException("Estado inconsistente detectado ao liberar controle de execução");
        }
        
        log.debug("Controle de execução liberado");
    }
}