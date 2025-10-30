# Exemplo de JSON - Endpoint /plano/criar

Este documento mostra um exemplo de JSON que é enviado no fluxo de inclusão de planos através do endpoint `/empresa/2.0/plano/criar`.

## Fluxo de Inclusão de Empresas com Planos

O fluxo de inclusão de empresas inclui **automaticamente** a inclusão de planos no final do processo através do endpoint `/plano/criar`:

1. POST → Incluir empresa na API
2. Procedure → Cadastrar código da empresa
3. GET → Buscar dados da empresa na API
4. TBSYNC → Cadastrar sucesso na tabela de controle (tipo ADIÇÃO - 1)
5. PME → Cadastrar empresa no endpoint PME
6. **PLANOS → Criar planos via endpoint /plano/criar**
7. **TBSYNC → Cadastrar sucesso PLANOS na tabela de controle (tipo 4)**

## Dados dos Planos na View

Os planos são buscados da view `VW_INTEGRACAO_ODONTOPREV` com os seguintes campos:

- `CODIGO_PLANO_1`, `CODIGO_PLANO_2`, `CODIGO_PLANO_3`
- `VALOR_TITULAR_1`, `VALOR_TITULAR_2`, `VALOR_TITULAR_3`
- `VALOR_DEPENDENTE_1`, `VALOR_DEPENDENTE_2`, `VALOR_DEPENDENTE_3`
- `DATA_INICIO_PLANO_1`, `DATA_INICIO_PLANO_2`, `DATA_INICIO_PLANO_3`

## Exemplo de JSON Enviado ao Endpoint /plano/criar

```json
{
  "codigoGrupoGerencial": "",
  "sistema": "Sabin Sinai",
  "codigoUsuario": "0",
  "listaPlano": [
    {
      "valorTitular": 27.42,
      "codigoPlano": 9972,
      "dataInicioPlano": "2025-07-17T00:00:00.000Z",
      "valorDependente": 27.42,
      "valorReembolsoUO": 0.0,
      "percentualAgregadoRedeGenerica": 0.0,
      "percentualDependenteRedeGenerica": 0.0,
      "idSegmentacaoGrupoRede": 0,
      "idNomeFantasia": 0,
      "redes": [
        { "codigoRede": "1" },
        { "codigoRede": "31" },
        { "codigoRede": "32" },
        { "codigoRede": "33" },
        { "codigoRede": "35" },
        { "codigoRede": "36" },
        { "codigoRede": "37" },
        { "codigoRede": "38" }
      ],
      "percentualAssociado": 0.0,
      "planoFamiliar": "",
      "periodicidade": ""
    },
    {
      "valorTitular": 35.50,
      "codigoPlano": 9973,
      "dataInicioPlano": "2025-07-17T00:00:00.000Z",
      "valorDependente": 35.50,
      "valorReembolsoUO": 0.0,
      "percentualAgregadoRedeGenerica": 0.0,
      "percentualDependenteRedeGenerica": 0.0,
      "idSegmentacaoGrupoRede": 0,
      "idNomeFantasia": 0,
      "redes": [
        { "codigoRede": "1" },
        { "codigoRede": "31" },
        { "codigoRede": "32" },
        { "codigoRede": "33" },
        { "codigoRede": "35" },
        { "codigoRede": "36" },
        { "codigoRede": "37" },
        { "codigoRede": "38" }
      ],
      "percentualAssociado": 0.0,
      "planoFamiliar": "",
      "periodicidade": ""
    },
    {
      "valorTitular": 45.00,
      "codigoPlano": 9974,
      "dataInicioPlano": "2025-07-17T00:00:00.000Z",
      "valorDependente": 45.00,
      "valorReembolsoUO": 0.0,
      "percentualAgregadoRedeGenerica": 0.0,
      "percentualDependenteRedeGenerica": 0.0,
      "idSegmentacaoGrupoRede": 0,
      "idNomeFantasia": 0,
      "redes": [
        { "codigoRede": "1" },
        { "codigoRede": "31" },
        { "codigoRede": "32" },
        { "codigoRede": "33" },
        { "codigoRede": "35" },
        { "codigoRede": "36" },
        { "codigoRede": "37" },
        { "codigoRede": "38" }
      ],
      "percentualAssociado": 0.0,
      "planoFamiliar": "",
      "periodicidade": ""
    }
  ]
}
```

## Registro na TBSYNC

Após o sucesso do endpoint `/plano/criar`, um registro é criado na tabela `TBSYNC` com as seguintes características:

- **tipoControle**: `PLANOS` (valor 4)
- **statusSync**: `SUCCESS`
- **endpointDestino**: `POST_EMPRESA_PLANO_CRIAR`
- **dadosJson**: Contém o JSON completo enviado (mostrado acima)
- **responseApi**: Contém a resposta completa da API
- **tipoOperacao**: `CREATE`
- **codigoEmpresa**: Código da empresa processada
- **dataCriacao**: Data/hora de criação do registro
- **dataSucesso**: Data/hora do sucesso

### Exemplo de Registro TBSYNC

```sql
SELECT 
    ID, 
    CODIGO_EMPRESA, 
    TIPO_OPERACAO, 
    TIPO_CONTROLE, 
    ENDPOINT_DESTINO, 
    STATUS_SYNC, 
    DATA_CRIACAO, 
    DATA_SUCESSO
FROM TASY.TB_CONTROLE_SYNC_ODONTOPREV
WHERE TIPO_CONTROLE = 4
ORDER BY DATA_CRIACAO DESC;
```

Resultado esperado:
```
ID  | CODIGO_EMPRESA | TIPO_OPERACAO | TIPO_CONTROLE | ENDPOINT_DESTINO           | STATUS_SYNC | DATA_CRIACAO           | DATA_SUCESSO
123 | EMPRESA_001   | CREATE        | 4             | POST_EMPRESA_PLANO_CRIAR  | SUCCESS     | 2025-01-15 10:30:00    | 2025-01-15 10:30:05
```

## Logs Adicionados

O sistema agora gera logs detalhados em cada etapa do processo:

### 1. Dados da View
```
📋 [CRIAÇÃO PLANOS] Dados da view - CODIGO_PLANO_1=9972, CODIGO_PLANO_2=9973, CODIGO_PLANO_3=9974
📋 [CRIAÇÃO PLANOS] Valores - TITULAR_1=27.42, DEPENDENTE_1=27.42, ...
📋 [CRIAÇÃO PLANOS] Datas - INICIO_PLANO_1=2025-07-17, ...
```

### 2. Planos Criados
```
📋 [CRIAÇÃO PLANOS] PLANO 1: codigoPlano=9972, valorTitular=27.42, valorDependente=27.42, dataInicio=2025-07-17T00:00:00.000Z
📋 [CRIAÇÃO PLANOS] PLANO 2: codigoPlano=9973, valorTitular=35.50, valorDependente=35.50, dataInicio=2025-07-17T00:00:00.000Z
📋 [CRIAÇÃO PLANOS] PLANO 3: codigoPlano=9974, valorTitular=45.00, valorDependente=45.00, dataInicio=2025-07-17T00:00:00.000Z
```

### 3. JSON Completo
```
📤 [CRIAÇÃO PLANOS] JSON completo que será enviado:
{ ... JSON completo mostrado acima ... }
📤 [CRIAÇÃO PLANOS] Tamanho do JSON: 1234 caracteres
```

### 4. Endpoint
```
📤 [CRIAÇÃO PLANOS] Enviando request para endpoint /empresa/2.0/plano/criar
⏰ [CRIAÇÃO PLANOS] Chamada finalizada (duração: 850ms)
📄 [CRIAÇÃO PLANOS] Resposta da API: {"status": "success"}
✅ [CRIAÇÃO PLANOS] Planos criados com sucesso para empresa EMPRESA_001
```

### 5. Registro TBSYNC
```
💾 [CRIAÇÃO PLANOS] Registro TBSYNC será criado com:
   - tipoControle: PLANOS (4)
   - codigoEmpresa: EMPRESA_001
   - statusSync: SUCCESS
💾 [TBSYNC PLANOS] Sucesso! Registro PLANOS cadastrado na TBSYNC:
   - ID: 123
   - codigoEmpresa: EMPRESA_001
   - tipoControle: PLANOS (4)
   - status: SUCCESS
   - endpoint: POST_EMPRESA_PLANO_CRIAR
   - dataCriacao: 2025-01-15T10:30:00
   - dataSucesso: 2025-01-15T10:30:05
```

## Observações Importantes

1. **Múltiplos Planos**: O sistema suporta até 3 planos (CODIGO_PLANO_1, 2, 3)
2. **Planos Opcionais**: Se o código do plano estiver nulo na view, ele não será incluído no request
3. **Redes**: Todos os planos recebem as mesmas 8 redes padrão (1, 31, 32, 33, 35, 36, 37, 38)
4. **TBSYNC**: Um registro é criado APENAS em caso de sucesso do endpoint /plano/criar
5. **Tipo de Controle**: Sempre será tipo 4 (PLANOS) para este endpoint
6. **Endpoint**: POST https://apim-hml.odontoprev.com.br/empresa/2.0/plano/criar
