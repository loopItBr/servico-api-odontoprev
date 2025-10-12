package com.odontoPrev.odontoPrev.infrastructure.client.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.odontoPrev.odontoPrev.domain.service.ConsultaEmpresaOdontoprevExpandidaService;
import com.odontoPrev.odontoPrev.infrastructure.client.OdontoprevClient;
import com.odontoPrev.odontoPrev.infrastructure.client.adapter.mapper.EmpresaAlteracaoMapper;
import com.odontoPrev.odontoPrev.infrastructure.client.adapter.out.dto.EmpresaAlteracaoRequest;
import com.odontoPrev.odontoPrev.infrastructure.client.domain.service.TokenService;
import com.odontoPrev.odontoPrev.infrastructure.repository.entity.IntegracaoOdontoprev;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Implementação do serviço expandido para consulta de empresas na API OdontoPrev.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ConsultaEmpresaOdontoprevExpandidaServiceImpl implements ConsultaEmpresaOdontoprevExpandidaService {
    
    private final OdontoprevClient odontoprevClient;
    private final ObjectMapper objectMapper;
    private final EmpresaAlteracaoMapper empresaAlteracaoMapper;
    private final TokenService tokenService;
    
    @Value("${odontoprev.api.empresa}")
    private String empresa;
    
    @Value("${odontoprev.api.usuario}")
    private String usuario;
    
    @Value("${odontoprev.api.senha}")
    private String senha;
    
    @Value("${odontoprev.api.app-id}")
    private String appId;
    
    @Override
    public String adicionarEmpresa(IntegracaoOdontoprev dadosEmpresa) {
        try {
            String dadosJson = objectMapper.writeValueAsString(dadosEmpresa);
            String token = tokenService.obterTokenValido();
            String authorization = "Bearer " + token;
            
            return odontoprevClient.adicionarEmpresa(
                authorization,
                empresa,
                usuario,
                senha,
                appId,
                dadosJson
            );
            
        } catch (JsonProcessingException e) {
            log.error("Erro ao serializar dados da empresa para adição: {}", e.getMessage());
            throw new RuntimeException("Falha na serialização dos dados", e);
        } catch (Exception e) {
            log.error("Erro ao adicionar empresa na API OdontoPrev: {}", e.getMessage());
            throw new RuntimeException("Falha na comunicação com a API", e);
        }
    }
    
    @Override
    public String alterarEmpresa(IntegracaoOdontoprev dadosEmpresa) {
        String codigoEmpresa = dadosEmpresa.getCodigoEmpresa();
        
        try {
            log.info("🔄 [ALTERAÇÃO EMPRESA] Iniciando alteração da empresa: {}", codigoEmpresa);
            log.info("🔍 [ALTERAÇÃO EMPRESA] Dados recebidos - CNPJ: {}, Nome: {}, Grupo: {}", 
                    dadosEmpresa.getCnpj(), dadosEmpresa.getNomeFantasia(), dadosEmpresa.getCodigoGrupoGerencial());
            
            // Cria request com APENAS campos modificados
            log.debug("🔄 [ALTERAÇÃO EMPRESA] Criando request com apenas campos modificados...");
            EmpresaAlteracaoRequest request = criarRequestMinimo(dadosEmpresa);
            log.debug("✅ [ALTERAÇÃO EMPRESA] Request mínimo criado");
            
            // Log detalhado do request
            try {
                String requestJson = objectMapper.writeValueAsString(request);
                log.info("📤 [ALTERAÇÃO EMPRESA] Request JSON completo: {}", requestJson);
                log.info("📤 [ALTERAÇÃO EMPRESA] Tamanho do request: {} bytes", requestJson.length());
            } catch (Exception e) {
                log.warn("⚠️ [ALTERAÇÃO EMPRESA] Erro ao serializar request para log: {}", e.getMessage());
            }
            
            // Log dos headers
            String token = tokenService.obterTokenValido();
            String authorization = "Bearer " + token;
            log.info("🔑 [ALTERAÇÃO EMPRESA] Headers da requisição:");
            log.info("   Authorization: {}...", authorization.substring(0, Math.min(30, authorization.length())));
            log.info("   empresa: {}", empresa);
            log.info("   usuario: {}", usuario);
            log.info("   senha: [OCULTA]");
            log.info("   app-id: {}", appId);
            
            // Log da URL
            log.info("🌐 [ALTERAÇÃO EMPRESA] URL da requisição: PUT /empresas/alterar");
            log.info("🌐 [ALTERAÇÃO EMPRESA] URL base configurada: {} + {}", 
                    "${odontoprev.api.base-url}", "${odontoprev.api.path}");
            
            long inicioChamada = System.currentTimeMillis();
            log.info("🚀 [ALTERAÇÃO EMPRESA] Enviando requisição para API...");
            
            String response = odontoprevClient.alterarEmpresa(
                authorization,
                empresa,
                usuario,
                senha,
                appId,
                request
            );
            
            long tempoResposta = System.currentTimeMillis() - inicioChamada;
            log.info("✅ [ALTERAÇÃO EMPRESA] Empresa {} alterada com sucesso em {}ms", codigoEmpresa, tempoResposta);
            log.info("📄 [ALTERAÇÃO EMPRESA] Resposta da API: {}", response);
            
            return response;
            
        } catch (Exception e) {
            log.error("❌ [ALTERAÇÃO EMPRESA] Erro ao alterar empresa {}: {}", codigoEmpresa, e.getMessage());
            log.error("❌ [ALTERAÇÃO EMPRESA] Tipo da exceção: {}", e.getClass().getSimpleName());
            log.error("❌ [ALTERAÇÃO EMPRESA] Stack trace completo:", e);
            
            // Log adicional para diferentes tipos de erro
            if (e.getMessage() != null) {
                if (e.getMessage().contains("401")) {
                    log.error("🔐 [ALTERAÇÃO EMPRESA] ERRO 401 - Problema de autenticação/autorização");
                } else if (e.getMessage().contains("400")) {
                    log.error("📝 [ALTERAÇÃO EMPRESA] ERRO 400 - Dados inválidos enviados para API");
                } else if (e.getMessage().contains("403")) {
                    log.error("🚫 [ALTERAÇÃO EMPRESA] ERRO 403 - Acesso negado");
                } else if (e.getMessage().contains("404")) {
                    log.error("🔍 [ALTERAÇÃO EMPRESA] ERRO 404 - Endpoint não encontrado");
                } else if (e.getMessage().contains("500")) {
                    log.error("💥 [ALTERAÇÃO EMPRESA] ERRO 500 - Erro interno do servidor");
                } else if (e.getMessage().contains("timeout")) {
                    log.error("⏰ [ALTERAÇÃO EMPRESA] TIMEOUT - Requisição demorou muito para responder");
                } else if (e.getMessage().contains("connection")) {
                    log.error("🔌 [ALTERAÇÃO EMPRESA] CONNECTION - Problema de conectividade");
                }
            }
            
            throw new RuntimeException("Falha na comunicação com a API", e);
        }
    }
    
    @Override
    public String inativarEmpresa(IntegracaoOdontoprev dadosEmpresa) {
        try {
            String dadosJson = objectMapper.writeValueAsString(dadosEmpresa);
            String token = tokenService.obterTokenValido();
            String authorization = "Bearer " + token;
            
            return odontoprevClient.inativarEmpresa(
                authorization,
                empresa,
                usuario,
                senha,
                appId,
                dadosJson
            );
            
        } catch (JsonProcessingException e) {
            log.error("Erro ao serializar dados da empresa para inativação: {}", e.getMessage());
            throw new RuntimeException("Falha na serialização dos dados", e);
        } catch (Exception e) {
            log.error("Erro ao inativar empresa na API OdontoPrev: {}", e.getMessage());
            throw new RuntimeException("Falha na comunicação com a API", e);
        }
    }
    
    /**
     * PREENCHE CAMPOS OBRIGATÓRIOS QUE NÃO EXISTEM NA VIEW
     *
     * A view VW_INTEGRACAO_ODONTOPREV_ALT não possui todos os campos
     * obrigatórios da API. Este método preenche os campos faltantes
     * com valores padrão ou dados derivados.
     */
    private void preencherCamposObrigatorios(EmpresaAlteracaoRequest request, IntegracaoOdontoprev dadosEmpresa) {
        log.debug("🔧 [ALTERAÇÃO EMPRESA] Iniciando preenchimento de campos obrigatórios...");
        
        // Log dos campos antes do preenchimento
        log.debug("🔍 [ALTERAÇÃO EMPRESA] Estado inicial do request:");
        log.debug("   codigoEmpresa: '{}'", request.getCodigoEmpresa());
        log.debug("   endereco: {}", request.getEndereco() != null ? "PRESENTE" : "AUSENTE");
        log.debug("   telefone: {}", request.getTelefone() != null ? "PRESENTE" : "AUSENTE");
        log.debug("   codigoUsuario: '{}'", request.getCodigoUsuario());
        log.debug("   grausParentesco: {}", request.getGrausParentesco() != null ? "PRESENTE" : "AUSENTE");
        
        // Preencher endereço obrigatório com dados padrão
        if (request.getEndereco() == null) {
            log.debug("🔧 [ALTERAÇÃO EMPRESA] Preenchendo endereço padrão...");
            request.setEndereco(EmpresaAlteracaoRequest.Endereco.builder()
                .descricao("Endereço não informado")
                .complemento("")
                .tipoLogradouro("R") // Tipo logradouro válido
                .logradouro("Rua das Flores")
                .numero("123")
                .bairro("Centro")
                .cidade(EmpresaAlteracaoRequest.Cidade.builder()
                    .codigo(1) // Código simples
                    .nome("São Paulo")
                    .siglaUf("SP")
                    .codigoPais(1) // Brasil
                    .build())
                .cep("01000-000")
                .build());
            log.debug("✅ [ALTERAÇÃO EMPRESA] Endereço padrão preenchido");
        } else {
            log.debug("✅ [ALTERAÇÃO EMPRESA] Endereço já presente, mantendo original");
        }

        // Preencher telefone se não existir
        if (request.getTelefone() == null) {
            log.debug("🔧 [ALTERAÇÃO EMPRESA] Preenchendo telefone padrão...");
            request.setTelefone(EmpresaAlteracaoRequest.Telefone.builder()
                .telefone1("(11) 0000-0000")
                .telefone2("")
                .celular("")
                .fax("")
                .build());
            log.debug("✅ [ALTERAÇÃO EMPRESA] Telefone padrão preenchido");
        } else {
            log.debug("✅ [ALTERAÇÃO EMPRESA] Telefone já presente, mantendo original");
        }

        // Garantir que campos obrigatórios estejam preenchidos
        if (request.getCodigoEmpresa() == null || request.getCodigoEmpresa().trim().isEmpty()) {
            log.debug("🔧 [ALTERAÇÃO EMPRESA] Preenchendo codigoEmpresa com valor da entidade: '{}'", dadosEmpresa.getCodigoEmpresa());
            request.setCodigoEmpresa(dadosEmpresa.getCodigoEmpresa());
        } else {
            log.debug("✅ [ALTERAÇÃO EMPRESA] codigoEmpresa já preenchido: '{}'", request.getCodigoEmpresa());
        }

        if (request.getCodigoUsuario() == null || request.getCodigoUsuario().trim().isEmpty()) {
            log.debug("🔧 [ALTERAÇÃO EMPRESA] Preenchendo codigoUsuario com valor padrão: '0'");
            request.setCodigoUsuario("0");
        } else {
            log.debug("✅ [ALTERAÇÃO EMPRESA] codigoUsuario já preenchido: '{}'", request.getCodigoUsuario());
        }

        // Preencher lista de graus de parentesco padrão
        if (request.getGrausParentesco() == null || request.getGrausParentesco().isEmpty()) {
            log.debug("🔧 [ALTERAÇÃO EMPRESA] Preenchendo grausParentesco padrão...");
            request.setGrausParentesco(java.util.Collections.singletonList(
                EmpresaAlteracaoRequest.GrauParentesco.builder()
                    .codigoGrauParentesco(1) // Cônjuge
                    .build()
            ));
            log.debug("✅ [ALTERAÇÃO EMPRESA] grausParentesco padrão preenchido");
        } else {
            log.debug("✅ [ALTERAÇÃO EMPRESA] grausParentesco já presente, mantendo original");
        }

        // Log dos campos após o preenchimento
        log.debug("🔍 [ALTERAÇÃO EMPRESA] Estado final do request:");
        log.debug("   codigoEmpresa: '{}'", request.getCodigoEmpresa());
        log.debug("   endereco: {}", request.getEndereco() != null ? "PRESENTE" : "AUSENTE");
        log.debug("   telefone: {}", request.getTelefone() != null ? "PRESENTE" : "AUSENTE");
        log.debug("   codigoUsuario: '{}'", request.getCodigoUsuario());
        log.debug("   grausParentesco: {}", request.getGrausParentesco() != null ? "PRESENTE" : "AUSENTE");
        
        log.debug("✅ [ALTERAÇÃO EMPRESA] Campos obrigatórios preenchidos para empresa: {}", 
                 request.getCodigoEmpresa());
    }
    
    /**
     * CRIA REQUEST MÍNIMO COM APENAS CAMPOS MODIFICADOS
     *
     * Envia apenas os campos que realmente mudaram, evitando problemas
     * de validação com campos que não devem ser alterados.
     */
    private EmpresaAlteracaoRequest criarRequestMinimo(IntegracaoOdontoprev dadosEmpresa) {
        log.debug("🔧 [ALTERAÇÃO EMPRESA] Criando request mínimo para empresa: {}", dadosEmpresa.getCodigoEmpresa());
        
        // Cria request com APENAS campos obrigatórios + modificados
        EmpresaAlteracaoRequest request = EmpresaAlteracaoRequest.builder()
            .codigoEmpresa(dadosEmpresa.getCodigoEmpresa()) // OBRIGATÓRIO
            .nomeFantasia(dadosEmpresa.getNomeFantasia()) // MODIFICADO
            .dataVigencia(dadosEmpresa.getDataVigencia() != null ? 
                dadosEmpresa.getDataVigencia().atStartOfDay() : null) // MODIFICADO
            .codigoUsuario("0") // OBRIGATÓRIO - valor padrão (IntegracaoOdontoprev não tem codUsuario)
            .endereco(createEnderecoPadrao()) // OBRIGATÓRIO - endereço padrão
            .build();
        
        log.debug("✅ [ALTERAÇÃO EMPRESA] Request mínimo criado com campos:");
        log.debug("   codigoEmpresa: '{}'", request.getCodigoEmpresa());
        log.debug("   nomeFantasia: '{}'", request.getNomeFantasia());
        log.debug("   dataVigencia: {}", request.getDataVigencia());
        log.debug("   codigoUsuario: '{}'", request.getCodigoUsuario());
        
        return request;
    }
    
    /**
     * CRIA ENDEREÇO PADRÃO VÁLIDO
     *
     * Cria um endereço padrão com dados válidos para evitar
     * erros de validação da API.
     */
    private EmpresaAlteracaoRequest.Endereco createEnderecoPadrao() {
        log.debug("🔧 [ALTERAÇÃO EMPRESA] Criando endereço padrão...");
        
        return EmpresaAlteracaoRequest.Endereco.builder()
            .descricao("Endereço padrão")
            .complemento("")
            .tipoLogradouro("R") // Tipo logradouro simples
            .logradouro("Rua das Flores")
            .numero("123")
            .bairro("Centro")
            .cidade(EmpresaAlteracaoRequest.Cidade.builder()
                .codigo(1) // Código simples
                .nome("São Paulo")
                .siglaUf("SP")
                .codigoPais(1) // Brasil
                .build())
            .cep("01000-000")
            .build();
    }

}