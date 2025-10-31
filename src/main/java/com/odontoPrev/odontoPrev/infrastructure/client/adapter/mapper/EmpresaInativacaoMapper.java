package com.odontoPrev.odontoPrev.infrastructure.client.adapter.mapper;

import com.odontoPrev.odontoPrev.infrastructure.client.adapter.out.dto.EmpresaInativacaoRequest;
import com.odontoPrev.odontoPrev.infrastructure.repository.entity.IntegracaoOdontoprev;
import com.odontoPrev.odontoPrev.infrastructure.repository.entity.IntegracaoOdontoprevExclusao;
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
     * CONVERTE DADOS DE EXCLUSÃO PARA REQUEST DE INATIVAÇÃO
     *
     * IMPORTANTE: Usa os valores da view VW_INTEGRACAO_ODONTOPREV_EXC:
     * - sistema: campo SISTEMA da view (CHAR(10)), ou fallback se null
     * - codigoUsuario: campo CODIGOUSUARIO da view (NUMBER), convertido para String
     * 
     * @param dadosExclusao dados da empresa excluída da view
     * @param sistemaFallback código da empresa do header (usado apenas se SISTEMA da view for null)
     * @return request formatado para a API
     */
    default EmpresaInativacaoRequest toInativacaoRequestExclusao(IntegracaoOdontoprevExclusao dadosExclusao, String sistemaFallback) {
        // Usa sistema da view ou fallback
        String sistema = usarSistemaOuFallback(dadosExclusao.getSistema(), sistemaFallback);
        
        // Converte codigoUsuario da view
        String codigoUsuario = codigoUsuarioToString(dadosExclusao.getCodigoUsuario());
        
        // Cria lista de dados de inativação
        List<EmpresaInativacaoRequest.DadosInativacaoEmpresa> listaDados = criarListaInativacaoExclusao(dadosExclusao);
        
        return EmpresaInativacaoRequest.builder()
                .sistema(sistema)
                .codigoUsuario(codigoUsuario)
                .listaDadosInativacaoEmpresa(listaDados)
                .build();
    }

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
                    .dataFimContrato(formatarData(null)) // Campo removido da view
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
     * CRIA LISTA DE DADOS DE INATIVAÇÃO A PARTIR DE DADOS DE EXCLUSÃO
     *
     * Converte dados da view de exclusão em uma lista contendo os dados de inativação.
     * Usa os dados específicos da view de exclusão (motivo, data fim, etc.).
     */
    @Named("criarListaInativacaoExclusao")
    default List<EmpresaInativacaoRequest.DadosInativacaoEmpresa> criarListaInativacaoExclusao(IntegracaoOdontoprevExclusao dadosExclusao) {
        if (dadosExclusao == null) {
            return Collections.emptyList();
        }

        try {
            // Formata código da empresa para 6 dígitos com zeros à esquerda
            String codigoEmpresaFormatado = formatarCodigoEmpresa(dadosExclusao.getCodigoEmpresa());
            
            // Usa o motivo da view de exclusão - obrigatório na API
            String motivoFim;
            if (dadosExclusao.getCodigoMotivoFimEmpresa() != null) {
                motivoFim = dadosExclusao.getCodigoMotivoFimEmpresa().toString();
            } else {
                throw new IllegalArgumentException("CODIGOMOTIVOFIMEMPRESA é obrigatório na view VW_INTEGRACAO_ODONTOPREV_EXC");
            }
            
            // Usa a data da view de exclusão - obrigatória na API
            String dataFim;
            if (dadosExclusao.getDataFimContrato() != null) {
                dataFim = formatarData(dadosExclusao.getDataFimContrato());
            } else {
                throw new IllegalArgumentException("DATA_FIM_CONTRATO é obrigatória na view VW_INTEGRACAO_ODONTOPREV_EXC");
            }
            
            EmpresaInativacaoRequest.DadosInativacaoEmpresa dados = EmpresaInativacaoRequest.DadosInativacaoEmpresa.builder()
                    .codigoEmpresa(codigoEmpresaFormatado)
                    .codigoMotivoFimEmpresa(motivoFim) // Obrigatório - da view
                    .codigoMotivoInativacao(null) // Opcional conforme documentação
                    .dataFimContrato(dataFim) // Obrigatório - da view
                    .build();

            return Collections.singletonList(dados);
        } catch (Exception e) {
            // Log do erro e retorna lista vazia para evitar quebrar a transação
            System.err.println("Erro ao criar lista de inativação de exclusão: " + e.getMessage());
            return Collections.emptyList();
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

    /**
     * USA SISTEMA DA VIEW OU FALLBACK
     * 
     * Se o campo SISTEMA da view estiver preenchido, usa ele.
     * Caso contrário, usa o sistemaFallback (do header).
     * 
     * @param sistema sistema da view (CHAR(10))
     * @param sistemaFallback sistema do header (fallback)
     * @return sistema da view ou fallback
     */
    default String usarSistemaOuFallback(String sistema, String sistemaFallback) {
        if (sistema != null && !sistema.trim().isEmpty()) {
            return sistema.trim();
        }
        // Se sistema da view for null ou vazio, usa o fallback do header
        if (sistemaFallback != null && !sistemaFallback.trim().isEmpty()) {
            return sistemaFallback.trim();
        }
        // Se ambos forem null, retorna null (validação do DTO vai reclamar)
        return null;
    }

    /**
     * CONVERTE CODIGO USUÁRIO (LONG) PARA STRING
     * 
     * Converte o campo CODIGOUSUARIO da view (NUMBER) para String.
     * Se for null, retorna "0" conforme especificação da API.
     */
    @Named("codigoUsuarioToString")
    default String codigoUsuarioToString(Long codigoUsuario) {
        if (codigoUsuario == null) {
            return "0"; // Valor padrão obrigatório conforme documentação
        }
        return codigoUsuario.toString();
    }
}
