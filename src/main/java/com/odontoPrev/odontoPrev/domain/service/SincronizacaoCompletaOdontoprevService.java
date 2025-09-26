package com.odontoPrev.odontoPrev.domain.service;

/**
 * Interface para sincronização completa com OdontoPrev.
 * 
 * Responsável por coordenar a sincronização de empresas em três cenários:
 * 1. Adição - empresas novas
 * 2. Alteração - empresas modificadas
 * 3. Exclusão - empresas inativadas/excluídas
 */
public interface SincronizacaoCompletaOdontoprevService {
    
    /**
     * Executa sincronização completa incluindo adições, alterações e exclusões.
     */
    void executarSincronizacaoCompleta();
    
    /**
     * Executa sincronização apenas de empresas alteradas.
     */
    void executarSincronizacaoAlteracoes();
    
    /**
     * Executa sincronização apenas de empresas excluídas.
     */
    void executarSincronizacaoExclusoes();
}
