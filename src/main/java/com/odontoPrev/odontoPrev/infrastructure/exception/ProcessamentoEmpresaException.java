package com.odontoPrev.odontoPrev.infrastructure.exception;

import java.util.Map;

/**
 * EXCEÇÃO ESPECÍFICA PARA ERROS NO PROCESSAMENTO DE UMA EMPRESA INDIVIDUAL
 * 
 * QUANDO ESTA EXCEÇÃO É LANÇADA:
 * - Quando falha ao processar os dados de uma empresa específica
 * - Quando há erro ao consultar informações na API da OdontoPrev para uma empresa
 * - Quando os dados de uma empresa estão corrompidos ou inválidos
 * - Quando uma empresa específica causa erro que impede o processamento
 * 
 * IMPORTÂNCIA:
 * Esta exceção é crucial pois identifica EXATAMENTE qual empresa causou problema.
 * Em um lote de 1000 empresas, se uma der erro, sabemos qual é sem perder as outras.
 * 
 * EXEMPLO PRÁTICO:
 * - Lote processando empresas: A001, A002, A003, A004
 * - Empresa A003 tem dados inválidos na API
 * - Sistema lança esta exceção identificando "A003" como problemática  
 * - As outras empresas (A001, A002, A004) continuam sendo processadas
 * 
 * INFORMAÇÕES ÚTEIS PARA INVESTIGAÇÃO:
 * - Código da empresa que causou o erro
 * - Dados específicos da empresa no momento do erro
 * - Qual operação estava sendo realizada (consulta, validação, etc.)
 */
public class ProcessamentoEmpresaException extends SincronizacaoException {

    // Código único para identificar este tipo de erro nos logs/monitoramento
    public static final String CODIGO_ERRO = "COMPANY_PROCESSING_ERROR";

    /**
     * CONSTRUTOR PARA ERRO DE EMPRESA SEM CAUSA ESPECÍFICA
     * 
     * Usado quando detectamos que uma empresa específica tem problema
     * na nossa própria validação ou lógica de negócio.
     * 
     * @param codigoEmpresa - Identificador único da empresa problemática
     * @param detalhes - Explicação específica do que deu errado com esta empresa
     * @param contexto - Informações sobre o estado da empresa quando o erro ocorreu
     */
    public ProcessamentoEmpresaException(String codigoEmpresa, String detalhes, Map<String, Object> contexto) {
        super(CODIGO_ERRO, 
              String.format("Falha no processamento da empresa %s: %s", codigoEmpresa, detalhes), 
              contexto);
    }

    /**
     * CONSTRUTOR PARA ERRO DE EMPRESA CAUSADO POR OUTRA EXCEÇÃO
     * 
     * Usado quando uma empresa específica causou erro devido a problema externo
     * (ex: timeout na consulta à API, dados corrompidos no banco, etc.)
     * 
     * @param codigoEmpresa - Identificador único da empresa problemática  
     * @param detalhes - Explicação específica do que deu errado com esta empresa
     * @param contexto - Informações sobre o estado da empresa quando o erro ocorreu
     * @param causa - A exceção original que causou este problema com a empresa
     */
    public ProcessamentoEmpresaException(String codigoEmpresa, String detalhes, Map<String, Object> contexto, Throwable causa) {
        super(CODIGO_ERRO, 
              String.format("Falha no processamento da empresa %s: %s", codigoEmpresa, detalhes), 
              contexto, causa);
    }
}