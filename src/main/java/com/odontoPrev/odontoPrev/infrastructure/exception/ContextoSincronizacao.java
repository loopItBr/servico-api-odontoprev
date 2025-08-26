package com.odontoPrev.odontoPrev.infrastructure.exception;

import lombok.Getter;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Classe utilitária para gerenciar contexto de sincronização.
 * Simplifica a criação e manipulação de contextos para logs e exceções.
 */
@Getter
public class ContextoSincronizacao {
    
    private final Map<String, Object> dados;
    private final LocalDateTime timestamp;
    
    private ContextoSincronizacao() {
        this.timestamp = LocalDateTime.now();
        this.dados = new HashMap<>();
        this.dados.put("timestamp", timestamp);
    }
    
    /**
     * Cria um novo contexto com timestamp automático
     */
    public static ContextoSincronizacao criar() {
        return new ContextoSincronizacao();
    }
    
    /**
     * Cria um novo contexto com operação específica
     */
    public static ContextoSincronizacao criar(String operacao) {
        return criar().com("operacao", operacao);
    }
    
    /**
     * Adiciona um par chave-valor ao contexto
     */
    public ContextoSincronizacao com(String chave, Object valor) {
        this.dados.put(chave, valor);
        return this;
    }
    
    /**
     * Adiciona múltiplos valores ao contexto
     */
    public ContextoSincronizacao com(Map<String, Object> valores) {
        this.dados.putAll(valores);
        return this;
    }
    
    /**
     * Adiciona informação de erro ao contexto
     */
    public ContextoSincronizacao comErro(Exception e) {
        return this
            .com("tipoErro", e.getClass().getSimpleName())
            .com("mensagemErro", e.getMessage())
            .com("resultado", "erro");
    }
    
    /**
     * Adiciona informação de erro com detalhes
     */
    public ContextoSincronizacao comErro(String tipoErro, String mensagem) {
        return this
            .com("tipoErro", tipoErro)
            .com("mensagemErro", mensagem)
            .com("resultado", "erro");
    }
    
    /**
     * Marca contexto como sucesso
     */
    public ContextoSincronizacao comSucesso() {
        return this.com("resultado", "sucesso");
    }
    
    /**
     * Adiciona informações de thread
     */
    public ContextoSincronizacao comThreadInfo() {
        return this.com("thread", Thread.currentThread().getName());
    }
    
    /**
     * Adiciona informações de duração
     */
    public ContextoSincronizacao comDuracao(LocalDateTime inicio) {
        LocalDateTime fim = LocalDateTime.now();
        long duracaoMs = java.time.Duration.between(inicio, fim).toMillis();
        return this
            .com("inicioExecucao", inicio)
            .com("fimExecucao", fim)
            .com("duracaoMs", duracaoMs);
    }
    
    /**
     * Retorna o mapa de dados para uso em logs
     */
    public Map<String, Object> paraMapa() {
        return new HashMap<>(dados);
    }
    
    @Override
    public String toString() {
        return dados.toString();
    }
    
    /**
     * Métodos de conveniência para contextos comuns
     */
    public static class Operacao {
        public static final String INICIALIZACAO_SCHEDULER = "inicializacao_scheduler";
        public static final String EXECUCAO_SCHEDULER = "execucao_scheduler";
        public static final String CONTROLE_EXECUCAO = "controle_execucao";
        public static final String SINCRONIZACAO = "sincronizacao";
        public static final String CONTAGEM_EMPRESAS = "contagem_empresas";
        public static final String PROCESSAMENTO_LOTE = "processamento_lote";
        public static final String PROCESSAMENTO_EMPRESA = "processamento_empresa";
        public static final String BUSCA_PAGINADA = "busca_paginada";
        public static final String VALIDACAO_CONFIGURACAO = "validacao_configuracao";
        public static final String COMUNICACAO_API = "comunicacao_api";
        public static final String AUTENTICACAO = "autenticacao";
    }
}