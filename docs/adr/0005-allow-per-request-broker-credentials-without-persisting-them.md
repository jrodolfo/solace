# ADR-0005: Allow Per-Request Broker Credentials Without Persisting Them

- Status: `accepted`
- Date: `2026-05-24`

## Context

The project needs a flexible way to publish messages in environments where:

- shared server-side environment variables may already be configured
- a caller may want to override broker connection values for one request
- sensitive connection data should not become part of the stored message history

Persisting request-level credentials would create unnecessary security risk and
mix connection secrets with message business data.

## Decision

`solace-broker-api` may accept broker connection parameters on publish requests,
but those values are used only for the publish attempt and are not persisted with
the stored message record.

The backend therefore separates:

- message data and lifecycle state, which are persisted
- connection override data, which is transient request-scoped input

When per-request credentials are not provided, the backend can rely on the shared
environment-based configuration boundary already used across the workspace.

## Rationale

This approach balances flexibility and safety. It allows callers to test or
override broker connection settings for a specific request without forcing those
secrets into the database.

The project’s stored history should explain message intent and publish outcome,
not become a repository of reusable connection credentials. Keeping overrides
transient reduces the security and data-handling burden while preserving useful
runtime flexibility.

## Primary Implementation

- `solace-broker-api/src/main/java/net/jrodolfo/solace/broker/api/controller/MessageController.java`
- `solace-broker-api/src/main/java/net/jrodolfo/solace/broker/api/service/DirectPublisherService.java`
- `solace-broker-api/src/main/java/net/jrodolfo/solace/broker/api/service/DirectPublisherServiceImpl.java`

## Consequences

Benefits:

- callers can test different broker settings without changing server configuration
- sensitive connection values are kept out of stored message history
- persistence remains focused on message intent and outcome rather than secrets

Tradeoffs:

- publish behavior depends on a mix of persisted data and transient runtime input
- troubleshooting may require remembering whether a failed publish used request overrides
- future auditing needs may require metadata that identifies override usage without storing secrets

## Revisit Triggers

- security requirements change and per-request credential input is no longer acceptable
- replay or audit requirements need richer traceability for override usage
- the project standardizes on environment-only configuration for all publish flows
- secret-management infrastructure becomes available that supports a better override model

## Alternatives Considered

- Persist all request-supplied credentials with the message record
- Forbid per-request overrides and require environment variables only
- Store encrypted override credentials in the database for replay purposes
