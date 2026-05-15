# Smart ATM

Sistema de caixa eletrônico inteligente com backend em **Kotlin + Spring Boot**, integração com **IA generativa** para geração de resumos diários, persistência em **PostgreSQL** e frontend em **HTML/CSS/JS** puro.

---

## Sumário

- [Arquitetura](#arquitetura)
- [Stack](#stack)
- [Como executar](#como-executar)
- [Endpoints](#endpoints)
- [Algoritmo de saque](#algoritmo-de-saque)
- [Tratamento de exceções](#tratamento-de-exceções)
- [Testes](#testes)

---

## Arquitetura

Projeto estruturado seguindo **Arquitetura Hexagonal (Ports & Adapters)**, separando regras de negócio de detalhes técnicos:

```
com.smartatm/
├── domain/                     # Entidades e exceções 
│   ├── User.kt
│   ├── AtmCashInventory.kt
│   ├── Transaction.kt
│   └── Exceptions.kt
│
├── application/                # Casos de uso e orquestração
│   ├── WithdrawService.kt
│   ├── DepositService.kt
│   └── DailySummaryService.kt
│
└── adapter/
    ├── input/rest/             # Controllers HTTP, DTOs, configurações
    │   ├── WithdrawController.kt
    │   ├── DepositController.kt
    │   ├── DailySummaryController.kt
    │   ├── GlobalExceptionHandler.kt
    │   ├── OpenApiConfig.kt
    │   ├── AiConfig.kt
    │   └── CorsConfig.kt
    │
    └── output/persistence/     # Repositórios JPA
        └── Repositories.kt
```

---

## Stack

| Camada | Tecnologia |
|---|---|
| Linguagem | Kotlin 1.9.23 (JVM 21) |
| Framework | Spring Boot 3.2.5 |
| Persistência | Spring Data JPA + PostgreSQL 16 |
| IA | Spring AI + Ollama (llama3) |
| Documentação | Springdoc OpenAPI (Swagger UI) |
| Testes | JUnit 5 + Mockito (mockito-kotlin) |
| Containers | Docker + Docker Compose |
| CI/CD | GitHub Actions |

---

## Como executar

### Pré-requisitos

- JDK 21+
- Docker e Docker Compose
- (Opcional) Ollama com modelo `llama3` para o resumo por IA

### Configuração

Crie um arquivo `.env` na raiz baseado no `.env.example`:

```bash
cp .env.example .env
```
Edite o `.env` com suas credenciais.
### Subindo o banco

```bash
docker compose up db -d
```

### Rodando a aplicação

```bash
./gradlew bootRun
```

Acesse:
- API: `http://localhost:8080`
- Swagger: `http://localhost:8080/swagger-ui.html`
- Frontend: abra `frontend/index.html` no navegador

### Executando tudo via Docker

```bash
docker compose up --build
```

### IA (opcional)

Para que o resumo diário gere narrativas reais:

```bash
ollama pull llama3
ollama serve
```

Sem o Ollama, o endpoint funciona mas retorna mensagem de fallback em `aiSummary`.

---

## Endpoints

| Método | Rota | Descrição |
|--------|------|-----------|
| `POST` | `/withdraw` | Realiza saque com priorização de notas |
| `POST` | `/deposit` | Realiza depósito (R$2, R$5, R$10, R$50) |
| `GET`  | `/daily-summary` | Resumo do dia gerado por IA |

### Exemplos

**Saque:**
```http
POST /withdraw
{ "userId": 1, "amount": 130 }
```
Resposta:
```json
{
  "userId": 1,
  "amountWithdrawn": 130,
  "notesDispensed": { "50": 2, "10": 3 }
}
```

**Depósito:**
```http
POST /deposit
{ "userId": 1, "notes": { "50": 2, "10": 3 } }
```

**Resumo diário:**
```http
GET /daily-summary
```

---

## Algoritmo de saque

O `WithdrawService` implementa um algoritmo **greedy** que prioriza notas de maior valor:

1. Ordena o estoque por denominação (R$50 → R$10 → R$5 → R$2)
2. Para cada denominação, calcula `min(notasNecessárias, notasDisponíveis)`
3. Subtrai do valor restante e segue para a próxima denominação
4. **Fallback:** se não for possível atingir o valor exato com as notas disponíveis, calcula o `maxDispensableAmount` (valor máximo que poderia ser dispensado) e lança `InsufficientNotesException`

---

## Tratamento de exceções

Exceções de domínio são mapeadas para HTTP via `@RestControllerAdvice`:

| Exceção | HTTP | Detalhe |
|---------|------|---------|
| `InvalidAmountException` | 400 | Valor zero ou negativo |
| `InvalidDenominationException` | 400 | Denominação fora de {2, 5, 10, 50} |
| `UserNotFoundException` | 404 | Usuário inexistente |
| `InsufficientBalanceException` | 422 | Saldo insuficiente |
| `InsufficientNotesException` | 422 | Inclui `maxDispensableAmount` na resposta |
| `AtmOutOfCashException` | 503 | Caixa completamente vazio |

Todas as respostas seguem o formato:
```json
{
  "status": 422,
  "error": "Insufficient Notes",
  "message": "...",
  "timestamp": "2026-05-12T13:18:04",
  "detail": { "maxDispensableAmount": 50 }
}
```

---

## Testes

13 testes unitários cobrindo:

- **Saque (8):** greedy, fallback, valor não atendível, saldo insuficiente, caixa vazio, usuário inexistente, valor inválido, débito de saldo
- **Depósito (5):** depósito válido, denominação inválida, todas denominações aceitas, usuário inexistente, incremento de estoque

Para executar:

```bash
./gradlew test
```
