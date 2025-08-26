package com.odontoPrev.odontoPrev.infrastructure.exception;

import java.util.Map;

/**
 * Exceção lançada quando há falhas na autenticação com a API da OdontoPrev.
 * Monitora problemas específicos de token, credenciais e autorização.
 */
public class AutenticacaoOdontoprevException extends SincronizacaoException {

    public static final String CODIGO_ERRO = "ODONTOPREV_AUTH_ERROR";

    public AutenticacaoOdontoprevException(String detalhes, Map<String, Object> contexto) {
        super(CODIGO_ERRO, 
              "Falha na autenticação com OdontoPrev: " + detalhes, 
              contexto);
    }

    public AutenticacaoOdontoprevException(String detalhes, Map<String, Object> contexto, Throwable causa) {
        super(CODIGO_ERRO, 
              "Falha na autenticação com OdontoPrev: " + detalhes, 
              contexto, causa);
    }
}