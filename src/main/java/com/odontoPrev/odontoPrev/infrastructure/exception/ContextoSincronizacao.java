package com.odontoPrev.odontoPrev.infrastructure.exception;

import lombok.Getter;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * CLASSE UTILITÁRIA PARA CRIAR E GERENCIAR CONTEXTOS DE SINCRONIZAÇÃO
 * 
 * O QUE É UM "CONTEXTO":
 * É um conjunto de informações sobre o que está acontecendo no sistema
 * no momento de uma operação. Por exemplo: qual empresa está sendo processada,
 * que horas começou, qual thread está executando, etc.
 * 
 * POR QUE PRECISAMOS DISSO:
 * - Para debugar erros: saber exatamente o que estava acontecendo
 * - Para monitoramento: acompanhar performance e problemas
 * - Para logs organizados: todas as informações importantes em um lugar
 * - Para rastreabilidade: poder investigar problemas específicos
 * 
 * COMO FUNCIONA:
 * Esta classe usa o padrão "Fluent Interface" - você vai "encadeando" métodos
 * para adicionar informações. Por exemplo:
 * 
 * ContextoSincronizacao.criar()
 *    .com("empresa", "A001")
 *    .com("lote", 5)
 *    .comThreadInfo()
 *    .comSucesso();
 * 
 * VANTAGENS:
 * - Código mais limpo e legível
 * - Padroniza como coletamos informações
 * - Evita repetição de código
 * - Facilita manutenção
 */
@Getter
public class ContextoSincronizacao {
    
    // Mapa que armazena todas as informações do contexto
    // Chave = nome da informação, Valor = valor da informação
    private final Map<String, Object> dados;
    
    // Momento exato em que este contexto foi criado
    private final LocalDateTime timestamp;
    
    /**
     * CONSTRUTOR PRIVADO - não pode ser chamado diretamente
     * 
     * Inicializa o contexto com timestamp automático.
     * Use o método criar() para instanciar.
     */
    private ContextoSincronizacao() {
        // Registra o momento exato da criação
        this.timestamp = LocalDateTime.now();
        
        // Inicializa o mapa de dados
        this.dados = new HashMap<>();
        
        // Adiciona o timestamp como primeira informação
        this.dados.put("timestamp", timestamp);
    }
    
    /**
     * MÉTODO FÁBRICA - cria um novo contexto vazio
     * 
     * Este é o ponto de entrada para criar contextos.
     * Sempre use este método em vez de chamar o construtor diretamente.
     * 
     * @return Novo contexto pronto para receber informações
     */
    public static ContextoSincronizacao criar() {
        return new ContextoSincronizacao();
    }
    
    /**
     * CRIA CONTEXTO JÁ COM NOME DA OPERAÇÃO
     * 
     * Método de conveniência que cria um contexto e já adiciona
     * qual operação está sendo realizada.
     * 
     * @param operacao - Nome da operação (ex: "PROCESSAMENTO_LOTE")
     * @return Contexto com operação já definida
     */
    public static ContextoSincronizacao criar(String operacao) {
        return criar().com("operacao", operacao);
    }
    
    /**
     * ADICIONA UMA INFORMAÇÃO AO CONTEXTO
     * 
     * Método principal para adicionar qualquer informação.
     * Retorna o próprio contexto para permitir encadeamento.
     * 
     * Exemplo: contexto.com("empresa", "A001").com("lote", 5)
     * 
     * @param chave - Nome da informação (ex: "empresa", "lote", "erro")
     * @param valor - Valor da informação (ex: "A001", 5, "timeout")
     * @return O mesmo contexto para permitir encadeamento de métodos
     */
    public ContextoSincronizacao com(String chave, Object valor) {
        // Adiciona a informação no mapa interno
        this.dados.put(chave, valor);
        
        // Retorna o próprio objeto para permitir encadeamento
        return this;
    }
    
    /**
     * ADICIONA MÚLTIPLAS INFORMAÇÕES DE UMA VEZ
     * 
     * Útil quando você já tem um mapa com várias informações
     * e quer adicionar todas de uma vez.
     * 
     * @param valores - Mapa com múltiplas informações para adicionar
     * @return O mesmo contexto para permitir encadeamento de métodos
     */
    public ContextoSincronizacao com(Map<String, Object> valores) {
        // Adiciona todas as informações do mapa no contexto
        this.dados.putAll(valores);
        return this;
    }
    
    /**
     * ADICIONA INFORMAÇÕES DE ERRO AUTOMATICAMENTE
     * 
     * Quando acontece um erro, este método extrai automaticamente
     * as informações mais importantes da exceção e adiciona no contexto.
     * 
     * INFORMAÇÕES EXTRAÍDAS:
     * - Tipo da exceção (ex: "RuntimeException", "SQLException")  
     * - Mensagem da exceção (ex: "Conexão recusada")
     * - Marca o resultado como "erro"
     * 
     * @param e - A exceção que aconteceu
     * @return O mesmo contexto com informações do erro adicionadas
     */
    public ContextoSincronizacao comErro(Exception e) {
        return this
            .com("tipoErro", e.getClass().getSimpleName())  // Nome da classe da exceção
            .com("mensagemErro", e.getMessage())            // Mensagem da exceção
            .com("resultado", "erro");                      // Marca como erro
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