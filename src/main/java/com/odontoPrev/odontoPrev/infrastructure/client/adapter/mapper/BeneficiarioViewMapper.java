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
    @Mapping(target = "dataNascimento", source = "dataDeNascimento", qualifiedByName = "stringToLocalDate")
    @Mapping(target = "dtVigenciaRetroativa", source = "dtVigenciaRetroativa", qualifiedByName = "stringToLocalDate")
    @Mapping(target = "nomeMae", source = "nomeDaMae")
    @Mapping(target = "nomeBeneficiario", source = "nomeDoBeneficiario")
    @Mapping(target = "tpEndereco", source = "tpEndereco")
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
    @Mapping(target = "codigoMatricula", source = "cdAssociado") // Usar cdAssociado como matrícula
    @Mapping(target = "codigoEmpresa", source = "cdEmpresa")
    @Mapping(target = "cpf", ignore = true) // View de alteração não tem CPF
    @Mapping(target = "dataNascimento", source = "dataNascimento", qualifiedByName = "stringToLocalDate")
    @Mapping(target = "dtVigenciaRetroativa", source = "dtVigenciaRetroativa", qualifiedByName = "stringToLocalDate")
    @Mapping(target = "nomeMae", source = "nomeDaMae")
    @Mapping(target = "nomeBeneficiario", source = "nomeBeneficiario")
    @Mapping(target = "sexo", source = "sexo")
    @Mapping(target = "telefoneCelular", source = "telefoneCelular")
    @Mapping(target = "telefoneResidencial", source = "telefoneResidencial")
    @Mapping(target = "cep", source = "cep")
    @Mapping(target = "cidade", source = "cidade")
    @Mapping(target = "logradouro", source = "logradouro")
    @Mapping(target = "numero", source = "numero")
    @Mapping(target = "complemento", source = "complemento")
    @Mapping(target = "uf", source = "uf")
    @Mapping(target = "codigoPlano", source = "codigoPlano")
    @Mapping(target = "departamento", source = "departamento")
    @Mapping(target = "rg", source = "rg")
    @Mapping(target = "rgEmissor", source = "rgEmissor")
    @Mapping(target = "estadoCivil", source = "estadoCivil")
    @Mapping(target = "nmCargo", source = "nmCargo")
    @Mapping(target = "pisPasep", source = "pisPasep")
    @Mapping(target = "email", source = "email")
    @Mapping(target = "bairro", source = "bairro")
    @Mapping(target = "statusSincronizacao", constant = "ALTERADO")
    @Mapping(target = "idMotivoInativacao", ignore = true)
    @Mapping(target = "dataInativacao", ignore = true)
    @Mapping(target = "dataSincronizacao", ignore = true)
    @Mapping(target = "mensagemErro", ignore = true)
    @Mapping(target = "dataCriacao", ignore = true)
    @Mapping(target = "dataAtualizacao", ignore = true)
    // Campos que não existem na entidade BeneficiarioOdontoprev - ignorar
    @Mapping(target = "motivoExclusao", ignore = true) // Não existe na entidade
    @Mapping(target = "tpEndereco", ignore = true) // View de alteração não tem tpEndereco
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
    @Mapping(target = "nomeBeneficiario", source = "nome")
    @Mapping(target = "nomeMae", ignore = true)
    @Mapping(target = "sexo", ignore = true)
    @Mapping(target = "telefoneCelular", ignore = true)
    @Mapping(target = "telefoneResidencial", ignore = true)
    @Mapping(target = "codigoEmpresa", source = "cdEmpresa")
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
    @Mapping(target = "idMotivoInativacao", source = "idMotivo")
    @Mapping(target = "dataInativacao", source = "dataInativacao", qualifiedByName = "stringToLocalDate")
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
        
        String dataLimpa = dataString.trim();
        
        // Lista de formatos possíveis
        String[] formatos = {
            "dd/MM/yyyy",    // 08/10/2025
            "dd-MM-yyyy",    // 08-10-2025
            "yyyy-MM-dd",    // 2025-10-08
            "dd/MM/yy",      // 08/10/25
            "dd-MM-yy"       // 08-10-25
        };
        
        for (String formato : formatos) {
            try {
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern(formato);
                return LocalDate.parse(dataLimpa, formatter);
            } catch (Exception e) {
                // Continua tentando outros formatos
                continue;
            }
        }
        
        // Se nenhum formato funcionou, tenta formato ISO padrão
        try {
            return LocalDate.parse(dataLimpa);
        } catch (Exception ex) {
            // Log do erro para debug
            System.err.println("Erro ao converter data: '" + dataString + "' - " + ex.getMessage());
            return null;
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
