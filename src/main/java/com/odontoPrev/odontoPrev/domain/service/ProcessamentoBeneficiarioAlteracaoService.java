package com.odontoPrev.odontoPrev.domain.service;

import com.odontoPrev.odontoPrev.domain.entity.BeneficiarioOdontoprev;

/**
 * INTERFACE PARA PROCESSAMENTO DE ALTERAÇÕES DE BENEFICIÁRIOS
 *
 * Define o contrato para alteração de dados de beneficiários existentes
 * no sistema da OdontoPrev. Esta interface segue os princípios da
 * arquitetura hexagonal, separando a lógica de domínio da implementação
 * de infraestrutura.
 *
 * RESPONSABILIDADES:
 * - Validar dados alterados do beneficiário
 * - Comunicar com API da OdontoPrev para alteração
 * - Atualizar status de sincronização
 * - Registrar logs de operação
 *
 * QUANDO USAR:
 * - Ao sincronizar alterações de beneficiários no Tasy
 * - Durante execução do scheduler de sincronização
 * - Em processos manuais de reprocessamento
 */
public interface ProcessamentoBeneficiarioAlteracaoService {

    /**
     * PROCESSA ALTERAÇÃO DE UM BENEFICIÁRIO EXISTENTE
     *
     * Realiza todo o fluxo de alteração de um beneficiário:
     * 1. Valida se beneficiário já possui cdAssociado
     * 2. Valida dados obrigatórios para alteração
     * 3. Converte entidade para DTO de request
     * 4. Chama API da OdontoPrev
     * 5. Atualiza status no banco
     * 6. Registra logs da operação
     *
     * TRATAMENTO DE ERROS:
     * - Beneficiário sem cdAssociado: marca como ERRO
     * - Campos obrigatórios ausentes: marca como ERRO
     * - Falha na comunicação: marca como ERRO + retry
     * - Beneficiário não encontrado na OdontoPrev: marca como ERRO
     *
     * LOGS GERADOS:
     * - Início do processamento
     * - Dados enviados para OdontoPrev
     * - Resposta recebida
     * - Resultado final (sucesso/erro)
     *
     * @param beneficiario entidade do beneficiário a ser alterado
     * @throws ProcessamentoBeneficiarioException se houver erro no processamento
     */
    void processarAlteracaoBeneficiario(BeneficiarioOdontoprev beneficiario);

    /**
     * VALIDA SE BENEFICIÁRIO PODE SER ALTERADO
     *
     * Verifica se o beneficiário atende aos critérios para alteração:
     * - Deve possuir cdAssociado (já cadastrado na OdontoPrev)
     * - Deve ter status "ALTERADO"
     * - Deve ter dados obrigatórios preenchidos
     *
     * @param beneficiario beneficiário a ser validado
     * @return true se pode ser alterado
     */
    boolean validarBeneficiarioParaAlteracao(BeneficiarioOdontoprev beneficiario);
}
