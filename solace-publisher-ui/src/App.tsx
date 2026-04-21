import {useState} from "react";
import axios, {AxiosHeaders, AxiosResponse, InternalAxiosRequestConfig} from "axios";
import "bootstrap/dist/css/bootstrap.min.css";
import ShowOutput from "./ShowOutput.tsx";
import type {SolaceBrokerAPIError} from "./SolaceBrokerAPIError.ts";
import type {MessagePayloadValidationErrorMap} from "./SolaceBrokerAPIError.ts";
import type {SolaceBrokerAPIResponse} from "./SolaceBrokerAPIResponse.ts";

type MessagePropertyFormRow = {
    key: string;
    value: string;
};

function App() {
    const apiUrl = "http://localhost:8081/api/v1/messages/message";

    // State hooks for input fields
    const [userName, setUserName] = useState("");
    const [password, setPassword] = useState("");
    const [vpnName, setVpnName] = useState("");
    const [host, setHost] = useState("");
    const [innerMessageId, setInnerMessageId] = useState("");
    const [destination, setDestination] = useState("");
    const [deliveryMode, setDeliveryMode] = useState("");
    const [priority, setPriority] = useState("0");
    const [payloadType, setPayloadType] = useState("");
    const [payloadContent, setPayloadContent] = useState("");
    const [properties, setProperties] = useState<MessagePropertyFormRow[]>([{key: "", value: ""}]);
    const [response, setResponse] = useState<AxiosResponse<SolaceBrokerAPIResponse | SolaceBrokerAPIError> | null>(null);
    const [showResponse, setShowResponse] = useState(false);
    const [submissionMessage, setSubmissionMessage] = useState<string | null>(null);
    const [submissionVariant, setSubmissionVariant] = useState<"success" | "danger" | null>(null);

    const updateProperty = (index: number, field: keyof MessagePropertyFormRow, value: string) => {
        setProperties((currentProperties) =>
            currentProperties.map((property, propertyIndex) =>
                propertyIndex === index ? {...property, [field]: value} : property
            )
        );
    };

    const addPropertyRow = () => {
        setProperties((currentProperties) => [...currentProperties, {key: "", value: ""}]);
    };

    const removePropertyRow = (index: number) => {
        setProperties((currentProperties) => {
            if (currentProperties.length === 1) {
                return [{key: "", value: ""}];
            }

            return currentProperties.filter((_, propertyIndex) => propertyIndex !== index);
        });
    };

    // Submit handler
    const handleSubmit = async (e: React.FormEvent<HTMLFormElement>) => {
        e.preventDefault(); // Stop browser refresh
        setShowResponse(false);
        setSubmissionMessage(null);
        setSubmissionVariant(null);

        const parsedPriority = Number(priority);
        const trimmedProperties = properties
            .map((property) => ({
                key: property.key.trim(),
                value: property.value.trim()
            }))
            .filter((property) => property.key.length > 0 || property.value.length > 0);
        const messagePayload = {
            innerMessageId,
            destination,
            deliveryMode,
            priority: parsedPriority,
            payload: {
                type: payloadType,
                content: payloadContent
            },
            ...(trimmedProperties.length > 0 ? {properties: trimmedProperties} : {})
        };

        const validationErrors = validateMessagePayload(messagePayload);
        if (Object.keys(validationErrors).length > 0) {
            const validationErrorResponse = buildSyntheticErrorResponse(
                400,
                "Bad Request",
                {
                    status: 400,
                    error: "Bad Request",
                    message: "Request validation failed",
                    path: "/api/v1/messages/message",
                    validationErrors
                }
            );
            setResponse(validationErrorResponse);
            setShowResponse(true);
            setSubmissionVariant("danger");
            setSubmissionMessage("Request validation failed");
            return;
        }

        try {
            const response = await axios.post(
                apiUrl,
                {
                    userName,
                    password,
                    host,
                    vpnName,
                    message: messagePayload
                },
                {
                    headers: {
                        "Content-Type": "application/json",
                    },
                }
            );
            setShowResponse(true);
            setResponse(response);
            setSubmissionVariant("success");
            setSubmissionMessage("Message published successfully.");
        } catch (error) {
            if (axios.isAxiosError<SolaceBrokerAPIError>(error) && error.response) {
                const backendMessage = error.response.data?.message ?? "Failed to publish the message.";
                setResponse(error.response);
                setSubmissionVariant("danger");
                setSubmissionMessage(backendMessage);
                setShowResponse(true);
                return;
            }

            const fallbackMessage = "Failed to publish the message.";
            console.error(fallbackMessage, error);
            const networkErrorResponse = buildSyntheticErrorResponse(
                500,
                "Internal Server Error",
                {
                    status: 500,
                    error: "Internal Server Error",
                    message: fallbackMessage,
                    path: "/api/v1/messages/message",
                    validationErrors: null
                }
            );
            setResponse(networkErrorResponse);
            setShowResponse(true);
            setSubmissionVariant("danger");
            setSubmissionMessage(fallbackMessage);
        }
    };

    return (
        <div className="container-fluid p-5">

            <meta name="viewport" content="width=device-width, initial-scale=1"/>

            <h2 className="mb-4 align-content-lg-center">Solace Publisher</h2>

            {submissionMessage && submissionVariant && (
                <div className={`alert alert-${submissionVariant}`} role="alert">
                    {submissionMessage}
                </div>
            )}

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

                {/* Message Fields */}
                <div className="col-lg-12">
                    <label htmlFor="innerMessageId" className="form-label">
                        Inner Message Id
                    </label>
                    <input
                        id="innerMessageId"
                        type="text"
                        className="form-control w-100 mt-1 mb-4"
                        value={innerMessageId}
                        onChange={(e) => setInnerMessageId(e.target.value)}
                        placeholder="Enter the inner message id"
                        required
                    />
                </div>

                <div className="col-lg-12">
                    <label htmlFor="destination" className="form-label">
                        Destination
                    </label>
                    <input
                        id="destination"
                        type="text"
                        className="form-control w-100 mt-1 mb-4"
                        value={destination}
                        onChange={(e) => setDestination(e.target.value)}
                        placeholder="Enter the destination topic"
                        required
                    />
                </div>

                <div className="col-lg-12">
                    <label htmlFor="deliveryMode" className="form-label">
                        Delivery Mode
                    </label>
                    <input
                        id="deliveryMode"
                        type="text"
                        className="form-control w-100 mt-1 mb-4"
                        value={deliveryMode}
                        onChange={(e) => setDeliveryMode(e.target.value)}
                        placeholder="Enter the delivery mode"
                        required
                    />
                </div>

                <div className="col-lg-12">
                    <label htmlFor="priority" className="form-label">
                        Priority
                    </label>
                    <input
                        id="priority"
                        type="number"
                        min="0"
                        className="form-control w-100 mt-1 mb-4"
                        value={priority}
                        onChange={(e) => setPriority(e.target.value)}
                        placeholder="Enter the priority"
                        required
                    />
                </div>

                <div className="col-lg-12">
                    <label htmlFor="payloadType" className="form-label">
                        Payload Type
                    </label>
                    <input
                        id="payloadType"
                        type="text"
                        className="form-control w-100 mt-1 mb-4"
                        value={payloadType}
                        onChange={(e) => setPayloadType(e.target.value)}
                        placeholder="Enter the payload type"
                        required
                    />
                </div>

                <div className="col-lg-12">
                    <label htmlFor="payloadContent" className="form-label">
                        Payload Content
                    </label>
                    <textarea
                        id="payloadContent"
                        className="form-control w-100 mt-1 mb-4"
                        value={payloadContent}
                        onChange={(e) => setPayloadContent(e.target.value)}
                        rows={8}
                        placeholder="Enter the payload content"
                        required
                    ></textarea>
                </div>

                <div className="col-lg-12">
                    <label className="form-label">Message Properties</label>
                    {properties.map((property, index) => (
                        <div className="row g-2 align-items-end mt-1 mb-3" key={`property-row-${index}`}>
                            <div className="col-md-5">
                                <label htmlFor={`propertyKey-${index}`} className="form-label">
                                    Property Key {index + 1}
                                </label>
                                <input
                                    id={`propertyKey-${index}`}
                                    type="text"
                                    className="form-control"
                                    value={property.key}
                                    onChange={(e) => updateProperty(index, "key", e.target.value)}
                                    placeholder="Enter property key"
                                />
                            </div>
                            <div className="col-md-5">
                                <label htmlFor={`propertyValue-${index}`} className="form-label">
                                    Property Value {index + 1}
                                </label>
                                <input
                                    id={`propertyValue-${index}`}
                                    type="text"
                                    className="form-control"
                                    value={property.value}
                                    onChange={(e) => updateProperty(index, "value", e.target.value)}
                                    placeholder="Enter property value"
                                />
                            </div>
                            <div className="col-md-2">
                                <button
                                    type="button"
                                    className="btn btn-outline-secondary w-100"
                                    onClick={() => removePropertyRow(index)}
                                >
                                    Remove
                                </button>
                            </div>
                        </div>
                    ))}
                    <button
                        type="button"
                        className="btn btn-outline-primary mb-4"
                        onClick={addPropertyRow}
                    >
                        Add Property
                    </button>
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
function buildSyntheticErrorResponse(
    status: number,
    statusText: string,
    data: SolaceBrokerAPIError
): AxiosResponse<SolaceBrokerAPIError> {
    return {
        data,
        status,
        statusText,
        headers: new AxiosHeaders(),
        config: {
            headers: new AxiosHeaders()
        } as InternalAxiosRequestConfig
    };
}

function validateMessagePayload(payload: unknown): MessagePayloadValidationErrorMap {
    const validationErrors: MessagePayloadValidationErrorMap = {};

    if (!isRecord(payload)) {
        validationErrors["message"] = "message must be a JSON object";
        return validationErrors;
    }

    if (!hasNonBlankString(payload.innerMessageId)) {
        validationErrors["message.innerMessageId"] = "message.innerMessageId is required";
    }
    if (!hasNonBlankString(payload.destination)) {
        validationErrors["message.destination"] = "message.destination is required";
    }
    if (!hasNonBlankString(payload.deliveryMode)) {
        validationErrors["message.deliveryMode"] = "message.deliveryMode is required";
    }
    if (!isNonNegativeNumber(payload.priority)) {
        validationErrors["message.priority"] = "message.priority is required";
    }

    if (!isRecord(payload.payload)) {
        validationErrors["message.payload"] = "message.payload is required";
        return validationErrors;
    }

    if (!hasNonBlankString(payload.payload.type)) {
        validationErrors["message.payload.type"] = "payload.type is required";
    }
    if (!hasNonBlankString(payload.payload.content)) {
        validationErrors["message.payload.content"] = "payload.content is required";
    }

    if (payload.properties !== undefined) {
        if (!Array.isArray(payload.properties)) {
            validationErrors["message.properties"] = "message.properties must be an array";
            return validationErrors;
        }

        payload.properties.forEach((property, index) => {
            if (!isRecord(property)) {
                validationErrors[`message.properties[${index}]`] = `message.properties[${index}] must be an object`;
                return;
            }

            const hasKey = hasNonBlankString(property.key);
            const hasValue = hasNonBlankString(property.value);

            if (hasKey !== hasValue) {
                if (!hasKey) {
                    validationErrors[`message.properties[${index}].key`] = `message.properties[${index}].key is required`;
                }
                if (!hasValue) {
                    validationErrors[`message.properties[${index}].value`] = `message.properties[${index}].value is required`;
                }
            }
        });
    }

    return validationErrors;
}

function isRecord(value: unknown): value is Record<string, unknown> {
    return typeof value === "object" && value !== null && !Array.isArray(value);
}

function hasNonBlankString(value: unknown): value is string {
    return typeof value === "string" && value.trim().length > 0;
}

function isNonNegativeNumber(value: unknown): value is number {
    return typeof value === "number" && Number.isFinite(value) && value >= 0;
}

export default App;
