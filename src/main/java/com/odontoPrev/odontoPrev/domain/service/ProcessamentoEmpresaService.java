package com.odontoPrev.odontoPrev.domain.service;

/**
 * Interface responsável pelo processamento de empresas na sincronização.
 */
public interface ProcessamentoEmpresaService {
    
    /**
     * Processa uma única empresa por código.
     * 
     * @param codigoEmpresa código da empresa a ser processada
     */
    void processar(String codigoEmpresa);
}