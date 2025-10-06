package com.odontoPrev.odontoPrev.infrastructure.client.adapter.out.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO PARA RESPOSTA DE INCLUSÃO DE BENEFICIÁRIO DA ODONTOPREV
 *
 * Esta classe recebe a resposta da OdontoPrev após o cadastro bem-sucedido
 * de um novo beneficiário.
 *
 * QUANDO É USADA:
 * - Ao receber resposta do endpoint POST /incluir da OdontoPrev
 * - O campo cdAssociado contém o número da carteirinha gerado
 *
 * CAMPO PRINCIPAL:
 * - cdAssociado: Número da carteirinha do beneficiário na OdontoPrev
 *   Este número deve ser armazenado no banco para futuras operações
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BeneficiarioInclusaoResponse {

    /**
     * Código do associado (carteirinha) gerado pela OdontoPrev
     * Este é o identificador único do beneficiário no sistema da OdontoPrev
     */
    @JsonProperty("cdAssociado")
    private String cdAssociado;

    /**
     * Mensagem de retorno da API (opcional)
     */
    @JsonProperty("mensagem")
    private String mensagem;

    /**
     * Status da operação (opcional)
     */
    @JsonProperty("status")
    private String status;

    /**
     * Código de erro, se houver (opcional)
     */
    @JsonProperty("codigoErro")
    private String codigoErro;

    /**
     * Descrição detalhada do erro, se houver (opcional)
     */
    @JsonProperty("descricaoErro")
    private String descricaoErro;
}
