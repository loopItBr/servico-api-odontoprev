package com.odontoPrev.odontoPrev.infrastructure.client;

import com.odontoPrev.odontoPrev.infrastructure.client.adapter.out.dto.ContratoResponse;
import com.odontoPrev.odontoPrev.infrastructure.client.adapter.out.dto.EmpresaResponse;
import com.odontoPrev.odontoPrev.infrastructure.client.adapter.out.dto.PlanoResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@FeignClient(
    name = "odontoprev-client",
    url = "${odontoprev.api.base-url:https://apim-hml.odontoprev.com.br}${odontoprev.api.path:/empresa/2.0/}",
    configuration = {OdontoprevFeignConfig.class}
)
public interface OdontoprevClient {

    @GetMapping(
        value = "/empresas",
        produces = MediaType.APPLICATION_JSON_VALUE
    )
    List<EmpresaResponse> obterEmpresas(
        @RequestHeader("Authorization") String authorization,
        @RequestHeader("empresa") String empresa,
        @RequestHeader("usuario") String usuario,
        @RequestHeader("senha") String senha,
        @RequestHeader("app-id") String appId
    );

    @GetMapping(
        value = "/{codigoEmpresa}",
        produces = MediaType.APPLICATION_JSON_VALUE
    )
    EmpresaResponse obterEmpresaPorCodigo(
        @RequestHeader("Authorization") String authorization,
        @RequestHeader("empresa") String empresa,
        @RequestHeader("usuario") String usuario,
        @RequestHeader("senha") String senha,
        @RequestHeader("app-id") String appId,
        @PathVariable("codigoEmpresa") String codigoEmpresa
    );

    @GetMapping(
        value = "/empresas/{codigoEmpresa}/contratos",
        produces = MediaType.APPLICATION_JSON_VALUE
    )
    List<ContratoResponse> obterContratosPorEmpresa(
        @RequestHeader("Authorization") String authorization,
        @RequestHeader("empresa") String empresa,
        @RequestHeader("usuario") String usuario,
        @RequestHeader("senha") String senha,
        @RequestHeader("app-id") String appId,
        @PathVariable("codigoEmpresa") String codigoEmpresa
    );

    @GetMapping(
        value = "/empresas/{codigoEmpresa}/contratos/{codigoContrato}",
        produces = MediaType.APPLICATION_JSON_VALUE
    )
    ContratoResponse obterContratoPorCodigo(
        @RequestHeader("Authorization") String authorization,
        @RequestHeader("empresa") String empresa,
        @RequestHeader("usuario") String usuario,
        @RequestHeader("senha") String senha,
        @RequestHeader("app-id") String appId,
        @PathVariable("codigoEmpresa") String codigoEmpresa,
        @PathVariable("codigoContrato") String codigoContrato
    );

    @GetMapping(
        value = "/planos",
        produces = MediaType.APPLICATION_JSON_VALUE
    )
    List<PlanoResponse> obterPlanos(
        @RequestHeader("Authorization") String authorization,
        @RequestHeader("empresa") String empresa,
        @RequestHeader("usuario") String usuario,
        @RequestHeader("senha") String senha,
        @RequestHeader("app-id") String appId
    );

    @GetMapping(
        value = "/planos/{codigoPlano}",
        produces = MediaType.APPLICATION_JSON_VALUE
    )
    PlanoResponse obterPlanoPorCodigo(
        @RequestHeader("Authorization") String authorization,
        @RequestHeader("empresa") String empresa,
        @RequestHeader("usuario") String usuario,
        @RequestHeader("senha") String senha,
        @RequestHeader("app-id") String appId,
        @PathVariable("codigoPlano") String codigoPlano
    );


    @GetMapping(
        value = "/planos/{codigoPlano}/formas-pagamento",
        produces = MediaType.APPLICATION_JSON_VALUE
    )
    List<PlanoResponse.FormaPagamentoDto> obterFormasPagamentoPorPlano(
        @RequestHeader("Authorization") String authorization,
        @RequestHeader("empresa") String empresa,
        @RequestHeader("usuario") String usuario,
        @RequestHeader("senha") String senha,
        @RequestHeader("app-id") String appId,
        @PathVariable("codigoPlano") String codigoPlano
    );

    /**
     * Endpoint para adicionar uma nova empresa.
     * Baseado na documentação: /empresas (POST)
     */
    @PostMapping(
        value = "/empresas",
        consumes = MediaType.APPLICATION_JSON_VALUE,
        produces = MediaType.APPLICATION_JSON_VALUE
    )
    String adicionarEmpresa(
        @RequestHeader("Authorization") String authorization,
        @RequestHeader("empresa") String empresa,
        @RequestHeader("usuario") String usuario,
        @RequestHeader("senha") String senha,
        @RequestHeader("app-id") String appId,
        @RequestBody String dadosEmpresa
    );

    /**
     * Endpoint para alterar dados de uma empresa existente.
     * Baseado na documentação: /empresas/alterar (PUT)
     */
    @PutMapping(
        value = "/empresas/alterar",
        consumes = MediaType.APPLICATION_JSON_VALUE,
        produces = MediaType.APPLICATION_JSON_VALUE
    )
    String alterarEmpresa(
        @RequestHeader("Authorization") String authorization,
        @RequestHeader("empresa") String empresa,
        @RequestHeader("usuario") String usuario,
        @RequestHeader("senha") String senha,
        @RequestHeader("app-id") String appId,
        @RequestBody com.odontoPrev.odontoPrev.infrastructure.client.adapter.out.dto.EmpresaAlteracaoRequest dadosEmpresa
    );

    /**
     * Endpoint para inativar uma empresa.
     * Baseado na documentação: /empresas/inativar (PUI)
     */
    @PutMapping(
        value = "/empresas/inativar",
        consumes = MediaType.APPLICATION_JSON_VALUE,
        produces = MediaType.APPLICATION_JSON_VALUE
    )
    String inativarEmpresa(
        @RequestHeader("Authorization") String authorization,
        @RequestHeader("empresa") String empresa,
        @RequestHeader("usuario") String usuario,
        @RequestHeader("senha") String senha,
        @RequestHeader("app-id") String appId,
        @RequestBody com.odontoPrev.odontoPrev.infrastructure.client.adapter.out.dto.EmpresaInativacaoRequest dadosEmpresa
    );
}