# ADR-0001: Use a Three-Module Solace Workspace

- Status: `accepted`
- Date: `2026-05-24`

## Context

This repository needs to support three related but different concerns:

- an operator-facing interface for publishing and browsing messages
- a backend that can validate requests, publish to Solace, and persist state
- a direct subscriber that can independently observe broker traffic

A single executable or single codebase layer would blur these responsibilities.
The UI has browser concerns, the backend has persistence and HTTP concerns, and
the subscriber has direct broker-consumer concerns.

## Decision

We organize the repository as a small workspace with three active modules:

- `solace-publisher-ui` for the browser UI
- `solace-broker-api` for the Spring Boot backend
- `solace-subscriber` for the Java subscriber process

Shared documentation, helper scripts, and integration artifacts stay at the
repository root under `doc/`, `scripts/`, and the root `Makefile`.

## Consequences

Benefits:

- responsibilities are clearer at the module boundary
- each module can use the runtime and tooling that fits its job
- the backend remains reusable by non-UI clients such as Postman and JMeter
- subscriber behavior can be validated independently from the API and database

Tradeoffs:

- local setup involves multiple runtimes and startup commands
- cross-module changes require more coordination
- shared concepts must be documented explicitly rather than implied by one codebase

## Alternatives Considered

- Put the UI, API, and subscriber into one application with tightly coupled runtime behavior
- Keep only the backend and use Postman or JMeter instead of maintaining a dedicated UI
- Move the subscriber into the backend process and treat it as another internal service
