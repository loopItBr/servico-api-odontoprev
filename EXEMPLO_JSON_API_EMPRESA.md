# 📋 EXEMPLO JSON - API EMPRESA ODONTOPREV

## 🎯 **JSON de Exemplo Enviado para POST /empresa/2.0/empresas/contrato/empresarial**

Baseado no método `converterParaRequestEmpresarial` do `EmpresaInclusaoServiceImpl.java`:

```json
{
  "sistema": "SabinSinai",
  "tipoPessoa": "J",
  "emiteCarteirinhaPlastica": "N",
  "codigoEmpresaGestora": 1,
  "codigoFilialEmpresaGestora": 1,
  "codigoGrupoGerencial": "787392",
  "codigoNaturezaJuridica": "6550-2",
  "nomeNaturezaJuridica": "Planos de saúde",
  "situacaoCadastral": "ATIVO",
  "inscricaoMunicipal": "997.179.737.204",
  "inscricaoEstadual": "997.179.737.204",
  "dataConstituicao": "2025-10-01T00:00:00.000Z",
  "renovacaoAutomatica": "S",
  "codigoClausulaReajusteDiferenciado": "1",
  "departamento": "SEM DEPARTAMENTO",
  "dependentePaga": "N",
  "permissaoCadastroDep": true,
  "modeloCobrancaVarejo": false,
  "numeroMinimoAssociados": 3,
  "numeroFuncionarios": 0,
  "numeroDepedentes": 0,
  "idadeLimiteDependente": 21,
  "valorFator": 1,
  "tipoRetornoCritica": "T",
  "codigoLayoutCarteirinha": "B",
  "codigoOrdemCarteira": 3,
  "codigoDocumentoContrato": 0,
  "codigoCelula": 9,
  "codigoMarca": 1,
  "codigoDescricaoNF": 0,
  "diaVencimentoAg": 19,
  "codigoPerfilClienteFatura": 3,
  "codigoBancoFatura": "085 ",
  "multaFatura": 0,
  "descontaIR": "N",
  "retencaoIss": "N",
  "liberaSenhaInternet": "S",
  "faturamentoNotaCorte": "N",
  "proRata": "N",
  "custoFamiliar": "S",
  "planoFamiliar": "S",
  "percSinistroContrato": 60,
  "idadeLimiteUniversitaria": 24,
  "percentualINSSAutoGestao": 0,
  "percentualMateriaisAutoGestao": 0,
  "valorSinistroContrato": 60.0,
  "percentualAssociado": 0,
  "codigoRegiao": 0,
  "codigoImagemFatura": 1,
  "codigoMoeda": "7",
  "codigoParceriaEstrategica": 0,
  "sinistralidade": 60,
  "posicaoIniTIT": 1,
  "posicaoFimTIT": 7,
  "regraDowngrade": 0,
  "mesCompetenciaProximoFaturamento": "09",
  "codigoUsuarioFaturamento": "",
  "codigoUsuarioCadastro": "",
  "ramo": "Massificado",
  "cgc": "39.872.617/0001-32",
  "razaoSocial": "MAGIAFOTOSTUDIO",
  "nomeFantasia": "MAGIAFOTOSTUDIO",
  "diaInicioFaturamento": 20,
  "codigoUsuarioConsultor": "FEODPV01583",
  "mesAniversarioReajuste": 7,
  "dataInicioContrato": "2025-07-17T03:00:00.000",
  "dataVigencia": "2025-07-17T03:00:00.000",
  "descricaoRamoAtividade": "Saúde Suplementar",
  "diaVencimento": 15,
  "cnae": "6550-2/00",
  "codigoManual": "1 ",
  "diaLimiteConsumoAg": 19,
  "email": "diretoria@sabinjf.com.br",
  "diaMovAssociadoEmpresa": 15,
  "planos": [
    {
      "codigoPlano": "9972",
      "dataInicioPlano": "2025-01-01T03:00:00.000",
      "valorDependente": 27.42,
      "valorReembolsoUO": 0.0,
      "valorTitular": 27.42,
      "periodicidade": "N",
      "percentualAssociado": 0.0,
      "percentualDependenteRedeGenerica": 0.0,
      "percentualAgregadoRedeGenerica": 0.0,
      "redes": [
        {
          "codigoRede": "1"
        }
      ]
    }
  ],
  "grausParentesco": [
    {
      "codigoGrauParentesco": "1"
    }
  ],
  "grupos": [
    {
      "codigoGrupo": 1
    }
  ],
  "contatos": [
    {
      "cargo": "Gerente",
      "nome": "Contato Principal",
      "email": "contato@empresa.com",
      "idCorretor": "N",
      "telefone": {
        "telefone1": "(32) 99999-9999",
        "celular": "(32) 99999-9999"
      },
      "listaTipoComunicacao": [
        {
          "id": "1",
          "descricao": "E-mail"
        }
      ]
    }
  ],
  "contatosDaFatura": [
    {
      "codSequencial": 1,
      "email": "fatura@empresa.com",
      "nomeContato": "Contato Fatura",
      "relatorio": true
    }
  ],
  "endereco": {
    "cep": "36033318",
    "descricao": "Av. Presidente Itamar Franco",
    "complemento": "loja 202 E",
    "tipoLogradouro": "2",
    "logradouro": "Av. Presidente Itamar Franco",
    "numero": "4001",
    "bairro": "Cascatinha",
    "cidade": {
      "codigo": 3670,
      "nome": "Juiz de Fora",
      "siglaUf": "MG",
      "codigoPais": 1
    }
  },
  "cobranca": {
    "nome": "MAGIAFOTOSTUDIO",
    "cgc": "39.872.617/0001-32",
    "endereco": {
      "cep": "36033318",
      "descricao": "Av. Presidente Itamar Franco",
      "complemento": "loja 202 E",
      "tipoLogradouro": "2",
      "logradouro": "Av. Presidente Itamar Franco",
      "numero": "4001",
      "bairro": "Cascatinha",
      "cidade": {
        "codigo": 3670,
        "nome": "Juiz de Fora",
        "siglaUf": "MG",
        "codigoPais": 1
      }
    }
  }
}
```

## 📊 **Campos Dinâmicos da View**

Os seguintes campos são preenchidos dinamicamente com dados da view `VW_INTEGRACAO_ODONTOPREV`:

- **`cgc`**: `dadosEmpresa.getCnpj()` (ex: "39.872.617/0001-32")
- **`razaoSocial`**: `dadosEmpresa.getNomeFantasia()` (ex: "MAGIAFOTOSTUDIO")
- **`nomeFantasia`**: `dadosEmpresa.getNomeFantasia()` (ex: "MAGIAFOTOSTUDIO")
- **`cobranca.nome`**: `dadosEmpresa.getNomeFantasia()` (ex: "MAGIAFOTOSTUDIO")
- **`cobranca.cgc`**: `dadosEmpresa.getCnpj()` (ex: "39.872.617/0001-32")

## 🔧 **Campos Obrigatórios Adicionados**

Para resolver o erro `400 Bad Request`, foram adicionados os campos obrigatórios:

- **`grupos`**: Lista com código do grupo
- **`contatos`**: Lista com dados do contato principal
- **`contatosDaFatura`**: Lista com dados do contato da fatura

## 🎯 **Endpoint e Headers**

```http
POST https://apim-hml.odontoprev.com.br/empresa/2.0/empresas/contrato/empresarial
Content-Type: application/json
Authorization: Bearer {TOKEN_OAUTH2}
```

## 📝 **Resposta Esperada**

```json
{
  "codigoEmpresa": "ABC123",
  "senha": "SENHA456"
}
```

Este JSON é gerado automaticamente pelo método `converterParaRequestEmpresarial()` baseado nos dados da view `VW_INTEGRACAO_ODONTOPREV`.
