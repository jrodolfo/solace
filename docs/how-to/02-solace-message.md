# Publish Message Request Format

The broker API publishes messages through:

```text
POST /api/v1/messages/message
```

Broker connection values are read from the `SOLACE_CLOUD_*` environment variables.
Do not send Solace usernames or passwords in the request body.

Example JSON request:

```json
{
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
      "type": "BINARY",
      "content": "01001000 01100101 01101100 01101100"
    }
  }
}
```

## Fields

- `message`: Required outer wrapper expected by the broker API.
- `innerMessageId`: Descriptive message identifier provided by the caller. It is stored with the publish attempt, but it is not used as a uniqueness constraint.
- `destination`: The Solace topic or queue where the message should be published.
- `deliveryMode`: The requested Solace delivery mode. This project supports `PERSISTENT` and `DIRECT`.
- `priority`: Numeric priority for the message.
- `properties`: Optional custom key/value metadata for the message.
- `payload`: Required payload object containing the payload type and content.
- `payload.type`: The payload format. This project supports `TEXT`, `BINARY`, `JSON`, and `XML`. Lowercase values are accepted and normalized by the API.
- `payload.content`: The message payload content to publish.

This is the HTTP request contract used by the application. It is not intended to describe the full Solace Message Format wire-level structure.

## References

- [Inside a Solace Message](https://solace.com/blog/inside-solace-message-introduction)
- [Sending Text Data with Solace APIs](https://solace.com/blog/text-data-solace-apis-xml-json)
