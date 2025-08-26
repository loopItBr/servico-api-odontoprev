package com.odontoPrev.odontoPrev.infrastructure.exception;

import java.util.Map;

/**
 * Exceção lançada quando há problemas no controle de execução concorrente do scheduler.
 * Monitora conflitos de execução e problemas de sincronização.
 */
public class ControleExecucaoException extends SincronizacaoException {

    public static final String CODIGO_ERRO = "EXECUTION_CONTROL_ERROR";

    public ControleExecucaoException(String detalhes, Map<String, Object> contexto) {
        super(CODIGO_ERRO, 
              "Falha no controle de execução do scheduler: " + detalhes, 
              contexto);
    }

    public ControleExecucaoException(String detalhes, Map<String, Object> contexto, Throwable causa) {
        super(CODIGO_ERRO, 
              "Falha no controle de execução do scheduler: " + detalhes, 
              contexto, causa);
    }
}