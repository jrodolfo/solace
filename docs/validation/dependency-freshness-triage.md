# Dependency Freshness Triage

Date: 2026-06-26

Source command:

```bash
make dependency-freshness
```

The freshness report is advisory. It reports available Maven, npm, and Docker
image updates without changing project files.

## Current Read

The report is intentionally noisy. Treat it as maintenance radar, not as an
automatic upgrade instruction.

## Safe Routine Candidates

These updates look suitable for small, focused follow-up batches because they
stay within the current architecture and major-version posture:

- `com.solace:solace-messaging-client` `1.9.0 -> 1.10.0`
- `io.opentelemetry.semconv:opentelemetry-semconv` `1.41.1 -> 1.42.0`
- `org.projectlombok:lombok` to `1.18.46`
- `com.h2database:h2` `2.3.232 -> 2.4.240`
- `maven-compiler-plugin` `3.11.0 -> 3.15.0`
- `maven-jar-plugin` `3.3.0 -> 3.5.0`
- `maven-shade-plugin` `3.6.0 -> 3.6.2`
- npm packages where `Wanted` stays within the current package.json ranges,
  such as Axios, Bootstrap, Vite 6, TypeScript 5, ESLint 9, Testing Library,
  and React 18 typings

Validation for these updates should include:

```bash
make test-api
make test-subscriber
make test-ui
make build-ui
```

## Compatibility-Risk Candidates

Defer these unless there is a deliberate migration task:

- Spring Boot `3.5.16 -> 4.1.0`
- Spring Framework / Spring Data / Spring Security managed dependency jumps to
  the Spring 7 generation
- React `18.3.1 -> 19.2.7`
- Jest `29 -> 30`
- Vitest `3 -> 4`
- TypeScript `5 -> 6`
- JUnit Jupiter `5.11.4 -> 6.1.0`
- SpringDoc `2.8.17 -> 3.0.3`, unless paired with Spring Boot 4 compatibility
  review
- Hibernate Validator `8.0.1.Final -> 9.1.1.Final`
- Log4j `2.26.0 -> 3.0.0-beta*`
- Netty `4.2.15.Final -> 5.0.0.Alpha2`

These are major-version or prerelease changes and need compatibility review,
not a freshness-only upgrade.

## Docker Image Review

The report found all runtime image references are pinned and registry metadata
was available for:

- `eclipse-temurin:21-jre-alpine`
- `maven:3.9.9-eclipse-temurin-21`
- `mysql:8.4`
- `nginx:1.31.2-alpine3.23`
- `node:20-bookworm-slim`

Project-local images are intentionally tagged with `:local`:

- `solace-broker-api:local`
- `solace-publisher-ui:local`
- `solace-subscriber:local`

Docker tag freshness still needs periodic human review because the script only
checks pinning and registry availability. It does not choose newer image tags.

## Release-Check Observations

`make release-check` passed on 2026-06-26.

The Docker build reported npm audit findings for the current UI dependency
tree:

- 22 total vulnerabilities
- 3 low
- 8 moderate
- 8 high
- 3 critical

Treat this as a separate security-maintenance task from general freshness
triage. Start with `npm audit --omit=dev` to separate runtime exposure from
development-only tooling exposure before applying fixes.

The Docker image scan completed successfully under the current policy of
reporting findings without failing. It reported:

- `solace-publisher-ui:local`: 0 vulnerabilities
- `solace-broker-api:local`: 2 high Alpine findings in `p11-kit` /
  `p11-kit-trust`, fixed by Alpine package `0.26.2-r0`
- `solace-subscriber:local`: the same 2 high Alpine findings in `p11-kit` /
  `p11-kit-trust`
- `mysql:8.4`: local infrastructure findings in Oracle Linux / bundled Python
  packages

The Java application WAR/JAR contents did not report application dependency
vulnerabilities in the Trivy scan.

## Current Recommendation

For the next maintenance pass, prefer one small routine batch:

1. Java patch/minor updates for Solace client, OpenTelemetry semconv, Lombok,
   H2, and stable Maven plugins.
2. npm `Wanted` updates that remain within existing major versions.
3. Docker base-image review only after the Java and npm batch is green.

Do not combine the Spring Boot 4 or React 19 migrations with routine freshness
work.
