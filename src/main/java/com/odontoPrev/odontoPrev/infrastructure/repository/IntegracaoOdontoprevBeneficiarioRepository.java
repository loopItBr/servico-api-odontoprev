package com.odontoPrev.odontoPrev.infrastructure.repository;

import com.odontoPrev.odontoPrev.infrastructure.repository.entity.IntegracaoOdontoprevBeneficiario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * REPOSITÓRIO PARA CONSULTA DE BENEFICIÁRIOS PENDENTES DE INCLUSÃO
 *
 * Interface que define operações de acesso a dados para a view
 * VW_INTEGRACAO_ODONTOPREV_BENEFICIARIOS, que contém beneficiários
 * novos que precisam ser cadastrados na OdontoPrev.
 *
 * IMPORTANTE:
 * Esta é uma VIEW (apenas leitura), não uma tabela.
 * Os dados são consultados mas nunca modificados através desta interface.
 */
@Repository
public interface IntegracaoOdontoprevBeneficiarioRepository extends JpaRepository<IntegracaoOdontoprevBeneficiario, String> {

    /**
     * BUSCA TODOS OS BENEFICIÁRIOS PENDENTES DE INCLUSÃO
     *
     * @return lista de todos os beneficiários pendentes
     */
    List<IntegracaoOdontoprevBeneficiario> findAll();

    /**
     * BUSCA BENEFICIÁRIOS POR EMPRESA
     *
     * @param codigoEmpresa código da empresa
     * @return lista de beneficiários da empresa
     */
    List<IntegracaoOdontoprevBeneficiario> findByCodigoEmpresa(String codigoEmpresa);

    /**
     * BUSCA BENEFICIÁRIOS POR PLANO
     *
     * @param codigoPlano código do plano
     * @return lista de beneficiários do plano
     */
    List<IntegracaoOdontoprevBeneficiario> findByCodigoPlano(Long codigoPlano);

    /**
     * BUSCA BENEFICIÁRIOS POR EMPRESA E PLANO
     *
     * @param codigoEmpresa código da empresa
     * @param codigoPlano código do plano
     * @return lista de beneficiários filtrados
     */
    List<IntegracaoOdontoprevBeneficiario> findByCodigoEmpresaAndCodigoPlano(String codigoEmpresa, Long codigoPlano);

    /**
     * BUSCA BENEFICIÁRIOS POR TIPO (TITULAR/DEPENDENTE)
     *
     * @param identificacao T = Titular, D = Dependente
     * @return lista de beneficiários do tipo especificado
     */
    List<IntegracaoOdontoprevBeneficiario> findByIdentificacao(String identificacao);

    /**
     * BUSCA BENEFICIÁRIOS POR EMPRESA E TIPO
     *
     * @param codigoEmpresa código da empresa
     * @param identificacao T = Titular, D = Dependente
     * @return lista de beneficiários filtrados
     */
    List<IntegracaoOdontoprevBeneficiario> findByCodigoEmpresaAndIdentificacao(String codigoEmpresa, String identificacao);

    /**
     * BUSCA TODOS OS BENEFICIÁRIOS (SEM LIMITE)
     *
     * @return lista de todos os beneficiários
     */
    @Query(value = "SELECT * FROM VW_INTEGRACAO_ODONTOPREV_BENEFICIARIOS ORDER BY CODIGO_MATRICULA ASC", nativeQuery = true)
    List<IntegracaoOdontoprevBeneficiario> findWithLimit();

    /**
     * BUSCA BENEFICIÁRIOS POR EMPRESA (SEM LIMITE)
     *
     * @param codigoEmpresa código da empresa
     * @return lista de beneficiários da empresa
     */
    @Query(value = "SELECT * FROM VW_INTEGRACAO_ODONTOPREV_BENEFICIARIOS WHERE CODIGO_EMPRESA = :codigoEmpresa ORDER BY CODIGO_MATRICULA ASC", nativeQuery = true)
    List<IntegracaoOdontoprevBeneficiario> findByCodigoEmpresaWithLimit(@Param("codigoEmpresa") String codigoEmpresa);

    /**
     * CONTA TOTAL DE BENEFICIÁRIOS PENDENTES
     *
     * @return quantidade total de beneficiários pendentes
     */
    long count();

    /**
     * CONTA BENEFICIÁRIOS POR EMPRESA
     *
     * @param codigoEmpresa código da empresa
     * @return quantidade de beneficiários da empresa
     */
    long countByCodigoEmpresa(String codigoEmpresa);

    /**
     * CONTA BENEFICIÁRIOS POR PLANO
     *
     * @param codigoPlano código do plano
     * @return quantidade de beneficiários do plano
     */
    long countByCodigoPlano(Long codigoPlano);

    /**
     * CONTA BENEFICIÁRIOS POR TIPO
     *
     * @param identificacao T = Titular, D = Dependente
     * @return quantidade de beneficiários do tipo
     */
    long countByIdentificacao(String identificacao);

    /**
     * CONTA BENEFICIÁRIOS POR EMPRESA E TIPO
     *
     * @param codigoEmpresa código da empresa
     * @param identificacao T = Titular, D = Dependente
     * @return quantidade de beneficiários filtrados
     */
    long countByCodigoEmpresaAndIdentificacao(String codigoEmpresa, String identificacao);

    /**
     * BUSCA BENEFICIÁRIOS POR CÓDIGO DE MATRÍCULA
     *
     * @param codigoMatricula código da matrícula
     * @return beneficiário encontrado ou null
     */
    IntegracaoOdontoprevBeneficiario findByCodigoMatricula(String codigoMatricula);

    /**
     * BUSCA BENEFICIÁRIOS POR CPF
     *
     * @param cpf CPF do beneficiário
     * @return beneficiário encontrado ou null
     */
    IntegracaoOdontoprevBeneficiario findByCpf(String cpf);

    /**
     * VERIFICA SE EXISTE BENEFICIÁRIO COM A MATRÍCULA
     *
     * @param codigoMatricula código da matrícula
     * @return true se existe, false caso contrário
     */
    boolean existsByCodigoMatricula(String codigoMatricula);

    /**
     * VERIFICA SE EXISTE BENEFICIÁRIO COM O CPF
     *
     * @param cpf CPF do beneficiário
     * @return true se existe, false caso contrário
     */
    boolean existsByCpf(String cpf);
}
