package com.odontoPrev.odontoPrev.infrastructure.client.adapter.out.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO PARA ALTERAÇÃO DE BENEFICIÁRIO NA ODONTOPREV
 *
 * Esta classe contém os dados para atualizar informações de um beneficiário
 * já existente no sistema da OdontoPrev.
 *
 * QUANDO É USADA:
 * - Ao enviar requisição PUT para o endpoint /alterar da OdontoPrev
 * - Quando dados cadastrais do beneficiário são alterados no Tasy
 *
 * CAMPOS OBRIGATÓRIOS:
 * - cdEmpresa, cdAssociado, codigoPlano, departamento
 *
 * OBSERVAÇÃO:
 * Apenas beneficiários com alterações do dia corrente são enviados
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BeneficiarioAlteracaoRequest {

    @NotBlank(message = "Código da empresa é obrigatório")
    @JsonProperty("cdEmpresa")
    private String cdEmpresa;

    @NotBlank(message = "Código do associado é obrigatório")
    @JsonProperty("cdAssociado")
    private String cdAssociado;

    @NotBlank(message = "Código do plano é obrigatório")
    @JsonProperty("codigoPlano")
    private String codigoPlano;

    @NotBlank(message = "Departamento é obrigatório")
    @JsonProperty("departamento")
    private String departamento;

    // Campos opcionais que podem ser alterados
    @JsonProperty("dtVigenciaRetroativa")
    private String dtVigenciaRetroativa;

    @JsonProperty("IDENTIFICACAO")
    private String identificacao;

    @JsonProperty("dataNascimento")
    private String dataNascimento;

    @JsonProperty("motivoExclusao")
    private String motivoExclusao;

    @JsonProperty("telefoneCelular")
    private String telefoneCelular;

    @JsonProperty("telefoneResidencial")
    private String telefoneResidencial;

    @JsonProperty("rg")
    private String rg;

    @JsonProperty("estadoCivil")
    private String estadoCivil;

    @JsonProperty("nmCargo")
    private String nmCargo;

    @JsonProperty("ACAO")
    private String acao;

    @JsonProperty("CPF")
    private String cpf;

    @JsonProperty("SEXO")
    private String sexo;

    @JsonProperty("nomedaMae")
    private String nomeDaMae;

    @JsonProperty("pisPasep")
    private String pisPasep;

    @JsonProperty("BAIRRO")
    private String bairro;

    @JsonProperty("EMAIL")
    private String email;

    @JsonProperty("TIPO_EXCLUSAO")
    private String tipoExclusao;

    @JsonProperty("nrBanco")
    private String nrBanco;

    @JsonProperty("nrAgencia")
    private String nrAgencia;

    @JsonProperty("nrConta")
    private String nrConta;

    @JsonProperty("digConta")
    private String digConta;

    @JsonProperty("digAgencia")
    private String digAgencia;

    @JsonProperty("END_RESIDENCIAL")
    private String endResidencial;

    @JsonProperty("rgEmissor")
    private String rgEmissor;

    @JsonProperty("nomeBeneficiario")
    private String nomeBeneficiario;

    @JsonProperty("PLANO")
    private String plano;

    @JsonProperty("CNS")
    private String cns;

    @JsonProperty("COD_PAIS_EMISSOR")
    private String codPaisEmissor;

    @JsonProperty("COD_MUNICIPIO_IBGE")
    private String codMunicipioIbge;

    @JsonProperty("IND_RESIDENCIA")
    private String indResidencia;

    @JsonProperty("NR_LOGRADOURO")
    private String nrLogradouro;

    @JsonProperty("TIPO_ENDERECO")
    private String tipoEndereco;

    @JsonProperty("CIDADE_RESIDENCIA")
    private String cidadeResidencia;

    @JsonProperty("CEP")
    private String cep;

    @JsonProperty("CIDADE")
    private String cidade;

    @JsonProperty("LOGRADOURO")
    private String logradouro;

    @JsonProperty("NUMERO")
    private String numero;

    @JsonProperty("Complemento")
    private String complemento;

    @JsonProperty("UF")
    private String uf;
}
