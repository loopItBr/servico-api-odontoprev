package com.odontoPrev.odontoPrev.domain.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * ENTIDADE QUE REPRESENTA UM BENEFICIÁRIO DA ODONTOPREV
 *
 * Esta classe armazena todas as informações de um beneficiário (pessoa que
 * tem direito ao plano odontológico) cadastrado no sistema da OdontoPrev.
 *
 * QUANDO É USADA:
 * - Ao buscar beneficiários do banco de dados para sincronização
 * - Ao salvar resposta da OdontoPrev após cadastro bem-sucedido
 * - Ao atualizar status de sincronização de um beneficiário
 *
 * CAMPOS PRINCIPAIS:
 * - cdAssociado: Número da carteirinha gerado pela OdontoPrev
 * - codigoMatricula: Matrícula do beneficiário no sistema Tasy
 * - cpf: CPF do beneficiário
 * - statusSincronizacao: Se já foi enviado para OdontoPrev ou não
 */
@Entity
@Table(name = "TB_BENEFICIARIO_ODONTOPREV", schema = "TASY",
       uniqueConstraints = {
           @UniqueConstraint(name = "uk_beneficiario_matricula", columnNames = "codigo_matricula"),
           @UniqueConstraint(name = "uk_beneficiario_cpf", columnNames = "cpf"),
           @UniqueConstraint(name = "uk_beneficiario_cd_associado", columnNames = "cd_associado")
       })
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BeneficiarioOdontoprev {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    /**
     * Código do associado (carteirinha) gerado pela OdontoPrev
     * Este campo é preenchido após cadastro bem-sucedido na OdontoPrev
     */
    @Column(name = "cd_associado", length = 50)
    private String cdAssociado;

    /**
     * Código da matrícula do beneficiário no sistema Tasy
     * Identificador único do beneficiário no sistema de origem
     */
    @Column(name = "codigo_matricula", length = 50, nullable = false)
    private String codigoMatricula;

    /**
     * CPF do beneficiário (sem formatação)
     */
    @Column(name = "cpf", length = 11, nullable = false)
    private String cpf;

    /**
     * Nome completo do beneficiário
     */
    @Column(name = "nome_beneficiario", length = 255, nullable = false)
    private String nomeBeneficiario;

    /**
     * Data de nascimento do beneficiário
     */
    @Column(name = "data_nascimento", nullable = false)
    private LocalDate dataNascimento;

    /**
     * Data de vigência retroativa (início do direito ao plano)
     */
    @Column(name = "dt_vigencia_retroativa")
    private LocalDate dtVigenciaRetroativa;

    /**
     * Nome da mãe do beneficiário
     */
    @Column(name = "nome_mae", length = 255)
    private String nomeMae;

    /**
     * Sexo do beneficiário (M/F)
     */
    @Column(name = "sexo", length = 1, nullable = false)
    private String sexo;

    /**
     * Telefone celular do beneficiário
     */
    @Column(name = "telefone_celular", length = 15)
    private String telefoneCelular;

    /**
     * Telefone residencial do beneficiário
     */
    @Column(name = "telefone_residencial", length = 15)
    private String telefoneResidencial;

    /**
     * E-mail do beneficiário
     */
    @Column(name = "email", length = 255)
    private String email;

    /**
     * Código da empresa à qual o beneficiário está vinculado
     */
    @Column(name = "codigo_empresa", length = 50, nullable = false)
    private String codigoEmpresa;

    /**
     * Código do plano contratado
     */
    @Column(name = "codigo_plano", length = 50, nullable = false)
    private String codigoPlano;

    /**
     * Departamento do beneficiário na empresa
     */
    @Column(name = "departamento", length = 100)
    private String departamento;

    /**
     * CEP do endereço
     */
    @Column(name = "cep", length = 8, nullable = false)
    private String cep;

    /**
     * Logradouro do endereço
     */
    @Column(name = "logradouro", length = 255, nullable = false)
    private String logradouro;

    /**
     * Número do endereço
     */
    @Column(name = "numero", length = 20, nullable = false)
    private String numero;

    /**
     * Complemento do endereço
     */
    @Column(name = "complemento", length = 100)
    private String complemento;

    /**
     * Bairro do endereço
     */
    @Column(name = "bairro", length = 100)
    private String bairro;

    /**
     * Cidade do endereço
     */
    @Column(name = "cidade", length = 100, nullable = false)
    private String cidade;

    /**
     * Tipo de endereço
     */
    @Column(name = "tp_endereco")
    private Long tpEndereco;

    /**
     * UF do endereço (sigla do estado)
     */
    @Column(name = "uf", length = 2, nullable = false)
    private String uf;

    /**
     * RG do beneficiário
     */
    @Column(name = "rg", length = 20)
    private String rg;

    /**
     * Órgão emissor do RG
     */
    @Column(name = "rg_emissor", length = 20)
    private String rgEmissor;

    /**
     * Estado civil do beneficiário
     */
    @Column(name = "estado_civil", length = 20)
    private String estadoCivil;

    /**
     * Cargo do beneficiário na empresa
     */
    @Column(name = "nm_cargo", length = 100)
    private String nmCargo;

    /**
     * Grau de parentesco (para dependentes)
     */
    @Column(name = "grau_parentesco", length = 50)
    private String grauParentesco;

    /**
     * PIS/PASEP do beneficiário
     */
    @Column(name = "pis_pasep", length = 20)
    private String pisPasep;

    /**
     * Cartão Nacional de Saúde (CNS)
     */
    @Column(name = "cns", length = 15)
    private String cns;

    /**
     * Número da sequência da view de origem
     */
    @Column(name = "nr_sequencia")
    private Long nrSequencia;

    /**
     * CGC do estipulante (CNPJ da empresa)
     */
    @Column(name = "cd_cgc_estipulante", length = 14)
    private String cdCgcEstipulante;

    /**
     * Identificação do tipo de beneficiário
     * T = Titular, D = Dependente
     */
    @Column(name = "identificacao", length = 1)
    private String identificacao;

    /**
     * CÓDIGO DO ASSOCIADO TITULAR (TEMPORÁRIO - NÃO PERSISTIDO)
     * 
     * Usado apenas durante o processamento para dependentes.
     * Valor vem da view e não é persistido no banco.
     */
    @jakarta.persistence.Transient
    private String codigoAssociadoTitularTemp;

    /**
     * USUÁRIO (TEMPORÁRIO - NÃO PERSISTIDO)
     * 
     * Usado apenas durante o processamento.
     * Valor vem da view e não é persistido no banco.
     */
    @jakarta.persistence.Transient
    private Long usuarioTemp;

    /**
     * PARENTESCO (TEMPORÁRIO - NÃO PERSISTIDO)
     * 
     * Usado apenas durante o processamento de dependentes.
     * Valor vem da view (campo PARENTESCO) e não é persistido no banco.
     */
    @jakarta.persistence.Transient
    private Long parentescoTemp;

    /**
     * Status de sincronização com a OdontoPrev
     * PENDENTE / SINCRONIZADO / ERRO / ALTERADO / EXCLUIDO
     */
    @Column(name = "status_sincronizacao", length = 20)
    private String statusSincronizacao;

    /**
     * Data da última sincronização com a OdontoPrev
     */
    @Column(name = "data_sincronizacao")
    private LocalDateTime dataSincronizacao;

    /**
     * Mensagem de erro da última tentativa de sincronização
     */
    @Column(name = "mensagem_erro", columnDefinition = "TEXT")
    private String mensagemErro;

    /**
     * Motivo de exclusão (quando aplicável)
     */
    @Column(name = "motivo_exclusao", length = 100)
    private String motivoExclusao;

    /**
     * ID do motivo de inativação para a OdontoPrev
     */
    @Column(name = "id_motivo_inativacao")
    private Integer idMotivoInativacao;

    /**
     * Data de inativação do beneficiário
     */
    @Column(name = "data_inativacao")
    private LocalDate dataInativacao;

    /**
     * Data e hora de criação do registro
     */
    @CreationTimestamp
    @Column(name = "data_criacao", nullable = false, updatable = false)
    private LocalDateTime dataCriacao;

    /**
     * Data e hora da última atualização do registro
     */
    @UpdateTimestamp
    @Column(name = "data_atualizacao", nullable = false)
    private LocalDateTime dataAtualizacao;
}
