package com.odontoPrev.odontoPrev.infrastructure.repository;

import com.odontoPrev.odontoPrev.infrastructure.repository.entity.IntegracaoOdontoprev;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface IntegracaoOdontoprevRepository extends JpaRepository<IntegracaoOdontoprev, String> {

    @Query("SELECT DISTINCT i.codigoEmpresa FROM IntegracaoOdontoprev i")
    List<String> buscarCodigosEmpresasDisponiveis();

    @Query("SELECT i FROM IntegracaoOdontoprev i WHERE i.codigoEmpresa = :codigoEmpresa")
    List<IntegracaoOdontoprev> buscarDadosPorCodigoEmpresa(String codigoEmpresa);
}