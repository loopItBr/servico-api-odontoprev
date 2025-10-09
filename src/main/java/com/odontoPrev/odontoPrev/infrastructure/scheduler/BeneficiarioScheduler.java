package com.odontoPrev.odontoPrev.infrastructure.scheduler;

import com.odontoPrev.odontoPrev.domain.service.SincronizacaoCompletaBeneficiarioService;
import com.odontoPrev.odontoPrev.infrastructure.aop.MonitorarOperacao;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicBoolean;

import static com.odontoPrev.odontoPrev.infrastructure.aop.MonitorarOperacao.TipoExcecao.*;

/**
 * AGENDADOR AUTOMÁTICO PARA SINCRONIZAÇÃO DE BENEFICIÁRIOS COM ODONTOPREV
 *
 * FUNÇÃO PRINCIPAL:
 * Esta classe é responsável por executar automaticamente a sincronização de
 * beneficiários com a OdontoPrev em intervalos regulares definidos na configuração.
 *
 * COMO FUNCIONA:
 * 1. AGENDAMENTO: Spring executa automaticamente baseado no cron configurado
 * 2. CONTROLE DE CONCORRÊNCIA: Evita que duas sincronizações rodem simultaneamente
 * 3. EXECUÇÃO ASSÍNCRONA: Roda em thread separada para não bloquear sistema
 * 4. MONITORAMENTO: Usa anotação @MonitorarOperacao para logs automáticos
 * 5. RECUPERAÇÃO: Se der erro, libera o controle para próxima execução
 *
 * DIFERENÇA DO EMPRESA SCHEDULER:
 * - Processa beneficiários ao invés de empresas
 * - Usa endpoints específicos de beneficiários na OdontoPrev
 * - Executa procedure SS_PLS_CAD_CARTEIRINHA_ODONTOPREV após inclusões
 * - Registra logs na tabela TB_CONTROLE_SYNC_ODONTOPREV_BENEF
 *
 * EXEMPLO DE FUNCIONAMENTO:
 * - 09:00 - Sistema inicia primeira sincronização de beneficiários
 * - 09:10 - Sincronização ainda rodando, pula próxima execução
 * - 09:15 - Sincronização terminou, inicia nova execução
 * - 09:25 - Nova sincronização em andamento
 *
 * CONFIGURAÇÃO:
 * O agendamento é controlado pela propriedade:
 * odontoprev.sync.beneficiario.cron=0 45 * * * *  (a cada 45 minutos)
 *
 * COMPONENTES PRINCIPAIS:
 * - SincronizacaoCompletaBeneficiarioService: faz o trabalho real de sincronização
 * - ExecutorService: gerencia threads para execução paralela
 * - AtomicBoolean: controla se já tem sincronização em execução
 *
 * SEGURANÇA:
 * - Nunca executa duas sincronizações simultaneamente
 * - Se der erro, não trava o agendamento futuro
 * - Logs detalhados para investigação de problemas
 * - Isolamento completo do scheduler de empresas
 */
@Slf4j
@Component
@ConditionalOnProperty(
    name = "odontoprev.scheduler.beneficiario.enabled",
    havingValue = "true",
    matchIfMissing = true
)
@RequiredArgsConstructor
public class BeneficiarioScheduler {

    // Serviço que executa sincronização completa de beneficiários
    private final SincronizacaoCompletaBeneficiarioService sincronizacaoCompletaService;

    // Pool de threads para execução assíncrona (não trava thread principal)
    private final ExecutorService executorService;

    // Controla se já tem uma sincronização de beneficiários em execução
    // AtomicBoolean = thread-safe (múltiplas threads podem acessar sem problema)
    private final AtomicBoolean sincronizacaoEmExecucao = new AtomicBoolean(false);

    /**
     * MÉTODO PRINCIPAL - EXECUTADO AUTOMATICAMENTE PELO SPRING
     *
     * Este método é chamado automaticamente pelo Spring de acordo com o cron configurado
     * especificamente para beneficiários.
     *
     * FLUXO DE EXECUÇÃO:
     * 1. Spring chama este método no horário agendado
     * 2. Verifica se já tem sincronização de beneficiários rodando
     * 3. Se não tem, inicia nova sincronização em thread separada
     * 4. Se já tem, pula esta execução (evita sobrecarregar sistema)
     * 5. Monitora resultado da execução assíncrona
     *
     * ANOTAÇÕES:
     * @Scheduled = Spring executa automaticamente conforme cron
     * @MonitorarOperacao = Adiciona logs e monitoramento automático
     *
     * CRON PARA BENEFICIÁRIOS:
     * Configurado separadamente do scheduler de empresas para permitir
     * frequências diferentes conforme necessidade do negócio.
     *
     * DIFERENÇA DO EMPRESA SCHEDULER:
     * - Intervalo de execução pode ser diferente
     * - Logs específicos identificam como "BENEFICIARIO"
     * - Controle de execução independente (pode rodar em paralelo com empresas)
     */
    @Scheduled(fixedRate = 10000) // Executa a cada 10 segundos
    @MonitorarOperacao(
            operacao = "INICIALIZACAO_SCHEDULER_BENEFICIARIO",
            incluirThread = true,
            excecaoEmErro = INICIALIZACAO_SCHEDULER
    )
    public void executarSincronizacaoBeneficiarios() {
        // Primeiro, verifica se já tem sincronização de beneficiários rodando
        if (sincronizacaoJaEstaEmExecucao()) {
            log.warn("Sincronização de beneficiários já está em execução, pulando esta execução");
            return; // Sai do método sem fazer nada
        }

        // Se chegou aqui, não tem sincronização de beneficiários rodando
        // Inicia execução em thread separada (assíncrona)
        CompletableFuture
                .runAsync(this::executarSincronizacaoComControle, executorService)
                .whenComplete((result, throwable) -> {
                    // Este código executa quando a sincronização termina (sucesso ou erro)
                    if (throwable != null) {
                        // Se deu erro, registra no log
                        log.error("Erro durante execução assíncrona de sincronização de beneficiários", throwable);
                    }
                    // Se foi sucesso, não precisa fazer nada especial
                });
    }

    /**
     * VERIFICA SE JÁ TEM SINCRONIZAÇÃO DE BENEFICIÁRIOS RODANDO E "RESERVA" EXECUÇÃO
     *
     * Este método faz duas coisas importantes:
     * 1. VERIFICA: Se já tem sincronização de beneficiários em execução
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
     * ISOLAMENTO:
     * Este controle é específico para beneficiários e não interfere com
     * o scheduler de empresas (cada um tem seu próprio AtomicBoolean).
     *
     * @return true se JÁ ESTAVA em execução, false se COMEÇOU agora
     */
    private boolean sincronizacaoJaEstaEmExecucao() {
        // Tenta mudar de false para true. Se conseguiu, significa que não estava executando.
        // Se não conseguiu, significa que já estava executando.
        boolean jaEmExecucao = !sincronizacaoEmExecucao.compareAndSet(false, true);

        if (jaEmExecucao) {
            // Se já estava executando, registra no log
            log.warn("Tentativa de execução concorrente de beneficiários detectada - Já em execução");
        }

        return jaEmExecucao;
    }

    /**
     * EXECUTA A SINCRONIZAÇÃO DE BENEFICIÁRIOS COM CONTROLE DE ESTADO
     *
     * Este método é executado em thread separada (assíncrona) e faz:
     * 1. CHAMA o serviço real de sincronização de beneficiários
     * 2. GARANTE que sempre libera o controle no final (finally)
     * 3. MONITORA a execução com logs automáticos
     *
     * IMPORTÂNCIA DO FINALLY:
     * Mesmo que dê erro na sincronização, SEMPRE executa liberarControleExecucao()
     * Isso evita que o sistema "trave" em estado "executando" para sempre.
     *
     * ANOTAÇÃO @MonitorarOperacao:
     * Adiciona automaticamente logs de início/fim, captura de erros,
     * medição de tempo, etc. específicos para beneficiários.
     *
     * DIFERENÇA DO EMPRESA SCHEDULER:
     * - Chama SincronizacaoCompletaBeneficiarioService ao invés de empresa
     * - Logs identificam como operação de beneficiários
     * - Métricas separadas para monitoramento independente
     */
    @MonitorarOperacao(
            operacao = "EXECUCAO_SINCRONIZACAO_BENEFICIARIOS",
            incluirThread = true,
            excecaoEmErro = PROCESSAMENTO_LOTE
    )
    private void executarSincronizacaoComControle() {
        try {
            // Chama o serviço que faz o trabalho real de sincronização completa de beneficiários
            // Inclui inclusões, alterações e inativações
            sincronizacaoCompletaService.executarSincronizacaoCompleta();
        } finally {
            // SEMPRE executa, mesmo se der erro
            // Libera o controle para permitir próximas execuções
            liberarControleExecucao();
        }
    }

    /**
     * LIBERA O CONTROLE DE EXECUÇÃO PARA PRÓXIMAS SINCRONIZAÇÕES DE BENEFICIÁRIOS
     *
     * Este método "destrava" o sistema de beneficiários, permitindo que futuras
     * sincronizações possam ser executadas.
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
     *
     * ISOLAMENTO:
     * Este método só afeta o controle de beneficiários. O scheduler
     * de empresas continua funcionando normalmente.
     */
    private void liberarControleExecucao() {
        // Tenta mudar de true (executando) para false (livre)
        boolean liberado = sincronizacaoEmExecucao.compareAndSet(true, false);

        if (!liberado) {
            // Se não conseguiu liberar, é porque o estado não era "true"
            // Isso é um bug grave no controle de concorrência!
            log.error("Estado inconsistente do controle de execução de beneficiários detectado");
            throw new IllegalStateException("Estado inconsistente detectado ao liberar controle de execução de beneficiários");
        }

        // Se chegou aqui, liberou com sucesso
        log.debug("Controle de execução de beneficiários liberado");
    }
}
