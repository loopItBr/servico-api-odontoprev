package com.odontoPrev.odontoPrev.infrastructure.client.adapter.out.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ContratoResponse {

    @JsonProperty("codigoContrato")
    private String codigoContrato;

    @JsonProperty("dataInicioContrato")
    private LocalDateTime dataInicioContrato;

    @JsonProperty("dataInicioVigencia")
    private LocalDateTime dataInicioVigencia;

    @JsonProperty("dataTerminoContrato")
    private LocalDateTime dataTerminoContrato;

    @JsonProperty("dataTerminoVigencia")
    private LocalDateTime dataTerminoVigencia;

    @JsonProperty("flexibilizacao")
    private FlexibilizacaoDto flexibilizacao;

    @JsonProperty("formasPagamento")
    private List<FormaPagamentoDto> formasPagamento;

    @JsonProperty("periodicidade")
    private PeriodicidadeDto periodicidade;

    @JsonProperty("plano")
    private PlanoContratoDto plano;

    @JsonProperty("tipoVigencia")
    private String tipoVigencia;

    @JsonProperty("utilizaTetoTemporario")
    private Boolean utilizaTetoTemporario;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FlexibilizacaoDto {
        @JsonProperty("codigoFlexibilizacao")
        private Integer codigoFlexibilizacao;

        @JsonProperty("nomeFlexibilizacao")
        private String nomeFlexibilizacao;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FormaPagamentoDto {
        @JsonProperty("codigoFormaPagamento")
        private Integer codigoFormaPagamento;

        @JsonProperty("descricaoFormaPagamento")
        private String descricaoFormaPagamento;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PeriodicidadeDto {
        @JsonProperty("codigoPeriodicidade")
        private Integer codigoPeriodicidade;

        @JsonProperty("nomePeriodicidade")
        private String nomePeriodicidade;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PlanoContratoDto {
        @JsonProperty("codigoNacionalSaude")
        private String codigoNacionalSaude;

        @JsonProperty("codigoPlano")
        private String codigoPlano;

        @JsonProperty("nomeComercial")
        private String nomeComercial;

        @JsonProperty("nomePlano")
        private String nomePlano;

        @JsonProperty("numeroProcessoAns")
        private String numeroProcessoAns;
    }
}