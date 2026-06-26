# Fresh Clone Release Validation Checklist

Use this checklist before creating a release tag or calling the repository ready
for external review. It validates the project from the perspective of a new
developer starting from a clean checkout.

## Scope

This checklist covers:

- setup instructions from the root README
- root build/test scripts
- Docker start, status, logs, and stop scripts
- API health and OpenAPI UI
- publisher UI flow
- subscriber observation
- stored-message browsing
- retry and reconciliation workflows
- documentation/tooling artifacts

## Prerequisites

Confirm the machine has:

- Java 21
- Maven
- Node.js 20 or compatible current LTS
- npm
- Docker with the Compose plugin
- access to a Solace Cloud service or compatible Solace PubSub+ broker

Confirm these environment variables are configured:

```bash
SOLACE_CLOUD_HOST
SOLACE_CLOUD_VPN
SOLACE_CLOUD_USERNAME
SOLACE_CLOUD_PASSWORD
```

Reference setup guide:

- `docs/how-to/01-solace-cloud-account-demo-and-env-vars.md`

Reference sample destinations:

- `docs/reference/sample-destinations.md`

## 1. Fresh Checkout Assumption

Start from a clean checkout or simulate one by removing generated outputs before
the release validation.

Generated paths that must not be required for success:

- `solace-broker-api/target/`
- `solace-subscriber/target/`
- `solace-publisher-ui/dist/`
- `solace-publisher-ui/node_modules/`

Expected result:

- the workspace can be rebuilt without relying on generated files from a previous run
- ignored files such as `.DS_Store`, `target/`, `dist/`, and `node_modules/` are not part of the repository state

## 2. Documentation And Repository Hygiene

Run:

```bash
git status --short
find . -name .DS_Store -print
```

Expected result:

- `git status --short` shows only intentional release-validation notes or is empty
- `find . -name .DS_Store -print` prints nothing

Check:

- root `README.md` quick start is still accurate
- module README links work
- `docs/architecture.md` and `docs/architecture-walkthrough.md` are still current
- `docs/adr/README.md` lists all ADRs

## 3. Automated Validation

Run from the repository root:

```bash
make release-check
```

Expected result:

- root script smoke tests pass
- broker API tests pass
- publisher UI tests pass
- subscriber tests pass
- Docker runtime images build successfully
- Docker image security scan completes successfully

Run the advisory dependency freshness report:

```bash
make dependency-freshness
```

Expected result:

- Maven dependency and plugin update reports are printed for the Java modules
- npm update candidates are printed for the publisher UI when available
- Docker image references are listed and moving tags such as `latest` are flagged
- no dependency files are modified by the report

For the current triage posture, see
`docs/validation/dependency-freshness-triage.md`.

If you need to debug a specific stage, run the component commands directly:

```bash
make test-scripts
make test-api
make test-ui
make test-subscriber
make docker-build-all
make docker-scan
make dependency-freshness
```

Expected result:

- each component command passes independently

Run the module builds:

```bash
make build-api
make build-ui
make build-subscriber
```

Expected result:

- all modules build successfully
- `build-ui` installs dependencies automatically if `node_modules` is missing

## 4. Docker Image Build

The `make release-check` gate already builds the Docker runtime images. To run
this step by itself:

```bash
./scripts/docker-build-all.sh
```

Expected result:

- MySQL image is pulled
- broker API image builds successfully
- publisher UI image builds successfully
- subscriber image builds successfully
- no containers are started by this step

## 5. Docker Runtime Startup

Run:

```bash
./scripts/docker-start.sh
```

Expected result:

- MySQL starts successfully
- API starts successfully
- UI starts successfully
- subscriber starts successfully
- startup output includes API health, API docs, UI, MySQL, and log commands

Then run:

```bash
./scripts/docker-status.sh
```

Expected result:

- Docker Compose shows MySQL, API, UI, and subscriber services running
- API health is `UP`

## 6. API Health And Documentation

Open or request:

```text
http://localhost:8081/rest/actuator/health
http://localhost:8081/docs
```

Expected result:

- actuator health endpoint returns healthy status
- OpenAPI/Swagger UI loads
- documented API paths match the current broker API

## 7. Docker Image Security Scan

The `make release-check` gate already runs the Docker image security report. To
run this step by itself:

```bash
./scripts/docker-scan.sh
```

Expected result:

- MySQL, API, UI, and subscriber images are scanned with Trivy
- `LOW`, `MEDIUM`, `HIGH`, and `CRITICAL` findings are visible for review
- the script does not fail on vulnerability findings
- accepted local infrastructure findings, such as MySQL image findings, are
  documented in the release notes or follow-up issues

## 8. Publisher UI Publish Flow

Open the Docker publisher UI URL:

```text
http://localhost:5173
```

Publish one message with:

```text
destination: solace/java/direct/system-01
deliveryMode: PERSISTENT or DIRECT
payload.type: BINARY, TEXT, JSON, or XML
```

Expected result:

- UI validates the form before submission
- publish request succeeds
- response output shows the backend response
- stored-message browser can load the new row
- stored row has current lifecycle fields such as `publishStatus`, `stalePending`, `retrySupported`, and timestamps

## 9. Subscriber Observation

While the subscriber is running, confirm its logs show traffic for:

```text
solace/java/direct/system-0*
```

Expected result:

- publishing to `solace/java/direct/system-01` is visible in subscriber logs
- subscriber continues running after the message is observed
- no unexpected discard or reconnection warnings appear during normal validation

Recommended command:

```bash
./scripts/docker-logs.sh subscriber
```

Docker Desktop path:

```text
Containers > solace > solace-subscriber > Logs
```

## 10. Stored-Message Browser

In the UI, validate:

- load stored messages
- filter by `PUBLISHED`
- filter by destination
- filter by inner message id
- use lifecycle/date presets
- export current page as JSON
- export current page as CSV
- export full filtered results as JSON
- export full filtered results as CSV
- save, load, rename, export, import, and delete a browser view

Expected result:

- browser filters produce the expected query
- exports contain the expected stored-message shape
- saved views remain browser-local and do not require backend persistence

## 11. Retry Workflow

Create or identify a retryable `FAILED` stored message.

Validate:

- single-message retry
- visible-page bulk retry
- mixed-result bulk retry behavior if possible

Expected result:

- retryable failed rows expose retry actions
- successful retry changes lifecycle state to `PUBLISHED`
- non-retryable or non-failed rows are skipped with clear result details
- oversized bulk retry requests are rejected by the backend limit

## 12. Stale Pending Reconciliation

Create or identify a stale `PENDING` stored message.

Validate:

- stale pending filter
- manual reconciliation action
- rejection for non-stale or non-`PENDING` rows

Expected result:

- stale pending rows expose reconciliation action
- reconciliation marks the stored row as `FAILED`
- failure reason clearly says the row was manually reconciled
- rows that are not eligible are rejected with a clear error

## 13. Tooling Artifacts

Validate at least one non-UI API exercise path:

- run a representative curl command from `docs/curl/curl.txt`
- open or import `docs/postman/solace-producer-emulator.postman_collection.json`
- open `docs/jmeter/solace-producer-emulator.jmx` if JMeter is available

Expected result:

- request examples use the current wrapped request shape
- payload enum examples use uppercase values such as `BINARY`
- response examples align with current stored-message DTO fields

## 14. Shutdown

Stop the workspace:

```bash
./scripts/docker-stop.sh
./scripts/docker-status.sh
```

Expected result:

- Docker Compose services are stopped or absent
- API health is no longer reachable
- no unexpected project containers remain running

## 15. Optional Local Process Workflow

Use this only when validating the secondary local development workflow:

```bash
./scripts/start-all.sh
./scripts/status-all.sh
./scripts/stop-all.sh
```

Expected result:

- API, UI, and subscriber run as local processes
- `status-all.sh` reports local status accurately
- `stop-all.sh` stops local API, UI, and subscriber processes

## Release Readiness Result

Record the result before tagging:

```text
date:
commit:
validator:
result: pass / fail
notes:
```

Only tag a release when failed items are either fixed or explicitly accepted as
known limitations.
