# Correção do Token de Autenticação na Ativação de Plano

## Problema Identificado

O serviço `AtivacaoPlanoEmpresaServiceImpl` estava usando um token hardcoded `"Bearer TOKEN_OAUTH2"` em vez de obter o token real da OdontoPrev, causando erro `[401 Unauthorized]` na API de ativação do plano.

## Logs do Erro

```
2025-10-12T10:46:22.914-03:00 ERROR 36739 --- [servico-api-odontoprev] [ontoPrev-Task-5] .o.i.c.s.AtivacaoPlanoEmpresaServiceImpl : ❌ [ATIVAÇÃO PLANO] Erro ao ativar plano para empresa 008772: [401 Unauthorized] during [POST] to [https://apim-hml.odontoprev.com.br/empresa/2.0/empresas/contrato/empresarial] [BeneficiarioOdontoprevFeignClient#ativarPlanoEmpresa(String,EmpresaAtivacaoPlanoRequest)]: []
```

## Solução Implementada

### 1. Adicionado Import do TokenService

```java
import com.odontoPrev.odontoPrev.infrastructure.client.domain.service.TokenService;
```

### 2. Injetado TokenService como Dependência

```java
private final TokenService tokenService;
```

### 3. Implementada Obtenção Correta do Token

**ANTES:**
```java
EmpresaAtivacaoPlanoResponse response = feignClient.ativarPlanoEmpresa(
    "Bearer TOKEN_OAUTH2", // TODO: Implementar obtenção do token
    request
);
```

**DEPOIS:**
```java
// Etapa 3: Obter token de autenticação
log.info("🔑 [ATIVAÇÃO PLANO] Obtendo token de autenticação para empresa: {}", codigoEmpresa);
String token = tokenService.obterTokenValido();
String authorization = "Bearer " + token;
log.info("🔑 [ATIVAÇÃO PLANO] Token obtido com sucesso para empresa: {}", codigoEmpresa);

// Etapa 4: Chamar API da OdontoPrev
log.info("📡 [ATIVAÇÃO PLANO] Chamando API de ativação para empresa: {}", codigoEmpresa);
EmpresaAtivacaoPlanoResponse response = feignClient.ativarPlanoEmpresa(
    authorization,
    request
);
```

## Como Funciona o TokenService

O `TokenService` é o mesmo usado pelo `ConsultaEmpresaOdontoprevServiceImpl` e funciona da seguinte forma:

1. **Verifica se o token está expirado** (com margem de 5 minutos)
2. **Se expirado, renova automaticamente** chamando a API de autenticação
3. **Retorna o token válido** para uso nas requisições

### Métodos Principais:
- `obterTokenValido()`: Retorna token válido (renova se necessário)
- `tokenExpirado()`: Verifica se token precisa ser renovado
- `renovarToken()`: Obtém novo token da API de autenticação

## Benefícios da Correção

✅ **Autenticação Correta**: Agora usa o mesmo sistema de tokens das outras APIs  
✅ **Renovação Automática**: Token é renovado automaticamente quando expira  
✅ **Logs Detalhados**: Adicionados logs para rastrear obtenção do token  
✅ **Consistência**: Usa o mesmo padrão dos outros serviços  
✅ **Tratamento de Erros**: Herda o tratamento de erros do TokenService  

## Status

✅ **Compilação**: Bem-sucedida  
✅ **Linter**: Sem erros  
✅ **Correção**: Implementada  

## Próximos Passos

1. Testar a aplicação para verificar se o erro 401 foi resolvido
2. Monitorar logs para confirmar que o token está sendo obtido corretamente
3. Verificar se a ativação do plano está funcionando sem erros de autenticação

## Observações

- O `TokenService` já estava sendo usado com sucesso em outros serviços
- A correção mantém a mesma lógica de autenticação já testada
- Os logs adicionados facilitam o debug de problemas futuros
