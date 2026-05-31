# ADR-0006: Use Synchronous Publish with Pending-First Persistence

- Status: `accepted`
- Date: `2026-05-24`

## Context

The backend currently accepts a publish request, persists a stored-message
record, performs the Solace publish, and returns the final outcome in the same
request/response path.

This project benefits from an immediate operator-facing result because the UI
and manual API clients are used for:

- direct interactive publishing
- quick troubleshooting
- demonstration and interview walkthroughs
- immediate inspection of success or failure outcomes

At the same time, the system still needs a durable record of intent before the
publish outcome is finalized.

## Decision

We use a synchronous publish flow in `solace-broker-api`, but we persist the
message as `PENDING` before attempting the actual Solace publish.

The flow is:

1. receive the publish request
2. save the stored message as `PENDING`
3. attempt the Solace publish in the current request path
4. update the same stored message to `PUBLISHED` or `FAILED`
5. return the publish outcome to the caller

## Rationale

This combines two goals that matter for the current workspace:

- keep the publish experience simple and immediate for operators and demos
- preserve evidence of the requested action even if the process crashes before
  the final state update completes

A synchronous flow is easier to explain and operate at the current scale of the
project. The pending-first write preserves a safer audit trail than a publish
attempt that only writes after the broker outcome is known.

## Primary Implementation

- `solace-broker-api/src/main/java/net/jrodolfo/solace/broker/api/controller/MessageController.java`
- `solace-broker-api/src/main/java/net/jrodolfo/solace/broker/api/service/DatabaseImpl.java`

## Consequences

Benefits:

- callers receive the publish outcome immediately
- the implementation is simpler than adding background workers and job queues
- the stored record exists even if finalization fails after request acceptance
- the UI can show a consistent lifecycle model across publish, retry, and reconciliation

Tradeoffs:

- request latency includes the broker publish attempt
- database and broker availability both affect the publish path
- unresolved `PENDING` rows must still be handled explicitly when finalization fails
- this model may become less suitable if publish volume or concurrency grows substantially

## Revisit Triggers

- publish latency becomes too high for a request/response model
- throughput requirements justify queue-backed or worker-based publishing
- reliability requirements call for a stronger outbox or job-processing pattern
- the UI or API no longer needs immediate broker outcome feedback

## Alternatives Considered

- Use a fully asynchronous worker model and return acceptance before broker publish
- Publish first and persist only after the broker outcome is known
- Keep the API stateless and rely only on the immediate publish response
