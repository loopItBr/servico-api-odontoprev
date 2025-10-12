package com.odontoPrev.odontoPrev.infrastructure.client.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.odontoPrev.odontoPrev.domain.service.ConsultaEmpresaOdontoprevService;
import com.odontoPrev.odontoPrev.domain.service.GerenciadorControleSyncService;
import com.odontoPrev.odontoPrev.domain.service.ProcessamentoEmpresaService;
import com.odontoPrev.odontoPrev.infrastructure.client.adapter.out.dto.EmpresaResponse;
import com.odontoPrev.odontoPrev.infrastructure.repository.IntegracaoOdontoprevRepository;
import com.odontoPrev.odontoPrev.infrastructure.repository.entity.ControleSync;
import com.odontoPrev.odontoPrev.infrastructure.repository.entity.IntegracaoOdontoprev;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

/**
 * SERVIÇO PARA PROCESSAMENTO INDIVIDUAL DE EMPRESAS
 * 
 * FUNÇÃO PRINCIPAL:
 * Esta classe é responsável por processar uma única empresa por vez,
 * executando todo o fluxo necessário para sincronização com a OdontoPrev.
 * 
 * FLUXO COMPLETO DE PROCESSAMENTO:
 * 1. BUSCAR dados completos da empresa no banco local
 * 2. CRIAR registro de controle para auditoria
 * 3. CHAMAR API da OdontoPrev para buscar dados da empresa
 * 4. SALVAR resposta e resultado no controle de sincronização
 * 
 * ANALOGIA SIMPLES:
 * É como processar um pedido individual numa loja:
 * 1. Pega informações do cliente (buscar dados da empresa)
 * 2. Abre ficha do pedido (criar controle)
 * 3. Consulta estoque/fornecedor (chama API OdontoPrev) 
 * 4. Anota resultado na ficha (salva controle com resposta)
 * 
 * RESPONSABILIDADES:
 * - Processar UMA empresa por vez (não lotes)
 * - Gerenciar registros de controle e auditoria
 * - Integrar com API externa da OdontoPrev
 * - Tratar erros sem interromper processamento de outras empresas
 * - Medir tempo de resposta para métricas de performance
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
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ProcessamentoEmpresaServiceImpl implements ProcessamentoEmpresaService {

    // Repositório para buscar dados completos da empresa no banco
    private final IntegracaoOdontoprevRepository integracaoRepository;
    
    // Serviço para gerenciar registros de controle e auditoria
    private final GerenciadorControleSyncService gerenciadorControleSync;
    
    // Serviço para chamar API da OdontoPrev
    private final ConsultaEmpresaOdontoprevService consultaEmpresaService;
    
    // Serviço para ativação do plano da empresa
    private final AtivacaoPlanoEmpresaService ativacaoPlanoEmpresaService;
    
    // Conversor JSON para serializar respostas da API
    private final ObjectMapper objectMapper;

    /**
     * MÉTODO PRINCIPAL - PROCESSA UMA EMPRESA INDIVIDUAL
     * 
     * Este é o ponto de entrada para processamento de uma única empresa.
     * Executa todo o fluxo desde busca de dados até salvamento do resultado.
     * 
     * FLUXO DETALHADO:
     * 1. Busca dados completos da empresa no banco local
     * 2. Se não encontrou dados, registra warning e termina
     * 3. Se encontrou, cria registro de controle de sincronização
     * 4. Chama API da OdontoPrev para buscar dados da empresa
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
        log.debug("Processando empresa: {}", codigoEmpresa);

        // PASSO 1: Busca dados completos da empresa no banco
        IntegracaoOdontoprev dadosCompletos = buscarDadosEmpresaOuSair(codigoEmpresa);
        if (dadosCompletos == null) {
            return; // Se não encontrou dados, para aqui
        }
        
        // PASSO 2: Cria ou atualiza registro de controle para auditoria
        log.info("🔍 [EMPRESA] Verificando se já existe registro de controle para empresa {}", codigoEmpresa);
        ControleSync controleSync = criarEMSalvarControleSync(codigoEmpresa, dadosCompletos);
        
        // PASSO 3: Chama API da OdontoPrev e processa resultado
        buscarEProcessarResposta(controleSync, codigoEmpresa);
    }

    /**
     * BUSCA DADOS COMPLETOS DA EMPRESA OU TERMINA PROCESSAMENTO
     * 
     * Este método consulta o banco para obter todos os dados necessários
     * da empresa (planos, contratos, valores, etc.) que serão enviados
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
    private IntegracaoOdontoprev buscarDadosEmpresaOuSair(String codigoEmpresa) {
        // Usa Optional para lidar com possibilidade de dados não existirem
        Optional<IntegracaoOdontoprev> dadosEmpresaOpt = integracaoRepository.buscarPrimeiroDadoPorCodigoEmpresa(codigoEmpresa);
        
        // Se não encontrou dados da empresa
        if (dadosEmpresaOpt.isEmpty()) {
            log.warn("Nenhum dado encontrado para a empresa: {}", codigoEmpresa);
            return null; // Indica que não há dados para processar
        }
        
        // Se encontrou, retorna dados completos
        return dadosEmpresaOpt.get();
    }

    /**
     * CRIA E SALVA REGISTRO DE CONTROLE PARA AUDITORIA
     * 
     * Este método cria um registro na tabela de controle que serve para:
     * 1. AUDITORIA: registrar que empresa foi processada
     * 2. RASTREAMENTO: saber quando foi processada
     * 3. DADOS: guardar quais dados foram enviados para OdontoPrev
     * 4. RESULTADO: registrar se deu certo ou erro
     * 
     * O controle é salvo ANTES da chamada da API para garantir que sempre
     * temos registro do que foi tentado, mesmo se der erro na API.
     */
    private ControleSync criarEMSalvarControleSync(String codigoEmpresa, IntegracaoOdontoprev dadosCompletos) {
        // Cria ou atualiza objeto de controle com dados da empresa
        ControleSync controleSync = gerenciadorControleSync.criarControle(codigoEmpresa, dadosCompletos);
        
        // Salva no banco e retorna com ID gerado
        ControleSync controleSalvo = gerenciadorControleSync.salvar(controleSync);
        
        log.info("📝 [EMPRESA] Registro de controle processado - ID: {}, Status: {}, Tipo: {}", 
                controleSalvo.getId(), controleSalvo.getStatusSync(),
                controleSalvo.getId() != null ? "ATUALIZAÇÃO" : "CRIAÇÃO");
        
        return controleSalvo;
    }

    /**
     * CHAMA API DA ODONTOPREV E PROCESSA RESULTADO
     * 
     * Este é o "coração" da integração. Aqui acontece a comunicação real
     * com a API externa da OdontoPrev e o tratamento do resultado.
     * 
     * FLUXO:
     * 1. Mede tempo de início da chamada
     * 2. Chama API da OdontoPrev
     * 3. Calcula tempo de resposta 
     * 4. Se deu certo: processa resposta de sucesso
     * 5. Se deu erro: captura erro e salva no controle
     * 
     * MEDIÇÃO DE PERFORMANCE:
     * Registra tempo de resposta para monitorar performance da API externa.
     * Importante para identificar lentidão ou problemas na integração.
     */
    private void buscarEProcessarResposta(ControleSync controleSync, String codigoEmpresa) {
        try {
            // Registra momento do início da chamada para medir performance
            long inicioTempo = System.currentTimeMillis();
            
            // *** CHAMADA REAL PARA API DA ODONTOPREV ***
            EmpresaResponse response = consultaEmpresaService.buscarEmpresa(codigoEmpresa);
            
            // Calcula tempo total que a API demorou para responder
            long tempoResposta = System.currentTimeMillis() - inicioTempo;
            
            // Se chegou aqui, API respondeu com sucesso
            processarSucesso(controleSync, response, tempoResposta);
            
        } catch (Exception e) {
            // Se deu qualquer erro na chamada da API
            log.error("Erro ao buscar empresa {}: {}", codigoEmpresa, e.getMessage());
            
            // Atualiza controle com informações do erro
            gerenciadorControleSync.atualizarErro(controleSync, e.getMessage());
            
            // Salva controle atualizado no banco
            gerenciadorControleSync.salvar(controleSync);
        }
    }

    /**
     * PROCESSA RESPOSTA DE SUCESSO DA API
     * 
     * Quando a API da OdontoPrev responde com sucesso, este método:
     * 1. Converte resposta para JSON (para armazenamento)
     * 2. Atualiza controle com dados de sucesso
     * 3. Salva controle atualizado no banco
     * 4. ATIVA O PLANO DA EMPRESA automaticamente
     * 
     * TRATAMENTO DE ERRO NA SERIALIZAÇÃO:
     * Mesmo que a API tenha dado certo, pode dar erro na conversão para JSON.
     * Neste caso, registra como erro no controle.
     */
    private void processarSucesso(ControleSync controleSync, EmpresaResponse response, long tempoResposta) {
        try {
            // Converte objeto de resposta para JSON (String)
            String responseJson = objectMapper.writeValueAsString(response);
            
            // Atualiza controle com dados de sucesso
            gerenciadorControleSync.atualizarSucesso(controleSync, responseJson, tempoResposta);
            
            // Salva controle com informações de sucesso
            gerenciadorControleSync.salvar(controleSync);
            
            // 🚀 NOVA FUNCIONALIDADE: Ativar plano da empresa após sincronização bem-sucedida
            log.info("🎯 [SINCRONIZAÇÃO] Empresa {} sincronizada com sucesso, iniciando ativação do plano", 
                    controleSync.getCodigoEmpresa());
            
            // Buscar dados completos da empresa para ativação
            IntegracaoOdontoprev dadosEmpresa = buscarDadosEmpresaOuSair(controleSync.getCodigoEmpresa());
            if (dadosEmpresa != null) {
                ativacaoPlanoEmpresaService.ativarPlanoEmpresa(dadosEmpresa);
            } else {
                log.warn("⚠️ [ATIVAÇÃO PLANO] Não foi possível obter dados da empresa {} para ativação do plano", 
                        controleSync.getCodigoEmpresa());
            }
            
        } catch (Exception e) {
            // Erro na conversão para JSON (raro, mas pode acontecer)
            log.error("Erro ao processar resposta da empresa {}: {}", 
                    controleSync.getCodigoEmpresa(), e.getMessage());
            
            // Mesmo tendo recebido resposta da API, registra como erro 
            // porque não conseguimos armazenar adequadamente
            gerenciadorControleSync.atualizarErro(controleSync, 
                    "Erro ao serializar resposta: " + e.getMessage());
            
            gerenciadorControleSync.salvar(controleSync);
        }
    }

}