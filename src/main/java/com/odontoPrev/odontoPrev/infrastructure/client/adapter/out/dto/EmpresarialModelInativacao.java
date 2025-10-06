package com.odontoPrev.odontoPrev.infrastructure.client.adapter.out.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * MODELO EMPRESARIAL PARA INATIVAÇÃO DE BENEFICIÁRIOS
 *
 * Esta classe representa a estrutura completa do payload que deve ser
 * enviado no campo 'empresarialModel' via multipart/form-data para
 * o endpoint de inativação de associados da OdontoPrev.
 *
 * QUANDO É USADA:
 * - Ao enviar requisição POST para /cadastroonline-pj/1.0/inativarAssociadoEmpresarial
 * - Quando beneficiário tem status Rescindido/Suspenso no Tasy
 *
 * ESTRUTURA DO PAYLOAD:
 * {
 *   "cdEmpresa": "787392",
 *   "cdUsuario": "13433638",
 *   "associado": [{
 *     "cdMatricula": "00000001",
 *     "cdAssociado": "000000001",
 *     "nome": "NOME DO ASSOCIADO",
 *     "email": "email@exemplo.com",
 *     "idMotivo": "7"
 *   }],
 *   "dataInativacao": "2024-12-29"
 * }
 *
 * OBSERVAÇÕES:
 * - O campo 'associado' é um array (pode conter múltiplos beneficiários)
 * - dataInativacao é opcional (se não informado, usa data atual)
 * - Este objeto será serializado para JSON e enviado como string no multipart
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmpresarialModelInativacao {

    /**
     * Código da empresa contratante do plano
     * Exemplo: "787392"
     */
    @NotBlank(message = "Código da empresa é obrigatório")
    @JsonProperty("cdEmpresa")
    private String cdEmpresa;

    /**
     * Código do usuário que está executando a operação
     * Valor fixo conforme credenciais fornecidas pela OdontoPrev
     * Exemplo: "13433638"
     */
    @NotBlank(message = "Código do usuário é obrigatório")
    @JsonProperty("cdUsuario")
    private String cdUsuario;

    /**
     * Lista de associados a serem inativados
     * Pode conter um ou mais beneficiários
     */
    @NotEmpty(message = "Lista de associados não pode estar vazia")
    @JsonProperty("associado")
    private List<AssociadoInativacao> associado;

    /**
     * Data de inativação (opcional)
     * Formato: YYYY-MM-DD
     * Se não informado, OdontoPrev considera data atual
     * Exemplo: "2024-12-29"
     */
    @JsonProperty("dataInativacao")
    private String dataInativacao;
}
