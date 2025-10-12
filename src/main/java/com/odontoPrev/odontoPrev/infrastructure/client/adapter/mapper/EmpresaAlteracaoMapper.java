package com.odontoPrev.odontoPrev.infrastructure.client.adapter.mapper;

import com.odontoPrev.odontoPrev.infrastructure.client.adapter.out.dto.EmpresaAlteracaoRequest;
import com.odontoPrev.odontoPrev.infrastructure.repository.entity.IntegracaoOdontoprev;
import com.odontoPrev.odontoPrev.infrastructure.repository.entity.IntegracaoOdontoprevAlteracao;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Collections;

/**
 * MAPPER PARA CONVERSÃO DE EMPRESA PARA ALTERAÇÃO
 *
 * Este mapper converte entidades de empresa para o DTO de alteração
 * da API OdontoPrev, mapeando campos específicos e aplicando
 * transformações necessárias.
 */
@Mapper(componentModel = "spring")
public interface EmpresaAlteracaoMapper {

    /**
     * CONVERTE ENTIDADE DE ALTERAÇÃO PARA DTO DE ALTERAÇÃO
     *
     * Mapeia campos da view de alteração para o DTO da API.
     * Aplica transformações específicas para campos obrigatórios.
     */
    // APENAS CAMPOS OBRIGATÓRIOS + MODIFICADOS
    @Mapping(target = "codigoEmpresa", source = "codigoEmpresa") // OBRIGATÓRIO
    @Mapping(target = "nomeFantasia", source = "nomeFantasia") // MODIFICADO
    @Mapping(target = "dataVigencia", source = "dataVigencia", qualifiedByName = "localDateToLocalDateTime") // MODIFICADO
    @Mapping(target = "codigoUsuario", source = "codUsuario") // OBRIGATÓRIO - da view
    @Mapping(target = "endereco", ignore = true) // OBRIGATÓRIO - será preenchido com dados padrão
    // TODOS OS OUTROS CAMPOS SÃO IGNORADOS
    @Mapping(target = "razaoSocial", ignore = true)
    @Mapping(target = "emiteCarteirinhaPlastica", ignore = true)
    @Mapping(target = "permissaoCadastroDep", ignore = true)
    @Mapping(target = "descricaoRamoAtividade", ignore = true)
    @Mapping(target = "ramo", ignore = true)
    @Mapping(target = "numeroFuncionarios", ignore = true)
    @Mapping(target = "valorFator", ignore = true)
    @Mapping(target = "cnae", ignore = true)
    @Mapping(target = "codigoLayoutCarteirinha", ignore = true)
    @Mapping(target = "codigoOrdemCarteira", ignore = true)
    @Mapping(target = "liberaSenhaInternet", ignore = true)
    @Mapping(target = "dependentePaga", ignore = true)
    @Mapping(target = "custoFamiliar", ignore = true)
    @Mapping(target = "planoFamiliar", ignore = true)
    @Mapping(target = "idadeLimiteUniversitaria", ignore = true)
    @Mapping(target = "codigoRegiao", ignore = true)
    @Mapping(target = "numeroCei", ignore = true)
    @Mapping(target = "cic", ignore = true)
    @Mapping(target = "inscricaoMunicipal", ignore = true)
    @Mapping(target = "inscricaoEstadual", ignore = true)
    @Mapping(target = "telefone", ignore = true)
    @Mapping(target = "email", ignore = true)
    @Mapping(target = "codigoNaturezaJuridica", ignore = true)
    @Mapping(target = "nomeNaturezaJuridica", ignore = true)
    @Mapping(target = "situacaoCadastral", ignore = true)
    @Mapping(target = "dataConstituicao", ignore = true)
    @Mapping(target = "renovacaoAutomatica", ignore = true)
    @Mapping(target = "mesAniversarioReajuste", ignore = true)
    @Mapping(target = "anoProximoAniversarioReajuste", ignore = true)
    @Mapping(target = "sinistralidade", ignore = true)
    @Mapping(target = "sistema", ignore = true)
    @Mapping(target = "diaVencimentoPlano", ignore = true)
    @Mapping(target = "diaMovimentacaoCadastral", ignore = true)
    @Mapping(target = "codigoGrupoGerencial", ignore = true)
    @Mapping(target = "grausParentesco", ignore = true)
    EmpresaAlteracaoRequest toAlteracaoRequest(IntegracaoOdontoprevAlteracao empresa);

    /**
     * CONVERTE ENTIDADE BASE PARA DTO DE ALTERAÇÃO
     *
     * Mapeia campos da entidade base para o DTO da API.
     * Usado quando não há dados específicos de alteração.
     */
    // APENAS CAMPOS OBRIGATÓRIOS + MODIFICADOS
    @Mapping(target = "codigoEmpresa", source = "codigoEmpresa") // OBRIGATÓRIO
    @Mapping(target = "nomeFantasia", source = "nomeFantasia") // MODIFICADO
    @Mapping(target = "dataVigencia", source = "dataVigencia", qualifiedByName = "localDateToLocalDateTime") // MODIFICADO
    @Mapping(target = "codigoUsuario", constant = "0") // OBRIGATÓRIO - valor padrão (IntegracaoOdontoprev não tem codUsuario)
    @Mapping(target = "endereco", ignore = true) // OBRIGATÓRIO - será preenchido com dados padrão
    // TODOS OS OUTROS CAMPOS SÃO IGNORADOS
    @Mapping(target = "razaoSocial", ignore = true)
    @Mapping(target = "emiteCarteirinhaPlastica", ignore = true)
    @Mapping(target = "permissaoCadastroDep", ignore = true)
    @Mapping(target = "descricaoRamoAtividade", ignore = true)
    @Mapping(target = "ramo", ignore = true)
    @Mapping(target = "numeroFuncionarios", ignore = true)
    @Mapping(target = "valorFator", ignore = true)
    @Mapping(target = "cnae", ignore = true)
    @Mapping(target = "codigoLayoutCarteirinha", ignore = true)
    @Mapping(target = "codigoOrdemCarteira", ignore = true)
    @Mapping(target = "liberaSenhaInternet", ignore = true)
    @Mapping(target = "dependentePaga", ignore = true)
    @Mapping(target = "custoFamiliar", ignore = true)
    @Mapping(target = "planoFamiliar", ignore = true)
    @Mapping(target = "idadeLimiteUniversitaria", ignore = true)
    @Mapping(target = "codigoRegiao", ignore = true)
    @Mapping(target = "numeroCei", ignore = true)
    @Mapping(target = "cic", ignore = true)
    @Mapping(target = "inscricaoMunicipal", ignore = true)
    @Mapping(target = "inscricaoEstadual", ignore = true)
    @Mapping(target = "telefone", ignore = true)
    @Mapping(target = "email", ignore = true)
    @Mapping(target = "codigoNaturezaJuridica", ignore = true)
    @Mapping(target = "nomeNaturezaJuridica", ignore = true)
    @Mapping(target = "situacaoCadastral", ignore = true)
    @Mapping(target = "dataConstituicao", ignore = true)
    @Mapping(target = "renovacaoAutomatica", ignore = true)
    @Mapping(target = "mesAniversarioReajuste", ignore = true)
    @Mapping(target = "anoProximoAniversarioReajuste", ignore = true)
    @Mapping(target = "sinistralidade", ignore = true)
    @Mapping(target = "sistema", ignore = true)
    @Mapping(target = "diaVencimentoPlano", ignore = true)
    @Mapping(target = "diaMovimentacaoCadastral", ignore = true)
    @Mapping(target = "codigoGrupoGerencial", ignore = true)
    @Mapping(target = "grausParentesco", ignore = true)
    EmpresaAlteracaoRequest toAlteracaoRequest(IntegracaoOdontoprev empresa);

    /**
     * CONVERTE LOCALDATE PARA LOCALDATETIME
     */
    @Named("localDateToLocalDateTime")
    default LocalDateTime localDateToLocalDateTime(LocalDate date) {
        if (date == null) {
            return null;
        }
        return date.atStartOfDay();
    }

    /**
     * CONVERTE STRING PARA DOUBLE
     */
    @Named("stringToDouble")
    default Double stringToDouble(String value) {
        if (value == null || value.trim().isEmpty()) {
            return 0.0;
        }
        try {
            return Double.parseDouble(value);
        } catch (NumberFormatException e) {
            return 0.0;
        }
    }

    /**
     * CONVERTE LONG PARA STRING
     */
    @Named("longToString")
    default String longToString(Long value) {
        return value != null ? value.toString() : null;
    }

}
