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
public class PlanoResponse {

    @JsonProperty("celula")
    private CelulaDto celula;

    @JsonProperty("codigoNacionalSaude")
    private String codigoNacionalSaude;

    @JsonProperty("codigoPlano")
    private String codigoPlano;

    @JsonProperty("dataInicioVigencia")
    private LocalDateTime dataInicioVigencia;

    @JsonProperty("dataTerminoVigencia")
    private LocalDateTime dataTerminoVigencia;

    @JsonProperty("especialidades")
    private List<EspecialidadeDto> especialidades;

    @JsonProperty("formasPagamento")
    private List<FormaPagamentoDto> formasPagamento;

    @JsonProperty("modalidades")
    private List<ModalidadeDto> modalidades;

    @JsonProperty("nomeComercial")
    private String nomeComercial;

    @JsonProperty("nomePlano")
    private String nomePlano;

    @JsonProperty("numeroProcessoAns")
    private String numeroProcessoAns;

    @JsonProperty("regiao")
    private RegiaoDto regiao;

    @JsonProperty("segmentacao")
    private SegmentacaoDto segmentacao;

    @JsonProperty("tipoPlano")
    private TipoPlanoDto tipoPlano;

    @JsonProperty("tipoVigencia")
    private String tipoVigencia;

    @JsonProperty("utilizaTetoTemporario")
    private Boolean utilizaTetoTemporario;

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
    public static class EspecialidadeDto {
        @JsonProperty("codigoEspecialidade")
        private Integer codigoEspecialidade;

        @JsonProperty("codigoEspecialidadeDentista")
        private Integer codigoEspecialidadeDentista;

        @JsonProperty("nomeEspecialidade")
        private String nomeEspecialidade;
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
    public static class ModalidadeDto {
        @JsonProperty("codigoModalidade")
        private Integer codigoModalidade;

        @JsonProperty("nomeModalidade")
        private String nomeModalidade;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RegiaoDto {
        @JsonProperty("codigoRegiao")
        private Integer codigoRegiao;

        @JsonProperty("nomeRegiao")
        private String nomeRegiao;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SegmentacaoDto {
        @JsonProperty("codigoSegmentacao")
        private Integer codigoSegmentacao;

        @JsonProperty("nomeSegmentacao")
        private String nomeSegmentacao;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TipoPlanoDto {
        @JsonProperty("codigoTipoPlano")
        private Integer codigoTipoPlano;

        @JsonProperty("nomeTipoPlano")
        private String nomeTipoPlano;
    }
}