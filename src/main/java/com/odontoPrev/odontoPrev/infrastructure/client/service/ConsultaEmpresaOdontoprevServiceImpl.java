package com.odontoPrev.odontoPrev.infrastructure.client.service;

import com.odontoPrev.odontoPrev.domain.service.ConsultaEmpresaOdontoprevService;
import com.odontoPrev.odontoPrev.infrastructure.client.OdontoprevClient;
import com.odontoPrev.odontoPrev.infrastructure.client.adapter.out.dto.EmpresaResponse;
import com.odontoPrev.odontoPrev.infrastructure.client.domain.service.TokenService;
import feign.FeignException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;

/**
 * Implementação do serviço de consulta de empresas na OdontoPrev.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ConsultaEmpresaOdontoprevServiceImpl implements ConsultaEmpresaOdontoprevService {

    private final OdontoprevClient odontoprevClient;
    private final TokenService tokenService;

    @Value("${odontoprev.credentials.empresa}")
    private String empresa;

    @Value("${odontoprev.credentials.usuario}")
    private String usuario;

    @Value("${odontoprev.credentials.senha}")
    private String senha;

    @Value("${odontoprev.credentials.app-id}")
    private String appId;

    @Override
    @Retryable(
            value = { FeignException.TooManyRequests.class, FeignException.ServiceUnavailable.class, FeignException.GatewayTimeout.class },
            maxAttempts = 5,
            backoff = @Backoff(delay = 15000, multiplier = 1.5)
    )
    public EmpresaResponse buscarEmpresa(String codigoEmpresa) {
        log.debug("Buscando empresa: {}", codigoEmpresa);
        
        long inicioTempo = System.currentTimeMillis();

        if (codigoEmpresa.length() < 6) {
            codigoEmpresa = String.format("%-6s", codigoEmpresa).replace(' ', '0');
        };

        String token = tokenService.obterTokenValido();
        String authorization = "Bearer " + token;
        
        EmpresaResponse response = odontoprevClient.obterEmpresaPorCodigo(
                authorization, empresa, usuario, senha, appId, codigoEmpresa
        );

        long tempoResposta = System.currentTimeMillis() - inicioTempo;
        log.debug("Empresa {} encontrada com sucesso em {}ms", codigoEmpresa, tempoResposta);
        
        return response;
    }
    
    @Recover
    public EmpresaResponse recover(FeignException e, String codigoEmpresa) {
        log.error("Não foi possível buscar a empresa {} após múltiplas tentativas. Falha definitiva.", codigoEmpresa, e);
        throw new RuntimeException("Falha definitiva ao consultar a API da OdontoPrev para a empresa " + codigoEmpresa, e);
    }
}