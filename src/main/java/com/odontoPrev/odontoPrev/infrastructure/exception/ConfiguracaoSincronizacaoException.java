package com.odontoPrev.odontoPrev.infrastructure.exception;

import java.util.Map;

/**
 * Exceção lançada quando há problemas com configurações da sincronização.
 * Monitora problemas de parâmetros inválidos, configurações faltantes ou incorretas.
 */
public class ConfiguracaoSincronizacaoException extends SincronizacaoException {

    public static final String CODIGO_ERRO = "SYNC_CONFIG_ERROR";

    public ConfiguracaoSincronizacaoException(String parametro, String detalhes, Map<String, Object> contexto) {
        super(CODIGO_ERRO, 
              String.format("Configuração inválida para '%s': %s", parametro, detalhes), 
              contexto);
    }

    public ConfiguracaoSincronizacaoException(String parametro, String detalhes, Map<String, Object> contexto, Throwable causa) {
        super(CODIGO_ERRO, 
              String.format("Configuração inválida para '%s': %s", parametro, detalhes), 
              contexto, causa);
    }
}