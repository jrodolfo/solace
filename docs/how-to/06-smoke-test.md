# Solace Smoke Test

This guide walks through an end-to-end smoke test using all three project
modules:

- `solace-broker-api`
- `solace-publisher-ui`
- `solace-subscriber`

The goal is to publish one message from the React UI, receive it through Solace
Cloud, observe it in the local subscriber logs, and confirm that the broker API
persisted the publish attempt in MySQL.

Use your own Solace Cloud values. The screenshots are examples only.

## Prerequisites

Before starting, make sure you have:

- Docker Desktop running
- Java 21
- Maven
- Node.js and npm
- a Solace Cloud account and event broker service
- the four `SOLACE_CLOUD_*` environment variables available in your terminal

If you still need to create the Solace Cloud account or collect the four
connection values, follow
[Solace Cloud Account, Demo, and Environment Variables](01-solace-cloud-account-demo-and-env-vars.md)
first.

The smoke test uses the sample destinations documented in
[../reference/sample-destinations.md](../reference/sample-destinations.md).

## 1. Start Docker Desktop

Start Docker Desktop before running the project scripts. The broker API starts a
local MySQL container through Docker Compose.

![Docker Desktop application started](../images/smoke-test/01.png)

## 2. Build the Three Modules

From the repository root, run the build scripts:

```bash
cd scripts
./stop-all.sh
./build-all.sh
```

`stop-all.sh` clears any previous local run. `build-all.sh` builds the broker
API, publisher UI, and subscriber.

![Terminal running stop-all and build-all scripts](../images/smoke-test/02.png)

![Terminal showing build-all progress](../images/smoke-test/03.png)

## 3. Start the Three Modules

From the `scripts` directory, start the complete local stack:

```bash
./start-all.sh
```

The first run can take longer because Docker may need to download the MySQL
image.

![Terminal running start-all script](../images/smoke-test/04.png)

![Terminal showing API, UI, and subscriber startup logs](../images/smoke-test/05.png)

## 4. Confirm MySQL in Docker Desktop

Open Docker Desktop and confirm that the MySQL image and container are present.
The broker API uses this local database to store publish attempts.

![Docker Desktop showing local MySQL image and container](../images/smoke-test/06.png)

## 5. Open the Publisher UI

Open the React publisher UI in your browser:

```text
http://localhost:5173
```

If `5173` was already busy, use the Vite URL printed by `start-all.sh`.

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
and subscriber panels.

![Broker Manager Try Me page with Publisher and Subscriber panels](../images/smoke-test/10.png)

Edit the **Topic Subscriber** field.

![Topic Subscriber field ready to edit](../images/smoke-test/11.png)

Use this subscriber topic pattern:

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

Message properties are optional.

![Publisher UI with broker access fields filled](../images/smoke-test/17.png)

![Publisher UI with message fields filled](../images/smoke-test/18.png)

![Publisher UI with destination and payload fields filled](../images/smoke-test/19.png)

When the form is complete, select **Publish Message**.

![Publisher UI Publish Message button](../images/smoke-test/20.png)

The UI should show a successful publish response.

![Publisher UI showing successful publish result](../images/smoke-test/21.png)

## 10. Verify Logs and Solace Cloud

Check the terminal running `start-all.sh`. You should see logs from both:

- `solace-broker-api`
- `solace-subscriber`

The subscriber log should show that it received the message published to
`solace/java/direct/system-01`.

![Terminal logs showing broker API and subscriber activity](../images/smoke-test/22.png)

In Broker Manager, confirm that the subscribed topic receives the message.

![Solace Cloud Broker Manager showing the received message](../images/smoke-test/23.png)

## 11. Verify the Message in the Publisher UI Read Tab

The publisher UI can also confirm that the broker API persisted the publish
attempt. Open the read/stored-messages tab, load the records, and verify that
the latest message shows the expected destination, payload, lifecycle status,
and timestamps.

![Publisher UI Read tab showing the stored published message](../images/smoke-test/24.png)

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
docker ps --filter name=solace-mysql
```

If it is not running, start it from the broker API module:

```bash
cd solace-broker-api
docker compose up -d mysql
```

Then retry the Beekeeper Studio connection. If `localhost` does not work on
your machine, use `127.0.0.1` with the same port, `3307`.
