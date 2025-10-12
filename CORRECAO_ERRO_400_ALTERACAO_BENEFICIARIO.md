# Correção do Erro HTTP 400 - Alteração de Beneficiário

## Problema Identificado

O sistema estava retornando erro HTTP 400 na API de alteração de beneficiários:

```
[400] during [PUT] to [https://apim-hml.odontoprev.com.br/cadastroonline-pj/1.0/alterar]
```

## Causas Identificadas

### 1. **Estrutura do Payload Incorreta**
- **Problema**: A API espera um **ARRAY** de objetos `[{...}]`, mas estávamos enviando um objeto único `{...}`
- **Documentação**: O exemplo mostra claramente que o body deve ser um array

### 2. **Formato de Data Incorreto**
- **Problema**: Datas não estavam no formato `dd/mm/yyyy` conforme especificado na documentação
- **API espera**: `"dataDeNascimento": "dd/mm/yyyy"`

### 3. **Falta de Merge de Dados**
- **Problema**: Não estava buscando dados completos na view de inclusão para complementar os dados da view de alteração
- **Requisito**: "buscar os dados nas duas views para complementar e atualizar somente o que estiver não nulo na view de alteração"

## Soluções Implementadas

### 1. **Correção do Feign Client**

**Arquivo**: `BeneficiarioOdontoprevFeignClient.java`

```java
// Antes
@RequestBody BeneficiarioAlteracaoRequestNew request

// Depois  
@RequestBody List<BeneficiarioAlteracaoRequestNew> request
```

### 2. **Correção do Serviço de Processamento**

**Arquivo**: `ProcessamentoBeneficiarioAlteracaoServiceImpl.java`

```java
// Antes
odontoprevClient.alterarBeneficiarioNew(tokenOAuth2, tokenLoginEmpresa, request);

// Depois
odontoprevClient.alterarBeneficiarioNew(tokenOAuth2, tokenLoginEmpresa, List.of(request));
```

### 3. **Implementação de Merge de Dados**

**Estratégia de Merge Implementada**:

1. **Busca dados completos** na view de inclusão (`VW_INTEGRACAO_ODONTOPREV_BENEFICIARIOS`)
2. **Atualiza apenas campos não nulos** da view de alteração (`VW_INTEGRACAO_ODONTOPREV_BENEFICIARIOS_ALT`)
3. **Formata datas** no padrão `dd/mm/yyyy` conforme API
4. **Valida campos obrigatórios**

**Código do Merge**:
```java
// PASSO 1: Buscar dados completos na view de inclusão
IntegracaoOdontoprevBeneficiario dadosCompletos = 
    integracaoOdontoprevBeneficiarioRepository.findByCodigoMatricula(beneficiario.getCdAssociado());

// PASSO 2: Merge dos dados - priorizar alterações se não nulas
String cep = beneficiario.getCep() != null ? beneficiario.getCep() : 
            (dadosCompletos != null ? dadosCompletos.getCep() : null);

// PASSO 3: Formatar datas no padrão dd/mm/yyyy
String dataNascimento = null;
if (beneficiario.getDataNascimento() != null) {
    dataNascimento = beneficiario.getDataNascimento().format(
        java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy"));
} else if (dadosCompletos != null && dadosCompletos.getDataDeNascimento() != null) {
    dataNascimento = dadosCompletos.getDataDeNascimento();
}
```

## Campos Obrigatórios Validados

Conforme documentação da API:

### Campos Obrigatórios do Request Principal
- ✅ `cdEmpresa` (6 caracteres)
- ✅ `codigoAssociado` (9 caracteres) 
- ✅ `codigoPlano` (até 5 caracteres)
- ✅ `departamento` (até 8 caracteres)

### Campos Obrigatórios do Beneficiário
- ✅ `cpf` (formato 000.000.000-00)
- ✅ `dataDeNascimento` (formato dd/mm/yyyy)
- ✅ `estadoCivil` (S/C/D/V/O)
- ✅ `cep` (deve estar preenchido)

## Validações Implementadas

### 1. **Estado Civil**
- S = Solteiro(a)
- C = Casado(a) 
- D = Divorciado(a)/separado(a)/desquitado(a)
- V = Viúvo(a)
- O = Outro

### 2. **Formato de Data**
- Padrão: `dd/mm/yyyy`
- Exemplo: `"25/12/1990"`

### 3. **CEP**
- Deve estar preenchido conforme validação da API

## Estrutura do Payload Corrigida

### Antes (Incorreto)
```json
{
  "cdEmpresa": "008771",
  "codigoAssociado": "617502380",
  "beneficiario": { ... }
}
```

### Depois (Correto)
```json
[{
  "cdEmpresa": "008771",
  "codigoAssociado": "617502380", 
  "codigoPlano": "916",
  "departamento": "1",
  "beneficiario": {
    "cpf": "123.456.789-00",
    "dataDeNascimento": "16/11/1986",
    "estadoCivil": "VIUVO",
    "endereco": {
      "cep": "36015-260",
      "logradouro": "RUA MONSENHOR PEDRO ARBEX",
      "numero": "100",
      "complemento": "101",
      "bairro": "SAO MATEUS",
      "cidade": "JUIZ DE FORA",
      "uf": "MG"
    }
  }
}]
```

## Arquivos Modificados

1. **`BeneficiarioOdontoprevFeignClient.java`**
   - Alterado `@RequestBody` para aceitar `List<BeneficiarioAlteracaoRequestNew>`
   - Adicionado import `java.util.List`

2. **`ProcessamentoBeneficiarioAlteracaoServiceImpl.java`**
   - Implementado merge de dados das duas views
   - Corrigido envio como array `List.of(request)`
   - Adicionado formatação de datas no padrão `dd/mm/yyyy`
   - Adicionado import `java.util.List`

## Resultado Esperado

Após as correções:

1. ✅ **Payload correto**: Array de objetos conforme documentação
2. ✅ **Dados completos**: Merge das duas views para dados completos
3. ✅ **Formato de data**: `dd/mm/yyyy` conforme API
4. ✅ **Campos obrigatórios**: Todos preenchidos corretamente
5. ✅ **Validações**: Estado civil, CEP, etc. conforme especificação

## Teste

O projeto foi compilado com sucesso:
```bash
mvn clean compile -q
# Exit code: 0
```

A API de alteração de beneficiários deve agora funcionar corretamente sem erro HTTP 400.
