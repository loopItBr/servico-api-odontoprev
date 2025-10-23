package com.odontoPrev.odontoPrev.infrastructure.client.service;

import com.odontoPrev.odontoPrev.infrastructure.client.adapter.out.dto.EmpresaAtivacaoPlanoResponse;
import com.odontoPrev.odontoPrev.infrastructure.repository.entity.IntegracaoOdontoprev;

/**
 * SERVIÇO PARA ATIVAÇÃO DO PLANO DA EMPRESA
 *
 * Este serviço é responsável por ativar o plano odontológico de uma empresa
 * após o cadastro bem-sucedido da sincronização.
 *
 * FLUXO DE FUNCIONAMENTO:
 * 1. Recebe dados da empresa sincronizada
 * 2. Converte dados para formato da API de ativação
 * 3. Chama API da OdontoPrev para ativar o plano
 * 4. Registra resultado no controle de sincronização
 *
 * QUANDO É CHAMADO:
 * - Automaticamente após sincronização bem-sucedida da empresa
 * - Apenas para empresas que foram cadastradas com sucesso
 */
public interface AtivacaoPlanoEmpresaService {

    /**
     * ATIVA O PLANO DE UMA EMPRESA
     *
     * Este método é chamado automaticamente após o cadastro bem-sucedido
     * da sincronização da empresa na OdontoPrev.
     *
     * @param dadosEmpresa dados completos da empresa sincronizada
     */
    EmpresaAtivacaoPlanoResponse ativarPlanoEmpresa(IntegracaoOdontoprev dadosEmpresa);
}
