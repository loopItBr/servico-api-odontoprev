package com.odontoPrev.odontoPrev.infrastructure.client.adapter.out.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO PARA DADOS DE ASSOCIADO NA INATIVAÇÃO
 *
 * Esta classe representa um associado (beneficiário) que será inativado
 * no sistema da OdontoPrev. Faz parte do array 'associado' dentro do
 * EmpresarialModelInativacao.
 *
 * QUANDO É USADA:
 * - Como item do array 'associado' no payload de inativação
 * - Contém dados mínimos necessários para identificar o beneficiário
 *
 * CAMPOS OBRIGATÓRIOS:
 * - cdMatricula: Matrícula do associado na empresa
 * - cdAssociado: Número da carteirinha na OdontoPrev
 * - nome: Nome completo do beneficiário
 * - idMotivo: Código do motivo de inativação (ex: "7")
 *
 * CAMPOS OPCIONAIS:
 * - email: E-mail do beneficiário para notificações
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AssociadoInativacao {

    /**
     * Matrícula do associado na empresa
     * Identificador único do beneficiário no sistema da empresa
     * Exemplo: "00000001"
     */
    @NotBlank(message = "Código da matrícula é obrigatório")
    @JsonProperty("cdMatricula")
    private String cdMatricula;

    /**
     * Número da carteirinha do associado na OdontoPrev
     * Código retornado pela API no momento da inclusão
     * Exemplo: "000000001"
     */
    @NotBlank(message = "Código do associado é obrigatório")
    @JsonProperty("cdAssociado")
    private String cdAssociado;

    /**
     * Nome completo do beneficiário
     * Deve ser o mesmo nome cadastrado no sistema
     * Exemplo: "JOÃO SILVA SANTOS"
     */
    @NotBlank(message = "Nome é obrigatório")
    @JsonProperty("nome")
    private String nome;

    /**
     * E-mail do beneficiário (opcional)
     * Usado para envio de notificações pela OdontoPrev
     * Exemplo: "joao.silva@exemplo.com"
     */
    @JsonProperty("email")
    private String email;

    /**
     * ID do motivo de inativação
     * Código conforme tabela de motivos da OdontoPrev
     * Exemplos: "7" = Rescisão, outros conforme documentação
     */
    @NotNull(message = "ID do motivo é obrigatório")
    @JsonProperty("idMotivo")
    private String idMotivo;
}
