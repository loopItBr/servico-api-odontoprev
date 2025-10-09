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
 * ENTIDADE QUE REPRESENTA OS DADOS DE INTEGRAÇÃO COM ODONTOPREV
 * 
 * FUNÇÃO PRINCIPAL:
 * Esta classe representa uma VIEW do banco de dados que consolida todas as
 * informações necessárias sobre empresas, planos e contratos para enviar
 * à OdontoPrev durante a sincronização.
 * 
 * IMPORTANTE - É UMA VIEW, NÃO UMA TABELA:
 * - Esta entidade mapeia uma VIEW (vw_integracao_odontoprev)
 * - Views são "consultas salvas" que juntam dados de várias tabelas
 * - Por isso usa @Immutable - os dados não podem ser alterados
 * - Apenas consultamos os dados, não inserimos/atualizamos
 * 
 * ESTRUTURA DOS DADOS:
 * A view consolida informações de:
 * 1. EMPRESA: código, CNPJ, nome fantasia, datas de contrato
 * 2. PLANOS: códigos, descrições, valores, datas de vigência
 * 3. COBRANÇA: tipo, banco, parcelas
 * 4. MÉTRICAS: vidas ativas, faturamento, sinistralidade
 * 
 * USO NO SISTEMA:
 * 1. Sistema consulta esta view para obter lista de empresas
 * 2. Para cada empresa, busca seus dados completos
 * 3. Envia estes dados para API da OdontoPrev
 * 4. Salva controle do que foi enviado
 * 
 * VALIDAÇÕES:
 * Mesmo sendo uma view (dados já validados), mantemos validações para:
 * - Garantir integridade na integração
 * - Facilitar debug de problemas
 * - Proteger contra dados corrompidos
 * 
 * EXEMPLO DE REGISTRO:
 * Uma empresa "ACME Ltda" com CNPJ "12.345.678/0001-90" que tem
 * plano "Básico Empresarial" com 150 vidas ativas e faturamento
 * de R$ 45.000,00 no último mês.
 */
@Entity
@Table(name = "VW_INTEGRACAO_ODONTOPREV", schema = "TASY")
@Immutable  // Indica que esta entidade é apenas para leitura (VIEW)
@Data
@NoArgsConstructor
@AllArgsConstructor
public class IntegracaoOdontoprev {

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
    @Column(name = "CODIGO_CLIENTE_OPERADORA", nullable = true)
    private Long codigoClienteOperadora;

    /**
     * NOME COMERCIAL DA EMPRESA
     * 
     * Nome pelo qual a empresa é conhecida no mercado.
     * Exemplo: "Sabin Medicina Diagnóstica", "Laboratório ABC Ltda"
     */
    @Column(name = "NOME_FANTASIA", nullable = true, length = 80)
    private String nomeFantasia;

    /**
     * DATA DE INÍCIO DA VIGÊNCIA DO CONTRATO
     * Quando o contrato da empresa começou a valer
     */
    @Column(name = "DATA_INICIO_CONTRATO", nullable = true)
    @Temporal(TemporalType.DATE)
    private LocalDate dataInicioContrato;

    /**
     * DATA DE FIM DA VIGÊNCIA DO CONTRATO
     * Quando o contrato da empresa vai expirar ou já expirou
     */
    @Column(name = "DATA_FIM_CONTRATO", nullable = true)
    @Temporal(TemporalType.DATE)
    private LocalDate dataFimContrato;

    /**
     * DATA DE VIGÊNCIA ATUAL
     * Data de referência para validar se contrato está ativo
     */
    @Column(name = "DATA_VIGENCIA", nullable = true)
    @Temporal(TemporalType.DATE)
    private LocalDate dataVigencia;

    /**
     * TIPO DE EMPRESA: PESSOA FÍSICA OU JURÍDICA
     * Indica se é empresa (PJ) ou pessoa física (PF)
     */
    @Column(name = "EMPRESA_PF", nullable = true)
    private String empresaPf;

    // Campos organizacionais da estrutura interna
    @Column(name = "CODIGOGRUPOGERENCIAL", nullable = true)
    private Long codigoGrupoGerencial;

    @Column(name = "CODIGO_MARCA", nullable = true)
    private String codigoMarca;

    @Column(name = "CODIGO_CELULA", nullable = true)
    private String codigoCelula;

    /**
     * NÚMERO DE BENEFICIÁRIOS ATIVOS NO PLANO
     * Quantas pessoas estão cobertas pelo plano odontológico da empresa.
     * Métrica importante para cálculos de faturamento e risco.
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
    private String numeroRegistroAns;

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
    @Temporal(TemporalType.DATE)
    private LocalDate dataInicioPlano;

    @Column(name = "DATA_FIM_PLANO", nullable = true)
    @Temporal(TemporalType.DATE)
    private LocalDate dataFimPlano;

    /**
     * COPARTICIPAÇÃO DO PLANO
     * Define se o plano tem coparticipação (paciente paga parte do tratamento)
     */
    @Column(name = "CO_PARTICIPACAO", nullable = true)
    private String coParticipacao;

    /**
     * TIPO DE NEGOCIAÇÃO COMERCIAL
     * Modalidade de contrato (ex: "CORPORATIVO", "INDIVIDUAL")
     */
    @Column(name = "TIPO_NEGOCIACAO", nullable = true)
    private String tipoNegociacao;

    // === INFORMAÇÕES DE COBRANÇA E PAGAMENTO ===
    
    /**
     * CÓDIGO DO TIPO DE COBRANÇA
     * Identifica a forma de cobrança (boleto, débito, cartão, etc.)
     */
    @Column(name = "CODIGO_TIPO_COBRANCA", nullable = true)
    private String codigoTipoCobranca;

    /**
     * NOME DO TIPO DE COBRANÇA
     * Descrição da forma de pagamento escolhida pela empresa
     */
    @Column(name = "NOME_TIPO_COBRANCA", nullable = true)
    private String nomeTipoCobranca;

    @Column(name = "SIGLA_TIPO_COBRANCA", nullable = true)
    private String siglaTipoCobranca;

    /**
     * CÓDIGO DO BANCO PARA COBRANÇA
     * Número do banco usado para débito automático ou boletos
     */
    @Column(name = "NUMERO_BANCO", nullable = true)
    private String numeroBanco;

    /**
     * NOME DO BANCO PARA COBRANÇA
     * Nome da instituição financeira usada para pagamentos
     */
    @Column(name = "NOME_BANCO", nullable = true)
    private String nomeBanco;

    /**
     * QUANTIDADE DE PARCELAS PARA PAGAMENTO
     * Em quantas vezes o valor será cobrado (normalmente 1 = mensal)
     */
    @Column(name = "NUMERO_PARCELAS", nullable = true)
    private String numeroParcelas;
}