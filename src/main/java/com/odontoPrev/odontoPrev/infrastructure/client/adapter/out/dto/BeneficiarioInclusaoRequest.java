package com.odontoPrev.odontoPrev.infrastructure.client.adapter.out.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO PARA INCLUSÃO DE BENEFICIÁRIO NA ODONTOPREV
 *
 * Esta classe contém todos os dados necessários para cadastrar um novo
 * beneficiário no sistema da OdontoPrev através da API.
 *
 * QUANDO É USADA:
 * - Ao enviar requisição POST para o endpoint /incluir da OdontoPrev
 * - Quando um novo beneficiário é cadastrado no Tasy e precisa ser sincronizado
 *
 * CAMPOS OBRIGATÓRIOS (conforme documentação):
 * - codigoMatricula, CPF, dataNascimento, dtVigenciaRetroativa
 * - CEP, CIDADE, LOGRADOURO, NUMERO, UF
 * - nomeBeneficiario, nomeDaMae, SEXO
 * - telefoneCelular, telefoneResidencial
 * - USUARIO, codigoEmpresa, codigoPlano, departamento
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BeneficiarioInclusaoRequest {

    @NotBlank(message = "Código da matrícula é obrigatório")
    @JsonProperty("codigoMatricula")
    private String codigoMatricula;

    @NotBlank(message = "CPF é obrigatório")
    @JsonProperty("CPF")
    private String cpf;

    @NotBlank(message = "Data de nascimento é obrigatória")
    @JsonProperty("dataNascimento")
    private String dataNascimento;

    @NotBlank(message = "Data de vigência retroativa é obrigatória")
    @JsonProperty("dtVigenciaRetroativa")
    private String dtVigenciaRetroativa;

    @NotBlank(message = "CEP é obrigatório")
    @JsonProperty("CEP")
    private String cep;

    @NotBlank(message = "Cidade é obrigatória")
    @JsonProperty("CIDADE")
    private String cidade;

    @NotBlank(message = "Logradouro é obrigatório")
    @JsonProperty("LOGRADOURO")
    private String logradouro;

    @NotBlank(message = "Número é obrigatório")
    @JsonProperty("NUMERO")
    private String numero;

    @NotBlank(message = "UF é obrigatória")
    @JsonProperty("UF")
    private String uf;

    @NotBlank(message = "Nome do beneficiário é obrigatório")
    @JsonProperty("nomeBeneficiario")
    private String nomeBeneficiario;

    @NotBlank(message = "Nome da mãe é obrigatório")
    @JsonProperty("nomeDaMae")
    private String nomeDaMae;

    @NotBlank(message = "Sexo é obrigatório")
    @JsonProperty("SEXO")
    private String sexo;

    @NotBlank(message = "Telefone celular é obrigatório")
    @JsonProperty("telefoneCelular")
    private String telefoneCelular;

    @NotBlank(message = "Telefone residencial é obrigatório")
    @JsonProperty("telefoneResidencial")
    private String telefoneResidencial;

    @NotBlank(message = "Usuário é obrigatório")
    @JsonProperty("USUARIO")
    private String usuario;

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
    @JsonProperty("IDENTIFICACAO")
    private String identificacao;

    @JsonProperty("RG")
    private String rg;

    @JsonProperty("estadoCivil")
    private String estadoCivil;

    @JsonProperty("nmCargo")
    private String nmCargo;

    @JsonProperty("grauParentesco")
    private String grauParentesco;

    @JsonProperty("ACAO")
    private String acao;

    @JsonProperty("PIS_PASEP")
    private String pisPasep;

    @JsonProperty("DATA_ASSOCIACAO")
    private String dataAssociacao;

    @JsonProperty("BAIRRO")
    private String bairro;

    @JsonProperty("EMAIL")
    private String email;

    @JsonProperty("motivoExclusao")
    private String motivoExclusao;

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

    @JsonProperty("tipoConta")
    private String tipoConta;

    @JsonProperty("complemento")
    private String complemento;

    @JsonProperty("rgEmissor")
    private String rgEmissor;

    @JsonProperty("CNS")
    private String cns;

    @JsonProperty("COD_PAIS_EMISSOR")
    private String codPaisEmissor;

    @JsonProperty("COD_MUNICIPIO_IBGE")
    private String codMunicipioIbge;

    @JsonProperty("IND_RESIDENCIA")
    private String indResidencia;

    @JsonProperty("tpEndereco")
    private String tpEndereco;

    @JsonProperty("CIDADE_RESIDENCIA")
    private String cidadeResidencia;

    @JsonProperty("DNV")
    private String dnv;

    @JsonProperty("NUM_PORTABILIDADE")
    private String numPortabilidade;

    @JsonProperty("TEMPO_CONTRIBUICAO")
    private String tempoContribuicao;

    @JsonProperty("DIR_PERMANENCIA")
    private String dirPermanencia;
}
