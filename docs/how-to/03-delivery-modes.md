# Delivery Modes

Solace Event Brokers support several message delivery modes, each suited for different use cases.

## Supported By This Project

This project uses the `deliveryMode` field in the publish request and currently supports:

- `DIRECT`
- `PERSISTENT`

## Solace Delivery Modes

1. Direct messaging is designed for high-speed applications that can tolerate occasional message loss. Messages are delivered to consumers with matching topic subscriptions but are not persisted on the event broker.
2. Persistent, or guaranteed, messaging ensures that messages are saved in the event broker message spool before being acknowledged back to publishers. This mode is used when data must be received by the consuming application, even if consumers are offline.
3. Non-persistent messaging is used to fulfill JMS specification requirements and is similar to persistent messaging in function, but without the same persistence guarantee.
4. Transacted delivery supports session-based and XA transactions, ensuring that a series of operations are completed successfully before being committed.

Each mode offers different reliability and performance tradeoffs.

## References

- [Solace Message Delivery Modes](https://docs.solace.com/API/API-Developer-Guide/Message-Delivery-Modes.htm)
- [Delivery Modes: Direct Messaging vs Persistent Messaging](https://solace.com/blog/delivery-modes-direct-messaging-vs-persistent-messaging/)
