# Solace Cloud Account, Demo, and Environment Variables

This guide shows how to create a Solace Cloud account, create a developer event
broker service, run a simple publisher/subscriber demo, collect the four values
required by this project, and register those values on Windows, Linux, and
macOS.

The four required values are:

- `SOLACE_CLOUD_HOST`
- `SOLACE_CLOUD_VPN`
- `SOLACE_CLOUD_USERNAME`
- `SOLACE_CLOUD_PASSWORD`

Use your own Solace Cloud values. The screenshots below are examples only.

## 1. Create a Solace Cloud Account

Open the Solace Cloud console login page:

```text
https://console.solace.cloud/login
```

If you do not already have an account, select **Sign Up**.

![Solace Cloud login page with Sign Up link highlighted](../images/solace-cloud/01.png)

Fill out the trial account form.

![Solace Cloud sign-up form](../images/solace-cloud/02.png)

After filling out the required fields, accept the terms and select **Sign Up**.

![Completed Solace Cloud sign-up form with Sign Up button highlighted](../images/solace-cloud/03.png)

Solace Cloud confirms that an activation email was sent.

![Solace Cloud activation email confirmation page](../images/solace-cloud/04.png)

Open the activation email and select **Activate**.

![Solace Cloud activation email with Activate button highlighted](../images/solace-cloud/05.png)

After activation, sign in with the account credentials you created.

![Solace Cloud sign-in page after account activation](../images/solace-cloud/06.png)

## 2. Create an Event Broker Service

After signing in, Solace Cloud opens the event broker service creation flow. If
a welcome modal appears, close it.

![Solace Cloud welcome modal on Create Service page](../images/solace-cloud/07.png)

Open **Cluster Manager** from the left navigation if you are not already on the
Create Service page.

![Cluster Manager selected on the Create Service page](../images/solace-cloud/08.png)

Enter a service name, choose a cloud provider, choose a region, keep the
developer service type selected, and then select **Create Service**.

The screenshots use:

- service name: `my-solace-service`
- cloud provider: `Amazon Web Services`
- region: `eks-us-east-1a`
- service type: `Developer`

![Create Service form with service name, cloud, region, and Create Service highlighted](../images/solace-cloud/09.png)

Wait for the event broker service to be created.

![Solace Cloud service creation progress screen](../images/solace-cloud/10.png)

When the service is running, open the **Try Me!** tab.

![Running Solace Cloud service with Try Me tab highlighted](../images/solace-cloud/11.png)

## 3. Get the Four Required Values

In the **Try Me!** tab, Solace Cloud shows a publisher panel and a subscriber
panel. Before opening Broker Manager, copy the generated client username and
client password from the page.

Then select **Open Broker Manager**.

![Try Me page with client username, client password, and Open Broker Manager highlighted](../images/solace-cloud/12.png)

Broker Manager opens the **Send and Receive** page. Select **Connect** in the
publisher panel.

![Broker Manager Send and Receive page with publisher Connect button highlighted](../images/solace-cloud/13.png)

Expand or review the publisher connection settings and collect the remaining
values.

Map the Solace Cloud labels to this project's environment variables:

| Solace Cloud label | Environment variable |
| --- | --- |
| Broker URL | `SOLACE_CLOUD_HOST` |
| Message VPN | `SOLACE_CLOUD_VPN` |
| Client Username | `SOLACE_CLOUD_USERNAME` |
| Client Password | `SOLACE_CLOUD_PASSWORD` |

![Publisher connection fields mapped to the required environment variables](../images/solace-cloud/14.png)

Enter the client password you copied earlier and select **Connect**.

![Publisher panel with password entered and Connect button highlighted](../images/solace-cloud/15.png)

After the publisher connects, select **Connect** in the subscriber panel.

![Publisher connected and subscriber Connect button highlighted](../images/solace-cloud/16.png)

The subscriber panel should also show a connected state.

![Publisher and subscriber panels both connected](../images/solace-cloud/17.png)

## 4. Test Publisher and Subscriber in Broker Manager

Use the publisher panel to publish a sample direct message.

At first, the subscriber may not receive previously published messages if it was
not already subscribed to the topic.

![Publisher message area with Publish button highlighted](../images/solace-cloud/18.png)

Subscribe to the same topic pattern in the subscriber panel.

![Subscriber topic subscription field and Subscribe button highlighted](../images/solace-cloud/19.png)

After subscribing, publish the message again.

![Subscribed topic shown and Publish button highlighted again](../images/solace-cloud/20.png)

The subscriber panel should show the received direct message.

![Subscriber panel showing the received published message](../images/solace-cloud/21.png)

## 5. Save the Four Values

Before registering the values in your operating system, save them in a temporary
private note so you can copy them accurately.

Do not commit this note to the repository.

![Temporary local note containing the four Solace Cloud values](../images/solace-cloud/22.png)

## 6. Register the Variables on Windows

On Windows, you can add the values through the Environment Variables dialog.
Create these as user variables or system variables, depending on how you run the
backend and subscriber.

![Windows Environment Variables dialog with the four Solace variables highlighted](../images/solace-cloud/23.png)

You can also register user-level variables from PowerShell:

```powershell
[Environment]::SetEnvironmentVariable("SOLACE_CLOUD_HOST", "wss://your-broker-host.messaging.solace.cloud:443", "User")
[Environment]::SetEnvironmentVariable("SOLACE_CLOUD_VPN", "your-message-vpn", "User")
[Environment]::SetEnvironmentVariable("SOLACE_CLOUD_USERNAME", "your-client-username", "User")
[Environment]::SetEnvironmentVariable("SOLACE_CLOUD_PASSWORD", "your-client-password", "User")
```

Close and reopen your terminal after setting the variables.

To verify them in a new PowerShell session:

```powershell
$env:SOLACE_CLOUD_HOST
$env:SOLACE_CLOUD_VPN
$env:SOLACE_CLOUD_USERNAME
$env:SOLACE_CLOUD_PASSWORD
```

## 7. Register the Variables on Linux or macOS

For the current terminal session, export the values:

```bash
export SOLACE_CLOUD_HOST="wss://your-broker-host.messaging.solace.cloud:443"
export SOLACE_CLOUD_VPN="your-message-vpn"
export SOLACE_CLOUD_USERNAME="your-client-username"
export SOLACE_CLOUD_PASSWORD="your-client-password"
```

For a persistent shell setup, add the same exports to your shell startup file.
Common choices are:

- `~/.zshrc` for zsh
- `~/.bashrc` for bash on Linux
- `~/.bash_profile` for older bash setups on macOS

If you prefer to keep secrets separate from your main shell file, create a file
such as `~/.zsh_secrets`, add the exports there, and source it from `~/.zshrc`.

![Shell secret file containing the four Solace Cloud exports](../images/solace-cloud/24.png)

Example `~/.zshrc` entry:

```bash
source ~/.zsh_secrets
```

Reload your shell configuration:

```bash
source ~/.zshrc
```

To verify the values:

```bash
printf '%s\n' "$SOLACE_CLOUD_HOST"
printf '%s\n' "$SOLACE_CLOUD_VPN"
printf '%s\n' "$SOLACE_CLOUD_USERNAME"
printf '%s\n' "$SOLACE_CLOUD_PASSWORD"
```

## 8. Run This Project with the Solace Cloud Values

After the four variables are registered, start the Docker runtime from the
repository root:

```bash
./scripts/docker-start.sh
```

Useful runtime commands:

```bash
./scripts/docker-status.sh
./scripts/docker-logs.sh subscriber
./scripts/docker-stop.sh
```

If you need module-level debugging, the local development commands are
documented in the root `README.md` and `scripts/README.md`.

Use the UI at `http://localhost:5173` to publish a message. The subscriber
should log matching topic traffic, and the backend should persist the publish
attempt for browsing, retry, export, and reconciliation workflows.

## Notes

- Keep `SOLACE_CLOUD_PASSWORD` private.
- Do not commit local secret files or screenshots that expose real credentials.
- If you rotate the Solace Cloud client password, update the operating system
  environment variable before restarting the backend or subscriber.
