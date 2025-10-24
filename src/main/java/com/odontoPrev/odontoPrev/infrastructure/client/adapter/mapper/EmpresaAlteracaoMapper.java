package com.odontoPrev.odontoPrev.infrastructure.client.adapter.mapper;

import com.odontoPrev.odontoPrev.infrastructure.client.adapter.out.dto.EmpresaAlteracaoRequest;
import com.odontoPrev.odontoPrev.infrastructure.repository.entity.IntegracaoOdontoprev;
import com.odontoPrev.odontoPrev.infrastructure.repository.entity.IntegracaoOdontoprevAlteracao;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

import java.time.LocalDate;
import java.time.LocalDateTime;

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
    // MAPEAMENTO COMPLETO DOS CAMPOS DA VIEW
    @Mapping(target = "codigoEmpresa", source = "codigoEmpresa") // OBRIGATÓRIO
    @Mapping(target = "nomeFantasia", source = "nomeFantasia") // MODIFICADO
    @Mapping(target = "dataVigencia", source = "dataVigencia", qualifiedByName = "localDateToLocalDateTime") // MODIFICADO
    @Mapping(target = "codigoUsuario", constant = "0") // OBRIGATÓRIO - valor fixo "0" para alteração
    @Mapping(target = "endereco", source = ".", qualifiedByName = "createEnderecoFromView") // OBRIGATÓRIO - criado a partir da view
    @Mapping(target = "telefone", source = ".", qualifiedByName = "createTelefoneFromView") // Criado a partir da view
    @Mapping(target = "grausParentesco", source = ".", qualifiedByName = "createGrausParentescoFromView") // Criado a partir da view
    // CAMPOS DA VIEW QUE ESTÃO DISPONÍVEIS NO DTO
    // codigoGrupoGerencial removido - não deve ser enviado junto com codigoEmpresa
    @Mapping(target = "sinistralidade", ignore = true) // Enviar como null
    // CAMPOS QUE NÃO ESTÃO NA VIEW - IGNORADOS
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
    @Mapping(target = "email", ignore = true)
    @Mapping(target = "codigoNaturezaJuridica", ignore = true)
    @Mapping(target = "nomeNaturezaJuridica", ignore = true)
    @Mapping(target = "situacaoCadastral", ignore = true)
    @Mapping(target = "dataConstituicao", ignore = true)
    @Mapping(target = "renovacaoAutomatica", ignore = true)
    @Mapping(target = "mesAniversarioReajuste", ignore = true)
    @Mapping(target = "anoProximoAniversarioReajuste", ignore = true)
    @Mapping(target = "sistema", ignore = true)
    @Mapping(target = "diaVencimentoPlano", ignore = true)
    @Mapping(target = "diaMovimentacaoCadastral", ignore = true)
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
    @Mapping(target = "endereco", source = ".", qualifiedByName = "createEnderecoFromBase") // OBRIGATÓRIO - criado a partir da entidade base
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
    // codigoGrupoGerencial removido do DTO
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

    /**
     * CRIA ENDEREÇO A PARTIR DOS DADOS DA VIEW
     * 
     * Constrói o objeto Endereco usando os campos de endereço
     * disponíveis na view de alteração.
     */
    @Named("createEnderecoFromView")
    default EmpresaAlteracaoRequest.Endereco createEnderecoFromView(IntegracaoOdontoprevAlteracao view) {
        if (view == null) {
            return createEnderecoPadrao();
        }

        // Log dos dados da view para debug
        System.out.println("🔍 [MAPPER] Dados da view para endereço:");
        System.out.println("   CODIGOCIDADE: '" + view.getCodigoCidade() + "'");
        System.out.println("   CIDADE: '" + view.getCidade() + "'");
        System.out.println("   SIGLAUF: '" + view.getSiglaUf() + "'");
        System.out.println("   LOGRADOURO: '" + view.getLogradouro() + "'");
        System.out.println("   NUMERO: '" + view.getNumero() + "'");
        System.out.println("   BAIRRO: '" + view.getBairro() + "'");
        System.out.println("   CEP: '" + view.getCep() + "'");

        // Se não há dados de endereço na view, usa endereço padrão
        if (view.getLogradouro() == null || view.getLogradouro().trim().isEmpty()) {
            System.out.println("⚠️ [MAPPER] Logradouro vazio, usando endereço padrão");
            return createEnderecoPadrao();
        }

        EmpresaAlteracaoRequest.Endereco endereco = EmpresaAlteracaoRequest.Endereco.builder()
            .descricao("Endereço da empresa")
            .complemento("")
            .tipoLogradouro("2") // Sempre 2 (numérico como string)
            .logradouro(view.getLogradouro())
            .numero(view.getNumero() != null ? view.getNumero() : "S/N")
            .bairro(view.getBairro() != null ? view.getBairro() : "Centro")
            .cidade(EmpresaAlteracaoRequest.Cidade.builder()
                .codigo(parsearCodigoCidade(view.getCodigoCidade()))
                .nome(view.getCidade() != null ? view.getCidade() : "São Paulo")
                .siglaUf(view.getSiglaUf() != null ? view.getSiglaUf() : "SP")
                .codigoPais(view.getCodigoPais() != null ? view.getCodigoPais().intValue() : 1)
                .build())
            .cep(view.getCep() != null ? view.getCep() : "01000-000")
            .build();
            
        // Log do endereço construído
        System.out.println("✅ [MAPPER] Endereço construído:");
        System.out.println("   tipoLogradouro: '" + endereco.getTipoLogradouro() + "'");
        System.out.println("   logradouro: '" + endereco.getLogradouro() + "'");
        System.out.println("   numero: '" + endereco.getNumero() + "'");
        System.out.println("   bairro: '" + endereco.getBairro() + "'");
        System.out.println("   cep: '" + endereco.getCep() + "'");
        System.out.println("   cidade.codigo: " + endereco.getCidade().getCodigo());
        System.out.println("   cidade.nome: '" + endereco.getCidade().getNome() + "'");
        System.out.println("   cidade.siglaUf: '" + endereco.getCidade().getSiglaUf() + "'");
        System.out.println("   cidade.codigoPais: " + endereco.getCidade().getCodigoPais());
        
        return endereco;
    }

    /**
     * CRIA ENDEREÇO A PARTIR DA ENTIDADE BASE
     * 
     * Para a entidade IntegracaoOdontoprev (que não tem campos de endereço),
     * sempre retorna endereço padrão.
     */
    @Named("createEnderecoFromBase")
    default EmpresaAlteracaoRequest.Endereco createEnderecoFromBase(IntegracaoOdontoprev empresa) {
        return createEnderecoPadrao();
    }



    /**
     * PARSEIA CÓDIGO DA CIDADE COM TRATAMENTO ROBUSTO
     * 
     * Converte o CODIGOCIDADE da view para Integer com tratamento de erros.
     */
    default Integer parsearCodigoCidade(String codigoCidade) {
        if (codigoCidade == null || codigoCidade.trim().isEmpty()) {
            return 3670; // Código padrão
        }
        
        try {
            // Remove espaços e converte para número
            String codigoLimpo = codigoCidade.trim();
            return Integer.parseInt(codigoLimpo);
        } catch (NumberFormatException e) {
            // Se não conseguir converter, retorna código padrão
            return 3670;
        }
    }

    /**
     * CRIA TELEFONE A PARTIR DOS DADOS DA VIEW
     * 
     * Cria objeto telefone com dados padrão (a view não tem campos de telefone).
     */
    @Named("createTelefoneFromView")
    default EmpresaAlteracaoRequest.Telefone createTelefoneFromView(IntegracaoOdontoprevAlteracao view) {
        return EmpresaAlteracaoRequest.Telefone.builder()
            .telefone1("(11) 0000-0000")
            .telefone2("")
            .celular("")
            .fax("")
            .build();
    }

    /**
     * CRIA GRAUS DE PARENTESCO A PARTIR DOS DADOS DA VIEW
     * 
     * Cria lista de graus de parentesco padrão (a view não tem campos de grau de parentesco).
     */
    @Named("createGrausParentescoFromView")
    default java.util.List<EmpresaAlteracaoRequest.GrauParentesco> createGrausParentescoFromView(IntegracaoOdontoprevAlteracao view) {
        return java.util.Collections.singletonList(
            EmpresaAlteracaoRequest.GrauParentesco.builder()
                .codigoGrauParentesco(1) // Cônjuge
                .build()
        );
    }

    /**
     * CRIA ENDEREÇO PADRÃO
     * 
     * Usado quando não há dados de endereço na view.
     */
    default EmpresaAlteracaoRequest.Endereco createEnderecoPadrao() {
        return EmpresaAlteracaoRequest.Endereco.builder()
            .descricao("Endereço padrão")
            .complemento("")
            .tipoLogradouro("2") // Sempre 2 (numérico como string)
            .logradouro("Rua das Flores")
            .numero("123")
            .bairro("Centro")
            .cidade(EmpresaAlteracaoRequest.Cidade.builder()
                .codigo(3670) // Código padrão
                .nome("São Paulo")
                .siglaUf("SP")
                .codigoPais(1)
                .build())
            .cep("01000-000")
            .build();
    }

}
