# ADR-0004: Provide a React Publisher UI in Addition to API Clients

- Status: `accepted`
- Date: `2026-05-24`

## Context

The backend can already be exercised through HTTP clients such as Postman,
JMeter, and `curl`. Those tools are useful for low-level testing, but they are
not ideal for repeated exploration of stored messages, retry workflows, and
interactive demonstrations.

This repository also benefits from a browser-based surface for:

- quicker manual publishing
- browsing stored messages without crafting requests by hand
- showing the project in a portfolio or interview setting

## Decision

We provide `solace-publisher-ui` as a dedicated React application on top of the
backend API rather than relying only on generic API tools.

The UI focuses on:

- typed publish input
- client-side validation
- browsing stored messages
- filtering, export, and retry actions

Generic API tools remain supported for backend testing and automation.

## Rationale

Generic API tools are effective for protocol-level testing, but they are weaker
for repeated exploratory use, demonstrations, and day-to-day manual workflows.
A dedicated UI lowers the friction for publishing, browsing stored messages, and
showing the project to other people.

The React UI also creates a clearer separation between backend integration logic
and user-facing interaction design while still keeping the backend reusable for
automation and non-browser clients.

## Primary Implementation

- `solace-publisher-ui/src/App.tsx`
- `solace-publisher-ui/src/ShowOutput.tsx`
- `README.md`
- `docs/architecture.md`

## Consequences

Benefits:

- the project is easier to demo and explain visually
- common manual workflows are faster than repeating raw HTTP requests
- backend features can be exercised through a more guided interface

Tradeoffs:

- the repository must maintain an additional frontend codebase
- UI and API contracts must evolve together
- browser state and UX concerns add a separate testing surface

## Revisit Triggers

- the UI no longer provides enough value beyond Swagger, Postman, JMeter, and `curl`
- the project shifts away from interactive demo and portfolio use cases
- maintaining the frontend becomes disproportionate to the rest of the workspace
- a different frontend delivery model better matches future requirements

## Alternatives Considered

- Use only Postman, JMeter, Swagger UI, and `curl`
- Build a server-rendered interface inside the backend instead of a separate React app
- Avoid a UI entirely and keep the project focused on backend integration only
