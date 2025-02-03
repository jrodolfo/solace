# Solace Subscriber Application

This project provides functionality to connect and interact with a **Solace PubSub+ Broker**. It demonstrates how to configure and use Solace messaging APIs in a Java application by leveraging environment variables and properties for authentication and connection setup.

## Features

- Configures connection parameters to a Solace PubSub+ Broker.
- Supports publisher and receiver properties configuration.
- Handles reconnection attempts and configurable retry limits.
- Retrieves sensitive connection data (e.g., host, VPN name, username, and password) from environment variables.
- Complies with best practices for Solace PubSub+ Broker connectivity.

## How It Works

The project includes a utility class, `AccessProperties`, which encapsulates the logic for retrieving and validating the connection parameters before initializing them in a `Properties` object suitable for both Solace publishers and receivers.

### Key Methods

1. **`getPropertiesPublisher()`**:
    - Returns a pre-configured `Properties` object for connecting and publishing to the Solace Broker.

2. **`getPropertiesReceiver()`**:
    - Returns a `Properties` object for connecting and receiving messages from the Solace Broker.
    - Includes configuration for auto-reapplying direct subscriptions upon reconnect.

### Environment Variables

The following environment variables need to be set before running the application:

- `SOLACE_CLOUD_HOST`: The host URL of the Solace PubSub+ Broker (formatted as `host:port`).
- `SOLACE_CLOUD_VPN`: The Virtual Private Network (VPN) name.
- `SOLACE_CLOUD_USERNAME`: The username for authentication.
- `SOLACE_CLOUD_PASSWORD`: The password for authentication.

Read this document to learn how to configure the Solace Broker on the cloud and get the values of these 4 variables:

```bash
  ../solace-broker-api/doc/how-to/09-pubsub-plus-on-cloud.txt
```

### Connection Properties

The class uses Solace-provided configurations to establish and maintain a reliable connection, including:

- **Host**: Defined using the `TransportLayerProperties.HOST`.
- **VPN Name**: Defined using the `SolaceProperties.ServiceProperties.VPN_NAME`.
- **Authentication**: Configured using `AuthenticationProperties.SCHEME_BASIC_USER_NAME` and `AuthenticationProperties.SCHEME_BASIC_PASSWORD`.
- **Reconnection Attempts**: Configurable through `TransportLayerProperties.RECONNECTION_ATTEMPTS`.
- **Retries per Host**: Configurable through `TransportLayerProperties.CONNECTION_RETRIES_PER_HOST`.

### Error Handling

If any of the required environment variables are not set or invalid, the application:

- Logs a detailed error message.
- Throws an exception to ensure visibility into misconfiguration issues.

---

## Prerequisites

Before running this application, ensure the following:

1. **Java SDK 21** installed.
2. **Environment variables** for Solace connection configuration are properly set.
3. Access to a Solace PubSub+ Broker instance.

---

## Getting Started

### Clone the Repository

```bash
git clone https://github.ibm.com/roliveir/solace-subscriber
cd solace-subscriber
```

### Build the Project

Use a build tool (e.g., Maven or Gradle) to build the project. Ensure that Lombok and the required Solace dependencies are properly configured.

Example for Maven:

```bash
mvn clean install
```

### Run the Project

Ensure the necessary environment variables are set:

```bash
export SOLACE_CLOUD_HOST=your.broker.host:port
export SOLACE_CLOUD_VPN=your_vpn_name
export SOLACE_CLOUD_USERNAME=your_username
export SOLACE_CLOUD_PASSWORD=your_password
```

Start the application:

```bash
java -jar target/solace-subscriber-1.0-SNAPSHOT.jar
```

---

## Configuration

### Customizing Connection Properties

You can modify reconnection behavior, retries per host, or other settings by updating the respective constants in the `AccessProperties` class.

---

## References

- [Solace Documentation: Configuring Connection](https://docs.solace.com/Solace-PubSub-Messaging-APIs/API-Developer-Guide/Configuring-Connection-T.htm)
- [Solace PubSub+ Messaging APIs](https://docs.solace.com/Solace-PubSub-Messaging-APIs/Default.htm)

---

## Contact

For issues or inquiries, feel free to contact the maintainer:

- **Name:** Rod Oliveira
- **Role:** Software Developer
- **Email:** roliveir@ca.ibm.com
- **GitHub:** https://github.ibm.com/roliveir
- **LinkedIn:** https://www.linkedin.com/in/rodoliveira
- **Webpage:** https://jrodolfo.net

---
