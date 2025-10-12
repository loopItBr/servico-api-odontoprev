package com.odontoPrev.odontoPrev.infrastructure.client.service;

import com.odontoPrev.odontoPrev.infrastructure.client.OdontoprevAuthClient;
import com.odontoPrev.odontoPrev.infrastructure.client.adapter.out.dto.LoginEmpresaRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * SERVIÇO DE AUTENTICAÇÃO ESPECÍFICO PARA API DE BENEFICIÁRIOS
 *
 * Este serviço gerencia a autenticação dupla necessária para a API de beneficiários:
 * 1. Token OAuth2 (Bearer 1) - obtido via /oauth2/token
 * 2. Token Login Empresa (Bearer 2) - obtido via /empresa-login/1.0/api/auth/token
 *
 * FLUXO DE AUTENTICAÇÃO:
 * 1. Obtém token OAuth2 usando app-token
 * 2. Usa token OAuth2 para obter token de login empresa
 * 3. Retorna ambos os tokens para uso nas chamadas da API
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BeneficiarioTokenService {

    private final OdontoprevAuthClient odontoprevAuthClient;


    @Value("${odontoprev.api.app-token}")
    private String appToken;

    @Value("${odontoprev.api.login.usuario}")
    private String usuario;

    @Value("${odontoprev.api.login.senha}")
    private String senha;

    @Value("${odontoprev.api.login.app-id}")
    private String appId;

    /**
     * OBTÉM TOKEN OAUTH2 PARA API DE BENEFICIÁRIOS
     *
     * @return token OAuth2 (Bearer 1)
     */
    public String obterTokenOAuth2() {
        try {
            log.info("🔑 [TOKEN OAUTH2] Iniciando obtenção do token OAuth2 para API de beneficiários");
            log.info("🔑 [TOKEN OAUTH2] URL: https://apim-hml.odontoprev.com.br/oauth2/token");
            log.info("🔑 [TOKEN OAUTH2] Authorization: Basic {}...", appToken.substring(0, Math.min(20, appToken.length())));
            log.info("🔑 [TOKEN OAUTH2] Grant Type: client_credentials");
            
            long inicio = System.currentTimeMillis();
            var response = odontoprevAuthClient.obterToken("Basic " + appToken, "client_credentials");
            long tempo = System.currentTimeMillis() - inicio;
            
            String token = "Bearer " + response.getAccessToken();
            log.info("✅ [TOKEN OAUTH2] Token OAuth2 obtido com sucesso em {}ms", tempo);
            log.info("✅ [TOKEN OAUTH2] Token: {}...", token.substring(0, Math.min(30, token.length())));
            
            return token;
        } catch (Exception e) {
            log.error("❌ [TOKEN OAUTH2] Erro ao obter token OAuth2 para beneficiários: {}", e.getMessage());
            throw new RuntimeException("Falha na autenticação OAuth2 para beneficiários", e);
        }
    }

    /**
     * OBTÉM TOKEN DE LOGIN EMPRESA PARA API DE BENEFICIÁRIOS
     *
     * @param tokenOAuth2 token OAuth2 obtido anteriormente
     * @return token de login empresa (Bearer 2)
     */
    public String obterTokenLoginEmpresa(String tokenOAuth2) {
        try {
            log.info("🔑 [TOKEN LOGIN EMPRESA] Iniciando obtenção do token de login empresa para API de beneficiários");
            log.info("🔑 [TOKEN LOGIN EMPRESA] URL: https://apim-hml.odontoprev.com.br/empresa-login/1.0/api/auth/token");
            log.info("🔑 [TOKEN LOGIN EMPRESA] Authorization: {}...", tokenOAuth2.substring(0, Math.min(30, tokenOAuth2.length())));
            log.info("🔑 [TOKEN LOGIN EMPRESA] App ID: {}", appId);
            log.info("🔑 [TOKEN LOGIN EMPRESA] Usuário: {}", usuario);
            log.info("🔑 [TOKEN LOGIN EMPRESA] Senha: [OCULTA]");
            
            var request = new LoginEmpresaRequest(appId, usuario, senha);
            log.info("🔑 [TOKEN LOGIN EMPRESA] Request Body: {}", request);
            log.info("🔑 [TOKEN LOGIN EMPRESA] AppId: '{}', Usuario: '{}', Senha: [OCULTA]", appId, usuario);
            
            long inicio = System.currentTimeMillis();
            var response = odontoprevAuthClient.obterTokenLoginEmpresa(tokenOAuth2, request);
            long tempo = System.currentTimeMillis() - inicio;
            
            log.info("🔑 [TOKEN LOGIN EMPRESA] Resposta recebida em {}ms: {}", tempo, response);
            
            if (response == null) {
                throw new RuntimeException("Resposta nula do token de login empresa");
            }
            
            // Obtém o token do campo accessToken
            String token = response.getAccessToken();
            
            if (token == null || token.trim().isEmpty()) {
                throw new RuntimeException("Token de login empresa não foi retornado pela API. Resposta: " + response);
            }
            
            String tokenCompleto = "Bearer " + token;
            log.info("✅ [TOKEN LOGIN EMPRESA] Token de login empresa obtido com sucesso em {}ms", tempo);
            log.info("✅ [TOKEN LOGIN EMPRESA] Token: {}...", tokenCompleto.substring(0, Math.min(30, tokenCompleto.length())));
            
            return tokenCompleto;
        } catch (Exception e) {
            log.error("❌ [TOKEN LOGIN EMPRESA] Erro ao obter token de login empresa para beneficiários: {}", e.getMessage());
            throw new RuntimeException("Falha na autenticação de login empresa para beneficiários", e);
        }
    }

    /**
     * OBTÉM AMBOS OS TOKENS NECESSÁRIOS PARA API DE BENEFICIÁRIOS
     *
     * @return array com [tokenOAuth2, tokenLoginEmpresa]
     */
    public String[] obterTokensCompletos() {
        log.info("🚀 [AUTENTICAÇÃO COMPLETA] Iniciando processo de autenticação dupla para API de beneficiários");
        log.info("🚀 [AUTENTICAÇÃO COMPLETA] Etapa 1: Obtendo token OAuth2");
        log.info("🚀 [AUTENTICAÇÃO COMPLETA] Etapa 2: Obtendo token de login empresa");
        
        long inicioTotal = System.currentTimeMillis();
        
        String tokenOAuth2 = obterTokenOAuth2();
        String tokenLoginEmpresa = obterTokenLoginEmpresa(tokenOAuth2);
        
        long tempoTotal = System.currentTimeMillis() - inicioTotal;
        
        log.info("🎉 [AUTENTICAÇÃO COMPLETA] Processo de autenticação dupla concluído com sucesso em {}ms", tempoTotal);
        log.info("🎉 [AUTENTICAÇÃO COMPLETA] Token OAuth2: {}...", tokenOAuth2.substring(0, Math.min(30, tokenOAuth2.length())));
        log.info("🎉 [AUTENTICAÇÃO COMPLETA] Token Login Empresa: {}...", tokenLoginEmpresa.substring(0, Math.min(30, tokenLoginEmpresa.length())));
        
        return new String[]{tokenOAuth2, tokenLoginEmpresa};
    }
}
