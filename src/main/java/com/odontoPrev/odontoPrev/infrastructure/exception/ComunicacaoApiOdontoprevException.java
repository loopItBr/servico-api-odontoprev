package com.odontoPrev.odontoPrev.infrastructure.exception;

import java.util.Map;

/**
 * Exceção lançada quando há falhas na comunicação com a API da OdontoPrev.
 * Monitora problemas de conectividade, timeout e respostas inválidas da API externa.
 */
public class ComunicacaoApiOdontoprevException extends SincronizacaoException {

    public static final String CODIGO_ERRO = "ODONTOPREV_API_ERROR";

    public ComunicacaoApiOdontoprevException(String endpoint, int statusCode, String detalhes, Map<String, Object> contexto) {
        super(CODIGO_ERRO, 
              String.format("Falha na comunicação com API OdontoPrev [%s] - Status: %d - %s", endpoint, statusCode, detalhes), 
              contexto);
    }

    public ComunicacaoApiOdontoprevException(String endpoint, String detalhes, Map<String, Object> contexto, Throwable causa) {
        super(CODIGO_ERRO, 
              String.format("Falha na comunicação com API OdontoPrev [%s]: %s", endpoint, detalhes), 
              contexto, causa);
    }
}