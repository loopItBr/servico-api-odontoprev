package com.odontoPrev.odontoPrev.domain.repository;

import com.odontoPrev.odontoPrev.domain.entity.BeneficiarioOdontoprev;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * REPOSITÓRIO PARA OPERAÇÕES COM BENEFICIÁRIOS ODONTOPREV
 *
 * Interface que define operações de acesso a dados para a entidade
 * BeneficiarioOdontoprev, incluindo consultas específicas para
 * sincronização e atualizações de status.
 */
@Repository
public interface BeneficiarioOdontoprevRepository extends JpaRepository<BeneficiarioOdontoprev, Long> {

    /**
     * BUSCA BENEFICIÁRIO POR CÓDIGO DA MATRÍCULA
     *
     * @param codigoMatricula código da matrícula do beneficiário
     * @return beneficiário encontrado ou Optional.empty()
     */
    Optional<BeneficiarioOdontoprev> findByCodigoMatricula(String codigoMatricula);

    /**
     * BUSCA BENEFICIÁRIO POR CPF
     *
     * @param cpf CPF do beneficiário
     * @return beneficiário encontrado ou Optional.empty()
     */
    Optional<BeneficiarioOdontoprev> findByCpf(String cpf);

    /**
     * BUSCA BENEFICIÁRIO POR CÓDIGO DO ASSOCIADO
     *
     * @param cdAssociado código do associado (carteirinha)
     * @return beneficiário encontrado ou Optional.empty()
     */
    Optional<BeneficiarioOdontoprev> findByCdAssociado(String cdAssociado);

    /**
     * BUSCA BENEFICIÁRIOS POR STATUS DE SINCRONIZAÇÃO
     *
     * @param statusSincronizacao status a ser filtrado
     * @return lista de beneficiários com o status especificado
     */
    List<BeneficiarioOdontoprev> findByStatusSincronizacao(String statusSincronizacao);

    /**
     * BUSCA BENEFICIÁRIOS POR EMPRESA E STATUS
     *
     * @param codigoEmpresa código da empresa
     * @param statusSincronizacao status de sincronização
     * @return lista de beneficiários filtrados
     */
    List<BeneficiarioOdontoprev> findByCodigoEmpresaAndStatusSincronizacao(
            String codigoEmpresa, String statusSincronizacao);

    /**
     * ATUALIZA CÓDIGO DO ASSOCIADO E STATUS DE SINCRONIZAÇÃO
     *
     * Operação otimizada para atualizar apenas os campos necessários
     * após sucesso na inclusão na OdontoPrev.
     *
     * @param id ID do beneficiário
     * @param cdAssociado código do associado retornado pela OdontoPrev
     * @param statusSincronizacao novo status (geralmente "SINCRONIZADO")
     * @param dataSincronizacao data/hora da sincronização
     * @return número de registros atualizados
     */
    @Modifying
    @Query("UPDATE BeneficiarioOdontoprev b SET " +
           "b.cdAssociado = :cdAssociado, " +
           "b.statusSincronizacao = :statusSincronizacao, " +
           "b.dataSincronizacao = :dataSincronizacao " +
           "WHERE b.id = :id")
    int atualizarCdAssociado(
            @Param("id") Long id,
            @Param("cdAssociado") String cdAssociado,
            @Param("statusSincronizacao") String statusSincronizacao,
            @Param("dataSincronizacao") LocalDateTime dataSincronizacao);

    /**
     * ATUALIZA STATUS DE SINCRONIZAÇÃO E MENSAGEM DE ERRO
     *
     * Usado quando ocorre erro durante o processamento.
     *
     * @param id ID do beneficiário
     * @param statusSincronizacao novo status (geralmente "ERRO")
     * @param mensagemErro mensagem de erro
     * @param dataSincronizacao data/hora da tentativa
     * @return número de registros atualizados
     */
    @Modifying
    @Query("UPDATE BeneficiarioOdontoprev b SET " +
           "b.statusSincronizacao = :statusSincronizacao, " +
           "b.mensagemErro = :mensagemErro, " +
           "b.dataSincronizacao = :dataSincronizacao " +
           "WHERE b.id = :id")
    int atualizarStatusErro(
            @Param("id") Long id,
            @Param("statusSincronizacao") String statusSincronizacao,
            @Param("mensagemErro") String mensagemErro,
            @Param("dataSincronizacao") LocalDateTime dataSincronizacao);

    /**
     * CONTA BENEFICIÁRIOS POR STATUS DE SINCRONIZAÇÃO
     *
     * @param statusSincronizacao status a ser contado
     * @return quantidade de beneficiários com o status
     */
    long countByStatusSincronizacao(String statusSincronizacao);

    /**
     * CONTA BENEFICIÁRIOS POR EMPRESA E STATUS
     *
     * @param codigoEmpresa código da empresa
     * @param statusSincronizacao status de sincronização
     * @return quantidade de beneficiários filtrados
     */
    long countByCodigoEmpresaAndStatusSincronizacao(String codigoEmpresa, String statusSincronizacao);

    /**
     * BUSCA BENEFICIÁRIOS PENDENTES DE INCLUSÃO
     *
     * @param limit limite de registros a retornar
     * @return lista de beneficiários pendentes
     */
    @Query("SELECT b FROM BeneficiarioOdontoprev b " +
           "WHERE b.statusSincronizacao = 'PENDENTE' " +
           "ORDER BY b.dataCriacao ASC")
    List<BeneficiarioOdontoprev> findPendentesInclusao(@Param("limit") int limit);

    /**
     * BUSCA BENEFICIÁRIOS PENDENTES DE ALTERAÇÃO
     *
     * @param limit limite de registros a retornar
     * @return lista de beneficiários pendentes de alteração
     */
    @Query("SELECT b FROM BeneficiarioOdontoprev b " +
           "WHERE b.statusSincronizacao = 'ALTERADO' " +
           "ORDER BY b.dataAtualizacao ASC")
    List<BeneficiarioOdontoprev> findPendentesAlteracao(@Param("limit") int limit);

    /**
     * BUSCA BENEFICIÁRIOS PENDENTES DE EXCLUSÃO
     *
     * @param limit limite de registros a retornar
     * @return lista de beneficiários pendentes de exclusão
     */
    @Query("SELECT b FROM BeneficiarioOdontoprev b " +
           "WHERE b.statusSincronizacao = 'EXCLUIDO' " +
           "ORDER BY b.dataAtualizacao ASC")
    List<BeneficiarioOdontoprev> findPendentesExclusao(@Param("limit") int limit);
}
