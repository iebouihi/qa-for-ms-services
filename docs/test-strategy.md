# Test strategy: headless shop on microservices

> Goal: move from a **2-week** release cycle with mostly manual regression to **at least two
> releases a week**, by replacing the manual gate with fast, trustworthy, automated confidence
> at every stage. Not "maximum automation": **maximum confidence per minute of pipeline**.

## 1. Context

| | |
|---|---|
| Services | `customer-service`, `product-service`, `cart-service` (Spring Boot 3, Java 21) |
| Integration | Kafka events (`customer.registered`, `cart.created`) + one sync REST call (cart → product) |
| Frontend | Angular, one lazy-loaded area per domain (micro-frontend ready) |
| Runtime | Kubernetes, managed PostgreSQL (350 GB in production), managed Kafka |
| Environments | dev, staging, production (+ ephemeral per-PR, added by this strategy) |
| Business scope | 3 journeys: customer registration, browse products, create cart |

## 2. Diagnosis: why releasing takes 2 weeks

1. **Manual regression is the release gate.** People replay the same 3 journeys every release.
2. **No safety net between services.** A field renamed in an event breaks cart-service, and nobody knows until staging.
3. **Tests ran on HSQLDB, production runs PostgreSQL.** Dialect, index and migration bugs escape to staging.
4. **One shared staging.** Squads queue for it, its data is inconsistent, it drifts.
5. **No signal after deployment.** Every release feels risky, so releases get batched, which makes them riskier.

## 3. Target test architecture

```
                         ┌──────────────────────────────┐
  production             │ canary + synthetic journeys  │   shift-right
                         └──────────────────────────────┘
                         ┌──────────────────────────────┐
  ephemeral env/staging  │ UI journeys (2)  API (5)     │   few, happy path, real stack
                         └──────────────────────────────┘
                     ┌──────────────────────────────────────┐
  CI, no environment │ UI vs fake backend (10)               │
                     │ component: real PG + Kafka (15)       │   the backbone
                     │ contracts: Pact, 3 interactions       │
                     └──────────────────────────────────────┘
                ┌──────────────────────────────────────────────┐
  CI, ms        │ unit: Java + Angular (20+)                    │   many, fast
                └──────────────────────────────────────────────┘
```

| Layer | Proves | Tool | Where in this repo | Runs | Time |
|---|---|---|---|---|---|
| Unit | Business rules (pricing, validation, state) | JUnit 5, Mockito, Vitest | `*Test.java`, `*.spec.ts` | every commit | seconds |
| Contract, HTTP | cart's expectations of product's API still hold | Pact (consumer + provider) | `ProductClientContractTest` → `ProductApiContractTest` | every PR | seconds |
| Contract, async | the `CustomerRegistered` event still has what cart needs | Pact messages | `CustomerRegisteredContractTest` → `CustomerEventsContractTest` | every PR | seconds |
| Component | one service for real: SQL, migrations, Kafka, serialization, retries, DLT | Testcontainers, WireMock, Awaitility | `*ComponentIT.java` | every PR | ~3 min |
| UI, mocked | screens, states and error handling, including hard-to-reproduce ones | Playwright + in-browser fake backend | `e2e/tests/mocked` | every PR | ~1 min |
| API journeys | the 3 business journeys across all services and Kafka | REST Assured + Awaitility | `tests/api-tests` | ephemeral env, staging | ~1 min |
| UI journeys | the stack is wired end to end | Playwright | `e2e/tests/journeys` | ephemeral env, staging | ~1 min |
| Production | the release behaves under real traffic | canary + synthetic journeys | `cd.yml` (reference) | each deploy | continuous |

### Scenario coverage

| Journey | Unit | Contract | Component | UI mocked | API journey | UI journey |
|---|---|---|---|---|---|---|
| Registration | email normalisation, duplicates | `CustomerRegistered` shape | persisted, event published with correlation id, case-insensitive uniqueness, validation | success, duplicate, invalid form | register + read, duplicate | ✅ (part of the cart journey) |
| Browse | page-size clamp, search rules | product fields cart reads | seeded catalog, search, pagination, inactive hidden, 404 | list, search, empty, service down | list, search, detail | ✅ |
| Create cart | totals, rounding, currency, quantity merge | GET product 200/404 | price snapshot, `CartCreated`, idempotent consumer, poison message → DLT, unknown customer, inactive product, slow dependency → 503 | success, eventual-consistency retry, withdrawn product, sign-in required | cross-service journey, unknown customer | ✅ |

Edge cases live in the lowest layer that can prove them. The top of the pyramid only proves wiring.

## 4. Key decisions (ADR style)

**D1. Test on the production engines.** HSQLDB is removed; component tests run on PostgreSQL 17 and
Kafka through `libs/test-support` (`ProductionLikeContainers`). One shared module means one
place to upgrade images for every squad.

**D2. Contracts are the backbone, not E2E.** Pact makes each service independently releasable:
a provider cannot merge a change that breaks a consumer. Kafka events are contracts too and are
verified the same way. At scale: Pact Broker + `can-i-deploy` gate (see `contracts/README.md`).

**D3. Mocking strategy by test level.**

| Level | Internal dependency (other squad) | External dependency (3rd party) | Own database / Kafka |
|---|---|---|---|
| Unit | Mockito | Mockito | Mockito |
| Component | WireMock stub, kept honest by Pact | WireMock / virtual service from OpenAPI | **real** (Testcontainers) |
| UI mocked | fake backend from API shapes | fake backend | n/a |
| Ephemeral env | **real** | virtual service (Microcks/WireMock) | **real**, throw-away |
| Staging | **real** | provider sandbox | **real**, managed |

Rule: *a mock without a contract is a lie waiting to happen.* Every stub of an internal service
is backed by a Pact verified by the provider.

**D4. Eventual consistency is tested explicitly, never slept on.** Polling with a deadline
(Awaitility, Playwright auto-wait). The API returns a stable, retryable code (`CUSTOMER_UNKNOWN`)
and the frontend retries it; both behaviours are tested.

**D5. Kafka failure modes are first-class tests.** Idempotent consumer (replayed event),
poison message parked on `customer.registered.DLT` without blocking the partition, correlation
id carried in headers.

**D6. Observability is a testability feature.** `libs/service-observability` propagates
`X-Correlation-Id` through HTTP and Kafka automatically. Journey tests name their correlation
ids, so a CI failure leads straight to the right log lines in all three services.

**D7. Stable selectors.** UI tests use `data-testid` only, via page objects. A redesign does not
break the suite.

## 5. Environments

| Environment | Purpose | Data | Dependencies | Lifetime |
|---|---|---|---|---|
| CI (Testcontainers) | unit, contract, component | self-created per test | containers | per build |
| Ephemeral (compose in CI, `overlays/preview` on K8s) | journeys for one PR, no contention | seed catalog + self-created | in-namespace PG + Kafka | per PR |
| Dev | integration of main | seed + self-created | shared | permanent |
| Staging | production-like, release candidate | masked subset + seed | managed PG + Kafka (separate instances) | permanent, changed only by the pipeline |
| Production | real users | real | managed | — |

Everything is code (`docker-compose.yml`, `deploy/k8s`), so environments are reproducible and
drift-free. CI checks that production never enables the `seed` profile.

## 6. Test data

See [`test-data/README.md`](../test-data/README.md): self-created unique data, reference catalog
as code, masked production subset for staging only, nightly purge of synthetic data.
**The 350 GB production database is never copied.**

## 7. Pipeline and quality gates

```
PR ──► Gate 1 (parallel, ~10 min)
        backend: unit + contracts + component (Testcontainers)
        frontend: unit + build
        UI vs fake backend
        manifests render, no seed in production
   ──► Gate 2 (~10 min): ephemeral stack → API journeys + UI journeys
merge ► publish images once ► can-i-deploy ► staging ► journeys on staging
      ► production canary 10% ► synthetic journeys + error budget ► 100% or automatic rollback
```

Flaky tests: one retry in CI, but a test that passes on retry is reported as flaky and
quarantined within a day; target flaky rate < 2%.

## 8. Roadmap (12 weeks)

| Phase | Weeks | Deliverables | Release cadence |
|---|---|---|---|
| 0. Baseline | 1–2 | Inventory of manual checks, metrics baseline (lead time, escaped defects, flaky rate), this strategy agreed | every 2 weeks |
| 1. Quick wins | 3–5 | Testcontainers replaces HSQLDB, shared test-support library, 3 API journeys + UI journeys in the pipeline | weekly |
| 2. Decouple | 6–9 | Pact HTTP + messages, Pact Broker and `can-i-deploy`, ephemeral PR environments, seed data as code, fake backend for UI | weekly, per service |
| 3. Continuous | 10–12 | Canary + synthetic monitoring, masked staging dataset, flaky-test quarantine, manual testing = exploratory only | **2× per week or more** |

**People:** automation QAs own the shared frameworks (`libs/test-support`, Playwright fixtures,
pipeline templates) and coach squads; manual QAs move to scenario design, exploratory testing and
Playwright (pairing); developers own unit, contract and component tests. Quality is a squad
responsibility, not a phase.

## 9. KPIs

| KPI | Today | Target |
|---|---|---|
| Lead time, commit → production | 2 weeks | ≤ 3 days |
| Deployment frequency | every 2 weeks | ≥ 2 per week |
| Manual regression effort | ~100% of releases | 0% (exploratory only) |
| PR pipeline duration | n/a | < 20 min |
| Flaky test rate | unknown | < 2% |
| Change failure rate / escaped defects | baseline | ↓ quarter on quarter |
