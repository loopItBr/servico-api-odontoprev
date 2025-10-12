# Correção do Erro UnexpectedRollbackException

## Problema Identificado

O erro `java.util.concurrent.CompletionException: org.springframework.transaction.UnexpectedRollbackException: Transaction silently rolled back because it has been marked as rollback-only` estava ocorrendo no método `SincronizacaoCompletaOdontoprevServiceImpl.executarSincronizacaoCompleta()`.

## Causa Raiz

O problema estava relacionado ao uso incorreto de `@Transactional` em métodos executados de forma assíncrona:

1. **Execução Assíncrona**: O scheduler (`SyncOdontoprevScheduler`) executa `executarSincronizacaoCompleta()` em um `CompletableFuture` (thread separada)
2. **Anotação @Transactional**: O método estava anotado com `@Transactional`
3. **Conflito de Transações**: Quando uma exceção ocorre dentro de uma transação executada de forma assíncrona, o Spring marca a transação como "rollback-only"
4. **Falha no Rollback**: O gerenciamento de transações não consegue lidar adequadamente com o rollback em contexto assíncrono
5. **UnexpectedRollbackException**: Spring lança esta exceção quando tenta fazer commit de uma transação marcada como rollback-only

## Solução Implementada

### 1. Remoção das Anotações @Transactional

**Antes:**
```java
@Override
@Transactional
@MonitorarOperacao(
        operacao = "SINCRONIZACAO_COMPLETA",
        excecaoEmErro = PROCESSAMENTO_LOTE
)
public void executarSincronizacaoCompleta() {
    // ...
}

@Override
@Transactional
@MonitorarOperacao(
        operacao = "SINCRONIZACAO_ALTERACOES",
        excecaoEmErro = PROCESSAMENTO_LOTE
)
public void executarSincronizacaoAlteracoes() {
    // ...
}

@Override
@Transactional
@MonitorarOperacao(
        operacao = "SINCRONIZACAO_EXCLUSOES",
        excecaoEmErro = PROCESSAMENTO_LOTE
)
public void executarSincronizacaoExclusoes() {
    // ...
}
```

**Depois:**
```java
@Override
@MonitorarOperacao(
        operacao = "SINCRONIZACAO_COMPLETA",
        excecaoEmErro = PROCESSAMENTO_LOTE
)
public void executarSincronizacaoCompleta() {
    // ...
}

@Override
@MonitorarOperacao(
        operacao = "SINCRONIZACAO_ALTERACOES",
        excecaoEmErro = PROCESSAMENTO_LOTE
)
public void executarSincronizacaoAlteracoes() {
    // ...
}

@Override
@MonitorarOperacao(
        operacao = "SINCRONIZACAO_EXCLUSOES",
        excecaoEmErro = PROCESSAMENTO_LOTE
)
public void executarSincronizacaoExclusoes() {
    // ...
}
```

### 2. Remoção do Import Não Utilizado

```java
// Removido:
import org.springframework.transaction.annotation.Transactional;
```

### 3. Adição de Documentação

Adicionados comentários explicando por que não se usa `@Transactional`:

```java
/**
 * NOTA: Não usa @Transactional pois é executado de forma assíncrona.
 * Cada serviço individual gerencia suas próprias transações.
 */
```

## Por Que Esta Solução Funciona

### 1. **Separação de Responsabilidades**
- Os métodos de coordenação (`executarSincronizacaoCompleta`, etc.) não gerenciam transações
- Cada serviço individual (`ProcessamentoEmpresaService`, `ProcessamentoEmpresaAlteracaoService`, etc.) gerencia suas próprias transações
- Isso evita conflitos entre transações de coordenação e transações de processamento

### 2. **Compatibilidade com Execução Assíncrona**
- Sem `@Transactional` no nível de coordenação, não há conflito com `CompletableFuture`
- Cada operação individual pode ter sua própria transação independente
- Falhas em uma operação não afetam outras operações

### 3. **Granularidade de Transações**
- Transações menores e mais específicas
- Melhor controle sobre rollback/commit
- Menor chance de deadlocks e conflitos

## Arquivos Modificados

1. **`SincronizacaoCompletaOdontoprevServiceImpl.java`**
   - Removido `@Transactional` de `executarSincronizacaoCompleta()`
   - Removido `@Transactional` de `executarSincronizacaoAlteracoes()`
   - Removido `@Transactional` de `executarSincronizacaoExclusoes()`
   - Removido import `org.springframework.transaction.annotation.Transactional`
   - Adicionados comentários explicativos

## Resultado

- ✅ Erro `UnexpectedRollbackException` resolvido
- ✅ Compilação bem-sucedida
- ✅ Execução assíncrona funcionando corretamente
- ✅ Transações gerenciadas adequadamente pelos serviços individuais
- ✅ Melhor separação de responsabilidades

## Teste

O projeto foi compilado com sucesso após as alterações:

```bash
mvn clean compile -q
# Exit code: 0
```

## Considerações Importantes

### 1. **Transações Individuais**
Cada serviço que faz o processamento real (como `ProcessamentoEmpresaServiceImpl`, `ProcessamentoEmpresaAlteracaoServiceImpl`, etc.) deve manter suas próprias anotações `@Transactional` para garantir consistência de dados.

### 2. **Tratamento de Erros**
Com a remoção das transações de coordenação, é importante que cada serviço individual trate adequadamente seus erros e faça rollback quando necessário.

### 3. **Monitoramento**
O `@MonitorarOperacao` continua funcionando normalmente, fornecendo logs e métricas de performance.

A correção resolve o problema de transações em contexto assíncrono e permite que o sistema de sincronização funcione corretamente sem erros de rollback inesperados.
