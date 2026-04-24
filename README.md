# Solace Workspace

[![CI](https://github.com/jrodolfo/solace/actions/workflows/ci.yml/badge.svg)](https://github.com/jrodolfo/solace/actions/workflows/ci.yml)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)
[![Java 21](https://img.shields.io/badge/Java-21-007396?logo=openjdk&logoColor=white)](https://openjdk.org/)
[![Spring Boot 3.3](https://img.shields.io/badge/Spring%20Boot-3.3-6DB33F?logo=springboot&logoColor=white)](https://spring.io/projects/spring-boot)
[![React 18](https://img.shields.io/badge/React-18-61DAFB?logo=react&logoColor=black)](https://react.dev/)
[![Vite 6](https://img.shields.io/badge/Vite-6-646CFF?logo=vite&logoColor=white)](https://vite.dev/)
[![Solace](https://img.shields.io/badge/Solace-PubSub%2B-00C895)](https://solace.com/)

This repository contains a small Solace-focused workspace with three active modules:

- [solace-broker-api](solace-broker-api/README.md): Spring Boot backend for publishing, storing, retrying, and querying messages
- [solace-publisher-ui](solace-publisher-ui/README.md): React UI for publishing messages and browsing stored results
- [solace-subscriber](solace-subscriber/README.md): Java command-line subscriber for direct topic traffic

For the repo-level design, see [doc/architecture.md](doc/architecture.md).

## Shared Solace Contract

The backend and subscriber both use the same environment-variable names for broker connectivity:

- `SOLACE_CLOUD_HOST`
- `SOLACE_CLOUD_VPN`
- `SOLACE_CLOUD_USERNAME`
- `SOLACE_CLOUD_PASSWORD`

The UI does not read those variables directly. It talks to `solace-broker-api`, which uses them on the server side.

For the local MySQL runtime used by `solace-broker-api`, including host, port, credentials, and a sample `mysql` command, see [solace-broker-api/README.md](solace-broker-api/README.md).

Sample destinations across the workspace use values like:

```text
solace/java/direct/system-01
```

The subscriber listens to the broader direct topic pattern:

```text
solace/java/direct/system-0*
```

## Recommended Local Workflow

### Root Scripts And Makefile

This repo includes a root `scripts/` folder plus a `Makefile` so you do not have to remember each module’s startup command.

For a folder-local overview of the repo helper scripts, see [scripts/README.md](scripts/README.md).

Preferred local entry points:

- `./scripts/start-all.sh`
- `./scripts/stop-all.sh`
- `./scripts/restart-all.sh`
- `./scripts/status-all.sh`
- or the matching `make` targets from the repo root, such as `make start-all`, `make stop-all`, `make restart-all`, and `make status-all`

The root scripts folder and Makefile also cover:

- per-module build/start commands
- workspace-wide build/test helpers
- root script smoke tests

See [scripts/README.md](scripts/README.md) for the full script inventory and detailed behavior.

The module-level commands below are still useful when you want to run one component directly.

### 1. Start the backend

From `solace-broker-api`:

```bash
mvn spring-boot:run
```

The backend runs on `http://localhost:8081` by default.

### 2. Start the UI

From `solace-publisher-ui`:

```bash
npm install
npm run dev
```

Open `http://localhost:5173`.

### 3. Start the subscriber

From `solace-subscriber`:

```bash
mvn package
java -jar target/solace-subscriber-1.0-SNAPSHOT-all.jar
```

## Module Summary

### `solace-broker-api`

- typed API validation and error responses
- publish lifecycle tracking with `PENDING`, `PUBLISHED`, and `FAILED`
- retry support for failed stored messages
- paginated, filterable, sortable stored-message reads
- normalized read DTOs instead of raw JPA serialization

### `solace-publisher-ui`

- typed publish form instead of a raw JSON textarea
- optional property editing
- paginated stored-message browser
- lifecycle and date-range filter presets
- single-message and bulk retry actions for failed rows

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

## Notes

- Broker setup/how-to material lives under `doc/how-to/`.
- Postman, curl, JMeter, MySQL schema, and sample-message artifacts live under `doc/`.
- The architecture overview lives in [doc/architecture.md](doc/architecture.md).
- If you want details for one module, use that module’s README rather than relying on this root summary.

## Contact

- Software Developer: Rod Oliveira
- GitHub: https://github.com/jrodolfo
- Webpage: https://jrodolfo.net

## License

- MIT License
- Copyright (c) 2026 Rod Oliveira
- See [LICENSE](./LICENSE)
