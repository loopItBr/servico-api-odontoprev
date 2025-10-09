package com.odontoPrev.odontoPrev.infrastructure.client.adapter.mapper;

import com.odontoPrev.odontoPrev.infrastructure.client.adapter.out.dto.EmpresaInativacaoRequest;
import com.odontoPrev.odontoPrev.infrastructure.repository.entity.IntegracaoOdontoprev;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;

/**
 * MAPPER PARA CONVERSÃO DE DADOS DE INATIVAÇÃO DE EMPRESA
 *
 * Este mapper converte dados da entidade IntegracaoOdontoprev para o formato
 * esperado pela API OdontoPrev para inativação de empresas.
 *
 * CONVERSÕES REALIZADAS:
 * - Entidade -> EmpresaInativacaoRequest
 * - Formatação de datas (LocalDate -> String YYYY-MM-DD)
 * - Mapeamento de campos específicos para inativação
 */
@Mapper(componentModel = "spring")
public interface EmpresaInativacaoMapper {

    /**
     * CONVERTE ENTIDADE PARA REQUEST DE INATIVAÇÃO
     *
     * @param entidade dados da empresa a ser inativada
     * @param sistema código da empresa (vem do header)
     * @return request formatado para a API
     */
    @Mapping(target = "sistema", source = "sistema")
    @Mapping(target = "codigoUsuario", constant = "0")
    @Mapping(target = "listaDadosInativacaoEmpresa", source = "entidade", qualifiedByName = "criarListaInativacao")
    EmpresaInativacaoRequest toInativacaoRequest(IntegracaoOdontoprev entidade, String sistema);

    /**
     * CRIA LISTA DE DADOS DE INATIVAÇÃO
     *
     * Converte uma entidade em uma lista contendo os dados de inativação.
     * A API espera uma lista mesmo para uma única empresa.
     */
    @Named("criarListaInativacao")
    default List<EmpresaInativacaoRequest.DadosInativacaoEmpresa> criarListaInativacao(IntegracaoOdontoprev entidade) {
        if (entidade == null) {
            return Collections.emptyList();
        }

        try {
            // Formata código da empresa para 6 dígitos com zeros à esquerda
            String codigoEmpresaFormatado = formatarCodigoEmpresa(entidade.getCodigoEmpresa());
            
            EmpresaInativacaoRequest.DadosInativacaoEmpresa dados = EmpresaInativacaoRequest.DadosInativacaoEmpresa.builder()
                    .codigoEmpresa(codigoEmpresaFormatado)
                    .codigoMotivoFimEmpresa("1") // Valor padrão - pode ser configurável
                    .codigoMotivoInativacao("2") // Valor padrão - pode ser configurável
                    .dataFimContrato(formatarData(entidade.getDataFimContrato()))
                    .build();

            return Collections.singletonList(dados);
        } catch (Exception e) {
            // Log do erro e retorna lista vazia para evitar quebrar a transação
            System.err.println("Erro ao criar lista de inativação: " + e.getMessage());
            return Collections.emptyList();
        }
    }

    /**
     * FORMATA CÓDIGO DA EMPRESA PARA 6 DÍGITOS
     *
     * @param codigoEmpresa código da empresa
     * @return código formatado com zeros à esquerda
     */
    @Named("formatarCodigoEmpresa")
    default String formatarCodigoEmpresa(String codigoEmpresa) {
        if (codigoEmpresa == null || codigoEmpresa.trim().isEmpty()) {
            return "000000";
        }
        
        // Remove espaços e formata para 6 dígitos com zeros à esquerda
        String codigoLimpo = codigoEmpresa.trim().replaceAll("\\s+", "");
        if (codigoLimpo.length() >= 6) {
            return codigoLimpo.substring(0, 6);
        } else {
            return String.format("%-6s", codigoLimpo).replace(' ', '0');
        }
    }

    /**
     * FORMATA DATA PARA STRING YYYY-MM-DD
     *
     * @param data data a ser formatada
     * @return data formatada ou data atual se null
     */
    @Named("formatarData")
    default String formatarData(LocalDate data) {
        if (data == null) {
            // Se não há data de fim, usa data atual
            return LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE);
        }
        return data.format(DateTimeFormatter.ISO_LOCAL_DATE);
    }
}
