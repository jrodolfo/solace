# Solace Publisher UI

## What this app does

This application is designed to allow the user to send messages to the Solace Broker API. It is built with **React** and **TypeScript** to ensure optimal performance and scalability.


## Key Features

    - Real-time-ish updates using REST APIs
    - A modern and responsive UI for ease of use
    - Integration with the external tool Solace Broker API

# Technology Stack

The application uses the following technologies:

<pre style="font-family: 'Courier New', Courier, monospace;">
- React.........18.3.1
- React DOM.....18.3.1
- TypeScript....5.7.3
- Node..........23.6.0
- NPM...........11.0.0
- Axios.........1.7.9
- Bootstrap.....5.3.3
</pre>


## Running the Application

To run the application, open a terminal and execute the following commands:

```bash
cd solace-publisher-ui
npm install
npm run dev
```

Then open your browser and navigate to:

[http://localhost:5173/](http://localhost:5173/)

---

## Prerequisites

Before running the application, ensure the following:

1. The **solace-broker-api** application (located at `../solace/solace-broker-api`) is up and running.

2. You have the following **4 required credentials**:
    - `SOLACE_CLOUD_USERNAME`
    - `SOLACE_CLOUD_PASSWORD`
    - `SOLACE_CLOUD_HOST`
    - `SOLACE_CLOUD_VPN`

   Refer to the file

```bash
  ../solace/solace-broker-api/doc/how-to/01-using-solace-pubsubplus.txt
```

   to learn how to obtain these credentials.

---

## Testing the Application

You can use the message below for testing purposes:

### Sample Message:

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

### Topic for Testing:

Use this value for the topic:  
`solace/java/direct/system-01`


## React + TypeScript + Vite

This template provides a minimal setup to get React working in Vite with HMR and some ESLint rules.

Currently, two official plugins are available:

- [@vitejs/plugin-react](https://github.com/vitejs/vite-plugin-react/blob/main/packages/plugin-react/README.md) uses [Babel](https://babeljs.io/) for Fast Refresh
- [@vitejs/plugin-react-swc](https://github.com/vitejs/vite-plugin-react-swc) uses [SWC](https://swc.rs/) for Fast Refresh

## Expanding the ESLint configuration

If you are developing a production application, we recommend updating the configuration to enable type-aware lint rules:

- Configure the top-level `parserOptions` property like this:

```js
export default tseslint.config({
  languageOptions: {
    // other options...
    parserOptions: {
      project: ['./tsconfig.node.json', './tsconfig.app.json'],
      tsconfigRootDir: import.meta.dirname,
    },
  },
})
```

- Replace `tseslint.configs.recommended` with `tseslint.configs.recommendedTypeChecked` or `tseslint.configs.strictTypeChecked`.
- Optionally add `...tseslint.configs.stylisticTypeChecked`.
- Install [eslint-plugin-react](https://github.com/jsx-eslint/eslint-plugin-react) and update the config:

```js
// eslint.config.js
import react from 'eslint-plugin-react'

export default tseslint.config({
  // Set the react version
  settings: { react: { version: '18.3' } },
  plugins: {
    // Add the react plugin
    react,
  },
  rules: {
    // other rules...
    // Enable its recommended rules
    ...react.configs.recommended.rules,
    ...react.configs['jsx-runtime'].rules,
  },
})
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