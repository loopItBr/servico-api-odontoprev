package com.odontoPrev.odontoPrev.domain.service;

import java.util.List;

/**
 * Interface para processamento de empresas em lotes paginados.
 */
public interface ProcessamentoLoteService {
    
    /**
     * Processa empresas em lotes de forma paginada.
     * 
     * @param tamanhoBatch tamanho do lote para processamento
     * @param maxThreads número máximo de threads para processamento paralelo
     * @param totalEmpresas total de empresas já calculado (para evitar nova consulta)
     */
    void processarEmpresasEmLotes(int tamanhoBatch, int maxThreads, long totalEmpresas);
    
    /**
     * Busca códigos de empresas de forma paginada.
     * 
     * @param offset deslocamento (página)
     * @param limit limite de registros por página
     * @return lista de códigos de empresas
     */
    List<String> buscarCodigosEmpresasPaginado(int offset, int limit);
    
    /**
     * Conta o total de empresas disponíveis.
     * 
     * @return número total de empresas
     */
    long contarTotalEmpresas();
}