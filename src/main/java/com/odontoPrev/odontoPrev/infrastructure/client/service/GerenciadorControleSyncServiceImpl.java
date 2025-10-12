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
import java.util.Optional;

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
        return criarOuAtualizarControle(codigoEmpresa, dados, ControleSync.TipoOperacao.CREATE, ControleSync.TipoControle.ADICAO);
    }
    
    @Override
    public ControleSync criarControle(String codigoEmpresa, IntegracaoOdontoprev dados, ControleSync.TipoOperacao tipoOperacao) {
        // Mapeia automaticamente o tipo de controle baseado na operação
        ControleSync.TipoControle tipoControle = mapearTipoControle(tipoOperacao);
        return criarControle(codigoEmpresa, dados, tipoOperacao, tipoControle);
    }
    
    @Override
    public ControleSync criarControle(String codigoEmpresa, IntegracaoOdontoprev dados, ControleSync.TipoOperacao tipoOperacao, ControleSync.TipoControle tipoControle) {
        return criarOuAtualizarControle(codigoEmpresa, dados, tipoOperacao, tipoControle);
    }
    
    /**
     * CRIA OU ATUALIZA REGISTRO DE CONTROLE
     * 
     * Verifica se já existe um registro de controle para esta empresa e tipo de operação.
     * Se existir e o status for SUCCESS, não cria novo registro.
     * Se existir e o status for ERROR ou PENDING, atualiza o registro existente.
     * Se não existir, cria um novo registro.
     */
    private ControleSync criarOuAtualizarControle(String codigoEmpresa, IntegracaoOdontoprev dados, 
                                                  ControleSync.TipoOperacao tipoOperacao, ControleSync.TipoControle tipoControle) {
        try {
            String dadosJson = objectMapper.writeValueAsString(dados);
            
            // Verificar se já existe um registro de controle para esta empresa e tipo
            Optional<ControleSync> controleExistente = repository
                    .findByCodigoEmpresaAndTipoControle(codigoEmpresa, tipoControle.getCodigo());
            
            if (controleExistente.isPresent()) {
                ControleSync controle = controleExistente.get();
                
                // Se já foi processado com sucesso, não criar novo registro
                if (controle.getStatusSync() == ControleSync.StatusSync.SUCCESS) {
                    log.info("🔄 [CONTROLE] Empresa {} já foi processada com sucesso, não criando novo registro", codigoEmpresa);
                    return controle;
                }
                
                // Se está em erro ou pendente, atualizar o registro existente
                log.info("🔄 [CONTROLE] Atualizando registro existente para empresa {} - Status atual: {}", 
                        codigoEmpresa, controle.getStatusSync());
                
                controle.setDadosJson(dadosJson);
                controle.setStatusSync(ControleSync.StatusSync.PENDING);
                controle.setDataCriacao(LocalDateTime.now());
                controle.setResponseApi(null);
                controle.setErroMensagem(null);
                
                return controle;
            } else {
                // Criar novo registro
                log.info("🆕 [CONTROLE] Criando novo registro de controle para empresa {}", codigoEmpresa);
                
                String endpoint = determinarEndpoint(tipoOperacao, codigoEmpresa);
                
                return ControleSync.builder()
                        .codigoEmpresa(codigoEmpresa)
                        .tipoOperacao(tipoOperacao)
                        .tipoControle(tipoControle.getCodigo())
                        .endpointDestino(endpoint)
                        .dadosJson(dadosJson)
                        .statusSync(ControleSync.StatusSync.PENDING)
                        .dataCriacao(LocalDateTime.now())
                        .build();
            }
                    
        } catch (JsonProcessingException e) {
            log.error("Erro ao serializar dados da empresa {}: {}", codigoEmpresa, e.getMessage());
            throw new RuntimeException("Falha na criação do controle de sync", e);
        }
    }
    
    /**
     * Mapeia automaticamente o tipo de controle baseado na operação.
     */
    private ControleSync.TipoControle mapearTipoControle(ControleSync.TipoOperacao tipoOperacao) {
        return switch (tipoOperacao) {
            case CREATE -> ControleSync.TipoControle.ADICAO;
            case UPDATE -> ControleSync.TipoControle.ALTERACAO;
            case DELETE -> ControleSync.TipoControle.EXCLUSAO;
        };
    }
    
    /**
     * Determina o endpoint correto baseado no tipo de operação.
     */
    private String determinarEndpoint(ControleSync.TipoOperacao tipoOperacao, String codigoEmpresa) {
        return switch (tipoOperacao) {
            case CREATE -> "/empresas/" + codigoEmpresa;
            case UPDATE -> "/empresas/" + codigoEmpresa + "/atualizar";
            case DELETE -> "/empresas/" + codigoEmpresa + "/excluir";
        };
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