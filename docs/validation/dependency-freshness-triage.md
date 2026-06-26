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

Completed in the 2026-06-26 routine maintenance batch:

- `com.solace:solace-messaging-client` `1.9.0 -> 1.10.0`
- `io.opentelemetry.semconv:opentelemetry-semconv` `1.41.1 -> 1.42.0`
- `org.projectlombok:lombok` to `1.18.46`
- `com.h2database:h2` `2.3.232 -> 2.4.240`
- `maven-compiler-plugin` `3.11.0 -> 3.15.0`
- `maven-jar-plugin` `3.3.0 -> 3.5.0`
- `maven-shade-plugin` `3.6.0 -> 3.6.2`
- npm packages where `Wanted` stayed within the current package.json ranges,
  including Axios, Bootstrap, `ws`, Vite 6, Vitest 3, TypeScript 5, ESLint 9,
  Testing Library, and React 18 typings
- removed unused runtime dependency `@jest/globals` so runtime audit results do
  not include Jest-only transitive dependencies

Validation completed for this batch:

```bash
make test-ui
make build-ui
make test-api
make test-subscriber
npm audit --omit=dev
make dependency-freshness
```

`npm audit --omit=dev` reports zero runtime vulnerabilities after this batch.

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
- Maven Surefire `3.5.2 -> 3.6.0-M1`

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

The Docker build previously reported npm audit findings for the UI dependency
tree:

- 22 total vulnerabilities
- 3 low
- 8 moderate
- 8 high
- 3 critical

The 2026-06-26 routine dependency batch resolved runtime audit exposure:

```bash
npm audit --omit=dev
```

now reports zero vulnerabilities. Remaining full `npm audit` findings are
development-tooling transitive dependencies and should be handled separately
from runtime dependency risk.

The Docker image scan completed successfully under the current policy of
reporting findings without failing. It reported:

- `solace-publisher-ui:local`: 0 vulnerabilities
- `solace-broker-api:local`: 0 vulnerabilities after refreshing Alpine
  `p11-kit` / `p11-kit-trust` to `0.26.2-r0` during the image build
- `solace-subscriber:local`: 0 vulnerabilities after the same Alpine package
  refresh
- `mysql:8.4`: local infrastructure findings in Oracle Linux / bundled Python
  packages

The Java application WAR/JAR contents did not report application dependency
vulnerabilities in the Trivy scan.

## Current Recommendation

For the next maintenance pass:

1. Treat remaining Maven and npm freshness output as compatibility-migration
   work, not routine freshness work.
2. Review full `npm audit` dev-tooling findings separately from runtime
   exposure.
3. Keep watching Docker base images for upstream refreshes. The Java runtime
   images currently refresh Alpine `p11-kit` / `p11-kit-trust` during the image
   build because the fixed packages are available in the pinned Alpine 3.23
   repositories before they are present in the Temurin base layer.

Do not combine the Spring Boot 4 or React 19 migrations with routine freshness
work.
