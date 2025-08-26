package com.odontoPrev.odontoPrev.infrastructure.exception;

import lombok.Getter;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * CLASSE BASE PARA TODAS AS EXCEÇÕES DE SINCRONIZAÇÃO
 * 
 * Esta é uma classe "pai" que serve como modelo para todas as outras exceções 
 * relacionadas ao processo de sincronização com a OdontoPrev.
 * 
 * OBJETIVO:
 * - Padronizar informações que toda exceção de sincronização deve ter
 * - Facilitar o rastreamento de erros em ferramentas de monitoramento
 * - Fornecer contexto detalhado sobre quando e onde o erro ocorreu
 * 
 * INFORMAÇÕES ARMAZENADAS:
 * - Código do erro: identificador único para o tipo de problema
 * - Data/hora exata quando o erro aconteceu 
 * - Contexto adicional: informações extras sobre o estado do sistema
 * 
 * EXEMPLO DE USO:
 * Quando algo dá errado na sincronização, criamos uma exceção específica
 * (ex: ProcessamentoLoteException) que herda desta classe base.
 */
@Getter
public abstract class SincronizacaoException extends RuntimeException {

    // Código identificador único para este tipo de erro (ex: "LOTE_PROCESSING_ERROR")
    private final String codigoErro;
    
    // Momento exato em que o erro ocorreu - útil para rastreamento temporal
    private final LocalDateTime dataHoraOcorrencia;
    
    // Informações extras sobre o erro: qual empresa, qual lote, configurações, etc.
    private final Map<String, Object> contextoAdicional;

    /**
     * CONSTRUTOR BÁSICO PARA CRIAR UMA EXCEÇÃO DE SINCRONIZAÇÃO
     * 
     * @param codigoErro - Identificador único do tipo de erro (ex: "BATCH_ERROR") 
     * @param mensagem - Descrição legível do que aconteceu
     * @param contexto - Informações extras sobre o erro (empresa, configurações, etc.)
     */
    protected SincronizacaoException(String codigoErro, String mensagem, Map<String, Object> contexto) {
        // Chama o construtor da classe RuntimeException com a mensagem
        super(mensagem);
        
        // Armazena o código identificador deste erro
        this.codigoErro = codigoErro;
        
        // Registra o momento exato que o erro aconteceu
        this.dataHoraOcorrencia = LocalDateTime.now();
        
        // Guarda informações extras sobre o contexto do erro
        this.contextoAdicional = contexto;
    }

    /**
     * CONSTRUTOR COMPLETO PARA EXCEÇÃO COM CAUSA RAIZ
     * 
     * Este construtor é usado quando nossa exceção foi causada por outra exceção
     * (ex: erro de banco de dados, problema de rede, etc.)
     * 
     * @param codigoErro - Identificador único do tipo de erro
     * @param mensagem - Descrição do que aconteceu
     * @param contexto - Informações extras sobre o erro
     * @param causa - A exceção original que causou este problema
     */
    protected SincronizacaoException(String codigoErro, String mensagem, Map<String, Object> contexto, Throwable causa) {
        // Chama o construtor da RuntimeException passando mensagem e causa
        super(mensagem, causa);
        
        // Inicializa os mesmos campos do construtor anterior
        this.codigoErro = codigoErro;
        this.dataHoraOcorrencia = LocalDateTime.now();
        this.contextoAdicional = contexto;
    }
}