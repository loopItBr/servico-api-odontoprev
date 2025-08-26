package com.odontoPrev.odontoPrev.infrastructure.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * CONFIGURAÇÃO DO POOL DE THREADS PARA PROCESSAMENTO ASSÍNCRONO
 * 
 * FUNÇÃO PRINCIPAL:
 * Esta classe configura um pool de threads otimizado para processar a 
 * sincronização com OdontoPrev de forma assíncrona, sem travar o sistema principal.
 * 
 * COMO FUNCIONA:
 * Um pool de threads é como uma "equipe de trabalhadores" que pode executar
 * tarefas em paralelo. Em vez de processar uma empresa por vez, podemos
 * processar várias empresas simultaneamente.
 * 
 * ANALOGIA SIMPLES:
 * Imagine uma fila de banco com vários caixas:
 * - Threads = Caixas do banco
 * - Tarefas = Clientes na fila
 * - Pool = O conjunto de todos os caixas trabalhando
 * 
 * BENEFÍCIOS:
 * 1. PERFORMANCE: Processa múltiplas empresas ao mesmo tempo
 * 2. CONTROLE: Limita quantas threads usar para não sobrecarregar sistema
 * 3. EFICIÊNCIA: Reutiliza threads em vez de criar/destruir constantemente
 * 4. SEGURANÇA: Se uma thread trava, outras continuam funcionando
 * 
 * CONFIGURAÇÕES IMPORTANTES:
 * - core-pool-size: Quantas threads manter sempre ativas
 * - max-pool-size: Máximo de threads que podem existir
 * - queue-capacity: Quantas tarefas podem esperar na fila
 * - keep-alive-seconds: Tempo para manter threads extras vivas
 * 
 * EXEMPLO PRÁTICO:
 * Com configuração core=5, max=10, queue=25:
 * - Sistema inicia com 5 threads sempre ativas
 * - Se chega trabalho demais, cria até 10 threads no total
 * - Se mesmo assim não dá conta, até 25 tarefas ficam na fila esperando
 * - Threads extras são removidas após 60 segundos sem trabalho
 */
@Slf4j
@Configuration
public class ExecutorConfig {

    // Número mínimo de threads que ficam sempre ativas (configurável via application.yml)
    @Value("${executor.core-pool-size:5}")
    private int corePoolSize;

    // Número máximo de threads que podem ser criadas quando há muito trabalho
    @Value("${executor.max-pool-size:10}")
    private int maxPoolSize;

    // Quantas tarefas podem ficar na fila esperando quando todas as threads estão ocupadas
    @Value("${executor.queue-capacity:25}")
    private int queueCapacity;

    // Por quanto tempo (em segundos) manter threads extras vivas quando não há trabalho
    @Value("${executor.keep-alive-seconds:60}")
    private int keepAliveSeconds;

    /**
     * CRIA E CONFIGURA O POOL DE THREADS PRINCIPAL DO SISTEMA
     * 
     * Este método é chamado automaticamente pelo Spring na inicialização
     * e cria um pool de threads otimizado para nosso caso de uso.
     * 
     * FUNCIONAMENTO:
     * 1. Cria um ThreadPoolTaskExecutor (implementação Spring do pool de threads)
     * 2. Configura todos os parâmetros baseado nos valores do application.yml
     * 3. Define política de rejeição para quando pool estiver sobrecarregado
     * 4. Configura encerramento gracioso (aguarda tarefas terminarem)
     * 5. Retorna ExecutorService para uso em outras partes do sistema
     * 
     * POLÍTICA DE REJEIÇÃO (CallerRunsPolicy):
     * Quando o pool está cheio e a fila também:
     * - Em vez de rejeitar a tarefa, executa na thread que chamou
     * - Isso cria "pressão contrária" natural, diminuindo a velocidade
     * - Evita perder tarefas importantes
     * 
     * ENCERRAMENTO GRACIOSO:
     * - setWaitForTasksToCompleteOnShutdown(true): aguarda tarefas em execução
     * - setAwaitTerminationSeconds(30): espera até 30 segundos para terminar
     * 
     * NOME DAS THREADS:
     * Todas as threads criadas terão nome "OdontoPrev-Task-X" para facilitar
     * identificação nos logs e ferramentas de monitoramento.
     */
    @Bean("executorService")
    public ExecutorService executorService() {
        // Log das configurações para acompanhar nos logs de inicialização
        log.info("Configurando ThreadPoolTaskExecutor com core: {}, max: {}, queue: {}", 
                corePoolSize, maxPoolSize, queueCapacity);
        
        // 1. Cria o executor Spring
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        
        // 2. Configura tamanho do pool (mínimo e máximo de threads)
        executor.setCorePoolSize(corePoolSize);      // Threads sempre ativas
        executor.setMaxPoolSize(maxPoolSize);        // Máximo de threads
        executor.setQueueCapacity(queueCapacity);    // Tamanho da fila de espera
        executor.setKeepAliveSeconds(keepAliveSeconds); // Tempo para manter threads extras
        
        // 3. Nomeia as threads para facilitar identificação nos logs
        executor.setThreadNamePrefix("OdontoPrev-Task-");
        
        // 4. Define o que fazer quando pool + fila estão cheios
        // CallerRunsPolicy = executa na thread que chamou (cria pressão contrária)
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        
        // 5. Configura encerramento gracioso do sistema
        executor.setWaitForTasksToCompleteOnShutdown(true); // Aguarda tarefas terminarem
        executor.setAwaitTerminationSeconds(30);           // Tempo máximo para aguardar
        
        // 6. Inicializa o executor (obrigatório)
        executor.initialize();
        
        // 7. Retorna o ExecutorService que será injetado em outros componentes
        return executor.getThreadPoolExecutor();
    }

}