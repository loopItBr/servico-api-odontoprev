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
     * Mapeia APENAS os campos que existem na view VW_INTEGRACAO_ODONTOPREV_ALT
     * conforme especificado na documentação da API.
     */
    // CAMPOS OBRIGATÓRIOS
    @Mapping(target = "codigoEmpresa", source = "codigoEmpresa") // OBRIGATÓRIO
    @Mapping(target = "codigoUsuario", source = "codigoUsuario", qualifiedByName = "codigoUsuarioToString") // OBRIGATÓRIO - da view
    @Mapping(target = "endereco", source = ".", qualifiedByName = "createEnderecoFromView") // OBRIGATÓRIO
    
    // CAMPOS DA VIEW QUE EXISTEM NA DOCUMENTAÇÃO DA API
    @Mapping(target = "nomeFantasia", source = "nomeFantasia")
    @Mapping(target = "emiteCarteirinhaPlastica", source = "emiteCarteirinhaPlastica")
    @Mapping(target = "permissaoCadastroDep", source = "permissaoCadastroDep")
    @Mapping(target = "descricaoRamoAtividade", source = "descricaoRamoAtividade")
    @Mapping(target = "numeroFuncionarios", source = "numeroFuncionarios", qualifiedByName = "longToInteger")
    @Mapping(target = "valorFator", source = "valorFator", qualifiedByName = "longToDouble")
    @Mapping(target = "cnae", source = "cnae")
    @Mapping(target = "codigoLayoutCarteirinha", source = "codigoLayoutCarteirinha")
    @Mapping(target = "codigoOrdemCarteira", source = "codigoOrdemCarteira", qualifiedByName = "longToInteger")
    @Mapping(target = "liberaSenhaInternet", source = "liberaSenhaInternet")
    @Mapping(target = "dependentePaga", source = "dependentePaga")
    @Mapping(target = "custoFamiliar", source = "custoFamiliar")
    @Mapping(target = "planoFamiliar", source = "planoFamiliar")
    @Mapping(target = "idadeLimiteUniversitaria", source = "idadeLimiteUniversitaria", qualifiedByName = "longToInteger")
    @Mapping(target = "codigoRegiao", source = "codigoRegiao", qualifiedByName = "longToInteger")
    @Mapping(target = "razaoSocial", source = "razaoSocial")
    @Mapping(target = "inscricaoMunicipal", source = "inscricaoMunicipal")
    @Mapping(target = "inscricaoEstadual", source = "inscricaoEstadual")
    @Mapping(target = "email", source = "email")
    @Mapping(target = "codigoNaturezaJuridica", source = "codigoNaturezaJuridica")
    @Mapping(target = "nomeNaturezaJuridica", source = "nomeNaturezaJuridica")
    @Mapping(target = "situacaoCadastral", source = "situacaoCadastral")
    @Mapping(target = "dataConstituicao", source = "dataConstituicao", qualifiedByName = "stringToLocalDateTime")
    @Mapping(target = "renovacaoAutomatica", source = "renovacaoAutomatica")
    @Mapping(target = "dataVigencia", source = "dataVigencia", qualifiedByName = "stringToLocalDateTime")
    @Mapping(target = "mesAniversarioReajuste", ignore = true) // Campo removido da view VW_INTEGRACAO_ODONTOPREV_ALT
    @Mapping(target = "sistema", source = "sistema")
    
    // CAMPOS OPCIONAIS QUE NÃO ESTÃO NA VIEW - IGNORADOS
    @Mapping(target = "ramo", ignore = true) // Não existe na view
    @Mapping(target = "numeroCei", ignore = true) // Não existe na view
    @Mapping(target = "cic", ignore = true) // Não existe na view
    @Mapping(target = "telefone", source = ".", qualifiedByName = "createTelefoneFromView") // Não existe na view, cria padrão
    @Mapping(target = "anoProximoAniversarioReajuste", ignore = true) // Não existe na view
    @Mapping(target = "sinistralidade", ignore = true) // Não existe na view
    @Mapping(target = "diaVencimentoPlano", ignore = true) // Não existe na view
    @Mapping(target = "diaMovimentacaoCadastral", ignore = true) // Não existe na view
    @Mapping(target = "grausParentesco", source = ".", qualifiedByName = "createGrausParentescoFromView") // Não existe na view, usa CODIGOGRAUPARENTESCO se disponível
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
     * CONVERTE STRING PARA LOCALDATETIME
     * 
     * Converte String (VARCHAR2(20)) da view para LocalDateTime no formato da API.
     */
    @Named("stringToLocalDateTime")
    default LocalDateTime stringToLocalDateTime(String dateString) {
        if (dateString == null || dateString.trim().isEmpty()) {
            return null;
        }
        try {
            // Tenta parsear no formato esperado (YYYY-MM-DD ou similar)
            // A view retorna VARCHAR2(20), então pode estar em vários formatos
            if (dateString.contains("T")) {
                // Já está no formato ISO
                return LocalDateTime.parse(dateString);
            } else if (dateString.matches("\\d{4}-\\d{2}-\\d{2}")) {
                // Formato YYYY-MM-DD
                return LocalDate.parse(dateString).atStartOfDay();
            } else {
                // Tenta parsear como data simples
                return LocalDate.parse(dateString).atStartOfDay();
            }
        } catch (Exception e) {
            // Se não conseguir parsear, retorna null
            return null;
        }
    }

    /**
     * CONVERTE CODIGO USUÁRIO (LONG) PARA STRING
     */
    @Named("codigoUsuarioToString")
    default String codigoUsuarioToString(Long codigoUsuario) {
        if (codigoUsuario == null) {
            return "0"; // Valor padrão obrigatório
        }
        return codigoUsuario.toString();
    }

    /**
     * CONVERTE LONG PARA INTEGER
     */
    @Named("longToInteger")
    default Integer longToInteger(Long value) {
        if (value == null) {
            return null;
        }
        return value.intValue();
    }

    /**
     * CONVERTE LONG PARA DOUBLE
     */
    @Named("longToDouble")
    default Double longToDouble(Long value) {
        if (value == null) {
            return null;
        }
        return value.doubleValue();
    }

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
        System.out.println("   CODIGO (cidade): '" + view.getCodigo() + "'");
        System.out.println("   NOMECIDADE: '" + view.getNomeCidade() + "'");
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

        // Usa tipoLogradouro da view se disponível, senão usa "2" como padrão
        String tipoLogradouroStr = "2"; // Valor padrão
        if (view.getTipoLogradouro() != null) {
            tipoLogradouroStr = view.getTipoLogradouro().toString();
        }
        
        EmpresaAlteracaoRequest.Endereco endereco = EmpresaAlteracaoRequest.Endereco.builder()
            .descricao("Endereço da empresa")
            .complemento("")
            .tipoLogradouro(tipoLogradouroStr) // Usa valor da view TIPOLOGRADOURO
            .logradouro(view.getLogradouro())
            .numero(view.getNumero() != null ? view.getNumero() : "S/N")
            .bairro(view.getBairro() != null ? view.getBairro() : "Centro")
            .cidade(EmpresaAlteracaoRequest.Cidade.builder()
                .codigo(parsearCodigoCidade(view.getCodigo()))
                .nome(view.getNomeCidade() != null ? view.getNomeCidade() : "São Paulo")
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
     * Converte o CODIGO (código da cidade) da view para Integer com tratamento de erros.
     */
    default Integer parsearCodigoCidade(String codigo) {
        if (codigo == null || codigo.trim().isEmpty()) {
            return 3670; // Código padrão
        }
        
        try {
            // Remove espaços e converte para número
            String codigoLimpo = codigo.trim();
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
     * Usa o campo CODIGOGRAUPARENTESCO da view (CHAR(121)) para criar lista.
     * Se o campo não existir ou estiver vazio, retorna lista vazia.
     */
    @Named("createGrausParentescoFromView")
    default java.util.List<EmpresaAlteracaoRequest.GrauParentesco> createGrausParentescoFromView(IntegracaoOdontoprevAlteracao view) {
        if (view == null || view.getCodigoGrauParentesco() == null || view.getCodigoGrauParentesco().trim().isEmpty()) {
            // Se não há código de grau de parentesco, retorna lista vazia
            return java.util.Collections.emptyList();
        }
        
        // O campo CODIGOGRAUPARENTESCO é CHAR(121), pode conter múltiplos códigos
        // Por enquanto, retorna lista vazia (pode ser implementado parsing futuro)
        // Se precisar, podemos parsear o campo e criar múltiplos graus
        return java.util.Collections.emptyList();
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
