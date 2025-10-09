package com.odontoPrev.odontoPrev.infrastructure.client;

import com.odontoPrev.odontoPrev.infrastructure.client.adapter.out.dto.LoginEmpresaRequest;
import com.odontoPrev.odontoPrev.infrastructure.client.adapter.out.dto.LoginEmpresaResponse;
import com.odontoPrev.odontoPrev.infrastructure.client.adapter.out.dto.TokenResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(
    name = "odontoprev-auth",
    url = "${odontoprev.api.base-url-auth:https://apim-hml.odontoprev.com.br}",
    configuration = {OdontoprevAuthFeignConfig.class}
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

    /**
     * OBTÉM TOKEN OAUTH2 PARA API DE BENEFICIÁRIOS
     * 
     * @param authorization Basic {APP TOKEN}
     * @param grantType client_credentials
     * @return TokenResponse com access_token
     */
    @PostMapping(
        value = "/oauth2/token",
        consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE,
        produces = MediaType.APPLICATION_JSON_VALUE
    )
    TokenResponse obterTokenOAuth2(
        @RequestHeader("Authorization") String authorization,
        @RequestParam("grant_type") String grantType
    );

    /**
     * OBTÉM TOKEN DE LOGIN EMPRESA PARA API DE BENEFICIÁRIOS
     * 
     * @param authorization Bearer {BEARER 1}
     * @param request LoginEmpresaRequest com appId, login, senha
     * @return LoginEmpresaResponse com accessToken
     */
    @PostMapping(
        value = "/empresa-login/1.0/api/auth/token",
        consumes = MediaType.APPLICATION_JSON_VALUE,
        produces = MediaType.APPLICATION_JSON_VALUE
    )
    LoginEmpresaResponse obterTokenLoginEmpresa(
        @RequestHeader("Authorization") String authorization,
        @RequestBody LoginEmpresaRequest request
    );
}
