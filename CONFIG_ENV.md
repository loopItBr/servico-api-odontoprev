# Guia de Configuração de Ambientes

## 📋 Visão Geral

Este projeto utiliza profiles do Spring Boot para gerenciar diferentes ambientes (desenvolvimento e produção) com suporte a variáveis de ambiente para maior segurança e flexibilidade.

## 🔧 Estrutura de Arquivos

```
src/main/resources/
├── application.yml           # Configurações comuns a todos os ambientes
├── application-dev.yml       # Configurações específicas de desenvolvimento
└── application-prd.yml       # Configurações específicas de produção

Raiz do projeto/
├── .env.example              # Exemplo de variáveis de ambiente
├── .env.dev                  # Variáveis pré-configuradas para desenvolvimento
└── .env                      # Suas variáveis locais (não commitado)
```

## 🚀 Como Usar

### 1. Desenvolvimento Local

#### Opção A: Usando arquivo .env (Recomendado)
```bash
# Copie o arquivo de desenvolvimento
cp .env.dev .env

# Execute a aplicação com profile dev
mvn spring-boot:run -Dspring.profiles.active=dev
```

#### Opção B: Usando variáveis de ambiente diretas
```bash
# Linux/Mac
export SPRING_PROFILES_ACTIVE=dev
export DB_URL=jdbc:oracle:thin:@10.10.0.14:1521:TASYHML
export DB_USERNAME=pontetech
export DB_PASSWORD=Qvcp889z
# ... outras variáveis ...

mvn spring-boot:run
```

```powershell
# Windows PowerShell
$env:SPRING_PROFILES_ACTIVE="dev"
$env:DB_URL="jdbc:oracle:thin:@10.10.0.14:1521:TASYHML"
$env:DB_USERNAME="pontetech"
$env:DB_PASSWORD="Qvcp889z"
# ... outras variáveis ...

mvn spring-boot:run
```

### 2. Produção

#### Docker
```dockerfile
# Dockerfile exemplo
FROM openjdk:17-jdk-slim
COPY target/servico-api-odontoprev.jar app.jar

# Variáveis de ambiente serão injetadas na execução
ENTRYPOINT ["java", "-jar", "/app.jar", "--spring.profiles.active=prd"]
```

```bash
# Docker run com variáveis
docker run -d \
  -e SPRING_PROFILES_ACTIVE=prd \
  -e DB_URL=jdbc:oracle:thin:@servidor-prod:1521:PRODDB \
  -e DB_USERNAME=user_prod \
  -e DB_PASSWORD=senha_segura \
  -e ODONTOPREV_BASE_URL=https://api.odontoprev.com.br:8243 \
  -e ODONTOPREV_CREDENTIALS_TOKEN=token_producao \
  # ... outras variáveis ...
  -p 8080:8080 \
  servico-api-odontoprev:latest
```

#### Kubernetes
```yaml
apiVersion: v1
kind: ConfigMap
metadata:
  name: odontoprev-config
data:
  SPRING_PROFILES_ACTIVE: "prd"
  DB_URL: "jdbc:oracle:thin:@servidor-prod:1521:PRODDB"
  ODONTOPREV_BASE_URL: "https://api.odontoprev.com.br:8243"
  # ... outras configurações não sensíveis ...

---
apiVersion: v1
kind: Secret
metadata:
  name: odontoprev-secrets
type: Opaque
data:
  DB_PASSWORD: <base64-encoded-password>
  ODONTOPREV_CREDENTIALS_TOKEN: <base64-encoded-token>
  # ... outras credenciais ...
```

## 📊 Variáveis de Ambiente Principais

### Banco de Dados
- `DB_URL` - URL de conexão JDBC
- `DB_USERNAME` - Usuário do banco
- `DB_PASSWORD` - Senha do banco
- `DB_POOL_SIZE` - Tamanho máximo do pool de conexões

### API OdontoPrev
- `ODONTOPREV_BASE_URL` - URL base da API
- `ODONTOPREV_CREDENTIALS_TOKEN` - Token de autenticação
- `ODONTOPREV_CREDENTIALS_EMPRESA` - Código da empresa
- `ODONTOPREV_CREDENTIALS_USUARIO` - Usuário da API
- `ODONTOPREV_CREDENTIALS_SENHA` - Senha da API

### Logging
- `LOG_LEVEL` - Nível de log geral (DEBUG, INFO, WARN, ERROR)
- `LOG_LEVEL_APP` - Nível de log da aplicação
- `LOG_FILE_PATH` - Caminho do arquivo de log (produção)

## 🔒 Segurança

### Boas Práticas

1. **NUNCA** commite arquivos `.env` com credenciais reais
2. Use gerenciadores de secrets em produção:
   - AWS Secrets Manager
   - Azure Key Vault
   - HashiCorp Vault
   - Kubernetes Secrets

3. Rotacione credenciais regularmente
4. Use senhas fortes e tokens únicos por ambiente
5. Configure logs de auditoria para acesso às credenciais

### Exemplo com AWS Secrets Manager

```java
// Adicione a dependência no pom.xml
<dependency>
    <groupId>com.amazonaws.secretsmanager</groupId>
    <artifactId>aws-secretsmanager-jdbc</artifactId>
    <version>1.0.8</version>
</dependency>
```

```yaml
# application-prd.yml
spring:
  datasource:
    url: jdbc-secretsmanager:oracle:thin:@servidor-prod:1521:PRODDB
    username: odontoprev/db/credentials
    driver-class-name: com.amazonaws.secretsmanager.sql.AWSSecretsManagerOracleDriver
```

## 🧪 Testando as Configurações

### Verificar Profile Ativo
```bash
# Verificar nos logs da aplicação o profile ativo
# Procurar por: "The following profiles are active: dev"
```

### Logs de Inicialização
```
2024-01-10 10:00:00 - INFO - The following profiles are active: dev
2024-01-10 10:00:01 - INFO - Database URL: jdbc:oracle:thin:@10.10.0.14:1521:TASYHML
2024-01-10 10:00:02 - INFO - OdontoPrev API URL: https://api-hml.odontoprev.com.br:8243
```

## 📝 Troubleshooting

### Problema: Variáveis não sendo lidas
**Solução:** Verifique se o profile está ativo e se as variáveis estão definidas:
```bash
echo $SPRING_PROFILES_ACTIVE
env | grep ODONTOPREV
```

### Problema: Erro de conexão com banco
**Solução:** Teste a conexão diretamente:
```bash
telnet 10.10.0.14 1521
```

### Problema: Token da API inválido
**Solução:** Verifique se o token não tem espaços ou caracteres especiais:
```bash
echo -n "$ODONTOPREV_CREDENTIALS_TOKEN" | wc -c
```

## 📚 Referências

- [Spring Boot Profiles](https://docs.spring.io/spring-boot/docs/current/reference/html/features.html#features.profiles)
- [Spring Boot External Configuration](https://docs.spring.io/spring-boot/docs/current/reference/html/features.html#features.external-config)
- [12 Factor App - Config](https://12factor.net/config)