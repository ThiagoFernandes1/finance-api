# Finance API

API REST para gestão financeira pessoal: contas, lançamentos, orçamentos mensais por categoria e relatórios consolidados.

Construída com **Java 21** e **Spring Boot 3.3**, com autenticação JWT, migrations versionadas, documentação OpenAPI e pipeline de CI.

![CI](https://github.com/ThiagoFernandes1/finance-api/actions/workflows/ci.yml/badge.svg)

---

## Stack

| Camada | Tecnologia |
|---|---|
| Linguagem | Java 21 |
| Framework | Spring Boot 3.3 (Web, Data JPA, Security, Validation, Actuator) |
| Banco | PostgreSQL 16 · H2 nos testes |
| Migrations | Flyway |
| Autenticação | JWT (JJWT 0.12) + BCrypt |
| Documentação | springdoc-openapi (Swagger UI) |
| Testes | JUnit 5, Mockito, AssertJ, MockMvc, JaCoCo |
| Infra | Docker multi-stage, Docker Compose, GitHub Actions |

## Funcionalidades

- **Autenticação e cadastro** — registro com hash BCrypt e emissão de JWT. Novas contas já nascem com 9 categorias padrão, para que o usuário consiga lançar de imediato.
- **Contas** — contas bancárias, carteiras e cartões. O saldo corrente nunca é gravado: é sempre derivado do saldo inicial somado aos lançamentos, mantendo o extrato como única fonte de verdade.
- **Categorias** — classificação tipada (receita ou despesa), com validação que impede excluir categorias já usadas no extrato.
- **Lançamentos** — CRUD completo com filtros combináveis (período, conta, categoria, tipo, busca textual), paginação e ordenação.
- **Orçamentos** — limite mensal por categoria de despesa. A resposta já traz o valor consumido, o saldo restante, o percentual de uso e o status (`ON_TRACK`, `WARNING`, `EXCEEDED`), evitando que o cliente precise cruzar dados de outro endpoint.
- **Relatórios** — consolidação mensal (receitas, despesas, saldo, taxa de poupança e quebra percentual por categoria) e série de fluxo de caixa por mês.

## Decisões de projeto

Algumas escolhas que valem destaque:

**Saldo derivado, não armazenado.** Guardar um campo `balance` mutável na conta cria uma segunda fonte de verdade que diverge do extrato ao primeiro estorno ou edição retroativa. O saldo é calculado por agregação no banco, e o extrato permanece autoritativo.

**Valores sempre positivos com tipo explícito.** `amount` é sempre positivo e a direção vem do enum `TransactionType`. Isso elimina a ambiguidade entre "despesa de -100" e "despesa de 100", que costuma produzir erros de sinal em relatórios.

**Coerência entre lançamento e categoria.** Registrar uma despesa em categoria de receita distorceria silenciosamente todos os relatórios, então a API rejeita a combinação com HTTP 409.

**Isolamento por usuário na consulta.** Toda busca filtra por `user_id` na própria query, em vez de carregar o registro e conferir o dono depois. Um id de outro usuário retorna 404, sem vazar a existência do recurso.

**Schema versionado com validação.** O Flyway é dono do schema e o Hibernate roda com `ddl-auto=validate`. A CI aplica as migrations num PostgreSQL real e sobe o contexto: qualquer divergência entre as entidades e as migrations quebra o build, não a produção.

**401 e 403 com significados distintos.** Sem credenciais, a resposta é 401 (`RestAuthenticationEntryPoint`); autenticado porém sem permissão, 403. O padrão do Spring devolve 403 nos dois casos, o que confunde clientes da API.

## Como executar

### Docker Compose (recomendado)

```bash
docker compose up --build
```

A API sobe em `http://localhost:8080` com o PostgreSQL já provisionado.

### Local

Requer JDK 21 e um PostgreSQL acessível.

```bash
export DB_URL=jdbc:postgresql://localhost:5432/finance
export DB_USER=finance
export DB_PASSWORD=finance
export JWT_SECRET=uma-chave-com-no-minimo-32-bytes

mvn spring-boot:run
```

### Testes

```bash
mvn verify
```

30 testes: unitários para as regras de orçamento e relatório, e de integração cobrindo o fluxo HTTP completo sobre o contexto Spring. O relatório de cobertura fica em `target/site/jacoco/index.html`.

## Documentação da API

Com a aplicação no ar:

- **Swagger UI** — http://localhost:8080/swagger-ui.html
- **OpenAPI JSON** — http://localhost:8080/v3/api-docs
- **Health check** — http://localhost:8080/actuator/health

## Endpoints

| Método | Rota | Descrição |
|---|---|---|
| `POST` | `/api/v1/auth/register` | Cria conta e retorna o token |
| `POST` | `/api/v1/auth/login` | Autentica e retorna o token |
| `GET` | `/api/v1/accounts` | Lista contas com saldo corrente |
| `POST` | `/api/v1/accounts` | Cria conta |
| `GET` `PUT` `DELETE` | `/api/v1/accounts/{id}` | Detalha, atualiza e remove |
| `GET` | `/api/v1/categories` | Lista categorias |
| `POST` | `/api/v1/categories` | Cria categoria |
| `GET` `PUT` `DELETE` | `/api/v1/categories/{id}` | Detalha, atualiza e remove |
| `GET` | `/api/v1/transactions` | Extrato com filtros e paginação |
| `POST` | `/api/v1/transactions` | Registra lançamento |
| `GET` `PUT` `DELETE` | `/api/v1/transactions/{id}` | Detalha, atualiza e remove |
| `GET` | `/api/v1/budgets?month=2026-09` | Orçamentos do mês com consumo |
| `POST` | `/api/v1/budgets` | Define orçamento |
| `GET` `PUT` `DELETE` | `/api/v1/budgets/{id}` | Detalha, atualiza e remove |
| `GET` | `/api/v1/reports/monthly?month=2026-09` | Consolidação do mês |
| `GET` | `/api/v1/reports/cash-flow?start=2026-01&end=2026-09` | Fluxo de caixa mensal |

### Exemplo de uso

```bash
# 1. Cadastro
TOKEN=$(curl -s -X POST http://localhost:8080/api/v1/auth/register \
  -H 'Content-Type: application/json' \
  -d '{"name":"Thiago","email":"thiago@example.com","password":"senhaForte123"}' \
  | jq -r .accessToken)

# 2. Criar conta
curl -X POST http://localhost:8080/api/v1/accounts \
  -H "Authorization: Bearer $TOKEN" -H 'Content-Type: application/json' \
  -d '{"name":"Nubank","institution":"Nu Pagamentos","initialBalance":1000.00}'

# 3. Relatório do mês
curl "http://localhost:8080/api/v1/reports/monthly?month=2026-09" \
  -H "Authorization: Bearer $TOKEN"
```

Resposta do relatório:

```json
{
  "referenceMonth": "2026-09",
  "totalIncome": 7500.00,
  "totalExpense": 1200.00,
  "balance": 6300.00,
  "savingsRate": 84.00,
  "expensesByCategory": [
    { "categoryName": "Alimentacao", "total": 1200.00, "percentage": 100.00 }
  ],
  "incomeByCategory": [
    { "categoryName": "Salario", "total": 7500.00, "percentage": 100.00 }
  ]
}
```

## Arquitetura

Organização em camadas, com dependências apontando sempre para o domínio:

```
com.thiago.financeapi
├── domain
│   ├── model          Entidades JPA e regras invariantes
│   └── repository     Repositórios Spring Data e projeções de agregação
├── application
│   ├── dto            Records de entrada e saída, com Bean Validation
│   └── service        Regras de negócio e orquestração transacional
├── api
│   ├── controller     Controllers REST
│   └── exception      Exceções de negócio e handler global
├── security           JWT, filtro de autenticação e contexto do usuário
└── config             Spring Security e OpenAPI
```

Os controllers não conhecem entidades, apenas DTOs; os services concentram as regras e as transações; o domínio não depende de nada acima dele.

## Tratamento de erros

Todas as respostas de erro seguem o mesmo formato, com a lista de violações preenchida apenas em falhas de validação:

```json
{
  "timestamp": "2026-09-21T14:32:10.482Z",
  "status": 400,
  "error": "Bad Request",
  "message": "Requisicao invalida",
  "path": "/api/v1/transactions",
  "violations": [
    { "field": "amount", "message": "Valor deve ser maior que zero" }
  ]
}
```

| Status | Quando ocorre |
|---|---|
| `400` | Payload inválido ou parâmetro malformado |
| `401` | Token ausente, inválido ou expirado |
| `403` | Autenticado, sem permissão para o recurso |
| `404` | Recurso inexistente ou pertencente a outro usuário |
| `409` | Violação de regra de negócio |

## Variáveis de ambiente

| Variável | Padrão | Descrição |
|---|---|---|
| `DB_URL` | `jdbc:postgresql://localhost:5432/finance` | URL do banco |
| `DB_USER` | `finance` | Usuário do banco |
| `DB_PASSWORD` | `finance` | Senha do banco |
| `JWT_SECRET` | chave de desenvolvimento | Segredo HMAC, mínimo 32 bytes |
| `JWT_EXPIRATION` | `PT8H` | Validade do token (ISO-8601) |
| `SERVER_PORT` | `8080` | Porta da aplicação |

> Em produção, `JWT_SECRET` deve ser obrigatoriamente definido por variável de ambiente. O valor padrão existe apenas para desenvolvimento local.

## CI

O pipeline em `.github/workflows/ci.yml` roda a cada push e pull request na `main`:

1. **Build e testes** — `mvn verify` com cache de dependências, publicando os relatórios de teste e de cobertura como artefatos.
2. **Validação de migrations** — sobe um PostgreSQL 16, aplica as migrations do Flyway e inicializa o contexto com `ddl-auto=validate`, garantindo que o schema e as entidades não divirjam.

## Licença

MIT.
