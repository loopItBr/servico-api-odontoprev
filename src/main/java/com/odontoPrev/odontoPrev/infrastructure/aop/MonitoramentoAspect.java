package com.odontoPrev.odontoPrev.infrastructure.aop;

import com.odontoPrev.odontoPrev.infrastructure.exception.*;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * ASPECTO PARA MONITORAMENTO AUTOMÁTICO DE OPERAÇÕES
 * 
 * FUNÇÃO PRINCIPAL:
 * Este é o "coração" da funcionalidade @MonitorarOperacao. É uma classe especial
 * que usa AOP (Programação Orientada a Aspectos) para "interceptar" métodos
 * anotados e adicionar funcionalidades automáticas a eles.
 * 
 * COMO FUNCIONA AOP (ASPECT-ORIENTED PROGRAMMING):
 * Imagine que você tem um método que processa uma empresa:
 * 
 * ANTES (sem AOP):
 * public void processar(String codigo) {
 *     // logs de início...
 *     // medição de tempo...
 *     // lógica do negócio...
 *     // logs de fim...
 *     // tratamento de erro...
 * }
 * 
 * DEPOIS (com AOP):
 * @MonitorarOperacao(operacao = "PROCESSAR")
 * public void processar(String codigo) {
 *     // apenas a lógica do negócio!
 * }
 * 
 * O AOP "envolve" o método original com código adicional, sem modificá-lo.
 * É como colocar uma "camada invisível" ao redor do método.
 * 
 * ANALOGIA SIMPLES:
 * É como um "filtro de café":
 * - Água (dados de entrada) passa pelo filtro
 * - Filtro (AspectJ) adiciona funcionalidades (logs, contexto, etc.)
 * - Café (resultado) sai do outro lado
 * - Método original nem sabe que o filtro existe
 * 
 * BENEFÍCIOS:
 * 1. SEPARAÇÃO DE RESPONSABILIDADES: lógica de negócio fica limpa
 * 2. REUTILIZAÇÃO: mesmo código de monitoramento para todos os métodos
 * 3. MANUTENÇÃO: mudanças no monitoramento em um lugar só
 * 4. TRANSPARÊNCIA: método original não precisa saber do monitoramento
 * 
 * TECNOLOGIA USADA:
 * - Spring AOP: framework que implementa AOP no Spring
 * - AspectJ: biblioteca que fornece anotações e funcionalidades AOP
 * - @Around: tipo de "advice" que executa antes E depois do método
 */
@Aspect
@Component
@Slf4j
public class MonitoramentoAspect {
    
    /**
     * MÉTODO PRINCIPAL QUE INTERCEPTA MÉTODOS ANOTADOS COM @MonitorarOperacao
     * 
     * Este é o "interceptador" principal. Sempre que alguém chama um método
     * que tem @MonitorarOperacao, este método é executado AUTOMATICAMENTE.
     * 
     * FUNCIONAMENTO DETALHADO:
     * 1. ANTES da execução do método original: cria contexto, logs de início
     * 2. DURANTE: executa o método original
     * 3. DEPOIS: adiciona métricas, logs de sucesso/erro, tratamento exceções
     * 
     * PARÂMETROS:
     * - ProceedingJoinPoint: objeto que representa o método interceptado
     * - MonitorarOperacao: a anotação com as configurações
     * 
     * ANOTAÇÃO @Around:
     * Significa "execute este código ANTES e DEPOIS do método interceptado"
     * 
     * EXPRESSÃO "@annotation(monitorarOperacao)":
     * Spring AOP: "intercepte qualquer método que tenha a anotação @MonitorarOperacao"
     */
    @Around("@annotation(monitorarOperacao)")
    public Object monitorarOperacao(ProceedingJoinPoint joinPoint, MonitorarOperacao monitorarOperacao) throws Throwable {
        // 1. Captura momento de início para medir duração
        LocalDateTime inicio = LocalDateTime.now();
        
        // 2. Extrai informações do método interceptado
        String nomeMetodo = joinPoint.getSignature().getName();      // Ex: "processar"
        String nomeClasse = joinPoint.getTarget().getClass().getSimpleName(); // Ex: "ProcessamentoEmpresaServiceImpl"
        
        // 3. Cria contexto inicial com informações básicas
        ContextoSincronizacao ctx = ContextoSincronizacao.criar(monitorarOperacao.operacao())
                .com("classe", nomeClasse)
                .com("metodo", nomeMetodo);
        
        // 4. Adiciona informações de thread se solicitado na anotação
        if (monitorarOperacao.incluirThread()) {
            ctx.comThreadInfo(); // Nome da thread, ID, etc.
        }
        
        // 5. Adiciona parâmetros específicos do método se configurado
        adicionarParametros(ctx, joinPoint, monitorarOperacao.incluirParametros());
        
        try {
            // 6. Log de início da operação
            logarComNivel(monitorarOperacao.logSucesso(), 
                    "Iniciando operação [{}] - {}", monitorarOperacao.operacao(), ctx);
            
            // 7. *** EXECUTA O MÉTODO ORIGINAL *** (este é o ponto principal!)
            // joinPoint.proceed() chama o método que foi interceptado
            Object resultado = joinPoint.proceed();
            
            // 8. FLUXO DE SUCESSO - método executou sem erros
            
            // Adiciona duração total se configurado na anotação
            if (monitorarOperacao.medirDuracao()) {
                ctx.comDuracao(inicio); // Calcula tempo desde início até agora
            }
            
            // Log de sucesso com todas as informações coletadas
            logarComNivel(monitorarOperacao.logSucesso(), 
                    "Operação [{}] concluída com sucesso - {}", 
                    monitorarOperacao.operacao(), ctx.comSucesso());
            
            // Retorna o resultado original do método para quem chamou
            return resultado;
            
        } catch (Exception e) {
            // 9. FLUXO DE ERRO - método lançou exceção
            
            // Adiciona informações do erro ao contexto
            ctx.comErro(e);
            
            // Mede duração mesmo quando deu erro (para análise de performance)
            if (monitorarOperacao.medirDuracao()) {
                ctx.comDuracao(inicio);
            }
            
            // Log detalhado do erro com contexto completo
            logarComNivel(monitorarOperacao.logErro(), 
                    "Erro na operação [{}] - {}", 
                    monitorarOperacao.operacao(), ctx, e);
            
            // Converte exceção genérica em específica se configurado
            if (monitorarOperacao.propagarExcecoes()) {
                throw criarExcecaoCustomizada(monitorarOperacao.excecaoEmErro(), ctx, e);
            }
            
            // Se não propagar exceções, retorna null (caso raro)
            return null;
        }
    }
    
    /**
     * INTERCEPTADOR PARA CLASSES INTEIRAS ANOTADAS
     * 
     * Este método permite anotar uma classe inteira com @MonitorarOperacao
     * e todos os métodos públicos dela serão monitorados automaticamente.
     * 
     * EXPRESSÃO @within(monitorarOperacao):
     * "Intercepte qualquer método dentro de uma classe que tenha @MonitorarOperacao"
     */
    @Around("@within(monitorarOperacao)")
    public Object monitorarClasse(ProceedingJoinPoint joinPoint, MonitorarOperacao monitorarOperacao) throws Throwable {
        // Simplesmente reutiliza a lógica do método principal
        return monitorarOperacao(joinPoint, monitorarOperacao);
    }
    
    /**
     * ADICIONA PARÂMETROS DO MÉTODO AO CONTEXTO DE MONITORAMENTO
     * 
     * Este método extrai os valores dos parâmetros do método interceptado
     * e os adiciona ao contexto, mas apenas os parâmetros especificados
     * na configuração da anotação.
     * 
     * EXEMPLO:
     * Método: processar(String codigo, int tentativas)
     * Anotação: @MonitorarOperacao(incluirParametros = {"codigo"})
     * Resultado: contexto terá {"codigo": "A001"}
     * 
     * PARÂMETROS:
     * - ctx: contexto onde serão adicionados os valores
     * - joinPoint: ponto de interceptação com informações do método
     * - parametrosIncluir: lista de nomes dos parâmetros a incluir
     */
    private void adicionarParametros(ContextoSincronizacao ctx, ProceedingJoinPoint joinPoint, String[] parametrosIncluir) {
        // Se não especificou parâmetros para incluir, não faz nada
        if (parametrosIncluir.length == 0) {
            return;
        }
        
        // Obtém informações detalhadas sobre o método interceptado
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        Parameter[] parameters = method.getParameters();  // Nomes dos parâmetros
        Object[] args = joinPoint.getArgs();              // Valores dos parâmetros
        
        // Percorre todos os parâmetros do método
        for (int i = 0; i < parameters.length; i++) {
            String nomeParametro = parameters[i].getName(); // Ex: "codigo"
            
            // Verifica se este parâmetro foi solicitado na configuração
            for (String incluir : parametrosIncluir) {
                if (nomeParametro.equals(incluir) || incluir.equals("*")) {
                    // Adiciona ao contexto: {"codigo": "A001"}
                    ctx.com(nomeParametro, args[i]);
                }
            }
        }
    }
    
    /**
     * GERA LOGS COM NÍVEL CONFIGURÁVEL
     * 
     * Este método permite que a anotação configure o nível do log
     * (INFO, ERROR, DEBUG, etc.) sem precisar mudança no código.
     * 
     * EXEMPLO:
     * @MonitorarOperacao(logSucesso = INFO, logErro = ERROR)
     * -> Sucesso será logado como INFO, erro como ERROR
     */
    private void logarComNivel(MonitorarOperacao.NivelLog nivel, String mensagem, Object... args) {
        // Switch baseado no enum configurado na anotação
        switch (nivel) {
            case TRACE:
                if (log.isTraceEnabled()) log.trace(mensagem, args);
                break;
            case DEBUG:
                if (log.isDebugEnabled()) log.debug(mensagem, args);
                break;
            case INFO:
                log.info(mensagem, args); // Mais comum
                break;
            case WARN:
                log.warn(mensagem, args);
                break;
            case ERROR:
                // Tratamento especial para ERROR com exceção
                if (args.length > 0 && args[args.length - 1] instanceof Throwable) {
                    Throwable t = (Throwable) args[args.length - 1];
                    Object[] newArgs = new Object[args.length - 1];
                    System.arraycopy(args, 0, newArgs, 0, args.length - 1);
                    log.error(mensagem, newArgs, t); // Log com stack trace
                } else {
                    log.error(mensagem, args);
                }
                break;
        }
    }
    
    /**
     * CONVERTE EXCEÇÕES GENÉRICAS EM EXCEÇÕES ESPECÍFICAS
     * 
     * Este método transforma erros genéricos (Exception, RuntimeException)
     * em exceções customizadas específicas baseado no tipo configurado
     * na anotação @MonitorarOperacao.
     * 
     * BENEFÍCIO:
     * Em vez de lançar "Exception genérica", lança "ProcessamentoEmpresaException"
     * com contexto rico, facilitando identificação do problema.
     * 
     * PARÂMETROS:
     * - tipo: qual tipo de exceção criar (configurado na anotação)
     * - ctx: contexto com todas as informações coletadas
     * - causa: exceção original que foi capturada
     */
    private RuntimeException criarExcecaoCustomizada(
            MonitorarOperacao.TipoExcecao tipo, 
            ContextoSincronizacao ctx, 
            Exception causa) {
        
        // Usa o builder para criar exceção com contexto rico
        SincronizacaoExceptionBuilder builder = SincronizacaoExceptionBuilder.criar()
                .comContexto(ctx)      // Adiciona todo contexto coletado
                .comCausa(causa);      // Preserva exceção original
        
        // Switch baseado no tipo configurado na anotação
        switch (tipo) {
            case INICIALIZACAO_SCHEDULER:
                return builder.inicializacaoScheduler();
            case CONTROLE_EXECUCAO:
                return builder.controleExecucao();
            case CONSULTA_EMPRESAS:
                return builder.consultaEmpresas();
            case PROCESSAMENTO_LOTE:
                return builder.processamentoLote();
            case PROCESSAMENTO_EMPRESA:
                // Para empresa, tenta extrair código do contexto
                String codigoEmpresa = ctx.getDados().getOrDefault("codigoEmpresa", "DESCONHECIDO").toString();
                return builder.processamentoEmpresa(codigoEmpresa);
            case CONFIGURACAO:
                String parametro = ctx.getDados().getOrDefault("parametro", "DESCONHECIDO").toString();
                return builder.configuracao(parametro);
            case RECURSOS_INDISPONIVEIS:
                String recurso = ctx.getDados().getOrDefault("recurso", "DESCONHECIDO").toString();
                return builder.recursosIndisponiveis(recurso);
            case AUTENTICACAO:
                return builder.autenticacao();
            case COMUNICACAO_API:
                String endpoint = ctx.getDados().getOrDefault("endpoint", "DESCONHECIDO").toString();
                return builder.comunicacaoApi(endpoint);
            default:
                // Fallback para o tipo mais genérico
                return builder.processamentoLote();
        }
    }

}