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
 * informações sobre beneficiários que foram excluídos/inativados e precisam ser
 * removidos da OdontoPrev durante a sincronização.
 *
 * IMPORTANTE - É UMA VIEW, NÃO UMA TABELA:
 * - Esta entidade mapeia uma VIEW (vw_integracao_odontoprev_beneficiarios_exc)
 * - Views são "consultas salvas" que juntam dados de várias tabelas
 * - Por isso usa @Immutable - os dados não podem ser alterados
 * - Apenas consultamos os dados, não inserimos/atualizamos
 *
 * ESTRUTURA DOS DADOS DA VIEW:
 * A view VW_INTEGRACAO_ODONTOPREV_BENEFICIARIOS_EXC possui apenas 8 colunas:
 * 1. CDEMPRESA - Código da empresa
 * 2. CDUSUARIO - Código do usuário (fixo: 13433638)
 * 3. CODIGOMATRICULA - Matrícula do beneficiário
 * 4. CDASSOCIADO - Código do associado na OdontoPrev
 * 5. NOME - Nome do beneficiário
 * 6. IDMOTIVO - ID do motivo de exclusão
 * 7. DATAINATIVACAO - Data da inativação
 * 8. EMAIL - Email do beneficiário
 *
 * USO NO SISTEMA:
 * 1. Sistema consulta esta view para obter lista de beneficiários excluídos
 * 2. Para cada beneficiário, busca seus dados e motivo de exclusão
 * 3. Envia estes dados para API da OdontoPrev (endpoint /inativarAssociadoEmpresarial)
 * 4. Salva controle do que foi enviado
 *
 * EXEMPLO DE REGISTRO:
 * Um funcionário "João Silva" que foi demitido e precisa ser inativado
 * no plano odontológico empresarial.
 */
@Entity
@Table(name = "vw_integracao_odontoprev_beneficiarios_exc", schema = "TASY")
@Immutable  // Indica que esta entidade é apenas para leitura (VIEW)
@Data
@NoArgsConstructor
@AllArgsConstructor
public class IntegracaoOdontoprevBeneficiarioExclusao {

    /**
     * CÓDIGO DA MATRÍCULA DO FUNCIONÁRIO (CHAVE PRIMÁRIA)
     *
     * Matrícula única do beneficiário na empresa.
     * Usado para identificar o funcionário no sistema.
     */
    @Id
    @Column(name = "CODIGOMATRICULA", nullable = false, length = 20)
    @NotBlank(message = "Código da matrícula é obrigatório")
    private String codigoMatricula;

    /**
     * CÓDIGO DO ASSOCIADO NA ODONTOPREV
     *
     * Código da carteirinha gerado pela OdontoPrev.
     * Campo obrigatório para exclusão (beneficiário deve já existir).
     */
    @Column(name = "CDASSOCIADO", length = 50)
    @NotBlank(message = "Código do associado é obrigatório para exclusão")
    private String cdAssociado;

    /**
     * CÓDIGO DA EMPRESA DO BENEFICIÁRIO
     *
     * Código que identifica a empresa onde o beneficiário trabalha.
     */
    @Column(name = "CDEMPRESA", length = 10)
    @NotBlank(message = "Código da empresa é obrigatório")
    private String codigoEmpresa;

    /**
     * CÓDIGO DO USUÁRIO
     *
     * Código do usuário padrão para acesso da OdontoPrev (fixo: 13433638).
     */
    @Column(name = "CDUSUARIO", length = 10)
    private String codigoUsuario;

    /**
     * NOME COMPLETO DO BENEFICIÁRIO
     *
     * Nome completo conforme documento de identificação.
     */
    @Column(name = "NOME", length = 200)
    @NotBlank(message = "Nome do beneficiário é obrigatório")
    private String nome;

    /**
     * ID DO MOTIVO DE EXCLUSÃO
     *
     * ID específico do motivo de exclusão para a OdontoPrev.
     * Campo obrigatório para exclusão.
     */
    @Column(name = "IDMOTIVO")
    private Integer idMotivo;

    /**
     * DATA DE INATIVAÇÃO
     *
     * Data em que o beneficiário foi inativado (formato DD/MM/YYYY).
     */
    @Column(name = "DATAINATIVACAO", length = 10)
    private String dataInativacao;

    /**
     * EMAIL DO BENEFICIÁRIO
     *
     * Endereço de email para comunicações.
     */
    @Column(name = "EMAIL", length = 150)
    private String email;
}
