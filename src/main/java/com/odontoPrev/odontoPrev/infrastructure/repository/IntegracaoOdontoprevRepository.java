package com.odontoPrev.odontoPrev.infrastructure.repository;

import com.odontoPrev.odontoPrev.infrastructure.repository.entity.IntegracaoOdontoprev;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface IntegracaoOdontoprevRepository extends JpaRepository<IntegracaoOdontoprev, String> {

    @Query(value = "SELECT DISTINCT NR_SEQ_CONTRATO FROM TASY.VW_INTEGRACAO_ODONTOPREV WHERE CODIGO_EMPRESA IS NULL", nativeQuery = true)
    List<Long> buscarCodigosEmpresasDisponiveis();

    @Query(value = "SELECT DISTINCT NR_SEQ_CONTRATO FROM TASY.VW_INTEGRACAO_ODONTOPREV WHERE CODIGO_EMPRESA IS NULL ORDER BY NR_SEQ_CONTRATO", nativeQuery = true)
    List<Long> buscarCodigosEmpresasPaginado(Pageable pageable);

    @Query(value = "SELECT COUNT(DISTINCT NR_SEQ_CONTRATO) FROM TASY.VW_INTEGRACAO_ODONTOPREV WHERE CODIGO_EMPRESA IS NULL", nativeQuery = true)
    long contarTotalEmpresas();

    /**
     * Busca dados de uma empresa retornando apenas o primeiro registro.
     * Otimizado com ROWNUM = 1 para melhor performance no Oracle.
     */
    @Query(value = "SELECT * FROM TASY.VW_INTEGRACAO_ODONTOPREV WHERE NR_SEQ_CONTRATO = :nrSeqContrato AND ROWNUM = 1", 
           nativeQuery = true)
    Optional<IntegracaoOdontoprev> buscarPrimeiroDadoPorCodigoEmpresa(Long nrSeqContrato);

    /**
     * @deprecated Use buscarPrimeiroDadoPorCodigoEmpresa() para melhor performance
     */
    @Deprecated
    @Query("SELECT i FROM IntegracaoOdontoprev i WHERE i.nrSeqContrato = :nrSeqContrato")
    List<IntegracaoOdontoprev> buscarDadosPorCodigoEmpresa(Long nrSeqContrato);

    /**
     * BUSCA EMPRESAS PARA INCLUSÃO - QUERY ALTERNATIVA
     * 
     * Esta query alternativa busca empresas que ainda não foram processadas,
     * usando uma abordagem diferente para garantir que sempre pegue novidades.
     */
    @Query(value = "SELECT DISTINCT NR_SEQ_CONTRATO FROM TASY.VW_INTEGRACAO_ODONTOPREV " +
                   "WHERE CODIGO_EMPRESA IS NULL " +
                   "AND NR_SEQ_CONTRATO IS NOT NULL " +
                   "ORDER BY NR_SEQ_CONTRATO", nativeQuery = true)
    List<Long> buscarEmpresasParaInclusao();

    /**
     * CONTA EMPRESAS PARA INCLUSÃO - QUERY ALTERNATIVA
     * 
     * Conta empresas que ainda não foram processadas usando a mesma lógica
     * da query de busca para garantir consistência.
     */
    @Query(value = "SELECT COUNT(DISTINCT NR_SEQ_CONTRATO) FROM TASY.VW_INTEGRACAO_ODONTOPREV " +
                   "WHERE CODIGO_EMPRESA IS NULL " +
                   "AND NR_SEQ_CONTRATO IS NOT NULL", nativeQuery = true)
    long contarEmpresasParaInclusao();

    /**
     * BUSCA EMPRESAS COMPLETAS PARA INCLUSÃO
     * 
     * Busca empresas com todos os dados necessários que ainda não foram processadas.
     * Retorna dados completos para evitar segunda busca.
     */
    @Query(value = "SELECT * FROM TASY.VW_INTEGRACAO_ODONTOPREV " +
                   "WHERE CODIGO_EMPRESA IS NULL " +
                   "AND NR_SEQ_CONTRATO IS NOT NULL " +
                   "ORDER BY NR_SEQ_CONTRATO", nativeQuery = true)
    List<IntegracaoOdontoprev> buscarEmpresasCompletasParaInclusao();
}