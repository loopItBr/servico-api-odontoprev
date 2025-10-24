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
import java.util.List;
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
        log.info("🔧 [CRIAR CONTROLE] Iniciando criação/atualização para empresa: {}, Tipo: {}", 
                codigoEmpresa, tipoControle);
        
        try {
            String dadosJson = objectMapper.writeValueAsString(dados);
            log.debug("📄 [CRIAR CONTROLE] Dados JSON gerados: {} caracteres", dadosJson.length());
            
            // Verificar se já existe um registro de controle para esta empresa e tipo
            // Usa findFirst para evitar erro quando há múltiplos registros
            log.debug("🔍 [CRIAR CONTROLE] Buscando registro existente para empresa: {}, tipo: {}", codigoEmpresa, tipoControle.getCodigo());
            
            // DEBUG: Verificar se há múltiplos registros
            List<ControleSync> todosControles = repository
                    .findByCodigoEmpresaAndTipoControleOrderByDataCriacaoDesc(codigoEmpresa, tipoControle.getCodigo());
            log.debug("🔍 [CRIAR CONTROLE] Total de registros encontrados: {}", todosControles.size());
            
            if (todosControles.size() > 1) {
                log.warn("⚠️ [CRIAR CONTROLE] MÚLTIPLOS REGISTROS ENCONTRADOS para empresa {} - tipo {}: {}", 
                        codigoEmpresa, tipoControle.getCodigo(), todosControles.size());
                for (int i = 0; i < todosControles.size(); i++) {
                    ControleSync c = todosControles.get(i);
                    log.warn("⚠️ [CRIAR CONTROLE] Registro {}: ID={}, Status={}, Data={}", 
                            i + 1, c.getId(), c.getStatusSync(), c.getDataCriacao());
                }
            }
            
            Optional<ControleSync> controleExistente = repository
                    .findFirstByCodigoEmpresaAndTipoControleOrderByDataCriacaoDesc(codigoEmpresa, tipoControle.getCodigo());
            
            log.debug("🔍 [CRIAR CONTROLE] Resultado da busca: {}", controleExistente.isPresent() ? "ENCONTRADO" : "NÃO ENCONTRADO");
            
            if (controleExistente.isPresent()) {
                ControleSync controle = controleExistente.get();
                log.info("🔄 [CRIAR CONTROLE] Registro existente encontrado - ID: {}, Status: {}", 
                        controle.getId(), controle.getStatusSync());
                
                // Se já foi processado com sucesso, não criar novo registro
                if (controle.getStatusSync() == ControleSync.StatusSync.SUCCESS) {
                    log.info("✅ [CRIAR CONTROLE] Empresa {} já foi processada com sucesso, não criando novo registro", codigoEmpresa);
                    log.info("✅ [CRIAR CONTROLE] Retornando registro existente com ID: {}", controle.getId());
                    return controle;
                }
                
                // Se está em erro ou pendente, atualizar o registro existente
                log.info("🔄 [CRIAR CONTROLE] REUTILIZANDO registro existente para empresa {} - Status atual: {}", 
                        codigoEmpresa, controle.getStatusSync());
                log.info("🔄 [CRIAR CONTROLE] ATENÇÃO: Não criando novo registro - reutilizando ID: {}", controle.getId());
                log.info("🔄 [CRIAR CONTROLE] Atualizando dados do registro existente...");
                
                controle.setDadosJson(dadosJson);
                controle.setStatusSync(ControleSync.StatusSync.PENDING);
                controle.setDataCriacao(LocalDateTime.now());
                controle.setResponseApi(null);
                controle.setErroMensagem(null);
                
                return controle;
            } else {
                // Criar novo registro APENAS se não existir nenhum
                log.info("🆕 [CRIAR CONTROLE] Nenhum registro existente encontrado - Criando novo para empresa {}", codigoEmpresa);
                log.info("🆕 [CRIAR CONTROLE] ATENÇÃO: Este é um NOVO registro - empresa {} não tinha registro anterior", codigoEmpresa);
                
                String endpoint = determinarEndpoint(tipoOperacao, codigoEmpresa);
                log.debug("🌐 [CRIAR CONTROLE] Endpoint determinado: {}", endpoint);
                
                ControleSync novoControle = ControleSync.builder()
                        .codigoEmpresa(codigoEmpresa)
                        .tipoOperacao(tipoOperacao)
                        .tipoControle(tipoControle.getCodigo())
                        .endpointDestino(endpoint)
                        .dadosJson(dadosJson)
                        .statusSync(ControleSync.StatusSync.PENDING)
                        .dataCriacao(LocalDateTime.now())
                        .build();
                
                log.info("📋 [CRIAR CONTROLE] Novo controle criado - Empresa: {}, Tipo: {}, Status: {}", 
                        novoControle.getCodigoEmpresa(), novoControle.getTipoControle(), novoControle.getStatusSync());
                log.info("🆕 [CRIAR CONTROLE] ATENÇÃO: Este é um NOVO registro - empresa {} não tinha registro anterior", codigoEmpresa);
                
                return novoControle;
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
        log.info("🔄 [ATUALIZAR SUCESSO] Iniciando atualização de sucesso para empresa: {}", controle.getCodigoEmpresa());
        log.info("🔄 [ATUALIZAR SUCESSO] ID do controle: {}, Status atual: {}", controle.getId(), controle.getStatusSync());
        
        controle.setStatusSync(ControleSync.StatusSync.SUCCESS);
        controle.setResponseApi(responseJson);
        controle.setDataSucesso(LocalDateTime.now());
        controle.setErroMensagem(null);
        
        log.info("✅ [ATUALIZAR SUCESSO] Controle atualizado - Status: {}, Data sucesso: {}", 
                controle.getStatusSync(), controle.getDataSucesso());
        log.info("✅ [ATUALIZAR SUCESSO] Sincronização bem-sucedida para empresa {} em {}ms", 
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
        log.info("💾 [SALVAR CONTROLE] Iniciando salvamento para empresa: {}", controle.getCodigoEmpresa());
        log.info("💾 [SALVAR CONTROLE] ID: {}, Status: {}, Tipo: {}, Endpoint: {}", 
                controle.getId(), controle.getStatusSync(), controle.getTipoControle(), controle.getEndpointDestino());
        
        try {
            ControleSync saved = repository.save(controle);
            log.info("✅ [SALVAR CONTROLE] Controle salvo com sucesso - ID: {}, Empresa: {}, Status: {}", 
                    saved.getId(), saved.getCodigoEmpresa(), saved.getStatusSync());
            return saved;
        } catch (Exception e) {
            log.error("❌ [SALVAR CONTROLE] Erro ao salvar controle para empresa {}: {}", 
                    controle.getCodigoEmpresa(), e.getMessage(), e);
            throw e;
        }
    }
}