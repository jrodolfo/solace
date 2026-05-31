# ADR-0007: Use Manual Reconciliation for Stale Pending Messages

- Status: `accepted`
- Date: `2026-05-24`

## Context

Even with a synchronous publish flow, a stored message can remain in `PENDING`
if the process is interrupted after the initial write or if the final lifecycle
update cannot be completed cleanly.

Those rows need an explicit operator-facing way to be reviewed and resolved.
Automatically retrying them by default would blur the boundary between:

- a message that is definitely retryable because a previous attempt failed
- a message whose final publish outcome is uncertain because the lifecycle was
  left unresolved

## Decision

We provide a separate manual reconciliation workflow for stale `PENDING`
messages.

The workflow:

- applies only to messages still in `PENDING`
- requires the row to be stale under the current heuristic threshold
- does not attempt another broker publish
- updates the stored message to `FAILED`
- records an explicit reconciliation-oriented failure reason

This workflow is exposed separately from retry operations.

## Rationale

Stale `PENDING` rows represent uncertainty, not a normal retry case. Treating
them as a distinct operator workflow makes that uncertainty visible and avoids
accidentally republishing a message whose broker outcome may already have
occurred but was not recorded locally.

This also gives the UI and the documentation a clearer operational story:

- retry is for known failed messages
- reconciliation is for unresolved pending messages

## Primary Implementation

- `solace-broker-api/src/main/java/net/jrodolfo/solace/broker/api/controller/MessageController.java`
- `solace-broker-api/src/main/java/net/jrodolfo/solace/broker/api/dto/StoredMessageDTO.java`
- `solace-publisher-ui/src/App.tsx`

## Consequences

Benefits:

- the system avoids silently republishing uncertain messages
- operators get a clear recovery path for unresolved lifecycle rows
- stored-message history reflects that a human classification step occurred
- the distinction between retry and reconciliation stays visible in the UI and API

Tradeoffs:

- reconciliation requires operator judgment rather than full automation
- some reconciled rows may represent publishes that actually reached the broker
- the workflow resolves uncertainty by classification, not by reconstructing the exact broker truth

## Revisit Triggers

- the project gains a reliable way to prove broker delivery after interrupted finalization
- operational requirements push toward automatic recovery for stale pending rows
- stale pending volume becomes high enough that manual handling is no longer practical
- publish processing moves to an architecture where pending-state recovery is handled by workers

## Alternatives Considered

- Automatically retry stale `PENDING` rows as if they were normal `FAILED` messages
- Leave stale `PENDING` rows unresolved and provide no dedicated recovery workflow
- Delete stale `PENDING` rows instead of preserving them as part of message history
