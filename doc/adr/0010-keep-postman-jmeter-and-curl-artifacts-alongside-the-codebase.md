# ADR-0010: Keep Postman, JMeter, and Curl Artifacts Alongside the Codebase

- Status: `accepted`
- Date: `2026-05-24`

## Context

This repository is not used only through the browser UI. The backend is also a
general HTTP surface for:

- manual endpoint exploration
- repeatable API testing
- performance-style request generation
- troubleshooting without the UI
- demonstration of multiple ways to exercise the same contract

The repo already includes tooling artifacts and examples under `doc/`, such as
Postman collections, JMeter assets, curl samples, message examples, and setup
notes.

## Decision

We keep Postman, JMeter, curl, and related API exercise artifacts in the
repository under `doc/` alongside the codebase instead of treating them as
external or disposable assets.

These artifacts are documentation and operational companions to the backend
contract, not separate products.

## Rationale

Keeping these assets in the repo makes the backend easier to validate from
multiple entry points and keeps the examples version-adjacent to the code they
describe.

This is also useful for interview and onboarding scenarios. A reviewer can see
that the backend was designed to be testable and explorable beyond a single UI,
which reinforces the architecture claim that `solace-broker-api` is not
frontend-exclusive.

## Consequences

Benefits:

- API exercise artifacts stay close to the implementation and docs they support
- non-UI validation remains easy for developers and reviewers
- multiple client perspectives help demonstrate and test the same backend contract
- repository users can choose the tool that best fits their task

Tradeoffs:

- documentation artifacts must be maintained as the API evolves
- duplicated examples across tools can drift if not updated carefully
- the `doc/` tree grows beyond narrative documentation alone

## Revisit Triggers

- the repository adopts a different documentation publishing model for API assets
- tool-specific artifacts become too costly to keep synchronized
- automated contract-generation replaces most manually maintained examples
- the project intentionally narrows support to one primary client/testing path

## Alternatives Considered

- Keep these artifacts outside the repository in personal tooling workspaces
- Support only the browser UI and Swagger-style API exploration
- Maintain only one external client format instead of several complementary ones
