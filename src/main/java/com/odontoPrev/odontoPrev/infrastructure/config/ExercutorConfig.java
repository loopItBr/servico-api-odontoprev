package com.odontoPrev.odontoPrev.infrastructure.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Configuration
public class ExercutorConfig {
    @Bean
    public ExecutorService executorService(){
        //cria um executor que usa uma nova virtual thread para cada tarefa
        return Executors.newVirtualThreadPerTaskExecutor();
    }
}