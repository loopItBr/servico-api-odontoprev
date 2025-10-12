# Correções para Erros de Runtime

## Problemas Identificados

### 1. Erro: `ORA-01400: não é possível inserir NULL em ("TASY"."TB_CONTROLE_SYNC_ODONTOPREV"."TIPO_OPERACAO")`

**Causa**: O campo `tipoOperacao` estava sendo inserido como NULL no `AtivacaoPlanoEmpresaServiceImpl`.

**Solução**: Adicionado o campo `tipoOperacao` nos métodos de criação de `ControleSync`:

```java
// Em criarRegistroControleAtivacao()
ControleSync controle = ControleSync.builder()
        .codigoEmpresa(codigoEmpresa)
        .tipoOperacao(ControleSync.TipoOperacao.CREATE) // ✅ ADICIONADO
        .tipoControle(3)
        .endpointDestino("/empresa/2.0/empresas/contrato/empresarial")
        .dadosJson(payloadJson)
        .statusSync(ControleSync.StatusSync.PENDING)
        .dataCriacao(LocalDateTime.now())
        .build();

// Em processarErroAtivacao()
controleSync = ControleSync.builder()
        .codigoEmpresa(codigoEmpresa)
        .tipoOperacao(ControleSync.TipoOperacao.CREATE) // ✅ ADICIONADO
        .tipoControle(3)
        .endpointDestino("/empresa/2.0/empresas/contrato/empresarial")
        .statusSync(ControleSync.StatusSync.ERROR)
        .dataCriacao(LocalDateTime.now())
        .build();
```

### 2. Erro: `java.sql.SQLException: Nome de coluna inválido`

**Causa**: Inconsistência no tamanho da coluna `CNPJ` entre as entidades:
- `IntegracaoOdontoprevAlteracao`: `length = 18`
- `IntegracaoOdontoprev`: `length = 14`
- `IntegracaoOdontoprevExclusao`: `length = 14`

**Solução**: Padronizado o tamanho da coluna `CNPJ` para `length = 14` em todas as entidades:

```java
// Em IntegracaoOdontoprevAlteracao.java
@Column(name = "CNPJ", nullable = true, length = 14) // ✅ CORRIGIDO de 18 para 14
```

## Arquivos Modificados

1. **`AtivacaoPlanoEmpresaServiceImpl.java`**
   - Adicionado `tipoOperacao` em `criarRegistroControleAtivacao()`
   - Adicionado `tipoOperacao` em `processarErroAtivacao()`

2. **`IntegracaoOdontoprevAlteracao.java`**
   - Corrigido `length` da coluna `CNPJ` de 18 para 14

## Status

✅ **Compilação**: Bem-sucedida
✅ **Linter**: Sem erros
✅ **Correções**: Implementadas

## Próximos Passos

1. Testar a aplicação em runtime para verificar se os erros foram resolvidos
2. Monitorar logs para confirmar que os registros de controle estão sendo criados corretamente
3. Verificar se as sincronizações de empresa e ativação de planos estão funcionando sem erros

## Observações

- O campo `tipoOperacao` é obrigatório na tabela `TB_CONTROLE_SYNC_ODONTOPREV`
- A consistência no tamanho das colunas é importante para evitar erros de SQL
- Todas as entidades de integração agora têm o mesmo padrão para a coluna `CNPJ`
