# Architecture

## Overview

This repository is organized as a small three-module Solace workspace:

- `solace-publisher-ui`: browser-based operator UI
- `solace-broker-api`: HTTP API for publish, persistence, retry, and querying
- `solace-subscriber`: direct Solace subscriber for topic traffic

At a high level:

1. the UI sends publish and query requests to `solace-broker-api`
   other HTTP clients such as Postman, JMeter, or custom tools can call the same broker API endpoints directly
2. the broker API persists publish attempts and sends messages to Solace
3. the subscriber independently listens to Solace topics and logs inbound traffic

![Solace workspace architecture](./solace.png)

Related reading:

- concise walkthrough: [architecture-walkthrough.md](./architecture-walkthrough.md)
- decision history: [adr/README.md](./adr/README.md)

## Module Responsibilities

### `solace-publisher-ui`

Owns:

- typed publish form
- client-side validation
- filterable, paginated stored-message browser
- lifecycle summaries and page navigation
- single-message retry actions for failed rows
- manual reconciliation action for stale pending rows

Does not own:

- direct Solace connectivity
- server-side credentials
- message persistence

### `solace-broker-api`

Owns:

- HTTP publish endpoint
- publish lifecycle tracking
- retry endpoint
- persistence to the database
- paginated and filtered read API
- typed success and error responses

Does not own:

- direct browser rendering
- long-running subscription behavior

The broker API is not frontend-exclusive. It can be called by `solace-publisher-ui`, Postman, JMeter, or any other HTTP client that speaks the documented request contract.

Related ADRs:

- [ADR-0001](./adr/0001-use-a-three-module-solace-workspace.md)
- [ADR-0004](./adr/0004-provide-a-react-publisher-ui-in-addition-to-api-clients.md)
- [ADR-0010](./adr/0010-keep-postman-jmeter-and-curl-artifacts-alongside-the-codebase.md)

### `solace-subscriber`

Owns:

- direct Solace subscription
- reconnect/discard logging
- runtime visibility into inbound topic traffic

Does not own:

- message persistence
- publish lifecycle state
- browser/API read workflows

## Publish Flow

The normal publish flow is:

1. `solace-publisher-ui` sends `POST /api/v1/messages/message`
2. `solace-broker-api` saves the request as `PENDING`
3. `solace-broker-api` attempts broker publish
4. if publish succeeds:
   - the record becomes `PUBLISHED`
   - `publishedAt` is set
   - the API returns a typed success DTO
5. if publish fails:
   - the record becomes `FAILED`
   - `failureReason` is set
   - the API returns a typed error response

Today this lifecycle update is synchronous within the request/response path.
The record is written as `PENDING` before the publish attempt so the system
retains evidence of the requested operation even if the process crashes or the
outcome cannot be finalized cleanly afterward.

Important boundary:

- user-supplied broker credentials may be accepted on publish requests
- those connection parameters are not persisted with the stored message

Related ADRs:

- [ADR-0003](./adr/0003-persist-publish-attempts-and-track-message-lifecycle.md)
- [ADR-0005](./adr/0005-allow-per-request-broker-credentials-without-persisting-them.md)
- [ADR-0006](./adr/0006-use-synchronous-publish-with-pending-first-persistence.md)

## Persistence Model And Lifecycle

Stored messages represent publish attempts, not only successful publishes.

Each stored message can include:

- `publishStatus`
- `failureReason`
- `publishedAt`
- `stalePending` as a derived operational flag
- `innerMessageId` as descriptive payload metadata
- `payload.type` as an application enum with `TEXT`, `BINARY`, `JSON`, or `XML`
- normalized `properties` as a key/value map in API responses
- payload plus audit timestamps

Lifecycle states:

- `PENDING`: accepted and awaiting broker outcome finalization; in the current implementation this is a short-lived synchronous state
- `PUBLISHED`: successfully published
- `FAILED`: publish attempt failed

Stale pending signal:

- `stalePending` is derived when a message is still `PENDING` longer than the configured stale-pending threshold after `createdAt`
- this does not change the stored `publishStatus` by itself
- the default `PT5M` value is a current operational heuristic, not a broker-level guarantee
- it exists to highlight rows that may need operator review after a crash, timeout, or publish/database inconsistency

`innerMessageId` is not used as a database or API uniqueness constraint. Multiple stored publish attempts may carry the same `innerMessageId`, and the persisted record `id` remains the actual stored-message identity for lifecycle and retry operations.

Related ADRs:

- [ADR-0003](./adr/0003-persist-publish-attempts-and-track-message-lifecycle.md)
- [ADR-0006](./adr/0006-use-synchronous-publish-with-pending-first-persistence.md)

## Retry Flow

Retry is handled by:

- `POST /api/v1/messages/{messageId}/retry` for one stored message
- `POST /api/v1/messages/retry` for batch retry by id list

Rules:

- only `FAILED` messages can be retried
- the retry uses server-side Solace configuration
- the same stored message record is updated again

Retry sequence:

1. load stored message
2. reject unless status is `FAILED`
3. mark it `PENDING`
4. attempt publish again
5. mark it `PUBLISHED` or `FAILED`

The UI exposes single-message retry for retryable failed messages.
The backend also exposes batch retry by id list and enforces `app.retry.max-batch-size` to prevent accidental large retry bursts for API clients.

Related ADR:

- [ADR-0009](./adr/0009-support-retry-at-the-stored-message-level.md)

## Stale Pending Reconciliation Flow

Manual reconciliation is handled by `POST /api/v1/messages/{messageId}/reconcile-stale-pending`.

Rules:

- only `PENDING` messages can be reconciled
- the message must be stale under the same configured threshold used by the read DTO
- reconciliation does not attempt another broker publish
- the same stored message record is updated to `FAILED`
- `failureReason` is set to an explicit manual reconciliation message

Reconciliation sequence:

1. load stored message
2. reject unless status is `PENDING`
3. reject unless the row is stale
4. mark it `FAILED`
5. return the updated stored-message DTO

Operational distinction:

- retry is for retryable `FAILED` messages
- manual reconciliation is for stale `PENDING` messages that need operator classification without republishing

Related ADR:

- [ADR-0007](./adr/0007-use-manual-reconciliation-for-stale-pending-messages.md)

## Read Flow

The stored-message browser calls `GET /api/v1/messages/all`.

The broker API supports:

- pagination
- sorting
- text filters for `destination` and `innerMessageId`
- exact enum filters for `deliveryMode` and `payload.type`
- `publishStatus` filtering
- `stalePendingOnly=true` filtering for stale `PENDING` rows
- date-range filtering on `createdAt`
- date-range filtering on `publishedAt`
- lifecycle aggregate counts for the full filtered result set
- full filtered export through `GET /api/v1/messages/export`

The UI layers on:

- form controls for destination, delivery mode, payload type, inner message id, publish status, stale pending, created date ranges, and published date ranges
- messages-per-page control plus previous/next page navigation
- clickable lifecycle summary pills for `published`, `failed`, `pending`, and stale pending
- full filtered lifecycle aggregate counts returned by the backend
- page-level lifecycle counts derived from the currently loaded items
- reset behavior
- detail expansion
- copy actions
- single-message retry for retryable failed messages
- stale-pending reconciliation

Important distinction:

- backend `lifecycleCounts` describe the full filtered result set across all pages
- backend retryability counts describe how many failed rows are retryable versus blocked
- UI page counts describe only the items currently loaded in `items`
- page navigation changes the requested backend page while preserving the current filters and page size

## Subscriber Role

`solace-subscriber` is intentionally separate from the broker API persistence model.

It observes direct topic traffic from Solace and is useful for:

- confirming that published messages are reaching the broker/topic
- watching reconnect and discard conditions
- validating topic traffic independently from the database/browser view

It does not read from the broker API database.

## Shared Configuration Boundaries

Shared environment variables used by the backend and subscriber:

- `SOLACE_CLOUD_HOST`
- `SOLACE_CLOUD_VPN`
- `SOLACE_CLOUD_USERNAME`
- `SOLACE_CLOUD_PASSWORD`

The UI does not read these directly.

## Runtime Workflow

Recommended Docker entrypoints from the repo root:

- `make docker-start`
- `make docker-status`
- `make docker-logs`
- `make docker-stop`

The Docker workflow runs MySQL, the broker API, the publisher UI, and the
subscriber through the root `docker-compose.yml`.

The local process workflow is still available for module-level development:

- `make start-api`
- `make start-ui`
- `make start-subscriber`
- `make start-all`

Supporting scripts for both workflows live in `scripts/`.

## CI And Verification

Current verification layers:

- backend Maven tests
- UI Vitest suite
- subscriber Maven tests
- root script smoke tests

GitHub Actions CI runs those checks on pushes and pull requests through `.github/workflows/ci.yml`.
