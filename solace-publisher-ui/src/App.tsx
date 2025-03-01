import {useState} from "react";
import axios, {AxiosRequestHeaders, AxiosResponse} from "axios";
import "bootstrap/dist/css/bootstrap.min.css";
import ShowOutput from "./ShowOutput.tsx";

function App() {

    // State hooks for input fields
    const [userName, setUserName] = useState("");
    const [password, setPassword] = useState("");
    const [vpnName, setVpnName] = useState("");
    const [host, setHost] = useState("");
    const [message, setMessage] = useState("");
    const [response, setResponse] = useState<AxiosResponse<any, any> | null>(null);
    const [showResponse, setShowResponse] = useState(false);

    // Submit handler
    const handleSubmit = async (e: React.FormEvent<HTMLFormElement>) => {
        e.preventDefault(); // Stop browser refresh
        console.log("Submitting form...");
        try {
            // Send POST request using Axios
            const response = await axios.post(
                "http://localhost:8081/api/v1/messages/message",
                {
                    userName,   // Sent as a plain string
                    password,   // Sent as a plain string
                    host,       // Sent as a plain string
                    vpnName,    // Sent as a plain string
                    message: JSON.parse(message) // preserve the json object structure of the message
                },
                {
                    headers: {
                        "Content-Type": "application/json", // Explicitly set Content-Type
                    },
                }
            );
            console.log("Response:", response);
            alert("Message Published Successfully!");
            setShowResponse(true);
            setResponse(response);
        } catch (error) {
            const errorMessage = "Failed to publish the message. See console for details.";
            console.error(errorMessage, error);
            alert(errorMessage);
            const DEFAULT_EMPTY_HEADERS = {} as AxiosRequestHeaders;
            const DEFAULT_ERROR_MESSAGE = errorMessage;
            setShowResponse(true);
            setResponse({
                data: DEFAULT_ERROR_MESSAGE,
                status: 500,
                statusText: "Internal Server Error",
                headers: DEFAULT_EMPTY_HEADERS,
                config: {
                    headers: DEFAULT_EMPTY_HEADERS
                },
            });
        }
    };

    return (
        <div className="container-fluid p-5">

            <meta name="viewport" content="width=device-width, initial-scale=1"/>

            <h2 className="mb-4 align-content-lg-center">Solace Publisher</h2>

            <form onSubmit={handleSubmit}>

                {/* UserName Field */}
                <div className="col-lg-12">
                    <label htmlFor="userName" className="form-label">
                        User Name
                    </label>
                    <input
                        id="userName"
                        type="text"
                        className="form-control w-100 mt-1 mb-4"
                        value={userName}
                        onChange={(e) => setUserName(e.target.value)}
                        placeholder="Enter the user name"
                        required
                    />
                </div>

                {/* Password Field */}
                <div className="col-lg-12">
                    <label htmlFor="password" className="form-label">
                        Password
                    </label>
                    <input
                        id="password"
                        type="password"
                        className="form-control w-100 mt-1 mb-4"
                        value={password}
                        onChange={(e) => setPassword(e.target.value)}
                        placeholder="Enter the password"
                        required
                    />
                </div>

                {/* Host Field */}
                <div className="col-lg-12">
                    <label htmlFor="host" className="form-label">
                        Host
                    </label>
                    <input
                        id="host"
                        type="text"
                        className="form-control w-100 mt-1 mb-4"
                        value={host}
                        onChange={(e) => setHost(e.target.value)}
                        placeholder="Enter the host url and port"
                        required
                    />
                </div>

                {/* VPN Name Field */}
                <div className="col-lg-12">
                    <label htmlFor="vpnName" className="form-label">
                        Event VPN Name
                    </label>
                    <input
                        id="vpnName"
                        type="text"
                        className="form-control w-100 mt-1 mb-4"
                        value={vpnName}
                        onChange={(e) => setVpnName(e.target.value)}
                        placeholder="Enter the VPN name"
                        required
                    />
                </div>

                {/* Message Field */}
                <div className="col-lg-12">
                    <label htmlFor="message" className="form-label">
                        Message
                    </label>
                    <textarea
                        id="message"
                        className="form-control w-100 mt-1 mb-4"
                        value={message}
                        onChange={(e) => setMessage(e.target.value)}
                        rows={20}
                        placeholder="Enter the message content"
                        required
                    ></textarea>
                </div>

                {/* Submit Button */}
                <div className="text-center">
                    <button type="submit" className="btn btn-primary">
                        Publish Message
                    </button>
                </div>
            </form>

            <div className="col-lg-12 mt-6 mb-6">
                {showResponse && <ShowOutput res={response}></ShowOutput>}
            </div>

        </div>
    );
}

export default App;
