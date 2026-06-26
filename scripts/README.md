# Root Scripts

This folder contains the repo-level helper scripts for the workspace.

These scripts are wrappers around the three active modules:

- `solace-broker-api`
- `solace-publisher-ui`
- `solace-subscriber`

They are meant to give you a small, predictable operator workflow from the repo root.
The primary runtime workflow is Docker. The local process scripts remain useful
for module-level development and debugging.

## Supported Shells

The scripts support:

- macOS/Linux Bash
- Windows Git Bash, with PowerShell available on `PATH`

On macOS/Linux, process discovery uses standard tools such as `lsof`, `pgrep`, and `ps`.
On Windows Git Bash, `stop-all.sh` and `status-all.sh` use PowerShell process APIs instead of `lsof`, because `lsof` is not normally available in Git Bash.

## Docker Runtime Scripts

- `docker-build-all.sh`
  Pulls or builds all images used by the Docker runtime without starting
  containers. It pulls MySQL, builds the API and subscriber images, and rebuilds
  the UI image with no cache so browser assets reflect current source.

- `docker-start.sh`
  Prepares images with `docker-build-all.sh`, then starts the full Docker
  runtime: MySQL, broker API, publisher UI, and subscriber. It validates the
  shared Solace environment variables before starting the stack.

- `docker-stop.sh`
  Stops the full Docker runtime with `docker compose down`.

- `docker-status.sh`
  Shows Docker Compose service status and checks the broker API health endpoint.

- `docker-restart.sh`
  Runs `docker-stop.sh` and `docker-start.sh` in sequence.

- `docker-logs.sh`
  Follows logs for the full Docker runtime or one component:
  `api`, `ui`, `subscriber`, or `mysql`.

- `docker-scan.sh`
  Scans the Docker runtime images with Trivy. Default mode fails on fixed
  `HIGH` or `CRITICAL` vulnerabilities. `--full` reports all severities without
  failing.

## Local Build Scripts

- `build-broker-api.sh`
  Builds `solace-broker-api` with Maven.

- `build-publisher-ui.sh`
  Builds `solace-publisher-ui` with `npm run build`.
  If `node_modules` is missing, it runs `npm install` first.

- `build-subscriber.sh`
  Builds `solace-subscriber` with Maven.

- `build-all.sh`
  Runs the three build scripts in sequence.

Build scripts only compile and package the modules. They do not start the full
Docker runtime.

## Local Start Scripts

- `start-broker-api.sh`
  Starts `solace-broker-api` with `mvn spring-boot:run`.
  Requires the shared Solace environment variables.

- `start-publisher-ui.sh`
  Starts the Vite dev server for `solace-publisher-ui`.
  If `node_modules` is missing, it runs `npm install` first.

- `start-subscriber.sh`
  Builds and starts `solace-subscriber`.
  Requires the shared Solace environment variables.

- `start-all.sh`
  Starts API, UI, and subscriber together.
  It validates the shared Solace environment variables first, streams prefixed logs, and writes a temporary combined-log directory under `${TMPDIR:-/tmp}/solace-start-all.XXXXXX`.
  The latest generated log directory path is written to `${TMPDIR:-/tmp}/solace-start-all.latest` so `status-all.sh` can report the Vite URL selected during startup.
  Once the API and UI are ready, it also prints a clearer readiness block with their URLs.

When the API starts through the local process workflow, Spring Boot Docker
Compose can recreate and start the local MySQL container from
`solace-broker-api/docker-compose.yaml`.

## Local Stop / Restart / Status

- `stop-all.sh`
  Stops the locally detected API, UI, and subscriber processes when they are running.
  On Windows Git Bash, this uses PowerShell to find and stop the matching Windows processes.

- `restart-all.sh`
  Runs `stop-all.sh`, `build-all.sh`, and `start-all.sh` in sequence.

- `status-all.sh`
  Reports local workspace status for API, UI, and subscriber.
  On Windows Git Bash, this uses PowerShell to inspect listening ports and matching Windows processes.

  Status model:
  - API:
    checked via `http://localhost:8081/rest/actuator/health`
  - UI:
    checked via matching Vite process plus detected listening ports, and when available the latest `start-all.sh` UI log is used to show the last known Vite `Local:` URL
  - subscriber:
    checked via matching Java process

## Shared Helper

- `common.sh`
  Shared helper functions used by the other scripts.
  This is not meant to be run directly.

## Script Smoke Test

- `test-scripts.sh`
  Runs lightweight smoke checks for the root scripts and their Makefile wiring.

## Recommended Entry Points

If you want the smallest useful set to remember:

- start Docker runtime:
  `./scripts/docker-start.sh`
- build Docker runtime images:
  `./scripts/docker-build-all.sh`
- follow Docker runtime logs:
  `./scripts/docker-logs.sh`
- follow subscriber logs:
  `./scripts/docker-logs.sh subscriber`
- scan Docker images:
  `./scripts/docker-scan.sh`
- check Docker runtime status:
  `./scripts/docker-status.sh`
- stop Docker runtime:
  `./scripts/docker-stop.sh`
- restart Docker runtime:
  `./scripts/docker-restart.sh`

The same workflows are also exposed through the root `Makefile`.

## Detailed Notes

- `docker-build-all.sh` pulls MySQL and builds the application images from the
  root Docker Compose stack without starting containers.
- `docker-start.sh` prepares images with `docker-build-all.sh`, then starts the
  root Docker Compose stack, including MySQL, `solace-broker-api`,
  `solace-publisher-ui`, and `solace-subscriber`.
- `docker-build-all.sh` rebuilds the API and subscriber images and uses a no-cache
  build for the UI image so browser assets reflect the current source.
- `docker-logs.sh` is the recommended way to verify subscriber activity after a
  publish; Docker Desktop can also show logs under
  `Containers > solace > solace-subscriber > Logs`.
- `docker-status.sh` shows Compose service status and checks
  `http://localhost:8081/rest/actuator/health`.
- `docker-scan.sh` scans `mysql:8.4`, `solace-broker-api:local`,
  `solace-publisher-ui:local`, and `solace-subscriber:local`.
  Run `./scripts/docker-build-all.sh` or `./scripts/docker-start.sh` first so
  the images exist locally.
- `docker-scan.sh --full` is useful for investigation because it includes
  `LOW` and `MEDIUM` findings and does not fail the script.
- `build-broker-api.sh` runs `mvn clean package` inside `solace-broker-api`.
- `build-subscriber.sh` runs `mvn clean package` inside `solace-subscriber`.
- `build-publisher-ui.sh` runs `npm run build` inside `solace-publisher-ui` and runs `npm install` first when `node_modules` is missing.
- `build-all.sh` runs the three module builds sequentially from the repo root.
- `start-broker-api.sh` and `start-subscriber.sh` require the shared Solace environment variables.
- `start-publisher-ui.sh` starts the Vite dev server and runs `npm install` first when `solace-publisher-ui/node_modules` is missing.
- `start-all.sh` validates the shared Solace environment variables before starting any child processes, streams prefixed `[api]`, `[ui]`, and `[subscriber]` logs from a temporary combined-log directory, starts API runtime dependencies such as the local MySQL Docker container through Spring Boot Docker Compose, writes the latest log directory pointer for `status-all.sh`, prints a clear readiness block with the API and UI URLs once they are up, and prints a status summary when it stops.
- `stop-all.sh` sends `TERM` to the locally detected API, UI, and subscriber processes when they are running and reports which components were stopped versus already down.
- `restart-all.sh` runs `stop-all.sh`, `build-all.sh`, and `start-all.sh` in that order with clear step separators and fails fast if any step fails.
- `status-all.sh` reports local status for the three components using a hybrid model:
  - API via `http://localhost:8081/rest/actuator/health`
  - UI via the last known `start-all.sh` Vite `Local:` URL when available, otherwise matching Vite process plus detected listening port(s)
  - subscriber via matching Java process
- the Vite UI port is dynamic when `5173` is busy; `status-all.sh` reuses the latest `start-all.sh` UI log when available so it can show the actual Vite URL more explicitly.
- if `subscriber` exits, `start-all.sh` prints a large warning block and intentionally leaves `api` and `ui` running.
- if `api` or `ui` exits, `start-all.sh` still stops the remaining processes intentionally and reports which component failed plus where the combined logs were captured.
