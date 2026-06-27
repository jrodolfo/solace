# Spring Boot 4 Readiness

Date: 2026-06-26

Branch:

```bash
spring-boot-4-readiness
```

## Scope

This readiness pass moves the broker API module from the Spring Boot 3.5 line to
the Spring Boot 4.1 line and records the compatibility changes needed for the
current codebase to compile and test cleanly.

The migration is intentionally isolated to the API module. The publisher UI,
subscriber module, Docker release workflow, and full repository release gate
still need separate validation before this branch is treated as release-ready.

## Baseline

Before the Spring Boot 4 changes, the API test suite passed on Spring Boot
`3.5.16`:

```bash
make test-api
```

Result:

- 95 tests passed

## Compatibility Changes

The API module now uses:

- Spring Boot `4.1.0`
- Spring Framework `7.0.8`
- Spring Data JPA `4.1.0`
- Hibernate ORM `7.4.1.Final`
- JUnit Jupiter `6.0.3`
- Testcontainers `2.0.5`
- SpringDoc `3.0.3`
- Hibernate Validator `9.1.1.Final`

Required project changes:

- remove the old Jackson 2 BOM override because Spring Boot 4 manages separate
  Jackson 2 and Jackson 3 BOMs
- add `spring-boot-jackson2` so existing `com.fasterxml.jackson.databind`
  object mapper usage continues to receive Boot auto-configuration
- add the modular Spring Boot test dependencies for Web MVC and Data JPA test
  slices
- switch Testcontainers artifacts to the Spring Boot 4 managed artifact names
- move Web MVC and Data JPA test annotation imports to their Spring Boot 4
  packages
- replace deprecated Spring Boot `@MockBean` test usage with Spring Framework
  `@MockitoBean`
- initialize dynamic JPA specifications with `Specification.unrestricted()`
  instead of `Specification.where(null)`

## Validation

Run from the broker API module after the migration:

```bash
mvn clean test
```

Result:

- 95 tests passed
- 0 failures
- 0 errors

Run from the repository root after the migration:

```bash
make test-api
```

Result:

- 95 tests passed
- 0 failures
- 0 errors

The clean test run matters for this migration because stale classes can hide
test-slice package moves and API changes after a framework major-version jump.

## Remaining Work

Before merging this branch back to `main`:

1. Run the full release gate:

   ```bash
   make release-check
   ```

2. Smoke-test the Docker runtime if the release gate passes:

   ```bash
   ./scripts/docker-start.sh
   ./scripts/docker-status.sh
   ./scripts/docker-stop.sh
   ```

Treat any Docker, OpenAPI UI, or runtime behavior differences as Spring Boot 4
follow-up work rather than routine dependency freshness work.
