package com.odontoPrev.odontoPrev.infrastructure.exception;

import java.util.Map;

/**
 * Exceção lançada quando há falhas no processamento de lotes de empresas.
 * Monitora problemas específicos de paginação e processamento em batch.
 */
public class ProcessamentoLoteException extends SincronizacaoException {

    public static final String CODIGO_ERRO = "BATCH_PROCESSING_ERROR";

    public ProcessamentoLoteException(String detalhes, Map<String, Object> contexto) {
        super(CODIGO_ERRO, 
              "Falha no processamento de lote: " + detalhes, 
              contexto);
    }

    public ProcessamentoLoteException(String detalhes, Map<String, Object> contexto, Throwable causa) {
        super(CODIGO_ERRO, 
              "Falha no processamento de lote: " + detalhes, 
              contexto, causa);
    }
}