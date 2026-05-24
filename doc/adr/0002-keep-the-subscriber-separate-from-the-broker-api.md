# ADR-0002: Keep the Subscriber Separate from the Broker API

- Status: `accepted`
- Date: `2026-05-24`

## Context

The backend is responsible for accepting publish requests, persisting message
state, and exposing read and retry workflows. The subscriber serves a different
purpose: it provides direct visibility into topic traffic coming from Solace.

Combining those responsibilities in one runtime would make it harder to reason
about whether a behavior belongs to the API lifecycle or to independent broker
observation.

## Decision

We keep `solace-subscriber` as a separate Java application instead of embedding
subscription behavior inside `solace-broker-api`.

The subscriber:

- connects directly to Solace
- listens to topic traffic
- logs inbound events and connection lifecycle signals

It does not own persistence, publish lifecycle state, or API read/query behavior.

## Consequences

Benefits:

- inbound broker traffic can be inspected independently from database state
- subscriber failures or reconnect issues are isolated from publish and query APIs
- the runtime boundary reinforces the difference between observation and system-of-record behavior

Tradeoffs:

- another process must be started and configured locally
- there is no implicit shared in-memory state between publisher workflows and subscriber workflows
- operators need to correlate subscriber output with API/database state manually

## Alternatives Considered

- Run the subscriber inside the backend as a background component
- Persist subscriber observations into the same backend database by default
- Remove the dedicated subscriber and rely only on publish responses and stored records
