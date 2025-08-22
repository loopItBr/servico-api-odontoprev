package com.odontoPrev.odontoPrev.application.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class DadosBeneficiarioResponse {
    private DadosTitular dadosTitular;
    private Endereco endereco;
    private Contato contato;
    private DadosPlano dadosPlano;

    @Data
    public static class DadosTitular {
        private String cdAssociado;
        private String nrCpf;
        private String nmAssociado;
        private String dtNascimento;
        private String nmMaeAssociado;
    }

    @Data
    public static class Endereco {
        private String nrCep;
        private String dsEndereco;
        private String dsNumero;
        private String nmBairro;
        private String sgUf;
        private String nmCidade;
    }

    @Data
    public static class Contato {
        private String dsEmail;
        private String nmTelCelular;
    }

    @Data
    public static class DadosPlano {
        private String cdPlano;
        private String nmFantasiaPlano;
        @JsonProperty("flagPlanoFamiliar")
        private String flagPlanoFamiliar;
    }
}