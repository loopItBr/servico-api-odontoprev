package com.odontoPrev.odontoPrev.infrastructure.client.adapter.out.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO PARA RESPOSTA DE INATIVAÇÃO DE BENEFICIÁRIO NA ODONTOPREV - NOVA API
 *
 * Esta classe representa a resposta da API de inativação de beneficiários
 * conforme a nova documentação da OdontoPrev.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BeneficiarioInativacaoResponseNew {

    @JsonProperty("protocolo")
    private String protocolo;

    @JsonProperty("ocorrencia")
    private String ocorrencia;
}
