# Solace Smoke Test

This guide walks through an end-to-end smoke test using all three project
modules:

- `solace-broker-api`
- `solace-publisher-ui`
- `solace-subscriber`

The goal is to publish one message from the React UI, receive it through Solace
Cloud, observe it in the Subscriber container logs, and confirm that the broker
API persisted the publish attempt in MySQL.

Use your own Solace Cloud values. The screenshots are examples only.

## Prerequisites

Before starting, make sure you have:

- Docker Desktop running
- a Solace Cloud account and event broker service
- the four `SOLACE_CLOUD_*` environment variables available in your terminal

If you still need to create the Solace Cloud account or collect the four
connection values, follow
[Solace Cloud Account, Demo, and Environment Variables](01-solace-cloud-account-demo-and-env-vars.md)
first.

The smoke test uses the sample destinations documented in
[../reference/sample-destinations.md](../reference/sample-destinations.md).

## 1. Start Docker Desktop

Start Docker Desktop before running the project scripts. The project runtime
uses Docker Compose for MySQL, the Broker API, the Publisher UI, and the
Subscriber.

![Docker Desktop application started](../images/smoke-test/01.png)

## 2. Start the Docker Runtime

From the repository root, build and start the full Docker runtime:

```bash
./scripts/docker-build-all.sh
./scripts/docker-start.sh
```

- `docker-build-all.sh` - the first run can take longer because Docker may need
  to download base images and build each module image.
- `docker-start.sh` starts the Broker API, Publisher UI, Subscriber, and MySQL services.

## 3. Confirm Runtime Startup

Check the runtime status:

```bash
./scripts/docker-status.sh
```

## 4. Confirm MySQL, Subscriber, Broker API, and Publisher UI in Docker Desktop

Open Docker Desktop and confirm that the MySQL image and container are present.
The Broker API uses this database container to store publish attempts.

![Docker Desktop showing MySQL, Subscriber, Broker API, and Publisher UI containers](../images/smoke-test/06.png)

## 5. Open the Publisher UI

Open the React Publisher UI in your browser:

```text
http://localhost:5173
```

The Docker workflow exposes the Publisher UI on port `5173`.

![Publisher UI running in the browser](../images/smoke-test/07.png)

## 6. Open Solace Cloud

Log in to Solace Cloud:

```text
https://console.solace.cloud/login
```

![Solace Cloud login page](../images/smoke-test/08.png)

Open **Cluster Manager**, select your broker service, and then select
**Open Broker Manager**.

![Solace Cloud Cluster Manager with Open Broker Manager option](../images/smoke-test/09.png)

## 7. Connect Broker Manager Publisher and Subscriber

In Broker Manager, open **Try Me!** and select **Connect** in both the publisher
and Subscriber panels.

![Broker Manager Try Me page with Publisher and Subscriber panels](../images/smoke-test/10.png)

Edit the **Topic Subscriber** field.

![Topic Subscriber field ready to edit](../images/smoke-test/11.png)

Use this Subscriber topic pattern:

```text
solace/java/direct/system-0*
```

That value is documented in
[../reference/sample-destinations.md](../reference/sample-destinations.md).

![Topic Subscriber field using the sample wildcard topic](../images/smoke-test/12.png)

Select **Subscribe**.

![Subscribe button selected for the wildcard topic](../images/smoke-test/13.png)

Confirm that the topic appears under **Subscribed Topics**.

![Subscribed Topics list showing the wildcard topic](../images/smoke-test/14.png)

## 8. Collect the Solace Cloud Connection Values

In Solace Cloud, collect the four values required by this project:

- broker URL for `SOLACE_CLOUD_HOST`
- message VPN for `SOLACE_CLOUD_VPN`
- client username for `SOLACE_CLOUD_USERNAME`
- client password for `SOLACE_CLOUD_PASSWORD`

![Solace Cloud connection details](../images/smoke-test/15.png)

Enter those values in the **Connection Broker Access** section of the publisher
UI.

![Publisher UI Connection Broker Access section](../images/smoke-test/16.png)

## 9. Fill Out the Publish Form

For the **Destination** field, use:

```text
solace/java/direct/system-01
```

That value is also documented in
[../reference/sample-destinations.md](../reference/sample-destinations.md).

![Publisher UI with message fields filled](../images/smoke-test/18.png)

![Publisher UI with destination and payload fields filled](../images/smoke-test/19.png)

Message properties are optional.

When the form is complete, select **Publish Message**.

![Publisher UI Publish Message button](../images/smoke-test/20.png)

The UI should show a successful publish response.

![Publisher UI showing successful publish result](../images/smoke-test/21.png)

If you scroll down, you can see the response.

![Publisher UI showing successful publish result](../images/smoke-test/25.png)

## 10. Verify Logs and Solace Cloud

Check the Docker logs. From the repository root, run:

```bash
./scripts/docker-logs.sh subscriber
```

The Subscriber log should show that it received the message published to
`solace/java/direct/system-01`.

To follow all service logs, run:

```bash
./scripts/docker-logs.sh
```

In Docker Desktop, you can also open
`Containers > solace > solace-subscriber > Logs`.

![Docker logs showing Subscriber activity](../images/smoke-test/22.png)

In Broker Manager, confirm that the subscribed topic receives the message.

![Solace Cloud Broker Manager showing the received message](../images/smoke-test/23.png)

## 11. Verify the Message in the Publisher UI Read Tab

The Publisher UI can also confirm that the Broker API persisted the publish
attempt. Open the Read tab, load the records, and verify that
the latest message shows the expected destination, payload, lifecycle status,
and timestamps.

![Publisher UI Read tab showing the stored published message](../images/smoke-test/24.png)
![Publisher UI Read tab showing the stored published message](../images/smoke-test/26.png)
![Publisher UI Read tab showing the stored published message](../images/smoke-test/27.png)

## 12. Verify the Database

You can inspect the stored publish attempt in MySQL. One GUI option is
Beekeeper Studio Community Edition:

```text
https://www.beekeeperstudio.io
```

Use these connection settings:

| Field | Value |
| --- | --- |
| Connection type | `MySQL` |
| Host | `localhost` |
| Port | `3307` |
| User | `myuser` |
| Password | `secret` |
| Database | `solace` |

The smoke-test database queries are available in
[../mysql/smoke-test-queries.sql](../mysql/smoke-test-queries.sql).

In IntelliJ, open that SQL file, select the `solace` MySQL data source, and run
the queries directly from the editor.

For the schema reference, see
[../mysql/mysql-schema.sql](../mysql/mysql-schema.sql).

## If the Database Connection Fails

Make sure the MySQL container is running:

```bash
./scripts/docker-status.sh
```

If it is not running, restart the Docker runtime from the repository root:

```bash
./scripts/docker-restart.sh
```

Then retry the Beekeeper Studio connection. If `localhost` does not work on
your machine, use `127.0.0.1` with the same port, `3307`.

![Beekeeper Studio showing the stored published message on table message](../images/smoke-test/28.png)
![Beekeeper Studio showing the stored published message on table payload](../images/smoke-test/29.png)
![Beekeeper Studio showing the stored published message on table property](../images/smoke-test/30.png)
