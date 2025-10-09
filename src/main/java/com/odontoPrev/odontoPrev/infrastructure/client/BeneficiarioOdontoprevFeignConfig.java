package com.odontoPrev.odontoPrev.infrastructure.client;

import feign.codec.Encoder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectFactory;
import org.springframework.boot.autoconfigure.http.HttpMessageConverters;
import org.springframework.cloud.openfeign.support.SpringEncoder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration
public class BeneficiarioOdontoprevFeignConfig {

    @Bean
    public Encoder feignBeneficiarioEncoder(ObjectFactory<HttpMessageConverters> messageConverters) {
        // Usa SpringEncoder que suporta JSON para objetos complexos
        return new SpringEncoder(messageConverters);
    }
}
