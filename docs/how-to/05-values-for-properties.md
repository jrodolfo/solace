# Message Properties

Solace Event Broker messages can include properties that help control message behavior and routing.

In this project, `properties` are optional custom key/value metadata sent as part of the publish request and stored with the publish attempt.

Common Solace message property concepts include:

1. Time-to-live, or TTL, sets the lifespan of a message in milliseconds. A value of `0` means the message never expires.
2. Dead Message Queue, or DMQ, eligibility controls whether messages can be moved to a Dead Message Queue after exceeding TTL or maximum redelivery attempts.
3. Eliding eligibility controls whether a message can be skipped under certain conditions to optimize bandwidth.
4. Partition key can help route messages with the same key to the same partition, preserving order.

The application-level `properties` map is useful for metadata such as source system, region, correlation values, or business identifiers.

## Reference

- [Setting Solace JMS Message Properties](https://docs.solace.com/API/Solace-JMS-API/Setting-Message-Properties.htm)
