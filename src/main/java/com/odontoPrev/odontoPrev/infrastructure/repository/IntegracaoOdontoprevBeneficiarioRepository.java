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
    List<IntegracaoOdontoprevBeneficiario> findByCodigoPlano(String codigoPlano);

    /**
     * BUSCA BENEFICIÁRIOS POR EMPRESA E PLANO
     *
     * @param codigoEmpresa código da empresa
     * @param codigoPlano código do plano
     * @return lista de beneficiários filtrados
     */
    List<IntegracaoOdontoprevBeneficiario> findByCodigoEmpresaAndCodigoPlano(String codigoEmpresa, String codigoPlano);

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
     * BUSCA BENEFICIÁRIOS COM LIMITE DE REGISTROS
     *
     * @param limit número máximo de registros a retornar
     * @return lista limitada de beneficiários
     */
    @Query("SELECT b FROM IntegracaoOdontoprevBeneficiario b ORDER BY b.codigoMatricula ASC")
    List<IntegracaoOdontoprevBeneficiario> findWithLimit(@Param("limit") int limit);

    /**
     * BUSCA BENEFICIÁRIOS POR EMPRESA COM LIMITE
     *
     * @param codigoEmpresa código da empresa
     * @param limit número máximo de registros a retornar
     * @return lista limitada de beneficiários da empresa
     */
    @Query("SELECT b FROM IntegracaoOdontoprevBeneficiario b WHERE b.codigoEmpresa = :codigoEmpresa ORDER BY b.codigoMatricula ASC")
    List<IntegracaoOdontoprevBeneficiario> findByCodigoEmpresaWithLimit(@Param("codigoEmpresa") String codigoEmpresa, @Param("limit") int limit);

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
    long countByCodigoPlano(String codigoPlano);

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
