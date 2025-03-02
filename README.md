# Solace Projects Overview

Welcome to the Solace Suite! This repository provides everything you need to work with the **Solace Broker API** and the accompanying **UI application**. Follow this guide to get started, test the API, and dive into the features.

---

## Project Structure

### 1. **Solace Broker API**
The backend service that powers message exchanges. All integrations and API tests are managed here.

**Path:** `solace/solace-broker-api`

### 2. **Solace Publisher UI**
The React-based application that provides a user-friendly interface for interacting with the broker and sending messages.

**Path:** `solace/solace-publisher-ui`

---

## Running the UI Application

The **Solace Publisher UI** uses the following technologies:

```text
React.........18.3.1
React DOM.....18.3.1
TypeScript....5.7.3
Node..........23.6.0
NPM...........11.0.0
Axios.........1.7.9
Bootstrap.....5.3.3
```

### Steps to Run the UI Application:

1. Open your terminal of choice.
2. Navigate to the project directory:
   ```bash
   cd solace/solace-publisher-ui
   ```
3. Install dependencies:
   ```bash
   npm install
   ```
4. Start the development server:
   ```bash
   npm run dev
   ```
5. Open your browser and go to:  
   **[http://localhost:5173/](http://localhost:5173/)**

---

## Prerequisites

Before running the UI, **make sure the following are ready:**

1. The Solace Broker API is up and running.
2. You have the **four Solace Cloud credentials**:
   - `SOLACE_CLOUD_USERNAME`
   - `SOLACE_CLOUD_PASSWORD`
   - `SOLACE_CLOUD_HOST`
   - `SOLACE_CLOUD_VPN`

   Learn how to get these credentials here:  
   `solace/solace-broker-api/doc/how-to/01-using-solace-pubsubplus.txt`

---

## Testing the Solace Broker API

You can test the **Solace Broker API** in three different ways:

### 1. **Using JMeter**
- Import the JMeter script from:  
  `../solace-broker-api/doc/jmeter`

### 2. **Using Postman**
- Import the Postman collection from:  
  `../solace/solace-broker-api/doc/postman`

### 3. **Using the UI Application**
- Follow the steps described above to run the UI:  
  `../solace/solace-publisher-ui`

---

## Sending a Test Message

Here's an example message format you can use to test the application:

```json
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
}
```

For Postman:
```json
{
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
}
```

For Solace Publisher UI:
```json
{
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
```


### Testing Topic:
Use the following topic for your tests:  
`solace/java/direct/system-01`

---

## Additional Resources

- **How to Use Solace PubSub+**  
  Documentation is available at:  
  `solace/solace-broker-api/doc/how-to/01-using-solace-pubsubplus.txt`

- For more details about the UI or API, check their respective **README.md** files.

---

## Testing the Solace Broker API

You can test the **Solace Broker API** in 3 ways:

1. **Using JMeter**  
   Import the script from the following path:  
   `../solace-broker-api/doc/jmeter`

2. **Using Postman**  
   Import the collection from the following path:  
   `../solace/solace-broker-api/doc/postman`

3. **Using the UI Application**  
   Follow the steps described in this **README.md** file to run the application:  
   `../solace/solace-publisher-ui/README.md`

Enjoy exploring the Solace Platform! 🚀

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
