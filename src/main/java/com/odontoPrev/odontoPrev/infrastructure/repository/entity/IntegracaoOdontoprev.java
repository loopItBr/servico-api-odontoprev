package com.odontoPrev.odontoPrev.infrastructure.repository.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import org.hibernate.annotations.Immutable;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "vw_integracao_odontoprev", schema = "TASY")
@Immutable
@Data
@NoArgsConstructor
@AllArgsConstructor
public class IntegracaoOdontoprev {

    @Id
    @Column(name = "CODIGO_EMPRESA", nullable = false, length = 10)
    @NotBlank(message = "Código da empresa é obrigatório")
    @Size(min = 1, max = 10, message = "Código da empresa deve ter entre 1 e 10 caracteres")
    @Pattern(regexp = "^[A-Za-z0-9]+$", message = "Código da empresa deve conter apenas letras e números")
    private String codigoEmpresa;

    @Column(name = "CNPJ", nullable = true, length = 18)
    @Pattern(regexp = "^\\d{2}\\.\\d{3}\\.\\d{3}/\\d{4}-\\d{2}$|^\\d{14}$", 
             message = "CNPJ deve estar no formato XX.XXX.XXX/XXXX-XX ou XXXXXXXXXXXXXX")
    private String cnpj;

    @Column(name = "CODIGO_CLIENTE_OPERADORA", nullable = true)
    private String codigoClienteOperadora;

    @Column(name = "NOME_FANTASIA", nullable = true)
    private String nomeFantasia;

    @Column(name = "DATA_INICIO_CONTRATO", nullable = true)
    @Temporal(TemporalType.DATE)
    private LocalDate dataInicioContrato;

    @Column(name = "DATA_FIM_CONTRATO", nullable = true)
    @Temporal(TemporalType.DATE)
    private LocalDate dataFimContrato;

    @Column(name = "DATA_VIGENCIA", nullable = true)
    @Temporal(TemporalType.DATE)
    private LocalDate dataVigencia;

    @Column(name = "EMPRESA_PF", nullable = true)
    private String empresaPf;

    @Column(name = "CODIGO_GRUPO_GERENCIAL", nullable = true)
    private String codigoGrupoGerencial;

    @Column(name = "CODIGO_MARCA", nullable = true)
    private String codigoMarca;

    @Column(name = "CODIGO_CELULA", nullable = true)
    private String codigoCelula;

    @Column(name = "VIDAS_ATIVAS", nullable = true)
    private Integer vidasAtivas;

    @Column(name = "VALOR_ULTIMO_FATURAMENTO", nullable = true)
    private BigDecimal valorUltimoFaturamento;

    @Column(name = "SINISTRALIDADE", nullable = true)
    private BigDecimal sinistralidade;

    @Column(name = "CODIGO_PLANO", nullable = true)
    private String codigoPlano;

    @Column(name = "DESCRICAO_PLANO", nullable = true)
    private String descricaoPlano;

    @Column(name = "NOME_FANTASIA_PLANO", nullable = true)
    private String nomeFantasiaPlano;

    @Column(name = "NUMERO_REGISTRO_ANS", nullable = true)
    private String numeroRegistroAns;

    @Column(name = "SIGLA_PLANO", nullable = true)
    private String siglaPlano;

    @Column(name = "VALOR_TITULAR", nullable = true)
    private BigDecimal valorTitular;

    @Column(name = "VALOR_DEPENDENTE", nullable = true)
    private BigDecimal valorDependente;

    @Column(name = "DATA_INICIO_PLANO", nullable = true)
    @Temporal(TemporalType.DATE)
    private LocalDate dataInicioPlano;

    @Column(name = "DATA_FIM_PLANO", nullable = true)
    @Temporal(TemporalType.DATE)
    private LocalDate dataFimPlano;

    @Column(name = "CO_PARTICIPACAO", nullable = true)
    private String coParticipacao;

    @Column(name = "TIPO_NEGOCIACAO", nullable = true)
    private String tipoNegociacao;

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
    private Integer numeroParcelas;
}