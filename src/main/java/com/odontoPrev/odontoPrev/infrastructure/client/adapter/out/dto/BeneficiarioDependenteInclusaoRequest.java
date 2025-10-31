package com.odontoPrev.odontoPrev.infrastructure.client.adapter.out.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * DTO PARA INCLUSÃO DE DEPENDENTE NA ODONTOPREV
 *
 * Esta classe contém todos os dados necessários para cadastrar um novo
 * dependente no sistema da OdontoPrev através do endpoint /incluirDependente.
 *
 * REQUISITO IMPORTANTE:
 * O titular já deve existir na OdontoPrev antes de incluir o dependente.
 *
 * ENDPOINT: POST {{baseUrl}}/incluirDependente
 *
 * CAMPOS OBRIGATÓRIOS:
 * - codigoAssociadoTitular: Código do associado titular (9 caracteres)
 * - usuario: Usuário de movimentação (até 15 caracteres)
 * - cdUsuario: Código do usuário (até 15 caracteres)
 * - beneficiarios: Lista de dependentes a serem incluídos
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BeneficiarioDependenteInclusaoRequest {

    @NotBlank(message = "Código do associado titular é obrigatório")
    @JsonProperty("codigoAssociadoTitular")
    private String codigoAssociadoTitular;

    @NotBlank(message = "Usuário é obrigatório")
    @JsonProperty("usuario")
    private String usuario;

    @JsonProperty("beneficiarios")
    private List<BeneficiarioDependente> beneficiarios;

    @NotBlank(message = "Código do usuário é obrigatório")
    @JsonProperty("cdUsuario")
    private String cdUsuario;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BeneficiarioDependente {
        @NotNull(message = "Beneficiário é obrigatório")
        @JsonProperty("beneficiario")
        private Beneficiario beneficiario;

        @NotBlank(message = "Código da empresa é obrigatório")
        @JsonProperty("codigoEmpresa")
        private String codigoEmpresa;

        @JsonProperty("departamento")
        private String departamento;

        @JsonProperty("parentesco")
        private Integer parentesco;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Beneficiario {
        @JsonProperty("beneficiarioTitular")
        private String beneficiarioTitular;

        @JsonProperty("campanha")
        private String campanha;

        @NotBlank(message = "Código da matrícula é obrigatório")
        @JsonProperty("codigoMatricula")
        private String codigoMatricula;

        @JsonProperty("codigoPlano")
        private String codigoPlano;

        @NotBlank(message = "CPF é obrigatório")
        @JsonProperty("cpf")
        private String cpf;

        @NotBlank(message = "Data de nascimento é obrigatória")
        @JsonProperty("dataDeNascimento")
        private String dataDeNascimento;

        @JsonProperty("dtVigenciaRetroativa")
        private String dtVigenciaRetroativa;

        @JsonProperty("email")
        private String email;

        @JsonProperty("empresaNova")
        private String empresaNova;

        @JsonProperty("endereco")
        private Endereco endereco;

        @JsonProperty("estadoCivil")
        private String estadoCivil;

        @JsonProperty("grauParentesco")
        private String grauParentesco;

        @JsonProperty("identificacao")
        private String identificacao; // "D" para dependente (opcional)

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
}

