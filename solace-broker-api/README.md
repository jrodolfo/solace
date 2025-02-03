# Solace Broker API

This repository contains a Spring Boot application that exposes RESTful APIs and integrates with OpenAPI for API documentation and testing using Swagger UI.

## Features

- REST API endpoint for managing messages.
- Integrated OpenAPI 3.1 documentation.
- Swagger UI for testing and interacting with the API.
- Configurable `application.yml` for flexibility in API and Swagger settings.
- Actuator support for application monitoring.

---

## Technology Stack

- **Java 21**
- **Spring Boot**
    - Spring Web
    - Spring Actuator
- **SpringDoc OpenAPI** for API documentation and Swagger UI
- **Maven** for dependency management

---

## Requirements

- Java 21 or higher
- Maven 3.9 or higher
- Any IDE for development (e.g., IntelliJ IDEA)
- A REST client (Postman, Swagger UI, JMeter or browser-based)

---

## Setting Up the Application Locally

Follow these steps to get the application up and running on your local machine:

### Prerequisites

1. **Install Java JDK 21**  
   [Download JDK 21](https://www.oracle.com/java/technologies/downloads/#java21)

2. **Set the JAVA_HOME System Variable**
    - Register the `JAVA_HOME` system variable to point to the JDK installation directory.

3. **Install Apache Maven 3.9.9**  
   [Download Maven 3.9.9](https://maven.apache.org/download.cgi)

4. **Set the MVN_HOME System Variable**
    - Register the `MVN_HOME` system variable to point to the Maven installation directory.

5. **Update the Path System Variable**
    - Append the following to the `Path` system variable:
        - `JAVA_HOME\bin`
        - `MVN_HOME\bin`

6. **Install IntelliJ IDEA Community Edition**  
   [Download IntelliJ IDEA Community Edition](https://www.jetbrains.com/idea/)

7. **Install Git Bash**  
   [Download Git Bash](https://git-scm.com/downloads)

8. **Set Solace Environment Variables**
    - Register the following **4 system variables**:
        - `SOLACE_CLOUD_HOST`: The host URL of the Solace PubSub+ Broker (formatted as `host:port`)
        - `SOLACE_CLOUD_VPN`: The Virtual Private Network (VPN) name
        - `SOLACE_CLOUD_USERNAME`: The username for authentication
        - `SOLACE_CLOUD_PASSWORD`: The password for authentication
    - Ask the developer for the values of these variables
    - Or read this document to learn how to configure the Solace Broker on the cloud and get the values of these 4 variables:

```bash
  ../solace/solace-broker-api/doc/how-to/01-using-solace-pubsubplus.txt
```

---

### Setup Instructions

1. **Clone the Repository**  
   Run the following command to clone the repository:
   ```bash
   git clone https://github.ibm.com/roliveir/solace-broker-api.git
   cd solace-broker-api
   ```

2. **Load the Project into IntelliJ**
    - Open IntelliJ and load the cloned project.

3. **Build and Run the Application**
    - Use the Maven build tool:
      ```bash
      mvn clean install
      mvn spring-boot:run
      ```

4. **Install Postman**  
   [Download Postman](https://www.postman.com/downloads/)

5. **Load the Postman Collection**
    - Import the Postman collection available in the repository:
      ```
      https://github.ibm.com/roliveir/solace-broker-api/tree/main/doc/postman
      ```

6. **Send a Request via Postman**
    - Make sure the application is running before sending a request.
    - Use the loaded Postman collection to test the API endpoints.

7. The application runs on port `8081` (default). You can change the port in `application.yml` if needed.

---

By following these steps, you'll have the application running and ready to consume API requests locally.

---

## API Endpoints

### Base URL

[http://localhost:8081/api/v1/messages/](localhost:8081/api/v1/messages/)

### Message Endpoint

- **POST /message**  
  Sends a message using `application/json`, `application/xml`, or `application/x-www-form-urlencoded`.

  #### Sample Request (JSON):
  ```
  {
    "messageId": "001",
    "destination": "solace/java/direct/system-01",
    "deliveryMode": "PERSISTENT",
    "priority": 3,
    "properties": {
        "property01": "value01",
        "property02": "value02"
    },
    "payload": {
        "type": "binary",
        "content": "01001000 01100101 01101100 01101100 01101111 00101100 00100000 01010111 01101111 01110010 01101100 01100100 00100001"
    }
    ```

  #### Sample Response:
  ```
  {
    "destination": "solace/java/direct/system-01",
    "content": "01001000 01100101 01101100 01101100 01101111 00101100 00100000 01010111 01101111 01110010 01101100 01100100 00100001"
  }
  ```

---

## OpenAPI and Swagger UI

To view the API documentation and interact with the endpointSs:

1. Start the application.
2. Open Swagger UI in your browser using the URL:
   ```
   http://localhost:8081/swagger-ui/index.html
   ```

### API Documentation (JSON)

To view the raw OpenAPI spec, visit the `/api-docs` endpoint:
    ```
    http://localhost:8081/api-docs
    ```

---

## Configuration Details

The application uses `application.yml` for configuration. Some key configurations include:

### Server Settings
```yaml
server:
  port: 8081
```

### Swagger Configuration
```yaml
springdoc:
  api-docs:
    path: /api-docs
  swagger-ui:
    display-request-duration: true
    operations-sorter: method
```

### Actuator Configuration
```yaml
management:
  endpoints:
    web:
      base-path: /rest/actuator
      exposure:
        include: '*'
```

---

## Logging

To enable debugging:
1. Update `application.yml`:
   ```yaml
   logging:
     level:
       org.springframework: DEBUG
       org.springdoc: DEBUG
   ```
2. Restart the server.

Logs will be available in the console.

---

## Development and Testing

### Prerequisites
Make sure Java 21 and Maven are installed. Verify by running:

```bash
java -version
mvn -version
```

### Running Unit Tests
To execute tests, run:

```bash
mvn test
```

### Running Smoke Tests
To execute smoke tests, run the Postman collection available at the doc folder of this repository:

```bash
  ../solace-broker-api/doc/postman/solace-producer-emulator.postman_collection.json
```

### Running Stress Tests
To execute smoke tests, run the JMeter script available at the doc folder of this repository:

```bash
  ../solace-broker-api/doc/jmeter/solace-producer-emulator.jmx
```
---

## Troubleshooting

### Swagger UI Shows "No operations defined in spec!"
- Ensure the controller uses correct annotations (`@RestController`, `@RequestMapping`).
- Verify the `paths-to-match` in `application.yml` matches your API paths.
- Check for logs with:
  ```yaml
  logging:
    level:
      org.springdoc: DEBUG
  ```

### API Not Detected in Swagger
- Ensure all dependencies for `springdoc-openapi` are correctly configured.
- Add OpenAPI annotations (`@Operation`, `@Tag`) for specific endpoints.

---

## Future Improvements

- Add more REST endpoints for additional use cases.
- Enhance payload validations using `@Valid` and DTOs.
- Add database integration for persistent storage.
- Implement security using Spring Security and OAuth2.

---

## Contact

For issues or inquiries, feel free to contact the maintainer:

- **Name:** Rod Oliveira
- **Role:** Software Developer
- **Emails:** jrodolfo@gmail.com & roliveir@ca.ibm.com 
- **GitHub links:** https://github.com/jrodolfo & https://github.ibm.com/roliveir
- **LinkedIn:** https://www.linkedin.com/in/rodoliveira
- **Webpage:** https://jrodolfo.net

---
