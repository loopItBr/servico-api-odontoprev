package com.odontoPrev.odontoPrev.infrastructure.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadPoolExecutor;

@Slf4j
@Configuration
public class ExecutorConfig {

    @Value("${executor.core-pool-size:5}")
    private int corePoolSize;

    @Value("${executor.max-pool-size:10}")
    private int maxPoolSize;

    @Value("${executor.queue-capacity:25}")
    private int queueCapacity;

    @Value("${executor.keep-alive-seconds:60}")
    private int keepAliveSeconds;

    @Bean("executorService")
    public ExecutorService executorService() {
        log.info("Configurando ThreadPoolTaskExecutor com core: {}, max: {}, queue: {}", 
                corePoolSize, maxPoolSize, queueCapacity);
        
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(corePoolSize);
        executor.setMaxPoolSize(maxPoolSize);
        executor.setQueueCapacity(queueCapacity);
        executor.setKeepAliveSeconds(keepAliveSeconds);
        executor.setThreadNamePrefix("OdontoPrev-Task-");
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(30);
        executor.initialize();
        
        return executor.getThreadPoolExecutor();
    }
}