package com.odontoPrev.odontoPrev.infrastructure.aop;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Anotação para monitoramento automático de operações.
 * Cria automaticamente contexto de sincronização com logs e métricas.
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface MonitorarOperacao {
    
    /**
     * Nome da operação para identificação em logs e métricas
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