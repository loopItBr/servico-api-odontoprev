package com.odontoPrev.odontoPrev.infrastructure.client.service;

import com.odontoPrev.odontoPrev.domain.entity.BeneficiarioOdontoprev;
import com.odontoPrev.odontoPrev.domain.entity.ControleSyncBeneficiario;
import com.odontoPrev.odontoPrev.domain.repository.BeneficiarioOdontoprevRepository;
import com.odontoPrev.odontoPrev.domain.repository.ControleSyncBeneficiarioRepository;
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

import java.time.LocalDateTime;

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
    
    // Implementação do serviço de processamento para acessar métodos internos
    private final ProcessamentoBeneficiarioServiceImpl processamentoBeneficiarioService;
    
    // Repositórios das views
    private final IntegracaoOdontoprevBeneficiarioRepository inclusaoRepository;
    
    // Repositórios para contagem
    private final IntegracaoOdontoprevBeneficiarioAlteracaoRepository alteracaoRepository;
    private final IntegracaoOdontoprevBeneficiarioExclusaoRepository exclusaoRepository;
    
    // Repositório de controle de sincronização
    private final ControleSyncBeneficiarioRepository controleSyncRepository;
    
    // Mapper para conversão entre views e entidades de domínio
    private final BeneficiarioViewMapper beneficiarioViewMapper;
    
    // Repositório de beneficiários para verificação por CPF
    private final BeneficiarioOdontoprevRepository beneficiarioRepository;
    
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
        
        try {
            // 2. Processa alterações (atualiza dados existentes)
            log.info("📝 SINCRONIZAÇÃO BENEFICIÁRIOS: Executando alterações");
            int alteracoes = executarSincronizacaoAlteracoes();
            log.info("✅ SINCRONIZAÇÃO BENEFICIÁRIOS: Alterações processadas: {}", alteracoes);
        } catch (Exception e) {
            log.error("❌ SINCRONIZAÇÃO BENEFICIÁRIOS: Erro na sincronização de alterações: {}", e.getMessage());
        }
        
        try {
            // 3. Processa inativações (inativa beneficiários)
            log.info("📝 SINCRONIZAÇÃO BENEFICIÁRIOS: Executando inativações");
            int inativacoes = executarSincronizacaoInativacoes();
            log.info("✅ SINCRONIZAÇÃO BENEFICIÁRIOS: Inativações processadas: {}", inativacoes);
        } catch (Exception e) {
            log.error("❌ SINCRONIZAÇÃO BENEFICIÁRIOS: Erro na sincronização de inativações: {}", e.getMessage());
        }
        
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
        
            // PRIMEIRO: Buscar dependentes diretamente da view para garantir processamento
        try {
            log.error("🔍 BUSCANDO DEPENDENTES DIRETAMENTE DA VIEW...");
            var dependentes = inclusaoRepository.findByIdentificacao("D");
            log.error("📊 TOTAL DE DEPENDENTES ENCONTRADOS NA VIEW: {}", dependentes.size());
            
            // Remover duplicatas baseado em CPF para evitar processar o mesmo dependente múltiplas vezes
            java.util.Map<String, com.odontoPrev.odontoPrev.infrastructure.repository.entity.IntegracaoOdontoprevBeneficiario> dependentesUnicos = new java.util.LinkedHashMap<>();
            for (var dep : dependentes) {
                String cpfLimpo = dep.getCpf() != null ? dep.getCpf().replaceAll("[^0-9]", "") : "";
                if (!cpfLimpo.isEmpty() && !dependentesUnicos.containsKey(cpfLimpo)) {
                    dependentesUnicos.put(cpfLimpo, dep);
                    log.error("👨‍👩‍👧‍👦 DEPENDENTE NA VIEW - Matrícula: {} | CPF: {} | Nome: {} | IDENTIFICACAO: '{}' | codigoAssociadoTitular: '{}' | Empresa: {}", 
                            dep.getCodigoMatricula(), 
                            dep.getCpf(),
                            dep.getNomeDoBeneficiario(),
                            dep.getIdentificacao(),
                            dep.getCodigoAssociadoTitular(),
                            dep.getCodigoEmpresa());
                } else if (!cpfLimpo.isEmpty()) {
                    log.warn("⚠️ DEPENDENTE DUPLICADO IGNORADO - CPF: {} | Matrícula: {} (já existe no lote)", 
                            cpfLimpo, dep.getCodigoMatricula());
                }
            }
            
            var dependentesParaProcessar = new java.util.ArrayList<>(dependentesUnicos.values());
            log.error("📊 DEPENDENTES ÚNICOS APÓS REMOÇÃO DE DUPLICATAS: {} (de {} totais)", 
                    dependentesParaProcessar.size(), dependentes.size());
            
            // Processar dependentes encontrados diretamente
            if (!dependentesParaProcessar.isEmpty()) {
                log.error("🚨 PROCESSANDO {} DEPENDENTES ÚNICOS DIRETAMENTE DA CONSULTA ESPECÍFICA", dependentesParaProcessar.size());
                int processadosDep = processarLoteInclusoes(dependentesParaProcessar);
                log.error("✅ {} DEPENDENTES PROCESSADOS DIRETAMENTE", processadosDep);
            } else {
                log.error("⚠️ NENHUM DEPENDENTE ÚNICO PARA PROCESSAR");
            }
        } catch (Exception e) {
            log.error("❌ ERRO ao buscar dependentes diretamente: {}", e.getMessage(), e);
        }
        
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
                         beneficiario.getCdEmpresa(), e.getMessage());
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
                    String tipo = "T".equals(beneficiario.getIdentificacao()) ? "TITULAR" : 
                                 "D".equals(beneficiario.getIdentificacao()) ? "DEPENDENTE" : 
                                 "DESCONHECIDO(" + beneficiario.getIdentificacao() + ")";
                    log.info("   - Matrícula: {} | Nome: {} | CPF: {} | Tipo: {} | IDENTIFICACAO: {}", 
                            beneficiario.getCodigoMatricula(), 
                            beneficiario.getNomeDoBeneficiario(),
                            beneficiario.getCpf(),
                            tipo,
                            beneficiario.getIdentificacao());
                }
                
                // Log dos últimos registros também
                if (total > 5) {
                    var ultimos = inclusaoRepository.findAll(PageRequest.of((int)(total-5)/tamanhoBatch, 5, Sort.by("codigoMatricula").ascending()));
                    log.info("📋 ÚLTIMOS REGISTROS DA VIEW:");
                    for (var beneficiario : ultimos.getContent()) {
                        String tipo = "T".equals(beneficiario.getIdentificacao()) ? "TITULAR" : 
                                     "D".equals(beneficiario.getIdentificacao()) ? "DEPENDENTE" : 
                                     "DESCONHECIDO(" + beneficiario.getIdentificacao() + ")";
                        log.info("   - Matrícula: {} | Nome: {} | CPF: {} | Tipo: {} | IDENTIFICACAO: {}", 
                                beneficiario.getCodigoMatricula(), 
                                beneficiario.getNomeDoBeneficiario(),
                                beneficiario.getCpf(),
                                tipo,
                                beneficiario.getIdentificacao());
                    }
                }
                
                // Log de contagem por tipo (Titular/Dependente)
                long totalTitulares = inclusaoRepository.countByIdentificacao("T");
                long totalDependentes = inclusaoRepository.countByIdentificacao("D");
                log.info("📊 RESUMO POR TIPO - Titulares: {} | Dependentes: {} | Total: {}", 
                        totalTitulares, totalDependentes, total);
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
        String[] matriculasParaVerificar = {"0069037", "0069032", "0069043", "0069029", "0069034", "0069114"};
        
        log.info("🔍 VERIFICAÇÃO DE REGISTROS ESPECÍFICOS:");
        for (String matricula : matriculasParaVerificar) {
            try {
                var beneficiario = inclusaoRepository.findByCodigoMatricula(matricula);
                if (beneficiario != null) {
                    String tipo = "T".equals(beneficiario.getIdentificacao()) ? "TITULAR" : 
                                 "D".equals(beneficiario.getIdentificacao()) ? "DEPENDENTE" : 
                                 "DESCONHECIDO(" + (beneficiario.getIdentificacao() != null ? beneficiario.getIdentificacao() : "NULL") + ")";
                    log.info("✅ ENCONTRADO - Matrícula: {} | Nome: {} | CPF: {} | Tipo: {} | IDENTIFICACAO: {}", 
                            beneficiario.getCodigoMatricula(), 
                            beneficiario.getNomeDoBeneficiario(),
                            beneficiario.getCpf(),
                            tipo,
                            beneficiario.getIdentificacao());
                } else {
                    log.warn("❌ NÃO ENCONTRADO - Matrícula: {}", matricula);
                }
            } catch (Exception e) {
                log.error("⚠️ ERRO ao verificar matrícula {}: {}", matricula, e.getMessage());
                // Continua com as outras matrículas mesmo se uma falhar
            }
        }
        
        // VERIFICAÇÃO ESPECÍFICA DE DEPENDENTES
        try {
            log.info("🔍 VERIFICAÇÃO ESPECÍFICA DE DEPENDENTES NA VIEW:");
            var dependentes = inclusaoRepository.findByIdentificacao("D");
            log.info("📊 TOTAL DE DEPENDENTES ENCONTRADOS: {}", dependentes.size());
            for (var dependente : dependentes) {
                log.info("👨‍👩‍👧‍👦 DEPENDENTE - Matrícula: {} | Nome: {} | CPF: {} | IDENTIFICACAO: '{}' | codigoAssociadoTitular: '{}'", 
                        dependente.getCodigoMatricula(),
                        dependente.getNomeDoBeneficiario(),
                        dependente.getCpf(),
                        dependente.getIdentificacao(),
                        dependente.getCodigoAssociadoTitular());
            }
            
            // Verificar por empresa específica
            var dependentesEmpresa794472 = inclusaoRepository.findByCodigoEmpresaAndIdentificacao("794472", "D");
            log.info("📊 DEPENDENTES DA EMPRESA 794472: {}", dependentesEmpresa794472.size());
            for (var dep : dependentesEmpresa794472) {
                log.info("👨‍👩‍👧‍👦 DEPENDENTE EMPRESA 794472 - Matrícula: {} | CPF: {} | IDENTIFICACAO: '{}'", 
                        dep.getCodigoMatricula(), dep.getCpf(), dep.getIdentificacao());
            }
        } catch (Exception e) {
            log.error("❌ ERRO ao verificar dependentes na view: {}", e.getMessage(), e);
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
                            .map(b -> {
                                String tipo = "T".equals(b.getIdentificacao()) ? "T" : 
                                             "D".equals(b.getIdentificacao()) ? "D" : 
                                             "?(" + (b.getIdentificacao() != null ? b.getIdentificacao() : "NULL") + ")";
                                return b.getCodigoMatricula() + "[" + tipo + "](" + b.getNomeDoBeneficiario() + ")";
                            })
                            .toList());
            
            // Log EXTREMAMENTE DETALHADO de cada beneficiário da página
            for (var b : pagina.getContent()) {
                String identRaw = b.getIdentificacao();
                String identNorm = identRaw != null ? identRaw.trim().toUpperCase() : null;
                boolean isD = "D".equals(identNorm);
                boolean isT = "T".equals(identNorm);
                
                // Log com nível ERROR para garantir visibilidade
                log.error("📋 DETALHES DO BENEFICIÁRIO NA PÁGINA {} - Matrícula: {} | Nome: {} | CPF: {} | IDENTIFICACAO RAW: '{}' | NORMALIZADA: '{}' (null? {}, empty? {}, equals D? {}, equals T? {})", 
                        paginaAtual,
                        b.getCodigoMatricula(),
                        b.getNomeDoBeneficiario(),
                        b.getCpf(),
                        identRaw,
                        identNorm,
                        identRaw == null,
                        identRaw != null && identRaw.trim().isEmpty(),
                        isD,
                        isT);
                
                // Alerta crítico para dependentes
                if (isD) {
                    log.error("🚨🚨🚨🚨 DEPENDENTE ENCONTRADO NA PÁGINA {} - Matrícula: {} | CPF: {} | Nome: {} | IDENTIFICACAO: '{}' | codigoAssociadoTitular: '{}'", 
                            paginaAtual, b.getCodigoMatricula(), b.getCpf(), b.getNomeDoBeneficiario(), identRaw, b.getCodigoAssociadoTitular());
                }
            }
            
            // Contar dependentes na página (usando comparação normalizada)
            long countDependentes = pagina.getContent().stream()
                    .filter(b -> {
                        String id = b.getIdentificacao();
                        return id != null && "D".equals(id.trim().toUpperCase());
                    })
                    .count();
            long countTitulares = pagina.getContent().stream()
                    .filter(b -> {
                        String id = b.getIdentificacao();
                        return id != null && "T".equals(id.trim().toUpperCase());
                    })
                    .count();
            long countOutros = pagina.getContent().size() - countDependentes - countTitulares;
            
            log.warn("📊 CONTAGEM DA PÁGINA {} - Titulares: {} | Dependentes: {} | Outros/NULL: {} | Total: {}", 
                    paginaAtual, countTitulares, countDependentes, countOutros, pagina.getContent().size());
            
            // ALERTA CRÍTICO se houver dependentes mas não foram contados
            if (countDependentes == 0 && pagina.getContent().stream().anyMatch(b -> {
                String id = b.getIdentificacao();
                return id != null && id.trim().equalsIgnoreCase("d");
            })) {
                log.error("🚨🚨🚨 ERRO CRÍTICO - Dependentes detectados mas não contados corretamente!");
            }
            
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
     * Verifica se o beneficiário já foi processado com sucesso para evitar reprocessamento.
     */
    private int processarLoteInclusoes(java.util.List<com.odontoPrev.odontoPrev.infrastructure.repository.entity.IntegracaoOdontoprevBeneficiario> beneficiarios) {
        int processadosNoLote = 0;
        int jaProcessados = 0;
        
        // Set para rastrear CPFs já processados neste lote (evitar processar o mesmo beneficiário duas vezes)
        java.util.Set<String> cpfProcessadosNoLote = new java.util.HashSet<>();
        
        log.info("🔄 INICIANDO PROCESSAMENTO DO LOTE - {} beneficiários no lote", beneficiarios.size());
        
        for (var beneficiario : beneficiarios) {
            // Verificar se já foi processado neste lote (evitar duplicatas)
            String cpfBeneficiario = beneficiario.getCpf() != null ? beneficiario.getCpf().replaceAll("[^0-9]", "") : "";
            if (!cpfBeneficiario.isEmpty() && cpfProcessadosNoLote.contains(cpfBeneficiario)) {
                log.warn("⚠️ BENEFICIÁRIO JÁ PROCESSADO NESTE LOTE - CPF: {} | Matrícula: {} - Pulando para evitar duplicata", 
                        cpfBeneficiario, beneficiario.getCodigoMatricula());
                continue;
            }
            try {
                // Log CRÍTICO antes de qualquer processamento
                String identificacaoRaw = beneficiario.getIdentificacao();
                // Normalizar identificacao (trim e uppercase para comparação robusta)
                String identificacaoNormalizada = identificacaoRaw != null ? identificacaoRaw.trim().toUpperCase() : null;
                boolean isDependente = "D".equals(identificacaoNormalizada);
                boolean isTitular = "T".equals(identificacaoNormalizada);
                
                log.warn("🚨 INÍCIO DO LOOP - Matrícula: {} | CPF: {} | IDENTIFICACAO RAW: '{}' | NORMALIZADA: '{}' | isDependente? {} | isTitular? {}", 
                        beneficiario.getCodigoMatricula(),
                        beneficiario.getCpf(),
                        identificacaoRaw,
                        identificacaoNormalizada,
                        isDependente,
                        isTitular);
                
                String tipo = isTitular ? "TITULAR" : 
                             isDependente ? "DEPENDENTE" : 
                             "DESCONHECIDO(" + (identificacaoRaw != null ? identificacaoRaw : "NULL") + ")";
                
                log.warn("🔍 PROCESSANDO BENEFICIÁRIO - Matrícula: {} | Nome: {} | Tipo: {} | IDENTIFICACAO: '{}' | CPF: {}", 
                        beneficiario.getCodigoMatricula(), 
                        beneficiario.getNomeDoBeneficiario(),
                        tipo,
                        identificacaoRaw,
                        beneficiario.getCpf());
                
                // ALERTA CRÍTICO se for dependente
                if (isDependente) {
                    log.error("🚨🚨🚨 DEPENDENTE ENCONTRADO NO LOOP - Matrícula: {} | CPF: {} | Continuando processamento...", 
                            beneficiario.getCodigoMatricula(), beneficiario.getCpf());
                }
                
                // Verifica se o beneficiário já foi processado com sucesso
                // IMPORTANTE: Usa CPF para verificação pois dependentes podem ter mesma matrícula do titular
                // IMPORTANTE: Verificar TAMBÉM para dependentes se já foi processado com SUCESSO
                boolean jaProcessado = jaFoiProcessadoComSucessoPorCpf(beneficiario.getCodigoEmpresa(), beneficiario.getCpf(), "INCLUSAO");
                
                if (isDependente) {
                    if (jaProcessado) {
                        log.warn("⏭️ DEPENDENTE JÁ PROCESSADO COM SUCESSO - Matrícula: {} | CPF: {} - Pulando para evitar reprocessamento", 
                                beneficiario.getCodigoMatricula(), beneficiario.getCpf());
                    } else {
                        log.info("👨‍👩‍👧‍👦 DEPENDENTE SERÁ PROCESSADO - Matrícula: {} | CPF: {} - Ainda não processado com sucesso", 
                                beneficiario.getCodigoMatricula(), beneficiario.getCpf());
                    }
                }
                
                log.info("🔍 [VERIFICAÇÃO] Beneficiário {} ({} - CPF: {}) - jaProcessado: {}", 
                        beneficiario.getCodigoMatricula(), tipo, beneficiario.getCpf(), jaProcessado);
                
                if (jaProcessado) {
                    log.info("⏭️ BENEFICIÁRIO JÁ PROCESSADO - {} ({}) [{}] CPF: {} já foi processado com sucesso, pulando", 
                            beneficiario.getCodigoMatricula(), 
                            beneficiario.getNomeDoBeneficiario(),
                            tipo,
                            beneficiario.getCpf());
                    jaProcessados++;
                    continue;
                }
                
                log.info("✅ BENEFICIÁRIO SERÁ PROCESSADO - {} ({}) [{}] CPF: {} não foi processado ainda, iniciando processamento", 
                        beneficiario.getCodigoMatricula(), 
                        beneficiario.getNomeDoBeneficiario(),
                        tipo,
                        beneficiario.getCpf());
                
                // Log específico para dependentes
                if (isDependente) {
                    log.error("👨‍👩‍👧‍👦 DEPENDENTE DETECTADO - Iniciando processamento de dependente | Matrícula: {} | CPF: {} | codigoAssociadoTitular na view: '{}'", 
                            beneficiario.getCodigoMatricula(), 
                            beneficiario.getCpf(),
                            beneficiario.getCodigoAssociadoTitular());
                }
                
                // Converte a view para entidade de domínio e processa
                BeneficiarioOdontoprev beneficiarioDomínio = null;
                try {
                    // Validação prévia dos dados obrigatórios da view antes da conversão
                    if (beneficiario.getCodigoMatricula() == null || beneficiario.getCodigoMatricula().trim().isEmpty()) {
                        throw new IllegalArgumentException("Código da matrícula é obrigatório e está vazio na view");
                    }
                    if (beneficiario.getCpf() == null || beneficiario.getCpf().trim().isEmpty()) {
                        throw new IllegalArgumentException("CPF é obrigatório e está vazio na view");
                    }
                    if (beneficiario.getCodigoEmpresa() == null || beneficiario.getCodigoEmpresa().trim().isEmpty()) {
                        throw new IllegalArgumentException("Código da empresa é obrigatório e está vazio na view");
                    }
                    
                    // DEBUG: Log dos valores da view antes da conversão
                    log.info("🔍 [DEBUG VIEW] Antes da conversão - Matrícula: {} | codigoAssociadoTitular: '{}' | usuario: {}", 
                            beneficiario.getCodigoMatricula(), 
                            beneficiario.getCodigoAssociadoTitular(),
                            beneficiario.getUsuario());
                    
                    // Tentar converter a view para entidade de domínio
                    beneficiarioDomínio = beneficiarioViewMapper.fromInclusaoView(beneficiario);
                    
                    // Validação pós-conversão para garantir que a conversão foi bem-sucedida
                    if (beneficiarioDomínio == null) {
                        throw new IllegalStateException("A conversão da view retornou null - dados podem estar inválidos");
                    }
                    
                    // DEBUG: Log dos valores após a conversão
                    log.info("🔍 [DEBUG DOMINIO] Após a conversão - Matrícula: {} | codigoAssociadoTitularTemp: '{}' | usuarioTemp: {}", 
                            beneficiarioDomínio.getCodigoMatricula(),
                            beneficiarioDomínio.getCodigoAssociadoTitularTemp(),
                            beneficiarioDomínio.getUsuarioTemp());
                    
                    log.info("✅ INICIANDO PROCESSAMENTO - Matrícula: {} | Tipo: {} | CPF: {}", 
                            beneficiario.getCodigoMatricula(), tipo, beneficiario.getCpf());
                    
                    // Log adicional para dependentes antes de processar
                    if (isDependente) {
                        log.error("👨‍👩‍👧‍👦 PROCESSANDO DEPENDENTE - Chamando processarInclusaoBeneficiario para dependente | Matrícula: {} | CPF: {} | codigoAssociadoTitularTemp: '{}'", 
                                beneficiarioDomínio.getCodigoMatricula(),
                                beneficiarioDomínio.getCpf(),
                                beneficiarioDomínio.getCodigoAssociadoTitularTemp());
                    }
                    
                    try {
                        processamentoInclusoes.processarInclusaoBeneficiario(beneficiarioDomínio);
                        processadosNoLote++;
                        
                        // Marcar como processado neste lote
                        if (!cpfBeneficiario.isEmpty()) {
                            cpfProcessadosNoLote.add(cpfBeneficiario);
                        }
                        
                        // Log de sucesso após processamento
                        if (isDependente) {
                            log.error("✅ DEPENDENTE PROCESSADO COM SUCESSO - Matrícula: {} | CPF: {}", 
                                    beneficiario.getCodigoMatricula(), beneficiario.getCpf());
                        }
                    } catch (com.odontoPrev.odontoPrev.infrastructure.exception.ProcessamentoBeneficiarioException processamentoEx) {
                        // Se for ProcessamentoBeneficiarioException, verificar se é dependente já cadastrado
                        String mensagemEx = processamentoEx.getMessage() != null ? processamentoEx.getMessage() : "";
                        boolean dependenteJaCadastrado = (mensagemEx.contains("existe para o titular") || 
                                                         mensagemEx.contains("417") ||
                                                         (mensagemEx.contains("Dependente") && mensagemEx.contains("existe")));
                        
                        if (isDependente && dependenteJaCadastrado) {
                            log.warn("⚠️ DEPENDENTE JÁ CADASTRADO (capturado no catch interno) - {}: Não será registrado na TBSYNC", 
                                    beneficiario.getCodigoMatricula());
                            // NÃO registrar na TBSYNC - apenas continuar
                            continue;
                        }
                        // Se não for "já cadastrado", relançar para ser capturado pelo catch externo
                        throw processamentoEx;
                    } catch (Exception processamentoEx) {
                        // Se for outra exceção durante o processamento, verificar se é dependente já cadastrado
                        String mensagemEx = processamentoEx.getMessage() != null ? processamentoEx.getMessage() : "";
                        String causaEx = (processamentoEx.getCause() != null && processamentoEx.getCause().getMessage() != null) ? 
                                        processamentoEx.getCause().getMessage() : "";
                        String mensagemCompletaEx = mensagemEx + " " + causaEx;
                        
                        boolean dependenteJaCadastrado = (mensagemCompletaEx.contains("existe para o titular") || 
                                                         mensagemCompletaEx.contains("417") ||
                                                         (mensagemCompletaEx.contains("Dependente") && mensagemCompletaEx.contains("existe")));
                        
                        if (isDependente && dependenteJaCadastrado) {
                            log.warn("⚠️ DEPENDENTE JÁ CADASTRADO (capturado no catch interno) - {}: Não será registrado na TBSYNC", 
                                    beneficiario.getCodigoMatricula());
                            // NÃO registrar na TBSYNC - apenas continuar
                            continue;
                        }
                        // Se não for "já cadastrado", relançar para ser capturado pelo catch externo
                        throw processamentoEx;
                    }
                } catch (Exception mappingException) {
                    // Se deu erro na conversão, extrair a causa real da exceção
                    Throwable causaReal = mappingException;
                    String mensagemErroReal = mappingException.getMessage();
                    
                    // Se a exceção foi encapsulada (ex: ProcessamentoLoteException), extrair a causa original
                    if (mappingException.getCause() != null) {
                        causaReal = mappingException.getCause();
                        mensagemErroReal = causaReal.getMessage() != null ? causaReal.getMessage() : mensagemErroReal;
                    }
                    
                    // Log detalhado do erro real
                    log.error("❌ ERRO NA CONVERSÃO DA VIEW - Beneficiário {} | Tipo Exceção: {} | Erro: {} | Causa: {} | StackTrace: ", 
                             beneficiario.getCodigoMatricula(),
                             mappingException.getClass().getSimpleName(),
                             mensagemErroReal,
                             causaReal.getClass().getSimpleName(),
                             causaReal);
                    
                    if (beneficiarioDomínio == null) {
                        // Criar beneficiário mínimo para poder registrar erro na TBSYNC
                        beneficiarioDomínio = BeneficiarioOdontoprev.builder()
                                .codigoMatricula(beneficiario.getCodigoMatricula())
                                .codigoEmpresa(beneficiario.getCodigoEmpresa())
                                .nomeBeneficiario(beneficiario.getNomeDoBeneficiario() != null ? 
                                        beneficiario.getNomeDoBeneficiario() : "N/A")
                                .cpf(beneficiario.getCpf() != null ? beneficiario.getCpf() : "")
                                .identificacao(beneficiario.getIdentificacao()) // IMPORTANTE: Preencher identificacao
                                .codigoPlano(beneficiario.getCodigoPlano() != null ? String.valueOf(beneficiario.getCodigoPlano()) : null)
                                .build();
                    }
                    
                    // Verificar se é erro de dependente já cadastrado ANTES de registrar na TBSYNC
                    String mensagemCompleta = mensagemErroReal;
                    if (causaReal.getMessage() != null && !causaReal.getMessage().equals(mensagemErroReal)) {
                        mensagemCompleta = mensagemErroReal + " | Causa: " + causaReal.getMessage();
                    }
                    
                    boolean dependenteJaCadastradoMapping = (mensagemCompleta.contains("existe para o titular") || 
                                                           mensagemCompleta.contains("417") ||
                                                           (mensagemCompleta.contains("Dependente") && mensagemCompleta.contains("existe")));
                    
                    if (isDependente && dependenteJaCadastradoMapping) {
                        log.warn("⚠️ DEPENDENTE JÁ CADASTRADO (erro na conversão) - {}: Não será registrado na TBSYNC", 
                                beneficiario.getCodigoMatricula());
                        continue; // Não registrar na TBSYNC
                    }
                    
                    // Registrar erro na TBSYNC passando também a view para ter dados completos
                    // Usar a causa real da exceção para preservar informações originais
                    try {
                        registrarErroNaTBSync(beneficiarioDomínio, beneficiario,
                                "Erro ao converter view para entidade: " + mensagemCompleta, 
                                causaReal instanceof Exception ? (Exception) causaReal : mappingException);
                    } catch (Exception erroTBSync) {
                        log.error("❌ ERRO ao registrar na TBSYNC durante conversão: {}", erroTBSync.getMessage());
                    }
                    // NÃO relançar a exceção - já foi registrada e vamos continuar com o próximo beneficiário
                    continue; // Pula para o próximo beneficiário
                }
                
            } catch (Exception e) {
                String identificacaoRawErro = beneficiario.getIdentificacao();
                String identificacaoNormalizadaErro = identificacaoRawErro != null ? identificacaoRawErro.trim().toUpperCase() : null;
                boolean isDependenteErro = "D".equals(identificacaoNormalizadaErro);
                boolean isTitularErro = "T".equals(identificacaoNormalizadaErro);
                
                String tipoErro = isTitularErro ? "TITULAR" : 
                                 isDependenteErro ? "DEPENDENTE" : 
                                 "DESCONHECIDO";
                log.error("❌ ERRO AO PROCESSAR INCLUSÃO - Beneficiário {} ({} - CPF: {}): {} - {}", 
                         beneficiario.getCodigoMatricula(), 
                         tipoErro,
                         beneficiario.getCpf(),
                         e.getMessage(), 
                         e.getClass().getSimpleName());
                log.error("❌ STACK TRACE DO ERRO:", e);
                
                // Log específico para dependentes com erro
                if (isDependenteErro) {
                    log.error("❌ ERRO NO PROCESSAMENTO DE DEPENDENTE - Matrícula: {} | CPF: {} | Erro: {}", 
                            beneficiario.getCodigoMatricula(), 
                            beneficiario.getCpf(),
                            e.getMessage());
                }
                
                // Verificar se é erro de "dependente já cadastrado" antes de registrar na TBSYNC
                String mensagemErroCompleta = e.getMessage() != null ? e.getMessage() : "";
                if (e.getCause() != null && e.getCause().getMessage() != null) {
                    mensagemErroCompleta += " " + e.getCause().getMessage();
                }
                
                boolean dependenteJaCadastrado = (mensagemErroCompleta.contains("existe para o titular") || 
                                                 mensagemErroCompleta.contains("417") ||
                                                 (mensagemErroCompleta.contains("Dependente") && mensagemErroCompleta.contains("existe")) ||
                                                 mensagemErroCompleta.contains("\"mensagem\":\"Dependente"));
                
                if (dependenteJaCadastrado && isDependenteErro) {
                    log.warn("⚠️ DEPENDENTE JÁ CADASTRADO DETECTADO NO CATCH EXTERNO - {}: Não será registrado na TBSYNC", 
                            beneficiario.getCodigoMatricula());
                    // NÃO registrar na TBSYNC - apenas logar e continuar
                } else {
                    // Garantir que o erro seja registrado na TBSYNC mesmo se não passou pelo processamento
                    try {
                        // Tentar criar beneficiário mínimo se ainda não foi criado
                        if (beneficiario != null) {
                            BeneficiarioOdontoprev beneficiarioParaErro = BeneficiarioOdontoprev.builder()
                                    .codigoMatricula(beneficiario.getCodigoMatricula())
                                    .codigoEmpresa(beneficiario.getCodigoEmpresa())
                                    .nomeBeneficiario(beneficiario.getNomeDoBeneficiario() != null ? 
                                            beneficiario.getNomeDoBeneficiario() : "N/A")
                                    .cpf(beneficiario.getCpf() != null ? beneficiario.getCpf() : "")
                                    .identificacao(beneficiario.getIdentificacao()) // IMPORTANTE: Preencher identificacao
                                    .codigoPlano(beneficiario.getCodigoPlano() != null ? String.valueOf(beneficiario.getCodigoPlano()) : null)
                                    .build();
                            
                            registrarErroNaTBSync(beneficiarioParaErro, beneficiario,
                                    "Erro durante processamento: " + e.getMessage(), e);
                        }
                    } catch (Exception erroRegistro) {
                        log.error("❌ ERRO CRÍTICO - Não foi possível registrar erro na TBSYNC para beneficiário {}: {}", 
                                 beneficiario.getCodigoMatricula(), erroRegistro.getMessage());
                    }
                }
                
                // SEMPRE continua processando outros beneficiários - não lança exceção aqui
                log.info("🔄 CONTINUANDO PROCESSAMENTO - Próximo beneficiário será processado");
            }
        }
        
        if (jaProcessados > 0) {
            log.info("📊 RESUMO DO LOTE - Processados: {}, Já processados (pulados): {}", processadosNoLote, jaProcessados);
        }
        
        return processadosNoLote;
    }
    
    /**
     * REGISTRA ERRO NA TBSYNC PARA BENEFICIÁRIO (versão sem view - mantém compatibilidade)
     */
    private void registrarErroNaTBSync(BeneficiarioOdontoprev beneficiario, String mensagemErro, Exception excecao) {
        registrarErroNaTBSync(beneficiario, null, mensagemErro, excecao);
    }
    
    /**
     * REGISTRA ERRO NA TBSYNC PARA BENEFICIÁRIO
     * 
     * Garante que todos os erros sejam registrados na tabela de controle,
     * mesmo quando ocorrem antes do processamento completo.
     * IMPORTANTE: Tenta criar o request completo para preencher dadosJson.
     * 
     * @param beneficiario Entidade de domínio (pode estar incompleta)
     * @param beneficiarioView View original com dados completos (pode ser null)
     * @param mensagemErro Mensagem de erro
     * @param excecao Exceção que causou o erro (pode ser null)
     */
    private void registrarErroNaTBSync(BeneficiarioOdontoprev beneficiario, 
                                       com.odontoPrev.odontoPrev.infrastructure.repository.entity.IntegracaoOdontoprevBeneficiario beneficiarioView,
                                       String mensagemErro, Exception excecao) {
        try {
            log.info("📝 [TBSYNC] Registrando erro na TBSYNC para beneficiário {}: {}", 
                    beneficiario.getCodigoMatricula(), mensagemErro);
            
            // Determinar endpoint e tentar criar payload completo
            String endpointDestino = "/cadastroonline-pj/1.0/incluir"; // Endpoint padrão para titular
            String payloadJson = "{}";
            
            try {
                // Verificar se é dependente para criar o request correto
                if ("D".equals(beneficiario.getIdentificacao())) {
                    endpointDestino = "/cadastroonline-pj/1.0/incluirDependente";
                    
                    // PRIORIDADE 1: Usar codigoAssociadoTitular diretamente da view (se disponível)
                    String codigoAssociadoTitular = null;
                    if (beneficiarioView != null && beneficiarioView.getCodigoAssociadoTitular() != null 
                            && !beneficiarioView.getCodigoAssociadoTitular().trim().isEmpty()) {
                        codigoAssociadoTitular = beneficiarioView.getCodigoAssociadoTitular().replaceAll("[^0-9]", "");
                        log.info("✅ [TBSYNC] Usando codigoAssociadoTitular da view: {}", codigoAssociadoTitular);
                    }
                    
                    // PRIORIDADE 2: Tentar usar codigoAssociadoTitularTemp do beneficiário de domínio
                    if ((codigoAssociadoTitular == null || codigoAssociadoTitular.trim().isEmpty()) 
                            && beneficiario.getCodigoAssociadoTitularTemp() != null 
                            && !beneficiario.getCodigoAssociadoTitularTemp().trim().isEmpty()) {
                        codigoAssociadoTitular = beneficiario.getCodigoAssociadoTitularTemp().replaceAll("[^0-9]", "");
                        log.info("✅ [TBSYNC] Usando codigoAssociadoTitularTemp do beneficiário: {}", codigoAssociadoTitular);
                    }
                    
                    // Se temos codigoAssociadoTitular, tentar criar payload completo via reflexão
                    if (codigoAssociadoTitular != null && !codigoAssociadoTitular.trim().isEmpty()) {
                        try {
                            if (processamentoBeneficiarioService != null) {
                                // Criar request de dependente usando reflexão
                                java.lang.reflect.Method metodoConverter = ProcessamentoBeneficiarioServiceImpl.class
                                        .getDeclaredMethod("converterParaDependenteRequest", 
                                                BeneficiarioOdontoprev.class, String.class);
                                metodoConverter.setAccessible(true);
                                Object request = metodoConverter.invoke(
                                        processamentoBeneficiarioService, beneficiario, codigoAssociadoTitular);
                                
                                // Serializar request para JSON
                                com.fasterxml.jackson.databind.ObjectMapper mapper = 
                                        new com.fasterxml.jackson.databind.ObjectMapper();
                                payloadJson = mapper.writeValueAsString(request);
                                log.info("✅ [TBSYNC] Payload de dependente criado com sucesso - {} caracteres", 
                                        payloadJson.length());
                            }
                        } catch (Exception refletException) {
                            log.warn("⚠️ [TBSYNC] Não foi possível criar payload de dependente via reflexão: {} - Usando fallback", 
                                    refletException.getMessage());
                        }
                    } else {
                        log.warn("⚠️ [TBSYNC] codigoAssociadoTitular não encontrado na view nem no beneficiário - Usando fallback");
                    }
                } else {
                    // Criar request de titular usando reflexão
                    try {
                        if (processamentoBeneficiarioService != null) {
                            java.lang.reflect.Method metodoConverter = ProcessamentoBeneficiarioServiceImpl.class
                                    .getDeclaredMethod("converterParaInclusaoRequestNew", 
                                            BeneficiarioOdontoprev.class);
                            metodoConverter.setAccessible(true);
                            Object request = metodoConverter.invoke(
                                    processamentoBeneficiarioService, beneficiario);
                            
                            com.fasterxml.jackson.databind.ObjectMapper mapper = 
                                    new com.fasterxml.jackson.databind.ObjectMapper();
                            payloadJson = mapper.writeValueAsString(request);
                            log.info("✅ [TBSYNC] Payload de titular criado com sucesso - {} caracteres", 
                                    payloadJson.length());
                        }
                    } catch (Exception refletException) {
                        log.warn("⚠️ [TBSYNC] Não foi possível criar payload de titular via reflexão: {}", 
                                refletException.getMessage());
                    }
                }
            } catch (Exception e) {
                log.warn("⚠️ [TBSYNC] Erro ao tentar criar payload completo para beneficiário {}: {}", 
                        beneficiario.getCodigoMatricula(), e.getMessage());
            }
            
            // Se ainda não conseguiu criar o payload e temos a view, criar um JSON básico
            if (("{}".equals(payloadJson) || payloadJson.trim().isEmpty()) && beneficiarioView != null) {
                try {
                    com.fasterxml.jackson.databind.ObjectMapper mapper = 
                            new com.fasterxml.jackson.databind.ObjectMapper();
                    com.fasterxml.jackson.databind.node.ObjectNode payloadBasico = mapper.createObjectNode();
                    
                    // Adicionar dados básicos baseados na view
                    if ("D".equals(beneficiarioView.getIdentificacao())) {
                        // Payload básico para dependente
                        endpointDestino = "/cadastroonline-pj/1.0/incluirDependente";
                        
                        // Preparar codigoAssociadoTitular como String
                        String codigoAssociadoTitularStr = "";
                        if (beneficiarioView.getCodigoAssociadoTitular() != null && !beneficiarioView.getCodigoAssociadoTitular().trim().isEmpty()) {
                            codigoAssociadoTitularStr = beneficiarioView.getCodigoAssociadoTitular().replaceAll("[^0-9]", "");
                        }
                        payloadBasico.put("codigoAssociadoTitular", codigoAssociadoTitularStr);
                        
                        // Usar usuario da view se disponível (como String)
                        String usuarioStr = "";
                        if (beneficiarioView.getUsuario() != null) {
                            usuarioStr = String.valueOf(beneficiarioView.getUsuario());
                        }
                        payloadBasico.put("usuario", usuarioStr);
                        payloadBasico.put("cdUsuario", usuarioStr);
                        
                        com.fasterxml.jackson.databind.node.ObjectNode beneficiarioNode = mapper.createObjectNode();
                        com.fasterxml.jackson.databind.node.ObjectNode beneficiarioData = mapper.createObjectNode();
                        
                        // Campos básicos do beneficiário
                        beneficiarioData.put("codigoMatricula", beneficiarioView.getCodigoMatricula() != null ? beneficiarioView.getCodigoMatricula() : "");
                        if (beneficiarioView.getCodigoPlano() != null) {
                            beneficiarioData.put("codigoPlano", String.valueOf(beneficiarioView.getCodigoPlano()));
                        }
                        beneficiarioData.put("cpf", beneficiarioView.getCpf() != null ? beneficiarioView.getCpf() : "");
                        beneficiarioData.put("nomeBeneficiario", beneficiarioView.getNomeDoBeneficiario() != null ? beneficiarioView.getNomeDoBeneficiario() : "");
                        beneficiarioData.put("identificacao", beneficiarioView.getIdentificacao() != null ? beneficiarioView.getIdentificacao() : "");
                        
                        // Adicionar beneficiarioTitular se disponível (como String)
                        if (codigoAssociadoTitularStr != null && !codigoAssociadoTitularStr.isEmpty()) {
                            beneficiarioData.put("beneficiarioTitular", codigoAssociadoTitularStr);
                        }
                        
                        // Adicionar dataDeNascimento da view (formato DD/MM/YYYY)
                        if (beneficiarioView.getDataDeNascimento() != null && !beneficiarioView.getDataDeNascimento().trim().isEmpty()) {
                            beneficiarioData.put("dataDeNascimento", beneficiarioView.getDataDeNascimento());
                        }
                        
                        // Adicionar dtVigenciaRetroativa da view (formato DD/MM/YYYY)
                        if (beneficiarioView.getDtVigenciaRetroativa() != null && !beneficiarioView.getDtVigenciaRetroativa().trim().isEmpty()) {
                            beneficiarioData.put("dtVigenciaRetroativa", beneficiarioView.getDtVigenciaRetroativa());
                        }
                        
                        // Adicionar nomeDaMae da view
                        if (beneficiarioView.getNomeDaMae() != null && !beneficiarioView.getNomeDaMae().trim().isEmpty()) {
                            beneficiarioData.put("nomeDaMae", beneficiarioView.getNomeDaMae());
                        }
                        
                        // Adicionar sexo da view
                        if (beneficiarioView.getSexo() != null && !beneficiarioView.getSexo().trim().isEmpty()) {
                            beneficiarioData.put("sexo", beneficiarioView.getSexo());
                        }
                        
                        // Adicionar telefoneCelular da view
                        if (beneficiarioView.getTelefoneCelular() != null && !beneficiarioView.getTelefoneCelular().trim().isEmpty()) {
                            beneficiarioData.put("telefoneCelular", beneficiarioView.getTelefoneCelular());
                        } else {
                            beneficiarioData.put("telefoneCelular", (String) null);
                        }
                        
                        // Adicionar telefoneResidencial da view
                        if (beneficiarioView.getTelefoneResidencial() != null && !beneficiarioView.getTelefoneResidencial().trim().isEmpty()) {
                            beneficiarioData.put("telefoneResidencial", beneficiarioView.getTelefoneResidencial());
                        } else {
                            beneficiarioData.put("telefoneResidencial", (String) null);
                        }
                        
                        // Adicionar estadoCivil da view
                        if (beneficiarioView.getEstadoCivil() != null && !beneficiarioView.getEstadoCivil().trim().isEmpty()) {
                            beneficiarioData.put("estadoCivil", beneficiarioView.getEstadoCivil());
                        }
                        
                        // Adicionar nmCargo da view
                        if (beneficiarioView.getNmCargo() != null && !beneficiarioView.getNmCargo().trim().isEmpty()) {
                            beneficiarioData.put("nmCargo", beneficiarioView.getNmCargo());
                        }
                        
                        // Adicionar rg da view
                        if (beneficiarioView.getRg() != null && !beneficiarioView.getRg().trim().isEmpty()) {
                            beneficiarioData.put("rg", beneficiarioView.getRg());
                        } else {
                            beneficiarioData.put("rg", (String) null);
                        }
                        
                        // Adicionar rgEmissor da view
                        if (beneficiarioView.getRgEmissor() != null && !beneficiarioView.getRgEmissor().trim().isEmpty()) {
                            beneficiarioData.put("rgEmissor", beneficiarioView.getRgEmissor());
                        } else {
                            beneficiarioData.put("rgEmissor", (String) null);
                        }
                        
                        // Campos opcionais que devem ser null (igual ao payload de sucesso)
                        beneficiarioData.put("campanha", (String) null);
                        beneficiarioData.put("email", (String) null);
                        beneficiarioData.put("empresaNova", (String) null);
                        beneficiarioData.put("grauParentesco", (String) null);
                        beneficiarioData.put("motivoExclusao", (String) null);
                        beneficiarioData.put("pisPasep", (String) null);
                        beneficiarioData.put("telefoneComercial", (String) null);
                        
                        // Adicionar endereco completo da view
                        if (beneficiarioView.getLogradouro() != null || beneficiarioView.getCep() != null || 
                            beneficiarioView.getBairro() != null || beneficiarioView.getCidade() != null || 
                            beneficiarioView.getUf() != null || beneficiarioView.getNumero() != null) {
                            
                            com.fasterxml.jackson.databind.node.ObjectNode enderecoNode = mapper.createObjectNode();
                            
                            if (beneficiarioView.getBairro() != null) {
                                enderecoNode.put("bairro", beneficiarioView.getBairro());
                            }
                            if (beneficiarioView.getCep() != null) {
                                enderecoNode.put("cep", beneficiarioView.getCep());
                            }
                            if (beneficiarioView.getCidade() != null) {
                                enderecoNode.put("cidade", beneficiarioView.getCidade());
                            }
                            if (beneficiarioView.getLogradouro() != null) {
                                enderecoNode.put("logradouro", beneficiarioView.getLogradouro());
                            }
                            if (beneficiarioView.getNumero() != null) {
                                enderecoNode.put("numero", beneficiarioView.getNumero());
                            }
                            if (beneficiarioView.getUf() != null) {
                                enderecoNode.put("uf", beneficiarioView.getUf());
                            }
                            if (beneficiarioView.getComplemento() != null) {
                                enderecoNode.put("complemento", beneficiarioView.getComplemento());
                            }
                            if (beneficiarioView.getTpEndereco() != null) {
                                enderecoNode.put("tpEndereco", beneficiarioView.getTpEndereco());
                            }
                            enderecoNode.put("cidadeBeneficiario", (String) null);
                            
                            beneficiarioData.set("endereco", enderecoNode);
                        }
                        
                        beneficiarioNode.set("beneficiario", beneficiarioData);
                        if (beneficiarioView.getCodigoEmpresa() != null) {
                            String codigoEmpresaStr = beneficiarioView.getCodigoEmpresa().replaceAll("[^0-9]", "");
                            beneficiarioNode.put("codigoEmpresa", codigoEmpresaStr);
                        }
                        // Adicionar departamento da view (igual ao payload de sucesso)
                        if (beneficiarioView.getDepartamento() != null) {
                            beneficiarioNode.put("departamento", String.valueOf(beneficiarioView.getDepartamento()));
                        }
                        // Adicionar parentesco se disponível (como número Integer)
                        if (beneficiarioView.getParentesco() != null) {
                            beneficiarioNode.put("parentesco", beneficiarioView.getParentesco().intValue());
                        } else {
                            beneficiarioNode.put("parentesco", 0); // Valor padrão como número
                        }
                        
                        com.fasterxml.jackson.databind.node.ArrayNode beneficiariosArray = mapper.createArrayNode();
                        beneficiariosArray.add(beneficiarioNode);
                        payloadBasico.set("beneficiarios", beneficiariosArray);
                    } else {
                        // Payload completo para titular - incluir TODOS os campos da view
                        endpointDestino = "/cadastroonline-pj/1.0/incluir";
                        com.fasterxml.jackson.databind.node.ObjectNode beneficiarioTitular = mapper.createObjectNode();
                        com.fasterxml.jackson.databind.node.ObjectNode beneficiarioData = mapper.createObjectNode();
                        
                        // Campos básicos do beneficiário
                        beneficiarioData.put("codigoMatricula", beneficiarioView.getCodigoMatricula() != null ? beneficiarioView.getCodigoMatricula() : "");
                        if (beneficiarioView.getCodigoPlano() != null) {
                            beneficiarioData.put("codigoPlano", String.valueOf(beneficiarioView.getCodigoPlano()));
                        }
                        beneficiarioData.put("cpf", beneficiarioView.getCpf() != null ? beneficiarioView.getCpf() : "");
                        beneficiarioData.put("nomeBeneficiario", beneficiarioView.getNomeDoBeneficiario() != null ? beneficiarioView.getNomeDoBeneficiario() : "");
                        beneficiarioData.put("identificacao", beneficiarioView.getIdentificacao() != null ? beneficiarioView.getIdentificacao() : "T");
                        
                        // Adicionar dataDeNascimento da view (formato DD/MM/YYYY)
                        if (beneficiarioView.getDataDeNascimento() != null && !beneficiarioView.getDataDeNascimento().trim().isEmpty()) {
                            beneficiarioData.put("dataDeNascimento", beneficiarioView.getDataDeNascimento());
                        }
                        
                        // Adicionar dtVigenciaRetroativa da view (formato DD/MM/YYYY)
                        if (beneficiarioView.getDtVigenciaRetroativa() != null && !beneficiarioView.getDtVigenciaRetroativa().trim().isEmpty()) {
                            beneficiarioData.put("dtVigenciaRetroativa", beneficiarioView.getDtVigenciaRetroativa());
                        }
                        
                        // Adicionar nomeDaMae da view
                        if (beneficiarioView.getNomeDaMae() != null && !beneficiarioView.getNomeDaMae().trim().isEmpty()) {
                            beneficiarioData.put("nomeDaMae", beneficiarioView.getNomeDaMae());
                        }
                        
                        // Adicionar sexo da view
                        if (beneficiarioView.getSexo() != null && !beneficiarioView.getSexo().trim().isEmpty()) {
                            beneficiarioData.put("sexo", beneficiarioView.getSexo());
                        }
                        
                        // Adicionar telefoneCelular da view
                        if (beneficiarioView.getTelefoneCelular() != null && !beneficiarioView.getTelefoneCelular().trim().isEmpty()) {
                            beneficiarioData.put("telefoneCelular", beneficiarioView.getTelefoneCelular());
                        }
                        
                        // Adicionar telefoneResidencial da view
                        if (beneficiarioView.getTelefoneResidencial() != null && !beneficiarioView.getTelefoneResidencial().trim().isEmpty()) {
                            beneficiarioData.put("telefoneResidencial", beneficiarioView.getTelefoneResidencial());
                        }
                        
                        // Adicionar estadoCivil da view
                        if (beneficiarioView.getEstadoCivil() != null && !beneficiarioView.getEstadoCivil().trim().isEmpty()) {
                            beneficiarioData.put("estadoCivil", beneficiarioView.getEstadoCivil());
                        }
                        
                        // Adicionar nmCargo da view
                        if (beneficiarioView.getNmCargo() != null && !beneficiarioView.getNmCargo().trim().isEmpty()) {
                            beneficiarioData.put("nmCargo", beneficiarioView.getNmCargo());
                        }
                        
                        // Adicionar cns da view
                        if (beneficiarioView.getCns() != null && !beneficiarioView.getCns().trim().isEmpty()) {
                            beneficiarioData.put("cns", beneficiarioView.getCns());
                        }
                        
                        // Adicionar rg da view
                        if (beneficiarioView.getRg() != null && !beneficiarioView.getRg().trim().isEmpty()) {
                            beneficiarioData.put("rg", beneficiarioView.getRg());
                        }
                        
                        // Adicionar rgEmissor da view
                        if (beneficiarioView.getRgEmissor() != null && !beneficiarioView.getRgEmissor().trim().isEmpty()) {
                            beneficiarioData.put("rgEmissor", beneficiarioView.getRgEmissor());
                        }
                        
                        // Adicionar endereco completo da view
                        if (beneficiarioView.getLogradouro() != null || beneficiarioView.getCep() != null || 
                            beneficiarioView.getBairro() != null || beneficiarioView.getCidade() != null || 
                            beneficiarioView.getUf() != null || beneficiarioView.getNumero() != null) {
                            
                            com.fasterxml.jackson.databind.node.ObjectNode enderecoNode = mapper.createObjectNode();
                            
                            if (beneficiarioView.getCep() != null) {
                                enderecoNode.put("cep", beneficiarioView.getCep());
                            }
                            if (beneficiarioView.getCidade() != null) {
                                enderecoNode.put("cidade", beneficiarioView.getCidade());
                            }
                            if (beneficiarioView.getLogradouro() != null) {
                                enderecoNode.put("logradouro", beneficiarioView.getLogradouro());
                            }
                            if (beneficiarioView.getNumero() != null) {
                                enderecoNode.put("numero", beneficiarioView.getNumero());
                            }
                            if (beneficiarioView.getUf() != null) {
                                enderecoNode.put("uf", beneficiarioView.getUf());
                            }
                            if (beneficiarioView.getBairro() != null) {
                                enderecoNode.put("bairro", beneficiarioView.getBairro());
                            }
                            if (beneficiarioView.getComplemento() != null) {
                                enderecoNode.put("complemento", beneficiarioView.getComplemento());
                            }
                            if (beneficiarioView.getTpEndereco() != null) {
                                enderecoNode.put("tpEndereco", beneficiarioView.getTpEndereco());
                            }
                            enderecoNode.put("cidadeBeneficiario", (String) null);
                            
                            beneficiarioData.set("endereco", enderecoNode);
                        }
                        
                        // Campos opcionais que devem ser null no payload
                        beneficiarioData.put("beneficiarioTitular", (String) null);
                        beneficiarioData.put("campanha", (String) null);
                        beneficiarioData.put("email", (String) null);
                        beneficiarioData.put("empresaNova", (String) null);
                        beneficiarioData.put("grauParentesco", (String) null);
                        beneficiarioData.put("motivoExclusao", (String) null);
                        beneficiarioData.put("pisPasep", (String) null);
                        beneficiarioData.put("telefoneComercial", (String) null);
                        
                        // Adicionar departamento na raiz do beneficiarioData se disponível
                        if (beneficiarioView.getDepartamento() != null) {
                            beneficiarioData.put("departamento", String.valueOf(beneficiarioView.getDepartamento()));
                        }
                        
                        beneficiarioTitular.set("beneficiario", beneficiarioData);
                        payloadBasico.set("beneficiarioTitular", beneficiarioTitular);
                        
                        // Usar usuario da view se disponível (como String)
                        String usuarioStrTitular = "";
                        if (beneficiarioView.getUsuario() != null) {
                            usuarioStrTitular = String.valueOf(beneficiarioView.getUsuario());
                        }
                        payloadBasico.put("usuario", usuarioStrTitular);
                        
                        // Criar venda completa com todos os campos da view
                        com.fasterxml.jackson.databind.node.ObjectNode venda = mapper.createObjectNode();
                        if (beneficiarioView.getCodigoEmpresa() != null) {
                            String codigoEmpresaStrTitular = beneficiarioView.getCodigoEmpresa().replaceAll("[^0-9]", "");
                            venda.put("codigoEmpresa", codigoEmpresaStrTitular);
                        }
                        if (beneficiarioView.getCodigoPlano() != null) {
                            venda.put("codigoPlano", String.valueOf(beneficiarioView.getCodigoPlano()));
                        }
                        if (beneficiarioView.getDepartamento() != null) {
                            venda.put("departamento", String.valueOf(beneficiarioView.getDepartamento()));
                        }
                        venda.put("enviarKit", (String) null);
                        venda.put("segmentacao", (String) null);
                        venda.put("subsegmentacao", (String) null);
                        
                        payloadBasico.set("venda", venda);
                        
                        // Campos opcionais do request
                        payloadBasico.put("dadosBancarios", (String) null);
                        payloadBasico.put("protocolo", (String) null);
                    }
                    
                    // IMPORTANTE: Usar writeValueAsString sem pretty print para manter consistência
                    // com o formato gerado via reflexão (sem quebras de linha)
                    payloadJson = mapper.writeValueAsString(payloadBasico);
                    log.info("✅ [TBSYNC] Payload básico criado a partir da view - {} caracteres", payloadJson.length());
                } catch (Exception payloadException) {
                    log.warn("⚠️ [TBSYNC] Não foi possível criar payload básico da view: {}", payloadException.getMessage());
                    // Mantém "{}" como último recurso
                }
            }
            
            // Limitar tamanho do payloadJson se muito grande (pode causar problemas no CLOB)
            if (payloadJson != null && payloadJson.length() > 100000) { // Limitar a ~100KB
                log.warn("⚠️ [TBSYNC] Payload muito grande ({} caracteres), truncando para 100KB", payloadJson.length());
                payloadJson = payloadJson.substring(0, 100000);
            }
            
            // Limitar tamanho da mensagem de erro (máximo 4000 caracteres para CLOB)
            String erroMensagemFinal = mensagemErro + (excecao != null ? " | Exceção: " + excecao.getClass().getSimpleName() : "");
            if (erroMensagemFinal.length() > 4000) {
                erroMensagemFinal = erroMensagemFinal.substring(0, 4000);
            }
            
            // Garantir que todos os campos obrigatórios estejam preenchidos
            String codigoEmpresa = beneficiario.getCodigoEmpresa() != null ? beneficiario.getCodigoEmpresa() : "";
            String codigoMatricula = beneficiario.getCodigoMatricula() != null ? beneficiario.getCodigoMatricula() : "";
            String endpointFinal = endpointDestino != null ? endpointDestino : "";
            String payloadFinal = payloadJson != null ? payloadJson : "{}";
            
            ControleSyncBeneficiario controle = ControleSyncBeneficiario.builder()
                    .codigoEmpresa(codigoEmpresa)
                    .codigoBeneficiario(codigoMatricula)
                    .tipoLog("I") // I = Inclusão
                    .tipoOperacao("INCLUSAO")
                    .endpointDestino(endpointFinal)
                    .dadosJson(payloadFinal)
                    .statusSync("ERRO") // Status de erro (consistente com ProcessamentoBeneficiarioServiceImpl)
                    .erroMensagem(erroMensagemFinal)
                    .tentativas(1)
                    .maxTentativas(3)
                    .dataUltimaTentativa(LocalDateTime.now())
                    .build();
            
            ControleSyncBeneficiario controleSalvo = controleSyncRepository.save(controle);
            log.info("✅ [TBSYNC] Erro registrado na TBSYNC com ID: {} para beneficiário {} | Endpoint: {} | DadosJson: {} caracteres", 
                    controleSalvo.getId(), codigoMatricula, endpointFinal, payloadFinal.length());
        } catch (Exception e) {
            // Log detalhado do erro mas não relançar para não parar o processamento
            log.error("❌ [TBSYNC] Erro crítico ao registrar erro na TBSYNC para beneficiário {}: {} | Causa: {}", 
                     beneficiario != null && beneficiario.getCodigoMatricula() != null ? beneficiario.getCodigoMatricula() : "NULL", 
                     e.getMessage(),
                     e.getCause() != null ? e.getCause().getMessage() : "N/A");
            if (e.getStackTrace() != null && e.getStackTrace().length > 0) {
                log.error("❌ [TBSYNC] Stack trace: {}", e.getStackTrace()[0].toString());
            }
        }
    }
    
    /**
     * VERIFICA SE BENEFICIÁRIO JÁ FOI PROCESSADO COM SUCESSO POR CPF
     * 
     * IMPORTANTE: Usa CPF para verificação pois dependentes podem ter 
     * a mesma matrícula do titular. Cada pessoa tem CPF único.
     * 
     * VERIFICAÇÃO: Busca na VIEW VW_INTEGRACAO_ODONTOPREV_BENEFICIARIOS e
     * na TBSYNC (TB_CONTROLE_SYNC_ODONTOPREV_BENEF) para verificar se já foi processado.
     * 
     * @param codigoEmpresa código da empresa
     * @param cpf CPF do beneficiário (sem formatação)
     * @param tipoOperacao tipo da operação (INCLUSAO, ALTERACAO, EXCLUSAO)
     * @return true se já foi processado com sucesso, false caso contrário
     */
    private boolean jaFoiProcessadoComSucessoPorCpf(String codigoEmpresa, String cpf, String tipoOperacao) {
        try {
            if (cpf == null || cpf.trim().isEmpty()) {
                log.warn("⚠️ CPF vazio ou nulo, não é possível verificar - processando normalmente");
                return false;
            }
            
            // Limpar CPF (remover pontos, traços e espaços)
            String cpfLimpo = cpf.replaceAll("[^0-9]", "");
            
            // PASSO 1: Buscar beneficiário na VIEW VW_INTEGRACAO_ODONTOPREV_BENEFICIARIOS para obter a matrícula
            // IMPORTANTE: Buscar sempre da VIEW, nunca da tabela TB_BENEFICIARIO_ODONTOPREV
            com.odontoPrev.odontoPrev.infrastructure.repository.entity.IntegracaoOdontoprevBeneficiario beneficiarioView = inclusaoRepository.findByCpf(cpfLimpo);
            
            if (beneficiarioView == null) {
                log.debug("🆕 BENEFICIÁRIO NOVO NA VIEW - CPF: {} não encontrado na view, será processado", cpfLimpo);
                return false;
            }
            
            String codigoMatricula = beneficiarioView.getCodigoMatricula();
            log.debug("🔍 BENEFICIÁRIO ENCONTRADO NA VIEW - CPF: {} | Matrícula: {} | Empresa: {}", 
                    cpfLimpo, codigoMatricula, beneficiarioView.getCodigoEmpresa());
            
            // PASSO 2: Verificar na TBSYNC se já foi processado com sucesso
            // IMPORTANTE: Buscar TODOS os registros para esta matrícula (pode haver múltiplos)
            // Usar códigoEmpresa da view para garantir consistência
            String codigoEmpresaView = beneficiarioView.getCodigoEmpresa();
            String empresaParaBusca = codigoEmpresaView != null ? codigoEmpresaView : codigoEmpresa;
            
            // Buscar TODOS os registros para esta empresa e matrícula
            var todosControles = controleSyncRepository.findByCodigoEmpresaAndCodigoBeneficiario(
                    empresaParaBusca, codigoMatricula);
            
            // Se encontrou algum registro, verificar se algum tem sucesso
            if (todosControles != null && !todosControles.isEmpty()) {
                log.debug("🔍 ENCONTRADOS {} REGISTROS NA TBSYNC - CPF: {} | Matrícula: {} | Empresa: {}", 
                        todosControles.size(), cpfLimpo, codigoMatricula, empresaParaBusca);
                
                // Verificar se ALGUM dos registros tem status de sucesso
                for (ControleSyncBeneficiario controle : todosControles) {
                    String statusSync = controle.getStatusSync();
                    String erroMensagem = controle.getErroMensagem();
                    String responseApi = controle.getResponseApi();
                    String tipoOp = controle.getTipoOperacao();
                    
                    // Só considerar se for do mesmo tipo de operação
                    if (!tipoOperacao.equals(tipoOp)) {
                        continue; // Pula registros de outras operações
                    }
                    
                    boolean isSucesso = "SUCESSO".equals(statusSync) || 
                                       "SUCCESS".equalsIgnoreCase(statusSync);
                    
                    // Verificar se é erro de "já cadastrado" (também é considerado sucesso)
                    boolean jaCadastrado = false;
                    if (erroMensagem != null) {
                        jaCadastrado = (erroMensagem.contains("já cadastrado") || 
                                       erroMensagem.contains("existe para o titular") ||
                                       erroMensagem.contains("417") ||
                                       erroMensagem.contains("Beneficiário já cadastrado") ||
                                       (erroMensagem.contains("Dependente") && erroMensagem.contains("existe")));
                    }
                    if (!jaCadastrado && responseApi != null) {
                        jaCadastrado = ((responseApi.contains("\"mensagem\":\"Dependente") && responseApi.contains("existe")) ||
                                       responseApi.contains("\"status\":417") ||
                                       responseApi.contains("já cadastrado"));
                    }
                    
                    if (isSucesso || jaCadastrado) {
                        log.info("✅ BENEFICIÁRIO JÁ PROCESSADO COM SUCESSO - CPF: {} | Matrícula: {} | Status: {} | Data Sucesso: {} | JaCadastrado: {} | ID: {}", 
                                cpfLimpo, codigoMatricula, statusSync, controle.getDataSucesso(), jaCadastrado, controle.getId());
                        return true; // Já foi processado com sucesso
                    }
                }
                
                // Se chegou aqui, nenhum registro tinha sucesso
                log.info("🔄 BENEFICIÁRIO ENCONTRADO NA TBSYNC MAS SEM SUCESSO - CPF: {} | Matrícula: {} | Total registros: {} - Será processado", 
                        cpfLimpo, codigoMatricula, todosControles.size());
            }
            
            // Se não encontrou na TBSYNC, o beneficiário ainda não foi processado
            log.info("🆕 BENEFICIÁRIO NOVO - CPF: {} não encontrado na TBSYNC, será processado", cpfLimpo);
            return false;
        } catch (Exception e) {
            log.warn("⚠️ ERRO ao verificar se beneficiário (CPF: {}) já foi processado: {}", 
                    cpf, e.getMessage());
            return false; // Em caso de erro, processa para não perder dados
        }
    }
    
    /**
     * VERIFICA SE BENEFICIÁRIO JÁ FOI PROCESSADO COM SUCESSO
     * 
     * @param codigoEmpresa código da empresa
     * @param codigoBeneficiario código do beneficiário (matrícula)
     * @param tipoOperacao tipo da operação (INCLUSAO, ALTERACAO, EXCLUSAO)
     * @return true se já foi processado com sucesso, false caso contrário
     */
    private boolean jaFoiProcessadoComSucesso(String codigoEmpresa, String codigoBeneficiario, String tipoOperacao) {
        try {
            var controle = controleSyncRepository.findByCodigoEmpresaAndCodigoBeneficiarioAndTipoOperacao(
                    codigoEmpresa, codigoBeneficiario, tipoOperacao);
            
            if (controle.isPresent()) {
                String status = controle.get().getStatusSync();
                boolean jaProcessado = "SUCESSO".equals(status) || "SUCCESS".equals(status);
                
                if (jaProcessado) {
                    log.debug("✅ BENEFICIÁRIO JÁ PROCESSADO - {}: status={}, dataSucesso={}", 
                            codigoBeneficiario, status, controle.get().getDataSucesso());
                }
                
                return jaProcessado;
            }
            
            return false;
        } catch (Exception e) {
            log.warn("⚠️ ERRO ao verificar se beneficiário {} já foi processado: {}", 
                    codigoBeneficiario, e.getMessage());
            return false; // Em caso de erro, processa para não perder dados
        }
    }
}
