package com.odontoPrev.odontoPrev.infrastructure.client.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.odontoPrev.odontoPrev.domain.service.ConsultaEmpresaOdontoprevService;
import com.odontoPrev.odontoPrev.domain.service.GerenciadorControleSyncService;
import com.odontoPrev.odontoPrev.domain.service.ProcessamentoEmpresaService;
import com.odontoPrev.odontoPrev.infrastructure.client.adapter.out.dto.EmpresaResponse;
import com.odontoPrev.odontoPrev.infrastructure.repository.IntegracaoOdontoprevRepository;
import com.odontoPrev.odontoPrev.infrastructure.repository.entity.ControleSync;
import com.odontoPrev.odontoPrev.infrastructure.repository.entity.IntegracaoOdontoprev;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Implementação do serviço de processamento de empresas.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ProcessamentoEmpresaServiceImpl implements ProcessamentoEmpresaService {

    private final IntegracaoOdontoprevRepository integracaoRepository;
    private final GerenciadorControleSyncService gerenciadorControleSync;
    private final ConsultaEmpresaOdontoprevService consultaEmpresaService;
    private final ObjectMapper objectMapper;

    @Override
    public void processar(String codigoEmpresa) {
        log.debug("Processando empresa: {}", codigoEmpresa);

        List<IntegracaoOdontoprev> dadosEmpresa = integracaoRepository.buscarDadosPorCodigoEmpresa(codigoEmpresa);
        
        if (dadosEmpresa.isEmpty()) {
            log.warn("Nenhum dado encontrado para a empresa: {}", codigoEmpresa);
            return;
        }

        IntegracaoOdontoprev dadosCompletos = dadosEmpresa.get(0);
        
        ControleSync controleSync = gerenciadorControleSync.criarControle(codigoEmpresa, dadosCompletos);
        controleSync = gerenciadorControleSync.salvar(controleSync);

        buscarEProcessarResposta(controleSync, codigoEmpresa);
    }

    private void buscarEProcessarResposta(ControleSync controleSync, String codigoEmpresa) {
        try {
            long inicioTempo = System.currentTimeMillis();
            
            EmpresaResponse response = consultaEmpresaService.buscarEmpresa(codigoEmpresa);
            
            long tempoResposta = System.currentTimeMillis() - inicioTempo;
            processarSucesso(controleSync, response, tempoResposta);
            
        } catch (Exception e) {
            log.error("Erro ao buscar empresa {}: {}", codigoEmpresa, e.getMessage());
            gerenciadorControleSync.atualizarErro(controleSync, e.getMessage());
            gerenciadorControleSync.salvar(controleSync);
        }
    }

    private void processarSucesso(ControleSync controleSync, EmpresaResponse response, long tempoResposta) {
        try {
            String responseJson = objectMapper.writeValueAsString(response);
            
            gerenciadorControleSync.atualizarSucesso(controleSync, responseJson, tempoResposta);
            gerenciadorControleSync.salvar(controleSync);
            
        } catch (Exception e) {
            log.error("Erro ao processar resposta da empresa {}: {}", 
                    controleSync.getCodigoEmpresa(), e.getMessage());
            
            gerenciadorControleSync.atualizarErro(controleSync, 
                    "Erro ao serializar resposta: " + e.getMessage());
            gerenciadorControleSync.salvar(controleSync);
        }
    }
}