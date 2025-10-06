package com.odontoPrev.odontoPrev.infrastructure.exception;

/**
 * EXCEÇÃO PARA ERROS DE CONSULTA DE BENEFICIÁRIOS
 *
 * Esta exceção é lançada quando ocorrem erros durante operações
 * de consulta de beneficiários, incluindo validações e processamento
 * de dados das views.
 *
 * QUANDO É USADA:
 * - Erro ao buscar beneficiário por matrícula
 * - Erro ao contar beneficiários pendentes
 * - Erro de validação de dados das views
 * - Erro de mapeamento entre entidades
 *
 * CARACTERÍSTICAS:
 * - Herda de RuntimeException (não checked)
 * - Inclui código de erro específico
 * - Suporta causa raiz (Throwable)
 * - Mensagem descritiva do erro
 */
public class ConsultaBeneficiarioException extends RuntimeException {

    private final String codigoErro;

    /**
     * CONSTRUTOR COM MENSAGEM
     *
     * @param mensagem descrição do erro
     */
    public ConsultaBeneficiarioException(String mensagem) {
        super(mensagem);
        this.codigoErro = "CONSULTA_BENEFICIARIO_ERRO";
    }

    /**
     * CONSTRUTOR COM MENSAGEM E CAUSA
     *
     * @param mensagem descrição do erro
     * @param causa exceção que causou o erro
     */
    public ConsultaBeneficiarioException(String mensagem, Throwable causa) {
        super(mensagem, causa);
        this.codigoErro = "CONSULTA_BENEFICIARIO_ERRO";
    }

    /**
     * CONSTRUTOR COM MENSAGEM, CAUSA E CÓDIGO DE ERRO
     *
     * @param mensagem descrição do erro
     * @param causa exceção que causou o erro
     * @param codigoErro código específico do erro
     */
    public ConsultaBeneficiarioException(String mensagem, Throwable causa, String codigoErro) {
        super(mensagem, causa);
        this.codigoErro = codigoErro;
    }

    /**
     * CONSTRUTOR COM MENSAGEM E CÓDIGO DE ERRO
     *
     * @param mensagem descrição do erro
     * @param codigoErro código específico do erro
     */
    public ConsultaBeneficiarioException(String mensagem, String codigoErro) {
        super(mensagem);
        this.codigoErro = codigoErro;
    }

    /**
     * RETORNA O CÓDIGO DE ERRO
     *
     * @return código específico do erro
     */
    public String getCodigoErro() {
        return codigoErro;
    }

    /**
     * CÓDIGOS DE ERRO ESPECÍFICOS
     */
    public static class CodigoErro {
        public static final String BENEFICIARIO_NAO_ENCONTRADO = "BENEFICIARIO_NAO_ENCONTRADO";
        public static final String DADOS_INVALIDOS = "DADOS_INVALIDOS";
        public static final String MAPEAMENTO_ERRO = "MAPEAMENTO_ERRO";
        public static final String CONSULTA_ERRO = "CONSULTA_ERRO";
        public static final String VALIDACAO_ERRO = "VALIDACAO_ERRO";
    }
}
