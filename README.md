# Solace Workspace

This repository contains a small Solace-focused workspace with three active modules:

- [solace-broker-api](/Users/jrodolfo/workspace/solace/solace/solace-broker-api/README.md:1): Spring Boot backend for publishing and storing messages
- [solace-publisher-ui](/Users/jrodolfo/workspace/solace/solace/solace-publisher-ui/README.md:1): React UI for publishing and browsing stored messages
- [solace-subscriber](/Users/jrodolfo/workspace/solace/solace/solace-subscriber/README.md:1): Java command-line subscriber for direct topic traffic

## Shared Solace Contract

The backend and subscriber both use the same environment-variable names for broker connectivity:

- `SOLACE_CLOUD_HOST`
- `SOLACE_CLOUD_VPN`
- `SOLACE_CLOUD_USERNAME`
- `SOLACE_CLOUD_PASSWORD`

The UI does not read those variables directly. It talks to `solace-broker-api`, which uses them on the server side.

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

This repo now includes a root `scripts/` folder plus a `Makefile` so you do not have to remember each module’s startup command.

Available scripts:

- `scripts/start-broker-api.sh`
- `scripts/start-publisher-ui.sh`
- `scripts/start-subscriber.sh`
- `scripts/start-all.sh`

Available `make` targets:

- `make help`
- `make start-api`
- `make start-ui`
- `make start-subscriber`
- `make start-all`
- `make test-scripts`
- `make test`

Notes:

- `start-broker-api.sh` and `start-subscriber.sh` require the shared Solace env vars.
- `start-publisher-ui.sh` expects `solace-publisher-ui/node_modules` to already exist. If not, run `cd solace-publisher-ui && npm install` once first.
- `start-all.sh` runs all three modules together in the current terminal and stops them on `ctrl-c`.

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
java -jar target/solace-subscriber-1.0-SNAPSHOT.jar
```

## Module Summary

### `solace-broker-api`

- typed API validation and error responses
- paginated, filterable, sortable stored-message reads
- documented OpenAPI responses and examples
- no longer persists request connection credentials

### `solace-publisher-ui`

- typed publish form instead of a raw JSON textarea
- optional property editing
- paginated stored-message browser
- detail expansion, friendly timestamps, refresh/reset, and copy actions

### `solace-subscriber`

- instance-based connection-property access
- typed configuration failure on missing environment variables
- clearer lifecycle methods and standardized logging
- unit tests for config and message-state behavior

## Verification Commands

- Backend: `cd solace-broker-api && mvn test`
- UI: `cd solace-publisher-ui && npm test -- --run`
- Subscriber: `cd solace-subscriber && mvn test`
- Root script smoke tests: `make test-scripts`
- Whole workspace: `make test`

## Continuous Integration

GitHub Actions CI now runs on every push and pull request through [.github/workflows/ci.yml](/Users/jrodolfo/workspace/solace/solace/.github/workflows/ci.yml:1).

The workflow currently covers:

- root script smoke tests
- `solace-broker-api` tests
- `solace-publisher-ui` tests with `npm ci`
- `solace-subscriber` tests

## Notes

- Broker setup/how-to material lives under `solace-broker-api/doc/how-to/`.
- Postman, curl, JMeter, and sample-message artifacts still live under `solace-broker-api/doc/`.
- If you want details for one module, use that module’s README rather than relying on this root summary.
