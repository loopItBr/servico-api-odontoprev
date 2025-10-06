package com.odontoPrev.odontoPrev.domain.service;

/**
 * INTERFACE PARA SINCRONIZAÇÃO COMPLETA DE BENEFICIÁRIOS
 *
 * Define o contrato para execução de sincronização completa de beneficiários
 * com a OdontoPrev, incluindo inclusões, alterações e inativações.
 *
 * Esta é a interface principal chamada pelo scheduler automático,
 * orquestrando todos os tipos de operações de beneficiários.
 *
 * RESPONSABILIDADES:
 * - Coordenar sincronização de inclusões, alterações e exclusões
 * - Gerenciar ordem de execução das operações
 * - Controlar transações e rollback se necessário
 * - Registrar estatísticas e métricas da sincronização
 * - Tratar erros globais do processo
 *
 * QUANDO USAR:
 * - Execução automática via scheduler
 * - Sincronização manual completa via endpoint
 * - Reprocessamento completo após falhas
 */
public interface SincronizacaoCompletaBeneficiarioService {

    /**
     * EXECUTA SINCRONIZAÇÃO COMPLETA DE TODOS OS BENEFICIÁRIOS
     *
     * Realiza sincronização completa em três etapas:
     * 1. INCLUSÕES: Processa beneficiários novos (status PENDENTE)
     * 2. ALTERAÇÕES: Processa beneficiários alterados (status ALTERADO)
     * 3. INATIVAÇÕES: Processa beneficiários a inativar (status EXCLUIDO)
     *
     * ORDEM DE EXECUÇÃO:
     * A ordem é importante para manter consistência:
     * - Primeiro inclusões (para criar novos registros)
     * - Depois alterações (para atualizar existentes)
     * - Por último inativações (para desativar)
     *
     * CONTROLE DE TRANSAÇÕES:
     * - Cada etapa é executada em transação separada
     * - Falha em uma etapa não impede execução das outras
     * - Rollback automático dentro de cada etapa
     *
     * MÉTRICAS REGISTRADAS:
     * - Quantidade de registros processados por tipo
     * - Quantidade de sucessos e erros
     * - Tempo total de execução
     * - Tempo por etapa
     *
     * TRATAMENTO DE ERROS:
     * - Erros individuais não param o processo
     * - Registros com erro são marcados para retry
     * - Logs detalhados para diagnóstico
     * - Notificação se muitos erros (alerta)
     *
     * LOGS GERADOS:
     * - Início da sincronização completa
     * - Estatísticas de cada etapa
     * - Erros e exceções
     * - Resumo final com totais
     *
     */
    void executarSincronizacaoCompleta();

    /**
     * EXECUTA APENAS SINCRONIZAÇÃO DE INCLUSÕES
     *
     * Processa somente beneficiários pendentes de inclusão.
     * Útil para execução isolada ou reprocessamento específico.
     *
     * @return quantidade de beneficiários processados
     */
    int executarSincronizacaoInclusoes();

    /**
     * EXECUTA APENAS SINCRONIZAÇÃO DE ALTERAÇÕES
     *
     * Processa somente beneficiários pendentes de alteração.
     * Útil para execução isolada ou reprocessamento específico.
     *
     * @return quantidade de beneficiários processados
     */
    int executarSincronizacaoAlteracoes();

    /**
     * EXECUTA APENAS SINCRONIZAÇÃO DE INATIVAÇÕES
     *
     * Processa somente beneficiários pendentes de inativação.
     * Útil para execução isolada ou reprocessamento específico.
     *
     * @return quantidade de beneficiários processados
     */
    int executarSincronizacaoInativacoes();

    /**
     * OBTÉM ESTATÍSTICAS DA ÚLTIMA SINCRONIZAÇÃO
     *
     * Retorna informações sobre a última execução:
     * - Data/hora da execução
     * - Quantidades processadas por tipo
     * - Quantidades de sucesso/erro
     * - Tempo de execução
     *
     * @return objeto com estatísticas da sincronização
     */
    EstatisticasSincronizacao obterEstatisticasUltimaSincronizacao();

    /**
     * CLASSE PARA RETORNO DE ESTATÍSTICAS
     */
    record EstatisticasSincronizacao(
        java.time.LocalDateTime dataExecucao,
        int totalProcessados,
        int totalSucessos,
        int totalErros,
        int inclusoes,
        int alteracoes,
        int inativacoes,
        long tempoExecucaoMs
    ) {}
}
