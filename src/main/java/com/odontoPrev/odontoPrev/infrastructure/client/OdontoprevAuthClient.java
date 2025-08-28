package com.odontoPrev.odontoPrev.infrastructure.client;


import com.odontoPrev.odontoPrev.infrastructure.client.adapter.out.dto.TokenResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(
    name = "odontoprev-auth",
    url = "${odontoprev.api.base-url-auth:https://apim-hml.odontoprev.com.br}",
    configuration = {OdontoprevFeignConfig.class}
)
public interface OdontoprevAuthClient {

    @PostMapping(
        value = "/oauth2/token",
        consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE,
        produces = MediaType.APPLICATION_JSON_VALUE
    )
    TokenResponse obterToken(
        @RequestHeader("Authorization") String authorization,
        @RequestParam("grant_type") String grantType
    );
}
