package com.odontoPrev.odontoPrev.infrastructure.client;

import feign.codec.Encoder;
import feign.form.FormEncoder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration
public class OdontoprevFeignConfig {

    @Bean
    public Encoder feignFormEncoder() {
        return new FormEncoder();
    }
}