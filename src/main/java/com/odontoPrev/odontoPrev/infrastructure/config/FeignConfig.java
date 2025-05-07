package com.odontoPrev.odontoPrev.infrastructure.config;


import feign.Logger;
import feign.RequestInterceptor;
import feign.RequestTemplate;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Base64;

@Configuration
public class FeignConfig {

    private static final String USERNAME = "nicolly";
    private static final String PASSWORD = "1234";

    @Bean
    public RequestInterceptor requestInterceptor(){
        return new RequestInterceptor() {
            @Override
            public void apply(RequestTemplate requestTemplate) {
                String auth = USERNAME + ":" + PASSWORD;
                String encodeAuth = "Basic " + Base64.getEncoder().encodeToString(auth.getBytes());
                requestTemplate.header("Authorization",encodeAuth);
            }
        };
    }

    @Bean
    public Logger.Level feignLoggerLevel() {
        return Logger.Level.FULL;
    }
}
