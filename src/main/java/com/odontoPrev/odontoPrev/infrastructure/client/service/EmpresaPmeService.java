package com.odontoPrev.odontoPrev.infrastructure.client.service;

import com.odontoPrev.odontoPrev.infrastructure.client.adapter.out.dto.EmpresaPmeRequest;
import com.odontoPrev.odontoPrev.infrastructure.repository.entity.IntegracaoOdontoprev;

/**
 * SERVIÇO PARA CADASTRO PME DE EMPRESAS
 * 
 * Este serviço é responsável por converter dados da view VW_INTEGRACAO_ODONTOPREV
 * para o formato necessário do endpoint PME da OdontoPrev.
 * 
 * FUNCIONALIDADES:
 * - Conversão de dados da view para request PME
 * - Suporte a múltiplos planos (CODIGO_PLANO_1, 2, 3)
 * - Mapeamento de campos obrigatórios
 * - Tratamento de dados nulos e valores padrão
 */
public interface EmpresaPmeService {

    /**
     * CONVERTE DADOS DA VIEW PARA REQUEST PME
     * 
     * Converte os dados da empresa obtidos da view VW_INTEGRACAO_ODONTOPREV
     * para o formato necessário do endpoint PME da OdontoPrev.
     * 
     * CAMPOS MAPEADOS:
     * - Dados básicos da empresa (CNPJ, razão social, nome fantasia)
     * - Planos (suporte a múltiplos planos da view)
     * - Contatos e contatos da fatura
     * - Endereço e dados de cobrança
     * - Grupos e graus de parentesco
     * 
     * @param dadosEmpresa Dados da empresa da view
     * @return Request formatado para endpoint PME
     */
    EmpresaPmeRequest converterParaRequestPme(IntegracaoOdontoprev dadosEmpresa);
}
