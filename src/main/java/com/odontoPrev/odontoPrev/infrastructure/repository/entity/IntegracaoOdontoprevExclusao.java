package com.odontoPrev.odontoPrev.infrastructure.repository.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import org.hibernate.annotations.Immutable;

import java.time.LocalDate;

/**
 * ENTIDADE QUE REPRESENTA OS DADOS DE EXCLUSÃO DE EMPRESAS PARA INTEGRAÇÃO COM ODONTOPREV
 * 
 * FUNÇÃO PRINCIPAL:
 * Esta classe representa a VIEW VW_INTEGRACAO_ODONTOPREV_EXC que contém informações
 * básicas sobre empresas que foram inativadas/excluídas e precisam ser
 * removidas da OdontoPrev.
 * 
 * IMPORTANTE - É UMA VIEW, NÃO UMA TABELA:
 * - Esta entidade mapeia uma VIEW (vw_integracao_odontoprev_exc)
 * - Views são "consultas salvas" que juntam dados de várias tabelas
 * - Por isso usa @Immutable - os dados não podem ser alterados
 * - Apenas consultamos os dados, não inserimos/atualizamos
 * 
 * CRITÉRIO DE SELEÇÃO:
 * - Empresas com ATIVO = 2 (inativas)
 * - Planos com ATIVO = 2 e IE_SEGMENTACAO = 4 (excluídos)
 * - Identifica empresas que precisam ser removidas da OdontoPrev
 * 
 * ESTRUTURA DOS DADOS:
 * A view contém apenas informações básicas:
 * 1. SISTEMA: identificador do sistema
 * 2. CODIGOUSUARIO: código do usuário
 * 3. CODIGO_EMPRESA: código da empresa
 * 4. CODIGOMOTIVOFIMEMPRESA: motivo do fim da empresa
 * 5. DATA_FIM_CONTRATO: data de fim do contrato
 * 
 * USO NO SISTEMA:
 * 1. Sistema consulta esta view para obter lista de empresas excluídas
 * 2. Para cada empresa, busca seus dados completos em outras views
 * 3. Envia requisição de exclusão para API da OdontoPrev
 * 4. Salva controle do que foi enviado com tipo_controle = 3
 */
@Entity
@Table(name = "VW_INTEGRACAO_ODONTOPREV_EXC", schema = "TASY")
@Immutable
@Data
@NoArgsConstructor
@AllArgsConstructor
public class IntegracaoOdontoprevExclusao {

    /**
     * IDENTIFICADOR DO SISTEMA
     * 
     * Código que identifica o sistema de origem dos dados.
     * Campo de até 10 caracteres conforme view VW_INTEGRACAO_ODONTOPREV_EXC.
     */
    @Column(name = "SISTEMA", nullable = true, length = 10)
    private String sistema;

    /**
     * CÓDIGO DO USUÁRIO
     * 
     * Identificador do usuário responsável pela operação.
     */
    @Column(name = "CODIGOUSUARIO", nullable = true)
    private Long codigoUsuario;

    /**
     * CÓDIGO ÚNICO DA EMPRESA NO SISTEMA TASY
     * 
     * Este é o identificador principal de cada empresa em nosso sistema.
     * É usado como chave primária e para buscar dados da empresa na OdontoPrev.
     * 
     * Exemplo: "A001", "EMP123", "XYZ789"
     */
    @Id
    @Column(name = "CODIGOEMPRESA", nullable = false, length = 255)
    @NotBlank(message = "Código da empresa é obrigatório")
    @Size(min = 1, max = 255, message = "Código da empresa deve ter entre 1 e 255 caracteres")
    @Pattern(regexp = "^[A-Za-z0-9]+$", message = "Código da empresa deve conter apenas letras e números")
    private String codigoEmpresa;

    /**
     * CÓDIGO DO MOTIVO DO FIM DA EMPRESA
     * 
     * Identifica o motivo pelo qual a empresa foi inativada/excluída.
     * Exemplos: 1=Encerramento de contrato, 2=Inadimplência, etc.
     */
    @Column(name = "CODIGOMOTIVOFIMEMPRESA", nullable = true)
    private Long codigoMotivoFimEmpresa;

    /**
     * DATA DE FIM DO CONTRATO COM A EMPRESA
     * 
     * Quando termina a vigência do contrato odontológico.
     * Esta é a data oficial de encerramento da relação comercial.
     */
    @Column(name = "DATA_FIM_CONTRATO", nullable = true)
    private LocalDate dataFimContrato;
}
