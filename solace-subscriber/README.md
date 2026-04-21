# Solace Subscriber

`solace-subscriber` is a small Java 21 command-line receiver for a Solace PubSub+ broker. It connects with environment-based credentials, subscribes to the direct topic pattern `solace/java/direct/system-0*`, logs inbound traffic, and reports reconnect or discard conditions while it runs.

For the repo-level relationships between the subscriber, UI, and broker API, see [../doc/architecture.md](../doc/architecture.md).

## Requirements

- JDK 21
- Maven 3.9+
- Access to a Solace PubSub+ broker
- These environment variables:
  - `SOLACE_CLOUD_HOST`
  - `SOLACE_CLOUD_VPN`
  - `SOLACE_CLOUD_USERNAME`
  - `SOLACE_CLOUD_PASSWORD`

The subscriber reads those values through `AccessProperties`. If any are missing or blank, startup fails with a typed `SubscriberConfigurationException` and logs a clear configuration error.

## Build And Test

From the `solace-subscriber` directory:

```bash
mvn test
```

The module is configured for Java release 21 and currently includes unit coverage for:

- subscriber connection property resolution
- missing environment-variable handling
- direct receiver discard/message-state tracking

## Run

Export the required environment variables first:

```bash
export SOLACE_CLOUD_HOST=tcps://your-broker-host:55443
export SOLACE_CLOUD_VPN=your-vpn
export SOLACE_CLOUD_USERNAME=your-username
export SOLACE_CLOUD_PASSWORD=your-password
```

Then build and run:

```bash
mvn package
java -jar target/solace-subscriber-1.0-SNAPSHOT.jar
```

## Runtime Behavior

At startup the receiver:

1. loads and validates connection properties from the environment
2. creates and connects a Solace `MessagingService`
3. applies direct receiver reconnect settings
4. subscribes to `solace/java/direct/system-0*`
5. logs inbound-message throughput once per second

While running, it logs:

- `INFO` for normal lifecycle and throughput events
- `WARNING` for reconnect attempts or egress discard detection
- `SEVERE` for real startup or runtime failures

Press `ENTER` to shut the subscriber down cleanly.

## Notes

- The subscribed topic pattern is defined in [Constants.java](src/main/java/org/orgname/solace/subscriber/Constants.java).
- The receiver is currently a single-process command-line tool, not a Spring service.
- If you need help obtaining broker credentials or local broker setup guidance, see [../doc/how-to/](../doc/how-to/).

## References

- [Solace connection configuration docs](https://docs.solace.com/Solace-PubSub-Messaging-APIs/API-Developer-Guide/Configuring-Connection-T.htm)
- [Solace PubSub+ messaging APIs](https://docs.solace.com/Solace-PubSub-Messaging-APIs/Default.htm)
