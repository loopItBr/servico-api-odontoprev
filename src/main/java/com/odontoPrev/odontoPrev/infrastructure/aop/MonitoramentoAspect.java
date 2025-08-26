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
 * Aspect para monitoramento automático de operações anotadas com @MonitorarOperacao.
 * Gerencia contexto, logs e tratamento de exceções automaticamente.
 */
@Aspect
@Component
@Slf4j
public class MonitoramentoAspect {
    
    @Around("@annotation(monitorarOperacao)")
    public Object monitorarOperacao(ProceedingJoinPoint joinPoint, MonitorarOperacao monitorarOperacao) throws Throwable {
        LocalDateTime inicio = LocalDateTime.now();
        String nomeMetodo = joinPoint.getSignature().getName();
        String nomeClasse = joinPoint.getTarget().getClass().getSimpleName();
        
        // Criar contexto automaticamente
        ContextoSincronizacao ctx = ContextoSincronizacao.criar(monitorarOperacao.operacao())
                .com("classe", nomeClasse)
                .com("metodo", nomeMetodo);
        
        // Adicionar thread info se configurado
        if (monitorarOperacao.incluirThread()) {
            ctx.comThreadInfo();
        }
        
        // Adicionar parâmetros especificados
        adicionarParametros(ctx, joinPoint, monitorarOperacao.incluirParametros());
        
        try {
            // Log de início
            logarComNivel(monitorarOperacao.logSucesso(), 
                    "Iniciando operação [{}] - {}", monitorarOperacao.operacao(), ctx);
            
            // Executar método
            Object resultado = joinPoint.proceed();
            
            // Adicionar duração se configurado
            if (monitorarOperacao.medirDuracao()) {
                ctx.comDuracao(inicio);
            }
            
            // Log de sucesso
            logarComNivel(monitorarOperacao.logSucesso(), 
                    "Operação [{}] concluída com sucesso - {}", 
                    monitorarOperacao.operacao(), ctx.comSucesso());
            
            return resultado;
            
        } catch (Exception e) {
            // Adicionar erro ao contexto
            ctx.comErro(e);
            
            if (monitorarOperacao.medirDuracao()) {
                ctx.comDuracao(inicio);
            }
            
            // Log de erro
            logarComNivel(monitorarOperacao.logErro(), 
                    "Erro na operação [{}] - {}", 
                    monitorarOperacao.operacao(), ctx, e);
            
            // Lançar exceção apropriada se configurado
            if (monitorarOperacao.propagarExcecoes()) {
                throw criarExcecaoCustomizada(monitorarOperacao.excecaoEmErro(), ctx, e);
            }
            
            return null;
        }
    }
    
    @Around("@within(monitorarOperacao)")
    public Object monitorarClasse(ProceedingJoinPoint joinPoint, MonitorarOperacao monitorarOperacao) throws Throwable {
        // Aplicar monitoramento para todos os métodos da classe anotada
        return monitorarOperacao(joinPoint, monitorarOperacao);
    }
    
    private void adicionarParametros(ContextoSincronizacao ctx, ProceedingJoinPoint joinPoint, String[] parametrosIncluir) {
        if (parametrosIncluir.length == 0) {
            return;
        }
        
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        Parameter[] parameters = method.getParameters();
        Object[] args = joinPoint.getArgs();
        
        for (int i = 0; i < parameters.length; i++) {
            String nomeParametro = parameters[i].getName();
            
            for (String incluir : parametrosIncluir) {
                if (nomeParametro.equals(incluir) || incluir.equals("*")) {
                    ctx.com(nomeParametro, args[i]);
                }
            }
        }
    }
    
    private void logarComNivel(MonitorarOperacao.NivelLog nivel, String mensagem, Object... args) {
        switch (nivel) {
            case TRACE:
                if (log.isTraceEnabled()) log.trace(mensagem, args);
                break;
            case DEBUG:
                if (log.isDebugEnabled()) log.debug(mensagem, args);
                break;
            case INFO:
                log.info(mensagem, args);
                break;
            case WARN:
                log.warn(mensagem, args);
                break;
            case ERROR:
                if (args.length > 0 && args[args.length - 1] instanceof Throwable) {
                    Throwable t = (Throwable) args[args.length - 1];
                    Object[] newArgs = new Object[args.length - 1];
                    System.arraycopy(args, 0, newArgs, 0, args.length - 1);
                    log.error(mensagem, newArgs, t);
                } else {
                    log.error(mensagem, args);
                }
                break;
        }
    }
    
    private RuntimeException criarExcecaoCustomizada(
            MonitorarOperacao.TipoExcecao tipo, 
            ContextoSincronizacao ctx, 
            Exception causa) {
        
        SincronizacaoExceptionBuilder builder = SincronizacaoExceptionBuilder.criar()
                .comContexto(ctx)
                .comCausa(causa);
        
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
                return builder.processamentoLote();
        }
    }
}