## Estrutura de Pacotes
- O projeto deve ser organizado em pastas/pacotes de acordo com a arquitetura hexagonal (ports and adapters).
- Evite acoplamento direto entre as camadas. Sempre use interfaces para dependências externas.

## Padrão de Pacotes para Arquitetura Hexagonal
- **core** (ou **domain**): onde ficam as regras de negócio, entidades, value objects, interfaces (ports) de entrada e saída.
- **application**: serviços de aplicação, casos de uso, orquestradores. Aqui não deve haver dependência de frameworks ou tecnologia externa.
- **adapters** (ou **infrastructure**): implementações concretas das interfaces definidas no domínio, como repositórios, clients de APIs, implementações de gateways, etc.
- **config**: arquivos de configuração e inicialização de dependências.

## Boas Práticas de Código para Java
- Sempre use UUID para ID de qualquer entidade
- Nomes em Português:
  Todas as classes, variáveis, métodos, interfaces e enums devem ser nomeados em português, de forma clara e descritiva.
    - Classes: `Pedido`, `Cliente`, `ServicoFinanceiro`
    - Métodos: `calcularDesconto()`, `enviarEmailConfirmacao()`
    - Variáveis: `valorTotal`, `dataCadastro`, `listaProdutos`
    - Constantes: MAIÚSCULAS_SEPARADAS_POR_UNDERSCORE. Ex: `TAXA_JUROS`

- Utilize os principaios do SOLID e Clean code



## 🏷️ Bean Validator (Validação de Borda)

- Toda validação de campos **deve ser feita usando Bean Validation** (`jakarta.validation`).
- Use sempre anotações padrão (`@NotNull`, `@NotBlank`, `@Email`, `@Size`, etc.).
- Crie anotações customizadas **somente se absolutamente necessário**.
- Mensagens de erro devem ser claras e em português.
- Validação de borda: **todas as entradas vindas do mundo externo** (DTOs de request) **devem ser validadas** antes de entrar na camada de domínio.
- Nunca duplique validações no domínio se já estiverem no DTO.
- Utilize `@Validated` nos controllers e `@Valid` nos parâmetros dos métodos.
- Não misture lógica de validação com regras de negócio.


## 🔄 MapStruct (Conversão entre Objetos)

- Utilize MapStruct para mapear entre DTOs, Entities e outros tipos de objetos.
- Cada contexto/entidade deve possuir seu próprio mapper (ex: `ClienteMapper`).
- Os métodos de conversão devem seguir o padrão: `toEntidade`, `toDTO`, `toResponse`.
- O mapper **não deve conter nenhuma regra de negócio** – apenas conversão de dados.
- Use `@Mapper(componentModel = "spring")` para garantir injeção automática.
- Métodos auxiliares (como conversão de enums ou listas) podem ser privados e definidos no próprio mapper.
- Utilize nomes de métodos claros e autoexplicativos.
- Evite mapeamentos manuais: prefira a automação do MapStruct.

# 🌐 Regras para Feign Client

- Utilize Feign Client para comunicação HTTP com outros microsserviços.
- Cada serviço externo deve possuir uma interface Feign dedicada, anotada com `@FeignClient`.
- Defina métodos para cada endpoint, usando as anotações corretas do Spring (`@GetMapping`, `@PostMapping`, etc).
- Utilize DTOs para requisições e respostas. **Nunca** exponha entidades do domínio.
- Implemente tratamento de exceções centralizado usando `@ControllerAdvice` ou mecanismos globais.
- Configure timeouts e políticas de retry/fallback conforme a criticidade da integração.
- Adicione logs de entrada e saída para rastreabilidade.
- Documente parâmetros obrigatórios e exemplos de uso para cada método Feign.

## Padrões de Banco de Dados

### Nomeação de Tabelas
- Sempre em **minúsculo** e **no singular**.
- Palavras compostas separadas por underline:  
  Exemplo: `pedido_item`, `cliente_endereco`.

### Chave Primária (PK)
- Toda tabela deve ter uma PK chamada `id`.
- Preferencialmente do tipo numérico (`BIGINT`, `SERIAL`, etc.), autoincrementável.

### Chave Estrangeira (FK)
- Nomeie FK como:  
  `fk_<tabela_origem>_<tabela_destino>`
- Exemplo: `fk_pedido_cliente`
- Sempre utilize restrições explícitas e avalie necessidade de `ON DELETE CASCADE`.

### Chave Única (UK)
- Nomeie UK como:  
  `uk_<tabela>_<coluna>`
- Exemplo: `uk_cliente_cpf`
- Use quando um campo precisa ser único além da PK (ex: e-mail, cpf, cnpj).

### Colunas
- Sempre em **minúsculo** e separadas por underline.
- Use nomes claros e autoexplicativos (ex: `data_criacao`, `valor_total`).

### Índices
- Crie índices para colunas frequentemente usadas em filtros/buscas.
- Nome dos índices: `idx_<tabela>_<coluna>`

# 📝 Regras para Entidades JPA e Mapeamento Objeto-Relacional

## Padrão de Nomenclatura

- Os nomes de entidades JPA devem refletir fielmente os nomes das tabelas no banco de dados (em português, singular, e camelCase na classe; snake_case na anotação `@Table`).
- O nome da tabela sempre deve ser explicitado na anotação `@Table(name = "nome_tabela")`.
- Os atributos das entidades devem corresponder exatamente aos nomes das colunas (snake_case no banco; camelCase na entidade).  
  Exemplo: atributo `dataCriacao` com anotação `@Column(name = "data_criacao")`.

## Chave Primária (PK)

- Toda entidade deve ter um campo `id` anotado com `@Id`.
- Use `@GeneratedValue(strategy = GenerationType.IDENTITY)` (ou estratégia apropriada) para autoincremento.
- Tipo preferencial: `Long`.

## Chave Estrangeira (FK)

- Relacionamentos devem ser declarados usando `@ManyToOne`, `@OneToMany`, `@OneToOne` ou `@ManyToMany` conforme o caso.
- Sempre explicite o nome da coluna de FK usando `@JoinColumn(name = "fk_tabela_destino")`.
- Use o mesmo padrão de nomeação do banco: `fk_<tabela_origem>_<tabela_destino>`.

## Chave Única (UK)

- Restrições de unicidade devem ser representadas com a anotação `@Column(unique = true)` ou via `@Table(uniqueConstraints = ...)`, usando o padrão de nomeação do banco.
- O nome da constraint deve seguir: `uk_<tabela>_<coluna>`.

## Outras Regras e Boas Práticas

- Sempre use `@Column` explicitando o nome da coluna.
- Utilize `@Entity` e `@Table` em todas as entidades.
- Use `@CreationTimestamp` e `@UpdateTimestamp` para datas de auditoria.
- Relacionamentos do tipo lista (coleção) devem ser inicializados como `new ArrayList<>()` para evitar `NullPointerException`.
- Evite lógica de negócio nas entidades; mantenha apenas o modelo de dados.
- Mantenha entidades limpas, sem dependências desnecessárias (ex: DTOs, Beans de negócio).
- Mapeie todos os campos relevantes do banco, inclusive os campos de auditoria (`data_criacao`, `data_atualizacao`, etc.).
- Se houver necessidade de campos transientes, use `@Transient`.

🌐 Regras para Rotas e Endpoints REST

## Idioma e Nomeação

- **Todos os endpoints devem estar em português**, incluindo o nome dos recursos, caminhos (`paths`) e parâmetros.
- Utilize substantivos no plural para representar coleções (ex: `/clientes`, `/pedidos`).
- Utilize nomes claros, descritivos e consistentes para cada recurso.
- Utilize convenção kebab-case nos caminhos das rotas (ex: `/dados-pessoais`).

## Padrão de Rotas

- Sempre que possível, siga o padrão RESTful:
    - `GET /clientes` – listar clientes
    - `POST /clientes` – criar cliente
    - `GET /clientes/{id}` – buscar cliente por id
    - `PUT /clientes/{id}` – atualizar cliente
    - `DELETE /clientes/{id}` – remover cliente
- Para ações específicas, utilize sub-recursos claros em português (ex: `/clientes/{id}/ativar`, `/pedidos/{id}/cancelar`).



# 🧪 Regras para Testes Unitários

## Objetivo

Garantir a qualidade, confiabilidade e evolução segura do código através de testes unitários bem escritos, claros e automatizados.

---

## Padrões Gerais

- **Todo código de negócio (domínio, aplicação, serviço) deve possuir testes unitários cobrindo os principais fluxos.**
- Use sempre **JUnit 5** como framework principal de testes.
- Utilize **Mockito** ou similar para mocks/stubs/fakes quando necessário.
- Nomeie as classes de teste espelhando o nome da classe testada, seguido de `Test` (ex: `ClienteServiceTest`).
- Os métodos de teste devem ter nomes descritivos em português, refletindo a ação testada e o cenário (ex: `deveRetornarClienteQuandoIdExistir`).

---

## Boas Práticas

- Cada método de teste deve validar um único comportamento/cenário.
- Evite dependências externas (banco, rede, serviços reais): **mock tudo o que não for a unidade testada**.
- Prefira Arrange-Act-Assert (AAA):
    1. **Preparação** dos dados/mock
    2. **Execução** do método
    3. **Verificação** do resultado esperado
- Não deixe código morto ou comentários desnecessários nos testes.
- Cubra cenários positivos e negativos, incluindo exceções.
- Asserções devem ser claras e específicas (`assertEquals`, `assertTrue`, `assertThrows`, etc).
- Evite duplicação de código de preparação usando métodos auxiliares ou `@BeforeEach`.

---

## Cobertura e Manutenção

- Busque cobertura mínima de 80% do código de domínio, priorizando regras de negócio.
- Todo bug corrigido deve resultar em um novo teste que reproduza o erro.
- Os testes devem rodar rapidamente e serem determinísticos (mesmo resultado em qualquer execução).
- Testes unitários devem ser executados em cada build/CI.

## Lombok

- Utilize **Lombok** para reduzir boilerplate em entidades, DTOs, comandos e objetos de valor.
- Prefira as anotações:
    - `@Getter` e `@Setter` para métodos de acesso.
    - `@NoArgsConstructor` para construtor vazio.
    - `@AllArgsConstructor` para construtor com todos os campos.  
      **Nunca escreva o construtor manualmente** quando usar essa anotação.
    - `@Builder` para facilitar a criação de objetos complexos.
    - `@Data` apenas em DTOs simples.
- Evite misturar construtores manuais e anotações do Lombok na mesma classe.
- Sempre utilize a versão mais atual do Lombok compatível com o projeto.
# claude.md – Regras de Boas Práticas (Java, Arquitetura Hexagonal)

## 📝 Regras Obrigatórias para Comentários no Código

### Documentação Obrigatória

**TODO CÓDIGO DEVE SER COMENTADO PARA UM LEIGO ENTENDER**

#### Comentários de Classe (Obrigatório)
- **Toda classe deve ter comentário explicativo no topo**
- Explicar **o que a classe faz** e **quando é usada**
- Usar linguagem simples e exemplos práticos
- Incluir contexto de negócio quando relevante

```java
/**
 * CLASSE PARA PROCESSAR PAGAMENTOS DE CARTÃO
 * 
 * Esta classe é responsável por validar e processar pagamentos
 * feitos com cartão de crédito ou débito.
 * 
 * QUANDO É USADA:
 * - Quando cliente finaliza compra no checkout
 * - Quando precisa validar dados do cartão
 * - Quando precisa comunicar com operadora
 * 
 * EXEMPLO DE USO:
 * processarPagamento("1234-5678-9012-3456", 150.00)
 */
```

#### Comentários de Método (Obrigatório)
- **Todos os métodos públicos devem ter comentário**
- Explicar **o que faz**, **quando usar** e **parâmetros importantes**
- Métodos privados complexos também devem ser comentados

```java
/**
 * VALIDA SE OS DADOS DO CARTÃO ESTÃO CORRETOS
 * 
 * Verifica se número, CVV e data de validade são válidos
 * antes de tentar processar o pagamento.
 * 
 * @param numeroCartao - número do cartão (sem espaços)
 * @param cvv - código de segurança (3 dígitos)
 * @return true se cartão é válido, false se inválido
 */
```

#### Comentários Internos (Quando Necessário)
- Explicar **lógica complexa** ou **não óbvia**
- Usar quando algoritmo não é autoexplicativo
- Explicar **por que** algo é feito, não apenas **o que**

```java
// Multiplica por 1000 para converter de segundos para milissegundos
// API externa espera valor em milissegundos
long timeoutMs = timeoutSeconds * 1000;
```

### Padrão de Linguagem
- **Português claro e simples**
- **Evitar jargões técnicos** desnecessários
- **Usar exemplos práticos** quando possível
- **Explicar PARA QUE serve**, não apenas O QUE faz

## 🔧 Uso Obrigatório da Anotação @MonitorarOperacao

### Quando Usar (Obrigatório)

**TODA operação de negócio deve usar @MonitorarOperacao**

#### Métodos que DEVEM ter a anotação:
- Métodos de service públicos
- Operações de banco de dados
- Chamadas para APIs externas  
- Processamento de lotes
- Operações agendadas (schedulers)
- Validações complexas
- Operações que podem dar erro

#### Configuração Padrão
```java
@MonitorarOperacao(
    operacao = "NOME_OPERACAO_DESCRITIVO",
    excecaoEmErro = TipoExcecao.APROPRIADO
)
```

#### Configurações Avançadas
```java
@MonitorarOperacao(
    operacao = "PROCESSAR_PAGAMENTO",
    incluirParametros = {"numeroCartao", "valor"}, // Inclui parâmetros no contexto
    incluirThread = true,                          // Inclui info da thread
    logSucesso = MonitorarOperacao.NivelLog.INFO,  // Nível do log de sucesso
    excecaoEmErro = PROCESSAMENTO_PAGAMENTO        // Exceção específica
)
```

#### Métodos que NÃO precisam da anotação:
- Getters e setters simples
- Métodos de validação básica (isEmpty, isNull, etc.)
- Construtores
- Métodos de formatação/conversão simples

### Escolha do Tipo de Exceção

**Use o tipo mais específico possível:**

```java
// Para operações de banco de dados
excecaoEmErro = CONSULTA_EMPRESAS

// Para processamento individual
excecaoEmErro = PROCESSAMENTO_EMPRESA  

// Para processamento em lotes
excecaoEmErro = PROCESSAMENTO_LOTE

// Para comunicação com APIs
excecaoEmErro = COMUNICACAO_API

// Para problemas de configuração
excecaoEmErro = CONFIGURACAO
```

### Exemplo Completo
```java
/**
 * PROCESSA PAGAMENTO COM CARTÃO DE CRÉDITO
 * 
 * Valida dados do cartão e comunica com operadora para
 * aprovar ou rejeitar a transação.
 * 
 * @param dadosCartao - informações do cartão do cliente
 * @param valor - valor a ser cobrado
 * @return resultado do processamento (aprovado/rejeitado)
 */
@MonitorarOperacao(
    operacao = "PROCESSAR_PAGAMENTO_CARTAO",
    incluirParametros = {"valor"},
    excecaoEmErro = PROCESSAMENTO_PAGAMENTO
)
public ResultadoPagamento processarPagamento(DadosCartao dadosCartao, BigDecimal valor) {
    // Primeiro valida os dados básicos
    validarDadosCartao(dadosCartao);
    
    // Depois comunica com a operadora
    return comunicarComOperadora(dadosCartao, valor);
}
```

## ⚙️ Regras para Execução de Comandos

### Implementação Obrigatória em Toda Alteração

**SEMPRE que implementar qualquer funcionalidade, seguir estas regras:**

#### 1. Documentação Completa (Obrigatório)
- **Comentar todas as classes** criadas ou modificadas
- **Comentar todos os métodos públicos** 
- **Usar @MonitorarOperacao** em operações de negócio
- **Explicar lógica complexa** com comentários internos

#### 2. Anotação @MonitorarOperacao (Obrigatório)
- **Adicionar em todos os métodos de service**
- **Escolher TipoExcecao apropriado**
- **Incluir parâmetros importantes no contexto**
- **Usar nomes de operação descritivos**

#### 3. Tratamento de Exceções (Obrigatório)
- **Usar exceções customizadas específicas**
- **Nunca usar Exception genérica**
- **Incluir contexto rico para debugging**
- **Seguir padrão estabelecido no projeto**

#### 4. Commit e Deploy (Obrigatório)
- **Sempre fazer commit das alterações**
- **Usar mensagens descritivas em português**
- **Incluir assinatura Claude Code padrão**
- **Fazer push após commit bem-sucedido**

### Exemplo de Fluxo Completo

```java
/**
 * CLASSE PARA VALIDAR DOCUMENTOS DE CLIENTES
 * 
 * Responsável por validar CPF, CNPJ e outros documentos
 * antes de permitir cadastro no sistema.
 */
@Service 
public class ValidadorDocumentosService {

    /**
     * VALIDA SE CPF ESTÁ NO FORMATO CORRETO
     * 
     * Verifica formato, dígitos verificadores e se não é
     * CPF conhecido como inválido (000.000.000-00, etc.)
     * 
     * @param cpf - CPF a ser validado (com ou sem pontuação)
     * @return true se válido, false se inválido
     */
    @MonitorarOperacao(
        operacao = "VALIDAR_CPF",
        incluirParametros = {"cpf"},
        excecaoEmErro = VALIDACAO_DOCUMENTO
    )
    public boolean validarCpf(String cpf) {
        // Remove pontuação para validar apenas números
        String cpfLimpo = cpf.replaceAll("[^0-9]", "");
        
        // Valida se tem 11 dígitos
        if (cpfLimpo.length() != 11) {
            return false;
        }
        
        // Chama validação dos dígitos verificadores
        return validarDigitosVerificadores(cpfLimpo);
    }
}
```

### Comando de Commit Padrão

```bash
git commit -m "$(cat <<'EOF'
Implementa validação avançada de documentos

- Cria ValidadorDocumentosService com validação de CPF/CNPJ
- Adiciona exceções customizadas para erros de validação  
- Implementa @MonitorarOperacao para observabilidade
- Adiciona comentários detalhados para leigos
- Inclui testes unitários completos

🤖 Generated with [Claude Code](https://claude.ai/code)

Co-Authored-By: Claude <noreply@anthropic.com>
EOF
)"
```


## Organização de Pacotes

- Separe os pacotes conforme a arquitetura hexagonal:
    - **Adapters de Entrada (Inbound):** Recebe e envia dados para o usuário/sistemas externos. Exemplo: `adapter.inbound.rest`, `adapter.inbound.dto`.
    - **Adapters de Saída (Outbound):** Comunicação com sistemas externos (banco, APIs externas). Exemplo: `adapter.outbound`.
    - **Aplicação (Application):** Casos de uso. Exemplo: `application.usecase`.
    - **Domínio (Domain):** Entidades, lógica e regras de negócio. Exemplo: `domain.entity`, `domain.service`.

---

## Nomenclatura de Classes

- Sempre utilize sufixos claros e padronizados:
    - `Request`: Dados que chegam do usuário (entrada/borda).
    - `Input` ou `Command`: Objeto de transporte para o caso de uso.
    - `Response`: Dados enviados ao usuário.
- Exemplo: `MensagemUsuarioRequest`, `MensagemUsuarioInput`, `MensagemUsuarioResponse`.

---
## Regras para Classes de Entrada (Request)

- **Finalidade:** Receber dados do usuário (por exemplo, via API REST).
- **Validação:** Use anotações do Bean Validation (`jakarta.validation.constraints`) para validar campos obrigatórios, formatos e restrições de valores.
- **Lógica:** Nunca implemente lógica de negócio ou transformação nesses DTOs.
- **Pacote recomendado:** `adapter.inbound.dto`, `adapter.inbound.rest.dto`.

## Regras para Classes de Transporte (Input/Command/DTO)

- **Finalidade:** Transportar dados da camada de entrada até o caso de uso (Application/Service).
- **Validação:** Não deve conter validação (os dados já chegam validados).
- **Lógica:** Sem lógica de negócio.
- **Imutabilidade:** Prefira classes imutáveis (ex: `record` em Java 17+ ou classes com `final`).

## Regras para Classes de Saída do Caso de Uso (Output/OutputDTO)

- **Finalidade:** Transportar os dados resultantes da execução do UseCase para a borda (Controller/Adapter).
- **Lógica:** Nunca exponha entidades de domínio diretamente.
- **Campos:** Informe somente dados necessários para a resposta, nunca dados internos sensíveis ou entidades completas.
- **Pacote recomendado:** `application.usecase.output`, `application.dto.output`.

## Regras para Classes de Resposta (Response/Output para o usuário)

- **Finalidade:** Retornar ao usuário apenas os dados necessários, formatados e adaptados para a API/borda.
- **Lógica:** Nunca exponha entidades de domínio diretamente.
- **Campos:** Informe somente dados relevantes e permitidos.
- **Pacote recomendado:** `adapter.inbound.dto`, `adapter.inbound.rest.dto`.

## Validação com Bean Validation

- Toda validação obrigatória deve ser realizada nas classes `Request` (entrada).
- Use as anotações do Bean Validation, como:
    - `@NotNull`, `@NotBlank`, `@Email`, `@Size`, `@Min`, `@Max`, etc.
- Mensagens de validação devem ser claras e em português.

## Conversão entre Camadas

- **Todas as conversões entre Request → Input, Input → Output, Output → Response, ou qualquer outro DTO, devem ser realizadas utilizando obrigatoriamente o MapStruct.**
- O uso do MapStruct garante padronização, menos código boilerplate e fácil manutenção.
- Defina interfaces de mapeamento no pacote dedicado, por exemplo: `adapter.mapper` ou `application.mapper`.
- Nunca passe entidades do domínio diretamente entre as camadas externas.

## Fluxo de Dados Padrão

1. Controller/Adapter recebe um `Request`.
2. Valida automaticamente os dados via Bean Validation.
3. Utiliza MapStruct para converter o request em um `Input` e envia ao UseCase.
4. O UseCase executa a lógica de negócio e retorna um objeto de saída **Output**.
5. Adapter utiliza MapStruct para converter esse objeto `Output` em um `Response`.
6. O `Response` é enviado ao usuário.

## Regra: O que o UseCase deve retornar

- **Nunca retorne entidades do domínio (`Entity`) diretamente do UseCase para o Controller ou para qualquer camada externa (entrada/borda).**
- O UseCase deve retornar um **DTO de saída**, chamado de `Output` ou `OutputDTO`.
- O objetivo é isolar o domínio da aplicação das camadas externas, evitando o vazamento de detalhes internos.
- Mudanças no domínio não devem impactar as APIs públicas e vice-versa.
- A conversão da entidade para o DTO de saída deve ser feita no próprio UseCase (ou preferencialmente utilizando um Mapper, como o MapStruct).

### Exemplo de fluxo correto:

1. **UseCase** recebe um `Input` e retorna um **Output** (DTO de saída).
2. **Controller** recebe o **Output**, converte para um `Response` (DTO de resposta para o usuário) usando um Mapper, e retorna para a borda.


# Regras de Endpoints e Boas Práticas REST

## Parâmetros Obrigatórios
- Se qualquer parâmetro obrigatório não for fornecido na requisição, **retorne HTTP 400 (Bad Request)**.
- A resposta deve conter um corpo JSON com um campo `mensagem` ou `erro`, explicando de forma clara qual campo está ausente ou inválido.


## Métodos POST
- Em operações de criação (POST), sempre retorne o **status HTTP 201 (Created)** ao criar o recurso com sucesso.
- No corpo da resposta, retorne o objeto criado ou pelo menos seu identificador.

## Boas Práticas REST
- Utilize nomes de endpoints **no plural** e em **português**, seguindo o padrão `/api/entidades`.
    - Exemplo: `/api/clinicas`, `/api/planos`, `/api/beneficios`
- Sempre que possível, utilize **verbos HTTP** corretamente:
    - `GET` para buscar dados use paginação pra muitos registros
    - `POST` para criar recursos
    - `PUT` para atualizar recursos existentes (inteira substituição)
    - `PATCH` para atualização parcial
    - `DELETE` para remoção
- Ao buscar um recurso que não existe, retorne **HTTP 404 (Not Found)**.
- Ao remover um recurso com sucesso, retorne **HTTP 204 (No Content)**.
- Utilize **status HTTP padronizados** para cada operação.
- Inclua sempre exemplos de requisição e resposta no arquivo `curl-examples.md`.
- Toda documentação deve mencionar os parâmetros obrigatórios, opcionais, tipos e possíveis mensagens de erro.

## Tratamento de Erros
- Nunca exponha detalhes de stacktrace ou informações sensíveis na resposta.
- Mensagens de erro devem ser claras e úteis para o consumidor da API.

## Versionamento
- Exponha a API versionada no caminho: `/api/v1/...`

## Uso de Global Exception Handler

- Deve ser implementado um **Global Exception Handler** para toda a API.
- Todas as exceções (checked e unchecked) lançadas durante o processamento das requisições devem ser capturadas por este handler global.
- O código dos endpoints (controllers/services) **não deve conter blocos try-catch desnecessários**. As exceções devem ser propagadas e tratadas centralizadamente pelo handler, mantendo o código mais limpo e legível.
- O handler deve converter exceções em respostas HTTP apropriadas, sempre retornando status e mensagens intuitivas para o usuário.
    - Exemplo: exceptions de validação → HTTP 400, acesso negado → HTTP 403, não encontrado → HTTP 404, erro interno → HTTP 500, etc.
- O corpo da resposta deve conter sempre um campo `erro` ou `mensagem` clara para o consumidor da API.
- Detalhes sensíveis (como stacktrace) nunca devem ser enviados para o cliente, apenas logs internos.

