package com.odontoPrev.odontoPrev.infrastructure.repository.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import org.hibernate.annotations.Immutable;

/**
 * ENTIDADE QUE REPRESENTA OS DADOS DE EXCLUSÃO DE BENEFICIÁRIOS PARA INTEGRAÇÃO COM ODONTOPREV
 *
 * FUNÇÃO PRINCIPAL:
 * Esta classe representa a VIEW VW_INTEGRACAO_ODONTOPREV_BENEFICIARIOS_EXC que consolida
 * informações sobre beneficiários que foram inativados/excluídos e precisam ser
 * removidos da OdontoPrev.
 *
 * IMPORTANTE - É UMA VIEW, NÃO UMA TABELA:
 * - Esta entidade mapeia uma VIEW (VW_INTEGRACAO_ODONTOPREV_BENEFICIARIOS_EXC)
 * - Views são "consultas salvas" que juntam dados de várias tabelas
 * - Por isso usa @Immutable - os dados não podem ser alterados
 * - Apenas consultamos os dados, não inserimos/atualizamos
 *
 * ESTRUTURA DOS DADOS:
 * A view consolida informações de:
 * 1. EMPRESA: código empresa
 * 2. USUÁRIO: código do usuário
 * 3. BENEFICIÁRIO: código matrícula, nome, código associado
 * 4. MOTIVO: motivo de inativação
 * 5. DATA: data de inativação
 * 6. CONTATO: email
 *
 * USO NO SISTEMA:
 * 1. Sistema consulta esta view para obter lista de beneficiários excluídos
 * 2. Para cada beneficiário, busca seus dados completos
 * 3. Envia requisição de exclusão para API da OdontoPrev
 * 4. Salva controle do que foi enviado
 */
@Entity
@Table(name = "VW_INTEGRACAO_ODONTOPREV_BENEFICIARIOS_EXC", schema = "TASY")
@Immutable
@Data
@NoArgsConstructor
@AllArgsConstructor
public class IntegracaoOdontoprevBeneficiarioExclusao {

    /**
     * CÓDIGO DA EMPRESA
     */
    @Id
    @Column(name = "CDEMPRESA", nullable = false, length = 6)
    @NotBlank(message = "Código da empresa é obrigatório")
    private String cdEmpresa;

    /**
     * CÓDIGO DO USUÁRIO
     */
    @Column(name = "CDUSUARIO")
    private Long cdUsuario;

    /**
     * CÓDIGO DA MATRÍCULA DO FUNCIONÁRIO
     */
    @Column(name = "CODIGOMATRICULA", length = 48)
    private String codigoMatricula;

    /**
     * CÓDIGO DO ASSOCIADO NA ODONTOPREV
     */
    @Column(name = "CDASSOCIADO", length = 4000)
    private String cdAssociado;

    /**
     * NOME DO BENEFICIÁRIO
     */
    @Column(name = "NOME", length = 4000)
    private String nome;

    /**
     * ID DO MOTIVO DE INATIVAÇÃO
     */
    @Column(name = "IDMOTIVO")
    private Long idMotivo;

    /**
     * DATA DE INATIVAÇÃO
     */
    @Column(name = "DATAINATIVACAO", length = 10)
    private String dataInativacao;

    /**
     * EMAIL DO BENEFICIÁRIO
     */
    @Column(name = "EMAIL", length = 4000)
    private String email;
}