package com.odontoPrev.odontoPrev.infrastructure.exception;

/**
 * EXCEÇÃO PARA ERROS DE PROCESSAMENTO DE BENEFICIÁRIOS
 *
 * Esta exceção é lançada quando ocorrem erros durante o processamento
 * de beneficiários, incluindo validações, comunicação com API e
 * atualizações de status.
 *
 * QUANDO É USADA:
 * - Erro de validação de dados obrigatórios
 * - Erro de comunicação com API da OdontoPrev
 * - Erro ao executar procedure no Tasy
 * - Erro ao atualizar status no banco
 * - Erro de mapeamento de dados
 *
 * CARACTERÍSTICAS:
 * - Herda de RuntimeException (não checked)
 * - Inclui código de erro específico
 * - Suporta causa raiz (Throwable)
 * - Mensagem descritiva do erro
 * - Inclui informações do beneficiário (matrícula)
 */
public class ProcessamentoBeneficiarioException extends RuntimeException {

    private final String codigoErro;
    private final String codigoMatricula;
    private final TipoOperacao tipoOperacao;

    /**
     * CONSTRUTOR COM MENSAGEM
     *
     * @param mensagem descrição do erro
     */
    public ProcessamentoBeneficiarioException(String mensagem) {
        super(mensagem);
        this.codigoErro = "PROCESSAMENTO_BENEFICIARIO_ERRO";
        this.codigoMatricula = null;
        this.tipoOperacao = null;
    }

    /**
     * CONSTRUTOR COM MENSAGEM E MATRÍCULA
     *
     * @param mensagem descrição do erro
     * @param codigoMatricula código da matrícula do beneficiário
     */
    public ProcessamentoBeneficiarioException(String mensagem, String codigoMatricula) {
        super(mensagem);
        this.codigoErro = "PROCESSAMENTO_BENEFICIARIO_ERRO";
        this.codigoMatricula = codigoMatricula;
        this.tipoOperacao = null;
    }

    /**
     * CONSTRUTOR COM MENSAGEM, MATRÍCULA E TIPO DE OPERAÇÃO
     *
     * @param mensagem descrição do erro
     * @param codigoMatricula código da matrícula do beneficiário
     * @param tipoOperacao tipo da operação que falhou
     */
    public ProcessamentoBeneficiarioException(String mensagem, String codigoMatricula, TipoOperacao tipoOperacao) {
        super(mensagem);
        this.codigoErro = "PROCESSAMENTO_BENEFICIARIO_ERRO";
        this.codigoMatricula = codigoMatricula;
        this.tipoOperacao = tipoOperacao;
    }

    /**
     * CONSTRUTOR COM MENSAGEM, MATRÍCULA, TIPO DE OPERAÇÃO E CAUSA
     *
     * @param mensagem descrição do erro
     * @param codigoMatricula código da matrícula do beneficiário
     * @param tipoOperacao tipo da operação que falhou
     * @param causa exceção que causou o erro
     */
    public ProcessamentoBeneficiarioException(String mensagem, String codigoMatricula, TipoOperacao tipoOperacao, Throwable causa) {
        super(mensagem, causa);
        this.codigoErro = "PROCESSAMENTO_BENEFICIARIO_ERRO";
        this.codigoMatricula = codigoMatricula;
        this.tipoOperacao = tipoOperacao;
    }

    /**
     * CONSTRUTOR COM MENSAGEM E CAUSA
     *
     * @param mensagem descrição do erro
     * @param causa exceção que causou o erro
     */
    public ProcessamentoBeneficiarioException(String mensagem, Throwable causa) {
        super(mensagem, causa);
        this.codigoErro = "PROCESSAMENTO_BENEFICIARIO_ERRO";
        this.codigoMatricula = null;
        this.tipoOperacao = null;
    }

    /**
     * CONSTRUTOR COM MENSAGEM, CAUSA E CÓDIGO DE ERRO
     *
     * @param mensagem descrição do erro
     * @param causa exceção que causou o erro
     * @param codigoErro código específico do erro
     */
    public ProcessamentoBeneficiarioException(String mensagem, Throwable causa, String codigoErro) {
        super(mensagem, causa);
        this.codigoErro = codigoErro;
        this.codigoMatricula = null;
        this.tipoOperacao = null;
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
     * RETORNA O CÓDIGO DA MATRÍCULA
     *
     * @return código da matrícula do beneficiário
     */
    public String getCodigoMatricula() {
        return codigoMatricula;
    }

    /**
     * RETORNA O TIPO DE OPERAÇÃO
     *
     * @return tipo da operação que falhou
     */
    public TipoOperacao getTipoOperacao() {
        return tipoOperacao;
    }

    /**
     * ENUM PARA TIPOS DE OPERAÇÃO
     */
    public enum TipoOperacao {
        INCLUSAO("INCLUSAO"),
        ALTERACAO("ALTERACAO"),
        EXCLUSAO("EXCLUSAO");

        private final String valor;

        TipoOperacao(String valor) {
            this.valor = valor;
        }

        public String getValor() {
            return valor;
        }
    }

    /**
     * CÓDIGOS DE ERRO ESPECÍFICOS
     */
    public static class CodigoErro {
        public static final String DADOS_OBRIGATORIOS_AUSENTES = "DADOS_OBRIGATORIOS_AUSENTES";
        public static final String COMUNICACAO_API_ERRO = "COMUNICACAO_API_ERRO";
        public static final String PROCEDURE_TASY_ERRO = "PROCEDURE_TASY_ERRO";
        public static final String ATUALIZACAO_STATUS_ERRO = "ATUALIZACAO_STATUS_ERRO";
        public static final String MAPEAMENTO_ERRO = "MAPEAMENTO_ERRO";
        public static final String VALIDACAO_ERRO = "VALIDACAO_ERRO";
        public static final String CD_ASSOCIADO_NAO_RETORNADO = "CD_ASSOCIADO_NAO_RETORNADO";
        public static final String BENEFICIARIO_NAO_ENCONTRADO = "BENEFICIARIO_NAO_ENCONTRADO";
    }
}
