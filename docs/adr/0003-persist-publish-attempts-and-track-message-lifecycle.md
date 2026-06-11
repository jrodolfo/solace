# ADR-0003: Persist Publish Attempts and Track Message Lifecycle

- Status: `accepted`
- Date: `2026-05-24`

## Context

This project is not only a thin publish proxy. It also needs to support:

- later inspection of what was sent
- retry of failed stored messages
- UI browsing with lifecycle filters
- debugging and review scenarios where we need evidence of outcomes

If the backend only forwarded publish requests to Solace and returned the
immediate broker result, we would lose operational history and make retry and
reporting much harder.

## Decision

`solace-broker-api` persists publish attempts and tracks lifecycle state for each
message.

The intended flow is:

1. accept a publish request
2. save the request as `PENDING`
3. attempt the Solace publish
4. mark the record `PUBLISHED` on success or `FAILED` on error
5. store relevant timestamps and failure information for later reads and retries

The persisted record becomes the source for read APIs, UI browsing, and retry
workflows. In the current implementation this lifecycle transition is
synchronous in the request path, but the initial `PENDING` write is still kept
to preserve the requested operation before the final outcome is known.

## Rationale

The repository is designed for more than one-off publishing. It supports
inspection, retry, filtering, and demonstration of message outcomes over time.
Those workflows need a stored record with explicit lifecycle state, not only the
immediate result of a single HTTP call.

Persisting the lifecycle also creates a clearer operational and learning model:
the project can show what was attempted, what succeeded, what failed, and what
can be retried later.

The related `stalePending` signal uses a configurable threshold, defaulting to 5 minutes, as a practical
operator heuristic for highlighting rows that probably need review. That value
is not intended as a protocol guarantee; it is simply a conservative cutoff for
surfacing records that remained unresolved longer than expected in the current
runtime model, including cases where process interruption or unusually slow
runtime recovery leaves the record without a finalized outcome.

## Primary Implementation

- `solace-broker-api/src/main/java/net/jrodolfo/solace/broker/api/controller/MessageController.java`
- `solace-broker-api/src/main/java/net/jrodolfo/solace/broker/api/service/DatabaseImpl.java`
- `solace-broker-api/src/main/java/net/jrodolfo/solace/broker/api/dto/StoredMessageDTO.java`

## Consequences

Benefits:

- publish history survives beyond the immediate HTTP response
- failed messages can be retried from stored state
- the UI can provide lifecycle filtering, status counts, and exports
- debugging is easier because the system records both intent and outcome

Tradeoffs:

- persistence adds schema and state-management complexity
- lifecycle transitions must be kept consistent with publish behavior
- database availability becomes part of the publish path

## Revisit Triggers

- the project intentionally shifts to a stateless publish-only API
- persistence costs or operational complexity outweigh retry and history value
- lifecycle states need to expand beyond the current model in a way that requires a redesign
- another storage mechanism becomes a better fit than the current relational model
- publish volume or latency requirements push the system toward an asynchronous worker-based publish model

## Alternatives Considered

- Treat the backend as a stateless publish-only proxy
- Persist only failed requests and ignore successful publishes
- Store raw request payloads without lifecycle status or retry-oriented fields
