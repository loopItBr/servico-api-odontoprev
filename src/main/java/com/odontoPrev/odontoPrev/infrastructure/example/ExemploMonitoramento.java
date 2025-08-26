package com.odontoPrev.odontoPrev.infrastructure.example;

import com.odontoPrev.odontoPrev.infrastructure.aop.MonitorarOperacao;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import static com.odontoPrev.odontoPrev.infrastructure.aop.MonitorarOperacao.TipoExcecao.*;

/**
 * Exemplos de uso da anotação @MonitorarOperacao.
 * Demonstra como eliminar completamente repetição de código.
 */
@Slf4j
@Service
public class ExemploMonitoramento {
    
    // ANTES: Muito código repetitivo
    public void exemploAntes(String empresa) {
        // Map<String, Object> contexto = new HashMap<>();
        // LocalDateTime inicio = LocalDateTime.now();
        // contexto.put("timestamp", inicio);
        // contexto.put("empresa", empresa);
        // contexto.put("operacao", "processamento");
        
        // try {
        //     log.info("Iniciando processamento - {}", contexto);
        //     // lógica aqui...
        //     log.info("Sucesso - {}", contexto.comSucesso().comDuracao(inicio));
        // } catch (Exception e) {
        //     contexto.put("erro", e.getMessage());
        //     log.error("Erro - {}", contexto, e);
        //     throw new ProcessamentoException(...);
        // }
    }
    
    // DEPOIS: Código limpo, apenas lógica de negócio
    @MonitorarOperacao(
            operacao = "PROCESSAMENTO_EMPRESA_EXEMPLO",
            incluirParametros = {"empresa"},
            incluirThread = true,
            excecaoEmErro = PROCESSAMENTO_EMPRESA
    )
    public void exemploDepois(String empresa) {
        // Apenas lógica de negócio!
        if (empresa == null || empresa.isEmpty()) {
            throw new IllegalArgumentException("Empresa não pode ser vazia");
        }
        
        // Simular processamento
        log.info("Processando empresa: {}", empresa);
        
        // Simular possível erro para testar
        if ("ERRO".equals(empresa)) {
            throw new RuntimeException("Erro simulado para teste");
        }
    }
    
    // Exemplo com logs de nível DEBUG
    @MonitorarOperacao(
            operacao = "CONSULTA_RAPIDA",
            logSucesso = MonitorarOperacao.NivelLog.INFO,
            logErro = MonitorarOperacao.NivelLog.WARN,
            medirDuracao = false
    )
    public String consultaRapida(String codigo) {
        return "Resultado para: " + codigo;
    }
    
    // Exemplo que não propaga exceções (apenas loga)
    @MonitorarOperacao(
            operacao = "OPERACAO_OPCIONAL",
            propagarExcecoes = false,
            incluirParametros = {"*"}  // Inclui todos os parâmetros
    )
    public void operacaoOpcional(String param1, int param2, boolean param3) {
        // Se der erro, apenas loga mas não interrompe o fluxo
        throw new RuntimeException("Erro que será apenas logado");
    }
    
    // O Aspect automaticamente:
    // 1. Cria contexto com timestamp
    // 2. Adiciona parâmetros especificados  
    // 3. Loga início da operação
    // 4. Mede duração se configurado
    // 5. Captura exceções e loga
    // 6. Converte para exceção customizada
    // 7. Loga sucesso ou erro
    // 8. Adiciona thread info se solicitado
}