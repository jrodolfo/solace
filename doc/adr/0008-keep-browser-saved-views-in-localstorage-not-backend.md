# ADR-0008: Keep Browser Saved Views in Local Storage, Not the Backend

- Status: `accepted`
- Date: `2026-05-24`

## Context

The UI supports filtering, lifecycle presets, exports, and user-defined saved
views for stored-message browsing. Those saved views are primarily convenience
state for the current operator rather than business records that the backend
must own.

This project does not currently require:

- shared saved views across users
- server-side identity-aware personalization
- backend persistence of UI-only workspace preferences

## Decision

We keep saved browser views as client-side state in browser `localStorage`
instead of storing them in `solace-broker-api`.

The backend remains responsible for:

- stored-message query and export capabilities
- lifecycle counts and retryability information
- the data contract used to populate the UI

The UI remains responsible for:

- local saved-view persistence
- rename, overwrite confirmation, import/export, and history of saved-view actions

## Rationale

Saved views are operator convenience features, not part of the core message
domain. Keeping them in the browser avoids adding backend endpoints, database
schema, and user-state management for something that is currently local and
lightweight in scope.

This also preserves a cleaner system boundary: the backend owns message data and
operational workflows, while the UI owns presentation-specific local state.

## Consequences

Benefits:

- the feature is simple to implement and use without backend changes
- backend persistence stays focused on message records rather than UI preferences
- saved views remain available immediately in the same browser context
- import/export can still be used to move view definitions between environments manually

Tradeoffs:

- saved views are not automatically shared across browsers or users
- clearing browser storage can remove locally saved views
- local state behavior may differ across machines even against the same backend data

## Revisit Triggers

- the project needs shared or role-based saved views
- authentication is added and server-side user preferences become valuable
- browser-local storage becomes too fragile for the expected operator workflow
- audit or governance requirements demand centrally managed saved-view definitions

## Alternatives Considered

- Persist saved views in the backend database
- Store saved views in files checked into the repository
- Support only built-in presets and remove user-defined saved views entirely
