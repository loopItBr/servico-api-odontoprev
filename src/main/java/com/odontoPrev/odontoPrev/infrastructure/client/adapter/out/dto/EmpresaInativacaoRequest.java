package com.odontoPrev.odontoPrev.infrastructure.client.adapter.out.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * DTO PARA REQUISIÇÃO DE INATIVAÇÃO DE EMPRESA
 *
 * Este DTO representa o formato esperado pela API OdontoPrev para inativação de empresas.
 * Baseado na documentação atualizada da API que espera:
 * - sistema: Código da empresa (vem do header)
 * - codigoUsuario: Sempre "0" conforme especificação
 * - listaDadosInativacaoEmpresa: Lista com dados das empresas a serem inativadas
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmpresaInativacaoRequest {

    /**
     * SISTEMA QUE ESTÁ REALIZANDO A INATIVAÇÃO
     * 
     * Deve conter o código da empresa que vem do header "empresa".
     * Exemplo: "787392"
     */
    @JsonProperty("sistema")
    @NotBlank(message = "Sistema é obrigatório")
    private String sistema;

    /**
     * USUÁRIO QUE ESTÁ REALIZANDO A INATIVAÇÃO
     * 
     * Conforme especificação da API, este campo deve sempre ser "0".
     */
    @JsonProperty("codigoUsuario")
    @NotBlank(message = "Código do usuário é obrigatório")
    private String codigoUsuario;

    /**
     * LISTA DE DADOS DE INATIVAÇÃO DAS EMPRESAS
     * 
     * Contém os dados específicos de cada empresa que será inativada.
     * Pode conter múltiplas empresas em uma única requisição.
     */
    @JsonProperty("listaDadosInativacaoEmpresa")
    @NotEmpty(message = "Lista de dados de inativação não pode estar vazia")
    @NotNull(message = "Lista de dados de inativação é obrigatória")
    private List<DadosInativacaoEmpresa> listaDadosInativacaoEmpresa;

    /**
     * DADOS ESPECÍFICOS DE INATIVAÇÃO DE UMA EMPRESA
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DadosInativacaoEmpresa {

        /**
         * CÓDIGO DA EMPRESA
         * 
         * Código único da empresa no sistema (6 caracteres).
         * Exemplo: "008753"
         */
        @JsonProperty("codigoEmpresa")
        @NotBlank(message = "Código da empresa é obrigatório")
        private String codigoEmpresa;

        /**
         * CÓDIGO DO MOTIVO DO FIM DA EMPRESA
         * 
         * Código numérico que indica o motivo pelo qual a empresa está sendo finalizada.
         * Exemplo: "1"
         */
        @JsonProperty("codigoMotivoFimEmpresa")
        @NotBlank(message = "Código do motivo fim empresa é obrigatório")
        private String codigoMotivoFimEmpresa;

        /**
         * CÓDIGO DO MOTIVO DE INATIVAÇÃO
         * 
         * Código numérico que indica o motivo específico da inativação.
         * Campo opcional conforme documentação.
         * Exemplo: "2"
         */
        @JsonProperty("codigoMotivoInativacao")
        private String codigoMotivoInativacao;

        /**
         * DATA DE FIM DO CONTRATO
         * 
         * Data em que o contrato da empresa será finalizado.
         * Formato: YYYY-MM-DD
         * Exemplo: "2024-12-31"
         */
        @JsonProperty("dataFimContrato")
        @NotBlank(message = "Data fim contrato é obrigatória")
        private String dataFimContrato;
    }
}
