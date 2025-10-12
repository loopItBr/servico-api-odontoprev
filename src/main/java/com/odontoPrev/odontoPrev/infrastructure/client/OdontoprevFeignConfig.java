package com.odontoPrev.odontoPrev.infrastructure.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import feign.codec.Encoder;
import feign.form.FormEncoder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.lang.reflect.Type;

@Slf4j
@Configuration
public class OdontoprevFeignConfig {

    @Autowired
    private ObjectMapper objectMapper;

    /**
     * ENCODER HÍBRIDO PARA FEIGN
     * 
     * Este encoder suporta tanto form-urlencoded quanto JSON:
     * - FormEncoder: Para endpoints que precisam de form-urlencoded (ex: OAuth2)
     * - JSON Encoder customizado: Para endpoints que precisam de JSON (ex: alterarEmpresa)
     * 
     * O Feign automaticamente escolhe o encoder baseado no Content-Type da requisição.
     */
    @Bean
    public Encoder feignEncoder() {
        return new FormEncoder(new JsonEncoder(objectMapper));
    }

    /**
     * ENCODER JSON CUSTOMIZADO
     * 
     * Encoder que converte objetos Java para JSON usando Jackson ObjectMapper.
     */
    public static class JsonEncoder implements Encoder {
        private final ObjectMapper objectMapper;

        public JsonEncoder(ObjectMapper objectMapper) {
            this.objectMapper = objectMapper;
        }

        @Override
        public void encode(Object object, Type bodyType, feign.RequestTemplate template) {
            try {
                String json = objectMapper.writeValueAsString(object);
                template.body(json);
                log.debug("🔧 [FEIGN ENCODER] Objeto serializado para JSON: {} bytes", json.length());
            } catch (Exception e) {
                log.error("❌ [FEIGN ENCODER] Erro ao serializar objeto para JSON: {}", e.getMessage());
                throw new feign.codec.EncodeException("Erro ao serializar objeto para JSON", e);
            }
        }
    }
}