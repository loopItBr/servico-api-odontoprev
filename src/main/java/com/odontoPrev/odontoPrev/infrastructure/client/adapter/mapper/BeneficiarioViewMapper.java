package com.odontoPrev.odontoPrev.infrastructure.client.adapter.mapper;

import com.odontoPrev.odontoPrev.domain.entity.BeneficiarioOdontoprev;
import com.odontoPrev.odontoPrev.infrastructure.repository.entity.IntegracaoOdontoprevBeneficiario;
import com.odontoPrev.odontoPrev.infrastructure.repository.entity.IntegracaoOdontoprevBeneficiarioAlteracao;
import com.odontoPrev.odontoPrev.infrastructure.repository.entity.IntegracaoOdontoprevBeneficiarioExclusao;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.List;

/**
 * MAPPER PARA CONVERSÃO ENTRE ENTIDADES DAS VIEWS E BENEFICIARIOONDONTOPREV
 *
 * Este mapper utiliza MapStruct para realizar conversões automáticas
 * entre as entidades das views de beneficiário e a entidade de domínio
 * BeneficiarioOdontoprev.
 *
 * RESPONSABILIDADES:
 * - Converter entidade da view de inclusão para BeneficiarioOdontoprev
 * - Converter entidade da view de alteração para BeneficiarioOdontoprev
 * - Converter entidade da view de exclusão para BeneficiarioOdontoprev
 * - Tratar conversões específicas e campos com nomes diferentes
 * - Mapear listas de entidades
 *
 * PADRÕES DE MAPEAMENTO:
 * - Campos com nomes idênticos são mapeados automaticamente
 * - Campos renomeados usam @Mapping com source/target
 * - Campos inexistentes são ignorados ou definidos com valores padrão
 * - Status de sincronização é definido conforme o tipo de operação
 */
@Mapper(componentModel = "spring")
public interface BeneficiarioViewMapper {

    /**
     * CONVERTE ENTIDADE DA VIEW DE INCLUSÃO PARA BENEFICIARIOONDONTOPREV
     *
     * Mapeia todos os campos da view VW_INTEGRACAO_ODONTOPREV_BENEFICIARIOS.sql
     * para a entidade de domínio, definindo status como "PENDENTE".
     */
    @Mapping(target = "id", ignore = true) // ID será gerado automaticamente
    @Mapping(target = "cdCgcEstipulante", source = "cdCgcEstipulante") // Agora existe na view entity
    @Mapping(target = "dataNascimento", source = "dataNascimento", qualifiedByName = "stringToLocalDate")
    @Mapping(target = "dtVigenciaRetroativa", source = "dtVigenciaRetroativa", qualifiedByName = "dateToLocalDate")
    @Mapping(target = "nomeMae", source = "nomeDaMae")
    @Mapping(target = "statusSincronizacao", constant = "PENDENTE")
    @Mapping(target = "cdAssociado", ignore = true) // Inclusão não tem código associado ainda
    @Mapping(target = "idMotivoInativacao", ignore = true)
    @Mapping(target = "dataInativacao", ignore = true)
    @Mapping(target = "dataSincronizacao", ignore = true)
    @Mapping(target = "mensagemErro", ignore = true)
    @Mapping(target = "dataCriacao", ignore = true)
    @Mapping(target = "dataAtualizacao", ignore = true)
    // Mapear nrSequencia que agora existe na entidade de domínio
    @Mapping(target = "nrSequencia", source = "nrSequencia")
    // Campos que não existem na entidade de domínio - ignorar
    @Mapping(target = "motivoExclusao", ignore = true) // Campo da view, mas talvez não seja relevante para inclusão
    BeneficiarioOdontoprev fromInclusaoView(IntegracaoOdontoprevBeneficiario viewEntity);

    /**
     * CONVERTE LISTA DA VIEW DE INCLUSÃO
     */
    List<BeneficiarioOdontoprev> fromInclusaoViewList(List<IntegracaoOdontoprevBeneficiario> viewEntities);

    /**
     * CONVERTE ENTIDADE DA VIEW DE ALTERAÇÃO PARA BENEFICIARIOONDONTOPREV
     *
     * Mapeia todos os campos da view VW_INTEGRACAO_ODONTOPREV_BENEFICIARIOS_ALT
     * para a entidade de domínio, definindo status como "ALTERADO".
     */
    @Mapping(target = "id", ignore = true) // ID será gerado automaticamente
    @Mapping(target = "nrSequencia", ignore = true) // Não existe na entidade
    @Mapping(target = "cdCgcEstipulante", ignore = true)
    @Mapping(target = "codigoMatricula", ignore = true) // View de alteração não tem matrícula
    @Mapping(target = "codigoEmpresa", source = "codigoEmpresa")
    @Mapping(target = "dataNascimento", source = "dataNascimento", qualifiedByName = "stringToLocalDate")
    @Mapping(target = "dtVigenciaRetroativa", source = "dtVigenciaRetroativa", qualifiedByName = "dateToLocalDate")
    @Mapping(target = "nomeMae", source = "nomeDaMae")
    @Mapping(target = "statusSincronizacao", constant = "ALTERADO")
    @Mapping(target = "idMotivoInativacao", ignore = true)
    @Mapping(target = "dataInativacao", ignore = true)
    @Mapping(target = "dataSincronizacao", ignore = true)
    @Mapping(target = "mensagemErro", ignore = true)
    @Mapping(target = "dataCriacao", ignore = true)
    @Mapping(target = "dataAtualizacao", ignore = true)
    // Campos que não existem na entidade BeneficiarioOdontoprev - ignorar
    @Mapping(target = "motivoExclusao", ignore = true) // Não existe na entidade
    BeneficiarioOdontoprev fromAlteracaoView(IntegracaoOdontoprevBeneficiarioAlteracao viewEntity);

    /**
     * CONVERTE LISTA DA VIEW DE ALTERAÇÃO
     */
    List<BeneficiarioOdontoprev> fromAlteracaoViewList(List<IntegracaoOdontoprevBeneficiarioAlteracao> viewEntities);

    /**
     * CONVERTE ENTIDADE DA VIEW DE EXCLUSÃO PARA BENEFICIARIOONDONTOPREV
     *
     * Mapeia todos os campos da view VW_INTEGRACAO_ODONTOPREV_BENEFICIARIOS_EXC
     * para a entidade de domínio, definindo status como "EXCLUIDO".
     */
    @Mapping(target = "id", ignore = true) // ID será gerado automaticamente
    @Mapping(target = "nrSequencia", ignore = true) // Não existe na entidade
    @Mapping(target = "cdCgcEstipulante", ignore = true)
    @Mapping(target = "cpf", ignore = true) // View de exclusão não tem CPF
    @Mapping(target = "dataNascimento", ignore = true)
    @Mapping(target = "dtVigenciaRetroativa", ignore = true)
    @Mapping(target = "cep", ignore = true)
    @Mapping(target = "cidade", ignore = true)
    @Mapping(target = "logradouro", ignore = true)
    @Mapping(target = "numero", ignore = true)
    @Mapping(target = "uf", ignore = true)
    @Mapping(target = "nomeBeneficiario", source = "nomeBeneficiario")
    @Mapping(target = "nomeMae", ignore = true)
    @Mapping(target = "sexo", ignore = true)
    @Mapping(target = "telefoneCelular", ignore = true)
    @Mapping(target = "telefoneResidencial", ignore = true)
    @Mapping(target = "codigoEmpresa", source = "codigoEmpresa")
    @Mapping(target = "codigoMatricula", source = "codigoMatricula")
    @Mapping(target = "codigoPlano", ignore = true)
    @Mapping(target = "departamento", ignore = true)
    @Mapping(target = "rg", ignore = true)
    @Mapping(target = "estadoCivil", ignore = true)
    @Mapping(target = "nmCargo", ignore = true)
    @Mapping(target = "grauParentesco", ignore = true)
    @Mapping(target = "pisPasep", ignore = true)
    @Mapping(target = "bairro", ignore = true)
    @Mapping(target = "complemento", ignore = true)
    @Mapping(target = "rgEmissor", ignore = true)
    @Mapping(target = "cns", ignore = true)
    @Mapping(target = "statusSincronizacao", constant = "EXCLUIDO")
    @Mapping(target = "idMotivoInativacao", source = "idMotivoInativacao")
    @Mapping(target = "dataInativacao", source = "dataInativacao", qualifiedByName = "dateToLocalDate")
    @Mapping(target = "dataSincronizacao", ignore = true)
    @Mapping(target = "mensagemErro", ignore = true)
    @Mapping(target = "dataCriacao", ignore = true)
    @Mapping(target = "dataAtualizacao", ignore = true)
    @Mapping(target = "motivoExclusao", ignore = true) // Não existe na entidade
    BeneficiarioOdontoprev fromExclusaoView(IntegracaoOdontoprevBeneficiarioExclusao viewEntity);

    /**
     * CONVERTE LISTA DA VIEW DE EXCLUSÃO
     */
    List<BeneficiarioOdontoprev> fromExclusaoViewList(List<IntegracaoOdontoprevBeneficiarioExclusao> viewEntities);

    /**
     * CONVERTE STRING DE DATA DO BANCO PARA LOCALDATE
     *
     * O banco retorna datas como String no formato DD/MM/YYYY
     * devido à função PLS_OBTER_DADOS_SEGURADO
     *
     * @param dataString data no formato DD/MM/YYYY
     * @return LocalDate convertido ou null
     */
    @Named("stringToLocalDate")
    default LocalDate stringToLocalDate(String dataString) {
        if (dataString == null || dataString.trim().isEmpty()) {
            return null;
        }
        try {
            // Formato esperado do banco: DD/MM/YYYY
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
            return LocalDate.parse(dataString, formatter);
        } catch (Exception e) {
            // Se falhar, tenta formato ISO
            try {
                return LocalDate.parse(dataString);
            } catch (Exception ex) {
                // Log do erro se necessário
                return null;
            }
        }
    }

    /**
     * CONVERTE DATE DO BANCO PARA LOCALDATE
     *
     * Converte java.util.Date ou java.sql.Date para LocalDate
     *
     * @param date Date do banco
     * @return LocalDate convertido ou null
     */
    @Named("dateToLocalDate")
    default LocalDate dateToLocalDate(Date date) {
        if (date == null) {
            return null;
        }

        // java.sql.Date já tem método toLocalDate()
        if (date instanceof java.sql.Date) {
            return ((java.sql.Date) date).toLocalDate();
        }

        // Para java.util.Date normal
        return new java.sql.Date(date.getTime()).toLocalDate();
    }
}
