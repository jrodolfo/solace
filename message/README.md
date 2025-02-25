# Message Project

Welcome to the **Message** project! This repository provides a simple RESTful API to store and retrieve messages from a database. It is built using **Spring Boot** and **Java 21**, leveraging **Spring Data JPA** for database interactions. The application can be run locally or in a containerized environment using Docker.

This project is based on the tutorial “[Accessing data with MySQL](https://spring.io/guides/gs/accessing-data-mysql)” and demonstrates how to handle a One-to-Many relationship between a `Message` entity and its `properties`, and One-to-one relationships between `Message` and `Attribute`, and between `Message` and `Payload`.

## Features

- **Create Messages**: Use a POST endpoint to add new messages.
- **Retrieve Messages**: Use a GET endpoint to list all messages.
- **Persistence**: Messages are persisted in a relational database via JPA.

## Technologies Used

- **Java 21**
- **Spring Boot** (Spring Data JPA, Spring Web)
- **Docker** (optional for containerized deployment)

## Prerequisites

- **Java 21** installed on your system.
- **Maven** or **Gradle** if you plan to build without Docker.
- **Docker** (optional) if you choose to containerize the application.

## Getting Started

1. **Clone the repository**  
   Clone or download this project to your local machine.

2. **Set up the database**
    - Ensure you have a MySQL or compatible database running (you can also use Docker Compose if provided).
    - Update the application’s configuration properties if necessary (e.g., username, password, JDBC URL).

3. **Build and Run**  
   In the project’s root directory, run one of the following:
    - **Gradle**:
      ```bash
      ./gradlew bootRun
      ```
    - **Maven**:
      ```bash
      ./mvnw spring-boot:run
      ```

   These commands will start the Spring Boot application, and you should see logs indicating successful startup.

4. **Docker Option** (if available)
    - Build and run the Docker container using a Compose file or your preferred Docker commands.
    - Adjust ports or volumes as needed for your environment.

## Usage

Once the application is running, interact with the service via HTTP endpoints. By default, the application runs on port 8080.

### Retrieving All Messages

- **Endpoint**: `GET http://localhost:8080/message/all`
- **Response**: A JSON array containing existing messages.

```bash
curl http://localhost:8080/message/all
```

### Adding a New Message

- **Endpoint**: `POST http://localhost:8080/message/add`
- **Request Body**: JSON representing the new message, for example:

```bash
$ curl -X POST http://localhost:8080/demo/add \
     -H "Content-Type: application/json" \
     -d '{
           "userName": "solace-cloud-client",
           "password": "super-difficult",
           "host": "wss://mr-connection-blahblahblah.messaging.solace.cloud:443",
           "vpnName": "my-solace-broker-on-aws",
           "topicName": "solace/java/direct/system-01",

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
               "type": "binary",
               "content": "01001000 01100101 01101100 01101100"
             }

           }
         }'
```

- **Response**: A confirmation message ("saved") upon successful addition.

## License

This project is provided as-is without any specific license attached. Before using this code in production or for commercial purposes, please verify any license or usage restrictions that might apply.

## Contributing

Contributions or suggestions for improvements are welcome. Feel free to open a pull request or file an issue to discuss any changes.

---

**Thank you for checking out the Message project!** If you have any questions or run into issues, please open an issue in the repository. We welcome your feedback.
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
