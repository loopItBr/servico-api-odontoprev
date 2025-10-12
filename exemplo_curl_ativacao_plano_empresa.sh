#!/bin/bash

# EXEMPLO DE CURL PARA ATIVAÇÃO DO PLANO DA EMPRESA
# Este script demonstra como a API de ativação do plano é chamada automaticamente
# após o cadastro bem-sucedido da sincronização da empresa.

echo "🚀 Exemplo de cURL para Ativação do Plano da Empresa"
echo "=================================================="
echo ""

# URL da API de ativação do plano
URL="https://apim-hml.odontoprev.com.br/empresa/2.0/empresas/contrato/empresarial"

# Token OAuth2 (substitua pelo token real)
TOKEN_OAUTH2="Bearer eyJ4NXQiOiJNek5rWmpVM1ltWmhaRGRpTkRabVpHVTJabVJsT1RoaE9XVXpOV0UzTWpRNFpERmpOV1k0TXciLCJraWQiOiJPREJtTVRVMFpqSmpPREprTkdZMVpUaG1ZamsyWVRZek56UmpZekl6TVRCbFlqRTBNV0prWTJJeE5qZzNPRGRqWVdRNVpXWmhOV0kwTkRBM1pqTTROUV9SUzI1NiIsInR5cCI6ImF0K2p3dCIsImFsZyI6IlJTMjU2In0.eyJzdWIiOiI0NmI3ZDIwZC0yOGNiLTQ4OTItYWFkYS02MTQwYWE4M2JiZjQiLCJhdXQiOiJBUFBMSUNBVElPTiIsImF1ZCI6Il9iZVFmZDFuT05xOVlqcmFWcGZJeTdidHlYSWEiLCJuYmYiOjE3NjAwMzM4MTQsImF6cCI6Il9iZVFmZDFuT05xOVlqcmFWcGZJeTdidHlYSWEiLCJzY29wZSI6ImRlZmF1bHQiLCJpc3MiOiJodHRwczovL2FwaW0taG1sLm9kb250b3ByZXYuY29tLmJyL29hdXRoMi90b2tlbiIsImV4cCI6MTc2MDAzNzQxNCwiaWF0IjoxNzYwMDMzODE0LCJqdGkiOiI2NmI3YTBjYy01NWU4LTQxNjAtYmUyZS05ZDMyYjNmOWE5YWYiLCJjbGllbnRfaWQiOiJfYmVRZmQxbk9OcTlZanJhVnBmSXk3YnR5WElhIn0.Z6-urGXjzNWYRCi8vO2-Z3lkUjbzv6n6lhpWJj0KVz4cEocOF0hnnFRaRgqqqTEyRob9QcOBmQTgJX5IHZNR1xMOtWzYOAhJC5YuqNw7GtlTB-NIosQGOMrT5OyuRLkFhG7xdfjiIKpM70hWAtWgf41viHLfm-beNi4wKk4URHLoPXFLmvz62M9sbPjMeT9h0RV9yDHWc1bkI-pcYwmFNQrS4vLpiV72hGlVw1Au1V3LlT9jZWpQYJF-Zt6Y4prm27W8bGwmp3jHwjWVkum8Is3D9GTxXZNZxe09KVDcCbpWOgdGA_-wqIJbPdD6t-VD8Ro-sSLsMRjrCeJqH4qHQQ"

echo "📡 Fazendo chamada para: $URL"
echo "🔑 Token: ${TOKEN_OAUTH2:0:50}..."
echo ""

# Executar o cURL
curl --location --request POST "$URL" \
--header "Content-Type: application/json" \
--header "Authorization: $TOKEN_OAUTH2" \
--data-raw '{
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
    "cgc": "01.648.339/0001-61",
    "razaoSocial": "VITA ASSISTENCIA A SAUDE LTDA",
    "nomeFantasia": "SABIN SINAI",
    "diaInicioFaturamento": 20,
    "codigoUsuarioConsultor": "FEODPV01583",
    "mesAniversarioReajuste": 7,
    "dataInicioContrato": "2025-07-17T03:00:00.000",
    "dataVigencia": "2025-07-17T03:00:00.000",
    "descricaoRamoAtividade": "Saúde Suplementar",
    "planos": [
        {
            "codigoPlano": "9972",
            "dataInicioPlano": "2025-01-01T03:00:00.000",
            "valorDependente": 27.42,
            "valorReembolsoUO": 0,
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
        },
        {
            "codigoPlano": "9973",
            "dataInicioPlano": "2025-01-01T03:00:00.000",
            "valorDependente": 27.42,
            "periodicidade": "N",
            "percentualAssociado": 0.0,
            "percentualDependenteRedeGenerica": 0.0,
            "percentualAgregadoRedeGenerica": 0.0,
            "valorReembolsoUO": 0,
            "valorTitular": 27.42,
            "redes": [
                {
                    "codigoRede": "1"
                }
            ]
        },
        {
            "codigoPlano": "9974",
            "dataInicioPlano": "2025-01-01T03:00:00.000",
            "valorDependente": 27.42,
            "valorReembolsoUO": 0,
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
    "diaVencimento": 15,
    "cnae": "6550-2/00",
    "codigoManual": "1 ",
    "diaLimiteConsumoAg": 19,
    "grausParentesco": [
        {
            "codigoGrauParentesco": "1"
        },
        {
            "codigoGrauParentesco": "2"
        },
        {
            "codigoGrauParentesco": "3"
        },
        {
            "codigoGrauParentesco": "11"
        },
        {
            "codigoGrauParentesco": "17"
        },
        {
            "codigoGrauParentesco": "18"
        },
        {
            "codigoGrauParentesco": "42"
        }
    ],
    "contatosDaFatura": [
        {
            "codSequencial": 1,
            "email": "diretoria@sabinjf.com.br",
            "nomeContato": "Célio Carneiro Chagas",
            "relatorio": false
        }
    ],
    "email": "diretoria@sabinjf.com.br",
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
        "nome": "VITA ASSISTENCIA A SAUDE LTDA",
        "cgc": "01.648.339/0001-61",
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
    },
    "diaMovAssociadoEmpresa": 15,
    "contatos": [
        {
            "cargo": "Diretor Adjunto de Novos Projetos",
            "nome": "Gustavo de Moraes Ramalho",
            "email": "gustavoramalho@hospitalmontesinai.com.br",
            "idCorretor": "N",
            "telefone": {
                "telefone1": "(11) 1111-1111",
                "celular": "(11) 11111-1111"
            },
            "listaTipoComunicacao": [
                {
                    "id": "1",
                    "descricao": "E-mail"
                }
            ]
        }
    ],
    "comissionamentos": [
        {
            "cnpjCorretor": "27833136000139",
            "codigoRegra": 1,
            "numeroParcelaDe": 1,
            "numeroParcelaAte": 999,
            "porcentagem": 10
        }
    ],
    "grupos": [
        {
            "codigoGrupo": 268
        }
    ]
}'

echo ""
echo "✅ Chamada concluída!"
echo ""
echo "📋 INFORMAÇÕES IMPORTANTES:"
echo "- Esta chamada é feita AUTOMATICAMENTE após o cadastro bem-sucedido da empresa"
echo "- O sistema busca os dados da empresa na view VW_INTEGRACAO_ODONTOPREV"
echo "- Valores padrão são usados para campos não disponíveis na view"
echo "- Registro de controle é criado na tabela TB_CONTROLE_SYNC_ODONTOPREV (tipo_controle = 3)"
echo "- Logs detalhados são gerados para auditoria"
