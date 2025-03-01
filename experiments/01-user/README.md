# Spring Boot Application – Extended User Properties

This project is based on the tutorial “[Accessing data with MySQL](https://spring.io/guides/gs/accessing-data-mysql)” and demonstrates how to handle a One-to-Many relationship between a `User` entity and its properties.

## Why Extend `User`?

Instead of having just a `name` and an `email`, each user can have one or more properties with key-value pairs:

```json
{
  "name": "Rod Oliveira",
  "email": "jrodolfo@gmail.com",
  "properties": [
    {"propertyKey": "key01", "propertyValue": "value01"},
    {"propertyKey": "key02", "propertyValue": "value02"},
    {"propertyKey": "key03", "propertyValue": "value03"}
  ]
}
```

This setup demonstrates how to handle a One-to-Many relationship (one user has zero or many properties).

## Run the Application

1. Make sure your Docker is up and running.
2. Verify no other applications are using the same ports.
3. From the project’s root directory, run either:

```bash
./gradlew bootRun
```
**or**
```bash
./mvnw spring-boot:run
```

These commands will build and run the application, automatically finding and using the `compose.yaml` file (if applicable).

## Test the Application

With the application running (on port 8080), you have two primary HTTP endpoints to test:

1. **GET** `http://localhost:8080/demo/all` – Retrieves all users and their properties.  
2. **POST** `http://localhost:8080/demo/add` – Adds a new user.

### Example: Adding a User

Use `curl` to add a new user:

```bash
curl -X POST http://localhost:8080/demo/add \
     -H "Content-Type: application/json" \
     -d '{
           "name": "Rod Oliveira",
           "email": "jrodolfo@gmail.com",
           "properties": [
               {"propertyKey": "key01", "propertyValue": "value01"},
               {"propertyKey": "key02", "propertyValue": "value02"},
               {"propertyKey": "key03", "propertyValue": "value03"}
           ]
         }'
```

The expected reply should be:

```bash
saved
```


### Example: Retrieving All Users

Use `curl` to retrieve all existing users:

```bash
curl http://localhost:8080/demo/all
```

The expected response will look like:

```json
[
  {
    "createdAt": "2025-02-24T08:08:35.216218",
    "updatedAt": "2025-02-24T08:08:35.216218",
    "userId": 1,
    "name": "Rod Oliveira",
    "email": "jrodolfo@gmail.com",
    "properties": [
      {
        "createdAt": "2025-02-24T08:08:35.311541",
        "updatedAt": "2025-02-24T08:08:35.311541",
        "id": 1,
        "propertyKey": "key01",
        "propertyValue": "value01"
      },
      {
        "createdAt": "2025-02-24T08:08:35.326244",
        "updatedAt": "2025-02-24T08:08:35.326244",
        "id": 2,
        "propertyKey": "key02",
        "propertyValue": "value02"
      },
      {
        "createdAt": "2025-02-24T08:08:35.326782",
        "updatedAt": "2025-02-24T08:08:35.326782",
        "id": 3,
        "propertyKey": "key03",
        "propertyValue": "value03"
      }
    ]
  },
  {
    "createdAt": "2025-02-24T08:09:11.494301",
    "updatedAt": "2025-02-24T08:09:11.494301",
    "userId": 2,
    "name": "Rod Oliveira",
    "email": "jrodolfo@outlook.com",
    "properties": [
      {
        "createdAt": "2025-02-24T08:09:11.49872",
        "updatedAt": "2025-02-24T08:09:11.49872",
        "id": 4,
        "propertyKey": "key01",
        "propertyValue": "value01"
      },
      {
        "createdAt": "2025-02-24T08:09:11.499269",
        "updatedAt": "2025-02-24T08:09:11.499269",
        "id": 5,
        "propertyKey": "key02",
        "propertyValue": "value02"
      },
      {
        "createdAt": "2025-02-24T08:09:11.499818",
        "updatedAt": "2025-02-24T08:09:11.499818",
        "id": 6,
        "propertyKey": "key03",
        "propertyValue": "value03"
      }
    ]
  }
]
```
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