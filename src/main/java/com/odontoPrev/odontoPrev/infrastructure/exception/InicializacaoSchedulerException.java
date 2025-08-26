package com.odontoPrev.odontoPrev.infrastructure.exception;

import java.util.Map;

/**
 * Exceção lançada quando há falhas na inicialização do scheduler de sincronização.
 * Usado para monitorar problemas de configuração e recursos do scheduler.
 */
public class InicializacaoSchedulerException extends SincronizacaoException {

    public static final String CODIGO_ERRO = "SCHEDULER_INIT_ERROR";

    public InicializacaoSchedulerException(String detalhes, Map<String, Object> contexto) {
        super(CODIGO_ERRO, 
              "Falha na inicialização do scheduler de sincronização: " + detalhes, 
              contexto);
    }

    public InicializacaoSchedulerException(String detalhes, Map<String, Object> contexto, Throwable causa) {
        super(CODIGO_ERRO, 
              "Falha na inicialização do scheduler de sincronização: " + detalhes, 
              contexto, causa);
    }
}