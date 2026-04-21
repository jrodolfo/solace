# Solace Broker API

`solace-broker-api` is a Spring Boot service that accepts publish requests, persists them with lifecycle state, sends them to Solace, and exposes a paginated read API for browsing stored messages.

For the repo-level module relationships and flow, see [../doc/architecture.md](../doc/architecture.md).

## Stack

- Java 21
- Spring Boot 3.3
- Spring Web
- Spring Data JPA
- SpringDoc OpenAPI / Swagger UI
- Maven
- MySQL for local runtime
- H2 for tests

## Runtime contract

- `POST /api/v1/messages/message` saves the request first as `PENDING`.
- If publish succeeds, the stored record becomes `PUBLISHED` and `publishedAt` is set.
- If publish fails, the stored record becomes `FAILED` and `failureReason` is set.
- `POST /api/v1/messages/{messageId}/retry` retries only `FAILED` messages.
- `GET /api/v1/messages/all` returns normalized DTOs, not raw JPA entities.

## Publish lifecycle

Each stored message has:

- `publishStatus`: `PENDING`, `PUBLISHED`, or `FAILED`
- `failureReason`: present when publish fails
- `publishedAt`: present when publish succeeds

This means the database represents publish attempts and their outcomes, not only successful publishes.

## Requirements

- Java 21+
- Maven 3.9+
- Docker
- Solace broker access

## Environment variables

The service uses these server-side Solace settings when no per-request credentials are provided, and for retries of failed stored messages:

- `SOLACE_CLOUD_HOST`
- `SOLACE_CLOUD_VPN`
- `SOLACE_CLOUD_USERNAME`
- `SOLACE_CLOUD_PASSWORD`

## Run locally

From `solace-broker-api`:

```bash
mvn spring-boot:run
```

The default port is `8081`.

Swagger UI:

```text
http://localhost:8081/swagger-ui/index.html
```

OpenAPI JSON:

```text
http://localhost:8081/api-docs
```

## Publish endpoint

Base URL:

```text
http://localhost:8081/api/v1/messages
```

### `POST /message`

Sends a message to Solace and stores the publish attempt.

Sample request with explicit connection parameters:

```json
{
  "userName": "solace-cloud-client",
  "password": "super-difficult",
  "host": "wss://example.messaging.solace.cloud:443",
  "vpnName": "my-solace-broker-on-aws",
  "message": {
    "innerMessageId": "001",
    "destination": "solace/java/direct/system-01",
    "deliveryMode": "PERSISTENT",
    "priority": 3,
    "properties": {
      "property01": "value01",
      "property02": "value02"
    },
    "payload": {
      "type": "binary",
      "content": "01001000 01100101 01101100 01101100"
    }
  }
}
```

Sample request using server-side Solace environment variables:

```json
{
  "message": {
    "innerMessageId": "001",
    "destination": "solace/java/direct/system-01",
    "deliveryMode": "PERSISTENT",
    "priority": 3,
    "properties": {
      "property01": "value01"
    },
    "payload": {
      "type": "binary",
      "content": "01001000 01100101 01101100 01101100"
    }
  }
}
```

Successful response, `201 Created`:

```json
{
  "destination": "solace/java/direct/system-01",
  "content": "01001000 01100101 01101100 01101100"
}
```

`innerMessageId` is required as part of the request payload, but it is treated as descriptive message metadata. The API does not enforce uniqueness for this field, and duplicate `innerMessageId` values are allowed across separate stored publish attempts.

Validation failure, `400 Bad Request`:

```json
{
  "timestamp": "2026-04-20T19:55:00Z",
  "status": 400,
  "error": "Bad Request",
  "message": "Request validation failed",
  "path": "/api/v1/messages/message",
  "validationErrors": {
    "message.innerMessageId": "message.innerMessageId is required",
    "message.payload": "message.payload is required"
  }
}
```

Publisher input rejection, `400 Bad Request`:

```json
{
  "timestamp": "2026-04-20T19:55:00Z",
  "status": 400,
  "error": "Bad Request",
  "message": "Topic name cannot be empty",
  "path": "/api/v1/messages/message",
  "validationErrors": null
}
```

Missing server configuration, `500 Internal Server Error`:

```json
{
  "timestamp": "2026-04-20T19:55:00Z",
  "status": 500,
  "error": "Internal Server Error",
  "message": "System environment variables SOLACE_CLOUD_HOST, SOLACE_CLOUD_VPN, SOLACE_CLOUD_USERNAME, SOLACE_CLOUD_PASSWORD are not set.",
  "path": "/api/v1/messages/message",
  "validationErrors": null
}
```

Connection failure, `503 Service Unavailable`:

```json
{
  "timestamp": "2026-04-20T19:55:00Z",
  "status": 503,
  "error": "Service Unavailable",
  "message": "Failed to connect to Solace broker",
  "path": "/api/v1/messages/message",
  "validationErrors": null
}
```

Downstream publish failure, `502 Bad Gateway`:

```json
{
  "timestamp": "2026-04-20T19:55:00Z",
  "status": 502,
  "error": "Bad Gateway",
  "message": "Failed to publish message to Solace broker",
  "path": "/api/v1/messages/message",
  "validationErrors": null
}
```

## Retry endpoint

### `POST /{messageId}/retry`

Retries a stored message only when its current `publishStatus` is `FAILED`.

Contract:

- loads the stored message by id
- rejects retry unless the message is `FAILED`
- sets the record to `PENDING`
- republishes using the server-side Solace configuration
- updates the same record to `PUBLISHED` or back to `FAILED`

Successful response, `200 OK`:

```json
{
  "destination": "solace/java/direct/system-02",
  "content": "01001000 01100101 01101100"
}
```

Rejected retry, `400 Bad Request`:

```json
{
  "timestamp": "2026-04-20T19:55:00Z",
  "status": 400,
  "error": "Bad Request",
  "message": "Only FAILED messages can be retried",
  "path": "/api/v1/messages/2/retry",
  "validationErrors": null
}
```

## Read endpoint

### `GET /all`

Returns normalized stored-message DTOs with paging metadata.

Supported query parameters:

- `page`: zero-based page index, default `0`
- `size`: page size, default `20`, max `100`
- `destination`: case-insensitive contains filter
- `deliveryMode`: case-insensitive contains filter
- `innerMessageId`: case-insensitive contains filter on descriptive payload metadata, not a unique key
- `publishStatus`: exact filter, one of `PENDING`, `PUBLISHED`, `FAILED`
- `createdAtFrom`: ISO-8601 local date-time lower bound
- `createdAtTo`: ISO-8601 local date-time upper bound
- `publishedAtFrom`: ISO-8601 local date-time lower bound
- `publishedAtTo`: ISO-8601 local date-time upper bound
- `sortBy`: `createdAt`, `priority`, `destination`, or `innerMessageId`
- `sortDirection`: `asc` or `desc`

Example:

```text
GET /api/v1/messages/all?page=0&size=20&publishStatus=FAILED&createdAtFrom=2026-04-21T00:00:00&createdAtTo=2026-04-21T23:59:59&sortBy=createdAt&sortDirection=desc
```

Representative response:

```json
{
  "items": [
    {
      "id": 1,
      "innerMessageId": "001",
      "destination": "solace/java/direct/system-01",
      "deliveryMode": "PERSISTENT",
      "priority": 3,
      "publishStatus": "PUBLISHED",
      "failureReason": null,
      "publishedAt": "2026-04-20T19:55:10",
      "properties": {
        "property01": "value01"
      },
      "payload": {
        "type": "binary",
        "content": "01001000 01100101 01101100",
        "createdAt": null,
        "updatedAt": null
      },
      "createdAt": "2026-04-20T19:55:00",
      "updatedAt": "2026-04-20T19:55:10"
    },
    {
      "id": 2,
      "innerMessageId": "002",
      "destination": "solace/java/direct/system-02",
      "deliveryMode": "DIRECT",
      "priority": 1,
      "publishStatus": "FAILED",
      "failureReason": "Failed to publish message to Solace broker",
      "publishedAt": null,
      "properties": {},
      "payload": {
        "type": "binary",
        "content": "01010111 01101111 01110010 01101100 01100100",
        "createdAt": null,
        "updatedAt": null
      },
      "createdAt": "2026-04-20T20:00:00",
      "updatedAt": "2026-04-20T20:00:01"
    }
  ],
  "page": 0,
  "size": 20,
  "totalElements": 2,
  "totalPages": 1,
  "first": true,
  "last": true
}
```

Invalid date filter, `400 Bad Request`:

```json
{
  "timestamp": "2026-04-20T19:55:00Z",
  "status": 400,
  "error": "Bad Request",
  "message": "createdAtFrom must be a valid ISO-8601 date-time",
  "path": "/api/v1/messages/all",
  "validationErrors": null
}
```

Invalid publish status, `400 Bad Request`:

```json
{
  "timestamp": "2026-04-20T19:55:00Z",
  "status": 400,
  "error": "Bad Request",
  "message": "publishStatus must be one of PENDING, PUBLISHED, FAILED",
  "path": "/api/v1/messages/all",
  "validationErrors": null
}
```

## Development

Run tests:

```bash
mvn test
```

## Notes

- Stored connection parameters are not persisted with the message.
- Retry uses server-side Solace configuration, not the original request credentials.
- The read API returns normalized DTOs, including `properties` as a plain object map.
