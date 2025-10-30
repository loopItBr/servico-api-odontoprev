package com.odontoPrev.odontoPrev.infrastructure.repository.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Pattern;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import org.hibernate.annotations.Immutable;


/**
 * ENTIDADE QUE REPRESENTA OS DADOS DE INTEGRAÇÃO COM ODONTOPREV
 * 
 * FUNÇÃO PRINCIPAL:
 * Esta classe representa uma VIEW do banco de dados que consolida todas as
 * informações necessárias sobre empresas, planos e contratos para enviar
 * à OdontoPrev durante a sincronização.
 * 
 * IMPORTANTE - É UMA VIEW, NÃO UMA TABELA:
 * - Esta entidade mapeia uma VIEW (vw_integracao_odontoprev)
 * - Views são "consultas salvas" que juntam dados de várias tabelas
 * - Por isso usa @Immutable - os dados não podem ser alterados
 * - Apenas consultamos os dados, não inserimos/atualizamos
 * 
 * ESTRUTURA DOS DADOS:
 * A view consolida informações de:
 * 1. EMPRESA: código, CNPJ, nome fantasia, datas de contrato
 * 2. PLANOS: códigos, descrições, valores, datas de vigência
 * 3. COBRANÇA: tipo, banco, parcelas
 * 4. MÉTRICAS: vidas ativas, faturamento, sinistralidade
 * 
 * USO NO SISTEMA:
 * 1. Sistema consulta esta view para obter lista de empresas
 * 2. Para cada empresa, busca seus dados completos
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
 * Uma empresa "ACME Ltda" com CNPJ "12.345.678/0001-90" que tem
 * plano "Básico Empresarial" com 150 vidas ativas e faturamento
 * de R$ 45.000,00 no último mês.
 */
@Entity
@Table(name = "VW_INTEGRACAO_ODONTOPREV", schema = "TASY")
@Immutable  // Indica que esta entidade é apenas para leitura (VIEW)
@Data
@NoArgsConstructor
@AllArgsConstructor
public class IntegracaoOdontoprev {

    /**
     * NÚMERO SEQUENCIAL DO CONTRATO
     * 
     * Identificador único do contrato da empresa na view de inclusão.
     * Este campo é usado na procedure SS_PLS_CAD_CODEMPRESA_ODONTOPREV.
     * 
     * IMPORTANTE:
     * - Este campo é a chave primária para empresas não processadas
     * - Empresas já processadas têm CODIGO_EMPRESA preenchido
     * - Serve como identificador único para inclusão
     */
    @Id
    @Column(name = "NR_SEQ_CONTRATO", nullable = true)
    private Long nrSeqContrato;

    /**
     * CÓDIGO DA EMPRESA NA OPERADORA ODONTOPREV
     * 
     * Quando a empresa já existe na OdontoPrev, ela recebe um código específico
     * que a identifica unicamente no sistema da operadora.
     * 
     * IMPORTANTE:
     * - Este campo vem da resposta da API após o POST
     * - É usado para identificar a empresa em operações futuras
     * - Para empresas não processadas, este campo é NULL
     */
    @Column(name = "CODIGO_EMPRESA", nullable = true, length = 20)
    private String codigoEmpresa;



    /**
     * CNPJ DA EMPRESA (CADASTRO NACIONAL DE PESSOA JURÍDICA)
     * 
     * Documento oficial que identifica a empresa junto à Receita Federal.
     * Pode vir formatado (12.345.678/0001-90) ou apenas números (12345678000190).
     * Campo opcional porque algumas empresas podem não ter CNPJ cadastrado ainda.
     */
     @Column(name = "CGC", nullable = true, length = 18)
    @Pattern(regexp = "^\\d{2}\\.\\d{3}\\.\\d{3}/\\d{4}-\\d{2}$|^\\d{14}$", 
             message = "CNPJ deve estar no formato XX.XXX.XXX/XXXX-XX ou XXXXXXXXXXXXXX")
    private String cnpj;


    /**
     * NOME COMERCIAL DA EMPRESA
     * 
     * Nome pelo qual a empresa é conhecida no mercado.
     * Exemplo: "Sabin Medicina Diagnóstica", "Laboratório ABC Ltda"
     */
    @Column(name = "NOMEFANTASIA", nullable = true, length = 20)
    private String nomeFantasia;

    /**
     * DATA DE INÍCIO DA VIGÊNCIA DO CONTRATO
     * Quando o contrato da empresa começou a valer
     */
    @Column(name = "DATAINICIOCONTRATO", nullable = true, length = 20)
    private String dataInicioContrato;

    /**
     * DATA DE VIGÊNCIA ATUAL
     * Data de referência para validar se contrato está ativo
     */
    @Column(name = "DATAVIGENCIA", nullable = true, length = 20)
    private String dataVigencia;

    // Campos organizacionais da estrutura interna
    @Column(name = "CODIGOGRUPOGERENCIAL", nullable = true)
    private Long codigoGrupoGerencial;

    @Column(name = "CODIGOMARCA", nullable = true)
    private Long codigoMarca;

    @Column(name = "CODIGOCELULA", nullable = true)
    private Long codigoCelula;


    // ===== COLUNAS DE PLANO (SUFIXO _1, _2, _3) =====
    @Column(name = "CODIGOPLANO_1", nullable = true)
    private Long codigoPlano1;

    @Column(name = "VALORTITULAR_1", nullable = true)
    private Long valorTitular1;

    @Column(name = "VALORDEPENDENTE_1", nullable = true)
    private Long valorDependente1;

    @Column(name = "DATAINICIOPLANO_1", nullable = true, length = 20)
    private String dataInicioPlano1;

    @Column(name = "VALORREEMBOLSOUO_1", nullable = true)
    private Long valorReembolsoUo1;

    @Column(name = "PERCENTUALDEPENDENTEREDEGENERICA_1", nullable = true)
    private Long percentualDependenteRedeGenerica1;

    @Column(name = "PERCENTUALAGREGADOREDEGENERICA_1", nullable = true)
    private Long percentualAgregadoRedeGenerica1;

    @Column(name = "PERIODICIDADE_1", nullable = true, length = 1)
    private String periodicidade1;

    @Column(name = "PERCENTUALASSOCIADO_1", nullable = true)
    private Long percentualAssociado1;

    @Column(name = "CODIGOREDE_1", nullable = true, length = 45)
    private String codigoRede1;

    @Column(name = "CODIGOPLANO_2", nullable = true)
    private Long codigoPlano2;

    @Column(name = "VALORTITULAR_2", nullable = true)
    private Long valorTitular2;

    @Column(name = "VALORDEPENDENTE_2", nullable = true)
    private Long valorDependente2;

    @Column(name = "DATAINICIOPLANO_2", nullable = true, length = 22)
    private String dataInicioPlano2;

    @Column(name = "VALORREEMBOLSOUO_2", nullable = true)
    private Long valorReembolsoUo2;

    @Column(name = "PERCENTUALDEPENDENTEREDEGENERICA_2", nullable = true)
    private Long percentualDependenteRedeGenerica2;

    @Column(name = "PERCENTUALAGREGADOREDEGENERICA_2", nullable = true)
    private Long percentualAgregadoRedeGenerica2;

    @Column(name = "PERIODICIDADE_2", nullable = true, length = 1)
    private String periodicidade2;

    @Column(name = "PERCENTUALASSOCIADO_2", nullable = true)
    private Long percentualAssociado2;

    @Column(name = "CODIGOREDE_2", nullable = true, length = 45)
    private String codigoRede2;

    @Column(name = "CODIGOPLANO_3", nullable = true)
    private Long codigoPlano3;

    @Column(name = "VALORTITULAR_3", nullable = true)
    private Long valorTitular3;

    @Column(name = "VALORDEPENDENTE_3", nullable = true)
    private Long valorDependente3;

    @Column(name = "DATAINICIOPLANO_3", nullable = true, length = 22)
    private String dataInicioPlano3;

    @Column(name = "VALORREEMBOLSOUO_3", nullable = true)
    private Long valorReembolsoUo3;

    @Column(name = "PERCENTUALDEPENDENTEREDEGENERICA_3", nullable = true)
    private Long percentualDependenteRedeGenerica3;

    @Column(name = "PERCENTUALAGREGADOREDEGENERICA_3", nullable = true)
    private Long percentualAgregadoRedeGenerica3;

    @Column(name = "PERIODICIDADE_3", nullable = true, length = 1)
    private String periodicidade3;

    @Column(name = "PERCENTUALASSOCIADO_3", nullable = true)
    private Long percentualAssociado3;

    @Column(name = "CODIGOREDE_3", nullable = true, length = 45)
    private String codigoRede3;


    // === CAMPOS ADICIONAIS DA VIEW ===
    
    @Column(name = "SISTEMA", nullable = true, length = 10)
    private String sistema;

    @Column(name = "TIPOPESSOA", nullable = true, length = 1)
    private String tipoPessoa;

    @Column(name = "EMITECARTEIRINHAPLASTICA", nullable = true, length = 1)
    private String emiteCarteirinhaPlastica;

    @Column(name = "CODIGOEMPRESAGESTORA", nullable = true)
    private Long codigoEmpresaGestora;

    @Column(name = "CODIGOFILIALEMPRESAGESTORA", nullable = true)
    private Long codigoFilialEmpresaGestora;

    @Column(name = "CODIGONATUREZAJURIDICA", nullable = true, length = 50)
    private String codigoNaturezaJuridica;

    @Column(name = "NOMENATUREZAJURIDICA", nullable = true, length = 50)
    private String nomeNaturezaJuridica;

    @Column(name = "SITUACAOCADASTRAL", nullable = true, length = 5)
    private String situacaoCadastral;

    @Column(name = "INSCRICAOMUNICIPAL", nullable = true, length = 20)
    private String inscricaoMunicipal;

    @Column(name = "INSCRICAOESTADUAL", nullable = true, length = 20)
    private String inscricaoEstadual;

    @Column(name = "DATACONSTITUICAO", nullable = true, length = 20)
    private String dataConstituicao;

    @Column(name = "RENOVACAOAUTOMATICA", nullable = true, length = 1)
    private String renovacaoAutomatica;

    @Column(name = "CODIGOCLAUSULAREAJUSTEDIFERENCIADO", nullable = true)
    private Long codigoClausulaReajusteDiferenciado;

    @Column(name = "DEPARTAMENTO", nullable = true)
    private Long departamento;

    @Column(name = "DEPENDENTEPAGA", nullable = true, length = 1)
    private String dependentePaga;

    @Column(name = "PERMISSAOCADASTRODEP", nullable = true, length = 1)
    private String permissaoCadastroDep;

    @Column(name = "NUMEROMINIMOASSOCIADOS", nullable = true)
    private Long numeroMinimoAssociados;

    @Column(name = "NUMEROFUNCIONARIOS", nullable = true)
    private Long numeroFuncionarios;

    @Column(name = "NUMERODEPENDENTES", nullable = true)
    private Long numeroDependentes;

    @Column(name = "IDADELIMITEDEPENDENTE", nullable = true)
    private Long idadeLimiteDependente;

    @Column(name = "VALORFATOR", nullable = true)
    private Long valorFator;

    @Column(name = "CODIGOLAYOUTCARTEIRINHA", nullable = true, length = 1)
    private String codigoLayoutCarteirinha;

    @Column(name = "CODIGOORDEMCARTEIRA", nullable = true)
    private Long codigoOrdemCarteira;

    @Column(name = "DIAVENCIMENTOAG", nullable = true)
    private Long diaVencimentoAg;

    @Column(name = "CODIGOPERFILCLIENTEFATURA", nullable = true)
    private Long codigoPerfilClienteFatura;

    @Column(name = "CODIGOBANCOFATURA", nullable = true)
    private Long codigoBancoFatura;

    @Column(name = "MULTAFATURA", nullable = true)
    private Long multaFatura;

    @Column(name = "DESCONTAIR", nullable = true, length = 1)
    private String descontaIr;

    @Column(name = "RETENCAOISS", nullable = true, length = 1)
    private String retencaoIss;

    @Column(name = "LIBERASENHAINTERNET", nullable = true, length = 1)
    private String liberaSenhaInternet;

    @Column(name = "FATURAMENTONOTACORTE", nullable = true, length = 1)
    private String faturamentoNotaCorte;

    @Column(name = "PRORATA", nullable = true, length = 1)
    private String prorata;

    @Column(name = "CUSTOFAMILIAR", nullable = true, length = 1)
    private String custoFamiliar;

    @Column(name = "PLANOFAMILIAR", nullable = true, length = 1)
    private String planoFamiliar;

    @Column(name = "POSICAOFIMTIT", nullable = true)
    private Long posicaoFimTit;

    @Column(name = "IDADELIMITEUNIVERSITARIA", nullable = true)
    private Long idadeLimiteUniversitario;

    @Column(name = "VALORSINISTROCONTRATO", nullable = true)
    private Long valorSinistroContrato;

    @Column(name = "CODIGOREGIAO", nullable = true)
    private Long codigoRegiao;

    @Column(name = "CODIGOIMAGEMFATURA", nullable = true)
    private Long codigoImagemFatura;

    @Column(name = "CODIGOMOEDA", nullable = true)
    private Long codigoMoeda;

    @Column(name = "POSICAOINITIT", nullable = true)
    private Long posicaoInitTit;

    @Column(name = "RAZAOSOCIAL", nullable = true, length = 45)
    private String razaoSocial;

    @Column(name = "DIAINICIOFATURAMENTO", nullable = true)
    private Long diaInicioFaturamento;

    @Column(name = "CODIGOUSUARIOCONSULTOR", nullable = true)
    private Long codigoUsuarioConsultor;

    @Column(name = "MESANIVERSARIREAJUSTE", nullable = true)
    private Long mesAniversarioReajuste;

    @Column(name = "DESCRICAORAMOATIVIDADE", nullable = true, length = 15)
    private String descricaoRamoAtividade;

    @Column(name = "DIAVENCIMENTO", nullable = true)
    private Long diaVencimento;

    @Column(name = "CNAE", nullable = true, length = 50)
    private String cnae;

    @Column(name = "CODIGOMANUAL", nullable = true)
    private Long codigoManual;

    @Column(name = "CODIGOGRAUPARENTESCO", nullable = true, length = 121)
    private String codigoGrauParentesco;

    @Column(name = "CODSEQUENCIAL", nullable = true)
    private Long codSequencial;

    @Column(name = "EMAILCONTATOFATURA", nullable = true, length = 32)
    private String emailContatoFatura;

    @Column(name = "NOMECONTATOFATURA", nullable = true, length = 14)
    private String nomeContatoFatura;

    @Column(name = "EMAIL", nullable = true, length = 32)
    private String email;

    @Column(name = "TIPOLOGRADOURO", nullable = true)
    private Long tipoLogradouro;

    @Column(name = "LOGRADOURO", nullable = true, length = 40)
    private String logradouro;

    @Column(name = "NUMERO", nullable = true, length = 10)
    private String numero;

    @Column(name = "BAIRRO", nullable = true, length = 40)
    private String bairro;

    @Column(name = "CODIGO", nullable = true, length = 10)
    private String codigo;

    @Column(name = "NOMECIDADE", nullable = true, length = 40)
    private String nomeCidade;

    @Column(name = "SIGLAUF", nullable = true, length = 15)
    private String siglaUf;

    @Column(name = "CODIGOPAIS", nullable = true)
    private Long codigoPais;

    @Column(name = "CEP", nullable = true, length = 9)
    private String cep;

    @Column(name = "NOMECOBRANCA", nullable = true, length = 11)
    private String nomeCobranca;

    @Column(name = "TIPOLOGRADOUROCOBRANCA", nullable = true)
    private Long tipoLogradouroCobranca;

    @Column(name = "LOGRADOUROCOBRANCA", nullable = true, length = 24)
    private String logradouroCobranca;

    @Column(name = "NUMEROCOBRANCA", nullable = true)
    private Long numeroCobranca;

    @Column(name = "BAIRROCOBRANCA", nullable = true, length = 10)
    private String bairroCobranca;

    @Column(name = "CODIGOCOBRANCA", nullable = true)
    private Long codigoCobranca;

    @Column(name = "NOMECIDADECOBRANCA", nullable = true, length = 12)
    private String nomeCidadeCobranca;

    @Column(name = "SIGLAUFCOBRANCA", nullable = true, length = 2)
    private String siglaUfCobranca;

    @Column(name = "CODIGOPAISCOBRANCA", nullable = true)
    private Long codigoPaisCobranca;

    @Column(name = "CEPCOBRANCA", nullable = true, length = 9)
    private String cepCobranca;

    @Column(name = "CGCCOBRANCA", nullable = true, length = 18)
    private String cgcCobranca;

    @Column(name = "DIAMOVASSOCIADOEMPRESA", nullable = true)
    private Long diaMovAssociadoEmpresa;

    @Column(name = "CARGOCONTATO", nullable = true, length = 2)
    private String cargoContato;

    @Column(name = "NOMECONTATO", nullable = true, length = 13)
    private String nomeContato;

    @Column(name = "EMAILCONTATO", nullable = true, length = 32)
    private String emailContato;

    @Column(name = "DEPARTAMENTOCONTATO", nullable = true, length = 2)
    private String departamentoContato;

    @Column(name = "ID", nullable = true)
    private Long id;

    @Column(name = "DESCRICAO", nullable = true, length = 6)
    private String descricao;

    @Column(name = "CNPJCORRETOR", nullable = true, length = 14)
    private String cnpjCorretor;

    @Column(name = "CODIGOREGRA", nullable = true)
    private Long codigoRegra;

    @Column(name = "NUMEROPARCELADE", nullable = true)
    private Long numeroParcelaDe;

    @Column(name = "NUMEROPARCELAATE", nullable = true)
    private Long numeroParcelaAte;

    @Column(name = "PORCENTAGEM", nullable = true)
    private Long porcentagem;

    @Column(name = "CODIGOGRUPO", nullable = true)
    private Long codigoGrupo;
}