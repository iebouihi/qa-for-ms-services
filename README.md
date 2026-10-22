# QA for microservices: a reference implementation

A small headless e-commerce (3 Spring Boot services on Kafka + an Angular frontend) built to
show **how a QA / test architect automates a microservice platform end to end**: test layers,
contracts, mocking strategy, environments, test data and CI/CD quality gates.

The business scope is deliberately tiny, three journeys, so the focus stays on the test
architecture:

1. **Customer registration**
2. **Browse products**
3. **Create cart**

→ The full reasoning (diagnosis, decisions, roadmap, KPIs) is in **[docs/test-strategy.md](docs/test-strategy.md)**.

## Architecture

```
            Angular (lazy-loaded domain areas, data-testid selectors)
                                │  /api/*
                       nginx gateway (frontend image)
          ┌─────────────────────┼─────────────────────────┐
          ▼                     ▼                         ▼
  customer-service        product-service            cart-service
  PostgreSQL              PostgreSQL                 PostgreSQL
     │ CustomerRegistered     ▲   GET /api/products/{id}  │
     └──────── Kafka ─────────┼───────────────────────────►│ known_customer projection
                              └────────── REST ────────────┤
                                                           └──► Kafka: CartCreated
```

- **Eventual consistency:** cart-service learns about customers from `customer.registered`,
  so a cart created right after registration may get `422 CUSTOMER_UNKNOWN` (retryable).
- **Price snapshot:** cart-service calls product-service to freeze name and price at creation.
- **Correlation id:** `X-Correlation-Id` flows through HTTP and Kafka headers (`libs/service-observability`).

## Test layers in this repository

| Layer | What it proves | Tools | Location |
|---|---|---|---|
| Unit | Business rules | JUnit 5, Mockito, Vitest | `services/*/src/test/**/*Test.java`, `frontend/src/**/*.spec.ts` |
| Contract | Services still agree (REST **and** Kafka events) | Pact consumer + provider | `*ContractTest.java`, [contracts/](contracts/README.md) |
| Component | One service on real PostgreSQL + Kafka, dependencies virtualized | Testcontainers, WireMock, Awaitility | `services/*/src/test/**/*ComponentIT.java` |
| UI, fake backend | Screens and error states, deterministic | Playwright + in-browser fake | `e2e/tests/mocked` |
| API journeys | The 3 journeys across all services and Kafka | REST Assured, Awaitility | `tests/api-tests` |
| UI journeys | End-to-end wiring | Playwright | `e2e/tests/journeys` |

Reusable building blocks for squads: [`libs/test-support`](libs/test-support) (production-like
containers, Kafka test consumer, unique test data), [`libs/service-observability`](libs/service-observability)
(correlation ids), Playwright [page objects](e2e/pages) and [fake backend](e2e/fixtures/fake-backend.ts).

## Run it

Requirements: Java 21, Maven 3.9, Node 24, Docker.

```bash
# 1. Unit + contract + component tests (Testcontainers starts PostgreSQL and Kafka)
mvn verify

# 2. Frontend unit tests
cd frontend && npm ci && npm test && cd ..

# 3. UI tests against the fake backend (starts the Angular dev server)
cd e2e && npm ci && npx playwright install chromium && npm run test:mocked && cd ..

# 4. Full stack, then the journeys
mvn -DskipTests package
docker compose up -d --build --wait          # http://localhost:8080
mvn -Pjourneys -pl tests/api-tests verify -Dshop.baseUrl=http://localhost:8080
cd e2e && BASE_URL=http://localhost:8080 npm run test:journeys
docker compose down -v
```

## Pipeline

[`ci.yml`](.github/workflows/ci.yml) on every PR:

- **Gate 1** (parallel): backend unit + contract + component · frontend unit + build ·
  UI against fake backend · Kubernetes manifests render (and production has no test data)
- **Gate 2**: the whole stack in an ephemeral environment (docker compose) · API journeys ·
  UI journeys · service logs attached on failure

[`cd.yml`](.github/workflows/cd.yml) is a reference delivery pipeline (disabled until
`DEPLOY_ENABLED` is set): build once, `can-i-deploy`, staging, journeys on staging,
production canary.

## Repository map

```
services/            customer-, product-, cart-service (Spring Boot, Flyway, Kafka)
libs/                shared platform libraries: observability, test-support
frontend/            Angular app + nginx gateway
tests/api-tests/     API journey tests (black box, against a deployed environment)
e2e/                 Playwright: fake-backend UI tests and full-stack journeys
contracts/           OpenAPI + AsyncAPI specs; generated Pact files
test-data/           test data strategy, masking and purge scripts
deploy/              docker-compose support files, Kubernetes base + overlays (preview, staging, production)
docs/                test strategy, decisions, roadmap, KPIs
```
