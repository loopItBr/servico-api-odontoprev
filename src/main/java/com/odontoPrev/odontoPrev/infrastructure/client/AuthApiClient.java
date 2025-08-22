package com.odontoPrev.odontoPrev.infrastructure.client;

import com.odontoPrev.odontoPrev.application.dto.ApiTokenResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;

import java.util.Map;

// Cliente específico para a API de autenticação da aplicação
@FeignClient(name = "authApi", url = "${odontoprev.api.base-url-auth}")
public interface AuthApiClient {

    @PostMapping(path = "/oauth2/token", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    ApiTokenResponse getApiToken(
            @RequestHeader("Authorization") String authorizationHeader,
            Map<String, ?> formParams
    );
}