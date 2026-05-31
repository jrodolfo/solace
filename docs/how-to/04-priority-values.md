# Priority Values

Solace Event Brokers support ten levels of message priority, ranging from `0` to `9`.

- `0`: Lowest priority.
- `9`: Highest priority.

If a message does not define a priority, Solace treats it as priority `4` by default.

This project requires the `priority` field in publish requests and stores it with each publish attempt.

## Reference

- [Solace Message Priority](https://docs.solace.com/Messaging/Guaranteed-Msg/Message-Priority.htm)
