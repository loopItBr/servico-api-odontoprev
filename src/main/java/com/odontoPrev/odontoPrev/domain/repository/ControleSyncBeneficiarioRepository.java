package com.odontoPrev.odontoPrev.domain.repository;

import com.odontoPrev.odontoPrev.domain.entity.ControleSyncBeneficiario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * REPOSITÓRIO PARA CONTROLE DE SINCRONIZAÇÃO DE BENEFICIÁRIOS
 *
 * Interface que define operações de acesso a dados para a entidade
 * ControleSyncBeneficiario, incluindo consultas específicas para
 * auditoria e controle de tentativas de sincronização.
 */
@Repository
public interface ControleSyncBeneficiarioRepository extends JpaRepository<ControleSyncBeneficiario, Long> {

    /**
     * BUSCA CONTROLE POR EMPRESA E BENEFICIÁRIO
     *
     * @param codigoEmpresa código da empresa
     * @param codigoBeneficiario código do beneficiário
     * @return lista de controles encontrados
     */
    List<ControleSyncBeneficiario> findByCodigoEmpresaAndCodigoBeneficiario(
            String codigoEmpresa, String codigoBeneficiario);

    /**
     * BUSCA CONTROLE POR STATUS DE SINCRONIZAÇÃO
     *
     * @param statusSync status da sincronização
     * @return lista de controles com o status especificado
     */
    List<ControleSyncBeneficiario> findByStatusSync(String statusSync);

    /**
     * BUSCA CONTROLE POR TIPO DE OPERAÇÃO
     *
     * @param tipoOperacao tipo da operação (INCLUSAO, ALTERACAO, EXCLUSAO)
     * @return lista de controles do tipo especificado
     */
    List<ControleSyncBeneficiario> findByTipoOperacao(String tipoOperacao);

    /**
     * BUSCA CONTROLE POR EMPRESA, BENEFICIÁRIO E TIPO DE OPERAÇÃO
     *
     * @param codigoEmpresa código da empresa
     * @param codigoBeneficiario código do beneficiário
     * @param tipoOperacao tipo da operação
     * @return controle encontrado ou Optional.empty()
     */
    Optional<ControleSyncBeneficiario> findByCodigoEmpresaAndCodigoBeneficiarioAndTipoOperacao(
            String codigoEmpresa, String codigoBeneficiario, String tipoOperacao);

    /**
     * BUSCA CONTROLES PENDENTES DE RETRY
     *
     * @param maxTentativas número máximo de tentativas permitidas
     * @return lista de controles que podem ser reprocessados
     */
    @Query("SELECT c FROM ControleSyncBeneficiario c " +
           "WHERE c.statusSync = 'ERROR' " +
           "AND c.tentativas < c.maxTentativas " +
           "ORDER BY c.dataUltimaTentativa ASC")
    List<ControleSyncBeneficiario> findPendentesRetry();

    /**
     * BUSCA CONTROLES PENDENTES DE RETRY POR EMPRESA
     *
     * @param codigoEmpresa código da empresa
     * @return lista de controles pendentes de retry para a empresa
     */
    @Query("SELECT c FROM ControleSyncBeneficiario c " +
           "WHERE c.codigoEmpresa = :codigoEmpresa " +
           "AND c.statusSync = 'ERROR' " +
           "AND c.tentativas < c.maxTentativas " +
           "ORDER BY c.dataUltimaTentativa ASC")
    List<ControleSyncBeneficiario> findPendentesRetryPorEmpresa(@Param("codigoEmpresa") String codigoEmpresa);

    /**
     * MARCA CONTROLE COMO SUCESSO
     *
     * @param id ID do controle
     * @param dataSucesso data/hora do sucesso
     * @param responseApi resposta da API
     * @return número de registros atualizados
     */
    @Modifying
    @Query("UPDATE ControleSyncBeneficiario c SET " +
           "c.statusSync = 'SUCCESS', " +
           "c.dataSucesso = :dataSucesso, " +
           "c.responseApi = :responseApi " +
           "WHERE c.id = :id")
    int marcarComoSucesso(
            @Param("id") Long id,
            @Param("dataSucesso") LocalDateTime dataSucesso,
            @Param("responseApi") String responseApi);

    /**
     * ATUALIZA CONTROLE APÓS TENTATIVA (SUCESSO OU ERRO)
     *
     * @param id ID do controle
     * @param statusSync novo status
     * @param tentativas número de tentativas
     * @param dataUltimaTentativa data/hora da última tentativa
     * @param erroMensagem mensagem de erro (se houver)
     * @param responseApi resposta da API (se houver)
     * @return número de registros atualizados
     */
    @Modifying
    @Query("UPDATE ControleSyncBeneficiario c SET " +
           "c.statusSync = :statusSync, " +
           "c.tentativas = :tentativas, " +
           "c.dataUltimaTentativa = :dataUltimaTentativa, " +
           "c.erroMensagem = :erroMensagem, " +
           "c.responseApi = :responseApi " +
           "WHERE c.id = :id")
    int atualizarAposTentativa(
            @Param("id") Long id,
            @Param("statusSync") String statusSync,
            @Param("tentativas") Integer tentativas,
            @Param("dataUltimaTentativa") LocalDateTime dataUltimaTentativa,
            @Param("erroMensagem") String erroMensagem,
            @Param("responseApi") String responseApi);

    /**
     * CONTA CONTROLES POR STATUS
     *
     * @param statusSync status a ser contado
     * @return quantidade de controles com o status
     */
    long countByStatusSync(String statusSync);

    /**
     * CONTA CONTROLES POR EMPRESA E STATUS
     *
     * @param codigoEmpresa código da empresa
     * @param statusSync status da sincronização
     * @return quantidade de controles filtrados
     */
    long countByCodigoEmpresaAndStatusSync(String codigoEmpresa, String statusSync);

    /**
     * CONTA CONTROLES POR TIPO DE OPERAÇÃO E STATUS
     *
     * @param tipoOperacao tipo da operação
     * @param statusSync status da sincronização
     * @return quantidade de controles filtrados
     */
    long countByTipoOperacaoAndStatusSync(String tipoOperacao, String statusSync);

    /**
     * BUSCA CONTROLES CRIADOS EM PERÍODO ESPECÍFICO
     *
     * @param dataInicio data de início do período
     * @param dataFim data de fim do período
     * @return lista de controles criados no período
     */
    @Query("SELECT c FROM ControleSyncBeneficiario c " +
           "WHERE c.dataCriacao BETWEEN :dataInicio AND :dataFim " +
           "ORDER BY c.dataCriacao DESC")
    List<ControleSyncBeneficiario> findByDataCriacaoBetween(
            @Param("dataInicio") LocalDateTime dataInicio,
            @Param("dataFim") LocalDateTime dataFim);

    /**
     * BUSCA CONTROLES COM ERRO EM PERÍODO ESPECÍFICO
     *
     * @param dataInicio data de início do período
     * @param dataFim data de fim do período
     * @return lista de controles com erro no período
     */
    @Query("SELECT c FROM ControleSyncBeneficiario c " +
           "WHERE c.statusSync = 'ERROR' " +
           "AND c.dataUltimaTentativa BETWEEN :dataInicio AND :dataFim " +
           "ORDER BY c.dataUltimaTentativa DESC")
    List<ControleSyncBeneficiario> findErrosPorPeriodo(
            @Param("dataInicio") LocalDateTime dataInicio,
            @Param("dataFim") LocalDateTime dataFim);
}
