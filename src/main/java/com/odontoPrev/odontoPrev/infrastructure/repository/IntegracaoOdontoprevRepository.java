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

    @Query("SELECT DISTINCT i.codigoEmpresa FROM IntegracaoOdontoprev i")
    List<String> buscarCodigosEmpresasDisponiveis();

    @Query("SELECT DISTINCT i.codigoEmpresa FROM IntegracaoOdontoprev i")
    List<String> buscarCodigosEmpresasPaginado(Pageable pageable);

    @Query("SELECT COUNT(DISTINCT i.codigoEmpresa) FROM IntegracaoOdontoprev i")
    long contarTotalEmpresas();

    /**
     * Busca dados de uma empresa retornando apenas o primeiro registro.
     * Otimizado com ROWNUM = 1 para melhor performance no Oracle.
     */
    @Query(value = "SELECT * FROM TASY.VW_INTEGRACAO_ODONTOPREV WHERE CODIGO_EMPRESA = :codigoEmpresa AND ROWNUM = 1", 
           nativeQuery = true)
    Optional<IntegracaoOdontoprev> buscarPrimeiroDadoPorCodigoEmpresa(String codigoEmpresa);

    /**
     * @deprecated Use buscarPrimeiroDadoPorCodigoEmpresa() para melhor performance
     */
    @Deprecated
    @Query("SELECT i FROM IntegracaoOdontoprev i WHERE i.codigoEmpresa = :codigoEmpresa")
    List<IntegracaoOdontoprev> buscarDadosPorCodigoEmpresa(String codigoEmpresa);
}