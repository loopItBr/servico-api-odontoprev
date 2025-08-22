package com.odontoPrev.odontoPrev.domain.service;

import com.odontoPrev.odontoPrev.application.dto.ApiTokenResponse;
import com.odontoPrev.odontoPrev.application.dto.DadosBeneficiarioResponse;
import com.odontoPrev.odontoPrev.application.dto.EmpresaTokenRequest;
import com.odontoPrev.odontoPrev.application.dto.EmpresaTokenResponse;
import com.odontoPrev.odontoPrev.infrastructure.client.AuthApiClient;
import com.odontoPrev.odontoPrev.infrastructure.client.LoginEmpresaApiClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class OdontoPrevService {

    private final AuthApiClient authApiClient;
    private final LoginEmpresaApiClient loginEmpresaApiClient;

    @Value("${odontoprev.api.app-token}")
    private String appToken;
    @Value("${odontoprev.api.user.login}")
    private String userLogin;
    @Value("${odontoprev.api.user.senha}")
    private String userSenha;
    @Value("${odontoprev.api.user.appid}")
    private String userAppId;

    private String cachedApiToken;
    private LocalDateTime apiTokenExpiration;
    private String cachedEmpresaToken;
    private LocalDateTime empresaTokenExpiration;

    public DadosBeneficiarioResponse getDadosBeneficiario(String codigoAssociado) {
        String apiToken = getValidApiToken();
        String empresaToken = getValidEmpresaToken(apiToken);

        log.info("Buscando dados para o associado: {}", codigoAssociado);
        return loginEmpresaApiClient.getDadosBeneficiario(
                "Bearer " + apiToken,
                "Bearer " + empresaToken,
                codigoAssociado
        );
    }

    public String getValidApiToken() {
        if (cachedApiToken != null && LocalDateTime.now().isBefore(apiTokenExpiration)) {
            log.info("Usando API Token (Bearer 1) do cache.");
            return cachedApiToken;
        }

        log.info("Solicitando novo API Token (Bearer 1)...");
        String basicAuthHeader = "Basic " + appToken;
        Map<String, ?> formParams = Collections.singletonMap("grant_type", "client_credentials");

        ApiTokenResponse response = authApiClient.getApiToken(basicAuthHeader, formParams);
        cachedApiToken = response.getAccessToken();
        apiTokenExpiration = LocalDateTime.now().plusSeconds(response.getExpiresIn()).minusMinutes(5); // Margem de segurança

        log.info("Novo API Token obtido com sucesso.");
        return cachedApiToken;
    }

    public String getValidEmpresaToken(String apiToken) {
        if (cachedEmpresaToken != null && LocalDateTime.now().isBefore(empresaTokenExpiration)) {
            log.info("Usando Empresa Token (Bearer 2) do cache.");
            return cachedEmpresaToken;
        }

        log.info("Solicitando novo Empresa Token (Bearer 2)...");
        EmpresaTokenRequest request = new EmpresaTokenRequest(userAppId, userLogin, userSenha);
        EmpresaTokenResponse response = loginEmpresaApiClient.getEmpresaToken("Bearer " + apiToken, request);
        cachedEmpresaToken = response.getAccessToken();
        empresaTokenExpiration = LocalDateTime.now().plusHours(1).minusMinutes(5); // Margem de segurança

        log.info("Novo Empresa Token obtido com sucesso para o usuário {}.", response.getCodigoUsuario());
        return cachedEmpresaToken;
    }
}
