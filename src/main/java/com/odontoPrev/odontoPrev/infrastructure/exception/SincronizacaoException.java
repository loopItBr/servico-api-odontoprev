package com.odontoPrev.odontoPrev.infrastructure.exception;

import lombok.Getter;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Exceção base para erros de sincronização com OdontoPrev.
 * Fornece contexto específico para monitoramento e observabilidade.
 */
@Getter
public abstract class SincronizacaoException extends RuntimeException {

    private final String codigoErro;
    private final LocalDateTime dataHoraOcorrencia;
    private final Map<String, Object> contextoAdicional;

    protected SincronizacaoException(String codigoErro, String mensagem, Map<String, Object> contexto) {
        super(mensagem);
        this.codigoErro = codigoErro;
        this.dataHoraOcorrencia = LocalDateTime.now();
        this.contextoAdicional = contexto;
    }

    protected SincronizacaoException(String codigoErro, String mensagem, Map<String, Object> contexto, Throwable causa) {
        super(mensagem, causa);
        this.codigoErro = codigoErro;
        this.dataHoraOcorrencia = LocalDateTime.now();
        this.contextoAdicional = contexto;
    }
}