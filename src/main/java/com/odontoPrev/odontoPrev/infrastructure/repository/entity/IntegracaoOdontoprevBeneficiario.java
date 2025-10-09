package com.odontoPrev.odontoPrev.infrastructure.repository.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import org.hibernate.annotations.Immutable;

/**
 * ENTIDADE QUE REPRESENTA OS DADOS DE INCLUSÃO DE BENEFICIÁRIOS PARA INTEGRAÇÃO COM ODONTOPREV
 *
 * FUNÇÃO PRINCIPAL:
 * Esta classe representa a VIEW VW_INTEGRACAO_ODONTOPREV_BENEFICIARIOS.sql que consolida todas as
 * informações necessárias sobre beneficiários novos que precisam ser cadastrados
 * na OdontoPrev durante a sincronização.
 *
 * IMPORTANTE - É UMA VIEW, NÃO UMA TABELA:
 * - Esta entidade mapeia uma VIEW (vw_integracao_odontoprev_beneficiarios)
 * - Views são "consultas salvas" que juntam dados de várias tabelas
 * - Por isso usa @Immutable - os dados não podem ser alterados
 * - Apenas consultamos os dados, não inserimos/atualizamos
 *
 * ESTRUTURA DOS DADOS:
 * A view consolida informações de:
 * 1. BENEFICIÁRIO: CPF, nome, data nascimento, sexo
 * 2. ENDEREÇO: CEP, logradouro, número, bairro, cidade, UF
 * 3. CONTATO: telefones, email
 * 4. EMPRESA: código empresa, matrícula, departamento
 * 5. PLANO: código plano, vigência
 * 6. DOCUMENTOS: RG, PIS/PASEP, CNS
 *
 * USO NO SISTEMA:
 * 1. Sistema consulta esta view para obter lista de beneficiários novos
 * 2. Para cada beneficiário, busca seus dados completos
 * 3. Envia estes dados para API da OdontoPrev
 * 4. Salva controle do que foi enviado
 *
 * VALIDAÇÕES:
 * Mesmo sendo uma view (dados já validados), mantemos validações para:
 * - Garantir integridade na integração
 * - Facilitar debug de problemas
 * - Proteger contra dados corrompidos
 *
 * EXEMPLO DE REGISTRO:
 * Um funcionário "João Silva" com CPF "123.456.789-00" que foi
 * contratado e precisa ser incluído no plano odontológico empresarial.
 */
@Entity
@Table(name = "vw_integracao_odontoprev_beneficiarios", schema = "TASY")
@Immutable  // Indica que esta entidade é apenas para leitura (VIEW)
@Data
@NoArgsConstructor
@AllArgsConstructor
public class IntegracaoOdontoprevBeneficiario {

    /**
     * CÓDIGO DA MATRÍCULA DO FUNCIONÁRIO (CHAVE PRIMÁRIA)
     *
     * Matrícula única do beneficiário na empresa.
     * Usado para identificar o funcionário no sistema.
     */
    @Id
    @Column(name = "CODIGOMATRICULA", nullable = false, length = 7)
    @NotBlank(message = "Código da matrícula é obrigatório")
    private String codigoMatricula;

    /**
     * CÓDIGO DA EMPRESA DO BENEFICIÁRIO
     *
     * Código que identifica a empresa onde o beneficiário trabalha.
     */
    @Column(name = "CODIGOEMPRESA", length = 6)
    @NotBlank(message = "Código da empresa é obrigatório")
    private String codigoEmpresa;

    /**
     * CÓDIGO DO PLANO ODONTOLÓGICO
     *
     * Identifica qual plano o beneficiário possui.
     */
    @Column(name = "CODIGOPLANO")
    private Long codigoPlano;

    /**
     * CPF DO BENEFICIÁRIO
     *
     * Documento de identificação pessoal do beneficiário.
     */
    @Column(name = "CPF", length = 14)
    @NotBlank(message = "CPF é obrigatório")
    private String cpf;

    /**
     * DATA DE NASCIMENTO DO BENEFICIÁRIO
     *
     * Data de nascimento usada para cálculos de idade e elegibilidade.
     */
    @Column(name = "DATADENASCIMENTO", length = 10)
    private String dataDeNascimento;

    /**
     * DATA DE VIGÊNCIA RETROATIVA
     *
     * Data a partir da qual o beneficiário tem direito ao plano.
     */
    @Column(name = "DTVIGENCIARETROATIVA", length = 10)
    private String dtVigenciaRetroativa;

    /**
     * NOME DO BENEFICIÁRIO
     *
     * Nome completo do beneficiário.
     */
    @Column(name = "NOMEDOBENEFICIARIO", length = 200)
    @NotBlank(message = "Nome do beneficiário é obrigatório")
    private String nomeDoBeneficiario;

    /**
     * NOME DA MÃE DO BENEFICIÁRIO
     *
     * Nome completo da mãe do beneficiário.
     */
    @Column(name = "NOMEDAMAE", length = 200)
    private String nomeDaMae;

    /**
     * SEXO DO BENEFICIÁRIO
     *
     * M = Masculino, F = Feminino
     */
    @Column(name = "SEXO", length = 1)
    private String sexo;

    /**
     * IDENTIFICAÇÃO DO TIPO DE BENEFICIÁRIO
     *
     * T = Titular, D = Dependente
     */
    @Column(name = "IDENTIFICACAO", length = 1)
    private String identificacao;

    /**
     * REGISTRO GERAL (RG) DO BENEFICIÁRIO
     *
     * Documento de identidade civil.
     */
    @Column(name = "RG", length = 20)
    private String rg;

    /**
     * ÓRGÃO EMISSOR DO RG
     *
     * Órgão que emitiu o documento de identidade.
     */
    @Column(name = "RGEMISSOR", length = 40)
    private String rgEmissor;

    /**
     * ESTADO CIVIL DO BENEFICIÁRIO
     *
     * Estado civil do beneficiário.
     */
    @Column(name = "ESTADOCIVIL", length = 50)
    private String estadoCivil;

    /**
     * CARGO DO BENEFICIÁRIO NA EMPRESA
     *
     * Função ou cargo que o beneficiário ocupa.
     */
    @Column(name = "NMCARGO", length = 100)
    private String nmCargo;

    /**
     * CARTÃO NACIONAL DE SAÚDE (CNS)
     *
     * Número do cartão SUS do beneficiário.
     */
    @Column(name = "CNS", length = 20)
    private String cns;

    /**
     * LOGRADOURO DO ENDEREÇO DO BENEFICIÁRIO
     *
     * Nome da rua, avenida, travessa, etc.
     */
    @Column(name = "LOGRADOURO", length = 40)
    private String logradouro;

    /**
     * NÚMERO DO ENDEREÇO DO BENEFICIÁRIO
     */
    @Column(name = "NUMERO", length = 10)
    private String numero;

    /**
     * COMPLEMENTO DO ENDEREÇO
     *
     * Informações adicionais do endereço (apto, bloco, etc).
     */
    @Column(name = "COMPLEMENTO", length = 100)
    private String complemento;

    /**
     * BAIRRO DO ENDEREÇO DO BENEFICIÁRIO
     */
    @Column(name = "BAIRRO", length = 100)
    private String bairro;

    /**
     * CEP DO ENDEREÇO DO BENEFICIÁRIO
     *
     * Código de endereçamento postal do beneficiário.
     */
    @Column(name = "CEP", length = 9)
    private String cep;

    /**
     * CIDADE DE RESIDÊNCIA DO BENEFICIÁRIO
     */
    @Column(name = "CIDADE", length = 100)
    private String cidade;

    /**
     * UNIDADE FEDERATIVA (ESTADO) DO BENEFICIÁRIO
     */
    @Column(name = "UF", length = 2)
    private String uf;

    /**
     * TIPO DE ENDEREÇO
     *
     * Código do tipo de endereço.
     */
    @Column(name = "TPENDERECO", length = 10)
    private String tpEndereco;

    /**
     * CIDADE DO BENEFICIÁRIO
     *
     * Cidade específica do beneficiário.
     */
    @Column(name = "CIDADEBENEFICIARIO", length = 100)
    private String cidadeBeneficiario;

    /**
     * TELEFONE CELULAR DO BENEFICIÁRIO
     *
     * Telefone móvel para contato.
     */
    @Column(name = "TELEFONECELULAR", length = 43)
    private String telefoneCelular;

    /**
     * TELEFONE RESIDENCIAL DO BENEFICIÁRIO
     *
     * Telefone fixo para contato.
     */
    @Column(name = "TELEFONERESIDENCIAL", length = 18)
    private String telefoneResidencial;

    /**
     * DEPARTAMENTO DO BENEFICIÁRIO NA EMPRESA
     *
     * Setor ou departamento onde o funcionário trabalha.
     */
    @Column(name = "DEPARTAMENTO")
    private Long departamento;

    /**
     * NÚMERO DO BANCO
     *
     * Código do banco para dados bancários.
     */
    @Column(name = "NRBANCO", length = 10)
    private String nrBanco;

    /**
     * NÚMERO DA AGÊNCIA
     *
     * Número da agência bancária.
     */
    @Column(name = "NRAGENCIA", length = 10)
    private String nrAgencia;

    /**
     * NÚMERO DA CONTA
     *
     * Número da conta bancária.
     */
    @Column(name = "NRCONTA", length = 20)
    private String nrConta;

    /**
     * DÍGITO DA CONTA
     *
     * Dígito verificador da conta bancária.
     */
    @Column(name = "DIGCONTA", length = 5)
    private String digConta;

    /**
     * TIPO DE CONTA
     *
     * Tipo da conta bancária (CC = Conta Corrente).
     */
    @Column(name = "TIPOCONTA", length = 2)
    private String tipoConta;

    /**
     * USUÁRIO QUE CADASTROU O BENEFICIÁRIO
     *
     * Código do usuário responsável pelo cadastro.
     */
    @Column(name = "USUARIO")
    private Long usuario;

    /**
     * NÚMERO DE SEQUÊNCIA DO SEGURADO NO SISTEMA TASY
     *
     * Identificador sequencial único do segurado no sistema de origem.
     */
    @Column(name = "NR_SEQUENCIA")
    private Long nrSequencia;

    /**
     * CÓDIGO DA PESSOA FÍSICA NO SISTEMA TASY
     *
     * Identificador da pessoa física no sistema de origem.
     */
    @Column(name = "CD_PESSOA_FISICA", length = 10)
    private String cdPessoaFisica;

    /**
     * CNPJ DO ESTIPULANTE (EMPRESA CONTRATANTE)
     *
     * CNPJ da empresa que contratou o plano odontológico.
     */
    @Column(name = "CD_CGC_ESTIPULANTE", length = 14)
    private String cdCgcEstipulante;
}