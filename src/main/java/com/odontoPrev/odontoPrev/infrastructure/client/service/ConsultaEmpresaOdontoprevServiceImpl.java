package com.odontoPrev.odontoPrev.infrastructure.client.service;

import com.odontoPrev.odontoPrev.domain.service.ConsultaEmpresaOdontoprevService;
import com.odontoPrev.odontoPrev.infrastructure.client.OdontoprevClient;
import com.odontoPrev.odontoPrev.infrastructure.client.adapter.out.dto.EmpresaResponse;
import com.odontoPrev.odontoPrev.infrastructure.client.domain.service.TokenService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
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
    public EmpresaResponse buscarEmpresa(String codigoEmpresa) {
        log.debug("Buscando empresa: {}", codigoEmpresa);
        
        long inicioTempo = System.currentTimeMillis();
        
        String token = tokenService.obterTokenValido();
        String authorization = "Bearer " + token;
        
        EmpresaResponse response = odontoprevClient.obterEmpresaPorCodigo(
                authorization, empresa, usuario, senha, appId, codigoEmpresa
        );
        
        long tempoResposta = System.currentTimeMillis() - inicioTempo;
        log.debug("Empresa {} encontrada com sucesso em {}ms", codigoEmpresa, tempoResposta);
        
        return response;
    }
}