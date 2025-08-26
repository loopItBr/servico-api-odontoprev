package com.odontoPrev.odontoPrev.domain.service;

import com.odontoPrev.odontoPrev.infrastructure.client.adapter.out.dto.EmpresaResponse;

/**
 * Interface responsável por consultas de empresas na OdontoPrev.
 */
public interface ConsultaEmpresaOdontoprevService {
    
    /**
     * Busca uma empresa na OdontoPrev.
     * 
     * @param codigoEmpresa código da empresa
     * @return resposta da empresa
     * @throws RuntimeException em caso de erro na consulta
     */
    EmpresaResponse buscarEmpresa(String codigoEmpresa);
}