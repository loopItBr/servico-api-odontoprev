package com.odontoPrev.odontoPrev.infrastructure.client.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.odontoPrev.odontoPrev.domain.service.GerenciadorControleSyncService;
import com.odontoPrev.odontoPrev.infrastructure.repository.ControleSyncRepository;
import com.odontoPrev.odontoPrev.infrastructure.repository.entity.ControleSync;
import com.odontoPrev.odontoPrev.infrastructure.repository.entity.IntegracaoOdontoprev;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * Implementação do gerenciador de controle de sincronização.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GerenciadorControleSyncServiceImpl implements GerenciadorControleSyncService {

    private final ControleSyncRepository repository;
    private final ObjectMapper objectMapper;

    @Override
    public ControleSync criarControle(String codigoEmpresa, IntegracaoOdontoprev dados) {
        try {
            String dadosJson = objectMapper.writeValueAsString(dados);
            
            return ControleSync.builder()
                    .codigoEmpresa(codigoEmpresa)
                    .tipoOperacao(ControleSync.TipoOperacao.CREATE)
                    .endpointDestino("/empresas/" + codigoEmpresa)
                    .dadosJson(dadosJson)
                    .statusSync(ControleSync.StatusSync.PENDING)
                    .dataCriacao(LocalDateTime.now())
                    .build();
                    
        } catch (JsonProcessingException e) {
            log.error("Erro ao serializar dados da empresa {}: {}", codigoEmpresa, e.getMessage());
            throw new RuntimeException("Falha na criação do controle de sync", e);
        }
    }

    @Override
    public void atualizarSucesso(ControleSync controle, String responseJson, long tempoResposta) {
        controle.setStatusSync(ControleSync.StatusSync.SUCCESS);
        controle.setResponseApi(responseJson);
        controle.setDataSucesso(LocalDateTime.now());
        controle.setErroMensagem(null);
        
        log.info("Sincronização bem-sucedida para empresa {} em {}ms", 
                controle.getCodigoEmpresa(), tempoResposta);
    }

    @Override
    public void atualizarErro(ControleSync controle, String mensagemErro) {
        controle.setStatusSync(ControleSync.StatusSync.ERROR);
        controle.setErroMensagem(mensagemErro);
        controle.setResponseApi(null);
        
        log.error("Erro na sincronização da empresa {}: {}", 
                controle.getCodigoEmpresa(), mensagemErro);
    }

    @Override
    @Transactional
    public ControleSync salvar(ControleSync controle) {
        log.debug("Salvando controle sync para empresa: {}", controle.getCodigoEmpresa());
        ControleSync saved = repository.save(controle);
        log.debug("Controle sync salvo com ID: {}", saved.getId());
        return saved;
    }
}