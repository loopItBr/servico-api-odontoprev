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
 * Esta classe representa a VIEW VW_INTEGRACAO_ODONTOPREV_BENEFICIARIOS_ALT que consolida
 * informações sobre beneficiários que tiveram seus dados alterados e precisam ser
 * atualizados na OdontoPrev durante a sincronização.
 *
 * IMPORTANTE - É UMA VIEW, NÃO UMA TABELA:
 * - Esta entidade mapeia uma VIEW (VW_INTEGRACAO_ODONTOPREV_BENEFICIARIOS_ALT)
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
@Table(name = "VW_INTEGRACAO_ODONTOPREV_BENEFICIARIOS_ALT", schema = "TASY")
@Immutable  // Indica que esta entidade é apenas para leitura (VIEW)
@Data
@NoArgsConstructor
@AllArgsConstructor
public class IntegracaoOdontoprevBeneficiarioAlteracao {

    /**
     * CÓDIGO DA EMPRESA
     */
    @Id
    @Column(name = "CDEMPRESA", nullable = false, length = 6)
    @NotBlank(message = "Código da empresa é obrigatório")
    private String cdEmpresa;

    /**
     * CÓDIGO DO ASSOCIADO NA ODONTOPREV
     */
    @Column(name = "CDASSOCIADO", length = 4000)
    private String cdAssociado;

    /**
     * CÓDIGO DO PLANO ODONTOLÓGICO
     */
    @Column(name = "CODIGOPLANO")
    private Long codigoPlano;

    /**
     * DÍGITO DA AGÊNCIA
     */
    @Column(name = "DIGAGENCIA", length = 4000)
    private String digAgencia;

    /**
     * DÍGITO DA CONTA
     */
    @Column(name = "DIGCONTA", length = 4000)
    private String digConta;

    /**
     * NÚMERO DA AGÊNCIA
     */
    @Column(name = "NRAGENCIA", length = 4000)
    private String nrAgencia;

    /**
     * NÚMERO DA CONTA
     */
    @Column(name = "NRCONTA", length = 4000)
    private String nrConta;

    /**
     * NÚMERO DO BANCO
     */
    @Column(name = "NRBANCO", length = 4000)
    private String nrBanco;

    /**
     * DEPARTAMENTO DO BENEFICIÁRIO NA EMPRESA
     */
    @Column(name = "DEPARTAMENTO")
    private Long departamento;

    /**
     * DATA DE VIGÊNCIA RETROATIVA
     */
    @Column(name = "DTVIGENCIARETROATIVA", length = 10)
    private String dtVigenciaRetroativa;

    /**
     * IDENTIFICAÇÃO DO TIPO DE BENEFICIÁRIO
     */
    @Column(name = "IDENTIFICACAO", length = 1)
    private String identificacao;

    /**
     * DATA DE NASCIMENTO DO BENEFICIÁRIO
     */
    @Column(name = "DATANASCIMENTO", length = 4000)
    private String dataNascimento;

    /**
     * MOTIVO DE EXCLUSÃO
     */
    @Column(name = "MOTIVOEXCLUSAO")
    private Long motivoExclusao;

    /**
     * TELEFONE CELULAR DO BENEFICIÁRIO
     */
    @Column(name = "TELEFONECELULAR", length = 43)
    private String telefoneCelular;

    /**
     * TELEFONE RESIDENCIAL DO BENEFICIÁRIO
     */
    @Column(name = "TELEFONERESIDENCIAL", length = 18)
    private String telefoneResidencial;

    /**
     * REGISTRO GERAL (RG) DO BENEFICIÁRIO
     */
    @Column(name = "RG", length = 4000)
    private String rg;

    /**
     * ÓRGÃO EMISSOR DO RG
     */
    @Column(name = "RGEMISSOR", length = 40)
    private String rgEmissor;

    /**
     * ESTADO CIVIL DO BENEFICIÁRIO
     */
    @Column(name = "ESTADOCIVIL", length = 4000)
    private String estadoCivil;

    /**
     * CARGO DO BENEFICIÁRIO NA EMPRESA
     */
    @Column(name = "NMCARGO", length = 4000)
    private String nmCargo;

    /**
     * NOME DA MÃE DO BENEFICIÁRIO
     */
    @Column(name = "NOMEDAMAE", length = 4000)
    private String nomeDaMae;

    /**
     * NÚMERO PIS/PASEP
     */
    @Column(name = "PISPASEP", length = 4000)
    private String pisPasep;

    /**
     * BAIRRO DO ENDEREÇO DO BENEFICIÁRIO
     */
    @Column(name = "BAIRRO", length = 4000)
    private String bairro;

    /**
     * EMAIL DO BENEFICIÁRIO
     */
    @Column(name = "EMAIL", length = 4000)
    private String email;

    /**
     * NOME DO BENEFICIÁRIO
     */
    @Column(name = "NOMEBENEFICIARIO", length = 4000)
    private String nomeBeneficiario;

    /**
     * SEXO DO BENEFICIÁRIO
     */
    @Column(name = "SEXO", length = 1)
    private String sexo;

    /**
     * CEP DO ENDEREÇO DO BENEFICIÁRIO
     */
    @Column(name = "CEP", length = 9)
    private String cep;

    /**
     * CIDADE DE RESIDÊNCIA DO BENEFICIÁRIO
     */
    @Column(name = "CIDADE", length = 4000)
    private String cidade;

    /**
     * LOGRADOURO DO ENDEREÇO DO BENEFICIÁRIO
     */
    @Column(name = "LOGRADOURO", length = 40)
    private String logradouro;

    /**
     * NÚMERO DO ENDEREÇO DO BENEFICIÁRIO
     */
    @Column(name = "NUMERO", length = 4000)
    private String numero;

    /**
     * COMPLEMENTO DO ENDEREÇO
     */
    @Column(name = "COMPLEMENTO", length = 4000)
    private String complemento;

    /**
     * UNIDADE FEDERATIVA (ESTADO) DO BENEFICIÁRIO
     */
    @Column(name = "UF", length = 4000)
    private String uf;
}