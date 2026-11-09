# 🎙️ Orçamento API: Assistente Financeiro por Voz com Spring AI

API REST de controle de gastos desenvolvida como desafio final do módulo de **Spring AI** da DIO, a partir do
[projeto base do expert](https://github.com/digitalinnovationone/dio-spring-boot-learning-track/tree/main/05-spring-ai).
O usuário envia um áudio dizendo o que gastou ("gastei 80 reais no mercado"), a API transcreve a fala, usa IA para
entender a intenção, executa uma função real da aplicação (registrar ou consultar gastos) e devolve a resposta **em áudio**.

O foco do projeto é mostrar, de forma prática, como conectar IA a uma aplicação real sem misturar responsabilidades:
a IA **decide o que fazer**, mas quem **executa** são as regras de negócio do próprio sistema.

## 🧠 Como a IA foi integrada

```
Áudio ──► Transcrição (Whisper) ──► ChatClient ──┬──► registrar-gasto            ┐
                                                  ├──► listar-gastos-por-categoria ├──► TransacaoService ──► banco H2
                                                  └──► resumo-de-gastos           ┘
                                       │
                                       ▼
                                 resposta em texto ──► Voz (TTS) ──► MP3
```

### ChatClient: `AssistenteConfig`

O `ChatClient` é montado uma única vez, já com o prompt de sistema (`resources/prompts/assistente-financeiro.st`) e as
ferramentas registradas. O prompt define as categorias, proíbe a IA de inventar valores e pede respostas curtas,
porque elas viram áudio.

```java
return builder
        .defaultSystem(promptSistema.getContentAsString(StandardCharsets.UTF_8))
        .defaultTools(ferramentas)
        .build();
```

### Tool Calling: `FerramentasFinanceiras`

Cada método com `@Tool` vira uma ferramenta que o modelo pode chamar. A IA lê o nome e a descrição, escolhe qual usar
e preenche os parâmetros a partir da fala:

```java
@Tool(name = "registrar-gasto",
        description = "Registra um novo gasto. Use somente quando a pessoa informar o valor do gasto.")
public TransacaoResponse registrarGasto(String descricao, BigDecimal valor, Categoria categoria) {
    return transacaoService.registrar(descricao, valor, categoria);
}
```

As ferramentas **não acessam o banco direto**: elas delegam para o `TransacaoService`, o mesmo usado pelo controller REST.
Isso é importante porque, quando a IA registra um gasto, a requisição não passa pelo `@Valid` do controller. Com as
validações no serviço, a mesma regra vale para os dois caminhos. Se a IA tentar salvar um valor zerado, o serviço
recusa, o erro volta para o modelo e ele explica o problema para o usuário.

| Ferramenta | Quando a IA usa |
|---|---|
| `registrar-gasto` | "Gastei 45 reais no restaurante" |
| `listar-gastos-por-categoria` | "Quais foram meus gastos com farmácia?" |
| `resumo-de-gastos` | "Quanto eu gastei?", "Onde gastei mais?" |

### Áudio: `AssistenteFinanceiro`

Esconde os três modelos do fluxo de voz atrás de métodos simples (funciona como uma Facade): `transcrever`,
`responder` e `falar`. O controller de voz fica com três linhas:

```java
String mensagem = assistente.transcrever(arquivo.getResource());
String resposta = assistente.responder(mensagem);
byte[] audio = assistente.falar(resposta);
```

## ✨ O que evoluí em relação ao projeto base

- **Validações antes de salvar:** descrição obrigatória (até 255 caracteres), valor maior que zero e até R$ 1.000.000,00, e categoria obrigatória. Elas valem tanto para a API quanto para a IA.
- **Valores em reais com `BigDecimal`:** o projeto base guardava centavos em `long` e exibia esse número como se fosse reais (8000 centavos apareciam como "8000.00"). Agora a IA envia `19.90` direto e não precisa converter nada.
- **Nova consulta e nova ferramenta:** resumo com o total geral e o total por categoria, da maior para a menor. A soma é feita no código e não pela IA, então o total é sempre exato.
- **Endpoint de texto (`/assistente/texto`):** é o mesmo fluxo da IA sem gravar áudio. Fica mais fácil de testar e gasta menos créditos.
- **Prompt de sistema melhorado:** traz o mapa de categorias, proíbe inventar valores e pede respostas curtas.
- **Novas categorias:** RESTAURANTE, LAZER e OUTROS.
- **Melhorias de uso:**
  - Swagger para explorar a API.
  - Erros no padrão Problem Details.
  - Cabeçalho `X-Transcricao` mostrando o que a IA entendeu do áudio.
  - Banco H2 em memória, sem precisar de Docker.
- **Testes:**
  - Testes unitários das regras de negócio.
  - Testes de integração com a OpenAI. Um deles confere que a IA **não registra nada** quando o valor não é dito.

## 🛠️ Tecnologias

- Java 21
- Spring Boot 4 (Web, Data JPA, Validation)
- Spring AI 2 com OpenAI (`gpt-4o-mini`, `whisper-1` e `gpt-4o-mini-tts`)
- H2 Database (banco em memória)
- SpringDoc OpenAPI (Swagger UI)
- JUnit 5, Mockito e AssertJ

## ▶️ Como rodar

Pré-requisitos: **Java 21+** e uma **chave da API da OpenAI**. Não é preciso instalar o Maven, porque o projeto inclui
o Maven Wrapper (`mvnw`).

```bash
# Linux/macOS
export OPENAI_API_KEY="sua_chave_aqui"
./mvnw spring-boot:run
```

```powershell
# Windows (PowerShell)
$env:OPENAI_API_KEY="sua_chave_aqui"
mvnw.cmd spring-boot:run
```

Depois acesse:

- Swagger UI: http://localhost:8080/swagger-ui.html
- Console do H2: http://localhost:8080/h2-console (JDBC URL: `jdbc:h2:mem:orcamentodb`, usuário `sa`, sem senha)

Para rodar os testes:

```bash
./mvnw test
```

Os testes unitários rodam sem chave. Os testes de integração (`*IntegrationTest`) só rodam quando `OPENAI_API_KEY` está definida,
porque chamam a OpenAI de verdade.

## 📡 Endpoints

| Método | Rota | Descrição |
|---|---|---|
| `POST` | `/assistente/voz` | Envia um áudio (campo `arquivo`) e recebe a resposta em MP3 |
| `POST` | `/assistente/texto` | Envia o comando em texto e recebe a resposta em texto |
| `POST` | `/transacoes` | Registra um gasto (retorna `201 Created`) |
| `GET` | `/transacoes?categoria=` | Lista os gastos, com filtro opcional por categoria |
| `GET` | `/transacoes/resumo` | Total gasto e total por categoria |
| `GET` | `/transacoes/categorias` | Categorias disponíveis |

### Exemplo: comando por voz (fluxo principal)

O projeto já traz áudios de exemplo em `src/test/resources/audio`:

```bash
curl -X POST http://localhost:8080/assistente/voz \
  -F "arquivo=@src/test/resources/audio/recording-1.m4a" \
  -D - --output resposta.mp3
```

Abra o `resposta.mp3` para ouvir a resposta. O cabeçalho `X-Transcricao` mostra o texto que a IA entendeu.

### Exemplo: comando por texto

```bash
curl -X POST http://localhost:8080/assistente/texto \
  -H "Content-Type: application/json" \
  -d '{"mensagem": "Gastei 45 reais e 90 centavos no restaurante"}'
```

```json
{
  "mensagem": "Gastei 45 reais e 90 centavos no restaurante",
  "resposta": "Pronto, registrei 45 reais e 90 centavos em restaurante."
}
```

A IA escolheu a ferramenta `registrar-gasto`, converteu a fala para `45.90` e escolheu a categoria `RESTAURANTE`.
O gasto aparece em `GET /transacoes`.

### Exemplo: resumo de gastos

```bash
curl http://localhost:8080/transacoes/resumo
```

```json
{
  "quantidade": 4,
  "total": 315.40,
  "porCategoria": [
    { "categoria": "TRANSPORTE", "quantidade": 1, "total": 200.00 },
    { "categoria": "MERCADO", "quantidade": 2, "total": 95.50 },
    { "categoria": "FARMACIA", "quantidade": 1, "total": 19.90 }
  ]
}
```

É essa mesma resposta que a IA recebe quando alguém pergunta "onde eu gastei mais?".

### Tratamento de erros

Os erros seguem o padrão **Problem Details (RFC 7807)**:

| Situação | Status |
|---|---|
| Campos inválidos, valor zerado ou negativo, categoria inexistente | `400` |
| Áudio vazio ou que não pôde ser entendido | `400` |
| Áudio maior que 10 MB | `413` |

```json
{
  "status": 400,
  "detail": "Um ou mais campos são inválidos",
  "campos": {
    "valor": "Valor deve ser maior que zero"
  }
}
```

## 📁 Estrutura

```
src/main/java/br/com/orcamentoapi
├── config/        → AssistenteConfig (monta o ChatClient com prompt e ferramentas)
├── ia/            → FerramentasFinanceiras (Tool Calling) e AssistenteFinanceiro (áudio ↔ texto)
├── controller/    → endpoints REST de transações e do assistente
├── service/       → regras de negócio e validações (usadas pela API e pela IA)
├── repository/    → acesso ao banco
├── model/         → entidade Transacao e enum Categoria
├── dto/           → objetos de entrada e saída
└── exception/     → exceções e tratamento global de erros

src/main/resources/prompts/assistente-financeiro.st → prompt de sistema da IA
```

## 💡 O que aprendi

- O Spring AI deixa o código independente do provedor. `ChatClient`, `TranscriptionModel` e `TextToSpeechModel` são abstrações, e a OpenAI é só configuração no `application.properties`.
- Tool Calling transforma métodos Java comuns em ações que a IA pode executar. A descrição de cada ferramenta e de cada parâmetro muda muito o comportamento do modelo.
- A IA é mais uma porta de entrada da aplicação. Por isso as validações precisam ficar no serviço, e não só no controller.
- Contas devem ser feitas pelo código, não pelo modelo. Por isso o resumo de gastos é uma ferramenta.
- O prompt de sistema também é código. Regras claras, como não inventar valores e responder de forma curta, deixam as respostas mais confiáveis.
