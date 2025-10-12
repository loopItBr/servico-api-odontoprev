package com.odontoPrev.odontoPrev.infrastructure.client.adapter.out.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * DTO PARA ATIVAÇÃO DO PLANO DA EMPRESA NA ODONTOPREV
 *
 * Esta classe contém os dados necessários para ativar o plano de uma empresa
 * no sistema da OdontoPrev através da API /empresa/2.0/empresas/contrato/empresarial.
 *
 * QUANDO É USADA:
 * - Após o cadastro bem-sucedido da sincronização da empresa
 * - Para ativar os planos odontológicos da empresa
 *
 * ESTRUTURA CONFORME DOCUMENTAÇÃO:
 * - Dados da empresa (CNPJ, razão social, nome fantasia)
 * - Dados do contrato (vigência, valores, planos)
 * - Dados de cobrança e endereço
 * - Contatos e comissionamentos
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmpresaAtivacaoPlanoRequest {

    @JsonProperty("sistema")
    private String sistema;

    @JsonProperty("tipoPessoa")
    private String tipoPessoa;

    @JsonProperty("emiteCarteirinhaPlastica")
    private String emiteCarteirinhaPlastica;

    @JsonProperty("codigoEmpresaGestora")
    private Integer codigoEmpresaGestora;

    @JsonProperty("codigoFilialEmpresaGestora")
    private Integer codigoFilialEmpresaGestora;

    @JsonProperty("codigoGrupoGerencial")
    private String codigoGrupoGerencial;

    @JsonProperty("codigoNaturezaJuridica")
    private String codigoNaturezaJuridica;

    @JsonProperty("nomeNaturezaJuridica")
    private String nomeNaturezaJuridica;

    @JsonProperty("situacaoCadastral")
    private String situacaoCadastral;

    @JsonProperty("inscricaoMunicipal")
    private String inscricaoMunicipal;

    @JsonProperty("inscricaoEstadual")
    private String inscricaoEstadual;

    @JsonProperty("dataConstituicao")
    private String dataConstituicao;

    @JsonProperty("renovacaoAutomatica")
    private String renovacaoAutomatica;

    @JsonProperty("codigoClausulaReajusteDiferenciado")
    private String codigoClausulaReajusteDiferenciado;

    @JsonProperty("departamento")
    private String departamento;

    @JsonProperty("dependentePaga")
    private String dependentePaga;

    @JsonProperty("permissaoCadastroDep")
    private Boolean permissaoCadastroDep;

    @JsonProperty("modeloCobrancaVarejo")
    private Boolean modeloCobrancaVarejo;

    @JsonProperty("numeroMinimoAssociados")
    private Integer numeroMinimoAssociados;

    @JsonProperty("numeroFuncionarios")
    private Integer numeroFuncionarios;

    @JsonProperty("numeroDepedentes")
    private Integer numeroDepedentes;

    @JsonProperty("idadeLimiteDependente")
    private Integer idadeLimiteDependente;

    @JsonProperty("valorFator")
    private Integer valorFator;

    @JsonProperty("tipoRetornoCritica")
    private String tipoRetornoCritica;

    @JsonProperty("codigoLayoutCarteirinha")
    private String codigoLayoutCarteirinha;

    @JsonProperty("codigoOrdemCarteira")
    private Integer codigoOrdemCarteira;

    @JsonProperty("codigoDocumentoContrato")
    private Integer codigoDocumentoContrato;

    @JsonProperty("codigoCelula")
    private Integer codigoCelula;

    @JsonProperty("codigoMarca")
    private Integer codigoMarca;

    @JsonProperty("codigoDescricaoNF")
    private Integer codigoDescricaoNF;

    @JsonProperty("diaVencimentoAg")
    private Integer diaVencimentoAg;

    @JsonProperty("codigoPerfilClienteFatura")
    private Integer codigoPerfilClienteFatura;

    @JsonProperty("codigoBancoFatura")
    private String codigoBancoFatura;

    @JsonProperty("multaFatura")
    private Integer multaFatura;

    @JsonProperty("descontaIR")
    private String descontaIR;

    @JsonProperty("retencaoIss")
    private String retencaoIss;

    @JsonProperty("liberaSenhaInternet")
    private String liberaSenhaInternet;

    @JsonProperty("faturamentoNotaCorte")
    private String faturamentoNotaCorte;

    @JsonProperty("proRata")
    private String proRata;

    @JsonProperty("custoFamiliar")
    private String custoFamiliar;

    @JsonProperty("planoFamiliar")
    private String planoFamiliar;

    @JsonProperty("percSinistroContrato")
    private Integer percSinistroContrato;

    @JsonProperty("idadeLimiteUniversitaria")
    private Integer idadeLimiteUniversitaria;

    @JsonProperty("percentualINSSAutoGestao")
    private Integer percentualINSSAutoGestao;

    @JsonProperty("percentualMateriaisAutoGestao")
    private Integer percentualMateriaisAutoGestao;

    @JsonProperty("valorSinistroContrato")
    private Double valorSinistroContrato;

    @JsonProperty("percentualAssociado")
    private Integer percentualAssociado;

    @JsonProperty("codigoRegiao")
    private Integer codigoRegiao;

    @JsonProperty("codigoImagemFatura")
    private Integer codigoImagemFatura;

    @JsonProperty("codigoMoeda")
    private String codigoMoeda;

    @JsonProperty("codigoParceriaEstrategica")
    private Integer codigoParceriaEstrategica;

    @JsonProperty("sinistralidade")
    private Integer sinistralidade;

    @JsonProperty("posicaoIniTIT")
    private Integer posicaoIniTIT;

    @JsonProperty("posicaoFimTIT")
    private Integer posicaoFimTIT;

    @JsonProperty("regraDowngrade")
    private Integer regraDowngrade;

    @JsonProperty("mesCompetenciaProximoFaturamento")
    private String mesCompetenciaProximoFaturamento;

    @JsonProperty("codigoUsuarioFaturamento")
    private String codigoUsuarioFaturamento;

    @JsonProperty("codigoUsuarioCadastro")
    private String codigoUsuarioCadastro;

    @JsonProperty("ramo")
    private String ramo;

    @JsonProperty("cgc")
    private String cgc;

    @JsonProperty("razaoSocial")
    private String razaoSocial;

    @JsonProperty("nomeFantasia")
    private String nomeFantasia;

    @JsonProperty("diaInicioFaturamento")
    private Integer diaInicioFaturamento;

    @JsonProperty("codigoUsuarioConsultor")
    private String codigoUsuarioConsultor;

    @JsonProperty("mesAniversarioReajuste")
    private Integer mesAniversarioReajuste;

    @JsonProperty("dataInicioContrato")
    private String dataInicioContrato;

    @JsonProperty("dataVigencia")
    private String dataVigencia;

    @JsonProperty("descricaoRamoAtividade")
    private String descricaoRamoAtividade;

    @JsonProperty("planos")
    private List<Plano> planos;

    @JsonProperty("diaVencimento")
    private Integer diaVencimento;

    @JsonProperty("cnae")
    private String cnae;

    @JsonProperty("codigoManual")
    private String codigoManual;

    @JsonProperty("diaLimiteConsumoAg")
    private Integer diaLimiteConsumoAg;

    @JsonProperty("grausParentesco")
    private List<GrauParentesco> grausParentesco;

    @JsonProperty("contatosDaFatura")
    private List<ContatoFatura> contatosDaFatura;

    @JsonProperty("email")
    private String email;

    @JsonProperty("endereco")
    private Endereco endereco;

    @JsonProperty("cobranca")
    private Cobranca cobranca;

    @JsonProperty("diaMovAssociadoEmpresa")
    private Integer diaMovAssociadoEmpresa;

    @JsonProperty("contatos")
    private List<Contato> contatos;

    @JsonProperty("comissionamentos")
    private List<Comissionamento> comissionamentos;

    @JsonProperty("grupos")
    private List<Grupo> grupos;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Plano {
        @JsonProperty("codigoPlano")
        private String codigoPlano;

        @JsonProperty("dataInicioPlano")
        private String dataInicioPlano;

        @JsonProperty("valorDependente")
        private Double valorDependente;

        @JsonProperty("valorReembolsoUO")
        private Double valorReembolsoUO;

        @JsonProperty("valorTitular")
        private Double valorTitular;

        @JsonProperty("periodicidade")
        private String periodicidade;

        @JsonProperty("percentualAssociado")
        private Double percentualAssociado;

        @JsonProperty("percentualDependenteRedeGenerica")
        private Double percentualDependenteRedeGenerica;

        @JsonProperty("percentualAgregadoRedeGenerica")
        private Double percentualAgregadoRedeGenerica;

        @JsonProperty("redes")
        private List<Rede> redes;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Rede {
        @JsonProperty("codigoRede")
        private String codigoRede;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class GrauParentesco {
        @JsonProperty("codigoGrauParentesco")
        private String codigoGrauParentesco;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ContatoFatura {
        @JsonProperty("codSequencial")
        private Integer codSequencial;

        @JsonProperty("email")
        private String email;

        @JsonProperty("nomeContato")
        private String nomeContato;

        @JsonProperty("relatorio")
        private Boolean relatorio;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Endereco {
        @JsonProperty("cep")
        private String cep;

        @JsonProperty("descricao")
        private String descricao;

        @JsonProperty("complemento")
        private String complemento;

        @JsonProperty("tipoLogradouro")
        private String tipoLogradouro;

        @JsonProperty("logradouro")
        private String logradouro;

        @JsonProperty("numero")
        private String numero;

        @JsonProperty("bairro")
        private String bairro;

        @JsonProperty("cidade")
        private Cidade cidade;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Cidade {
        @JsonProperty("codigo")
        private Integer codigo;

        @JsonProperty("nome")
        private String nome;

        @JsonProperty("siglaUf")
        private String siglaUf;

        @JsonProperty("codigoPais")
        private Integer codigoPais;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Cobranca {
        @JsonProperty("nome")
        private String nome;

        @JsonProperty("cgc")
        private String cgc;

        @JsonProperty("endereco")
        private Endereco endereco;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Contato {
        @JsonProperty("cargo")
        private String cargo;

        @JsonProperty("nome")
        private String nome;

        @JsonProperty("email")
        private String email;

        @JsonProperty("idCorretor")
        private String idCorretor;

        @JsonProperty("telefone")
        private Telefone telefone;

        @JsonProperty("listaTipoComunicacao")
        private List<TipoComunicacao> listaTipoComunicacao;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Telefone {
        @JsonProperty("telefone1")
        private String telefone1;

        @JsonProperty("celular")
        private String celular;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TipoComunicacao {
        @JsonProperty("id")
        private String id;

        @JsonProperty("descricao")
        private String descricao;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Comissionamento {
        @JsonProperty("cnpjCorretor")
        private String cnpjCorretor;

        @JsonProperty("codigoRegra")
        private Integer codigoRegra;

        @JsonProperty("numeroParcelaDe")
        private Integer numeroParcelaDe;

        @JsonProperty("numeroParcelaAte")
        private Integer numeroParcelaAte;

        @JsonProperty("porcentagem")
        private Integer porcentagem;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Grupo {
        @JsonProperty("codigoGrupo")
        private Integer codigoGrupo;
    }
}
