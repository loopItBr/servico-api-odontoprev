package com.odontoPrev.odontoPrev.domain.service;

/**
 * Interface para processamento de empresas alteradas.
 * 
 * Responsável por processar empresas que tiveram dados modificados
 * e precisam ser atualizadas na OdontoPrev.
 */
public interface ProcessamentoEmpresaAlteracaoService {
    
    /**
     * Processa uma empresa alterada individualmente.
     * 
     * @param codigoEmpresa código da empresa a ser processada
     */
    void processar(String codigoEmpresa);
}
