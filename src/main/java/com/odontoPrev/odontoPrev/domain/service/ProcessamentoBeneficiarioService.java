package com.odontoPrev.odontoPrev.domain.service;

import com.odontoPrev.odontoPrev.domain.entity.BeneficiarioOdontoprev;

/**
 * INTERFACE PARA PROCESSAMENTO DE BENEFICIÁRIOS
 *
 * Define o contrato para inclusão de novos beneficiários no sistema da OdontoPrev.
 * Esta interface segue os princípios da arquitetura hexagonal, separando
 * a lógica de domínio da implementação de infraestrutura.
 *
 * RESPONSABILIDADES:
 * - Validar dados do beneficiário antes do envio
 * - Comunicar com API da OdontoPrev para inclusão
 * - Salvar código do associado retornado
 * - Registrar logs de operação
 * - Executar procedure para atualizar sistema Tasy
 *
 * QUANDO USAR:
 * - Ao sincronizar novos beneficiários cadastrados no Tasy
 * - Durante execução do scheduler de sincronização
 * - Em processos manuais de reprocessamento
 */
public interface ProcessamentoBeneficiarioService {

    /**
     * PROCESSA INCLUSÃO DE UM ÚNICO BENEFICIÁRIO
     *
     * Realiza todo o fluxo de inclusão de um beneficiário:
     * 1. Valida dados obrigatórios
     * 2. Converte entidade para DTO de request
     * 3. Chama API da OdontoPrev
     * 4. Processa resposta (cdAssociado)
     * 5. Atualiza status no banco
     * 6. Executa procedure no Tasy
     * 7. Registra logs da operação
     *
     * TRATAMENTO DE ERROS:
     * - Campos obrigatórios vazios: marca como ERRO
     * - Falha na comunicação: marca como ERRO + retry
     * - cdAssociado não retornado: marca como ERRO
     * - Procedure falha: marca como ERRO
     *
     * LOGS GERADOS:
     * - Início do processamento
     * - Dados enviados para OdontoPrev
     * - Resposta recebida
     * - Resultado final (sucesso/erro)
     *
     * @param beneficiario entidade do beneficiário a ser processado
     * @throws ProcessamentoBeneficiarioException se houver erro no processamento
     */
    void processarInclusaoBeneficiario(BeneficiarioOdontoprev beneficiario);

    /**
     * VALIDA SE BENEFICIÁRIO TEM DADOS OBRIGATÓRIOS COMPLETOS
     *
     * Verifica se todos os campos obrigatórios para inclusão estão preenchidos
     * conforme especificação da API da OdontoPrev.
     *
     * CAMPOS OBRIGATÓRIOS VALIDADOS:
     * - codigoMatricula, CPF, dataNascimento, dtVigenciaRetroativa
     * - CEP, cidade, logradouro, numero, uf
     * - nomeBeneficiario, nomeMae, sexo
     * - telefoneCelular, telefoneResidencial
     * - codigoEmpresa, codigoPlano, departamento
     *
     * @param beneficiario beneficiário a ser validado
     * @return true se todos os campos obrigatórios estão preenchidos
     */
    boolean validarDadosObrigatorios(BeneficiarioOdontoprev beneficiario);
}
