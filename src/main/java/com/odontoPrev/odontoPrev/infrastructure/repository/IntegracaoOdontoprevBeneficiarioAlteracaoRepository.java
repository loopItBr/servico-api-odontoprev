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
    List<IntegracaoOdontoprevBeneficiarioAlteracao> findByCdEmpresa(String cdEmpresa);

    /**
     * BUSCA BENEFICIÁRIOS POR PLANO
     *
     * @param codigoPlano código do plano
     * @return lista de beneficiários do plano
     */
    List<IntegracaoOdontoprevBeneficiarioAlteracao> findByCodigoPlano(Long codigoPlano);

    /**
     * BUSCA BENEFICIÁRIOS POR EMPRESA E PLANO
     *
     * @param codigoEmpresa código da empresa
     * @param codigoPlano código do plano
     * @return lista de beneficiários filtrados
     */
    List<IntegracaoOdontoprevBeneficiarioAlteracao> findByCdEmpresaAndCodigoPlano(String cdEmpresa, Long codigoPlano);

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
    List<IntegracaoOdontoprevBeneficiarioAlteracao> findByCdEmpresaAndIdentificacao(String cdEmpresa, String identificacao);

    /**
     * BUSCA TODOS OS BENEFICIÁRIOS (SEM LIMITE)
     *
     * @return lista de todos os beneficiários
     */
    @Query(value = "SELECT * FROM TASY.VW_INTEGRACAO_ODONTOPREV_BENEFICIARIOS_ALT ORDER BY CDEMPRESA", nativeQuery = true)
    List<IntegracaoOdontoprevBeneficiarioAlteracao> findWithLimit();

    /**
     * BUSCA BENEFICIÁRIOS POR EMPRESA (SEM LIMITE)
     *
     * @param codigoEmpresa código da empresa
     * @return lista de beneficiários da empresa
     */
    @Query(value = "SELECT * FROM TASY.VW_INTEGRACAO_ODONTOPREV_BENEFICIARIOS_ALT WHERE CDEMPRESA = :cdEmpresa ORDER BY CDEMPRESA", nativeQuery = true)
    List<IntegracaoOdontoprevBeneficiarioAlteracao> findByCdEmpresaWithLimit(@Param("cdEmpresa") String cdEmpresa);

    /**
     * CONTA TOTAL DE BENEFICIÁRIOS PENDENTES DE ALTERAÇÃO
     *
     * @return quantidade total de beneficiários pendentes
     */
    @Query(value = "SELECT COUNT(*) FROM TASY.VW_INTEGRACAO_ODONTOPREV_BENEFICIARIOS_ALT", nativeQuery = true)
    long count();

    /**
     * CONTA BENEFICIÁRIOS POR EMPRESA
     *
     * @param codigoEmpresa código da empresa
     * @return quantidade de beneficiários da empresa
     */
    @Query(value = "SELECT COUNT(*) FROM TASY.VW_INTEGRACAO_ODONTOPREV_BENEFICIARIOS_ALT WHERE CDEMPRESA = :cdEmpresa", nativeQuery = true)
    long countByCdEmpresa(@Param("cdEmpresa") String cdEmpresa);

    /**
     * CONTA BENEFICIÁRIOS POR PLANO
     *
     * @param codigoPlano código do plano
     * @return quantidade de beneficiários do plano
     */
    @Query(value = "SELECT COUNT(*) FROM TASY.VW_INTEGRACAO_ODONTOPREV_BENEFICIARIOS_ALT WHERE CODIGOPLANO = :codigoPlano", nativeQuery = true)
    long countByCodigoPlano(@Param("codigoPlano") Long codigoPlano);

    /**
     * CONTA BENEFICIÁRIOS POR TIPO
     *
     * @param identificacao T = Titular, D = Dependente
     * @return quantidade de beneficiários do tipo
     */
    @Query(value = "SELECT COUNT(*) FROM TASY.VW_INTEGRACAO_ODONTOPREV_BENEFICIARIOS_ALT WHERE IDENTIFICACAO = :identificacao", nativeQuery = true)
    long countByIdentificacao(@Param("identificacao") String identificacao);

    /**
     * CONTA BENEFICIÁRIOS POR EMPRESA E TIPO
     *
     * @param codigoEmpresa código da empresa
     * @param identificacao T = Titular, D = Dependente
     * @return quantidade de beneficiários filtrados
     */
    @Query(value = "SELECT COUNT(*) FROM TASY.VW_INTEGRACAO_ODONTOPREV_BENEFICIARIOS_ALT WHERE CDEMPRESA = :cdEmpresa AND IDENTIFICACAO = :identificacao", nativeQuery = true)
    long countByCdEmpresaAndIdentificacao(@Param("cdEmpresa") String cdEmpresa, @Param("identificacao") String identificacao);

    /**
     * BUSCA BENEFICIÁRIOS POR CÓDIGO DO ASSOCIADO
     *
     * @param cdAssociado código do associado (carteirinha)
     * @return beneficiário encontrado ou null
     */
    IntegracaoOdontoprevBeneficiarioAlteracao findByCdAssociado(String cdAssociado);

    /**
     * VERIFICA SE EXISTE BENEFICIÁRIO COM O CÓDIGO DO ASSOCIADO
     *
     * @param cdAssociado código do associado
     * @return true se existe, false caso contrário
     */
    boolean existsByCdAssociado(String cdAssociado);
}
