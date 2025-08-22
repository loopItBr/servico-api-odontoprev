package com.odontoPrev.odontoPrev.infrastructure.client;

import com.odontoPrev.odontoPrev.application.dto.DadosBeneficiarioResponse;
import com.odontoPrev.odontoPrev.application.dto.EmpresaTokenRequest;
import com.odontoPrev.odontoPrev.application.dto.EmpresaTokenResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;

@FeignClient(name = "loginEmpresaApi", url = "${odontoprev.api.base-url-login}")
public interface LoginEmpresaApiClient {

    @PostMapping("/empresa-login/1.0/api/auth/token")
    EmpresaTokenResponse getEmpresaToken(
            @RequestHeader("Authorization") String apiToken,
            @RequestBody EmpresaTokenRequest request
    );

    @GetMapping("/empresa-login/1.0/api/obterDadosBeneficiario/{codigoAssociado}")
    DadosBeneficiarioResponse getDadosBeneficiario(
            @RequestHeader("Authorization") String apiToken,
            @RequestHeader("AuthorizationOdonto") String empresaToken,
            @PathVariable("codigoAssociado") String codigoAssociado
    );

    @GetMapping("/dcms/empresa/2.0/{codigoEmpresa}")
    Object getEmpresa(
            @RequestHeader("Authorization") String authorizationHeader,
            @PathVariable("codigoEmpresa") String codigoEmpresa
    );
}
