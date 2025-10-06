package com.odontoPrev.odontoPrev.domain.service;

import com.odontoPrev.odontoPrev.domain.entity.BeneficiarioOdontoprev;

/**
 * INTERFACE PARA SERVIÇOS DE CONSULTA DE BENEFICIÁRIOS COM LÓGICA DE NEGÓCIO
 *
 * Define o contrato para operações que envolvem lógica específica
 * de negócio sobre os dados de beneficiários das views.
 *
 * NOTA IMPORTANTE:
 * Para consultas simples nas views (buscar todos, buscar por filtros básicos),
 * use diretamente os repositórios:
 * - IntegracaoOdontoprevBeneficiarioRepository (inclusões)
 * - IntegracaoOdontoprevBeneficiarioAlteracaoRepository (alterações)
 * - IntegracaoOdontoprevBeneficiarioExclusaoRepository (exclusões)
 * + BeneficiarioViewMapper para conversão
 *
 * RESPONSABILIDADES DESTA INTERFACE:
 * - Operações que combinam múltiplas consultas
 * - Lógica de negócio específica
 * - Agregações e cálculos complexos
 * - Tratamento de casos especiais
 */
public interface ConsultaBeneficiarioOdontoprevService {

    /**
     * BUSCA BENEFICIÁRIO ESPECÍFICO POR MATRÍCULA
     *
     * Localiza um beneficiário específico através do código da matrícula,
     * consultando as views conforme necessário.
     *
     * @param codigoMatricula código da matrícula do beneficiário
     * @return beneficiário encontrado ou null se não existir
     * @throws ConsultaBeneficiarioException se houver erro na consulta
     */
    BeneficiarioOdontoprev buscarBeneficiarioPorMatricula(String codigoMatricula);

    /**
     * CONTA TOTAL DE BENEFICIÁRIOS PENDENTES EM TODAS AS OPERAÇÕES
     *
     * Operação que agrega contadores de múltiplas views para fornecer
     * visão completa dos beneficiários pendentes de sincronização.
     *
     * LÓGICA DE NEGÓCIO:
     * - Consulta todas as views (inclusão + alteração + exclusão)
     * - Soma os totais para fornecer visão consolidada
     * - Calcula métricas importantes para dashboards
     *
     * CONTADORES INDIVIDUAIS:
     * - Pendentes de inclusão
     * - Pendentes de alteração
     * - Pendentes de exclusão
     * - Total geral
     *
     * @return objeto com contadores por tipo de operação
     */
    ContadoresPendentes contarBeneficiariosPendentes();

    /**
     * CLASSE PARA RETORNO DE CONTADORES
     */
    record ContadoresPendentes(
        long pendentesInclusao,
        long pendentesAlteracao,
        long pendentesExclusao,
        long totalPendentes
    ) {}
}
