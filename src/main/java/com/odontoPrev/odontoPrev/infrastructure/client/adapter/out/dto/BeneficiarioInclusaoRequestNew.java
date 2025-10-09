package com.odontoPrev.odontoPrev.infrastructure.client.adapter.out.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO PARA INCLUSÃO DE BENEFICIÁRIO NA ODONTOPREV - NOVA API
 *
 * Esta classe contém todos os dados necessários para cadastrar um novo
 * beneficiário no sistema da OdontoPrev através da nova API /cadastroonline-pj/1.0.
 *
 * QUANDO É USADA:
 * - Ao enviar requisição POST para o endpoint /incluir da OdontoPrev
 * - Quando um novo beneficiário é cadastrado no Tasy e precisa ser sincronizado
 *
 * ESTRUTURA CONFORME DOCUMENTAÇÃO:
 * - beneficiarioTitular: Objeto contendo dados do beneficiário
 * - usuario: Usuário que realizou a movimentação
 * - venda: Dados da venda/empresa
 * - dadosBancarios: Dados bancários (opcional)
 * - protocolo: Dados do protocolo CRM (opcional)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BeneficiarioInclusaoRequestNew {

    @NotNull(message = "Beneficiário titular é obrigatório")
    @JsonProperty("beneficiarioTitular")
    private BeneficiarioTitular beneficiarioTitular;

    @NotBlank(message = "Usuário é obrigatório")
    @JsonProperty("usuario")
    private String usuario;

    @NotNull(message = "Venda é obrigatória")
    @JsonProperty("venda")
    private Venda venda;

    @JsonProperty("dadosBancarios")
    private DadosBancarios dadosBancarios;

    @JsonProperty("protocolo")
    private Protocolo protocolo;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BeneficiarioTitular {
        @NotNull(message = "Beneficiário é obrigatório")
        @JsonProperty("beneficiario")
        private Beneficiario beneficiario;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Beneficiario {
        @NotBlank(message = "Código da matrícula é obrigatório")
        @JsonProperty("codigoMatricula")
        private String codigoMatricula;

        @NotBlank(message = "Código do plano é obrigatório")
        @JsonProperty("codigoPlano")
        private String codigoPlano;

        @NotBlank(message = "CPF é obrigatório")
        @JsonProperty("cpf")
        private String cpf;

        @NotBlank(message = "Data de nascimento é obrigatória")
        @JsonProperty("dataDeNascimento")
        private String dataDeNascimento;

        @NotBlank(message = "Data de vigência retroativa é obrigatória")
        @JsonProperty("dtVigenciaRetroativa")
        private String dtVigenciaRetroativa;

        @NotBlank(message = "Nome do beneficiário é obrigatório")
        @JsonProperty("nomeBeneficiario")
        private String nomeBeneficiario;

        @NotBlank(message = "Nome da mãe é obrigatório")
        @JsonProperty("nomeDaMae")
        private String nomeDaMae;

        @NotBlank(message = "Sexo é obrigatório")
        @JsonProperty("sexo")
        private String sexo;

        @NotBlank(message = "Telefone celular é obrigatório")
        @JsonProperty("telefoneCelular")
        private String telefoneCelular;

        @NotNull(message = "Endereço é obrigatório")
        @JsonProperty("endereco")
        private Endereco endereco;

        // Campos opcionais
        @JsonProperty("beneficiarioTitular")
        private String beneficiarioTitular;

        @JsonProperty("campanha")
        private String campanha;

        @JsonProperty("email")
        private String email;

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

        @JsonProperty("pisPasep")
        private String pisPasep;

        @JsonProperty("rg")
        private String rg;

        @JsonProperty("rgEmissor")
        private String rgEmissor;

        @JsonProperty("cns")
        private String cns;

        @JsonProperty("telefoneComercial")
        private String telefoneComercial;

        @JsonProperty("telefoneResidencial")
        private String telefoneResidencial;

        @JsonProperty("departamento")
        private String departamento;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Endereco {
        @NotBlank(message = "CEP é obrigatório")
        @JsonProperty("cep")
        private String cep;

        @NotBlank(message = "Cidade é obrigatória")
        @JsonProperty("cidade")
        private String cidade;

        @NotBlank(message = "Logradouro é obrigatório")
        @JsonProperty("logradouro")
        private String logradouro;

        @NotBlank(message = "Número é obrigatório")
        @JsonProperty("numero")
        private String numero;

        @NotBlank(message = "UF é obrigatória")
        @JsonProperty("uf")
        private String uf;

        // Campos opcionais
        @JsonProperty("bairro")
        private String bairro;

        @JsonProperty("cidadeBeneficiario")
        private String cidadeBeneficiario;

        @JsonProperty("complemento")
        private String complemento;

        @JsonProperty("tpEndereco")
        private String tpEndereco;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Venda {
        @NotBlank(message = "Código da empresa é obrigatório")
        @JsonProperty("codigoEmpresa")
        private String codigoEmpresa;

        @NotBlank(message = "Código do plano é obrigatório")
        @JsonProperty("codigoPlano")
        private String codigoPlano;

        @NotBlank(message = "Departamento é obrigatório")
        @JsonProperty("departamento")
        private String departamento;

        // Campos opcionais
        @JsonProperty("enviarKit")
        private String enviarKit;

        @JsonProperty("segmentacao")
        private String segmentacao;

        @JsonProperty("subsegmentacao")
        private String subsegmentacao;
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

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Protocolo {
        @JsonProperty("codEmpresa")
        private String codEmpresa;

        @JsonProperty("emailResposavel")
        private String emailResposavel;

        @JsonProperty("foneResponsavel")
        private String foneResponsavel;

        @JsonProperty("nomeResponsavel")
        private String nomeResponsavel;

        @JsonProperty("protocolo")
        private String protocolo;
    }
}
