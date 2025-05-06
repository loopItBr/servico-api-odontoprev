package com.odontoPrev.odontoPrev.infrastructure.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Configuration
public class ExercutorConfig {

    @Bean
    public ExecutorService executorService(){

        //aqui está o pool fixo de 10 threads, o Executors é uma classe para criar diferentes tipos de thread pool
        return Executors.newFixedThreadPool(10);
    }
}
