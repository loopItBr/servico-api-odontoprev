package com.odontoPrev.odontoPrev.infrastructure.client.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.odontoPrev.odontoPrev.domain.service.ConsultaEmpresaOdontoprevExpandidaService;
import com.odontoPrev.odontoPrev.infrastructure.client.OdontoprevClient;
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
            String authorization = "Bearer " + obterTokenAutenticacao();
            
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
        try {
            String dadosJson = objectMapper.writeValueAsString(dadosEmpresa);
            String authorization = "Bearer " + obterTokenAutenticacao();
            
            return odontoprevClient.alterarEmpresa(
                authorization,
                empresa,
                usuario,
                senha,
                appId,
                dadosJson
            );
            
        } catch (JsonProcessingException e) {
            log.error("Erro ao serializar dados da empresa para alteração: {}", e.getMessage());
            throw new RuntimeException("Falha na serialização dos dados", e);
        } catch (Exception e) {
            log.error("Erro ao alterar empresa na API OdontoPrev: {}", e.getMessage());
            throw new RuntimeException("Falha na comunicação com a API", e);
        }
    }
    
    @Override
    public String inativarEmpresa(IntegracaoOdontoprev dadosEmpresa) {
        try {
            String dadosJson = objectMapper.writeValueAsString(dadosEmpresa);
            String authorization = "Bearer " + obterTokenAutenticacao();
            
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
     * Obtém o token de autenticação da API OdontoPrev.
     * TODO: Implementar lógica de autenticação real
     * 
     * @return Token de autenticação
     */
    private String obterTokenAutenticacao() {
        // TODO: Implementar autenticação real com OdontoprevAuthClient
        return "token-placeholder";
    }
}