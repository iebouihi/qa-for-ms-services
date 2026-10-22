# Test data strategy

Production holds 350 GB. Tests never need it, and copying it would be slow, costly and a
GDPR risk. Instead, three tiers, each with a clear purpose and owner.

| Tier | What | Where it is used | Lives in |
|---|---|---|---|
| 1. Self-created data | Each test creates the customer/cart it needs through the API, with unique values (`test-<random>@example.test`) | Component, contract, API journeys, E2E | The tests themselves (`TestData`, `JourneySupport`, Playwright fixtures) |
| 2. Reference data as code | The product catalog, fixed ids, versioned and reviewed like code | CI, ephemeral PR environments, staging | `services/product-service/src/main/resources/db/seed` (Flyway, `seed` profile only) |
| 3. Masked production subset | A few GB of realistic, anonymised data | Staging only: volume, performance, exploratory testing | `masking/` scripts, monthly refresh |

## Rules

1. **A test owns its data.** No shared fixtures, no "use customer 42". Tests run in parallel and in any order.
2. **Synthetic data is recognisable.** The `test-` prefix and the reserved `.test` domain make it easy to purge (`purge/`).
3. **No personal data outside production.** The subset is masked inside the production perimeter, deterministically (joins across services still match) and irreversibly.
4. **Production never loads seed data.** The `seed` Spring profile is enabled in compose, preview and staging overlays only.
5. **Eventual consistency is part of the data contract.** A customer created through the API reaches cart-service via Kafka; tests poll with a deadline, never sleep.
