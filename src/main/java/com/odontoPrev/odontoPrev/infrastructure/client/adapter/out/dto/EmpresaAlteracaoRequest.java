package com.odontoPrev.odontoPrev.infrastructure.client.adapter.out.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * DTO PARA ALTERAÇÃO DE EMPRESA NA API ODONTOPREV
 *
 * Este DTO representa o payload necessário para alterar dados de uma empresa
 * através do endpoint PUT /empresas/alterar da API OdontoPrev.
 *
 * CAMPOS OBRIGATÓRIOS:
 * - codigoEmpresa: Código da empresa (6 caracteres)
 * - endereco: Objeto com dados do endereço
 * - codigoUsuario: Código do usuário (0)
 *
 * ESTRUTURA HIERÁRQUICA:
 * - Telefone: Objeto com telefones da empresa
 * - Endereco: Objeto com dados completos do endereço
 * - Cidade: Objeto dentro do endereço
 * - GrausParentesco: Lista de graus de parentesco permitidos
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmpresaAlteracaoRequest {

    /**
     * CÓDIGO DA EMPRESA (OBRIGATÓRIO)
     * 6 caracteres - Login da empresa
     */
    @JsonProperty("codigoEmpresa")
    private String codigoEmpresa;

    /**
     * NOME FANTASIA
     * Até 20 caracteres
     */
    @JsonProperty("nomeFantasia")
    private String nomeFantasia;

    /**
     * RAZÃO SOCIAL
     * Até 45 caracteres
     */
    @JsonProperty("razaoSocial")
    private String razaoSocial;

    /**
     * EMITE CARTEIRINHA PLÁSTICA
     * S (Sim) ou N (Não)
     */
    @JsonProperty("emiteCarteirinhaPlastica")
    private String emiteCarteirinhaPlastica;

    /**
     * PERMISSÃO CADASTRO DEPENDENTES
     * S (Sim) ou N (Não)
     */
    @JsonProperty("permissaoCadastroDep")
    private String permissaoCadastroDep;

    /**
     * DESCRIÇÃO RAMO ATIVIDADE
     * Até 200 caracteres
     */
    @JsonProperty("descricaoRamoAtividade")
    private String descricaoRamoAtividade;

    /**
     * RAMO
     * 3 caracteres - Ver Código Ramo em Enums
     */
    @JsonProperty("ramo")
    private Integer ramo;

    /**
     * NÚMERO DE FUNCIONÁRIOS
     * Numérico
     */
    @JsonProperty("numeroFuncionarios")
    private Integer numeroFuncionarios;

    /**
     * VALOR FATOR
     * Numérico - Campo não utilizado
     */
    @JsonProperty("valorFator")
    private Double valorFator;

    /**
     * CNAE
     * Até 10 caracteres
     */
    @JsonProperty("cnae")
    private String cnae;

    /**
     * CÓDIGO LAYOUT CARTEIRINHA
     * 1 carácter - Ver Código Layout Carteirinha Enum
     */
    @JsonProperty("codigoLayoutCarteirinha")
    private String codigoLayoutCarteirinha;

    /**
     * CÓDIGO ORDEM CARTEIRA
     * Numérico - Ver Código Ordem Carteira Enum
     */
    @JsonProperty("codigoOrdemCarteira")
    private Integer codigoOrdemCarteira;

    /**
     * LIBERA SENHA INTERNET
     * S (Sim) ou N (Não)
     */
    @JsonProperty("liberaSenhaInternet")
    private String liberaSenhaInternet;

    /**
     * DEPENDENTE PAGA
     * S (Sim) ou N (Não)
     */
    @JsonProperty("dependentePaga")
    private String dependentePaga;

    /**
     * CUSTO FAMILIAR
     * S (Sim) ou N (Não)
     */
    @JsonProperty("custoFamiliar")
    private String custoFamiliar;

    /**
     * PLANO FAMILIAR
     * S (Sim) ou N (Não)
     */
    @JsonProperty("planoFamiliar")
    private String planoFamiliar;

    /**
     * IDADE LIMITE UNIVERSITÁRIA
     * Numérico
     */
    @JsonProperty("idadeLimiteUniversitaria")
    private Integer idadeLimiteUniversitaria;

    /**
     * CÓDIGO REGIÃO
     * Numérico - Ver Código Região em Enums
     */
    @JsonProperty("codigoRegiao")
    private Integer codigoRegiao;

    /**
     * NÚMERO CEI
     * Até 15 caracteres
     */
    @JsonProperty("numeroCei")
    private String numeroCei;

    /**
     * CIC
     * Até 14 caracteres
     */
    @JsonProperty("cic")
    private String cic;

    /**
     * INSCRIÇÃO MUNICIPAL
     * Até 19 caracteres
     */
    @JsonProperty("inscricaoMunicipal")
    private String inscricaoMunicipal;

    /**
     * INSCRIÇÃO ESTADUAL
     * Até 19 caracteres
     */
    @JsonProperty("inscricaoEstadual")
    private String inscricaoEstadual;

    /**
     * TELEFONE
     * Objeto com dados de telefone
     */
    @JsonProperty("telefone")
    private Telefone telefone;

    /**
     * EMAIL
     * Até 50 caracteres
     */
    @JsonProperty("email")
    private String email;

    /**
     * ENDEREÇO (OBRIGATÓRIO)
     * Objeto com dados do endereço
     */
    @JsonProperty("endereco")
    private Endereco endereco;

    /**
     * CÓDIGO NATUREZA JURÍDICA
     * Até 20 caracteres
     */
    @JsonProperty("codigoNaturezaJuridica")
    private String codigoNaturezaJuridica;

    /**
     * NOME NATUREZA JURÍDICA
     * Até 255 caracteres
     */
    @JsonProperty("nomeNaturezaJuridica")
    private String nomeNaturezaJuridica;

    /**
     * SITUAÇÃO CADASTRAL
     * ATIVO ou INATIVO
     */
    @JsonProperty("situacaoCadastral")
    private String situacaoCadastral;

    /**
     * DATA CONSTITUIÇÃO
     * YYYY-MM-DDTHH:mm:ss.sssZ
     */
    @JsonProperty("dataConstituicao")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
    private LocalDateTime dataConstituicao;

    /**
     * RENOVAÇÃO AUTOMÁTICA
     * S (Sim) ou N (Não)
     */
    @JsonProperty("renovacaoAutomatica")
    private String renovacaoAutomatica;

    /**
     * DATA VIGÊNCIA
     * YYYY-MM-DDTHH:mm:ss.sssZ
     */
    @JsonProperty("dataVigencia")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
    private LocalDateTime dataVigencia;

    /**
     * MÊS ANIVERSÁRIO REAJUSTE
     * Numérico
     */
    @JsonProperty("mesAniversarioReajuste")
    private Integer mesAniversarioReajuste;

    /**
     * ANO PRÓXIMO ANIVERSÁRIO REAJUSTE
     * Numérico - Campo não utilizado
     */
    @JsonProperty("anoProximoAniversarioReajuste")
    private Integer anoProximoAniversarioReajuste;

    /**
     * SINISTRALIDADE
     * Numérico
     */
    @JsonProperty("sinistralidade")
    private Double sinistralidade;

    /**
     * CÓDIGO USUÁRIO (OBRIGATÓRIO)
     * 0 - Código de usuário de alteração
     */
    @JsonProperty("codigoUsuario")
    private String codigoUsuario;

    /**
     * SISTEMA
     * Até 30 caracteres
     */
    @JsonProperty("sistema")
    private String sistema;

    /**
     * DIA VENCIMENTO PLANO
     * Numérico
     */
    @JsonProperty("diaVencimentoPlano")
    private Integer diaVencimentoPlano;

    /**
     * DIA MOVIMENTAÇÃO CADASTRAL
     * Numérico
     */
    @JsonProperty("diaMovimentacaoCadastral")
    private Integer diaMovimentacaoCadastral;

    /**
     * CÓDIGO GRUPO GERENCIAL
     * 6 caracteres
     */
    @JsonProperty("codigoGrupoGerencial")
    private String codigoGrupoGerencial;

    /**
     * GRAUS PARENTESCO
     * Lista de graus de parentesco permitidos
     */
    @JsonProperty("grausParentesco")
    private List<GrauParentesco> grausParentesco;

    /**
     * DTO PARA TELEFONE DA EMPRESA
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Telefone {
        @JsonProperty("telefone1")
        private String telefone1;

        @JsonProperty("telefone2")
        private String telefone2;

        @JsonProperty("celular")
        private String celular;

        @JsonProperty("fax")
        private String fax;
    }

    /**
     * DTO PARA ENDEREÇO DA EMPRESA
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Endereco {
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

        @JsonProperty("cep")
        private String cep;
    }

    /**
     * DTO PARA CIDADE
     */
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

    /**
     * DTO PARA GRAU DE PARENTESCO
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class GrauParentesco {
        @JsonProperty("codigoGrauParentesco")
        private Integer codigoGrauParentesco;
    }
}
