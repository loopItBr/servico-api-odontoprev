package com.odontoPrev.odontoPrev.infrastructure.repository;

import com.odontoPrev.odontoPrev.infrastructure.repository.entity.IntegracaoOdontoprevExclusao;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repositório para consulta da view de empresas excluídas.
 * 
 * Esta view contém empresas que foram inativadas (ATIVO = 2) ou
 * planos que foram excluídos (ATIVO = 2 e IE_SEGMENTACAO = 4)
 * e precisam ser removidas da OdontoPrev.
 */
@Repository
public interface IntegracaoOdontoprevExclusaoRepository extends JpaRepository<IntegracaoOdontoprevExclusao, String> {

    /**
     * Busca códigos de empresas que foram excluídas.
     * 
     * @return lista de códigos de empresas excluídas
     */
    @Query(value = "SELECT DISTINCT CODIGO_EMPRESA FROM TASY.VW_INTEGRACAO_ODONTOPREV_EXC", nativeQuery = true)
    List<String> buscarCodigosEmpresasExcluidas();

    /**
     * Busca códigos de empresas excluídas com paginação.
     * 
     * @param pageable configuração de paginação
     * @return lista paginada de códigos de empresas excluídas
     */
    @Query(value = "SELECT DISTINCT CODIGO_EMPRESA FROM TASY.VW_INTEGRACAO_ODONTOPREV_EXC", nativeQuery = true)
    List<String> buscarCodigosEmpresasExcluidasPaginado(Pageable pageable);

    /**
     * Conta total de empresas excluídas.
     * 
     * @return número total de empresas excluídas
     */
    @Query(value = "SELECT COUNT(DISTINCT CODIGO_EMPRESA) FROM TASY.VW_INTEGRACAO_ODONTOPREV_EXC", nativeQuery = true)
    long contarTotalEmpresasExcluidas();

    /**
     * Busca dados de uma empresa excluída retornando apenas o primeiro registro.
     * Usa query nativa para evitar problemas de mapeamento de colunas.
     * 
     * @param codigoEmpresa código da empresa
     * @return dados da empresa excluída
     */
    @Query(value = "SELECT * FROM TASY.VW_INTEGRACAO_ODONTOPREV_EXC WHERE CODIGO_EMPRESA = :codigoEmpresa", nativeQuery = true)
    List<IntegracaoOdontoprevExclusao> buscarPrimeiroDadoPorCodigoEmpresa(String codigoEmpresa);

    /**
     * Busca todos os dados de uma empresa excluída.
     * 
     * @param codigoEmpresa código da empresa
     * @return lista de dados da empresa excluída
     */
    @Query(value = "SELECT * FROM TASY.VW_INTEGRACAO_ODONTOPREV_EXC WHERE CODIGO_EMPRESA = :codigoEmpresa", nativeQuery = true)
    List<IntegracaoOdontoprevExclusao> buscarDadosPorCodigoEmpresa(String codigoEmpresa);

    /**
     * Busca dados de uma empresa excluída (método convencional do Spring Data JPA).
     * 
     * @param codigoEmpresa código da empresa
     * @return dados da empresa excluída
     */
    Optional<IntegracaoOdontoprevExclusao> findByCodigoEmpresa(String codigoEmpresa);
}
