package com.odontoPrev.odontoPrev.infrastructure.repository.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import org.hibernate.annotations.Immutable;

/**
 * ENTIDADE QUE REPRESENTA OS DADOS DE ALTERAÇÃO DE EMPRESAS PARA INTEGRAÇÃO COM ODONTOPREV
 * 
 * FUNÇÃO PRINCIPAL:
 * Esta classe representa a VIEW VW_INTEGRACAO_ODONTOPREV_ALT que consolida todas as
 * informações sobre empresas que foram alteradas e precisam ser sincronizadas
 * com a OdontoPrev.
 * 
 * IMPORTANTE - É UMA VIEW, NÃO UMA TABELA:
 * - Esta entidade mapeia uma VIEW (VW_INTEGRACAO_ODONTOPREV_ALT)
 * - Views são "consultas salvas" que juntam dados de várias tabelas
 * - Por isso usa @Immutable - os dados não podem ser alterados
 * - Apenas consultamos os dados, não inserimos/atualizamos
 * 
 * CRITÉRIO DE SELEÇÃO:
 * - Empresas que tiveram DT_ALTERACAO = SYSDATE (alteradas hoje)
 * - Identifica empresas que precisam ter dados atualizados na OdontoPrev
 * 
 * USO NO SISTEMA:
 * 1. Sistema consulta esta view para obter lista de empresas alteradas
 * 2. Para cada empresa, busca seus dados completos
 * 3. Envia estes dados para API da OdontoPrev com operação UPDATE
 * 4. Salva controle do que foi enviado com tipo_controle = 2
 */
@Entity
@Table(name = "VW_INTEGRACAO_ODONTOPREV_ALT", schema = "TASY")
@Immutable
@Data
@NoArgsConstructor
@AllArgsConstructor
public class IntegracaoOdontoprevAlteracao {

    /**
     * NÚMERO SEQUENCIAL DO CONTRATO
     * NUMBER(10,0)
     */
    @Id
    @Column(name = "NR_SEQ_CONTRATO", nullable = true)
    private Long nrSeqContrato;

    /**
     * CÓDIGO DA EMPRESA
     * VARCHAR2(255)
     */
    @Column(name = "CODIGOEMPRESA", nullable = true, length = 255)
    private String codigoEmpresa;

    /**
     * NOME FANTASIA
     * VARCHAR2(20)
     */
    @Column(name = "NOMEFANTASIA", nullable = true, length = 20)
    private String nomeFantasia;

    /**
     * EMITE CARTEIRINHA PLÁSTICA
     * CHAR(1)
     */
    @Column(name = "EMITECARTEIRINHAPLASTICA", nullable = true, length = 1)
    private String emiteCarteirinhaPlastica;

    /**
     * PERMISSÃO CADASTRO DEP
     * CHAR(1)
     */
    @Column(name = "PERMISSAOCADASTRODEP", nullable = true, length = 1)
    private String permissaoCadastroDep;

    /**
     * DESCRIÇÃO RAMO ATIVIDADE
     * VARCHAR2(40)
     */
    @Column(name = "DESCRICAORAMOATIVIDADE", nullable = true, length = 40)
    private String descricaoRamoAtividade;

    /**
     * NÚMERO DE FUNCIONÁRIOS
     * NUMBER
     */
    @Column(name = "NUMEROFUNCIONARIOS", nullable = true)
    private Long numeroFuncionarios;

    /**
     * VALOR FATOR
     * NUMBER
     */
    @Column(name = "VALORFATOR", nullable = true)
    private Long valorFator;

    /**
     * CNAE
     * VARCHAR2(50)
     */
    @Column(name = "CNAE", nullable = true, length = 50)
    private String cnae;

    /**
     * CÓDIGO LAYOUT CARTEIRINHA
     * CHAR(1)
     */
    @Column(name = "CODIGOLAYOUTCARTEIRINHA", nullable = true, length = 1)
    private String codigoLayoutCarteirinha;

    /**
     * CÓDIGO ORDEM CARTEIRA
     * NUMBER
     */
    @Column(name = "CODIGOORDEMCARTEIRA", nullable = true)
    private Long codigoOrdemCarteira;

    /**
     * LIBERA SENHA INTERNET
     * CHAR(1)
     */
    @Column(name = "LIBERASENHAINTERNET", nullable = true, length = 1)
    private String liberaSenhaInternet;

    /**
     * DEPENDENTE PAGA
     * CHAR(1)
     */
    @Column(name = "DEPENDENTEPAGA", nullable = true, length = 1)
    private String dependentePaga;

    /**
     * CUSTO FAMILIAR
     * CHAR(1)
     */
    @Column(name = "CUSTOFAMILIAR", nullable = true, length = 1)
    private String custoFamiliar;

    /**
     * PLANO FAMILIAR
     * CHAR(1)
     */
    @Column(name = "PLANOFAMILIAR", nullable = true, length = 1)
    private String planoFamiliar;

    /**
     * IDADE LIMITE UNIVERSITÁRIA
     * NUMBER
     */
    @Column(name = "IDADELIMITEUNIVERSITARIA", nullable = true)
    private Long idadeLimiteUniversitaria;

    /**
     * CÓDIGO REGIÃO
     * NUMBER
     */
    @Column(name = "CODIGOREGIAO", nullable = true)
    private Long codigoRegiao;

    /**
     * RAZÃO SOCIAL
     * VARCHAR2(45)
     */
    @Column(name = "RAZAOSOCIAL", nullable = true, length = 45)
    private String razaoSocial;

    /**
     * INSCRIÇÃO MUNICIPAL
     * VARCHAR2(20)
     */
    @Column(name = "INSCRICAOMUNICIPAL", nullable = true, length = 20)
    private String inscricaoMunicipal;

    /**
     * INSCRIÇÃO ESTADUAL
     * VARCHAR2(20)
     */
    @Column(name = "INSCRICAOESTADUAL", nullable = true, length = 20)
    private String inscricaoEstadual;

    /**
     * EMAIL
     * VARCHAR2(4000)
     */
    @Column(name = "EMAIL", nullable = true, length = 4000)
    private String email;

    /**
     * TIPO LOGRADOURO
     * NUMBER
     */
    @Column(name = "TIPOLOGRADOURO", nullable = true)
    private Long tipoLogradouro;

    /**
     * LOGRADOURO
     * VARCHAR2(40)
     */
    @Column(name = "LOGRADOURO", nullable = true, length = 40)
    private String logradouro;

    /**
     * NÚMERO
     * VARCHAR2(10)
     */
    @Column(name = "NUMERO", nullable = true, length = 10)
    private String numero;

    /**
     * BAIRRO
     * VARCHAR2(40)
     */
    @Column(name = "BAIRRO", nullable = true, length = 40)
    private String bairro;

    /**
     * CÓDIGO (CIDADE)
     * VARCHAR2(10)
     */
    @Column(name = "CODIGO", nullable = true, length = 10)
    private String codigo;

    /**
     * NOME CIDADE
     * VARCHAR2(40)
     */
    @Column(name = "NOMECIDADE", nullable = true, length = 40)
    private String nomeCidade;

    /**
     * SIGLA UF
     * VARCHAR2(15)
     */
    @Column(name = "SIGLAUF", nullable = true, length = 15)
    private String siglaUf;

    /**
     * CÓDIGO PAÍS
     * NUMBER
     */
    @Column(name = "CODIGOPAIS", nullable = true)
    private Long codigoPais;

    /**
     * CEP
     * VARCHAR2(9)
     */
    @Column(name = "CEP", nullable = true, length = 9)
    private String cep;

    /**
     * CÓDIGO NATUREZA JURÍDICA
     * VARCHAR2(50)
     */
    @Column(name = "CODIGONATUREZAJURIDICA", nullable = true, length = 50)
    private String codigoNaturezaJuridica;

    /**
     * NOME NATUREZA JURÍDICA
     * VARCHAR2(50)
     */
    @Column(name = "NOMENATUREZAJURIDICA", nullable = true, length = 50)
    private String nomeNaturezaJuridica;

    /**
     * SITUAÇÃO CADASTRAL
     * CHAR(5)
     */
    @Column(name = "SITUACAOCADASTRAL", nullable = true, length = 5)
    private String situacaoCadastral;

    /**
     * DATA CONSTITUIÇÃO
     * VARCHAR2(20)
     */
    @Column(name = "DATACONSTITUICAO", nullable = true, length = 20)
    private String dataConstituicao;

    /**
     * RENOVAÇÃO AUTOMÁTICA
     * CHAR(1)
     */
    @Column(name = "RENOVACAOAUTOMATICA", nullable = true, length = 1)
    private String renovacaoAutomatica;

    /**
     * DATA VIGÊNCIA
     * VARCHAR2(20)
     */
    @Column(name = "DATAVIGENCIA", nullable = true, length = 20)
    private String dataVigencia;

    /**
     * MÊS ANIVERSÁRIO REAJUSTE
     * NUMBER
     */
    @Column(name = "MESANIVERSARIREAJUSTE", nullable = true)
    private Long mesAniversarioReajuste;

    /**
     * CÓDIGO USUÁRIO
     * NUMBER
     */
    @Column(name = "CODIGOUSUARIO", nullable = true)
    private Long codigoUsuario;

    /**
     * SISTEMA
     * CHAR(10)
     */
    @Column(name = "SISTEMA", nullable = true, length = 10)
    private String sistema;

    /**
     * CÓDIGO GRUPO GERENCIAL
     * NUMBER
     */
    @Column(name = "CODIGOGRUPOGERENCIAL", nullable = true)
    private Long codigoGrupoGerencial;

    /**
     * CÓDIGO GRAU PARENTESCO
     * CHAR(121)
     */
    @Column(name = "CODIGOGRAUPARENTESCO", nullable = true, length = 121)
    private String codigoGrauParentesco;
}
