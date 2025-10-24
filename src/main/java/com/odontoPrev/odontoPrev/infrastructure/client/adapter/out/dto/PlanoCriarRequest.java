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
 * DTO para criação de planos na API OdontoPrev
 * 
 * Endpoint: POST /empresa/2.0/plano/criar
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PlanoCriarRequest {

    @JsonProperty("codigoGrupoGerencial")
    private String codigoGrupoGerencial;

    @JsonProperty("codigoEmpresa")
    private List<String> codigoEmpresa;

    @JsonProperty("sistema")
    private String sistema;

    @JsonProperty("codigoUsuario")
    private String codigoUsuario;

    @JsonProperty("listaPlano")
    private List<PlanoItem> listaPlano;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PlanoItem {
        @JsonProperty("valorTitular")
        private Double valorTitular;

        @JsonProperty("codigoPlano")
        private Integer codigoPlano;

        @JsonProperty("dataInicioPlano")
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
        private LocalDateTime dataInicioPlano;

        @JsonProperty("valorDependente")
        private Double valorDependente;

        @JsonProperty("valorReembolsoUO")
        private Double valorReembolsoUO;

        @JsonProperty("percentualAgregadoRedeGenerica")
        private Double percentualAgregadoRedeGenerica;

        @JsonProperty("percentualDependenteRedeGenerica")
        private Double percentualDependenteRedeGenerica;

        @JsonProperty("idSegmentacaoGrupoRede")
        private Integer idSegmentacaoGrupoRede;

        @JsonProperty("idNomeFantasia")
        private Integer idNomeFantasia;

        @JsonProperty("redes")
        private List<Rede> redes;

        @JsonProperty("percentualAssociado")
        private Double percentualAssociado;

        @JsonProperty("planoFamiliar")
        private String planoFamiliar;

        @JsonProperty("periodicidade")
        private String periodicidade;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Rede {
        @JsonProperty("codigoRede")
        private String codigoRede;
    }
}
