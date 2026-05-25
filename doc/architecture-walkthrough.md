# Architecture Walkthrough

This document is a concise walkthrough of the Solace workspace for maintainers,
reviewers, and future contributors. It is intentionally shorter and more
conversational than
[`architecture.md`](./architecture.md) and the ADR set in [`adr/`](./adr/).

## One-Minute System Summary

This repository is a small Solace-focused workspace with three modules:

- `solace-broker-api`: Spring Boot backend that accepts publish requests,
  persists message lifecycle state, supports retry and reconciliation, and
  exposes stored-message query/export APIs
- `solace-publisher-ui`: React UI for publishing messages and browsing stored
  results
- `solace-subscriber`: separate Java subscriber that listens directly to Solace
  topic traffic

The main architectural idea is that the backend is the system of record for
publish history, while the subscriber is a separate observer of broker traffic
and the UI is an operator tool on top of the backend.

## Core Design Story

If I had to explain the project in a few sentences:

1. the UI or another client sends a publish request to `solace-broker-api`
2. the backend stores the request as `PENDING`
3. the backend attempts the Solace publish synchronously
4. the stored message is finalized as `PUBLISHED` or `FAILED`
5. the UI can later browse, retry, export, or reconcile those stored records

This design was chosen so the project can do more than just publish messages in
real time. It can also explain what happened afterward.

## Main Architectural Decisions

### 1. Three-module workspace instead of one app

Why:

- the browser UI, backend API, and subscriber solve different problems
- the boundaries are easier to explain and maintain
- the backend stays reusable for Postman, JMeter, curl, or other HTTP clients

Related ADRs:

- [ADR-0001](./adr/0001-use-a-three-module-solace-workspace.md)
- [ADR-0004](./adr/0004-provide-a-react-publisher-ui-in-addition-to-api-clients.md)

### 2. Persist publish attempts before final outcome

Why:

- the project needs history, retry, filtering, and clear evidence of what
  happened
- writing `PENDING` first preserves evidence of intent even if the process
  crashes before finalization

Related ADRs:

- [ADR-0003](./adr/0003-persist-publish-attempts-and-track-message-lifecycle.md)
- [ADR-0006](./adr/0006-use-synchronous-publish-with-pending-first-persistence.md)

### 3. Keep publish synchronous for now

Why:

- simpler operational model
- immediate feedback for the UI and manual clients
- appropriate for the current scope of the project

Tradeoff:

- request latency includes the actual broker publish attempt

Related ADR:

- [ADR-0006](./adr/0006-use-synchronous-publish-with-pending-first-persistence.md)

### 4. Separate retry from stale-pending reconciliation

Why:

- a `FAILED` message is a known failure that may be retryable
- a stale `PENDING` message is an unresolved state and should not be
  republished automatically

Related ADRs:

- [ADR-0007](./adr/0007-use-manual-reconciliation-for-stale-pending-messages.md)
- [ADR-0009](./adr/0009-support-retry-at-the-stored-message-level.md)

### 5. Keep some UI state in the browser

Why:

- saved views are operator convenience state, not core backend domain data
- `localStorage` keeps the backend focused on message workflows rather than UI
  preferences

Related ADR:

- [ADR-0008](./adr/0008-keep-browser-saved-views-in-localstorage-not-backend.md)

## Key Design Points

### Clear separation of responsibilities

- backend owns publish lifecycle, persistence, retry, reconciliation, and query
- UI owns operator interactions and browser-local convenience state
- subscriber owns direct broker observation

### Defensive design around uncertain outcomes

The `PENDING` state exists even though publish is currently synchronous because
accepted work should still leave a trace if the process fails before the final
status update.

### Explicit handling of ambiguity

I separated retry from reconciliation because retry is for known failed
messages, while stale `PENDING` rows represent uncertainty. That distinction
reduces the risk of accidental duplicate publishing.

### Practical operator tooling

The project is not only about backend correctness. It also includes a usable UI,
export behavior, saved views, Postman/JMeter/curl artifacts, and operational
recovery paths.

## Common Design Questions

### Why not make publish asynchronous?

Short answer:

- synchronous publish is simpler and gives immediate feedback
- the current project scope does not justify queue or worker complexity yet
- the ADRs explicitly identify async workers as a future revisit trigger

Related ADR:

- [ADR-0006](./adr/0006-use-synchronous-publish-with-pending-first-persistence.md)

### Why store `PENDING` first if the flow is synchronous?

Short answer:

- to preserve evidence of accepted intent before the final outcome is known
- if the process crashes after the initial write, the system still has a record
  that can be reviewed or reconciled

### Why not auto-retry stale `PENDING` rows?

Short answer:

- because stale `PENDING` means uncertain outcome, not confirmed failure
- auto-retrying could republish a message that already reached the broker

Related ADR:

- [ADR-0007](./adr/0007-use-manual-reconciliation-for-stale-pending-messages.md)

### Why is retry tied to the stored message record?

Short answer:

- the stored message is the operational identity in this system
- retrying the stored record preserves lifecycle continuity and works naturally
  with the UI

Related ADR:

- [ADR-0009](./adr/0009-support-retry-at-the-stored-message-level.md)

### Why keep saved views in `localStorage`?

Short answer:

- they are UI convenience state, not shared business data
- storing them in the backend would add complexity without current value

Related ADR:

- [ADR-0008](./adr/0008-keep-browser-saved-views-in-localstorage-not-backend.md)

### Why keep Postman, JMeter, and curl assets in the repo?

Short answer:

- they prove the backend is reusable outside the UI
- they make the project easier to validate, demo, and discuss

Related ADR:

- [ADR-0010](./adr/0010-keep-postman-jmeter-and-curl-artifacts-alongside-the-codebase.md)

## Future Evolution Paths

If the system needs to evolve, likely next steps would be:

- move publish to an async worker/outbox model if throughput or reliability
  requirements increase
- add stronger delivery verification if stale pending rows become a frequent
  operational problem
- move saved views server-side only if shared user-specific state becomes a real
  requirement
- add richer audit history if retries need immutable per-attempt records

## Where To Read More

- System overview: [`architecture.md`](./architecture.md)
- Architectural decisions: [`adr/README.md`](./adr/README.md)
