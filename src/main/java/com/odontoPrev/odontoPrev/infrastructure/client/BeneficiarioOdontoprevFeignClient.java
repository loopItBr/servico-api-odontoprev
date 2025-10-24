package com.odontoPrev.odontoPrev.infrastructure.client;

import com.odontoPrev.odontoPrev.infrastructure.client.adapter.out.dto.EmpresaAtivacaoPlanoRequest;
import com.odontoPrev.odontoPrev.infrastructure.client.adapter.out.dto.EmpresaAtivacaoPlanoResponse;
import com.odontoPrev.odontoPrev.infrastructure.client.adapter.out.dto.EmpresaPmeRequest;
import com.odontoPrev.odontoPrev.infrastructure.client.adapter.out.dto.PlanoCriarRequest;
import com.odontoPrev.odontoPrev.infrastructure.client.adapter.out.dto.EmpresaResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;

/**
 * CLIENT FEIGN PARA INTEGRAÇÃO COM API ODONTOPREV
 * 
 * Este client é responsável por fazer as chamadas HTTP para a API externa
 * da OdontoPrev, incluindo endpoints para empresas e beneficiários.
 * 
 * ENDPOINTS IMPLEMENTADOS:
 * - POST /empresa/2.0/empresas/contrato/empresarial: Inclusão de empresa
 * - GET /empresa/2.0/empresas/{codigoEmpresa}: Consulta de empresa
 * - POST /empresa/2.0/empresas/pme: Cadastro PME de empresa
 * 
 * CONFIGURAÇÃO:
 * - URL base configurada via application.yml
 * - Timeout e retry configurados via FeignConfig
 * - Interceptors para logs e monitoramento
 */
@FeignClient(
    name = "empresa-odontoprev-client",
    url = "${odontoprev.api.base-url}",
    configuration = com.odontoPrev.odontoPrev.infrastructure.client.BeneficiarioOdontoprevFeignConfig.class
)
public interface BeneficiarioOdontoprevFeignClient {

    /**
     * INCLUSÃO DE EMPRESA - POST /empresa/2.0/empresas/contrato/empresarial
     * 
     * Endpoint para incluir uma nova empresa na OdontoPrev.
     * Retorna dados da empresa incluída, incluindo código da empresa.
     * 
     * @param authorization Token de autorização Bearer
     * @param request Dados da empresa para inclusão
     * @return Resposta com dados da empresa incluída
     */
    @PostMapping("/empresa/2.0/empresas/contrato/empresarial")
    EmpresaAtivacaoPlanoResponse ativarPlanoEmpresa(
        @RequestHeader("Authorization") String authorization,
        @RequestBody EmpresaAtivacaoPlanoRequest request
    );

    /**
     * CONSULTA DE EMPRESA - GET /empresa/2.0/empresas/{codigoEmpresa}
     * 
     * Endpoint para consultar dados de uma empresa específica.
     * Usado após inclusão para confirmar dados da empresa.
     * 
     * @param authorization Token de autorização Bearer
     * @param codigoEmpresa Código da empresa a ser consultada
     * @return Dados completos da empresa
     */
    @GetMapping("/empresa/2.0/empresas/{codigoEmpresa}")
    EmpresaResponse buscarEmpresa(
        @RequestHeader("Authorization") String authorization,
        @PathVariable("codigoEmpresa") String codigoEmpresa
    );

    /**
     * CADASTRO PME DE EMPRESA - POST /empresa/2.0/empresas/pme
     * 
     * Endpoint para cadastrar empresa no sistema PME após inclusão bem-sucedida.
     * Esta é a etapa final do fluxo de inclusão empresarial.
     * 
     * @param authorization Token de autorização Bearer
     * @param request Dados da empresa para cadastro PME
     * @return Resposta do cadastro PME (pode ser void ou response específica)
     */
    @PostMapping("/empresa/2.0/empresas/pme")
    void cadastrarEmpresaPme(
        @RequestHeader("Authorization") String authorization,
        @RequestBody EmpresaPmeRequest request
    );

    @PostMapping("/empresa/2.0/plano/criar")
    String criarPlano(
        @RequestHeader("Authorization") String authorization,
        @RequestBody PlanoCriarRequest request
    );
}
