package com.odontoPrev.odontoPrev.infrastructure.aop;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * ANOTAÇÃO MÁGICA PARA MONITORAMENTO AUTOMÁTICO DE MÉTODOS
 * 
 * O QUE ESTA ANOTAÇÃO FAZ:
 * Esta é uma "anotação" - um tipo especial de marcador que você coloca
 * em métodos para adicionar funcionalidades automáticas. Quando você coloca
 * @MonitorarOperacao em um método, o sistema automaticamente:
 * 
 * 1. CRIA CONTEXTO: Coleta informações sobre a execução (timestamp, thread, etc.)
 * 2. GERA LOGS: Registra início e fim da operação automaticamente  
 * 3. MEDE TEMPO: Calcula quanto tempo a operação demorou
 * 4. CAPTURA ERROS: Se der erro, captura e trata automaticamente
 * 5. CONVERTE EXCEÇÕES: Transforma erros genéricos em específicos
 * 6. ADICIONA RASTREABILIDADE: Facilita investigação de problemas
 * 
 * EXEMPLO PRÁTICO:
 * 
 * // ANTES (sem a anotação) - muito código repetitivo:
 * public void processarEmpresa(String codigo) {
 *     LocalDateTime inicio = LocalDateTime.now();
 *     Map<String, Object> contexto = new HashMap<>(); 
 *     contexto.put("codigo", codigo);
 *     contexto.put("timestamp", inicio);
 *     
 *     try {
 *         log.info("Iniciando processamento - {}", contexto);
 *         // lógica aqui...
 *         long duracao = Duration.between(inicio, LocalDateTime.now()).toMillis();
 *         contexto.put("duracao", duracao);
 *         log.info("Sucesso - {}", contexto);
 *     } catch (Exception e) {
 *         contexto.put("erro", e.getMessage());
 *         log.error("Erro - {}", contexto, e);
 *         throw new ProcessamentoEmpresaException(...);
 *     }
 * }
 * 
 * // DEPOIS (com a anotação) - código limpo:
 * @MonitorarOperacao(operacao = "PROCESSAR_EMPRESA")  
 * public void processarEmpresa(String codigo) {
 *     // apenas a lógica de negócio!
 *     // todo o resto é feito automaticamente
 * }
 * 
 * TECNOLOGIA:
 * Usa AOP (Aspect-Oriented Programming) - uma técnica que permite "interceptar" 
 * métodos e executar código adicional antes/depois/durante sua execução,
 * sem modificar o método original.
 */
@Target({ElementType.METHOD, ElementType.TYPE})  // Pode ser usada em métodos ou classes inteiras
@Retention(RetentionPolicy.RUNTIME)              // Fica disponível durante execução
public @interface MonitorarOperacao {
    
    /**
     * NOME DA OPERAÇÃO PARA IDENTIFICAÇÃO
     * 
     * Este nome aparece nos logs e sistemas de monitoramento
     * para identificar qual operação está sendo executada.
     * 
     * Use nomes descritivos e consistentes.
     * 
     * Exemplos: "PROCESSAR_EMPRESA", "CONSULTAR_API", "SALVAR_DADOS"
     */
    String operacao();
    
    /**
     * Incluir informações de thread no contexto
     */
    boolean incluirThread() default false;
    
    /**
     * Incluir duração da execução automaticamente
     */
    boolean medirDuracao() default true;
    
    /**
     * Nível de log para mensagens de sucesso
     */
    NivelLog logSucesso() default NivelLog.INFO;
    
    /**
     * Nível de log para mensagens de erro
     */
    NivelLog logErro() default NivelLog.ERROR;
    
    /**
     * Propagar exceções ou apenas logar
     */
    boolean propagarExcecoes() default true;
    
    /**
     * Parâmetros adicionais para incluir no contexto (nome dos parâmetros do método)
     */
    String[] incluirParametros() default {};
    
    /**
     * Tipo de exceção customizada a ser lançada em caso de erro
     */
    TipoExcecao excecaoEmErro() default TipoExcecao.PROCESSAMENTO_LOTE;
    
    enum NivelLog {
        TRACE, DEBUG, INFO, WARN, ERROR
    }
    
    enum TipoExcecao {
        INICIALIZACAO_SCHEDULER,
        CONTROLE_EXECUCAO,
        CONSULTA_EMPRESAS,
        PROCESSAMENTO_LOTE,
        PROCESSAMENTO_EMPRESA,
        CONFIGURACAO,
        RECURSOS_INDISPONIVEIS,
        AUTENTICACAO,
        COMUNICACAO_API
    }
}