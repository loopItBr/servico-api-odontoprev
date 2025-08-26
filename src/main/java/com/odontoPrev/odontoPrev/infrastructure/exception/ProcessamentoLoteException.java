package com.odontoPrev.odontoPrev.infrastructure.exception;

import java.util.Map;

/**
 * EXCEÇÃO ESPECÍFICA PARA ERROS NO PROCESSAMENTO DE LOTES DE EMPRESAS
 * 
 * QUANDO ESTA EXCEÇÃO É LANÇADA:
 * - Quando falha ao dividir empresas em grupos menores (lotes)
 * - Quando há erro ao processar uma página de empresas 
 * - Quando o sistema não consegue gerenciar a paginação
 * - Quando há problemas na coordenação do processamento em paralelo
 * 
 * CONTEXTO DO PROBLEMA:
 * O sistema processa milhares de empresas dividindo em "lotes" menores
 * (ex: 50 empresas por vez) para não sobrecarregar memória e banco.
 * Se algo dá errado nessa divisão ou coordenação, esta exceção é disparada.
 * 
 * INFORMAÇÕES ÚTEIS PARA INVESTIGAÇÃO:
 * - Quantas empresas estavam sendo processadas
 * - Qual página/lote estava sendo processado quando deu erro
 * - Configurações de tamanho de lote e threads
 * - Tempo de processamento até o momento do erro
 */
public class ProcessamentoLoteException extends SincronizacaoException {

    // Código único para identificar este tipo de erro nos logs/monitoramento
    public static final String CODIGO_ERRO = "BATCH_PROCESSING_ERROR";

    /**
     * CONSTRUTOR PARA ERRO SEM CAUSA ESPECÍFICA
     * 
     * Usado quando o erro é detectado pela nossa própria lógica
     * (ex: configuração inválida, lote vazio, etc.)
     * 
     * @param detalhes - Explicação específica do que deu errado
     * @param contexto - Informações sobre o estado quando o erro ocorreu
     */
    public ProcessamentoLoteException(String detalhes, Map<String, Object> contexto) {
        super(CODIGO_ERRO, 
              "Falha no processamento de lote: " + detalhes, 
              contexto);
    }

    /**
     * CONSTRUTOR PARA ERRO CAUSADO POR OUTRA EXCEÇÃO
     * 
     * Usado quando o erro foi causado por algo externo
     * (ex: banco de dados fora do ar, problema de memória, etc.)
     * 
     * @param detalhes - Explicação específica do que deu errado
     * @param contexto - Informações sobre o estado quando o erro ocorreu  
     * @param causa - A exceção original que causou este problema
     */
    public ProcessamentoLoteException(String detalhes, Map<String, Object> contexto, Throwable causa) {
        super(CODIGO_ERRO, 
              "Falha no processamento de lote: " + detalhes, 
              contexto, causa);
    }
}