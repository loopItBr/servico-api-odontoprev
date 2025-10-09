package com.odontoPrev.odontoPrev.infrastructure.client.adapter.out.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO PARA ALTERAÇÃO DE BENEFICIÁRIO NA ODONTOPREV - NOVA API
 *
 * Esta classe contém os dados necessários para alterar um beneficiário
 * existente no sistema da OdontoPrev através da nova API /cadastroonline-pj/1.0.
 *
 * QUANDO É USADA:
 * - Ao enviar requisição PUT para o endpoint /alterar da OdontoPrev
 * - Quando dados de um beneficiário são alterados no Tasy
 *
 * ESTRUTURA CONFORME DOCUMENTAÇÃO:
 * - cdEmpresa: Código da empresa (obrigatório)
 * - codigoAssociado: Carteirinha do beneficiário (obrigatório)
 * - codigoPlano: Código do plano (obrigatório)
 * - departamento: Código do departamento (obrigatório)
 * - beneficiario: Dados do beneficiário (opcional)
 * - dadosBancarios: Dados bancários (opcional)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BeneficiarioAlteracaoRequestNew {

    @NotBlank(message = "Código da empresa é obrigatório")
    @JsonProperty("cdEmpresa")
    private String cdEmpresa;

    @NotBlank(message = "Código do associado é obrigatório")
    @JsonProperty("codigoAssociado")
    private String codigoAssociado;

    @NotBlank(message = "Código do plano é obrigatório")
    @JsonProperty("codigoPlano")
    private String codigoPlano;

    @NotBlank(message = "Departamento é obrigatório")
    @JsonProperty("departamento")
    private String departamento;

    @JsonProperty("beneficiario")
    private Beneficiario beneficiario;

    @JsonProperty("dadosBancarios")
    private DadosBancarios dadosBancarios;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Beneficiario {
        @JsonProperty("beneficiarioTitular")
        private String beneficiarioTitular;

        @JsonProperty("campanha")
        private String campanha;

        @JsonProperty("codigoMatricula")
        private String codigoMatricula;

        @JsonProperty("codigoPlano")
        private String codigoPlano;

        @JsonProperty("cpf")
        private String cpf;

        @JsonProperty("dataDeNascimento")
        private String dataDeNascimento;

        @JsonProperty("dtVigenciaRetroativa")
        private String dtVigenciaRetroativa;

        @JsonProperty("email")
        private String email;

        @JsonProperty("endereco")
        private Endereco endereco;

        @JsonProperty("empresaNova")
        private String empresaNova;

        @JsonProperty("estadoCivil")
        private String estadoCivil;

        @JsonProperty("grauParentesco")
        private String grauParentesco;

        @JsonProperty("identificacao")
        private String identificacao;

        @JsonProperty("motivoExclusao")
        private String motivoExclusao;

        @JsonProperty("nmCargo")
        private String nmCargo;

        @JsonProperty("nomeBeneficiario")
        private String nomeBeneficiario;

        @JsonProperty("nomeDaMae")
        private String nomeDaMae;

        @JsonProperty("pisPasep")
        private String pisPasep;

        @JsonProperty("rg")
        private String rg;

        @JsonProperty("rgEmissor")
        private String rgEmissor;

        @JsonProperty("sexo")
        private String sexo;

        @JsonProperty("telefoneCelular")
        private String telefoneCelular;

        @JsonProperty("telefoneComercial")
        private String telefoneComercial;

        @JsonProperty("telefoneResidencial")
        private String telefoneResidencial;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Endereco {
        @JsonProperty("bairro")
        private String bairro;

        @JsonProperty("cep")
        private String cep;

        @JsonProperty("cidade")
        private String cidade;

        @JsonProperty("cidadeBeneficiario")
        private String cidadeBeneficiario;

        @JsonProperty("complemento")
        private String complemento;

        @JsonProperty("logradouro")
        private String logradouro;

        @JsonProperty("numero")
        private String numero;

        @JsonProperty("tpEndereco")
        private String tpEndereco;

        @JsonProperty("uf")
        private String uf;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DadosBancarios {
        @JsonProperty("digAgencia")
        private String digAgencia;

        @JsonProperty("digConta")
        private String digConta;

        @JsonProperty("nrAgencia")
        private String nrAgencia;

        @JsonProperty("nrBanco")
        private String nrBanco;

        @JsonProperty("nrConta")
        private String nrConta;

        @JsonProperty("tipoConta")
        private String tipoConta;
    }
}
