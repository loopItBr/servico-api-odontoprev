package com.odontoPrev.odontoPrev.infrastructure.repository.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import org.hibernate.annotations.Immutable;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "vw_integracao_odontoprev")
@Immutable
@Data
@NoArgsConstructor
@AllArgsConstructor
public class IntegracaoOdontoprev {

    @Id
    @Column(name = "codigo_empresa")
    private String codigoEmpresa;

    @Column(name = "cnpj")
    private String cnpj;

    @Column(name = "codigo_cliente_operadora")
    private String codigoClienteOperadora;

    @Column(name = "nome_fantasia")
    private String nomeFantasia;

    @Column(name = "data_inicio_contrato")
    private LocalDate dataInicioContrato;

    @Column(name = "data_fim_contrato")
    private LocalDate dataFimContrato;

    @Column(name = "data_vigencia")
    private LocalDate dataVigencia;

    @Column(name = "empresa_pf")
    private Boolean empresaPf;

    @Column(name = "codigo_grupo_gerencial")
    private String codigoGrupoGerencial;

    @Column(name = "codigo_marca")
    private String codigoMarca;

    @Column(name = "codigo_celula")
    private String codigoCelula;

    @Column(name = "vidas_ativas")
    private Integer vidasAtivas;

    @Column(name = "valor_ultimo_faturamento")
    private BigDecimal valorUltimoFaturamento;

    @Column(name = "sinistralidade")
    private BigDecimal sinistralidade;

    @Column(name = "codigo_plano")
    private String codigoPlano;

    @Column(name = "descricao_plano")
    private String descricaoPlano;

    @Column(name = "nome_fantasia_plano")
    private String nomeFantasiaPlano;

    @Column(name = "numero_registro_ans")
    private String numeroRegistroAns;

    @Column(name = "sigla_plano")
    private String siglaPlano;

    @Column(name = "valor_titular")
    private BigDecimal valorTitular;

    @Column(name = "valor_dependente")
    private BigDecimal valorDependente;

    @Column(name = "data_inicio_plano")
    private LocalDate dataInicioPlano;

    @Column(name = "data_fim_plano")
    private LocalDate dataFimPlano;

    @Column(name = "co_participacao")
    private Boolean coParticipacao;

    @Column(name = "tipo_negociacao")
    private String tipoNegociacao;

    @Column(name = "codigo_tipo_cobranca")
    private String codigoTipoCobranca;

    @Column(name = "nome_tipo_cobranca")
    private String nomeTipoCobranca;

    @Column(name = "sigla_tipo_cobranca")
    private String siglaTipoCobranca;

    @Column(name = "numero_banco")
    private String numeroBanco;

    @Column(name = "nome_banco")
    private String nomeBanco;

    @Column(name = "numero_parcelas")
    private Integer numeroParcelas;
}