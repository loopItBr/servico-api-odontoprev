# Diagnóstico do Erro HTTP 403 - Inclusão de Beneficiário

## Problema Identificado

O sistema está retornando erro HTTP 403 (Forbidden) na API de inclusão de beneficiários:

```json
{
  "status": 403,
  "timestamp": "2025-10-12T13:53:36.35",
  "type": "/regra-de-seguranca",
  "title": "Regra de segurança", 
  "detail": "A usuário não possui acesso aos dados informados",
  "userMessage": "A usuário não possui acesso aos dados informados"
}
```

## Análise do Erro

### 🔍 **Tipo de Erro**: Regra de Segurança
- **Código**: 403 Forbidden
- **Categoria**: `/regra-de-seguranca`
- **Mensagem**: "A usuário não possui acesso aos dados informados"

### 🎯 **Possíveis Causas**

#### 1. **Inconsistência no Código da Empresa**
- O **token de login empresa** pode estar associado a uma empresa diferente
- O **código da empresa** no payload pode estar incorreto
- **Mismatch** entre empresa do token e empresa do payload

#### 2. **Problema de Autenticação Dupla**
- **Token OAuth2** pode estar válido, mas **token de login empresa** inválido
- **Credenciais de login empresa** podem estar incorretas
- **AppId** pode não ter permissão para a empresa especificada

#### 3. **Dados do Beneficiário**
- **Código da matrícula** pode não pertencer à empresa especificada
- **Departamento** pode não existir na empresa
- **Plano** pode não estar disponível para a empresa

## Logs de Debug Implementados

### 🔍 **Logs de Payload**
```java
log.info("🔍 DEBUG PAYLOAD - Beneficiário {}: CódigoEmpresa: '{}', Usuario: '{}', CodigoMatricula: '{}'", 
         codigoMatricula, 
         request.getVenda().getCodigoEmpresa(),
         request.getUsuario(),
         request.getBeneficiarioTitular().getBeneficiario().getCodigoMatricula());
```

### 🔑 **Logs de Tokens**
```java
log.info("🔑 DEBUG TOKENS - Beneficiário {}: OAuth2: {}..., LoginEmpresa: {}...", 
         codigoMatricula,
         tokenOAuth2.substring(0, 30),
         tokenLoginEmpresa.substring(0, 30));
```

### 🏢 **Logs de Empresa**
```java
log.info("🔍 DEBUG EMPRESA - CódigoEmpresa da view: '{}' (tamanho: {})", 
         codigoEmpresa, codigoEmpresa.length());
```

### 🔐 **Logs de Credenciais**
```java
log.info("🔑 [TOKEN LOGIN EMPRESA] AppId: '{}', Usuario: '{}', Senha: [OCULTA]", appId, usuario);
```

## Configurações Atuais

### 📋 **Credenciais de Login Empresa**
```yaml
odontoprev:
  api:
    login:
      app-id: ODPV
      usuario: 13433638
      senha: gWZ84t1NCX3a
```

### 🏢 **Código da Empresa**
```yaml
odontoprev:
  api:
    empresa: 787392
```

## Próximos Passos para Diagnóstico

### 1. **Verificar Logs de Debug**
Executar o sistema e verificar os logs para identificar:
- Qual **código da empresa** está sendo enviado no payload
- Se o **token de login empresa** está sendo obtido corretamente
- Se há **inconsistência** entre empresa do token e payload

### 2. **Validar Credenciais**
- Verificar se o **usuário 13433638** tem acesso à **empresa 787392**
- Confirmar se o **AppId ODPV** tem permissão para esta empresa
- Testar as credenciais manualmente via cURL

### 3. **Verificar Dados do Beneficiário**
- Confirmar se o **código da matrícula** pertence à empresa especificada
- Validar se o **departamento** existe na empresa
- Verificar se o **plano** está disponível para a empresa

## Exemplo de cURL para Teste Manual

```bash
# 1. Obter token OAuth2
curl -X POST "https://apim-hml.odontoprev.com.br/oauth2/token" \
  -H "Authorization: Basic X2JlUWZkMW5PTnE5WWpyYVZwZkl5N2J0eVhJYTpCV25wMURLMlJkeXowYXNBVXU2QkFkSUdnM01h" \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "grant_type=client_credentials"

# 2. Obter token de login empresa
curl -X POST "https://apim-hml.odontoprev.com.br/empresa-login/1.0/api/auth/token" \
  -H "Authorization: Bearer {TOKEN_OAUTH2}" \
  -H "Content-Type: application/json" \
  -d '{
    "appId": "ODPV",
    "login": "13433638", 
    "senha": "gWZ84t1NCX3a"
  }'

# 3. Testar inclusão de beneficiário
curl -X POST "https://apim-hml.odontoprev.com.br/cadastroonline-pj/1.0/incluir" \
  -H "Authorization: Bearer {TOKEN_OAUTH2}" \
  -H "AuthorizationOdonto: Bearer {TOKEN_LOGIN_EMPRESA}" \
  -H "Content-Type: application/json" \
  -d '{
    "beneficiarioTitular": {
      "beneficiario": {
        "codigoMatricula": "TESTE001",
        "cpf": "123.456.789-00",
        "nomeBeneficiario": "TESTE BENEFICIARIO"
      }
    },
    "usuario": "13433638",
    "venda": {
      "codigoEmpresa": "787392",
      "codigoPlano": "916",
      "departamento": "1"
    }
  }'
```

## Soluções Possíveis

### 🔧 **Solução 1: Corrigir Código da Empresa**
Se o código da empresa no payload estiver incorreto:
```java
// Verificar se beneficiario.getCodigoEmpresa() retorna o valor correto
String codigoEmpresa = beneficiario.getCodigoEmpresa();
if (!"787392".equals(codigoEmpresa)) {
    log.error("Código da empresa incorreto: {} (esperado: 787392)", codigoEmpresa);
}
```

### 🔧 **Solução 2: Validar Credenciais**
Se as credenciais estiverem incorretas:
```yaml
# Verificar se as credenciais estão corretas no application.yml
odontoprev:
  api:
    login:
      app-id: ODPV
      usuario: 13433638  # Verificar se este usuário tem acesso à empresa
      senha: gWZ84t1NCX3a
```

### 🔧 **Solução 3: Verificar Permissões**
Se o usuário não tiver permissão:
- Contatar suporte OdontoPrev para verificar permissões
- Confirmar se o usuário tem acesso à empresa 787392
- Verificar se o AppId ODPV tem permissão para esta empresa

## Status

✅ **Logs de debug implementados**
✅ **Projeto compilado com sucesso**
⏳ **Aguardando execução para análise dos logs**
⏳ **Investigação das credenciais em andamento**

## Arquivos Modificados

1. **`ProcessamentoBeneficiarioServiceImpl.java`**
   - Adicionados logs de debug para payload, tokens e empresa
   - Melhor rastreabilidade para identificar a causa do erro 403

2. **`BeneficiarioTokenService.java`**
   - Adicionados logs detalhados das credenciais de login empresa
   - Melhor visibilidade do processo de autenticação
