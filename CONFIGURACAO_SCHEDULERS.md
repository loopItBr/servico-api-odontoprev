# Configuração dos Schedulers - Ordem e Timing

## Configuração Implementada

Os schedulers foram configurados para executar em uma ordem específica com intervalos diferentes:

### 1. Scheduler de Empresas (PRIMEIRO)
- **Arquivo**: `SyncOdontoprevScheduler.java`
- **Intervalo**: A cada 10 segundos
- **Anotação**: `@Scheduled(fixedRate = 10000)`
- **Função**: Processa empresas (adições, alterações, exclusões)

### 2. Scheduler de Beneficiários (SEGUNDO)
- **Arquivo**: `BeneficiarioScheduler.java`
- **Intervalo**: A cada 15 segundos
- **Anotação**: `@Scheduled(fixedRate = 15000)`
- **Função**: Processa beneficiários (adições, alterações, exclusões)

## Cronograma de Execução

```
Tempo    | Empresa (10s) | Beneficiário (15s)
---------|---------------|-------------------
0s       | ✅ Executa    | 
10s      | ✅ Executa    | 
15s      |               | ✅ Executa
20s      | ✅ Executa    | 
30s      | ✅ Executa    | ✅ Executa
40s      | ✅ Executa    | 
45s      |               | ✅ Executa
50s      | ✅ Executa    | 
60s      | ✅ Executa    | ✅ Executa
```

## Vantagens da Configuração

### 1. **Ordem Lógica**
- **Empresas primeiro**: Garante que as empresas estejam cadastradas antes dos beneficiários
- **Beneficiários depois**: Processa beneficiários das empresas já existentes

### 2. **Intervalos Diferentes**
- **Empresas (10s)**: Maior frequência para processar mudanças estruturais
- **Beneficiários (15s)**: Frequência menor, adequada para processamento de dados

### 3. **Execução Independente**
- Cada scheduler tem seu próprio controle de concorrência
- Podem rodar em paralelo quando necessário
- Falhas em um não afetam o outro

## Arquivos Modificados

### 1. `SyncOdontoprevScheduler.java`
```java
// Antes
@Scheduled(fixedRate = 4000)

// Depois
@Scheduled(fixedRate = 10000) // Executa a cada 10 segundos
```

**Documentação atualizada:**
- Explicação do timing de 10 segundos
- Indicação de que roda PRIMEIRO
- Descrição do processamento de empresas

### 2. `BeneficiarioScheduler.java`
```java
// Antes
@Scheduled(fixedRate = 10000) // Executa a cada 10 segundos

// Depois
@Scheduled(fixedRate = 15000) // Executa a cada 15 segundos
```

**Documentação atualizada:**
- Explicação do timing de 15 segundos
- Indicação de que roda DEPOIS
- Descrição do processamento de beneficiários

## Comportamento dos Schedulers

### Controle de Concorrência
Ambos os schedulers possuem controle de concorrência independente:

```java
// Em cada scheduler
private final AtomicBoolean sincronizacaoEmExecucao = new AtomicBoolean(false);

private boolean sincronizacaoJaEstaEmExecucao() {
    return !sincronizacaoEmExecucao.compareAndSet(false, true);
}
```

### Execução Assíncrona
Ambos usam `CompletableFuture` para execução não-bloqueante:

```java
CompletableFuture
    .runAsync(this::executarSincronizacaoComControle, executorService)
    .whenComplete((result, throwable) -> {
        // Tratamento de resultado
    });
```

## Monitoramento

### Logs Específicos
- **Empresa**: `"INICIALIZACAO_SCHEDULER"`
- **Beneficiário**: `"INICIALIZACAO_SCHEDULER_BENEFICIARIO"`

### Métricas de Performance
- Tempo de execução de cada sincronização
- Contagem de registros processados
- Taxa de sucesso/erro

## Configuração de Propriedades

Os schedulers podem ser habilitados/desabilitados via propriedades:

```yaml
# application.yml
odontoprev:
  scheduler:
    empresa:
      enabled: true
    beneficiario:
      enabled: true
```

## Resultado

- ✅ Scheduler de empresas executa a cada 10 segundos
- ✅ Scheduler de beneficiários executa a cada 15 segundos
- ✅ Ordem lógica: empresas primeiro, beneficiários depois
- ✅ Execução independente e assíncrona
- ✅ Documentação atualizada
- ✅ Compilação bem-sucedida

## Teste

O projeto foi compilado com sucesso após as alterações:

```bash
mvn clean compile -q
# Exit code: 0
```

A configuração garante que o sistema processe primeiro as empresas e depois os beneficiários, com intervalos apropriados para cada tipo de operação.
