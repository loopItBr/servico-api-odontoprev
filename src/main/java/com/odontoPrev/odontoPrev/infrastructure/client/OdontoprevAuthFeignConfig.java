package com.odontoPrev.odontoPrev.infrastructure.client;

import feign.codec.Encoder;
import feign.form.FormEncoder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectFactory;
import org.springframework.boot.autoconfigure.http.HttpMessageConverters;
import org.springframework.cloud.openfeign.support.SpringEncoder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration
public class OdontoprevAuthFeignConfig {

    @Bean
    public Encoder feignAuthEncoder(ObjectFactory<HttpMessageConverters> messageConverters) {
        // Usa SpringEncoder padrão que suporta JSON
        return new SpringEncoder(messageConverters);
    }
}
