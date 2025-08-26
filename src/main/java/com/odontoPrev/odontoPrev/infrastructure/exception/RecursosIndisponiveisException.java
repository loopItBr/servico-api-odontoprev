package com.odontoPrev.odontoPrev.infrastructure.exception;

import java.util.Map;

/**
 * Exceção lançada quando recursos necessários para a sincronização estão indisponíveis.
 * Monitora problemas de pool de threads, conexões de banco, e outros recursos.
 */
public class RecursosIndisponiveisException extends SincronizacaoException {

    public static final String CODIGO_ERRO = "RESOURCES_UNAVAILABLE_ERROR";

    public RecursosIndisponiveisException(String recurso, String detalhes, Map<String, Object> contexto) {
        super(CODIGO_ERRO, 
              String.format("Recurso indisponível '%s': %s", recurso, detalhes), 
              contexto);
    }

    public RecursosIndisponiveisException(String recurso, String detalhes, Map<String, Object> contexto, Throwable causa) {
        super(CODIGO_ERRO, 
              String.format("Recurso indisponível '%s': %s", recurso, detalhes), 
              contexto, causa);
    }
}