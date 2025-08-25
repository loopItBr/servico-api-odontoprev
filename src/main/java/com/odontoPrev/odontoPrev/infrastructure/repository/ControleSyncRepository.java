package com.odontoPrev.odontoPrev.infrastructure.repository;

import com.odontoPrev.odontoPrev.infrastructure.repository.entity.ControleSync;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ControleSyncRepository extends JpaRepository<ControleSync, Integer> {

    List<ControleSync> findByCodigoEmpresaOrderByDataCriacaoDesc(String codigoEmpresa);
    
    List<ControleSync> findByStatusSyncOrderByDataCriacaoDesc(ControleSync.StatusSync statusSync);
    
    long countByStatusSync(ControleSync.StatusSync statusSync);
}

