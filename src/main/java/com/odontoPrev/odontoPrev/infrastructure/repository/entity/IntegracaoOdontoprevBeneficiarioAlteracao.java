package com.odontoPrev.odontoPrev.infrastructure.repository.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import org.hibernate.annotations.Immutable;

import java.util.Date;

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
     * CPF DO BENEFICIÁRIO
     *
     * Documento de identificação pessoal do beneficiário.
     */
    @Column(name = "CPF", length = 14)
    @NotBlank(message = "CPF é obrigatório")
    @Pattern(regexp = "^\\d{3}\\.\\d{3}\\.\\d{3}-\\d{2}$|^\\d{11}$",
             message = "CPF deve estar no formato XXX.XXX.XXX-XX ou XXXXXXXXXXX")
    private String cpf;

    /**
     * DATA DE NASCIMENTO DO BENEFICIÁRIO
     *
     * Data de nascimento usada para cálculos de idade e elegibilidade.
     * IMPORTANTE: Este campo vem como String do banco (formato DD/MM/YYYY)
     * devido à função PLS_OBTER_DADOS_SEGURADO retornar String
     */
    @Column(name = "DATANASCIMENTO")
    private String dataNascimento;

    /**
     * DATA DE VIGÊNCIA RETROATIVA
     *
     * Data a partir da qual o beneficiário tem direito ao plano,
     * podendo ser retroativa à data de admissão.
     * Este campo é uma Date real do banco (DT_CONTRATACAO)
     */
    @Column(name = "DTVIGENCIARETROATIVA")
    @Temporal(TemporalType.DATE)
    private Date dtVigenciaRetroativa;

    /**
     * CEP DO ENDEREÇO DO BENEFICIÁRIO
     *
     * Código de endereçamento postal do beneficiário.
     */
    @Column(name = "CEP", length = 9)
    @Pattern(regexp = "^\\d{5}-\\d{3}$|^\\d{8}$",
             message = "CEP deve estar no formato XXXXX-XXX ou XXXXXXXX")
    private String cep;

    /**
     * CIDADE DE RESIDÊNCIA DO BENEFICIÁRIO
     */
    @Column(name = "CIDADE", length = 100)
    private String cidade;

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
     * UNIDADE FEDERATIVA (ESTADO) DO BENEFICIÁRIO
     */
    @Column(name = "UF", length = 2)
    @Pattern(regexp = "^[A-Z]{2}$", message = "UF deve conter 2 letras maiúsculas")
    private String uf;

    /**
     * NOME COMPLETO DO BENEFICIÁRIO
     *
     * Nome completo conforme documento de identificação.
     */
    @Column(name = "NOMEBENEFICIARIO", length = 200)
    @NotBlank(message = "Nome do beneficiário é obrigatório")
    private String nomeBeneficiario;

    /**
     * NOME DA MÃE DO BENEFICIÁRIO
     *
     * Nome completo da mãe, usado para identificação adicional.
     */
    @Column(name = "NOMEDAMAE", length = 200)
    private String nomeDaMae;

    /**
     * SEXO DO BENEFICIÁRIO
     *
     * M = Masculino, F = Feminino
     */
    @Column(name = "SEXO", length = 1)
    @Pattern(regexp = "^[MF]$", message = "Sexo deve ser M ou F")
    private String sexo;

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
     * USUÁRIO QUE CADASTROU O BENEFICIÁRIO
     *
     * Nome do usuário responsável pelo cadastro.
     */
    @Column(name = "USUARIO", length = 50)
    private String usuario;

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
     * DEPARTAMENTO DO BENEFICIÁRIO NA EMPRESA
     *
     * Setor ou departamento onde o funcionário trabalha.
     */
    @Column(name = "DEPARTAMENTO", length = 100)
    private String departamento;

    /**
     * IDENTIFICAÇÃO DO TIPO DE BENEFICIÁRIO
     *
     * T = Titular, D = Dependente
     */
    @Column(name = "IDENTIFICACAO", length = 1)
    @Pattern(regexp = "^[TD]$", message = "Identificação deve ser T ou D")
    private String identificacao;

    /**
     * REGISTRO GERAL (RG) DO BENEFICIÁRIO
     *
     * Documento de identidade civil.
     */
    @Column(name = "RG", length = 20)
    private String rg;

    /**
     * ESTADO CIVIL DO BENEFICIÁRIO
     *
     * S = Solteiro, C = Casado, D = Divorciado, V = Viúvo
     */
    @Column(name = "ESTADOCIVIL", length = 1)
    private String estadoCivil;

    /**
     * CARGO DO BENEFICIÁRIO NA EMPRESA
     *
     * Função ou cargo que o beneficiário ocupa.
     */
    @Column(name = "NMCARGO", length = 100)
    private String nmCargo;

    /**
     * GRAU DE PARENTESCO
     *
     * Define se é titular ou dependente e qual o parentesco.
     * Ex: TITULAR, CONJUGE, FILHO, etc.
     */
    @Column(name = "GRAUPARENTESCO", length = 50)
    private String grauParentesco;

    /**
     * AÇÃO A SER EXECUTADA
     *
     * A = Alteração (sempre A para esta view)
     */
    @Column(name = "ACAO", length = 1)
    private String acao;

    /**
     * PIS/PASEP DO BENEFICIÁRIO
     *
     * Programa de Integração Social / Programa de Formação do Patrimônio do Servidor Público
     */
    @Column(name = "PIS_PASEP", length = 15)
    private String pisPasep;

    /**
     * DATA DE ASSOCIAÇÃO
     *
     * Data em que o beneficiário foi associado ao plano.
     * Este campo é uma Date real do banco (DT_CONTRATACAO)
     */
    @Column(name = "DATA_ASSOCIACAO")
    @Temporal(TemporalType.DATE)
    private Date dataAssociacao;

    /**
     * BAIRRO DO ENDEREÇO DO BENEFICIÁRIO
     */
    @Column(name = "BAIRRO", length = 100)
    private String bairro;

    /**
     * EMAIL DO BENEFICIÁRIO
     *
     * Endereço de email para comunicações.
     */
    @Column(name = "EMAIL", length = 150)
    @Pattern(regexp = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$",
             message = "Email inválido")
    private String email;

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
     * COMPLEMENTO DO ENDEREÇO
     *
     * Informações adicionais do endereço (apto, bloco, etc).
     */
    @Column(name = "COMPLEMENTO", length = 100)
    private String complemento;

    /**
     * ÓRGÃO EMISSOR DO RG
     *
     * Órgão que emitiu o documento de identidade.
     */
    @Column(name = "RGEMISSOR", length = 20)
    private String rgEmissor;

    /**
     * CARTÃO NACIONAL DE SAÚDE (CNS)
     *
     * Número do cartão SUS do beneficiário.
     */
    @Column(name = "CNS", length = 20)
    private String cns;

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
     * TIPO DE ENDEREÇO
     *
     * Código do tipo de endereço.
     */
    @Column(name = "TPENDERECO")
    private Integer tpEndereco;

    /**
     * CIDADE DE RESIDÊNCIA
     *
     * Nome da cidade de residência.
     */
    @Column(name = "CIDADE_RESIDENCIA", length = 100)
    private String cidadeResidencia;

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
