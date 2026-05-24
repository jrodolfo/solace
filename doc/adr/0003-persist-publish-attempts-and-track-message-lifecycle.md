# ADR-0003: Persist Publish Attempts and Track Message Lifecycle

- Status: `accepted`
- Date: `2026-05-24`

## Context

This project is not only a thin publish proxy. It also needs to support:

- later inspection of what was sent
- retry of failed stored messages
- UI browsing with lifecycle filters
- interview and debugging scenarios where we need evidence of outcomes

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

The persisted record becomes the source for read APIs, UI browsing, and retry workflows.

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

## Alternatives Considered

- Treat the backend as a stateless publish-only proxy
- Persist only failed requests and ignore successful publishes
- Store raw request payloads without lifecycle status or retry-oriented fields
