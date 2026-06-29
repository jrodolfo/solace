# Solace Publisher UI

[![CI](https://github.com/jrodolfo/solace/actions/workflows/ci.yml/badge.svg)](https://github.com/jrodolfo/solace/actions/workflows/ci.yml)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](../LICENSE)
[![React 19](https://img.shields.io/badge/React-19-61DAFB?logo=react&logoColor=black)](https://react.dev/)
[![TypeScript 6](https://img.shields.io/badge/TypeScript-6-3178C6?logo=typescript&logoColor=white)](https://www.typescriptlang.org/)
[![Vite 8](https://img.shields.io/badge/Vite-8-646CFF?logo=vite&logoColor=white)](https://vite.dev/)
[![Bootstrap 5](https://img.shields.io/badge/Bootstrap-5-7952B3?logo=bootstrap&logoColor=white)](https://getbootstrap.com/)

`solace-publisher-ui` is a React + TypeScript frontend for the local `solace-broker-api` module. It now supports both sides of the workflow:

- publishing Solace messages with typed form fields
- browsing stored messages with pagination, filtering, sorting, details expansion, timestamp formatting, and copy actions

For the repo-level data flow and module boundaries, see [../docs/architecture.md](../docs/architecture.md).

## Requirements

- Node.js 20+ or newer
- npm
- `solace-broker-api` running locally on `http://localhost:8081`

The UI itself does not read Solace environment variables directly. Those are required by the backend and subscriber modules.
By default, the UI calls the backend at `http://localhost:8081`. To point it somewhere else, set `VITE_BROKER_API_BASE_URL` before starting Vite.

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

If port `5173` is already in use, Vite will pick another local port. In that case, use the URL printed by `npm run dev` or the repo-level `status-all.sh` script.

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
  `type` is selected from the supported application values: `TEXT`, `BINARY`, `JSON`, `XML`
- optional repeatable message properties as `key` / `value` rows

The UI performs client-side validation before calling the backend and preserves typed backend error responses when the API returns validation or publish failures.

### Stored message browser

The browser loads `GET /api/v1/messages/all` from `solace-broker-api` and supports:

- `destination`, `deliveryMode`, `payloadType`, and `innerMessageId` filters
- `publishStatus`, `createdAt`, and `publishedAt` filtering
- `stalePendingOnly` filtering for stale pending rows
- `sortBy` and `sortDirection`
- messages-per-page control
- previous/next page navigation
- clickable lifecycle summary pills for `published`, `failed`, `pending`, and stale pending
- one lifecycle summary strip for full filtered totals returned by the backend
- filtered failed totals include retryable versus non-retryable counts
- one lifecycle summary strip for counts on the current page only
- reset controls
- expandable message details
- friendly timestamp rendering
- copy actions for destination, payload content, and properties
- single-message retry for retryable failed messages
- manual reconciliation for stale `PENDING` messages

## Notes

- Sample destinations used throughout the repo are listed in [../docs/reference/sample-destinations.md](../docs/reference/sample-destinations.md).
- If the backend is unavailable, the UI will surface the backend/network failure in the response area rather than silently swallowing it.
- For backend env-var setup, see [../solace-broker-api/README.md](../solace-broker-api/README.md).

## Contact

- Software Developer: Rod Oliveira
- GitHub: https://github.com/jrodolfo
- Webpage: https://jrodolfo.net

## License

- MIT License
- Copyright (c) 2026 Rod Oliveira
- See [LICENSE](../LICENSE)
