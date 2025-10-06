package com.odontoPrev.odontoPrev.infrastructure.client.adapter.out.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO PARA INATIVAÇÃO DE BENEFICIÁRIO NA ODONTOPREV
 *
 * Esta classe contém os dados necessários para inativar um beneficiário
 * no sistema da OdontoPrev (rescisão ou suspensão do plano).
 *
 * QUANDO É USADA:
 * - Ao enviar requisição POST para o endpoint /inativarAssociadoEmpresarial
 * - Quando beneficiário tem status Rescindido/Suspenso no Tasy
 *
 * CAMPOS OBRIGATÓRIOS:
 * - cdEmpresa, cdUsuario, cdMatricula, cdAssociado, nome, idMotivo
 *
 * OBSERVAÇÃO:
 * O campo idMotivo já vem preenchido pela view conforme padrão OdontoPrev
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BeneficiarioInativacaoRequest {

    @NotBlank(message = "Código da empresa é obrigatório")
    @JsonProperty("cdEmpresa")
    private String cdEmpresa;

    @NotBlank(message = "Código do usuário é obrigatório")
    @JsonProperty("cdUsuario")
    private String cdUsuario;

    @NotBlank(message = "Código da matrícula é obrigatório")
    @JsonProperty("cdMatricula")
    private String cdMatricula;

    @NotBlank(message = "Código do associado é obrigatório")
    @JsonProperty("cdAssociado")
    private String cdAssociado;

    @NotBlank(message = "Nome é obrigatório")
    @JsonProperty("nome")
    private String nome;

    @NotNull(message = "ID do motivo é obrigatório")
    @JsonProperty("idMotivo")
    private Integer idMotivo;

    /**
     * Data de inativação (opcional)
     * Usado quando a inativação é futura (ex: direito até determinada data)
     * Se não informado, considera a data atual
     */
    @JsonProperty("dataInativacao")
    private String dataInativacao;

    /**
     * E-mail do beneficiário (opcional)
     */
    @JsonProperty("EMAIL")
    private String email;
}
