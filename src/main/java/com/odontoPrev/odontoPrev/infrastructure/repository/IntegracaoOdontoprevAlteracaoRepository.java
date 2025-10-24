package com.odontoPrev.odontoPrev.infrastructure.repository;

import com.odontoPrev.odontoPrev.infrastructure.repository.entity.IntegracaoOdontoprevAlteracao;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repositório para consulta da view de empresas alteradas.
 * 
 * Esta view contém empresas que tiveram DT_ALTERACAO = SYSDATE
 * e precisam ter dados atualizados na OdontoPrev.
 */
@Repository
public interface IntegracaoOdontoprevAlteracaoRepository extends JpaRepository<IntegracaoOdontoprevAlteracao, String> {

    /**
     * Busca códigos de empresas que foram alteradas.
     * 
     * @return lista de códigos de empresas alteradas
     */
    @Query(value = "SELECT DISTINCT CODIGO_EMPRESA FROM TASY.VW_INTEGRACAO_ODONTOPREV_ALT", nativeQuery = true)
    List<String> buscarCodigosEmpresasAlteradas();

    /**
     * Busca códigos de empresas alteradas com paginação.
     * 
     * @param pageable configuração de paginação
     * @return lista paginada de códigos de empresas alteradas
     */
    @Query(value = "SELECT DISTINCT CODIGO_EMPRESA FROM TASY.VW_INTEGRACAO_ODONTOPREV_ALT", nativeQuery = true)
    List<String> buscarCodigosEmpresasAlteradasPaginado(Pageable pageable);

    /**
     * Conta total de empresas alteradas.
     * 
     * @return número total de empresas alteradas
     */
    @Query(value = "SELECT COUNT(DISTINCT CODIGO_EMPRESA) FROM TASY.VW_INTEGRACAO_ODONTOPREV_ALT", nativeQuery = true)
    long contarTotalEmpresasAlteradas();

    /**
     * Busca dados de uma empresa alterada retornando apenas o primeiro registro.
     * Otimizado com ROWNUM = 1 para melhor performance no Oracle.
     * 
     * @param codigoEmpresa código da empresa
     * @return dados da empresa alterada
     */
    @Query(value = "SELECT * FROM TASY.VW_INTEGRACAO_ODONTOPREV_ALT WHERE CODIGO_EMPRESA = :codigoEmpresa AND ROWNUM = 1", nativeQuery = true)
    Optional<IntegracaoOdontoprevAlteracao> buscarPrimeiroDadoPorCodigoEmpresa(String codigoEmpresa);

    /**
     * Busca todos os dados de uma empresa alterada.
     * 
     * @param codigoEmpresa código da empresa
     * @return lista de dados da empresa alterada
     */
    @Query(value = "SELECT * FROM TASY.VW_INTEGRACAO_ODONTOPREV_ALT WHERE CODIGO_EMPRESA = :codigoEmpresa", nativeQuery = true)
    List<IntegracaoOdontoprevAlteracao> buscarDadosPorCodigoEmpresa(String codigoEmpresa);

    // Método findByCodigoEmpresa removido - usar buscarPrimeiroDadoPorCodigoEmpresa
}
