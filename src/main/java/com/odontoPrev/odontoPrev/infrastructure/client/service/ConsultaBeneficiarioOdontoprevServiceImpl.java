package com.odontoPrev.odontoPrev.infrastructure.client.service;

import com.odontoPrev.odontoPrev.domain.entity.BeneficiarioOdontoprev;
import com.odontoPrev.odontoPrev.domain.service.ConsultaBeneficiarioOdontoprevService;
import com.odontoPrev.odontoPrev.infrastructure.aop.MonitorarOperacao;
import com.odontoPrev.odontoPrev.infrastructure.exception.ConsultaBeneficiarioException;
import com.odontoPrev.odontoPrev.infrastructure.client.adapter.mapper.BeneficiarioViewMapper;
import com.odontoPrev.odontoPrev.infrastructure.repository.IntegracaoOdontoprevBeneficiarioAlteracaoRepository;
import com.odontoPrev.odontoPrev.infrastructure.repository.IntegracaoOdontoprevBeneficiarioExclusaoRepository;
import com.odontoPrev.odontoPrev.infrastructure.repository.IntegracaoOdontoprevBeneficiarioRepository;
import com.odontoPrev.odontoPrev.infrastructure.repository.entity.IntegracaoOdontoprevBeneficiario;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;


import static com.odontoPrev.odontoPrev.infrastructure.aop.MonitorarOperacao.TipoExcecao.CONSULTA_BENEFICIARIOS;

/**
 * IMPLEMENTAÇÃO DO SERVIÇO DE CONSULTA DE BENEFICIÁRIOS COM LÓGICA DE NEGÓCIO
 *
 * Implementa operações que envolvem lógica específica de negócio
 * sobre os dados de beneficiários das views.
 *
 * RESPONSABILIDADES:
 * - Operações que combinam múltiplas consultas
 * - Lógica de negócio específica
 * - Agregações e cálculos complexos
 * - Tratamento de casos especiais
 *
 * NOTA: Para consultas simples, use diretamente os repositórios + mapper
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ConsultaBeneficiarioOdontoprevServiceImpl implements ConsultaBeneficiarioOdontoprevService {

    private final IntegracaoOdontoprevBeneficiarioRepository beneficiarioRepository;
    private final IntegracaoOdontoprevBeneficiarioAlteracaoRepository beneficiarioAlteracaoRepository;
    private final IntegracaoOdontoprevBeneficiarioExclusaoRepository beneficiarioExclusaoRepository;
    private final BeneficiarioViewMapper beneficiarioViewMapper;


    /**
     * BUSCA BENEFICIÁRIO ESPECÍFICO POR MATRÍCULA
     *
     * Tenta localizar beneficiário na view de inclusões usando o repositório.
     * Para beneficiários em outras situações, use os repositórios específicos.
     */
    @Override
    @MonitorarOperacao(
            operacao = "BUSCAR_BENEFICIARIO_POR_MATRICULA",
            incluirParametros = {"codigoMatricula"},
            excecaoEmErro = CONSULTA_BENEFICIARIOS
    )
    public BeneficiarioOdontoprev buscarBeneficiarioPorMatricula(String codigoMatricula) {
        try {
            // Busca na view de inclusões usando o repositório
            IntegracaoOdontoprevBeneficiario viewEntity = beneficiarioRepository.findByCodigoMatricula(codigoMatricula);

            if (viewEntity != null) {
                return beneficiarioViewMapper.fromInclusaoView(viewEntity);
            }

            log.info("Beneficiário com matrícula {} não encontrado", codigoMatricula);
            return null;

        } catch (DataAccessException e) {
            log.error("Erro ao buscar beneficiário por matrícula {}: {}", codigoMatricula, e.getMessage(), e);
            throw new ConsultaBeneficiarioException(
                    "Falha na busca por matrícula: " + e.getMessage(),
                    e
            );
        }
    }

    /**
     * CONTA TOTAL DE BENEFICIÁRIOS PENDENTES EM TODAS AS OPERAÇÕES
     *
     * Utiliza os repositórios JPA para contar beneficiários pendentes
     * em cada view, seguindo o mesmo padrão das views de empresa.
     */
    @Override
    @MonitorarOperacao(
            operacao = "CONTAR_BENEFICIARIOS_PENDENTES",
            excecaoEmErro = CONSULTA_BENEFICIARIOS
    )
    public ContadoresPendentes contarBeneficiariosPendentes() {
        try {
            // Conta inclusões pendentes usando o repositório
            long pendentesInclusao = beneficiarioRepository.count();

            // Conta alterações pendentes usando o repositório
            long pendentesAlteracao = beneficiarioAlteracaoRepository.count();

            // Conta exclusões pendentes usando o repositório
            long pendentesExclusao = beneficiarioExclusaoRepository.count();

            // Calcula o total geral
            long totalPendentes = pendentesInclusao + pendentesAlteracao + pendentesExclusao;

            log.info("Contadores: {} inclusões, {} alterações, {} exclusões. Total: {}",
                    pendentesInclusao, pendentesAlteracao, pendentesExclusao, totalPendentes);

            return new ContadoresPendentes(pendentesInclusao, pendentesAlteracao, pendentesExclusao, totalPendentes);

        } catch (DataAccessException e) {
            log.error("Erro ao contar beneficiários pendentes: {}", e.getMessage(), e);
            throw new ConsultaBeneficiarioException(
                    "Falha na contagem de beneficiários pendentes: " + e.getMessage(),
                    e
            );
        }
    }

}
