package com.odontoPrev.odontoPrev.infrastructure.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.odontoPrev.odontoPrev.infrastructure.client.adapter.out.dto.EmpresaResponse;
import com.odontoPrev.odontoPrev.infrastructure.client.OdontoprevClient;
import com.odontoPrev.odontoPrev.infrastructure.client.domain.service.TokenService;
import com.odontoPrev.odontoPrev.infrastructure.repository.ControleSyncRepository;
import com.odontoPrev.odontoPrev.infrastructure.repository.IntegracaoOdontoprevRepository;
import com.odontoPrev.odontoPrev.infrastructure.repository.entity.ControleSync;
import com.odontoPrev.odontoPrev.infrastructure.repository.entity.IntegracaoOdontoprev;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class SyncOdontoprevService {

    private final IntegracaoOdontoprevRepository integracaoRepository;
    private final ControleSyncRepository controleSyncRepository;
    private final OdontoprevClient odontoprevClient;
    private final TokenService tokenService;
    private final ObjectMapper objectMapper;

    @Value("${odontoprev.credentials.empresa}")
    private String empresa;

    @Value("${odontoprev.credentials.usuario}")
    private String usuario;

    @Value("${odontoprev.credentials.senha}")
    private String senha;

    @Value("${odontoprev.credentials.app-id}")
    private String appId;

    @Transactional
    public void executarSincronizacao() {
        log.info("Iniciando sincronização com OdontoPrev - {}", LocalDateTime.now());

        try {
            List<String> codigosEmpresas = integracaoRepository.buscarCodigosEmpresasDisponiveis();
            log.info("Encontradas {} empresas para sincronização", codigosEmpresas.size());

            for (String codigoEmpresa : codigosEmpresas) {
                processarEmpresa(codigoEmpresa);
            }

        } catch (Exception e) {
            log.error("Erro durante a sincronização com OdontoPrev", e);
        }

        log.info("Sincronização com OdontoPrev finalizada - {}", LocalDateTime.now());
    }

    private void processarEmpresa(String codigoEmpresa) {
        log.debug("Processando empresa: {}", codigoEmpresa);

        List<IntegracaoOdontoprev> dadosEmpresa = integracaoRepository.buscarDadosPorCodigoEmpresa(codigoEmpresa);
        
        if (dadosEmpresa.isEmpty()) {
            log.warn("Nenhum dado encontrado para a empresa: {}", codigoEmpresa);
            return;
        }

        IntegracaoOdontoprev dadosCompletos = dadosEmpresa.get(0);
        
        ControleSync controleSync = criarControleSyncInicial(codigoEmpresa, dadosCompletos);
        controleSync = controleSyncRepository.save(controleSync);

        tentarBuscarEmpresaNaOdontoprev(controleSync, codigoEmpresa);
    }

    private ControleSync criarControleSyncInicial(String codigoEmpresa, IntegracaoOdontoprev dados) {
        try {
            String dadosJson = objectMapper.writeValueAsString(dados);
            
            ControleSync controle = new ControleSync();
            controle.setCodigoEmpresa(codigoEmpresa);
            controle.setTipoOperacao(ControleSync.TipoOperacao.GET);
            controle.setEndpointDestino("/empresas/" + codigoEmpresa);
            controle.setDadosJson(dadosJson);
            controle.setStatusSync(ControleSync.StatusSync.PENDING);
            controle.setTentativas(0);
            controle.setMaxTentativas(3);
            controle.setDataCriacao(LocalDateTime.now());
            
            return controle;
            
        } catch (Exception e) {
            log.error("Erro ao serializar dados da empresa {}", codigoEmpresa, e);
            throw new RuntimeException("Falha na criação do controle de sync", e);
        }
    }

    private void tentarBuscarEmpresaNaOdontoprev(ControleSync controleSync, String codigoEmpresa) {
        int tentativasMaximas = controleSync.getMaxTentativas();
        
        for (int tentativa = 1; tentativa <= tentativasMaximas; tentativa++) {
            controleSync.setTentativas(tentativa);
            controleSync.setDataUltimaTentativa(LocalDateTime.now());
            
            log.debug("Tentativa {} de {} para empresa: {}", tentativa, tentativasMaximas, codigoEmpresa);
            
            try {
                long inicioTempo = System.currentTimeMillis();
                
                String token = tokenService.obterTokenValido();
                String authorization = "Bearer " + token;
                
                EmpresaResponse response = odontoprevClient.obterEmpresaPorCodigo(
                    authorization, empresa, usuario, senha, appId, codigoEmpresa
                );
                
                long tempoResposta = System.currentTimeMillis() - inicioTempo;
                
                processarSucesso(controleSync, response, tempoResposta);
                return;
                
            } catch (Exception e) {
                log.error("Erro na tentativa {} para empresa {}: {}", tentativa, codigoEmpresa, e.getMessage());
                
                if (tentativa == tentativasMaximas) {
                    processarErroFinal(controleSync, e);
                } else {
                    controleSync.setStatusSync(ControleSync.StatusSync.RETRY);
                    controleSync.setErroMensagem("Tentativa " + tentativa + " falhou: " + e.getMessage());
                }
                
                controleSyncRepository.save(controleSync);
                
                if (tentativa < tentativasMaximas) {
                    aguardarEntreRetry();
                }
            }
        }
    }

    private void processarSucesso(ControleSync controleSync, EmpresaResponse response, long tempoResposta) {
        try {
            String responseJson = objectMapper.writeValueAsString(response);
            
            controleSync.setStatusSync(ControleSync.StatusSync.SUCCESS);
            controleSync.setResponseApi(responseJson);
            controleSync.setDataSucesso(LocalDateTime.now());
            controleSync.setTempoRespostaMs(tempoResposta);
            controleSync.setErroMensagem(null);
            
            controleSyncRepository.save(controleSync);
            
            log.info("Sucesso na sincronização da empresa: {} em {}ms", 
                controleSync.getCodigoEmpresa(), tempoResposta);
                
        } catch (Exception e) {
            log.error("Erro ao serializar resposta da empresa {}", controleSync.getCodigoEmpresa(), e);
            processarErroFinal(controleSync, e);
        }
    }

    private void processarErroFinal(ControleSync controleSync, Exception e) {
        controleSync.setStatusSync(ControleSync.StatusSync.ERROR);
        controleSync.setErroMensagem("Falha após " + controleSync.getMaxTentativas() + 
            " tentativas. Último erro: " + e.getMessage());
        controleSync.setResponseApi(null);
        
        controleSyncRepository.save(controleSync);
        
        log.error("Erro final na sincronização da empresa: {} após {} tentativas", 
            controleSync.getCodigoEmpresa(), controleSync.getMaxTentativas());
    }

    private void aguardarEntreRetry() {
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("Interrompido durante aguardo entre retries");
        }
    }
}