package com.odontoPrev.odontoPrev.infrastructure.client.service;

import com.odontoPrev.odontoPrev.domain.service.*;
import com.odontoPrev.odontoPrev.infrastructure.aop.MonitorarOperacao;
import com.odontoPrev.odontoPrev.infrastructure.repository.IntegracaoOdontoprevBeneficiarioRepository;
import com.odontoPrev.odontoPrev.infrastructure.repository.IntegracaoOdontoprevBeneficiarioAlteracaoRepository;
import com.odontoPrev.odontoPrev.infrastructure.repository.IntegracaoOdontoprevBeneficiarioExclusaoRepository;
import com.odontoPrev.odontoPrev.infrastructure.client.adapter.mapper.BeneficiarioViewMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import static com.odontoPrev.odontoPrev.infrastructure.aop.MonitorarOperacao.TipoExcecao.*;

/**
 * SERVIÇO PARA SINCRONIZAÇÃO COMPLETA DE BENEFICIÁRIOS COM ODONTOPREV
 * 
 * FUNÇÃO PRINCIPAL:
 * Esta classe coordena todo o processo de sincronização de beneficiários incluindo:
 * 1. INCLUSÃO: beneficiários novos (view padrão)
 * 2. ALTERAÇÃO: beneficiários modificados (VW_INTEGRACAO_ODONTOPREV_BENEFICIARIOS_ALT)
 * 3. EXCLUSÃO: beneficiários inativados (VW_INTEGRACAO_ODONTOPREV_BENEFICIARIOS_EXC)
 * 
 * ESTRATÉGIA DE PROCESSAMENTO:
 * - Processa cada tipo de operação em sequência
 * - Usa os mesmos parâmetros de lote e threads para consistência
 * - Monitora progresso de cada tipo de operação
 * - Trata erros sem interromper outros tipos
 * 
 * CONFIGURAÇÕES:
 * - batch-size: quantos beneficiários processar por vez
 * - max-threads: quantas threads usar em paralelo
 * 
 * ORDEM DE PROCESSAMENTO:
 * 1. Inclusões (para criar novos registros)
 * 2. Alterações (para atualizar dados existentes)
 * 3. Exclusões (para inativar beneficiários)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SincronizacaoCompletaBeneficiarioServiceImpl implements SincronizacaoCompletaBeneficiarioService {

    // Serviços de sincronização específicos
    private final ProcessamentoBeneficiarioService processamentoInclusoes;
    private final ProcessamentoBeneficiarioAlteracaoService processamentoAlteracoes;
    private final ProcessamentoBeneficiarioExclusaoService processamentoExclusoes;
    
    // Repositórios das views
    private final IntegracaoOdontoprevBeneficiarioRepository inclusaoRepository;
    
    // Repositórios para contagem
    private final IntegracaoOdontoprevBeneficiarioAlteracaoRepository alteracaoRepository;
    private final IntegracaoOdontoprevBeneficiarioExclusaoRepository exclusaoRepository;
    
    // Mapper para conversão entre views e entidades de domínio
    private final BeneficiarioViewMapper beneficiarioViewMapper;
    
    // Configurações
    @Value("${odontoprev.sync.beneficiario.batch-size:50}")
    private int tamanhoBatch;
    
    @Value("${odontoprev.sync.beneficiario.max-threads:5}")
    private int maxThreads;

    /**
     * MÉTODO PRINCIPAL - EXECUTA SINCRONIZAÇÃO COMPLETA
     * 
     * Coordena todo o processo de sincronização incluindo inclusões, alterações e exclusões.
     * Processa em ordem: inclusões → alterações → exclusões
     */
    @Override
    @MonitorarOperacao(
            operacao = "SINCRONIZACAO_COMPLETA_BENEFICIARIOS",
            excecaoEmErro = PROCESSAMENTO_BENEFICIARIO
    )
    public void executarSincronizacaoCompleta() {
        log.info("🚀 SINCRONIZAÇÃO BENEFICIÁRIOS: Iniciando sincronização completa com OdontoPrev");
        
        try {
            // 1. Processa inclusões primeiro (cria novos registros)
            log.info("📝 SINCRONIZAÇÃO BENEFICIÁRIOS: Executando inclusões");
            int inclusoes = executarSincronizacaoInclusoes();
            log.info("✅ SINCRONIZAÇÃO BENEFICIÁRIOS: Inclusões processadas: {}", inclusoes);
        } catch (Exception e) {
            log.error("❌ SINCRONIZAÇÃO BENEFICIÁRIOS: Erro na sincronização de inclusões: {}", e.getMessage());
        }
        
        // TEMPORARIAMENTE DESABILITADO - View de alterações não existe
        // try {
        //     // 2. Processa alterações (atualiza dados existentes)
        //     log.info("📝 SINCRONIZAÇÃO BENEFICIÁRIOS: Executando alterações");
        //     int alteracoes = executarSincronizacaoAlteracoes();
        //     log.info("✅ SINCRONIZAÇÃO BENEFICIÁRIOS: Alterações processadas: {}", alteracoes);
        // } catch (Exception e) {
        //     log.error("❌ SINCRONIZAÇÃO BENEFICIÁRIOS: Erro na sincronização de alterações: {}", e.getMessage());
        // }
        
        // TEMPORARIAMENTE DESABILITADO - View de exclusões com problema de subconsulta
        // try {
        //     // 3. Processa inativações (inativa beneficiários)
        //     log.info("📝 SINCRONIZAÇÃO BENEFICIÁRIOS: Executando inativações");
        //     int inativacoes = executarSincronizacaoInativacoes();
        //     log.info("✅ SINCRONIZAÇÃO BENEFICIÁRIOS: Inativações processadas: {}", inativacoes);
        // } catch (Exception e) {
        //     log.error("❌ SINCRONIZAÇÃO BENEFICIÁRIOS: Erro na sincronização de inativações: {}", e.getMessage());
        // }
        
        log.info("🏁 SINCRONIZAÇÃO BENEFICIÁRIOS: Sincronização completa finalizada");
    }

    /**
     * EXECUTA SINCRONIZAÇÃO APENAS DE INCLUSÕES
     * 
     * Processa beneficiários que são novos e precisam ser incluídos.
     */
    @Override
    @MonitorarOperacao(
            operacao = "SINCRONIZACAO_INCLUSOES_BENEFICIARIOS",
            excecaoEmErro = PROCESSAMENTO_BENEFICIARIO
    )
    public int executarSincronizacaoInclusoes() {
        log.info("🔍 INICIANDO SINCRONIZAÇÃO DE INCLUSÕES - {}", java.time.LocalDateTime.now());
        
        // Conta total de beneficiários para inclusão
        long totalInclusoes = contarTotalInclusoes();
        
        if (totalInclusoes == 0) {
            log.info("📭 NENHUM BENEFICIÁRIO ENCONTRADO PARA INCLUSÃO - Verifique se há novos cadastros na view VW_INTEGRACAO_ODONTOPREV_BENEFICIARIOS");
            return 0;
        }
        
        log.info("📊 BENEFICIÁRIOS ENCONTRADOS: {} beneficiários para inclusão em lotes de {} - {}", totalInclusoes, tamanhoBatch, java.time.LocalDateTime.now());
        
        // Processa inclusões em lotes
        int processados = processarInclusoesEmLotes(totalInclusoes);
        
        log.info("Sincronização de inclusões de beneficiários finalizada - {} processados", processados);
        return processados;
    }

    /**
     * EXECUTA SINCRONIZAÇÃO APENAS DE ALTERAÇÕES
     * 
     * Processa beneficiários que tiveram dados modificados e precisam ser atualizados.
     */
    @Override
    @MonitorarOperacao(
            operacao = "SINCRONIZACAO_ALTERACOES_BENEFICIARIOS",
            excecaoEmErro = PROCESSAMENTO_BENEFICIARIO
    )
    public int executarSincronizacaoAlteracoes() {
        log.info("Iniciando sincronização de alterações de beneficiários");
        
        // Conta total de beneficiários alterados
        long totalAlteracoes = contarTotalAlteracoes();
        
        if (totalAlteracoes == 0) {
            log.info("Nenhum beneficiário alterado encontrado para sincronização");
            return 0;
        }
        
        log.info("Processando {} beneficiários alterados em lotes de {}", totalAlteracoes, tamanhoBatch);
        
        // Processa alterações em lotes
        int processados = processarAlteracoesEmLotes(totalAlteracoes);
        
        log.info("Sincronização de alterações de beneficiários finalizada - {} processados", processados);
        return processados;
    }

    /**
     * EXECUTA SINCRONIZAÇÃO APENAS DE INATIVAÇÕES
     * 
     * Processa beneficiários que foram inativados e precisam ser removidos.
     */
    @Override
    @MonitorarOperacao(
            operacao = "SINCRONIZACAO_INATIVACOES_BENEFICIARIOS",
            excecaoEmErro = PROCESSAMENTO_BENEFICIARIO
    )
    public int executarSincronizacaoInativacoes() {
        log.info("Iniciando sincronização de exclusões de beneficiários");
        
        // Conta total de beneficiários excluídos
        long totalExclusoes = contarTotalExclusoes();
        
        if (totalExclusoes == 0) {
            log.info("Nenhum beneficiário excluído encontrado para sincronização");
            return 0;
        }
        
        log.info("Processando {} beneficiários excluídos em lotes de {}", totalExclusoes, tamanhoBatch);
        
        // Processa exclusões em lotes
        int processados = processarExclusoesEmLotes(totalExclusoes);
        
        log.info("Sincronização de inativações de beneficiários finalizada - {} processados", processados);
        return processados;
    }

    /**
     * OBTÉM ESTATÍSTICAS DA ÚLTIMA SINCRONIZAÇÃO
     * 
     * Retorna informações sobre a última execução.
     */
    @Override
    public SincronizacaoCompletaBeneficiarioService.EstatisticasSincronizacao obterEstatisticasUltimaSincronizacao() {
        // TODO: Implementar lógica de estatísticas
        return new SincronizacaoCompletaBeneficiarioService.EstatisticasSincronizacao(
            java.time.LocalDateTime.now(),
            0, 0, 0, 0, 0, 0, 0L
        );
    }

    /**
     * CONTA TOTAL DE BENEFICIÁRIOS ALTERADOS
     */
    @MonitorarOperacao(
            operacao = "CONTAGEM_ALTERACOES_BENEFICIARIOS",
            logSucesso = MonitorarOperacao.NivelLog.INFO,
            excecaoEmErro = CONSULTA_BENEFICIARIOS
    )
    private long contarTotalAlteracoes() {
        long total = alteracaoRepository.count();
        log.info("📊 CONTAGEM BENEFICIÁRIOS: Total de alterações encontradas: {}", total);
        return total;
    }

    /**
     * CONTA TOTAL DE BENEFICIÁRIOS EXCLUÍDOS
     */
    @MonitorarOperacao(
            operacao = "CONTAGEM_EXCLUSOES_BENEFICIARIOS",
            logSucesso = MonitorarOperacao.NivelLog.INFO,
            excecaoEmErro = CONSULTA_BENEFICIARIOS
    )
    private long contarTotalExclusoes() {
        long total = exclusaoRepository.count();
        log.info("📊 CONTAGEM BENEFICIÁRIOS: Total de exclusões encontradas: {}", total);
        return total;
    }

    /**
     * PROCESSA ALTERAÇÕES EM LOTES
     * 
     * Implementa processamento em lotes para beneficiários alterados.
     */
    private int processarAlteracoesEmLotes(long totalAlteracoes) {
        int beneficiariosProcessados = 0;
        
        // Busca todos os beneficiários alterados de uma vez
        var todosBeneficiarios = alteracaoRepository.findWithLimit();
        
        if (todosBeneficiarios.isEmpty()) {
            log.info("Nenhum beneficiário alterado encontrado");
            return 0;
        }
        
        log.info("Processando {} beneficiários alterados", todosBeneficiarios.size());
        
        // Processa todos os beneficiários
        beneficiariosProcessados = processarLoteAlteracoes(todosBeneficiarios);
        
        log.info("Processamento de alterações concluído - Total processados: {}/{}", 
                beneficiariosProcessados, totalAlteracoes);
        
        return beneficiariosProcessados;
    }

    /**
     * PROCESSA EXCLUSÕES EM LOTES
     * 
     * Implementa processamento em lotes para beneficiários excluídos.
     */
    private int processarExclusoesEmLotes(long totalExclusoes) {
        int beneficiariosProcessados = 0;
        
        // Busca todos os beneficiários excluídos de uma vez
        var todosBeneficiarios = exclusaoRepository.findWithLimit();
        
        if (todosBeneficiarios.isEmpty()) {
            log.info("Nenhum beneficiário excluído encontrado");
            return 0;
        }
        
        log.info("Processando {} beneficiários excluídos", todosBeneficiarios.size());
        
        // Processa todos os beneficiários
        beneficiariosProcessados = processarLoteExclusoes(todosBeneficiarios);
        
        log.info("Processamento de exclusões concluído - Total processados: {}/{}", 
                beneficiariosProcessados, totalExclusoes);
        
        return beneficiariosProcessados;
    }

    /**
     * PROCESSA LOTE DE ALTERAÇÕES
     * 
     * Processa cada beneficiário alterado do lote atual.
     */
    private int processarLoteAlteracoes(java.util.List<com.odontoPrev.odontoPrev.infrastructure.repository.entity.IntegracaoOdontoprevBeneficiarioAlteracao> beneficiarios) {
        int processadosNoLote = 0;
        
        for (var beneficiario : beneficiarios) {
            try {
                // Converte a view para entidade de domínio e processa
                var beneficiarioDomínio = beneficiarioViewMapper.fromAlteracaoView(beneficiario);
                processamentoAlteracoes.processarAlteracaoBeneficiario(beneficiarioDomínio);
                processadosNoLote++;
            } catch (Exception e) {
                log.error("Erro ao processar alteração do beneficiário {}: {}", 
                         beneficiario.getCodigoMatricula(), e.getMessage());
                // Continua processando outros beneficiários
            }
        }
        
        return processadosNoLote;
    }

    /**
     * PROCESSA LOTE DE EXCLUSÕES
     * 
     * Processa cada beneficiário excluído do lote atual.
     */
    private int processarLoteExclusoes(java.util.List<com.odontoPrev.odontoPrev.infrastructure.repository.entity.IntegracaoOdontoprevBeneficiarioExclusao> beneficiarios) {
        int processadosNoLote = 0;
        
        for (var beneficiario : beneficiarios) {
            try {
                // Converte a view para entidade de domínio e processa
                var beneficiarioDomínio = beneficiarioViewMapper.fromExclusaoView(beneficiario);
                processamentoExclusoes.processarInativacaoBeneficiario(beneficiarioDomínio);
                processadosNoLote++;
            } catch (Exception e) {
                log.error("Erro ao processar exclusão do beneficiário {}: {}", 
                         beneficiario.getCodigoMatricula(), e.getMessage());
                // Continua processando outros beneficiários
            }
        }
        
        return processadosNoLote;
    }

    /**
     * CONTA TOTAL DE BENEFICIÁRIOS PARA INCLUSÃO
     */
    private long contarTotalInclusoes() {
        long total = inclusaoRepository.count();
        log.info("🔢 CONTAGEM DA VIEW: VW_INTEGRACAO_ODONTOPREV_BENEFICIARIOS retornou {} registros", total);
        
        // Log adicional para debug - mostra alguns registros da view
        if (total > 0) {
            try {
                var amostra = inclusaoRepository.findAll(PageRequest.of(0, 5, Sort.by("codigoMatricula").ascending()));
                log.info("📋 AMOSTRA DA VIEW (primeiros 5 registros):");
                for (var beneficiario : amostra.getContent()) {
                    log.info("   - Matrícula: {} | Nome: {} | CPF: {}", 
                            beneficiario.getCodigoMatricula(), 
                            beneficiario.getNomeDoBeneficiario(),
                            beneficiario.getCpf());
                }
                
                // Log dos últimos registros também
                if (total > 5) {
                    var ultimos = inclusaoRepository.findAll(PageRequest.of((int)(total-5)/tamanhoBatch, 5, Sort.by("codigoMatricula").ascending()));
                    log.info("📋 ÚLTIMOS REGISTROS DA VIEW:");
                    for (var beneficiario : ultimos.getContent()) {
                        log.info("   - Matrícula: {} | Nome: {} | CPF: {}", 
                                beneficiario.getCodigoMatricula(), 
                                beneficiario.getNomeDoBeneficiario(),
                                beneficiario.getCpf());
                    }
                }
            } catch (Exception e) {
                log.warn("⚠️ Erro ao obter amostra da view: {}", e.getMessage());
            }
        }
        
        return total;
    }

    /**
     * MÉTODO DE DEBUG - VERIFICA REGISTROS ESPECÍFICOS NA VIEW
     */
    private void verificarRegistrosEspecificosNaView() {
        String[] matriculasParaVerificar = {"0069037", "0069032", "0069043", "0069029", "0069034"};
        
        log.info("🔍 VERIFICAÇÃO DE REGISTROS ESPECÍFICOS:");
        for (String matricula : matriculasParaVerificar) {
            try {
                var beneficiario = inclusaoRepository.findByCodigoMatricula(matricula);
                if (beneficiario != null) {
                    log.info("✅ ENCONTRADO - Matrícula: {} | Nome: {} | CPF: {}", 
                            beneficiario.getCodigoMatricula(), 
                            beneficiario.getNomeDoBeneficiario(),
                            beneficiario.getCpf());
                } else {
                    log.warn("❌ NÃO ENCONTRADO - Matrícula: {}", matricula);
                }
            } catch (Exception e) {
                log.error("⚠️ ERRO ao verificar matrícula {}: {}", matricula, e.getMessage());
                // Continua com as outras matrículas mesmo se uma falhar
            }
        }
        
        // DEBUG: Lista TODOS os registros da view para verificar (com paginação para evitar problemas)
        try {
            log.info("🔍 LISTANDO TODOS OS REGISTROS DA VIEW:");
            var todosRegistros = inclusaoRepository.findAll(PageRequest.of(0, 100, Sort.by("codigoMatricula").ascending()));
            log.info("📊 TOTAL DE REGISTROS ENCONTRADOS: {}", todosRegistros.getTotalElements());
            for (var beneficiario : todosRegistros.getContent()) {
                log.info("   - Matrícula: {} | Nome: {} | CPF: {}", 
                        beneficiario.getCodigoMatricula(), 
                        beneficiario.getNomeDoBeneficiario(),
                        beneficiario.getCpf());
            }
        } catch (Exception e) {
            log.error("⚠️ ERRO ao listar todos os registros: {}", e.getMessage());
        }
    }

    /**
     * PROCESSA INCLUSÕES EM LOTES COM PAGINAÇÃO ADEQUADA
     */
    private int processarInclusoesEmLotes(long totalInclusoes) {
        int totalProcessados = 0;
        int paginaAtual = 0;
        
        log.info("🔍 INICIANDO PROCESSAMENTO EM LOTES - Total de beneficiários: {}", totalInclusoes);
        
        // DEBUG: Verifica se os registros específicos estão na view
        verificarRegistrosEspecificosNaView();
        
        // IMPORTANTE: Processa TODAS as páginas até não haver mais registros
        // Não para baseado no totalInclusoes para garantir que novos registros sejam capturados
        while (true) {
            // Cria configuração de paginação - ordena por codigoMatricula para garantir ordem consistente
            Pageable pageable = PageRequest.of(paginaAtual, tamanhoBatch, Sort.by("codigoMatricula").ascending());
            
            // Busca página de beneficiários para inclusão
            Page<com.odontoPrev.odontoPrev.infrastructure.repository.entity.IntegracaoOdontoprevBeneficiario> pagina = inclusaoRepository.findAll(pageable);
            
            if (pagina.isEmpty() || pagina.getContent().isEmpty()) {
                log.info("📭 Nenhum beneficiário encontrado na página {}, finalizando processamento", paginaAtual);
                break;
            }
            
            log.info("📄 PROCESSANDO PÁGINA {} - {} beneficiários encontrados (total na view: {})", 
                    paginaAtual, pagina.getContent().size(), pagina.getTotalElements());
            
            // Log detalhado dos beneficiários da página para debug
            log.info("🔍 BENEFICIÁRIOS DA PÁGINA {}: {}", paginaAtual, 
                    pagina.getContent().stream()
                            .map(b -> b.getCodigoMatricula() + "(" + b.getNomeDoBeneficiario() + ")")
                            .toList());
            
            // Processa cada beneficiário da página
            int processadosNaPagina = processarLoteInclusoes(pagina.getContent());
            totalProcessados += processadosNaPagina;
            
            log.info("✅ PÁGINA {} PROCESSADA - {} beneficiários incluídos (total processados: {})", 
                    paginaAtual, processadosNaPagina, totalProcessados);
            
            // Se não há mais páginas, termina
            if (!pagina.hasNext()) {
                log.info("🏁 Última página processada, finalizando");
                break;
            }
            
            paginaAtual++;
        }
        
        log.info("🎯 PROCESSAMENTO EM LOTES CONCLUÍDO - Total processados: {}", totalProcessados);
        return totalProcessados;
    }

    /**
     * PROCESSA LOTE DE INCLUSÕES
     * 
     * Processa cada beneficiário do lote atual para inclusão.
     */
    private int processarLoteInclusoes(java.util.List<com.odontoPrev.odontoPrev.infrastructure.repository.entity.IntegracaoOdontoprevBeneficiario> beneficiarios) {
        int processadosNoLote = 0;
        
        for (var beneficiario : beneficiarios) {
            try {
                // Converte a view para entidade de domínio e processa
                var beneficiarioDomínio = beneficiarioViewMapper.fromInclusaoView(beneficiario);
                processamentoInclusoes.processarInclusaoBeneficiario(beneficiarioDomínio);
                processadosNoLote++;
            } catch (Exception e) {
                log.error("Erro ao processar inclusão do beneficiário {}: {}", 
                         beneficiario.getCodigoMatricula(), e.getMessage());
                // Continua processando outros beneficiários
            }
        }
        
        return processadosNoLote;
    }
}
