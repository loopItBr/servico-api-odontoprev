# Atualização do Campo Matrícula - Beneficiários

## Problema Identificado

O campo `codigoMatricula` estava sendo enviado como `null` na API de alteração de beneficiários, conforme mostrado no log:

```json
"codigoMatricula": null
```

## Causa do Problema

1. **Mapper incorreto**: O mapper estava mapeando `cdAssociado` para `codigoMatricula`
2. **Campo ausente**: A view de alteração não tinha o campo `MATRICULA` mapeado na entidade
3. **Dados nulos**: O `cdAssociado` pode estar `null` na view de alteração

## Solução Implementada

### 1. Adicionado Campo `MATRICULA` na Entidade de Alteração

**Arquivo**: `IntegracaoOdontoprevBeneficiarioAlteracao.java`

```java
/**
 * CÓDIGO DA MATRÍCULA DO FUNCIONÁRIO
 */
@Column(name = "MATRICULA", length = 7)
private String matricula;
```

### 2. Atualizado Mapper para Usar Campo Correto

**Arquivo**: `BeneficiarioViewMapper.java`

```java
// Antes
@Mapping(target = "codigoMatricula", source = "cdAssociado") // Usar cdAssociado como matrícula

// Depois
@Mapping(target = "codigoMatricula", source = "matricula") // Usar campo matricula da view
```

## Estrutura da View Atualizada

### VW_INTEGRACAO_ODONTOPREV_BENEFICIARIOS_ALT
A view agora deve conter o campo `MATRICULA` com as seguintes características:
- **Nome**: `MATRICULA`
- **Tipo**: `VARCHAR2(7)`
- **Função**: Código da matrícula do funcionário

### VW_INTEGRACAO_ODONTOPREV_BENEFICIARIOS_EXC
A view de exclusão já possui o campo `CODIGOMATRICULA` que está corretamente mapeado:
- **Nome**: `CODIGOMATRICULA`
- **Tipo**: `VARCHAR2(7)`
- **Mapeado como**: `codigoMatricula`

## Fluxo de Dados Corrigido

### Alteração de Beneficiários
1. **View**: `VW_INTEGRACAO_ODONTOPREV_BENEFICIARIOS_ALT` → Campo `MATRICULA`
2. **Entidade**: `IntegracaoOdontoprevBeneficiarioAlteracao` → Campo `matricula`
3. **Mapper**: `BeneficiarioViewMapper.fromAlteracaoView()` → Mapeia `matricula` para `codigoMatricula`
4. **DTO**: `BeneficiarioAlteracaoRequestNew.Beneficiario` → Campo `codigoMatricula`
5. **API**: Enviado para OdontoPrev com valor correto

### Exclusão de Beneficiários
1. **View**: `VW_INTEGRACAO_ODONTOPREV_BENEFICIARIOS_EXC` → Campo `CODIGOMATRICULA`
2. **Entidade**: `IntegracaoOdontoprevBeneficiarioExclusao` → Campo `codigoMatricula`
3. **Mapper**: `BeneficiarioViewMapper.fromExclusaoView()` → Mapeia `codigoMatricula` para `codigoMatricula`
4. **DTO**: `EmpresarialModelInativacao.AssociadoInativacao` → Campo `cdMatricula`
5. **API**: Enviado para OdontoPrev com valor correto

## Validação

### Campos Obrigatórios para Alteração
- ✅ `codigoMatricula` (agora preenchido corretamente)
- ✅ `cdAssociado` (código do associado na OdontoPrev)
- ✅ `codigoEmpresa` (código da empresa)
- ✅ `codigoPlano` (código do plano)
- ✅ `departamento` (departamento)

### Campos Obrigatórios para Exclusão
- ✅ `codigoMatricula` (já estava correto)
- ✅ `cdAssociado` (código do associado na OdontoPrev)
- ✅ `nomeBeneficiario` (nome do beneficiário)
- ✅ `idMotivoInativacao` (motivo da inativação)

## Resultado Esperado

Após as correções, o campo `codigoMatricula` deve ser enviado com o valor correto da matrícula do funcionário, resolvendo o erro HTTP 400 da API da OdontoPrev.

## Arquivos Modificados

1. **`IntegracaoOdontoprevBeneficiarioAlteracao.java`**
   - Adicionado campo `matricula` mapeado para `MATRICULA`

2. **`BeneficiarioViewMapper.java`**
   - Atualizado mapeamento para usar `matricula` em vez de `cdAssociado`

## Compilação

✅ Projeto compilado com sucesso após as alterações:
```bash
mvn clean compile -q
# Exit code: 0
```

## Próximos Passos

1. **Testar**: Executar o sistema e verificar se o campo `codigoMatricula` está sendo enviado corretamente
2. **Validar**: Confirmar que a API da OdontoPrev aceita as requisições de alteração
3. **Monitorar**: Acompanhar os logs para garantir que não há mais erros HTTP 400
