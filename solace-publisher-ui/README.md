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
- exporting the current page as JSON or CSV
- exporting the full filtered result set as JSON or CSV

Bulk retry now calls the backend batch retry endpoint (`POST /api/v1/messages/retry`) instead of sending one browser request per failed row.

Saved browser views are client-side only:

- built-in views ship with the app and are always available
- built-in views are separate from user-defined saved views
- built-in views reuse the normal browser query/load path and update the visible controls
- rename applies only to user-defined saved views
- save and rename intentionally overwrite by name when the target name already exists
- save asks for confirmation before replacing an existing saved view with the same name
- rename asks for confirmation before overwriting a different existing saved view
- cancelling the confirmation keeps the existing saved view unchanged
- saved-view import merges by name, so matching names update existing user-defined views
- invalid saved-view import entries are skipped instead of stopping the whole import
- saved-view import feedback reports how many views were added, updated, and skipped
- saved-view import feedback also lists which views were added, updated, or skipped
- skipped invalid entries use fallback labels such as `entry 3` when no usable view name is present
- the UI also keeps a short recent-action history for save, rename, delete, and import events
- that recent saved-view history can be cleared explicitly from the UI
- only the 5 most recent saved-view actions are kept in that history
- they live in the browser's `localStorage`
- they are not stored in `solace-broker-api`
- JSON export/import is the intended sharing path between browsers or operators

Export behavior:

- `Export Current Page JSON` downloads the currently loaded page exactly as it exists in the browser, including page metadata, lifecycle aggregates, and `items`
- `Export Current Page CSV` flattens the currently loaded page into spreadsheet-friendly rows
- `Export Filtered Results JSON` calls the backend export endpoint and downloads the full filtered result set as structured JSON
- `Export Filtered Results CSV` calls the backend export endpoint for the full filtered result set and then flattens that response into CSV in the browser

CSV is intentionally flatter than JSON:

- payload fields are split into explicit columns such as `payloadType` and `payloadContent`
- lifecycle, retryability, and timestamp fields each get their own column
- `properties` are serialized into a single CSV field
- JSON is the better fit when you want to preserve the nested response structure exactly

## Notes

- Sample destinations used throughout the repo still follow the pattern `solace/java/direct/system-01`.
- If the backend is unavailable, the UI will surface the backend/network failure in the response area rather than silently swallowing it.
- For backend env-var setup, see [../solace-broker-api/README.md](../solace-broker-api/README.md).
