# ADR-0011: Separate Current-Page Export from Full Filtered Export

- Status: `accepted`
- Date: `2026-05-24`

## Context

The stored-message browser supports both immediate inspection of the currently
loaded page and broader export of all records matching the active filters.

Those are related but different tasks:

- current-page export is about the data already loaded in the browser
- full filtered export is about the complete filtered result set across all pages

Trying to force both into the same implementation path would blur an important
boundary between browser-local state and backend-owned query execution.

## Decision

We treat current-page export and full filtered export as separate workflows.

- current-page export is generated directly from the UI’s already loaded data
- full filtered export is delegated to the backend export endpoint

Both workflows can produce JSON or CSV, but they do not rely on the same source
of truth.

## Rationale

The browser already has the current page in memory, so exporting that data
locally is fast and avoids an unnecessary round-trip. By contrast, a full
filtered export should come from the backend because only the backend has the
complete filtered result set, aggregate counts, and canonical query execution
context across all pages.

This separation keeps each export mode honest about what it represents.

## Primary Implementation

- `solace-publisher-ui/src/App.tsx`
- `solace-publisher-ui/src/StoredMessageTypes.ts`
- `solace-broker-api/src/main/java/net/jrodolfo/solace/broker/api/controller/MessageController.java`
- `solace-broker-api/src/main/java/net/jrodolfo/solace/broker/api/service/DatabaseImpl.java`

## Consequences

Benefits:

- current-page export is immediate and aligned with what the operator is looking at
- full filtered export reflects the backend’s full filtered dataset, not just one loaded page
- the UI/backend boundary stays clear
- export semantics are easier to explain during maintenance and technical review

Tradeoffs:

- there are two export paths to maintain
- users must understand that current-page and full filtered exports are intentionally different
- formatting logic is split between browser-local and backend-driven workflows

## Revisit Triggers

- the UI moves to a different pagination or caching model that changes what “current page” means
- export requirements become complex enough that all export generation should be centralized
- the backend begins serving richer export formats that make local export less useful
- user confusion suggests the two export workflows should be redesigned or renamed

## Alternatives Considered

- Route every export through the backend, including the current page
- Export only the current page and remove full filtered export
- Export only the full filtered dataset and remove browser-local export behavior
