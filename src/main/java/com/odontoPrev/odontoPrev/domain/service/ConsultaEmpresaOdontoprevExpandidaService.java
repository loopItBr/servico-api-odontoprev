package com.odontoPrev.odontoPrev.domain.service;

import com.odontoPrev.odontoPrev.infrastructure.repository.entity.IntegracaoOdontoprev;

/**
 * Serviço expandido para consulta de empresas na API OdontoPrev.
 * Inclui operações de adição, alteração e exclusão.
 */
public interface ConsultaEmpresaOdontoprevExpandidaService {
    
    /**
     * Adiciona uma nova empresa na API OdontoPrev.
     * 
     * @param dadosEmpresa Dados da empresa para adição
     * @return Resposta da API
     */
    String adicionarEmpresa(IntegracaoOdontoprev dadosEmpresa);
    
    /**
     * Altera uma empresa existente na API OdontoPrev.
     * 
     * @param dadosEmpresa Dados da empresa para alteração
     * @return Resposta da API
     */
    String alterarEmpresa(IntegracaoOdontoprev dadosEmpresa);
    
    /**
     * Inativa uma empresa na API OdontoPrev.
     * 
     * @param dadosEmpresa Dados da empresa para inativação
     * @return Resposta da API
     */
    String inativarEmpresa(IntegracaoOdontoprev dadosEmpresa);
}