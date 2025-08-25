package com.odontoPrev.odontoPrev.infrastructure.client.adapter.out.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class EmpresaResponse {

    @JsonProperty("celula")
    private CelulaDto celula;

    @JsonProperty("codigoClienteOperadora")
    private String codigoClienteOperadora;

    @JsonProperty("codigoEmpresa")
    private String codigoEmpresa;

    @JsonProperty("dataInicioContrato")
    private LocalDateTime dataInicioContrato;

    @JsonProperty("dataVigencia")
    private LocalDateTime dataVigencia;

    @JsonProperty("empresaPF")
    private Boolean empresaPF;

    @JsonProperty("grupoGerencial")
    private GrupoGerencialDto grupoGerencial;

    @JsonProperty("marca")
    private MarcaDto marca;

    @JsonProperty("nomeFantasia")
    private String nomeFantasia;

    @JsonProperty("valorUltimoFaturamento")
    private Double valorUltimoFaturamento;

    @JsonProperty("vidasAtivas")
    private Integer vidasAtivas;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CelulaDto {
        @JsonProperty("codigoCelula")
        private Integer codigoCelula;

        @JsonProperty("nomeCelula")
        private String nomeCelula;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MarcaDto {
        @JsonProperty("codigoMarca")
        private Integer codigoMarca;

        @JsonProperty("nomeMarca")
        private String nomeMarca;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class GrupoGerencialDto {
        @JsonProperty("celula")
        private CelulaDto celula;

        @JsonProperty("codigoClienteOperadora")
        private String codigoClienteOperadora;

        @JsonProperty("codigoEmpresa")
        private String codigoEmpresa;

        @JsonProperty("dataVigencia")
        private LocalDateTime dataVigencia;

        @JsonProperty("empresaPF")
        private Boolean empresaPF;

        @JsonProperty("marca")
        private MarcaDto marca;

        @JsonProperty("nomeFantasia")
        private String nomeFantasia;
    }
}