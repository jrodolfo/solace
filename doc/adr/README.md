# Architecture Decision Records

This folder stores Architecture Decision Records (ADRs) for the Solace workspace.

ADRs capture the architectural decisions that shaped the repository, why those
decisions were made, and what tradeoffs they introduced. They complement
[`doc/architecture.md`](../architecture.md), which describes the current
design, by preserving the decision history behind that design.

## Status Values

- `proposed`: decision is being discussed and is not yet the project baseline
- `accepted`: decision is in effect and reflected in the codebase
- `superseded`: decision was replaced by a newer ADR

## Naming Convention

Use zero-padded numeric prefixes and kebab-case titles:

```text
0001-short-kebab-case-title.md
```

Each ADR should focus on one decision and stay short enough to scan quickly in
an interview, maintenance, or onboarding context.

When an ADR is replaced, keep the old file, change its status to `superseded`,
and add a short note near the top linking to the newer ADR that replaces it.

## ADR Template

Start new records from [template.md](template.md).

## Current ADRs

- [0001-use-a-three-module-solace-workspace.md](0001-use-a-three-module-solace-workspace.md)
- [0002-keep-the-subscriber-separate-from-the-broker-api.md](0002-keep-the-subscriber-separate-from-the-broker-api.md)
- [0003-persist-publish-attempts-and-track-message-lifecycle.md](0003-persist-publish-attempts-and-track-message-lifecycle.md)
- [0004-provide-a-react-publisher-ui-in-addition-to-api-clients.md](0004-provide-a-react-publisher-ui-in-addition-to-api-clients.md)
- [0005-allow-per-request-broker-credentials-without-persisting-them.md](0005-allow-per-request-broker-credentials-without-persisting-them.md)
