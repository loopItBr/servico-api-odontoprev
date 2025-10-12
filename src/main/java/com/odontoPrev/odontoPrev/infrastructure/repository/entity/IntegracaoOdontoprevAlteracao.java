package com.odontoPrev.odontoPrev.infrastructure.repository.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import org.hibernate.annotations.Immutable;

import java.time.LocalDate;

/**
 * ENTIDADE QUE REPRESENTA OS DADOS DE ALTERAÇÃO DE EMPRESAS PARA INTEGRAÇÃO COM ODONTOPREV
 * 
 * FUNÇÃO PRINCIPAL:
 * Esta classe representa a VIEW VW_INTEGRACAO_ODONTOPREV_ALT que consolida todas as
 * informações sobre empresas que foram alteradas e precisam ser sincronizadas
 * com a OdontoPrev.
 * 
 * IMPORTANTE - É UMA VIEW, NÃO UMA TABELA:
 * - Esta entidade mapeia uma VIEW (vw_integracao_odontoprev_alt)
 * - Views são "consultas salvas" que juntam dados de várias tabelas
 * - Por isso usa @Immutable - os dados não podem ser alterados
 * - Apenas consultamos os dados, não inserimos/atualizamos
 * 
 * CRITÉRIO DE SELEÇÃO:
 * - Empresas que tiveram DT_ALTERACAO = SYSDATE (alteradas hoje)
 * - Identifica empresas que precisam ter dados atualizados na OdontoPrev
 * 
 * ESTRUTURA DOS DADOS:
 * A view consolida informações de:
 * 1. EMPRESA: código, CNPJ, nome fantasia, datas de contrato
 * 2. PLANOS: códigos, descrições, valores, datas de vigência
 * 3. COBRANÇA: tipo, banco, parcelas (atualmente NULL)
 * 
 * USO NO SISTEMA:
 * 1. Sistema consulta esta view para obter lista de empresas alteradas
 * 2. Para cada empresa, busca seus dados completos
 * 3. Envia estes dados para API da OdontoPrev com operação UPDATE
 * 4. Salva controle do que foi enviado com tipo_controle = 2
 */
@Entity
@Table(name = "VW_INTEGRACAO_ODONTOPREV_ALT", schema = "TASY")
@Immutable
@Data
@NoArgsConstructor
@AllArgsConstructor
public class IntegracaoOdontoprevAlteracao {

    /**
     * CÓDIGO ÚNICO DA EMPRESA NO SISTEMA TASY
     * 
     * Este é o identificador principal de cada empresa em nosso sistema.
     * É usado como chave primária e para buscar dados da empresa na OdontoPrev.
     * 
     * Exemplo: "A001", "EMP123", "XYZ789"
     */
    @Id
    @Column(name = "CODIGO_EMPRESA", nullable = false, length = 6)
    @NotBlank(message = "Código da empresa é obrigatório")
    @Size(min = 1, max = 6, message = "Código da empresa deve ter entre 1 e 6 caracteres")
    @Pattern(regexp = "^[A-Za-z0-9]+$", message = "Código da empresa deve conter apenas letras e números")
    private String codigoEmpresa;

    /**
     * CNPJ DA EMPRESA (CADASTRO NACIONAL DE PESSOA JURÍDICA)
     * 
     * Documento oficial que identifica a empresa junto à Receita Federal.
     * Pode vir formatado (12.345.678/0001-90) ou apenas números (12345678000190).
     * Campo opcional porque algumas empresas podem não ter CNPJ cadastrado ainda.
     */
    @Column(name = "CNPJ", nullable = true, length = 14)
    @Pattern(regexp = "^\\d{2}\\.\\d{3}\\.\\d{3}/\\d{4}-\\d{2}$|^\\d{14}$", 
             message = "CNPJ deve estar no formato XX.XXX.XXX/XXXX-XX ou XXXXXXXXXXXXXX")
    private String cnpj;

    /**
     * CÓDIGO DA EMPRESA NA OPERADORA ODONTOPREV
     * 
     * Quando a empresa já existe na OdontoPrev, ela recebe um código específico
     * deles. Este campo armazena esse código para futuras sincronizações.
     * Campo opcional porque empresas novas ainda não têm código na operadora.
     */
    @Column(name = "CODIGO_CLIENTE_OPERADORA", nullable = true, length = 6)
    private String codigoClienteOperadora;

    /**
     * NOME COMERCIAL DA EMPRESA
     * 
     * Nome pelo qual a empresa é conhecida no mercado.
     * Exemplo: "Sabin Medicina Diagnóstica", "Laboratório ABC Ltda"
     */
    @Column(name = "NOME_FANTASIA", nullable = true, length = 80)
    private String nomeFantasia;

    /**
     * DATA DE INÍCIO DO CONTRATO COM A EMPRESA
     * Quando começou a vigência do contrato odontológico
     */
    @Column(name = "DATA_INICIO_CONTRATO", nullable = true)
    private LocalDate dataInicioContrato;

    /**
     * DATA DE FIM DO CONTRATO COM A EMPRESA
     * Quando termina a vigência do contrato odontológico
     */
    @Column(name = "DATA_FIM_CONTRATO", nullable = true)
    private LocalDate dataFimContrato;

    /**
     * DATA DE VIGÊNCIA ATUAL DO CONTRATO
     * Data de referência para cálculos e cobranças
     */
    @Column(name = "DATA_VIGENCIA", nullable = true)
    private LocalDate dataVigencia;

    /**
     * INDICA SE É EMPRESA PESSOA FÍSICA
     * true = pessoa física, false = pessoa jurídica
     */
    @Column(name = "EMPRESA_PF", nullable = true, length = 60)
    private String empresaPf;

    /**
     * CÓDIGO DO GRUPO GERENCIAL
     * Identifica o grupo administrativo da empresa
     */
    @Column(name = "CODIGO_GRUPO_GERENCIAL", nullable = true)
    private Long codigoGrupoGerencial;

    /**
     * CÓDIGO DA MARCA
     * Identifica a marca comercial da empresa
     */
    @Column(name = "CODIGO_MARCA", nullable = true)
    private Long codigoMarca;

    /**
     * CÓDIGO DA CÉLULA
     * Identifica a célula de negócio da empresa
     */
    @Column(name = "CODIGO_CELULA", nullable = true)
    private Long codigoCelula;

    /**
     * NÚMERO DE VIDAS ATIVAS DA EMPRESA
     * Quantos funcionários estão ativos no plano odontológico
     */
    @Column(name = "VIDAS_ATIVAS", nullable = true)
    private Long vidasAtivas;

    /**
     * VALOR DO ÚLTIMO FATURAMENTO DA EMPRESA
     * Quanto a empresa pagou na última cobrança.
     * Usado para análises financeiras e de risco.
     */
    @Column(name = "VALOR_ULTIMO_FATURAMENTO", nullable = true)
    private String valorUltimoFaturamento;

    /**
     * ÍNDICE DE SINISTRALIDADE DA EMPRESA
     * Relação entre o que a empresa paga e o que gasta em tratamentos.
     * Sinistralidade alta = empresa gasta mais do que paga.
     */
    @Column(name = "SINISTRALIDADE", nullable = true)
    private String sinistralidade;

    /**
     * CÓDIGO IDENTIFICADOR DO PLANO ODONTOLÓGICO
     * Código único que identifica o plano contratado pela empresa
     */
    @Column(name = "CODIGO_PLANO", nullable = true)
    private Long codigoPlano;

    /**
     * DESCRIÇÃO DETALHADA DO PLANO
     * Nome completo e características do plano odontológico
     */
    @Column(name = "DESCRICAO_PLANO", nullable = true)
    private String descricaoPlano;

    /**
     * NOME COMERCIAL DO PLANO
     * Nome simplificado usado para divulgação
     */
    @Column(name = "NOME_FANTASIA_PLANO", nullable = true)
    private String nomeFantasiaPlano;

    /**
     * REGISTRO NA ANS (AGÊNCIA NACIONAL DE SAÚDE SUPLEMENTAR)
     * Número oficial do plano junto ao órgão regulador
     */
    @Column(name = "NUMERO_REGISTRO_ANS", nullable = true)
    private Long numeroRegistroAns;

    /**
     * SIGLA OU ABREVIAÇÃO DO PLANO
     * Identificação curta do plano (ex: "BAS", "PREM", "EXEC")
     */
    @Column(name = "SIGLA_PLANO", nullable = true)
    private String siglaPlano;

    /**
     * VALOR MENSAL POR TITULAR DO PLANO
     * Quanto cada funcionário principal da empresa paga mensalmente
     */
    @Column(name = "VALOR_TITULAR", nullable = true)
    private String valorTitular;

    /**
     * VALOR MENSAL POR DEPENDENTE
     * Quanto cada dependente (cônjuge, filhos) custa mensalmente
     */
    @Column(name = "VALOR_DEPENDENTE", nullable = true)
    private String valorDependente;

    // Datas de vigência específicas do plano
    @Column(name = "DATA_INICIO_PLANO", nullable = true)
    private LocalDate dataInicioPlano;

    @Column(name = "DATA_FIM_PLANO", nullable = true)
    private LocalDate dataFimPlano;

    /**
     * CO-PARTICIPAÇÃO DO PLANO
     * Percentual que o beneficiário paga em cada procedimento
     */
    @Column(name = "CO_PARTICIPACAO", nullable = true, length = 1)
    private String coParticipacao;

    /**
     * TIPO DE NEGOCIAÇÃO DO PLANO
     * Como foi negociado o plano (individual, coletivo, etc.)
     */
    @Column(name = "TIPO_NEGOCIACAO", nullable = true, length = 2)
    private String tipoNegociacao;

    // Campos de cobrança (atualmente NULL na view)
    @Column(name = "CODIGO_TIPO_COBRANCA", nullable = true)
    private String codigoTipoCobranca;

    @Column(name = "NOME_TIPO_COBRANCA", nullable = true)
    private String nomeTipoCobranca;

    @Column(name = "SIGLA_TIPO_COBRANCA", nullable = true)
    private String siglaTipoCobranca;

    @Column(name = "NUMERO_BANCO", nullable = true)
    private String numeroBanco;

    @Column(name = "NOME_BANCO", nullable = true)
    private String nomeBanco;

    @Column(name = "NUMERO_PARCELAS", nullable = true)
    private String numeroParcelas;

    /**
     * CÓDIGO DO USUÁRIO
     * 
     * Código do usuário responsável pela alteração da empresa.
     * Este campo é usado na API de alteração da OdontoPrev.
     */
    @Column(name = "CODUSUARIO", nullable = true, length = 20)
    private String codUsuario;
}
