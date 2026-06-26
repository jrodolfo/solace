# Solace Workspace

[![CI](https://github.com/jrodolfo/solace/actions/workflows/ci.yml/badge.svg)](https://github.com/jrodolfo/solace/actions/workflows/ci.yml)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)
[![Java 21](https://img.shields.io/badge/Java-21-007396?logo=openjdk&logoColor=white)](https://openjdk.org/)
[![Spring Boot 3.3](https://img.shields.io/badge/Spring%20Boot-3.3-6DB33F?logo=springboot&logoColor=white)](https://spring.io/projects/spring-boot)
[![React 18](https://img.shields.io/badge/React-18-61DAFB?logo=react&logoColor=black)](https://react.dev/)
[![Vite 6](https://img.shields.io/badge/Vite-6-646CFF?logo=vite&logoColor=white)](https://vite.dev/)
[![Solace](https://img.shields.io/badge/Solace-PubSub%2B-00C895)](https://solace.com/)

Solace Workspace is a small full-stack PubSub+ project for publishing messages,
tracking publish lifecycle state, retrying failed sends, and observing direct
topic traffic end to end.

It is organized as three active modules:

- [solace-broker-api](solace-broker-api/README.md): Spring Boot backend for publishing, storing, retrying, reconciling, and querying messages
- [solace-publisher-ui](solace-publisher-ui/README.md): React UI for publishing messages and browsing stored message history
- [solace-subscriber](solace-subscriber/README.md): Java command-line subscriber for direct Solace topic traffic

## What This Project Demonstrates

- Solace PubSub+ publishing and direct topic subscription from Java
- a Spring Boot API that persists publish attempts before broker delivery
- lifecycle tracking with `PENDING`, `PUBLISHED`, `FAILED`, stale-pending detection, retry, and manual reconciliation
- a React/Vite publisher UI with filters, pagination, saved views, retry actions, and export flows
- practical project documentation through architecture notes, ADRs, curl/Postman/JMeter artifacts, and setup walkthroughs

## Shared Solace Contract

The backend and subscriber both use the same environment-variable names for Solace Cloud connectivity:

- `SOLACE_CLOUD_HOST`
- `SOLACE_CLOUD_VPN`
- `SOLACE_CLOUD_USERNAME`
- `SOLACE_CLOUD_PASSWORD`

The UI does not read those variables directly. It talks to `solace-broker-api`, which uses them on the server side.

For a screenshot-based walkthrough that shows how to create a Solace Cloud account, create a demo broker service, find these four values, and register them on Windows, Linux, or macOS, see [docs/how-to/01-solace-cloud-account-demo-and-env-vars.md](docs/how-to/01-solace-cloud-account-demo-and-env-vars.md).

Sample destinations across the workspace are listed in [docs/reference/sample-destinations.md](docs/reference/sample-destinations.md).

## Quick Start

If your goal is to see the project running end to end for the first time, follow
these two screenshot-based guides in order:

1. [Solace Cloud setup guide](docs/how-to/01-solace-cloud-account-demo-and-env-vars.md): create the Solace Cloud account, create a broker service, collect the four `SOLACE_CLOUD_*` values, and register them locally.
2. [Smoke test guide](docs/how-to/06-smoke-test.md): build and start the API, UI, and subscriber, publish a message, and verify it in logs, Solace Cloud, the UI read tab, and MySQL.

The commands below are the Docker-first short version once Solace Cloud is
already configured.

### 1. Configure Solace Cloud

Set the shared Solace environment variables:

```bash
export SOLACE_CLOUD_HOST="tcps://..."
export SOLACE_CLOUD_VPN="..."
export SOLACE_CLOUD_USERNAME="..."
export SOLACE_CLOUD_PASSWORD="..."
```

Use the Solace Cloud setup guide if you need help finding those values: [docs/how-to/01-solace-cloud-account-demo-and-env-vars.md](docs/how-to/01-solace-cloud-account-demo-and-env-vars.md).

### 2. Start the Docker runtime

From the repo root:

```bash
./scripts/docker-start.sh
```

This builds and starts MySQL, the API, the UI, and the subscriber through the
root `docker-compose.yml`. The script prints the API health URL, API docs URL,
publisher UI URL, MySQL port, and log commands.

Common root commands:

- `./scripts/docker-build-all.sh`: build Docker runtime images without starting containers
- `./scripts/docker-status.sh`: show Docker Compose service status and API health
- `./scripts/docker-logs.sh`: follow logs for all Docker services
- `./scripts/docker-logs.sh subscriber`: follow only subscriber logs
- `./scripts/docker-scan.sh`: report Docker runtime image findings with Trivy
- `./scripts/docker-stop.sh`: stop the Docker runtime
- `./scripts/docker-restart.sh`: rebuild and restart the Docker runtime
- `make test`: run API, UI, subscriber, and script tests

For the full script inventory, see [scripts/README.md](scripts/README.md).

### 3. Open the tools

- API health: `http://localhost:8081/rest/actuator/health`
- API docs: `http://localhost:8081/docs`
- Publisher UI: `http://localhost:5173`

### Local Development Workflow

Docker is the preferred runtime workflow. The local scripts are still useful
when you want to run one module directly while developing or debugging.

Start all three local processes:

```bash
./scripts/start-all.sh
```

Local process helpers:

- `./scripts/status-all.sh`: show local API, UI, and subscriber process status
- `./scripts/stop-all.sh`: stop local API, UI, and subscriber processes
- `./scripts/restart-all.sh`: stop, build, and start the local process workflow

Module-level commands:

Backend:

```bash
cd solace-broker-api
mvn spring-boot:run
```

UI:

```bash
cd solace-publisher-ui
npm install
npm run dev
```

Subscriber:

```bash
cd solace-subscriber
mvn package
java -jar target/solace-subscriber-1.0-SNAPSHOT-all.jar
```

## Module Summary

### `solace-broker-api`

- typed API validation and error responses
- publish lifecycle tracking with `PENDING`, `PUBLISHED`, and `FAILED`
- retry support for failed stored messages
- manual reconciliation support for stale `PENDING` messages
- paginated, filterable, sortable stored-message reads
- normalized read DTOs instead of raw JPA serialization

### `solace-publisher-ui`

- typed publish form instead of a raw JSON textarea
- optional property editing
- paginated stored-message browser
- lifecycle and date-range filter presets
- single-message and bulk retry actions for failed rows
- manual reconciliation action for stale pending rows

### `solace-subscriber`

- instance-based connection-property access
- typed configuration failure on missing environment variables
- clearer lifecycle methods and standardized logging

## Continuous Integration

GitHub Actions CI runs on every push and pull request through [.github/workflows/ci.yml](.github/workflows/ci.yml).

The workflow currently covers:

- root script smoke tests
- `solace-broker-api` tests
- `solace-broker-api` build
- `solace-publisher-ui` tests with `npm ci`
- `solace-publisher-ui` build
- `solace-subscriber` tests
- `solace-subscriber` build

## Documentation Map

- [Architecture overview](docs/architecture.md): current module boundaries, publish flow, lifecycle model, retry, reconciliation, and export behavior
- [Architecture walkthrough](docs/architecture-walkthrough.md): concise technical narrative for understanding the design quickly
- [Architecture Decision Records](docs/adr/README.md): why the major design decisions were made
- [Solace Cloud setup guide](docs/how-to/01-solace-cloud-account-demo-and-env-vars.md): account, demo broker, credentials, and OS environment variables
- [Smoke test guide](docs/how-to/06-smoke-test.md): end-to-end validation with API, UI, subscriber, Solace Cloud, logs, and MySQL
- [Fresh clone release validation checklist](docs/validation/fresh-clone-release-checklist.md): repeatable pre-release validation from setup through publish, retry, reconciliation, and shutdown
- [Sample destinations](docs/reference/sample-destinations.md): canonical demo topic names used by docs, tests, and tooling artifacts
- [Broker API README](solace-broker-api/README.md): backend API contract, MySQL runtime, request/response examples, and tuning settings
- [Publisher UI README](solace-publisher-ui/README.md): frontend behavior and development commands
- [Subscriber README](solace-subscriber/README.md): subscriber configuration and runtime behavior
- `docs/curl`, `docs/postman`, and `docs/jmeter`: ready-to-use API exercise artifacts
- `docs/mysql/mysql-schema.sql`: schema reference for the broker API database

## GitHub About

Suggested repository description:

```text
Full-stack Solace PubSub+ workspace with a Spring Boot broker API, React publisher UI, Java subscriber, lifecycle tracking, retry, reconciliation, and architecture documentation.
```

Suggested topics:

```text
solace, pubsub, pubsubplus, spring-boot, java, react, vite, mysql, messaging, event-driven-architecture, rest-api, jms, publisher-subscriber, architecture-decision-records
```

## Contact

- Software Developer: Rod Oliveira
- GitHub: https://github.com/jrodolfo
- Webpage: https://jrodolfo.net

## License

- MIT License
- Copyright (c) 2026 Rod Oliveira
- See [LICENSE](./LICENSE)
