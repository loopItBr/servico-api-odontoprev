package com.odontoPrev.odontoPrev.infrastructure.client;

import com.odontoPrev.odontoPrev.infrastructure.client.adapter.out.dto.TokenRequest;
import com.odontoPrev.odontoPrev.infrastructure.client.adapter.out.dto.TokenResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;

@FeignClient(
    name = "odontoprev-auth",
    url = "${odontoprev.api.base-url-auth:https://api-hml.odontoprev.com.br:8243}",
    configuration = {OdontoprevFeignConfig.class}
)
public interface OdontoprevAuthClient {

    @PostMapping(
        value = "/token",
        consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE,
        produces = MediaType.APPLICATION_JSON_VALUE
    )
    TokenResponse obterToken(
        @RequestHeader("Authorization") String authorization,
        @RequestBody TokenRequest request
    );
}