package com.odontoPrev.odontoPrev.infrastructure.repository;

import com.odontoPrev.odontoPrev.infrastructure.repository.entity.ControleSync;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ControleSyncRepository extends JpaRepository<ControleSync, Integer> {

    List<ControleSync> findByCodigoEmpresaOrderByDataCriacaoDesc(String codigoEmpresa);
    
    List<ControleSync> findByStatusSyncOrderByDataCriacaoDesc(ControleSync.StatusSync statusSync);
    
    long countByStatusSync(ControleSync.StatusSync statusSync);
    
    Optional<ControleSync> findByCodigoEmpresaAndTipoControle(String codigoEmpresa, Integer tipoControle);
    
    /**
     * BUSCA O PRIMEIRO CONTROLE POR EMPRESA E TIPO (ORDENADO POR DATA DE CRIAÇÃO DESC)
     * 
     * Usado quando há múltiplos registros para a mesma empresa e tipo.
     * Retorna o mais recente (último criado).
     */
    Optional<ControleSync> findFirstByCodigoEmpresaAndTipoControleOrderByDataCriacaoDesc(String codigoEmpresa, Integer tipoControle);
    
    /**
     * BUSCA TODOS OS CONTROLES POR EMPRESA E TIPO (ORDENADO POR DATA DE CRIAÇÃO DESC)
     * 
     * Usado para debug quando há múltiplos registros.
     * Retorna todos os registros ordenados por data de criação.
     */
    List<ControleSync> findByCodigoEmpresaAndTipoControleOrderByDataCriacaoDesc(String codigoEmpresa, Integer tipoControle);
    
    /**
     * BUSCA EMPRESAS COM ERRO NA CRIAÇÃO DE PLANOS
     * 
     * Busca todas as empresas que falharam na criação de planos
     * (tipo PLANOS = 4 e status ERROR).
     */
    List<ControleSync> findByTipoControleAndStatusSyncOrderByDataCriacaoDesc(Integer tipoControle, ControleSync.StatusSync statusSync);
    
    /**
     * BUSCA CÓDIGOS ÚNICOS DE EMPRESAS COM ERRO DE PLANOS
     * 
     * Retorna apenas os codigoEmpresa distintos que tiveram erro.
     */
    @Query("SELECT DISTINCT c.codigoEmpresa FROM ControleSync c WHERE c.tipoControle = ?1 AND c.statusSync = ?2")
    List<String> findDistinctCodigoEmpresaByTipoControleAndStatusSync(Integer tipoControle, ControleSync.StatusSync statusSync);
}

