# Diagnóstico - Scheduler de Beneficiário Não Captura Novos Registros

## Problema Reportado

O scheduler de beneficiário não está capturando novos registros na view `VW_INTEGRACAO_ODONTOPREV_BENEFICIARIOS_ALT` mesmo após adicionar um novo registro.

## Análise do Fluxo

### 1. Fluxo do Scheduler
```
BeneficiarioScheduler (15s) 
  ↓
SincronizacaoCompletaBeneficiarioService.executarSincronizacaoCompleta()
  ↓
executarSincronizacaoAlteracoes()
  ↓
contarTotalAlteracoes() → alteracaoRepository.count()
  ↓
processarAlteracoesEmLotes() → alteracaoRepository.findWithLimit()
  ↓
processarLoteAlteracoes() → processamentoAlteracoes.processar()
```

### 2. Métodos de Contagem e Busca
- **Contagem**: `alteracaoRepository.count()` - Query nativa: `SELECT COUNT(*) FROM TASY.VW_INTEGRACAO_ODONTOPREV_BENEFICIARIOS_ALT`
- **Busca**: `alteracaoRepository.findWithLimit()` - Query nativa: `SELECT * FROM TASY.VW_INTEGRACAO_ODONTOPREV_BENEFICIARIOS_ALT ORDER BY CDEMPRESA`

## Possíveis Causas

### 1. **View Não Retorna Dados**
- A view `VW_INTEGRACAO_ODONTOPREV_BENEFICIARIOS_ALT` pode estar vazia
- O novo registro pode não atender aos critérios da view
- A view pode ter algum filtro que exclui o registro

### 2. **Problema de Mapeamento**
- O campo `MATRICULA` que adicionamos pode não existir na view
- Isso pode causar erro no mapeamento e impedir a busca

### 3. **Problema de Transação**
- O novo registro pode não estar commitado no banco
- A view pode estar lendo dados de uma transação não commitada

### 4. **Problema de Cache**
- O Hibernate pode estar fazendo cache dos dados
- A view pode estar sendo cacheada pelo Oracle

## Soluções Implementadas

### 1. **Revertido Campo MATRICULA**
- Removido o campo `MATRICULA` da entidade `IntegracaoOdontoprevBeneficiarioAlteracao`
- Revertido o mapper para usar `cdAssociado` como `codigoMatricula`
- Isso evita erros de mapeamento se o campo não existir na view

### 2. **Script de Teste Criado**
- Criado `test_view_alteracao.sql` para testar a view diretamente no banco
- Permite verificar se a view está retornando dados

## Próximos Passos para Diagnóstico

### 1. **Executar Script de Teste**
```sql
-- Execute no banco Oracle
SELECT COUNT(*) as TOTAL_REGISTROS 
FROM TASY.VW_INTEGRACAO_ODONTOPREV_BENEFICIARIOS_ALT;
```

### 2. **Verificar Logs do Scheduler**
Procurar nos logs por:
```
📊 CONTAGEM BENEFICIÁRIOS: Total de alterações encontradas: X
```

### 3. **Verificar Estrutura da View**
```sql
DESC TASY.VW_INTEGRACAO_ODONTOPREV_BENEFICIARIOS_ALT;
```

### 4. **Verificar Critérios da View**
- Verificar se o novo registro atende aos critérios da view
- Verificar se há filtros que podem excluir o registro

## Configurações do Scheduler

### Timing
- **Empresas**: A cada 10 segundos
- **Beneficiários**: A cada 15 segundos

### Logs Esperados
```
🚀 SINCRONIZAÇÃO BENEFICIÁRIOS: Iniciando sincronização completa com OdontoPrev
📝 SINCRONIZAÇÃO BENEFICIÁRIOS: Executando alterações
📊 CONTAGEM BENEFICIÁRIOS: Total de alterações encontradas: X
```

## Verificações Necessárias

### 1. **No Banco de Dados**
- [ ] Executar script de teste
- [ ] Verificar se a view retorna dados
- [ ] Verificar estrutura da view
- [ ] Verificar critérios da view

### 2. **Nos Logs da Aplicação**
- [ ] Verificar se o scheduler está executando
- [ ] Verificar contagem de alterações
- [ ] Verificar se há erros de mapeamento

### 3. **Na View**
- [ ] Verificar se o novo registro aparece na view
- [ ] Verificar se há filtros que excluem o registro
- [ ] Verificar se a view está atualizada

## Arquivos Modificados

1. **`IntegracaoOdontoprevBeneficiarioAlteracao.java`**
   - Removido campo `MATRICULA` (pode não existir na view)

2. **`BeneficiarioViewMapper.java`**
   - Revertido para usar `cdAssociado` como `codigoMatricula`

3. **`test_view_alteracao.sql`**
   - Script para testar a view diretamente no banco

## Resultado Esperado

Após as correções, o scheduler deve:
1. Executar a cada 15 segundos
2. Contar registros na view de alteração
3. Processar registros encontrados
4. Logar o progresso adequadamente

Se ainda não capturar registros, o problema está na view ou nos critérios de seleção.
