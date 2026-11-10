<div align="center">

# Orçamento API

**Assistente financeiro por voz com Spring Boot e Spring AI**

[![Java 21](https://img.shields.io/badge/Java-21-orange?logo=openjdk&logoColor=white)](https://openjdk.org/projects/jdk/21/)
[![Spring Boot 4](https://img.shields.io/badge/Spring%20Boot-4.0-6DB33F?logo=springboot&logoColor=white)](https://spring.io/projects/spring-boot)
[![Spring AI 2](https://img.shields.io/badge/Spring%20AI-2.0-6DB33F?logo=spring&logoColor=white)](https://spring.io/projects/spring-ai)
[![OpenAI](https://img.shields.io/badge/OpenAI-gpt--4o--mini%20%7C%20whisper-412991?logo=openai&logoColor=white)](https://platform.openai.com/docs/models)

[Visão geral](#visão-geral) • [Como rodar](#como-rodar) • [Como usar](#como-usar) • [Como funciona](#como-funciona) • [Endpoints](#endpoints)

</div>

Diga *"gastei 80 reais no mercado"* e a API transcreve o áudio, entende a intenção, registra o gasto e responde **em áudio**. Tudo isso com regras de negócio reais executando por trás da IA.

## Visão geral

API REST de controle de gastos desenvolvida como desafio final do módulo de **Spring AI** da DIO, a partir do [projeto base do expert](https://github.com/digitalinnovationone/dio-spring-boot-learning-track/tree/main/05-spring-ai).

A ideia central é conectar IA a uma aplicação real sem misturar responsabilidades: **a IA decide o que fazer, mas quem executa são as regras de negócio do sistema**.

- **Comandos por voz ou texto:** transcrição com Whisper, resposta falada em MP3 com `gpt-4o-mini-tts`.
- **Tool Calling:** a IA escolhe entre registrar um gasto, listar por categoria ou gerar um resumo.
- **Mesmas regras para API e IA:** as validações ficam no serviço, então valem para os dois caminhos.
- **Contas exatas:** totais calculados em código com `BigDecimal`, nunca pelo modelo.
- **Pronto para explorar:** Swagger UI, console do H2 e erros no padrão Problem Details.

## Como rodar

### Pré-requisitos

- [Java 21+](https://adoptium.net/)
- Uma [chave da API da OpenAI](https://platform.openai.com/api-keys)

Não é preciso instalar o Maven nem Docker: o projeto inclui o Maven Wrapper (`mvnw`) e usa banco H2 em memória.

### Executar a aplicação

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

> [!IMPORTANT]
> A aplicação lê a chave da variável `OPENAI_API_KEY` e não sobe sem ela.

Com a aplicação no ar:

| Recurso | Endereço |
|---|---|
| Swagger UI | http://localhost:8080/swagger-ui.html |
| Console do H2 | http://localhost:8080/h2-console (JDBC URL `jdbc:h2:mem:orcamentodb`, usuário `sa`, sem senha) |

### Rodar os testes

```bash
./mvnw test
```

Os testes unitários de regras de negócio e das ferramentas rodam sem chave.

> [!NOTE]
> Os testes que dependem da OpenAI (`AssistenteFinanceiroIntegrationTest` e `OrcamentoApiApplicationTests`) só rodam quando `OPENAI_API_KEY` está definida, porque chamam a API de verdade e consomem créditos. Um deles garante que a IA **não registra nada** quando o valor não é dito.

## Como usar

### Comando por voz

O projeto traz áudios de exemplo em `src/test/resources/audio`:

```bash
curl -X POST http://localhost:8080/assistente/voz \
  -F "arquivo=@src/test/resources/audio/recording-1.m4a" \
  -D - --output resposta.mp3
```

Abra o `resposta.mp3` para ouvir a resposta. O cabeçalho `X-Transcricao` mostra o texto que a IA entendeu (codificado em URL, por causa dos acentos).

### Comando por texto

> [!TIP]
> O endpoint `/assistente/texto` executa o mesmo fluxo da IA, sem transcrição e sem voz. É mais rápido de testar e gasta menos créditos.

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

A IA chamou a ferramenta `registrar-gasto`, converteu a fala para `45.90` e escolheu a categoria `RESTAURANTE`. O gasto aparece em `GET /transacoes`.

### Resumo de gastos

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

É essa mesma resposta que a IA recebe quando alguém pergunta *"onde eu gastei mais?"*.

## Como funciona

```
Áudio ──► Transcrição (Whisper) ──► ChatClient ──┬──► registrar-gasto              ┐
                                                  ├──► listar-gastos-por-categoria  ├──► TransacaoService ──► H2
                                                  └──► resumo-de-gastos             ┘
                                       │
                                       ▼
                                 resposta em texto ──► Voz (TTS) ──► MP3
```

### ChatClient

`AssistenteConfig` monta o `ChatClient` uma única vez, com o prompt de sistema ([`prompts/assistente-financeiro.st`](src/main/resources/prompts/assistente-financeiro.st)) e as ferramentas registradas. O prompt traz o mapa de categorias, proíbe a IA de inventar valores e pede respostas curtas, porque elas viram áudio.

```java
return builder
        .defaultSystem(promptSistema.getContentAsString(StandardCharsets.UTF_8))
        .defaultTools(ferramentas)
        .build();
```

### Tool Calling

Em `FerramentasFinanceiras`, cada método com `@Tool` vira uma ferramenta que o modelo pode chamar. A IA lê o nome e a descrição, escolhe qual usar e preenche os parâmetros a partir da fala:

```java
@Tool(name = "registrar-gasto",
        description = "Registra um novo gasto. Use somente quando a pessoa informar o valor do gasto.")
public TransacaoResponse registrarGasto(String descricao, BigDecimal valor, Categoria categoria) {
    return transacaoService.registrar(descricao, valor, categoria);
}
```

| Ferramenta | Exemplo de comando |
|---|---|
| `registrar-gasto` | "Gastei 45 reais no restaurante" |
| `listar-gastos-por-categoria` | "Quais foram meus gastos com farmácia?" |
| `resumo-de-gastos` | "Quanto eu gastei?", "Onde gastei mais?" |

As ferramentas **não acessam o banco direto**: delegam para o `TransacaoService`, o mesmo usado pelo controller REST. Quando a IA registra um gasto, a requisição não passa pelo `@Valid` do controller, então as validações precisam estar no serviço. Se a IA tentar salvar um valor zerado, o serviço recusa, o erro volta para o modelo e ele explica o problema ao usuário.

### Fluxo de voz

`AssistenteFinanceiro` funciona como uma Facade para os três modelos (transcrição, chat e voz). O controller fica com três linhas:

```java
String mensagem = assistente.transcrever(arquivo.getResource());
String resposta = assistente.responder(mensagem);
byte[] audio = assistente.falar(resposta);
```

## Endpoints

| Método | Rota | Descrição |
|---|---|---|
| `POST` | `/assistente/voz` | Envia um áudio (campo `arquivo`, até 10 MB) e recebe a resposta em MP3 |
| `POST` | `/assistente/texto` | Envia o comando em texto e recebe a resposta em texto |
| `POST` | `/transacoes` | Registra um gasto (`201 Created`) |
| `GET` | `/transacoes?categoria=` | Lista os gastos, dos mais recentes para os mais antigos, com filtro opcional |
| `GET` | `/transacoes/resumo` | Total gasto e total por categoria, do maior para o menor |
| `GET` | `/transacoes/categorias` | Categorias disponíveis |

Categorias: `MERCADO`, `FARMACIA`, `TRANSPORTE`, `RESTAURANTE`, `LAZER` e `OUTROS`.

Regras de um gasto: descrição obrigatória (até 255 caracteres), valor maior que zero e até R$ 1.000.000,00, e categoria obrigatória.

### Erros

Os erros seguem o padrão **Problem Details** ([RFC 7807](https://datatracker.ietf.org/doc/html/rfc7807)):

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

## Estrutura do projeto

```
src/main/java/br/com/orcamentoapi
├── config/        AssistenteConfig: monta o ChatClient com prompt e ferramentas
├── ia/            FerramentasFinanceiras (Tool Calling) e AssistenteFinanceiro (áudio ↔ texto)
├── controller/    endpoints REST de transações e do assistente
├── service/       regras de negócio e validações (usadas pela API e pela IA)
├── repository/    acesso ao banco
├── model/         entidade Transacao e enum Categoria
├── dto/           objetos de entrada e saída
└── exception/     exceções e tratamento global de erros

src/main/resources
├── application.properties           modelos, voz e limites de upload
└── prompts/assistente-financeiro.st prompt de sistema da IA
```

## Tecnologias

- Java 21 e Spring Boot 4 (Web, Data JPA, Validation)
- Spring AI 2 com OpenAI: `gpt-4o-mini` (chat), `whisper-1` (transcrição) e `gpt-4o-mini-tts` (voz)
- H2 Database em memória
- SpringDoc OpenAPI (Swagger UI)
- JUnit 5, Mockito e AssertJ

## Evoluções em relação ao projeto base

- **Validações no serviço**, valendo tanto para a API quanto para a IA.
- **Valores em reais com `BigDecimal`:** o projeto base guardava centavos em `long` e exibia esse número como reais (8000 centavos apareciam como "8000.00"). Agora a IA envia `19.90` direto.
- **Ferramenta de resumo** com total geral e por categoria, somados no código para serem sempre exatos.
- **Endpoint de texto** (`/assistente/texto`) para testar o fluxo da IA sem áudio.
- **Prompt de sistema melhorado** e novas categorias (`RESTAURANTE`, `LAZER` e `OUTROS`).
- **Swagger, Problem Details, cabeçalho `X-Transcricao` e H2** para facilitar o uso, sem Docker.
- **Testes** unitários das regras de negócio e de integração com a OpenAI.

## O que aprendi

- O Spring AI deixa o código independente do provedor: `ChatClient`, `TranscriptionModel` e `TextToSpeechModel` são abstrações, e a OpenAI é só configuração.
- Tool Calling transforma métodos Java comuns em ações da IA, e a descrição de cada ferramenta e parâmetro muda muito o comportamento do modelo.
- A IA é mais uma porta de entrada da aplicação, então as validações precisam ficar no serviço, não só no controller.
- Contas devem ser feitas pelo código, não pelo modelo.
- O prompt de sistema também é código: regras claras deixam as respostas mais confiáveis.
