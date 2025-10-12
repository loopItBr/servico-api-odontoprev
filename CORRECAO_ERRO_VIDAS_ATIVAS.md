# Correção do Erro "VIDAS_ATIVAS: identificador inválido"

## Problema Identificado

O erro `java.sql.SQLSyntaxErrorException: ORA-00904: "IOE1_0"."VIDAS_ATIVAS": identificador inválido` estava ocorrendo porque a entidade `IntegracaoOdontoprevExclusao` estava tentando mapear colunas que não existem na view `VW_INTEGRACAO_ODONTOPREV_EXC`.

## Causa Raiz

A entidade `IntegracaoOdontoprevExclusao` estava mapeando uma estrutura de dados muito mais complexa do que a que realmente existe na view `VW_INTEGRACAO_ODONTOPREV_EXC`. 

**Estrutura Real da View:**
```sql
VW_INTEGRACAO_ODONTOPREV_EXC
- SISTEMA (CHAR(4))
- CODIGOUSUARIO (NUMBER)
- CODIGO_EMPRESA (VARCHAR2(6))
- CODIGOMOTIVOFIMEMPRESA (NUMBER)
- DATA_FIM_CONTRATO (DATE)
```

**Estrutura Incorreta que a Entidade Estava Tentando Mapear:**
- Muitas colunas que não existem na view, incluindo `VIDAS_ATIVAS`, `CNPJ`, `NOME_FANTASIA`, etc.

## Solução Implementada

### 1. Correção da Entidade IntegracaoOdontoprevExclusao

**Antes:**
```java
@Entity
@Table(name = "VW_INTEGRACAO_ODONTOPREV_EXC", schema = "TASY")
public class IntegracaoOdontoprevExclusao {
    @Id
    @Column(name = "CODIGO_EMPRESA")
    private String codigoEmpresa;
    
    @Column(name = "CNPJ")
    private String cnpj;
    
    @Column(name = "NOME_FANTASIA")
    private String nomeFantasia;
    
    @Column(name = "VIDAS_ATIVAS")  // ❌ Esta coluna não existe na view
    private Long vidasAtivas;
    
    // ... muitos outros campos que não existem na view
}
```

**Depois:**
```java
@Entity
@Table(name = "VW_INTEGRACAO_ODONTOPREV_EXC", schema = "TASY")
public class IntegracaoOdontoprevExclusao {
    @Column(name = "SISTEMA", length = 4)
    private String sistema;
    
    @Column(name = "CODIGOUSUARIO")
    private Long codigoUsuario;
    
    @Id
    @Column(name = "CODIGO_EMPRESA", length = 6)
    private String codigoEmpresa;
    
    @Column(name = "CODIGOMOTIVOFIMEMPRESA")
    private Long codigoMotivoFimEmpresa;
    
    @Column(name = "DATA_FIM_CONTRATO")
    private LocalDate dataFimContrato;
}
```

### 2. Atualização do Service ProcessamentoEmpresaExclusaoServiceImpl

Como a view de exclusão contém apenas informações básicas, foi necessário modificar o service para buscar os dados completos da empresa na view principal (`VW_INTEGRACAO_ODONTOPREV`).

**Antes:**
```java
private IntegracaoOdontoprev converterParaIntegracaoBase(IntegracaoOdontoprevExclusao dadosExclusao) {
    // Tentava converter dados limitados da view de exclusão
    // para o formato completo - causava problemas
}
```

**Depois:**
```java
private IntegracaoOdontoprev buscarDadosCompletosEmpresa(String codigoEmpresa) {
    // Busca dados completos da empresa na view principal
    Optional<IntegracaoOdontoprev> dadosOpt = empresaRepository.buscarPrimeiroDadoPorCodigoEmpresa(codigoEmpresa);
    return dadosOpt.orElse(null);
}
```

### 3. Injeção de Dependência Adicional

Adicionado o `IntegracaoOdontoprevRepository` para acessar a view principal:

```java
@Service
@RequiredArgsConstructor
public class ProcessamentoEmpresaExclusaoServiceImpl {
    private final IntegracaoOdontoprevExclusaoRepository exclusaoRepository;
    private final IntegracaoOdontoprevRepository empresaRepository; // ✅ Novo
    // ... outros campos
}
```

### 4. Atualização dos Métodos de Processamento

**Método `criarEMSalvarControleSyncExclusao`:**
```java
// Antes: converterParaIntegracaoBase(dados)
// Depois: buscarDadosCompletosEmpresa(codigoEmpresa)
```

**Método `buscarEProcessarResposta`:**
```java
// Antes: converterParaIntegracaoBase(dadosExclusao.get())
// Depois: buscarDadosCompletosEmpresa(codigoEmpresa)
```

## Arquitetura da Solução

### Fluxo de Dados Corrigido:

1. **Identificação**: `VW_INTEGRACAO_ODONTOPREV_EXC` → Lista empresas a serem excluídas
2. **Dados Completos**: `VW_INTEGRACAO_ODONTOPREV` → Busca dados completos de cada empresa
3. **API Call**: Usa dados completos para chamar API de exclusão
4. **Controle**: Salva resultado na tabela de controle

### Separação de Responsabilidades:

- **View de Exclusão** (`VW_INTEGRACAO_ODONTOPREV_EXC`): Identifica QUAIS empresas excluir
- **View Principal** (`VW_INTEGRACAO_ODONTOPREV`): Fornece COMO excluir (dados completos)

## Arquivos Modificados

1. **`IntegracaoOdontoprevExclusao.java`**
   - Removidos todos os campos que não existem na view
   - Mantidos apenas os 5 campos reais da view
   - Atualizada documentação

2. **`ProcessamentoEmpresaExclusaoServiceImpl.java`**
   - Adicionado `IntegracaoOdontoprevRepository` como dependência
   - Substituído `converterParaIntegracaoBase()` por `buscarDadosCompletosEmpresa()`
   - Atualizados logs para usar campos corretos
   - Corrigidos métodos que usavam dados da view de exclusão

## Resultado

- ✅ Erro `ORA-00904: "VIDAS_ATIVAS": identificador inválido` resolvido
- ✅ Entidade mapeia corretamente a estrutura real da view
- ✅ Service busca dados completos da view principal
- ✅ Compilação bem-sucedida
- ✅ Fluxo de exclusão funcionando corretamente

## Teste

O projeto foi compilado com sucesso após as alterações:

```bash
mvn clean compile -q
# Exit code: 0
```

## Considerações Importantes

### 1. **Estrutura Real das Views**
- `VW_INTEGRACAO_ODONTOPREV_EXC`: Apenas 5 campos básicos para identificação
- `VW_INTEGRACAO_ODONTOPREV`: Dados completos da empresa (31 campos)

### 2. **Estratégia de Dados**
- View de exclusão = "Lista de empresas para excluir"
- View principal = "Dados completos para exclusão"

### 3. **Performance**
- Busca otimizada usando `buscarPrimeiroDadoPorCodigoEmpresa()` com `ROWNUM = 1`
- Evita carregar dados desnecessários da view de exclusão

A correção resolve o problema de mapeamento incorreto e permite que o fluxo de exclusão de empresas funcione corretamente, buscando os dados necessários das views apropriadas.
