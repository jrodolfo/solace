# ADR-0009: Support Retry at the Stored Message Level

- Status: `accepted`
- Date: `2026-05-24`

## Context

The backend persists publish attempts as stored messages with lifecycle state,
timestamps, retryability metadata, and a stable record `id`.

This project needs retry behavior that is compatible with:

- stored lifecycle history
- UI browsing of past publish attempts
- single-message retry
- batch retry from filtered browser results
- explicit distinction between retryable and non-retryable failed rows

Retrying raw ad hoc request payloads without reference to the stored record
would weaken that lifecycle model and make it harder to explain what exactly is
being retried.

## Decision

We implement retry as an operation on an existing stored message record rather
than as a separate replay-only workflow detached from persistence.

The retry flow:

- loads a stored message by its persisted identity
- rejects retry unless the record is in a retryable `FAILED` state
- updates the same record back to `PENDING`
- attempts publish again
- finalizes the same record as `PUBLISHED` or `FAILED`

Batch retry follows the same model using a list of stored message ids.

## Rationale

The stored message is the project’s operational unit of history. Retrying at
that level preserves continuity: the same record shows the original failure,
the retry attempt, and the latest outcome.

This also matches how operators actually work in the UI. They do not think in
terms of detached payload blobs; they think in terms of specific failed rows
that they can inspect, filter, and retry.

## Primary Implementation

- `solace-broker-api/src/main/java/org/orgname/solace/broker/api/controller/MessageController.java`
- `solace-broker-api/src/main/java/org/orgname/solace/broker/api/dto/StoredMessageDTO.java`
- `solace-broker-api/src/main/java/org/orgname/solace/broker/api/jpa/Message.java`
- `solace-publisher-ui/src/App.tsx`

## Consequences

Benefits:

- retry stays consistent with the stored lifecycle model
- single-item and batch retry share the same conceptual identity
- the UI can act on concrete failed records instead of reconstructing requests manually
- retryability rules can be surfaced directly on stored-message DTOs

Tradeoffs:

- retry depends on the integrity of the stored message record
- the same record reflects multiple attempts, so consumers must understand that history is lifecycle-oriented rather than append-only per attempt
- ad hoc payload replay outside the stored-message model is not the primary workflow

## Revisit Triggers

- the project needs immutable per-attempt history instead of reusing one stored record
- audit requirements demand a separate child record for each retry attempt
- external replay tooling becomes more important than UI-driven operational retry
- the lifecycle model changes to treat retries as separate jobs rather than updates to one stored message

## Alternatives Considered

- Retry by resubmitting a raw payload as a brand-new publish request
- Create a separate retry entity unrelated to the stored message identity
- Avoid retry support and require operators to publish again manually
