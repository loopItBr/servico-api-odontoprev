package com.odontoPrev.odontoPrev.infrastructure.client.adapter.mapper;

import com.odontoPrev.odontoPrev.domain.entity.BeneficiarioOdontoprev;
import com.odontoPrev.odontoPrev.infrastructure.client.adapter.out.dto.BeneficiarioAlteracaoRequest;
import com.odontoPrev.odontoPrev.infrastructure.client.adapter.out.dto.BeneficiarioInclusaoRequest;
// import com.odontoPrev.odontoPrev.infrastructure.client.adapter.out.dto.BeneficiarioInativacaoRequest;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

import java.time.LocalDate;

/**
 * MAPPER PARA CONVERSÃO ENTRE ENTIDADES E DTOS DE BENEFICIÁRIOS
 *
 * Este mapper utiliza MapStruct para realizar conversões automáticas
 * entre a entidade BeneficiarioOdontoprev e os DTOs utilizados na
 * comunicação com a API da OdontoPrev.
 *
 * RESPONSABILIDADES:
 * - Converter entidade para DTO de inclusão
 * - Converter entidade para DTO de alteração
 * - Converter entidade para DTO de inativação
 * - Tratar conversões específicas (datas, campos renomeados)
 * - Aplicar valores fixos conforme especificação
 *
 * PADRÕES DE MAPEAMENTO:
 * - Campos com nomes idênticos são mapeados automaticamente
 * - Campos renomeados usam @Mapping com source/target
 * - Datas são convertidas de LocalDate para String
 * - Campos opcionais são mapeados quando não nulos
 * - Valores fixos são definidos com expressões
 */
@Mapper(componentModel = "spring")
public interface BeneficiarioMapper {

    /**
     * CONVERTE ENTIDADE PARA DTO DE INCLUSÃO
     *
     * Mapeia todos os campos necessários para inclusão de um novo
     * beneficiário na OdontoPrev através do endpoint POST /incluir.
     *
     * MAPEAMENTOS ESPECIAIS:
     * - Datas convertidas para String formato ISO (YYYY-MM-DD)
     * - USUARIO fixo como "PONTETECH" conforme especificação
     * - Campos com nomes diferentes mapeados explicitamente
     */
    @Mapping(target = "cpf", source = "cpf")
    @Mapping(target = "dataNascimento", source = "dataNascimento", qualifiedByName = "localDateToString")
    @Mapping(target = "dtVigenciaRetroativa", source = "dtVigenciaRetroativa", qualifiedByName = "localDateToString")
    @Mapping(target = "cep", source = "cep")
    @Mapping(target = "cidade", source = "cidade")
    @Mapping(target = "logradouro", source = "logradouro")
    @Mapping(target = "numero", source = "numero")
    @Mapping(target = "uf", source = "uf")
    @Mapping(target = "nomeBeneficiario", source = "nomeBeneficiario")
    @Mapping(target = "nomeDaMae", source = "nomeMae")
    @Mapping(target = "sexo", source = "sexo")
    @Mapping(target = "telefoneCelular", source = "telefoneCelular")
    @Mapping(target = "telefoneResidencial", source = "telefoneResidencial")
    @Mapping(target = "usuario", constant = "PONTETECH")
    @Mapping(target = "codigoEmpresa", source = "codigoEmpresa")
    @Mapping(target = "codigoPlano", source = "codigoPlano")
    @Mapping(target = "departamento", source = "departamento")
    // Campos opcionais
    @Mapping(target = "rg", source = "rg")
    @Mapping(target = "estadoCivil", source = "estadoCivil")
    @Mapping(target = "nmCargo", source = "nmCargo")
    @Mapping(target = "grauParentesco", source = "grauParentesco")
    @Mapping(target = "pisPasep", source = "pisPasep")
    @Mapping(target = "bairro", source = "bairro")
    @Mapping(target = "email", source = "email")
    @Mapping(target = "complemento", source = "complemento")
    @Mapping(target = "rgEmissor", source = "rgEmissor")
    @Mapping(target = "cns", source = "cns")
    // Campos não mapeados (usar ignore ou default)
    @Mapping(target = "identificacao", ignore = true)
    @Mapping(target = "acao", ignore = true)
    @Mapping(target = "dataAssociacao", ignore = true)
    @Mapping(target = "motivoExclusao", ignore = true)
    @Mapping(target = "tipoExclusao", ignore = true)
    @Mapping(target = "nrBanco", ignore = true)
    @Mapping(target = "nrAgencia", ignore = true)
    @Mapping(target = "nrConta", ignore = true)
    @Mapping(target = "digConta", ignore = true)
    @Mapping(target = "digAgencia", ignore = true)
    @Mapping(target = "tipoConta", ignore = true)
    @Mapping(target = "codPaisEmissor", ignore = true)
    @Mapping(target = "codMunicipioIbge", ignore = true)
    @Mapping(target = "indResidencia", ignore = true)
    @Mapping(target = "tpEndereco", ignore = true)
    @Mapping(target = "cidadeResidencia", ignore = true)
    @Mapping(target = "dnv", ignore = true)
    @Mapping(target = "numPortabilidade", ignore = true)
    @Mapping(target = "tempoContribuicao", ignore = true)
    @Mapping(target = "dirPermanencia", ignore = true)
    BeneficiarioInclusaoRequest toInclusaoRequest(BeneficiarioOdontoprev beneficiario);

    /**
     * CONVERTE ENTIDADE PARA DTO DE ALTERAÇÃO
     *
     * Mapeia campos necessários para alteração de um beneficiário
     * existente na OdontoPrev através do endpoint PUT /alterar.
     *
     * CAMPOS OBRIGATÓRIOS:
     * - cdEmpresa (mapeado de codigoEmpresa)
     * - cdAssociado (já preenchido na entidade)
     * - codigoPlano
     * - departamento
     */
    @Mapping(target = "cdEmpresa", source = "codigoEmpresa")
    @Mapping(target = "cdAssociado", source = "cdAssociado")
    @Mapping(target = "codigoPlano", source = "codigoPlano")
    @Mapping(target = "departamento", source = "departamento")
    // Campos opcionais que podem ser alterados
    @Mapping(target = "dtVigenciaRetroativa", source = "dtVigenciaRetroativa", qualifiedByName = "localDateToString")
    @Mapping(target = "dataNascimento", source = "dataNascimento", qualifiedByName = "localDateToString")
    @Mapping(target = "telefoneCelular", source = "telefoneCelular")
    @Mapping(target = "telefoneResidencial", source = "telefoneResidencial")
    @Mapping(target = "rg", source = "rg")
    @Mapping(target = "estadoCivil", source = "estadoCivil")
    @Mapping(target = "nmCargo", source = "nmCargo")
    @Mapping(target = "cpf", source = "cpf")
    @Mapping(target = "sexo", source = "sexo")
    @Mapping(target = "nomeDaMae", source = "nomeMae")
    @Mapping(target = "pisPasep", source = "pisPasep")
    @Mapping(target = "bairro", source = "bairro")
    @Mapping(target = "email", source = "email")
    @Mapping(target = "rgEmissor", source = "rgEmissor")
    @Mapping(target = "nomeBeneficiario", source = "nomeBeneficiario")
    @Mapping(target = "cns", source = "cns")
    @Mapping(target = "cep", source = "cep")
    @Mapping(target = "cidade", source = "cidade")
    @Mapping(target = "logradouro", source = "logradouro")
    @Mapping(target = "numero", source = "numero")
    @Mapping(target = "complemento", source = "complemento")
    @Mapping(target = "uf", source = "uf")
    // Campos não utilizados na alteração
    @Mapping(target = "identificacao", ignore = true)
    @Mapping(target = "motivoExclusao", ignore = true)
    @Mapping(target = "acao", ignore = true)
    @Mapping(target = "tipoExclusao", ignore = true)
    @Mapping(target = "nrBanco", ignore = true)
    @Mapping(target = "nrAgencia", ignore = true)
    @Mapping(target = "nrConta", ignore = true)
    @Mapping(target = "digConta", ignore = true)
    @Mapping(target = "digAgencia", ignore = true)
    @Mapping(target = "endResidencial", ignore = true)
    @Mapping(target = "plano", ignore = true)
    @Mapping(target = "codPaisEmissor", ignore = true)
    @Mapping(target = "codMunicipioIbge", ignore = true)
    @Mapping(target = "indResidencia", ignore = true)
    @Mapping(target = "nrLogradouro", ignore = true)
    @Mapping(target = "tipoEndereco", ignore = true)
    @Mapping(target = "cidadeResidencia", ignore = true)
    BeneficiarioAlteracaoRequest toAlteracaoRequest(BeneficiarioOdontoprev beneficiario);

    /**
     * CONVERTE ENTIDADE PARA DTO DE INATIVAÇÃO
     *
     * Mapeia campos necessários para inativação de um beneficiário
     * na OdontoPrev através do endpoint POST /inativarAssociadoEmpresarial.
     *
     * CAMPOS OBRIGATÓRIOS:
     * - cdEmpresa (mapeado de codigoEmpresa)
     * - cdUsuario (fixo como "PONTETECH")
     * - cdMatricula (mapeado de codigoMatricula)
     * - cdAssociado
     * - nome (mapeado de nomeBeneficiario)
     * - idMotivo (mapeado de idMotivoInativacao)
     */
    /*
    @Mapping(target = "cdEmpresa", source = "codigoEmpresa")
    @Mapping(target = "cdUsuario", constant = "PONTETECH")
    @Mapping(target = "cdMatricula", source = "codigoMatricula")
    @Mapping(target = "cdAssociado", source = "cdAssociado")
    @Mapping(target = "nome", source = "nomeBeneficiario")
    @Mapping(target = "idMotivo", source = "idMotivoInativacao")
    // Campos opcionais
    @Mapping(target = "dataInativacao", source = "dataInativacao", qualifiedByName = "localDateToString")
    @Mapping(target = "email", source = "email")
    BeneficiarioInativacaoRequest toInativacaoRequest(BeneficiarioOdontoprev beneficiario);
    */

    /**
     * MÉTODO AUXILIAR PARA CONVERSÃO DE DATA
     *
     * Converte LocalDate para String no formato ISO (YYYY-MM-DD)
     * que é esperado pela API da OdontoPrev.
     */
    @Named("localDateToString")
    default String localDateToString(LocalDate date) {
        return date != null ? date.toString() : null;
    }
}
