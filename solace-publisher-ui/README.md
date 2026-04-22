# Solace Publisher UI

`solace-publisher-ui` is a React + TypeScript frontend for the local `solace-broker-api` module. It now supports both sides of the workflow:

- publishing Solace messages with typed form fields
- browsing stored messages with pagination, filtering, sorting, details expansion, timestamp formatting, and copy actions

For the repo-level data flow and module boundaries, see [../doc/architecture.md](../doc/architecture.md).

## Requirements

- Node.js 20+ or newer
- npm
- `solace-broker-api` running locally on `http://localhost:8081`

The UI itself does not read Solace environment variables directly. Those are required by the backend and subscriber modules.

## Run

From the `solace-publisher-ui` directory:

```bash
npm install
npm run dev
```

Then open:

```text
http://localhost:5173/
```

## Test

```bash
npm test -- --run
```

## Current UI Behavior

### Publish workflow

The publish form uses typed fields instead of a raw JSON textarea. It collects:

- broker credentials: `userName`, `password`, `host`, `vpnName`
- message fields: `innerMessageId`, `destination`, `deliveryMode`, `priority`
- payload fields: `type`, `content`
- optional repeatable message properties as `key` / `value` rows

The UI performs client-side validation before calling the backend and preserves typed backend error responses when the API returns validation or publish failures.

### Stored message browser

The browser loads `GET /api/v1/messages/all` from `solace-broker-api` and supports:

- `destination`, `deliveryMode`, and `innerMessageId` filters
- `publishStatus`, `createdAt`, and `publishedAt` filtering
- `stalePendingOnly` filtering for stale pending rows
- `sortBy` and `sortDirection`
- `page` and `size`
- quick lifecycle/date presets such as `failed today`
- clickable lifecycle summary pills for `published`, `failed`, `pending`, and stale pending
- one lifecycle summary strip for full filtered totals returned by the backend
- filtered failed totals include retryable versus non-retryable counts
- one lifecycle summary strip for counts on the current page only
- previous/next paging
- refresh and reset controls
- expandable message details
- friendly timestamp rendering
- copy actions for destination, payload content, and properties
- bulk retry for currently visible failed messages
- manual reconciliation for stale `PENDING` messages
- built-in operator views for `failed today`, `stale pending only`, `published today`, and `pending now`
- saving the current browser view in local storage
- loading, renaming, and deleting saved browser views
- exporting saved browser views to JSON
- importing saved browser views from JSON
- exporting the full filtered result set through the backend export endpoint

Bulk retry now calls the backend batch retry endpoint (`POST /api/v1/messages/retry`) instead of sending one browser request per failed row.

Saved browser views are client-side only:

- built-in views ship with the app and are always available
- built-in views are separate from user-defined saved views
- built-in views reuse the normal browser query/load path and update the visible controls
- rename applies only to user-defined saved views
- save and rename intentionally overwrite by name when the target name already exists
- they live in the browser's `localStorage`
- they are not stored in `solace-broker-api`
- JSON export/import is the intended sharing path between browsers or operators

## Notes

- Sample destinations used throughout the repo still follow the pattern `solace/java/direct/system-01`.
- If the backend is unavailable, the UI will surface the backend/network failure in the response area rather than silently swallowing it.
- For backend env-var setup, see [../solace-broker-api/README.md](../solace-broker-api/README.md).
