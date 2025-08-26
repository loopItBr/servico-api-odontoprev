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
 * AGENDADOR AUTOMÁTICO PARA SINCRONIZAÇÃO COM ODONTOPREV
 * 
 * FUNÇÃO PRINCIPAL:
 * Esta classe é o "coração" do sistema de sincronização. Ela funciona como um 
 * despertador que executa automaticamente a sincronização de dados com a 
 * OdontoPrev em intervalos regulares (ex: a cada 30 minutos).
 * 
 * COMO FUNCIONA:
 * 1. AGENDAMENTO: Spring executa automaticamente baseado no cron configurado
 * 2. CONTROLE DE CONCORRÊNCIA: Evita que duas sincronizações rodem ao mesmo tempo
 * 3. EXECUÇÃO ASSÍNCRONA: Roda em thread separada para não travar o sistema
 * 4. MONITORAMENTO: Usa anotação @MonitorarOperacao para logs automáticos
 * 5. RECUPERAÇÃO: Se der erro, libera o controle para próxima execução
 * 
 * EXEMPLO DE FUNCIONAMENTO:
 * - 09:00 - Sistema inicia primeira sincronização
 * - 09:05 - Sincronização ainda rodando, pula próxima execução  
 * - 09:10 - Sincronização terminou, inicia nova execução
 * - 09:15 - Nova sincronização em andamento
 * 
 * CONFIGURAÇÃO:
 * O agendamento é controlado pela propriedade:
 * odontoprev.sync.cron=0 30 * * * *  (a cada 30 minutos)
 * 
 * COMPONENTES PRINCIPAIS:
 * - SincronizacaoOdontoprevService: faz o trabalho real de sincronização
 * - ExecutorService: gerencia threads para execução paralela
 * - AtomicBoolean: controla se já tem sincronização em execução
 * 
 * SEGURANÇA:
 * - Nunca executa duas sincronizações simultaneamente
 * - Se der erro, não trava o agendamento futuro
 * - Logs detalhados para investigação de problemas
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SyncOdontoprevScheduler {

    // Serviço que executa a lógica real de sincronização
    private final SincronizacaoOdontoprevService sincronizacaoService;
    
    // Pool de threads para execução assíncrona (não trava thread principal)
    private final ExecutorService executorService;
    
    // Controla se já tem uma sincronização em execução
    // AtomicBoolean = thread-safe (múltiplas threads podem acessar sem problema)
    private final AtomicBoolean sincronizacaoEmExecucao = new AtomicBoolean(false);

    /**
     * MÉTODO PRINCIPAL - EXECUTADO AUTOMATICAMENTE PELO SPRING
     * 
     * Este método é chamado automaticamente pelo Spring de acordo com o cron configurado.
     * É o ponto de entrada de toda sincronização.
     * 
     * FLUXO DE EXECUÇÃO:
     * 1. Spring chama este método no horário agendado
     * 2. Verifica se já tem sincronização rodando
     * 3. Se não tem, inicia nova sincronização em thread separada
     * 4. Se já tem, pula esta execução (evita sobrecarregar sistema)
     * 5. Monitora resultado da execução assíncrona
     * 
     * ANOTAÇÕES:
     * @Scheduled = Spring executa automaticamente conforme cron
     * @MonitorarOperacao = Adiciona logs e monitoramento automático
     * 
     * CRON EXPLICADO:
     * "${odontoprev.sync.cron:0 * * * * *}" significa:
     * - Usar valor da propriedade odontoprev.sync.cron
     * - Se não estiver configurada, usar "0 * * * * *" (a cada minuto)
     * - Formato: segundo minuto hora dia mês dia_da_semana
     */
    @Scheduled(cron = "${odontoprev.sync.cron:0 * * * * *}")
    @MonitorarOperacao(
            operacao = "INICIALIZACAO_SCHEDULER",
            incluirThread = true,
            excecaoEmErro = INICIALIZACAO_SCHEDULER
    )
    public void executarSincronizacaoOdontoprev() {
        // Primeiro, verifica se já tem sincronização rodando
        if (sincronizacaoJaEstaEmExecucao()) {
            log.warn("Sincronização já está em execução, pulando esta execução");
            return; // Sai do método sem fazer nada
        }

        // Se chegou aqui, não tem sincronização rodando
        // Inicia execução em thread separada (assíncrona)
        CompletableFuture
                .runAsync(this::executarSincronizacaoComControle, executorService)
                .whenComplete((result, throwable) -> {
                    // Este código executa quando a sincronização termina (sucesso ou erro)
                    if (throwable != null) {
                        // Se deu erro, registra no log
                        log.error("Erro durante execução assíncrona", throwable);
                    }
                    // Se foi sucesso, não precisa fazer nada especial
                });
    }

    /**
     * VERIFICA SE JÁ TEM SINCRONIZAÇÃO RODANDO E "RESERVA" EXECUÇÃO
     * 
     * Este método faz duas coisas importantes:
     * 1. VERIFICA: Se já tem sincronização em execução
     * 2. RESERVA: Se não tem, marca como "em execução" para evitar concorrência
     * 
     * FUNCIONAMENTO TÉCNICO:
     * Usa compareAndSet que é uma operação "atômica" (indivisível):
     * - Se valor atual é false (não executando), muda para true e retorna true
     * - Se valor atual é true (já executando), não muda nada e retorna false
     * 
     * EXEMPLO PRÁTICO:
     * - Thread 1 chama: compareAndSet(false, true) → mudou para true, retorna true
     * - Thread 2 chama: compareAndSet(false, true) → valor já é true, retorna false
     * 
     * @return true se JÁ ESTAVA em execução, false se COMEÇOU agora
     */
    private boolean sincronizacaoJaEstaEmExecucao() {
        // Tenta mudar de false para true. Se conseguiu, significa que não estava executando.
        // Se não conseguiu, significa que já estava executando.
        boolean jaEmExecucao = !sincronizacaoEmExecucao.compareAndSet(false, true);
        
        if (jaEmExecucao) {
            // Se já estava executando, registra no log
            log.warn("Tentativa de execução concorrente detectada - Já em execução");
        }
        
        return jaEmExecucao;
    }

    /**
     * EXECUTA A SINCRONIZAÇÃO COM CONTROLE DE ESTADO
     * 
     * Este método é executado em thread separada (assíncrona) e faz:
     * 1. CHAMA o serviço real de sincronização
     * 2. GARANTE que sempre libera o controle no final (finally)
     * 3. MONITORA a execução com logs automáticos
     * 
     * IMPORTÂNCIA DO FINALLY:
     * Mesmo que dê erro na sincronização, SEMPRE executa liberarControleExecucao()
     * Isso evita que o sistema "trave" em estado "executando" para sempre.
     * 
     * ANOTAÇÃO @MonitorarOperacao:
     * Adiciona automaticamente logs de início/fim, captura de erros, 
     * medição de tempo, etc.
     */
    @MonitorarOperacao(
            operacao = "EXECUCAO_SINCRONIZACAO",
            incluirThread = true,
            excecaoEmErro = PROCESSAMENTO_LOTE
    )
    private void executarSincronizacaoComControle() {
        try {
            // Chama o serviço que faz o trabalho real de sincronização
            sincronizacaoService.executarSincronizacao();
        } finally {
            // SEMPRE executa, mesmo se der erro
            // Libera o controle para permitir próximas execuções
            liberarControleExecucao();
        }
    }

    /**
     * LIBERA O CONTROLE DE EXECUÇÃO PARA PRÓXIMAS SINCRONIZAÇÕES
     * 
     * Este método "destrava" o sistema, permitindo que future sincronizações
     * possam ser executadas.
     * 
     * FUNCIONAMENTO:
     * 1. Tenta mudar o estado de true (executando) para false (livre)
     * 2. Se conseguiu mudar, tudo certo
     * 3. Se não conseguiu, significa estado inconsistente (BUG!)
     * 
     * ESTADO INCONSISTENTE:
     * Se chegou aqui mas o valor não era true, significa que algo deu
     * muito errado no controle de concorrência. Isso é um bug grave
     * que precisa ser investigado.
     */
    private void liberarControleExecucao() {
        // Tenta mudar de true (executando) para false (livre)
        boolean liberado = sincronizacaoEmExecucao.compareAndSet(true, false);
        
        if (!liberado) {
            // Se não conseguiu liberar, é porque o estado não era "true"
            // Isso é um bug grave no controle de concorrência!
            log.error("Estado inconsistente do controle de execução detectado");
            throw new IllegalStateException("Estado inconsistente detectado ao liberar controle de execução");
        }
        
        // Se chegou aqui, liberou com sucesso
        log.debug("Controle de execução liberado");
    }
}