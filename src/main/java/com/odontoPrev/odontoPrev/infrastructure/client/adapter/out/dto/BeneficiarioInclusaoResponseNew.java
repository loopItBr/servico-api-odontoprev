package com.odontoPrev.odontoPrev.infrastructure.client.adapter.out.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * DTO PARA RESPOSTA DE INCLUSÃO DE BENEFICIÁRIO NA ODONTOPREV - NOVA API
 *
 * Esta classe representa a resposta da API de inclusão de beneficiários
 * conforme a nova documentação da OdontoPrev.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BeneficiarioInclusaoResponseNew {

    @JsonProperty("status")
    private Integer status;

    @JsonProperty("cdMsg")
    private Integer cdMsg;

    @JsonProperty("beneficiarios")
    private BeneficiarioInfo beneficiarios;

    @JsonProperty("listaBeneficiarios")
    private List<BeneficiarioInfo> listaBeneficiarios;

    @JsonProperty("protocolo")
    private String protocolo;

    @JsonProperty("crm")
    private Crm crm;

    @JsonProperty("mensagem")
    private String mensagem;

    @JsonProperty("guidProtocolo")
    private String guidProtocolo;

    @JsonProperty("dadosAlteracao")
    private List<DadosAlteracao> dadosAlteracao;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BeneficiarioInfo {
        @JsonProperty("codigoMatricula")
        private String codigoMatricula;

        @JsonProperty("codigoAssociado")
        private String codigoAssociado;

        @JsonProperty("nomeAssociado")
        private String nomeAssociado;

        @JsonProperty("motivoInativacao")
        private String motivoInativacao;

        @JsonProperty("idMotivo")
        private String idMotivo;

        @JsonProperty("email")
        private String email;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Crm {
        @JsonProperty("status")
        private Integer status;

        @JsonProperty("mensagem")
        private String mensagem;

        @JsonProperty("redirect")
        private Boolean redirect;

        @JsonProperty("protocolo")
        private String protocolo;

        @JsonProperty("ocorrencia")
        private String ocorrencia;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DadosAlteracao {
        @JsonProperty("nmrImportacao")
        private String nmrImportacao;

        @JsonProperty("codigoAssociado")
        private String codigoAssociado;
    }
}
