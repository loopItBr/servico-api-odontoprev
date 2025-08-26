package com.odontoPrev.odontoPrev.infrastructure.exception;

import java.util.Map;

/**
 * Exceção lançada quando há falhas no processamento individual de uma empresa.
 * Permite identificar exatamente qual empresa causou o problema.
 */
public class ProcessamentoEmpresaException extends SincronizacaoException {

    public static final String CODIGO_ERRO = "COMPANY_PROCESSING_ERROR";

    public ProcessamentoEmpresaException(String codigoEmpresa, String detalhes, Map<String, Object> contexto) {
        super(CODIGO_ERRO, 
              String.format("Falha no processamento da empresa %s: %s", codigoEmpresa, detalhes), 
              contexto);
    }

    public ProcessamentoEmpresaException(String codigoEmpresa, String detalhes, Map<String, Object> contexto, Throwable causa) {
        super(CODIGO_ERRO, 
              String.format("Falha no processamento da empresa %s: %s", codigoEmpresa, detalhes), 
              contexto, causa);
    }
}