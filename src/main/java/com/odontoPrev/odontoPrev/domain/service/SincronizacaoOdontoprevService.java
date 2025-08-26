package com.odontoPrev.odontoPrev.domain.service;

/**
 * Interface para o serviço de sincronização com OdontoPrev.
 * Define o contrato para execução da sincronização.
 */
public interface SincronizacaoOdontoprevService {
    
    /**
     * Executa a sincronização completa com OdontoPrev.
     */
    void executarSincronizacao();
}