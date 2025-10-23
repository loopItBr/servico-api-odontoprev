package com.odontoPrev.odontoPrev.infrastructure.client.adapter.out.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO PARA RESPOSTA DA ATIVAÇÃO DO PLANO DA EMPRESA NA ODONTOPREV
 *
 * Esta classe representa a resposta da API de ativação do plano da empresa.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmpresaAtivacaoPlanoResponse {

    @JsonProperty("status")
    private Integer status;

    @JsonProperty("mensagem")
    private String mensagem;

    @JsonProperty("codigoEmpresa")
    private String codigoEmpresa;

    @JsonProperty("senha")
    private String senha;

    @JsonProperty("dataAtivacao")
    private String dataAtivacao;
}
