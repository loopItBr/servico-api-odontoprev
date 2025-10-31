package com.odontoPrev.odontoPrev.infrastructure.client.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.odontoPrev.odontoPrev.domain.service.ConsultaEmpresaOdontoprevExpandidaService;
import com.odontoPrev.odontoPrev.domain.service.GerenciadorControleSyncService;
import com.odontoPrev.odontoPrev.domain.service.ProcessamentoEmpresaAlteracaoService;
import com.odontoPrev.odontoPrev.infrastructure.client.adapter.mapper.EmpresaAlteracaoMapper;
import com.odontoPrev.odontoPrev.infrastructure.client.adapter.out.dto.EmpresaAlteracaoRequest;
import com.odontoPrev.odontoPrev.infrastructure.repository.IntegracaoOdontoprevAlteracaoRepository;
import com.odontoPrev.odontoPrev.infrastructure.repository.entity.ControleSync;
import com.odontoPrev.odontoPrev.infrastructure.repository.entity.IntegracaoOdontoprev;
import com.odontoPrev.odontoPrev.infrastructure.repository.entity.IntegracaoOdontoprevAlteracao;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * SERVIÇO PARA PROCESSAMENTO DE EMPRESAS ALTERADAS
 * 
 * FUNÇÃO PRINCIPAL:
 * Esta classe é responsável por processar empresas que tiveram dados modificados
 * e precisam ser atualizadas na OdontoPrev.
 * 
 * FLUXO COMPLETO DE PROCESSAMENTO:
 * 1. BUSCAR dados completos da empresa alterada no banco local
 * 2. CRIAR registro de controle para auditoria (tipo_controle = 2)
 * 3. CHAMAR API da OdontoPrev para atualizar dados da empresa
 * 4. SALVAR resposta e resultado no controle de sincronização
 * 
 * CRITÉRIO DE SELEÇÃO:
 * - Empresas com DT_ALTERACAO = SYSDATE (alteradas hoje)
 * - Identifica empresas que precisam ter dados atualizados na OdontoPrev
 * 
 * TRATAMENTO DE ERROS:
 * Se qualquer passo der erro, salva o erro no controle e continua.
 * Outras empresas não são afetadas por erro de uma empresa específica.
 * 
 * AUDITORIA E CONTROLE:
 * Cada processamento gera um registro na tabela de controle com:
 * - Dados enviados para OdontoPrev
 * - Resposta recebida (sucesso/erro)
 * - Timestamp e tempo de processamento
 * - Status final (sucesso/erro)
 * - tipo_controle = 2 (Alteração)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ProcessamentoEmpresaAlteracaoServiceImpl implements ProcessamentoEmpresaAlteracaoService {

    // Repositório para buscar dados de empresas alteradas
    private final IntegracaoOdontoprevAlteracaoRepository alteracaoRepository;
    
    // Serviço para gerenciar registros de controle e auditoria
    private final GerenciadorControleSyncService gerenciadorControleSync;
    
    // Serviço para chamar API da OdontoPrev (expandido para suportar alterações)
    private final ConsultaEmpresaOdontoprevExpandidaService consultaEmpresaService;
    
    // Mapper para converter dados da view para o request da API
    private final EmpresaAlteracaoMapper empresaAlteracaoMapper;
    
    // ObjectMapper para serializar dados para JSON
    private final ObjectMapper objectMapper;

    /**
     * MÉTODO PRINCIPAL - PROCESSA UMA EMPRESA ALTERADA INDIVIDUAL
     * 
     * Este é o ponto de entrada para processamento de uma única empresa alterada.
     * Executa todo o fluxo desde busca de dados até salvamento do resultado.
     * 
     * FLUXO DETALHADO:
     * 1. Busca dados completos da empresa alterada no banco local
     * 2. Se não encontrou dados, registra warning e termina
     * 3. Se encontrou, cria registro de controle de sincronização (tipo_controle = 2)
     * 4. Chama API da OdontoPrev para atualizar dados da empresa
     * 5. Processa resposta (sucesso ou erro) e salva no controle
     * 
     * TRATAMENTO DE ERROS:
     * Qualquer erro é capturado, registrado no controle, e não interrompe
     * o processamento de outras empresas. Sistema é resiliente.
     * 
     * PARÂMETRO:
     * - codigoEmpresa: código único da empresa a ser processada (ex: "A001")
     */
    @Override
    public void processar(String codigoEmpresa) {
        log.debug("Processando empresa alterada: {}", codigoEmpresa);

        // PASSO 1: Busca dados completos da empresa alterada no banco
        IntegracaoOdontoprevAlteracao dadosCompletos = buscarDadosEmpresaAlteradaOuSair(codigoEmpresa);
        if (dadosCompletos == null) {
            return; // Se não encontrou dados, para aqui
        }
        
        // PASSO 2: Cria registro de controle para auditoria (tipo_controle = 2)
        ControleSync controleSync = criarEMSalvarControleSyncAlteracao(codigoEmpresa, dadosCompletos);
        
        // PASSO 3: Chama API da OdontoPrev e processa resultado
        buscarEProcessarResposta(controleSync, codigoEmpresa);
    }

    /**
     * BUSCA DADOS COMPLETOS DA EMPRESA ALTERADA OU TERMINA PROCESSAMENTO
     * 
     * Este método consulta o banco para obter todos os dados necessários
     * da empresa alterada (planos, contratos, valores, etc.) que serão enviados
     * para a OdontoPrev.
     * 
     * ESTRATÉGIA:
     * - Busca apenas o primeiro registro da empresa (método do repository)
     * - Se não encontrou, registra warning e retorna null
     * - Se encontrou, retorna objeto completo com todos os dados
     * 
     * TRATAMENTO DE DADOS AUSENTES:
     * É normal algumas empresas não terem dados completos ainda.
     * Não é erro, apenas significa que não estão prontas para sincronização.
     */
    private IntegracaoOdontoprevAlteracao buscarDadosEmpresaAlteradaOuSair(String codigoEmpresa) {
        try {
            Optional<IntegracaoOdontoprevAlteracao> dadosOpt = alteracaoRepository.buscarPrimeiroDadoPorCodigoEmpresa(codigoEmpresa);
            
            if (dadosOpt.isEmpty()) {
                log.warn("Nenhum dado encontrado para empresa alterada: {}", codigoEmpresa);
                return null;
            }
            
            IntegracaoOdontoprevAlteracao dados = dadosOpt.get();
            log.debug("Dados encontrados para empresa alterada {}: {}", codigoEmpresa, dados.getNomeFantasia());
            return dados;
            
        } catch (Exception e) {
            log.error("Erro ao buscar dados da empresa alterada {}: {}", codigoEmpresa, e.getMessage());
            return null;
        }
    }

    /**
     * CRIA E SALVA REGISTRO DE CONTROLE DE SINCRONIZAÇÃO PARA ALTERAÇÃO
     * 
     * Este método cria um registro na tabela de controle que será usado para:
     * 1. Auditoria do que foi enviado para OdontoPrev
     * 2. Rastreamento de sucesso/erro
     * 3. Reprocessamento em caso de falha
     * 
     * CARACTERÍSTICAS ESPECÍFICAS PARA ALTERAÇÃO:
     * - tipo_operacao = UPDATE
     * - tipo_controle = 2 (Alteração)
     * - endpoint_destino = "/empresas/{codigo}/atualizar"
     */
    private ControleSync criarEMSalvarControleSyncAlteracao(String codigoEmpresa, IntegracaoOdontoprevAlteracao dados) {
        try {
            // Converte dados para o tipo base para compatibilidade
            IntegracaoOdontoprev dadosBase = converterParaIntegracaoBase(dados);
            
            ControleSync controle = gerenciadorControleSync.criarControle(
                codigoEmpresa, 
                dadosBase, 
                ControleSync.TipoOperacao.UPDATE,
                ControleSync.TipoControle.ALTERACAO
            );
            
            ControleSync salvo = gerenciadorControleSync.salvar(controle);
            log.debug("Controle de alteração criado para empresa {} com ID: {}", codigoEmpresa, salvo.getId());
            return salvo;
            
        } catch (Exception e) {
            log.error("Erro ao criar controle de alteração para empresa {}: {}", codigoEmpresa, e.getMessage());
            throw new RuntimeException("Falha na criação do controle de alteração", e);
        }
    }

    /**
     * BUSCA E PROCESSA RESPOSTA DA API DA ODONTOPREV
     * 
     * Este método chama a API da OdontoPrev para atualizar os dados da empresa
     * e processa a resposta (sucesso ou erro).
     * 
     * FLUXO:
     * 1. Busca dados da view de alteração
     * 2. Cria request completo para a API
     * 3. Atualiza dadosJson na TBSYNC com o request completo
     * 4. Chama API da OdontoPrev
     * 5. Mede tempo de resposta
     * 6. Se sucesso: salva resposta no controle
     * 7. Se erro: salva mensagem de erro no controle
     */
    private void buscarEProcessarResposta(ControleSync controleSync, String codigoEmpresa) {
        long inicioTempo = System.currentTimeMillis();
        
        try {
            log.debug("Chamando API OdontoPrev para atualizar empresa: {}", codigoEmpresa);
            
            // Busca dados da empresa na view de alteração
            Optional<IntegracaoOdontoprevAlteracao> dadosAlteracao = alteracaoRepository.buscarPrimeiroDadoPorCodigoEmpresa(codigoEmpresa);
            
            if (dadosAlteracao.isEmpty()) {
                throw new RuntimeException("Dados da empresa não encontrados na view de alteração: " + codigoEmpresa);
            }
            
            // Converte dados da view para o request completo da API
            EmpresaAlteracaoRequest requestCompleto = empresaAlteracaoMapper.toAlteracaoRequest(dadosAlteracao.get());
            
            // Atualiza dadosJson na TBSYNC com o request completo que será enviado
            try {
                String dadosJsonCompleto = objectMapper.writeValueAsString(requestCompleto);
                controleSync.setDadosJson(dadosJsonCompleto);
                log.debug("💾 [TBSYNC] Dados JSON atualizados com request completo - tamanho: {} caracteres", dadosJsonCompleto.length());
            } catch (Exception e) {
                log.warn("⚠️ [TBSYNC] Erro ao serializar request completo para TBSYNC: {}", e.getMessage());
            }
            
            // Converte para o tipo base (para compatibilidade com o serviço)
            IntegracaoOdontoprev dadosBase = converterParaIntegracaoBase(dadosAlteracao.get());
            
            // Chama API da OdontoPrev para atualizar empresa
            String responseJson = consultaEmpresaService.alterarEmpresa(dadosBase);
            
            long tempoResposta = System.currentTimeMillis() - inicioTempo;
            
            // Atualiza controle com sucesso
            gerenciadorControleSync.atualizarSucesso(controleSync, responseJson, tempoResposta);
            gerenciadorControleSync.salvar(controleSync);
            
            log.info("✅ [TBSYNC] Registro de alteração salvo na TBSYNC - Empresa: {}, ID: {}, Status: SUCCESS", 
                    codigoEmpresa, controleSync.getId());
            log.info("Empresa alterada {} processada com sucesso em {}ms", codigoEmpresa, tempoResposta);
            
        } catch (Exception e) {
            long tempoResposta = System.currentTimeMillis() - inicioTempo;
            
            // Atualiza controle com erro
            gerenciadorControleSync.atualizarErro(controleSync, e.getMessage());
            gerenciadorControleSync.salvar(controleSync);
            
            log.error("❌ [TBSYNC] Registro de alteração salvo na TBSYNC - Empresa: {}, ID: {}, Status: ERROR", 
                    codigoEmpresa, controleSync.getId());
            log.error("Erro ao processar empresa alterada {} em {}ms: {}", codigoEmpresa, tempoResposta, e.getMessage());
        }
    }

    /**
     * Converte dados de alteração para o tipo base para compatibilidade.
     * 
     * NOTA: A view VW_INTEGRACAO_ODONTOPREV_ALT tem estrutura diferente da view base,
     * então apenas os campos comuns são copiados.
     * 
     * @param dadosAlteracao dados da empresa alterada
     * @return dados convertidos para o tipo base
     */
    private IntegracaoOdontoprev converterParaIntegracaoBase(IntegracaoOdontoprevAlteracao dadosAlteracao) {
        IntegracaoOdontoprev dadosBase = new IntegracaoOdontoprev();
        
        // Copia apenas campos que existem em ambas as views
        dadosBase.setCodigoEmpresa(dadosAlteracao.getCodigoEmpresa());
        dadosBase.setNomeFantasia(dadosAlteracao.getNomeFantasia());
        dadosBase.setDataVigencia(dadosAlteracao.getDataVigencia()); // Já é String na view de alteração
        dadosBase.setCodigoGrupoGerencial(dadosAlteracao.getCodigoGrupoGerencial());
        
        // Campos específicos da view base que não existem na view de alteração são deixados como null
        // Isso é esperado, pois as views têm estruturas diferentes
        
        return dadosBase;
    }
}
