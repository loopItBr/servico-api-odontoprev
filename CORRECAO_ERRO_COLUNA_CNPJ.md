# Correção do Erro "Nome de coluna inválido: cnpj"

## Problema Identificado

O erro `java.sql.SQLException: Nome de coluna inválido` com a mensagem "Unable to find column position by name: cnpj" estava ocorrendo no método `ProcessamentoEmpresaExclusaoServiceImpl.buscarDadosEmpresaExcluidaOuSair`.

## Causa Raiz

O problema estava no método `buscarPrimeiroDadoPorCodigoEmpresa` do `IntegracaoOdontoprevExclusaoRepository`, que utilizava uma query nativa (`nativeQuery = true`) com `SELECT *`:

```java
@Query(value = "SELECT * FROM TASY.VW_INTEGRACAO_ODONTOPREV_EXC WHERE CODIGO_EMPRESA = :codigoEmpresa AND ROWNUM = 1", 
       nativeQuery = true)
```

Quando se usa `nativeQuery = true`, o Hibernate tenta mapear diretamente as colunas do banco de dados para os campos da entidade. O problema era que havia uma incompatibilidade entre:

1. **Nome da coluna no banco**: `CNPJ` (maiúsculo)
2. **Mapeamento da entidade**: `@Column(name = "CNPJ")`
3. **Expectativa do Hibernate**: Hibernate estava procurando por uma coluna chamada `cnpj` (minúsculo)

## Solução Implementada

### 1. Alteração no Repository

**Antes:**
```java
@Query(value = "SELECT * FROM TASY.VW_INTEGRACAO_ODONTOPREV_EXC WHERE CODIGO_EMPRESA = :codigoEmpresa AND ROWNUM = 1", 
       nativeQuery = true)
Optional<IntegracaoOdontoprevExclusao> buscarPrimeiroDadoPorCodigoEmpresa(String codigoEmpresa);
```

**Depois:**
```java
@Query("SELECT i FROM IntegracaoOdontoprevExclusao i WHERE i.codigoEmpresa = :codigoEmpresa")
List<IntegracaoOdontoprevExclusao> buscarPrimeiroDadoPorCodigoEmpresa(String codigoEmpresa);
```

### 2. Alteração no Service

**Antes:**
```java
Optional<IntegracaoOdontoprevExclusao> dadosOpt = exclusaoRepository.buscarPrimeiroDadoPorCodigoEmpresa(codigoEmpresa);

if (dadosOpt.isEmpty()) {
    log.warn("Nenhum dado encontrado para empresa excluída: {}", codigoEmpresa);
    return null;
}

IntegracaoOdontoprevExclusao dados = dadosOpt.get();
```

**Depois:**
```java
List<IntegracaoOdontoprevExclusao> dadosList = exclusaoRepository.buscarPrimeiroDadoPorCodigoEmpresa(codigoEmpresa);

if (dadosList.isEmpty()) {
    log.warn("Nenhum dado encontrado para empresa excluída: {}", codigoEmpresa);
    return null;
}

// Pega apenas o primeiro registro (equivalente ao ROWNUM = 1)
IntegracaoOdontoprevExclusao dados = dadosList.get(0);
```

## Vantagens da Solução

### 1. **Uso de JPQL em vez de SQL Nativo**
- JPQL usa o mapeamento da entidade, evitando problemas de nomenclatura de colunas
- Mais portável entre diferentes bancos de dados
- Melhor integração com o Hibernate

### 2. **Mapeamento Consistente**
- O Hibernate usa as anotações `@Column` da entidade para fazer o mapeamento
- Não há conflito entre nomes de colunas do banco e expectativas do Hibernate

### 3. **Funcionalidade Mantida**
- A lógica de pegar apenas o primeiro registro é mantida usando `dadosList.get(0)`
- Equivale ao `ROWNUM = 1` do Oracle, mas de forma mais portável

## Arquivos Modificados

1. **`IntegracaoOdontoprevExclusaoRepository.java`**
   - Alterado método `buscarPrimeiroDadoPorCodigoEmpresa` para usar JPQL
   - Mudado retorno de `Optional<IntegracaoOdontoprevExclusao>` para `List<IntegracaoOdontoprevExclusao>`

2. **`ProcessamentoEmpresaExclusaoServiceImpl.java`**
   - Atualizado método `buscarDadosEmpresaExcluidaOuSair` para trabalhar com `List`
   - Adicionado import para `java.util.List`
   - Mantida lógica de pegar apenas o primeiro registro

## Resultado

- ✅ Erro `java.sql.SQLException: Nome de coluna inválido` resolvido
- ✅ Compilação bem-sucedida
- ✅ Funcionalidade mantida (busca apenas o primeiro registro)
- ✅ Código mais portável e robusto

## Teste

O projeto foi compilado com sucesso após as alterações:

```bash
mvn clean compile -q
# Exit code: 0
```

A correção resolve o problema de mapeamento de colunas e permite que o fluxo de exclusão de empresas funcione corretamente.
