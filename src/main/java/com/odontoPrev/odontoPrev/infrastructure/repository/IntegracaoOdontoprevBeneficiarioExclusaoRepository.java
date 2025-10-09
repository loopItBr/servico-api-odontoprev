package com.odontoPrev.odontoPrev.infrastructure.repository;

import com.odontoPrev.odontoPrev.infrastructure.repository.entity.IntegracaoOdontoprevBeneficiarioExclusao;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * REPOSITÓRIO PARA CONSULTA DE BENEFICIÁRIOS PENDENTES DE EXCLUSÃO
 *
 * Interface que define operações de acesso a dados para a view
 * TASY.VW_INTEGRACAO_ODONTOPREV_BENEFICIARIOS_EXC, que contém beneficiários
 * que foram excluídos/inativados e precisam ser removidos da OdontoPrev.
 *
 * IMPORTANTE:
 * Esta é uma VIEW (apenas leitura), não uma tabela.
 * Os dados são consultados mas nunca modificados através desta interface.
 */
@Repository
public interface IntegracaoOdontoprevBeneficiarioExclusaoRepository extends JpaRepository<IntegracaoOdontoprevBeneficiarioExclusao, String> {

    /**
     * BUSCA TODOS OS BENEFICIÁRIOS PENDENTES DE EXCLUSÃO
     * 
     * Usa query nativa para contornar problema de subconsulta na view
     *
     * @return lista de todos os beneficiários pendentes de exclusão
     */
    @Query(value = "SELECT * FROM TASY.VW_INTEGRACAO_ODONTOPREV_BENEFICIARIOS_EXC ORDER BY CDEMPRESA", nativeQuery = true)
    List<IntegracaoOdontoprevBeneficiarioExclusao> findAll();

    /**
     * BUSCA BENEFICIÁRIOS POR EMPRESA
     * 
     * Usa query nativa para contornar problema de subconsulta na view
     *
     * @param codigoEmpresa código da empresa
     * @return lista de beneficiários da empresa
     */
    @Query(value = "SELECT * FROM TASY.VW_INTEGRACAO_ODONTOPREV_BENEFICIARIOS_EXC WHERE CDEMPRESA = :cdEmpresa ORDER BY CDEMPRESA", nativeQuery = true)
    List<IntegracaoOdontoprevBeneficiarioExclusao> findByCdEmpresa(@Param("cdEmpresa") String cdEmpresa);

    /**
     * BUSCA BENEFICIÁRIOS POR ID DO MOTIVO DE INATIVAÇÃO
     * 
     * Usa query nativa para contornar problema de subconsulta na view
     *
     * @param idMotivo ID do motivo de inativação
     * @return lista de beneficiários com o motivo especificado
     */
    @Query(value = "SELECT * FROM TASY.VW_INTEGRACAO_ODONTOPREV_BENEFICIARIOS_EXC WHERE IDMOTIVO = :idMotivo ORDER BY CDEMPRESA", nativeQuery = true)
    List<IntegracaoOdontoprevBeneficiarioExclusao> findByIdMotivo(@Param("idMotivo") Long idMotivo);

    /**
     * BUSCA TODOS OS BENEFICIÁRIOS (SEM LIMITE)
     * 
     * Query simples que busca diretamente da view sem subconsultas
     *
     * @return lista de todos os beneficiários
     */
    @Query(value = "SELECT * FROM TASY.VW_INTEGRACAO_ODONTOPREV_BENEFICIARIOS_EXC ORDER BY CDEMPRESA", nativeQuery = true)
    List<IntegracaoOdontoprevBeneficiarioExclusao> findWithLimit();

    /**
     * BUSCA BENEFICIÁRIOS POR EMPRESA (SEM LIMITE)
     * 
     * Query simples que busca diretamente da view sem subconsultas
     *
     * @param codigoEmpresa código da empresa
     * @return lista de beneficiários da empresa
     */
    @Query(value = "SELECT * FROM TASY.VW_INTEGRACAO_ODONTOPREV_BENEFICIARIOS_EXC WHERE CDEMPRESA = :cdEmpresa ORDER BY CDEMPRESA", nativeQuery = true)
    List<IntegracaoOdontoprevBeneficiarioExclusao> findByCdEmpresaWithLimit(@Param("cdEmpresa") String cdEmpresa);

    /**
     * CONTA TOTAL DE BENEFICIÁRIOS PENDENTES DE EXCLUSÃO
     * 
     * Usa query nativa com DISTINCT para contornar problema de subconsulta na view
     *
     * @return quantidade total de beneficiários pendentes
     */
    @Query(value = "SELECT COUNT(*) FROM TASY.VW_INTEGRACAO_ODONTOPREV_BENEFICIARIOS_EXC", nativeQuery = true)
    long count();

    /**
     * CONTA BENEFICIÁRIOS POR EMPRESA
     * 
     * Usa query nativa com DISTINCT para contornar problema de subconsulta na view
     *
     * @param codigoEmpresa código da empresa
     * @return quantidade de beneficiários da empresa
     */
    @Query(value = "SELECT COUNT(*) FROM TASY.VW_INTEGRACAO_ODONTOPREV_BENEFICIARIOS_EXC WHERE CDEMPRESA = :cdEmpresa", nativeQuery = true)
    long countByCdEmpresa(@Param("cdEmpresa") String cdEmpresa);

    /**
     * CONTA BENEFICIÁRIOS POR ID DO MOTIVO DE INATIVAÇÃO
     * 
     * Usa query nativa com DISTINCT para contornar problema de subconsulta na view
     *
     * @param idMotivo ID do motivo de inativação
     * @return quantidade de beneficiários com o motivo
     */
    @Query(value = "SELECT COUNT(*) FROM TASY.VW_INTEGRACAO_ODONTOPREV_BENEFICIARIOS_EXC WHERE IDMOTIVO = :idMotivo", nativeQuery = true)
    long countByIdMotivo(@Param("idMotivo") Long idMotivo);

    /**
     * BUSCA BENEFICIÁRIOS POR CÓDIGO DE MATRÍCULA
     *
     * @param codigoMatricula código da matrícula
     * @return beneficiário encontrado ou null
     */
    IntegracaoOdontoprevBeneficiarioExclusao findByCodigoMatricula(String codigoMatricula);

    /**
     * BUSCA BENEFICIÁRIOS POR CÓDIGO DO ASSOCIADO
     *
     * @param cdAssociado código do associado (carteirinha)
     * @return beneficiário encontrado ou null
     */
    IntegracaoOdontoprevBeneficiarioExclusao findByCdAssociado(String cdAssociado);

    /**
     * VERIFICA SE EXISTE BENEFICIÁRIO COM A MATRÍCULA
     *
     * @param codigoMatricula código da matrícula
     * @return true se existe, false caso contrário
     */
    boolean existsByCodigoMatricula(String codigoMatricula);

    /**
     * VERIFICA SE EXISTE BENEFICIÁRIO COM O CÓDIGO DO ASSOCIADO
     *
     * @param cdAssociado código do associado
     * @return true se existe, false caso contrário
     */
    boolean existsByCdAssociado(String cdAssociado);
}
