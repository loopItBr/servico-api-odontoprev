package com.odontoPrev.odontoPrev.infrastructure.client.domain.service;

import com.odontoPrev.odontoPrev.infrastructure.client.adapter.out.dto.TokenRequest;
import com.odontoPrev.odontoPrev.infrastructure.client.adapter.out.dto.TokenResponse;
import com.odontoPrev.odontoPrev.infrastructure.client.OdontoprevAuthClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class TokenService {

    private final OdontoprevAuthClient authClient;
    
    @Value("${odontoprev.credentials.token}")
    private String credentialsToken;
    
    private String accessToken;
    private LocalDateTime tokenExpiration;

    public String obterTokenValido() {
        if (tokenExpirado()) {
            renovarToken();
        }
        return accessToken;
    }

    private boolean tokenExpirado() {
        return accessToken == null || 
               tokenExpiration == null || 
               LocalDateTime.now().isAfter(tokenExpiration.minusMinutes(5));
    }

    private void renovarToken() {
        try {
            log.info("Renovando token de acesso da Odontoprev");
            
            String authorization = "Basic " + credentialsToken;
            TokenRequest request = new TokenRequest();
            
            TokenResponse response = authClient.obterToken(authorization, request.getGrantType());
            
            this.accessToken = response.getAccessToken();
            this.tokenExpiration = LocalDateTime.now().plusSeconds(response.getExpiresIn());
            
            log.info("Token renovado com sucesso. Expira em: {}", tokenExpiration);
            
        } catch (Exception e) {
            log.error("Erro ao renovar token da Odontoprev", e);
            throw new RuntimeException("Falha na autenticação com Odontoprev", e);
        }
    }
}