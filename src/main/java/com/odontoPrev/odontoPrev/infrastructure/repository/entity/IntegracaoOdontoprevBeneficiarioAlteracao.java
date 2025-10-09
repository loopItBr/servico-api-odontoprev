package com.odontoPrev.odontoPrev.infrastructure.repository.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import org.hibernate.annotations.Immutable;

/**
 * ENTIDADE QUE REPRESENTA OS DADOS DE ALTERAÇÃO DE BENEFICIÁRIOS PARA INTEGRAÇÃO COM ODONTOPREV
 *
 * FUNÇÃO PRINCIPAL:
 * Esta classe representa a VIEW VW_INTEGRACAO_ODONTOPREV_BENEFICIARIOS_ALT.sql que consolida
 * informações sobre beneficiários que tiveram seus dados alterados e precisam ser
 * atualizados na OdontoPrev durante a sincronização.
 *
 * IMPORTANTE - É UMA VIEW, NÃO UMA TABELA:
 * - Esta entidade mapeia uma VIEW (vw_integracao_odontoprev_beneficiarios_alt)
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
 * 7. CÓDIGO DO ASSOCIADO: já cadastrado na OdontoPrev
 *
 * USO NO SISTEMA:
 * 1. Sistema consulta esta view para obter lista de beneficiários alterados
 * 2. Para cada beneficiário, busca seus dados atualizados
 * 3. Envia estes dados para API da OdontoPrev (endpoint /alterar)
 * 4. Salva controle do que foi enviado
 *
 * VALIDAÇÕES:
 * Mesmo sendo uma view (dados já validados), mantemos validações para:
 * - Garantir integridade na integração
 * - Facilitar debug de problemas
 * - Proteger contra dados corrompidos
 *
 * EXEMPLO DE REGISTRO:
 * Um funcionário "João Silva" que mudou de endereço e telefone e precisa
 * ter seus dados atualizados no plano odontológico empresarial.
 */
@Entity
@Table(name = "vw_integracao_odontoprev_beneficiarios_alt", schema = "TASY")
@Immutable  // Indica que esta entidade é apenas para leitura (VIEW)
@Data
@NoArgsConstructor
@AllArgsConstructor
public class IntegracaoOdontoprevBeneficiarioAlteracao {

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
     * Campo obrigatório para alteração (beneficiário deve já existir).
     */
    @Column(name = "CD_ASSOCIADO", length = 50)
    @NotBlank(message = "Código do associado é obrigatório para alteração")
    private String cdAssociado;

    /**
     * CÓDIGO DA EMPRESA DO BENEFICIÁRIO
     *
     * Código que identifica a empresa onde o beneficiário trabalha.
     */
    @Column(name = "CODIGOEMPRESA", length = 10)
    @NotBlank(message = "Código da empresa é obrigatório")
    private String codigoEmpresa;

    /**
     * CÓDIGO DO PLANO ODONTOLÓGICO
     *
     * Identifica qual plano o beneficiário possui.
     */
    @Column(name = "CODIGOPLANO", length = 10)
    private String codigoPlano;

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
    @Column(name = "DATANASCIMENTO", length = 10)
    private String dataNascimento;

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
    @Column(name = "NOMEBENEFICIARIO", length = 200)
    @NotBlank(message = "Nome do beneficiário é obrigatório")
    private String nomeBeneficiario;

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
    @Column(name = "RGEMISSOR", length = 20)
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
    @Column(name = "LOGRADOURO", length = 200)
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
     * CIDADE DO BENEFICIÁRIO
     *
     * Cidade específica do beneficiário.
     */
    @Column(name = "CIDADE_RESIDENCIA", length = 100)
    private String cidadeResidencia;

    /**
     * TELEFONE CELULAR DO BENEFICIÁRIO
     *
     * Telefone móvel para contato.
     */
    @Column(name = "TELEFONECELULAR", length = 15)
    private String telefoneCelular;

    /**
     * TELEFONE RESIDENCIAL DO BENEFICIÁRIO
     *
     * Telefone fixo para contato.
     */
    @Column(name = "TELEFONERESIDENCIAL", length = 15)
    private String telefoneResidencial;

    /**
     * DEPARTAMENTO DO BENEFICIÁRIO NA EMPRESA
     *
     * Setor ou departamento onde o funcionário trabalha.
     */
    @Column(name = "DEPARTAMENTO", length = 100)
    private String departamento;

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
     * DÍGITO DA AGÊNCIA
     *
     * Dígito verificador da agência bancária.
     */
    @Column(name = "DIGAGENCIA", length = 5)
    private String digAgencia;

    /**
     * TIPO DE CONTA
     *
     * Tipo da conta bancária (CC = Conta Corrente).
     */
    @Column(name = "TIPOCONTA", length = 5)
    private String tipoConta;


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
    @Column(name = "CD_PESSOA_FISICA")
    private Long cdPessoaFisica;

    /**
     * CNPJ DO ESTIPULANTE (EMPRESA CONTRATANTE)
     *
     * CNPJ da empresa que contratou o plano odontológico.
     */
    @Column(name = "CD_CGC_ESTIPULANTE", length = 18)
    private String cdCgcEstipulante;

    /**
     * AÇÃO A SER EXECUTADA
     *
     * A = Alteração
     */
    @Column(name = "ACAO", length = 1)
    private String acao;

    /**
     * MOTIVO DE EXCLUSÃO
     *
     * Código do motivo de exclusão/cancelamento.
     */
    @Column(name = "MOTIVOEXCLUSAO")
    private Integer motivoExclusao;

    /**
     * TIPO DE EXCLUSÃO
     *
     * Descrição do tipo de exclusão.
     */
    @Column(name = "TIPO_EXCLUSAO", length = 50)
    private String tipoExclusao;

    /**
     * DATA DE ASSOCIAÇÃO
     *
     * Data em que o beneficiário foi associado ao plano.
     */
    @Column(name = "DATA_ASSOCIACAO", length = 10)
    private String dataAssociacao;

    /**
     * CÓDIGO DO PAÍS EMISSOR
     *
     * Código do país emissor do documento.
     */
    @Column(name = "COD_PAIS_EMISSOR")
    private Integer codPaisEmissor;

    /**
     * CÓDIGO DO MUNICÍPIO IBGE
     *
     * Código IBGE do município.
     */
    @Column(name = "COD_MUNICIPIO_IBGE")
    private Integer codMunicipioIbge;

    /**
     * INDICADOR DE RESIDÊNCIA
     *
     * Indicador se é residência principal.
     */
    @Column(name = "IND_RESIDENCIA")
    private Integer indResidencia;

    /**
     * DECLARAÇÃO DE NASCIDO VIVO (DNV)
     *
     * Número da DNV para nascidos após 2010.
     */
    @Column(name = "DNV", length = 20)
    private String dnv;

    /**
     * NÚMERO DE PORTABILIDADE
     *
     * Número de portabilidade se aplicável.
     */
    @Column(name = "NUM_PORTABILIDADE")
    private Integer numPortabilidade;

    /**
     * TEMPO DE CONTRIBUIÇÃO
     *
     * Tempo de contribuição em meses.
     */
    @Column(name = "TEMPO_CONTRIBUICAO")
    private Integer tempoContribuicao;

    /**
     * DIRETORIO DE PERMANÊNCIA
     *
     * Informação sobre permanência.
     */
    @Column(name = "DIR_PERMANENCIA", length = 50)
    private String dirPermanencia;
}