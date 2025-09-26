package com.odontoPrev.odontoPrev.domain.service;

/**
 * Interface para processamento de empresas excluídas.
 * 
 * Responsável por processar empresas que foram inativadas/excluídas
 * e precisam ser removidas da OdontoPrev.
 */
public interface ProcessamentoEmpresaExclusaoService {
    
    /**
     * Processa uma empresa excluída individualmente.
     * 
     * @param codigoEmpresa código da empresa a ser processada
     */
    void processar(String codigoEmpresa);
}
