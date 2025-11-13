package com.odontoPrev.odontoPrev.infrastructure.client.adapter.out;

import com.odontoPrev.odontoPrev.infrastructure.client.BeneficiarioOdontoprevFeignConfig;
import com.odontoPrev.odontoPrev.infrastructure.client.adapter.out.dto.BeneficiarioAlteracaoRequest;
import com.odontoPrev.odontoPrev.infrastructure.client.adapter.out.dto.BeneficiarioAlteracaoRequestNew;
import com.odontoPrev.odontoPrev.infrastructure.client.adapter.out.dto.BeneficiarioAlteracaoResponseNew;
import com.odontoPrev.odontoPrev.infrastructure.client.adapter.out.dto.BeneficiarioInclusaoRequest;
import com.odontoPrev.odontoPrev.infrastructure.client.adapter.out.dto.BeneficiarioInclusaoRequestNew;
import com.odontoPrev.odontoPrev.infrastructure.client.adapter.out.dto.BeneficiarioInclusaoResponse;
import com.odontoPrev.odontoPrev.infrastructure.client.adapter.out.dto.BeneficiarioInclusaoResponseNew;
import com.odontoPrev.odontoPrev.infrastructure.client.adapter.out.dto.BeneficiarioDependenteInclusaoRequest;
import com.odontoPrev.odontoPrev.infrastructure.client.adapter.out.dto.EmpresaAtivacaoPlanoRequest;
import com.odontoPrev.odontoPrev.infrastructure.client.adapter.out.dto.EmpresaAtivacaoPlanoResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestPart;

import java.util.List;

/**
 * CLIENTE FEIGN PARA COMUNICAÇÃO COM APIs DE BENEFICIÁRIO DA ODONTOPREV
 *
 * Esta interface define todos os endpoints necessários para gerenciar
 * beneficiários no sistema da OdontoPrev através de chamadas HTTP.
 *
 * ENDPOINTS DISPONÍVEIS:
 * 1. POST /incluir - Cadastra novo beneficiário
 * 2. PUT /alterar - Atualiza dados de beneficiário existente
 * 3. POST /inativarAssociadoEmpresarial - Inativa beneficiário
 *
 * CONFIGURAÇÃO:
 * - Nome do cliente: beneficiario-odontoprev-client
 * - URL base: ${odontoprev.api.base-url-beneficiario} (em PRD: https://apim.odontoprev.com.br/cadastroonline-pj/1.0)
 * - Timeout e retry configurados no application.yml
 *
 * TRATAMENTO DE ERROS:
 * Erros de comunicação são tratados pelo Global Exception Handler
 * e registrados na tabela de controle de sincronização.
 */
@FeignClient(
    name = "beneficiario-odontoprev-client",
    url = "${odontoprev.api.base-url-beneficiario:${odontoprev.api.base-url}}",
    configuration = {BeneficiarioOdontoprevFeignConfig.class}
)
public interface BeneficiarioOdontoprevFeignClient {

    /**
     * INCLUI NOVO BENEFICIÁRIO NA ODONTOPREV (MÉTODO ANTIGO - COMPATIBILIDADE)
     *
     * Envia dados de um novo beneficiário para cadastro no sistema
     * da OdontoPrev. Retorna o código do associado (carteirinha).
     *
     * ENDPOINT: POST {{baseUrl}}/incluir
     * URL completa em PRD: https://apim.odontoprev.com.br/cadastroonline-pj/1.0/incluir
     *
     * @param request dados do beneficiário a ser incluído
     * @return response contendo cdAssociado e status da operação
     */
    @PostMapping(
        value = "/incluir",
        consumes = MediaType.APPLICATION_JSON_VALUE,
        produces = MediaType.APPLICATION_JSON_VALUE
    )
    BeneficiarioInclusaoResponse incluirBeneficiario(
        @RequestHeader("Authorization") String authorization,
        @RequestHeader("empresa") String empresa,
        @RequestHeader("usuario") String usuario,
        @RequestHeader("senha") String senha,
        @RequestHeader("app-id") String appId,
        @RequestBody BeneficiarioInclusaoRequest request
    );

    /**
     * INCLUI NOVO BENEFICIÁRIO NA ODONTOPREV (MÉTODO NOVO - AUTENTICAÇÃO DUPLA)
     *
     * Envia dados de um novo beneficiário para cadastro no sistema
     * da OdontoPrev usando autenticação dupla conforme documentação.
     * Retorna o código do associado (carteirinha).
     *
     * ENDPOINT: POST {{baseUrl}}/incluir
     * URL completa em PRD: https://apim.odontoprev.com.br/cadastroonline-pj/1.0/incluir
     *
     * AUTENTICAÇÃO DUPLA:
     * - Authorization: Bearer {BEARER 1} - Token OAuth2
     * - AuthorizationOdonto: Bearer {BEARER 2} - Token Login Empresa
     *
     * FLUXO DE FUNCIONAMENTO:
     * 1. Recebe dados completos do beneficiário
     * 2. Valida campos obrigatórios (feito automaticamente)
     * 3. Envia requisição para OdontoPrev
     * 4. Retorna cdAssociado (número da carteirinha)
     * 5. cdAssociado deve ser salvo no banco via procedure
     *
     * CAMPOS OBRIGATÓRIOS:
     * beneficiarioTitular.beneficiario: codigoMatricula, CPF, dataDeNascimento, 
     * dtVigenciaRetroativa, nomeBeneficiario, nomeDaMae, sexo, telefoneCelular, endereco
     * venda: codigoEmpresa, codigoPlano, departamento
     * usuario: usuário que realizou a movimentação
     *
     * RETORNO ESPERADO:
     * {
     *   "status": 201,
     *   "cdMsg": 0,
     *   "beneficiarios": {
     *     "codigoAssociado": "123456789",
     *     "nomeAssociado": "NOME DO BENEFICIARIO"
     *   },
     *   "crm": {
     *     "protocolo": "20251008918833",
     *     "ocorrencia": "05925307"
     *   },
     *   "mensagem": "inserção gerada com sucesso"
     * }
     *
     * @param authorizationOAuth2 Token OAuth2 (Bearer {BEARER 1})
     * @param authorizationOdonto Token Login Empresa (Bearer {BEARER 2})
     * @param request dados do beneficiário a ser incluído
     * @return response contendo cdAssociado e status da operação
     */
    @PostMapping(
        value = "/incluir",
        consumes = MediaType.APPLICATION_JSON_VALUE,
        produces = MediaType.APPLICATION_JSON_VALUE
    )
    BeneficiarioInclusaoResponseNew incluirBeneficiario(
        @RequestHeader("Authorization") String authorizationOAuth2,
        @RequestHeader("AuthorizationOdonto") String authorizationOdonto,
        @RequestBody BeneficiarioInclusaoRequestNew request
    );

    /**
     * INCLUI DEPENDENTE NA ODONTOPREV
     *
     * Envia dados de um novo dependente para cadastro no sistema
     * da OdontoPrev. O titular já deve existir na OdontoPrev.
     *
     * ENDPOINT: POST {{baseUrl}}/incluirDependente
     * URL completa em PRD: https://apim.odontoprev.com.br/cadastroonline-pj/1.0/incluirDependente
     *
     * AUTENTICAÇÃO DUPLA:
     * - Authorization: Bearer {BEARER 1} - Token OAuth2
     * - AuthorizationOdonto: Bearer {BEARER 2} - Token Login Empresa
     *
     * REQUISITOS:
     * - O titular já deve estar cadastrado na OdontoPrev
     * - codigoAssociadoTitular é obrigatório (9 caracteres)
     * - A vigência do dependente não precisa ser a mesma do titular
     *
     * CAMPOS OBRIGATÓRIOS:
     * - codigoAssociadoTitular: Código do associado titular (9 caracteres)
     * - usuario: Usuário de movimentação (até 15 caracteres)
     * - cdUsuario: Código do usuário (até 15 caracteres)
     * - beneficiarios: Lista com dados do dependente
     *
     * RETORNO:
     * - Mesmo formato do BeneficiarioInclusaoResponseNew
     * - cdAssociado do dependente na listaBeneficiarios
     * - Protocolo e ocorrência no CRM
     *
     * @param authorizationOAuth2 Token OAuth2 (Bearer {BEARER 1})
     * @param authorizationOdonto Token Login Empresa (Bearer {BEARER 2})
     * @param request dados do dependente a ser incluído
     * @return response contendo cdAssociado e status da operação
     */
    @PostMapping(
        value = "/incluirDependente",
        consumes = MediaType.APPLICATION_JSON_VALUE,
        produces = MediaType.APPLICATION_JSON_VALUE
    )
    BeneficiarioInclusaoResponseNew incluirDependente(
        @RequestHeader("Authorization") String authorizationOAuth2,
        @RequestHeader("AuthorizationOdonto") String authorizationOdonto,
        @RequestBody BeneficiarioDependenteInclusaoRequest request
    );

    /**
     * ALTERA DADOS DE BENEFICIÁRIO EXISTENTE
     *
     * Atualiza informações cadastrais de um beneficiário já existente
     * no sistema da OdontoPrev. Usado quando dados são alterados no Tasy.
     *
     * ENDPOINT: PUT {{baseUrl}}/alterar
     *
     * FLUXO DE FUNCIONAMENTO:
     * 1. Beneficiário deve já existir na OdontoPrev (ter cdAssociado)
     * 2. Envia apenas campos que foram alterados no dia corrente
     * 3. View já filtra registros com alteração do dia
     * 4. Não há retorno específico (void response)
     *
     * CAMPOS OBRIGATÓRIOS:
     * cdEmpresa, cdAssociado, codigoPlano, departamento
     *
     * CAMPOS OPCIONAIS:
     * Todos os demais campos podem ser enviados conforme necessário
     *
     * OBSERVAÇÕES:
     * - Apenas beneficiários alterados no dia corrente são processados
     * - Se beneficiário não existir na OdontoPrev, retornará erro
     * - Operação é idempotente (pode ser executada múltiplas vezes)
     *
     * @param request dados alterados do beneficiário
     */
    @PutMapping(
        value = "/alterar",
        consumes = MediaType.APPLICATION_JSON_VALUE
    )
    void alterarBeneficiario(
        @RequestHeader("Authorization") String authorization,
        @RequestHeader("empresa") String empresa,
        @RequestHeader("usuario") String usuario,
        @RequestHeader("senha") String senha,
        @RequestHeader("app-id") String appId,
        @RequestBody BeneficiarioAlteracaoRequest request
    );

    /**
     * ALTERA DADOS DE BENEFICIÁRIO EXISTENTE (MÉTODO NOVO - AUTENTICAÇÃO DUPLA)
     *
     * Atualiza informações cadastrais de um beneficiário já existente
     * no sistema da OdontoPrev usando autenticação dupla conforme documentação.
     *
     * ENDPOINT: PUT {{baseUrl}}/alterar
     *
     * AUTENTICAÇÃO DUPLA:
     * - Authorization: Bearer {BEARER 1} - Token OAuth2
     * - AuthorizationOdonto: Bearer {BEARER 2} - Token Login Empresa
     *
     * FLUXO DE FUNCIONAMENTO:
     * 1. Recebe dados de alteração do beneficiário
     * 2. Valida campos obrigatórios (feito automaticamente)
     * 3. Envia requisição para OdontoPrev
     * 4. Retorna resposta com status da alteração
     *
     * CAMPOS OBRIGATÓRIOS:
     * - cdEmpresa: Código da empresa
     * - codigoAssociado: Carteirinha do beneficiário
     * - codigoPlano: Código do plano
     * - departamento: Código do departamento
     *
     * @param authorizationOAuth2 Token OAuth2 (Bearer 1)
     * @param authorizationOdonto Token Login Empresa (Bearer 2)
     * @param request Dados de alteração do beneficiário
     * @return Resposta da alteração
     */
    @PutMapping(
        value = "/alterar",
        consumes = MediaType.APPLICATION_JSON_VALUE,
        produces = MediaType.APPLICATION_JSON_VALUE
    )
    BeneficiarioAlteracaoResponseNew alterarBeneficiarioNew(
        @RequestHeader("Authorization") String authorizationOAuth2,
        @RequestHeader("AuthorizationOdonto") String authorizationOdonto,
        @RequestBody List<BeneficiarioAlteracaoRequestNew> request
    );

    /**
     * INATIVA BENEFICIÁRIO NA ODONTOPREV
     *
     * Remove acesso do beneficiário ao plano odontológico.
     * Usado quando beneficiário tem status Rescindido/Suspenso no Tasy.
     *
     * ENDPOINT: POST {{baseUrl}}/inativarAssociadoEmpresarial
     * URL completa em PRD: https://apim.odontoprev.com.br/cadastroonline-pj/1.0/inativarAssociadoEmpresarial
     *
     * FLUXO DE FUNCIONAMENTO:
     * 1. Beneficiário deve estar ativo na OdontoPrev
     * 2. Envia código do associado e motivo da inativação via multipart/form-data
     * 3. OdontoPrev processa a inativação
     * 4. Não há retorno específico (void response)
     *
     * AUTENTICAÇÃO DUPLA:
     * - Authorization: Bearer {TOKEN_OAUTH2} - Token do OAuth2
     * - AuthorizationOdonto: Bearer {TOKEN_LOGIN} - Token do login empresa
     *
     * FORMATO DO PAYLOAD:
     * multipart/form-data com campo 'empresarialModel' contendo JSON:
     * {
     *   "cdEmpresa": "787392",
     *   "cdUsuario": "13433638",
     *   "associado": [{
     *     "cdMatricula": "00000001",
     *     "cdAssociado": "000000001",
     *     "nome": "NOME DO ASSOCIADO",
     *     "email": "email@exemplo.com",
     *     "idMotivo": "7"
     *   }],
     *   "dataInativacao": "2024-12-29"
     * }
     *
     * CAMPOS OBRIGATÓRIOS:
     * cdEmpresa, cdUsuario, associado (array com cdMatricula, cdAssociado, nome, idMotivo)
     *
     * CAMPOS OPCIONAIS:
     * dataInativacao (se inativação for futura), email
     *
     * OBSERVAÇÕES:
     * - Podem ser enviados todos os inativos (OdontoPrev trata duplicação)
     * - Se dataInativacao não for informada, considera data atual
     * - Operação é idempotente
     *
     * @param authorizationOAuth2 Token OAuth2 (Bearer token)
     * @param authorizationOdonto Token de login empresa (Bearer token)
     * @param empresarialModelJson JSON com dados de inativação
     */
    @PostMapping(
        value = "/inativarAssociadoEmpresarial",
        consumes = MediaType.MULTIPART_FORM_DATA_VALUE,
        produces = MediaType.APPLICATION_JSON_VALUE
    )
    void inativarBeneficiario(
        @RequestHeader("Authorization") String authorizationOAuth2,
        @RequestHeader("AuthorizationOdonto") String authorizationOdonto,
        @RequestPart("empresarialModel") String empresarialModelJson
    );

    /**
     * ATIVAÇÃO DO PLANO DA EMPRESA
     * 
     * Endpoint para ativar o plano odontológico de uma empresa após o cadastro.
     * Este endpoint é chamado automaticamente após a sincronização bem-sucedida da empresa.
     * 
     * ENDPOINT: POST {{baseUrl}}/empresa/2.0/empresas/contrato/empresarial
     * 
     * AUTENTICAÇÃO:
     * - Authorization: Bearer {TOKEN_OAUTH2} - Token OAuth2
     * 
     * FLUXO DE FUNCIONAMENTO:
     * 1. Empresa deve estar cadastrada na OdontoPrev
     * 2. Envia dados completos do contrato empresarial
     * 3. OdontoPrev ativa os planos da empresa
     * 4. Retorna confirmação da ativação
     * 
     * @param authorizationOAuth2 Token OAuth2 (Bearer token)
     * @param request Dados do contrato empresarial para ativação
     * @return Resposta da ativação do plano
     */
    @PostMapping(
        value = "/empresa/2.0/empresas/contrato/empresarial",
        consumes = MediaType.APPLICATION_JSON_VALUE,
        produces = MediaType.APPLICATION_JSON_VALUE
    )
    EmpresaAtivacaoPlanoResponse ativarPlanoEmpresa(
        @RequestHeader("Authorization") String authorizationOAuth2,
        @RequestBody EmpresaAtivacaoPlanoRequest request
    );
}
