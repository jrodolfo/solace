# Sample Destinations

This repository uses a small, fixed set of Solace topic names for examples,
tests, curl/Postman/JMeter artifacts, screenshots, and release validation.

These values are demo and test destinations. They are not required Solace naming
conventions and they are not backend runtime configuration.

## Direct Topic Samples

| Label | Destination |
| --- | --- |
| system 01 | `solace/java/direct/system-01` |
| system 02 | `solace/java/direct/system-02` |
| system 03 | `solace/java/direct/system-03` |
| system 04 | `solace/java/direct/system-04` |

## Subscriber Pattern

The subscriber listens to the broader direct topic pattern:

```text
solace/java/direct/system-0*
```

That pattern matches the four sample destinations above.

## Tooling Artifacts

The curl, Postman, and JMeter files keep literal destination examples so each
tool artifact remains portable and easy to use on its own. When changing sample
destinations, update those literals alongside this reference.
