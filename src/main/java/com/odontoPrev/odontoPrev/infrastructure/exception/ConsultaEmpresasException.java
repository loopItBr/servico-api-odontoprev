package com.odontoPrev.odontoPrev.infrastructure.exception;

import java.util.Map;

/**
 * Exceção lançada quando há falhas na consulta de empresas no banco de dados.
 * Monitora problemas de conectividade e performance da base de dados.
 */
public class ConsultaEmpresasException extends SincronizacaoException {

    public static final String CODIGO_ERRO = "COMPANIES_QUERY_ERROR";

    public ConsultaEmpresasException(String detalhes, Map<String, Object> contexto) {
        super(CODIGO_ERRO, 
              "Falha na consulta de empresas: " + detalhes, 
              contexto);
    }

    public ConsultaEmpresasException(String detalhes, Map<String, Object> contexto, Throwable causa) {
        super(CODIGO_ERRO, 
              "Falha na consulta de empresas: " + detalhes, 
              contexto, causa);
    }
}