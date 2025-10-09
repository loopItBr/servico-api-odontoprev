package com.odontoPrev.odontoPrev.infrastructure.client.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.odontoPrev.odontoPrev.domain.service.ConsultaEmpresaOdontoprevExpandidaService;
import com.odontoPrev.odontoPrev.domain.service.GerenciadorControleSyncService;
import com.odontoPrev.odontoPrev.domain.service.ProcessamentoEmpresaExclusaoService;
import com.odontoPrev.odontoPrev.infrastructure.repository.IntegracaoOdontoprevExclusaoRepository;
import com.odontoPrev.odontoPrev.infrastructure.repository.entity.ControleSync;
import com.odontoPrev.odontoPrev.infrastructure.repository.entity.IntegracaoOdontoprev;
import com.odontoPrev.odontoPrev.infrastructure.repository.entity.IntegracaoOdontoprevExclusao;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * SERVIÇO PARA PROCESSAMENTO DE EMPRESAS EXCLUÍDAS
 * 
 * FUNÇÃO PRINCIPAL:
 * Esta classe é responsável por processar empresas que foram inativadas/excluídas
 * e precisam ser removidas da OdontoPrev.
 * 
 * FLUXO COMPLETO DE PROCESSAMENTO:
 * 1. BUSCAR dados completos da empresa excluída no banco local
 * 2. CRIAR registro de controle para auditoria (tipo_controle = 3)
 * 3. CHAMAR API da OdontoPrev para excluir empresa
 * 4. SALVAR resposta e resultado no controle de sincronização
 * 
 * CRITÉRIO DE SELEÇÃO:
 * - Empresas com ATIVO = 2 (inativas)
 * - Planos com ATIVO = 2 e IE_SEGMENTACAO = 4 (excluídos)
 * - Identifica empresas que precisam ser removidas da OdontoPrev
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
 * - tipo_controle = 3 (Exclusão)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ProcessamentoEmpresaExclusaoServiceImpl implements ProcessamentoEmpresaExclusaoService {

    // Repositório para buscar dados de empresas excluídas
    private final IntegracaoOdontoprevExclusaoRepository exclusaoRepository;
    
    // Serviço para gerenciar registros de controle e auditoria
    private final GerenciadorControleSyncService gerenciadorControleSync;
    
    // Serviço para chamar API da OdontoPrev (expandido para suportar exclusões)
    private final ConsultaEmpresaOdontoprevExpandidaService consultaEmpresaService;
    
    // Conversor JSON para serializar respostas da API
    private final ObjectMapper objectMapper;

    /**
     * MÉTODO PRINCIPAL - PROCESSA UMA EMPRESA EXCLUÍDA INDIVIDUAL
     * 
     * Este é o ponto de entrada para processamento de uma única empresa excluída.
     * Executa todo o fluxo desde busca de dados até salvamento do resultado.
     * 
     * FLUXO DETALHADO:
     * 1. Busca dados completos da empresa excluída no banco local
     * 2. Se não encontrou dados, registra warning e termina
     * 3. Se encontrou, cria registro de controle de sincronização (tipo_controle = 3)
     * 4. Chama API da OdontoPrev para excluir empresa
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
        log.debug("Processando empresa excluída: {}", codigoEmpresa);

        // PASSO 1: Busca dados completos da empresa excluída no banco
        IntegracaoOdontoprevExclusao dadosCompletos = buscarDadosEmpresaExcluidaOuSair(codigoEmpresa);
        if (dadosCompletos == null) {
            return; // Se não encontrou dados, para aqui
        }
        
        // PASSO 2: Cria registro de controle para auditoria (tipo_controle = 3)
        ControleSync controleSync = criarEMSalvarControleSyncExclusao(codigoEmpresa, dadosCompletos);
        
        // PASSO 3: Chama API da OdontoPrev e processa resultado
        buscarEProcessarResposta(controleSync, codigoEmpresa);
    }

    /**
     * BUSCA DADOS COMPLETOS DA EMPRESA EXCLUÍDA OU TERMINA PROCESSAMENTO
     * 
     * Este método consulta o banco para obter todos os dados necessários
     * da empresa excluída (planos, contratos, valores, etc.) que serão enviados
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
    private IntegracaoOdontoprevExclusao buscarDadosEmpresaExcluidaOuSair(String codigoEmpresa) {
        try {
            Optional<IntegracaoOdontoprevExclusao> dadosOpt = exclusaoRepository.buscarPrimeiroDadoPorCodigoEmpresa(codigoEmpresa);
            
            if (dadosOpt.isEmpty()) {
                log.warn("Nenhum dado encontrado para empresa excluída: {}", codigoEmpresa);
                return null;
            }
            
            IntegracaoOdontoprevExclusao dados = dadosOpt.get();
            log.debug("Dados encontrados para empresa excluída {}: {}", codigoEmpresa, dados.getNomeFantasia());
            return dados;
            
        } catch (Exception e) {
            log.error("Erro ao buscar dados da empresa excluída {}: {}", codigoEmpresa, e.getMessage());
            return null;
        }
    }

    /**
     * CRIA E SALVA REGISTRO DE CONTROLE DE SINCRONIZAÇÃO PARA EXCLUSÃO
     * 
     * Este método cria um registro na tabela de controle que será usado para:
     * 1. Auditoria do que foi enviado para OdontoPrev
     * 2. Rastreamento de sucesso/erro
     * 3. Reprocessamento em caso de falha
     * 
     * CARACTERÍSTICAS ESPECÍFICAS PARA EXCLUSÃO:
     * - tipo_operacao = DELETE
     * - tipo_controle = 3 (Exclusão)
     * - endpoint_destino = "/empresas/{codigo}/excluir"
     */
    private ControleSync criarEMSalvarControleSyncExclusao(String codigoEmpresa, IntegracaoOdontoprevExclusao dados) {
        try {
            // Converte dados para o tipo base para compatibilidade
            IntegracaoOdontoprev dadosBase = converterParaIntegracaoBase(dados);
            
            ControleSync controle = gerenciadorControleSync.criarControle(
                codigoEmpresa, 
                dadosBase, 
                ControleSync.TipoOperacao.DELETE,
                ControleSync.TipoControle.EXCLUSAO
            );
            
            ControleSync salvo = gerenciadorControleSync.salvar(controle);
            log.debug("Controle de exclusão criado para empresa {} com ID: {}", codigoEmpresa, salvo.getId());
            return salvo;
            
        } catch (Exception e) {
            log.error("Erro ao criar controle de exclusão para empresa {}: {}", codigoEmpresa, e.getMessage());
            throw new RuntimeException("Falha na criação do controle de exclusão", e);
        }
    }

    /**
     * BUSCA E PROCESSA RESPOSTA DA API DA ODONTOPREV
     * 
     * Este método chama a API da OdontoPrev para excluir a empresa
     * e processa a resposta (sucesso ou erro).
     * 
     * FLUXO:
     * 1. Chama API da OdontoPrev
     * 2. Mede tempo de resposta
     * 3. Se sucesso: salva resposta no controle
     * 4. Se erro: salva mensagem de erro no controle
     */
    private void buscarEProcessarResposta(ControleSync controleSync, String codigoEmpresa) {
        long inicioTempo = System.currentTimeMillis();
        
        try {
            log.debug("Chamando API OdontoPrev para excluir empresa: {}", codigoEmpresa);
            
            // Busca dados da empresa na view de exclusão
            Optional<IntegracaoOdontoprevExclusao> dadosExclusao = exclusaoRepository.findByCodigoEmpresa(codigoEmpresa);
            
            if (dadosExclusao.isEmpty()) {
                throw new RuntimeException("Dados da empresa não encontrados na view de exclusão: " + codigoEmpresa);
            }
            
            // Converte para o tipo base
            IntegracaoOdontoprev dadosBase = converterParaIntegracaoBase(dadosExclusao.get());
            
            // Chama API da OdontoPrev para inativar empresa
            String responseJson = consultaEmpresaService.inativarEmpresa(dadosBase);
            
            long tempoResposta = System.currentTimeMillis() - inicioTempo;
            
            // Atualiza controle com sucesso
            gerenciadorControleSync.atualizarSucesso(controleSync, responseJson, tempoResposta);
            gerenciadorControleSync.salvar(controleSync);
            
            log.info("Empresa excluída {} processada com sucesso em {}ms", codigoEmpresa, tempoResposta);
            
        } catch (Exception e) {
            long tempoResposta = System.currentTimeMillis() - inicioTempo;
            
            // Atualiza controle com erro
            gerenciadorControleSync.atualizarErro(controleSync, e.getMessage());
            gerenciadorControleSync.salvar(controleSync);
            
            log.error("Erro ao processar empresa excluída {} em {}ms: {}", codigoEmpresa, tempoResposta, e.getMessage());
        }
    }

    /**
     * Converte dados de exclusão para o tipo base para compatibilidade.
     * 
     * @param dadosExclusao dados da empresa excluída
     * @return dados convertidos para o tipo base
     */
    private IntegracaoOdontoprev converterParaIntegracaoBase(IntegracaoOdontoprevExclusao dadosExclusao) {
        IntegracaoOdontoprev dadosBase = new IntegracaoOdontoprev();
        
        // Copia todos os campos comuns
        dadosBase.setCodigoEmpresa(dadosExclusao.getCodigoEmpresa());
        dadosBase.setCnpj(dadosExclusao.getCnpj());
        dadosBase.setCodigoClienteOperadora(dadosExclusao.getCodigoClienteOperadora() != null ? Long.valueOf(dadosExclusao.getCodigoClienteOperadora()) : null);
        dadosBase.setNomeFantasia(dadosExclusao.getNomeFantasia());
        dadosBase.setDataInicioContrato(dadosExclusao.getDataInicioContrato());
        dadosBase.setDataFimContrato(dadosExclusao.getDataFimContrato());
        dadosBase.setDataVigencia(dadosExclusao.getDataVigencia());
        dadosBase.setEmpresaPf(dadosExclusao.getEmpresaPf() != null ? dadosExclusao.getEmpresaPf().toString() : null);
        dadosBase.setCodigoGrupoGerencial(dadosExclusao.getCodigoGrupoGerencial());
        dadosBase.setCodigoMarca(dadosExclusao.getCodigoMarca());
        dadosBase.setCodigoCelula(dadosExclusao.getCodigoCelula());
        dadosBase.setVidasAtivas(dadosExclusao.getVidasAtivas());
        dadosBase.setValorUltimoFaturamento(dadosExclusao.getValorUltimoFaturamento());
        dadosBase.setSinistralidade(dadosExclusao.getSinistralidade());
        dadosBase.setCodigoPlano(dadosExclusao.getCodigoPlano() != null ? Long.valueOf(dadosExclusao.getCodigoPlano()) : null);
        dadosBase.setDescricaoPlano(dadosExclusao.getDescricaoPlano());
        dadosBase.setNomeFantasiaPlano(dadosExclusao.getNomeFantasiaPlano());
        dadosBase.setNumeroRegistroAns(dadosExclusao.getNumeroRegistroAns());
        dadosBase.setSiglaPlano(dadosExclusao.getSiglaPlano());
        dadosBase.setValorTitular(dadosExclusao.getValorTitular());
        dadosBase.setValorDependente(dadosExclusao.getValorDependente());
        dadosBase.setDataInicioPlano(dadosExclusao.getDataInicioPlano());
        dadosBase.setDataFimPlano(dadosExclusao.getDataFimPlano());
        dadosBase.setCoParticipacao(dadosExclusao.getCoParticipacao() != null ? dadosExclusao.getCoParticipacao().toString() : null);
        dadosBase.setTipoNegociacao(dadosExclusao.getTipoNegociacao());
        dadosBase.setCodigoTipoCobranca(dadosExclusao.getCodigoTipoCobranca());
        dadosBase.setNomeTipoCobranca(dadosExclusao.getNomeTipoCobranca());
        dadosBase.setSiglaTipoCobranca(dadosExclusao.getSiglaTipoCobranca());
        dadosBase.setNumeroBanco(dadosExclusao.getNumeroBanco());
        dadosBase.setNomeBanco(dadosExclusao.getNomeBanco());
        dadosBase.setNumeroParcelas(dadosExclusao.getNumeroParcelas());
        
        return dadosBase;
    }
}
