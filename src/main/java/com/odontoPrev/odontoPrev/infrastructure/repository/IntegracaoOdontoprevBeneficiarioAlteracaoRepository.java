package com.odontoPrev.odontoPrev.infrastructure.repository;

import com.odontoPrev.odontoPrev.infrastructure.repository.entity.IntegracaoOdontoprevBeneficiarioAlteracao;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * REPOSITÓRIO PARA CONSULTA DE BENEFICIÁRIOS PENDENTES DE ALTERAÇÃO
 *
 * Interface que define operações de acesso a dados para a view
 * VW_INTEGRACAO_ODONTOPREV_BENEFICIARIOS_ALT, que contém beneficiários
 * que tiveram seus dados alterados e precisam ser atualizados na OdontoPrev.
 *
 * IMPORTANTE:
 * Esta é uma VIEW (apenas leitura), não uma tabela.
 * Os dados são consultados mas nunca modificados através desta interface.
 */
@Repository
public interface IntegracaoOdontoprevBeneficiarioAlteracaoRepository extends JpaRepository<IntegracaoOdontoprevBeneficiarioAlteracao, String> {

    /**
     * BUSCA TODOS OS BENEFICIÁRIOS PENDENTES DE ALTERAÇÃO
     *
     * @return lista de todos os beneficiários pendentes de alteração
     */
    List<IntegracaoOdontoprevBeneficiarioAlteracao> findAll();

    /**
     * BUSCA BENEFICIÁRIOS POR EMPRESA
     *
     * @param codigoEmpresa código da empresa
     * @return lista de beneficiários da empresa
     */
    List<IntegracaoOdontoprevBeneficiarioAlteracao> findByCodigoEmpresa(String codigoEmpresa);

    /**
     * BUSCA BENEFICIÁRIOS POR PLANO
     *
     * @param codigoPlano código do plano
     * @return lista de beneficiários do plano
     */
    List<IntegracaoOdontoprevBeneficiarioAlteracao> findByCodigoPlano(String codigoPlano);

    /**
     * BUSCA BENEFICIÁRIOS POR EMPRESA E PLANO
     *
     * @param codigoEmpresa código da empresa
     * @param codigoPlano código do plano
     * @return lista de beneficiários filtrados
     */
    List<IntegracaoOdontoprevBeneficiarioAlteracao> findByCodigoEmpresaAndCodigoPlano(String codigoEmpresa, String codigoPlano);

    /**
     * BUSCA BENEFICIÁRIOS POR TIPO (TITULAR/DEPENDENTE)
     *
     * @param identificacao T = Titular, D = Dependente
     * @return lista de beneficiários do tipo especificado
     */
    List<IntegracaoOdontoprevBeneficiarioAlteracao> findByIdentificacao(String identificacao);

    /**
     * BUSCA BENEFICIÁRIOS POR EMPRESA E TIPO
     *
     * @param codigoEmpresa código da empresa
     * @param identificacao T = Titular, D = Dependente
     * @return lista de beneficiários filtrados
     */
    List<IntegracaoOdontoprevBeneficiarioAlteracao> findByCodigoEmpresaAndIdentificacao(String codigoEmpresa, String identificacao);

    /**
     * BUSCA TODOS OS BENEFICIÁRIOS (SEM LIMITE)
     *
     * @return lista de todos os beneficiários
     */
    @Query(value = "SELECT * FROM VW_INTEGRACAO_ODONTOPREV_BENEFICIARIOS_ALT ORDER BY CODIGO_MATRICULA ASC", nativeQuery = true)
    List<IntegracaoOdontoprevBeneficiarioAlteracao> findWithLimit();

    /**
     * BUSCA BENEFICIÁRIOS POR EMPRESA (SEM LIMITE)
     *
     * @param codigoEmpresa código da empresa
     * @return lista de beneficiários da empresa
     */
    @Query(value = "SELECT * FROM VW_INTEGRACAO_ODONTOPREV_BENEFICIARIOS_ALT WHERE CODIGO_EMPRESA = :codigoEmpresa ORDER BY CODIGO_MATRICULA ASC", nativeQuery = true)
    List<IntegracaoOdontoprevBeneficiarioAlteracao> findByCodigoEmpresaWithLimit(@Param("codigoEmpresa") String codigoEmpresa);

    /**
     * CONTA TOTAL DE BENEFICIÁRIOS PENDENTES DE ALTERAÇÃO
     *
     * @return quantidade total de beneficiários pendentes
     */
    long count();

    /**
     * CONTA BENEFICIÁRIOS POR EMPRESA
     *
     * @param codigoEmpresa código da empresa
     * @return quantidade de beneficiários da empresa
     */
    long countByCodigoEmpresa(String codigoEmpresa);

    /**
     * CONTA BENEFICIÁRIOS POR PLANO
     *
     * @param codigoPlano código do plano
     * @return quantidade de beneficiários do plano
     */
    long countByCodigoPlano(String codigoPlano);

    /**
     * CONTA BENEFICIÁRIOS POR TIPO
     *
     * @param identificacao T = Titular, D = Dependente
     * @return quantidade de beneficiários do tipo
     */
    long countByIdentificacao(String identificacao);

    /**
     * CONTA BENEFICIÁRIOS POR EMPRESA E TIPO
     *
     * @param codigoEmpresa código da empresa
     * @param identificacao T = Titular, D = Dependente
     * @return quantidade de beneficiários filtrados
     */
    long countByCodigoEmpresaAndIdentificacao(String codigoEmpresa, String identificacao);

    /**
     * BUSCA BENEFICIÁRIOS POR CÓDIGO DE MATRÍCULA
     *
     * @param codigoMatricula código da matrícula
     * @return beneficiário encontrado ou null
     */
    IntegracaoOdontoprevBeneficiarioAlteracao findByCodigoMatricula(String codigoMatricula);

    /**
     * BUSCA BENEFICIÁRIOS POR CPF
     *
     * @param cpf CPF do beneficiário
     * @return beneficiário encontrado ou null
     */
    IntegracaoOdontoprevBeneficiarioAlteracao findByCpf(String cpf);

    /**
     * BUSCA BENEFICIÁRIOS POR CÓDIGO DO ASSOCIADO
     *
     * @param cdAssociado código do associado (carteirinha)
     * @return beneficiário encontrado ou null
     */
    IntegracaoOdontoprevBeneficiarioAlteracao findByCdAssociado(String cdAssociado);

    /**
     * VERIFICA SE EXISTE BENEFICIÁRIO COM A MATRÍCULA
     *
     * @param codigoMatricula código da matrícula
     * @return true se existe, false caso contrário
     */
    boolean existsByCodigoMatricula(String codigoMatricula);

    /**
     * VERIFICA SE EXISTE BENEFICIÁRIO COM O CPF
     *
     * @param cpf CPF do beneficiário
     * @return true se existe, false caso contrário
     */
    boolean existsByCpf(String cpf);

    /**
     * VERIFICA SE EXISTE BENEFICIÁRIO COM O CÓDIGO DO ASSOCIADO
     *
     * @param cdAssociado código do associado
     * @return true se existe, false caso contrário
     */
    boolean existsByCdAssociado(String cdAssociado);
}
