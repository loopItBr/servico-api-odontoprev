package com.odontoPrev.odontoPrev.infrastructure.client.adapter.out.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * DTO para request do endpoint PME (/empresa/2.0/empresas/pme)
 * 
 * Este DTO representa os dados necessários para cadastrar uma empresa
 * no endpoint PME da OdontoPrev após o sucesso da inclusão empresarial.
 * 
 * CAMPOS OBRIGATÓRIOS:
 * - sistema: "SabinSinai" (fixo)
 * - tipoPessoa: "J" (fixo para pessoa jurídica)
 * - cgc: CNPJ da empresa
 * - razaoSocial: Razão social da empresa
 * - nomeFantasia: Nome fantasia da empresa
 * - planos: Lista de planos da empresa
 * - contatos: Lista de contatos
 * - contatosDaFatura: Lista de contatos da fatura
 * - grupos: Lista de grupos
 * - endereco: Endereço da empresa
 * - cobranca: Dados de cobrança
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmpresaPmeRequest {

    @JsonProperty("sistema")
    private String sistema = "SabinSinai";

    @JsonProperty("tipoPessoa")
    private String tipoPessoa = "J";

    @JsonProperty("emiteCarteirinhaPlastica")
    private String emiteCarteirinhaPlastica = "N";

    @JsonProperty("codigoEmpresaGestora")
    private Integer codigoEmpresaGestora = 1;

    @JsonProperty("codigoFilialEmpresaGestora")
    private Integer codigoFilialEmpresaGestora = 1;

    @JsonProperty("dependentePaga")
    private String dependentePaga = "N";

    @JsonProperty("permissaoCadastroDep")
    private Boolean permissaoCadastroDep = true;

    @JsonProperty("modeloCobrancaVarejo")
    private Boolean modeloCobrancaVarejo = false;

    @JsonProperty("numeroMinimoAssociados")
    private Integer numeroMinimoAssociados = 3;

    @JsonProperty("numeroFuncionarios")
    private Integer numeroFuncionarios = 0;
    
    @JsonProperty("numeroDepedentes")
    private Integer numeroDepedentes = 0;

    @JsonProperty("idadeLimiteDependente")
    private Integer idadeLimiteDependente = 21;

    @JsonProperty("valorFator")
    private Integer valorFator = 1;

    @JsonProperty("tipoRetornoCritica")
    private String tipoRetornoCritica = "T";

    @JsonProperty("codigoLayoutCarteirinha")
    private String codigoLayoutCarteirinha = "B";

    @JsonProperty("codigoOrdemCarteira")
    private Integer codigoOrdemCarteira = 3;

    @JsonProperty("codigoDocumentoContrato")
    private Integer codigoDocumentoContrato = 0;

    @JsonProperty("codigoCelula")
    private Integer codigoCelula = 9;

    @JsonProperty("codigoMarca")
    private Integer codigoMarca = 1;

    @JsonProperty("codigoDescricaoNF")
    private Integer codigoDescricaoNF = 0;

    @JsonProperty("diaVencimentoAg")
    private Integer diaVencimentoAg = 19;

    @JsonProperty("codigoPerfilClienteFatura")
    private Integer codigoPerfilClienteFatura = 3;

    @JsonProperty("codigoBancoFatura")
    private String codigoBancoFatura = "085 ";

    @JsonProperty("multaFatura")
    private Integer multaFatura = 0;

    @JsonProperty("descontaIR")
    private String descontaIR = "N";

    @JsonProperty("retencaoIss")
    private String retencaoIss = "N";

    @JsonProperty("liberaSenhaInternet")
    private String liberaSenhaInternet = "S";

    @JsonProperty("faturamentoNotaCorte")
    private String faturamentoNotaCorte = "N";

    @JsonProperty("proRata")
    private String proRata = "N";

    @JsonProperty("custoFamiliar")
    private String custoFamiliar = "S";

    @JsonProperty("planoFamiliar")
    private String planoFamiliar = "S";

    @JsonProperty("percSinistroContrato")
    private Integer percSinistroContrato = 60;

    @JsonProperty("posicaoFimTIT")
    private Integer posicaoFimTIT = 18;

    @JsonProperty("idadeLimiteUniversitaria")
    private Integer idadeLimiteUniversitaria = 24;

    @JsonProperty("percentualINSSAutoGestao")
    private Integer percentualINSSAutoGestao = 0;

    @JsonProperty("percentualMateriaisAutoGestao")
    private Integer percentualMateriaisAutoGestao = 0;

    @JsonProperty("valorSinistroContrato")
    private Double valorSinistroContrato = 60.0;

    @JsonProperty("percentualAssociado")
    private Integer percentualAssociado = 0;

    @JsonProperty("codigoRegiao")
    private Integer codigoRegiao = 0;

    @JsonProperty("codigoImagemFatura")
    private Integer codigoImagemFatura = 1;

    @JsonProperty("codigoMoeda")
    private String codigoMoeda = "7";

    @JsonProperty("codigoParceriaEstrategica")
    private Integer codigoParceriaEstrategica = 0;

    @JsonProperty("sinistralidade")
    private Integer sinistralidade = 60;

    @JsonProperty("posicaoIniTIT")
    private Integer posicaoIniTIT = 0;

    @JsonProperty("regraDowngrade")
    private Integer regraDowngrade = 0;

    @JsonProperty("mesCompetenciaProximoFaturamento")
    private String mesCompetenciaProximoFaturamento = "09";

    @JsonProperty("codigoUsuarioFaturamento")
    private String codigoUsuarioFaturamento = "";

    @JsonProperty("codigoUsuarioCadastro")
    private String codigoUsuarioCadastro = "";

    @JsonProperty("ramo")
    private String ramo = "Massificado";

    // CAMPOS DINÂMICOS DA VIEW
    @JsonProperty("cgc")
    private String cgc;

    @JsonProperty("razaoSocial")
    private String razaoSocial;

    @JsonProperty("nomeFantasia")
    private String nomeFantasia;

    @JsonProperty("diaInicioFaturamento")
    private Integer diaInicioFaturamento = 20;

    @JsonProperty("codigoUsuarioConsultor")
    private String codigoUsuarioConsultor = "FEODPV01583";

    @JsonProperty("mesAniversarioReajuste")
    private Integer mesAniversarioReajuste = 7;

    @JsonProperty("dataInicioContrato")
    private String dataInicioContrato;

    @JsonProperty("dataVigencia")
    private String dataVigencia;

    @JsonProperty("descricaoRamoAtividade")
    private String descricaoRamoAtividade = "Saúde Suplementar";

    @JsonProperty("planos")
    private List<PlanoPme> planos;

    @JsonProperty("diaVencimento")
    private Integer diaVencimento = 15;

    @JsonProperty("cnae")
    private String cnae = "6550-2/00";

    @JsonProperty("codigoManual")
    private String codigoManual = "1 ";

    @JsonProperty("diaLimiteConsumoAg")
    private Integer diaLimiteConsumoAg = 19;

    @JsonProperty("grausParentesco")
    private List<GrauParentesco> grausParentesco;

    @JsonProperty("contatosDaFatura")
    private List<ContatoFatura> contatosDaFatura;

    @JsonProperty("email")
    private String email = "diretoria@sabinjf.com.br";

    @JsonProperty("endereco")
    private EnderecoPme endereco;

    @JsonProperty("cobranca")
    private CobrancaPme cobranca;

    @JsonProperty("diaMovAssociadoEmpresa")
    private Integer diaMovAssociadoEmpresa = 15;

    @JsonProperty("contatos")
    private List<ContatoPme> contatos;

    @JsonProperty("comissionamentos")
    private List<Comissionamento> comissionamentos;

    @JsonProperty("grupos")
    private List<GrupoPme> grupos;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PlanoPme {
        @JsonProperty("codigoPlano")
        private String codigoPlano;

        @JsonProperty("dataInicioPlano")
        private String dataInicioPlano;

        @JsonProperty("valorDependente")
        private Double valorDependente = 0.0;

        @JsonProperty("valorReembolsoUO")
        private Double valorReembolsoUO = 0.0;

        @JsonProperty("valorTitular")
        private Double valorTitular = 0.0;

        @JsonProperty("periodicidade")
        private String periodicidade = "N";

        @JsonProperty("percentualAssociado")
        private Double percentualAssociado = 0.0;

        @JsonProperty("percentualDependenteRedeGenerica")
        private Double percentualDependenteRedeGenerica = 0.0;

        @JsonProperty("percentualAgregadoRedeGenerica")
        private Double percentualAgregadoRedeGenerica = 0.0;

        @JsonProperty("redes")
        private List<RedePme> redes;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RedePme {
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
    public static class EnderecoPme {
        @JsonProperty("cep")
        private String cep = "36033318";

        @JsonProperty("descricao")
        private String descricao = "Av. Presidente Itamar Franco";

        @JsonProperty("complemento")
        private String complemento = "loja 202 E";

        @JsonProperty("tipoLogradouro")
        private String tipoLogradouro = "2";

        @JsonProperty("logradouro")
        private String logradouro = "Av. Presidente Itamar Franco";

        @JsonProperty("numero")
        private String numero = "4001";

        @JsonProperty("bairro")
        private String bairro = "Cascatinha";

        @JsonProperty("cidade")
        private CidadePme cidade;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CidadePme {
        @JsonProperty("codigo")
        private Integer codigo = 3670;

        @JsonProperty("nome")
        private String nome = "Juiz de Fora";

        @JsonProperty("siglaUf")
        private String siglaUf = "MG";

        @JsonProperty("codigoPais")
        private Integer codigoPais = 1;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CobrancaPme {
        @JsonProperty("nome")
        private String nome;

        @JsonProperty("cgc")
        private String cgc;

        @JsonProperty("endereco")
        private EnderecoPme endereco;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ContatoPme {
        @JsonProperty("cargo")
        private String cargo;

        @JsonProperty("nome")
        private String nome;

        @JsonProperty("email")
        private String email;

        @JsonProperty("idCorretor")
        private String idCorretor;

        @JsonProperty("telefone")
        private TelefonePme telefone;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TelefonePme {
        @JsonProperty("telefone1")
        private String telefone1;

        @JsonProperty("celular")
        private String celular;
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
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class GrupoPme {
        @JsonProperty("codigoGrupo")
        private Integer codigoGrupo;
    }
}
