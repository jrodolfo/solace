import {useState} from "react";
import axios, {AxiosHeaders, AxiosResponse, InternalAxiosRequestConfig} from "axios";
import "bootstrap/dist/css/bootstrap.min.css";
import ShowOutput from "./ShowOutput.tsx";
import type {SolaceBrokerAPIError} from "./SolaceBrokerAPIError.ts";
import type {MessagePayloadValidationErrorMap} from "./SolaceBrokerAPIError.ts";
import type {SolaceBrokerAPIResponse} from "./SolaceBrokerAPIResponse.ts";
import type {PagedStoredMessagesResponse} from "./StoredMessageTypes.ts";

type MessagePropertyFormRow = {
    key: string;
    value: string;
};

function App() {
    const apiUrl = "http://localhost:8081/api/v1/messages/message";
    const messagesApiUrl = "http://localhost:8081/api/v1/messages/all";

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
    const [filterDestination, setFilterDestination] = useState("");
    const [filterDeliveryMode, setFilterDeliveryMode] = useState("");
    const [filterInnerMessageId, setFilterInnerMessageId] = useState("");
    const [browserSortBy, setBrowserSortBy] = useState("createdAt");
    const [browserSortDirection, setBrowserSortDirection] = useState("desc");
    const [browserPage, setBrowserPage] = useState("0");
    const [browserSize, setBrowserSize] = useState("20");
    const [messagesResponse, setMessagesResponse] = useState<PagedStoredMessagesResponse | null>(null);
    const [browserMessage, setBrowserMessage] = useState<string | null>(null);
    const [browserVariant, setBrowserVariant] = useState<"success" | "danger" | "info" | null>(null);
    const [browserStatusCode, setBrowserStatusCode] = useState<number | null>(null);
    const [isLoadingMessages, setIsLoadingMessages] = useState(false);

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

    const fetchMessages = async (overrides?: { page?: number; size?: number }) => {
        const nextPage = overrides?.page ?? Number(browserPage);
        const nextSize = overrides?.size ?? Number(browserSize);

        if (!Number.isInteger(nextPage) || nextPage < 0) {
            setBrowserMessage("Page must be greater than or equal to 0.");
            setBrowserVariant("danger");
            setBrowserStatusCode(400);
            return;
        }

        if (!Number.isInteger(nextSize) || nextSize < 1 || nextSize > 100) {
            setBrowserMessage("Size must be between 1 and 100.");
            setBrowserVariant("danger");
            setBrowserStatusCode(400);
            return;
        }

        setIsLoadingMessages(true);
        setBrowserMessage(null);
        setBrowserVariant(null);
        setBrowserStatusCode(null);

        try {
            const response = await axios.get<PagedStoredMessagesResponse>(messagesApiUrl, {
                params: {
                    page: nextPage,
                    size: nextSize,
                    ...(filterDestination.trim() ? {destination: filterDestination.trim()} : {}),
                    ...(filterDeliveryMode.trim() ? {deliveryMode: filterDeliveryMode.trim()} : {}),
                    ...(filterInnerMessageId.trim() ? {innerMessageId: filterInnerMessageId.trim()} : {}),
                    sortBy: browserSortBy,
                    sortDirection: browserSortDirection
                }
            });

            setMessagesResponse(response.data);
            setBrowserPage(String(response.data.page));
            setBrowserSize(String(response.data.size));
            setBrowserMessage(`Loaded ${response.data.items.length} messages.`);
            setBrowserVariant("info");
            setBrowserStatusCode(response.status);
        } catch (error) {
            if (axios.isAxiosError<SolaceBrokerAPIError>(error) && error.response) {
                setBrowserMessage(error.response.data?.message ?? "Failed to load stored messages.");
                setBrowserVariant("danger");
                setBrowserStatusCode(error.response.status);
                return;
            }

            console.error("Failed to load stored messages.", error);
            setBrowserMessage("Failed to load stored messages.");
            setBrowserVariant("danger");
            setBrowserStatusCode(500);
        } finally {
            setIsLoadingMessages(false);
        }
    };

    const handleBrowseMessages = async (e: React.FormEvent<HTMLFormElement>) => {
        e.preventDefault();
        await fetchMessages();
    };

    const loadPreviousPage = async () => {
        if (!messagesResponse || messagesResponse.first) {
            return;
        }

        await fetchMessages({page: messagesResponse.page - 1});
    };

    const loadNextPage = async () => {
        if (!messagesResponse || messagesResponse.last) {
            return;
        }

        await fetchMessages({page: messagesResponse.page + 1});
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

            <section className="mt-5">
                <h2 className="mb-4 align-content-lg-center">Stored Messages</h2>

                {browserMessage && browserVariant && (
                    <div className={`alert alert-${browserVariant}`} role="alert">
                        {browserMessage}
                        {browserStatusCode !== null && ` (status: ${browserStatusCode})`}
                    </div>
                )}

                <form onSubmit={handleBrowseMessages}>
                    <div className="row g-3">
                        <div className="col-md-4">
                            <label htmlFor="filterDestination" className="form-label">
                                Filter Destination
                            </label>
                            <input
                                id="filterDestination"
                                type="text"
                                className="form-control"
                                value={filterDestination}
                                onChange={(e) => setFilterDestination(e.target.value)}
                                placeholder="Filter by destination"
                            />
                        </div>
                        <div className="col-md-4">
                            <label htmlFor="filterDeliveryMode" className="form-label">
                                Filter Delivery Mode
                            </label>
                            <input
                                id="filterDeliveryMode"
                                type="text"
                                className="form-control"
                                value={filterDeliveryMode}
                                onChange={(e) => setFilterDeliveryMode(e.target.value)}
                                placeholder="Filter by delivery mode"
                            />
                        </div>
                        <div className="col-md-4">
                            <label htmlFor="filterInnerMessageId" className="form-label">
                                Filter Inner Message Id
                            </label>
                            <input
                                id="filterInnerMessageId"
                                type="text"
                                className="form-control"
                                value={filterInnerMessageId}
                                onChange={(e) => setFilterInnerMessageId(e.target.value)}
                                placeholder="Filter by inner message id"
                            />
                        </div>
                        <div className="col-md-3">
                            <label htmlFor="browserSortBy" className="form-label">
                                Sort By
                            </label>
                            <select
                                id="browserSortBy"
                                className="form-select"
                                value={browserSortBy}
                                onChange={(e) => setBrowserSortBy(e.target.value)}
                            >
                                <option value="createdAt">createdAt</option>
                                <option value="priority">priority</option>
                                <option value="destination">destination</option>
                                <option value="innerMessageId">innerMessageId</option>
                            </select>
                        </div>
                        <div className="col-md-3">
                            <label htmlFor="browserSortDirection" className="form-label">
                                Sort Direction
                            </label>
                            <select
                                id="browserSortDirection"
                                className="form-select"
                                value={browserSortDirection}
                                onChange={(e) => setBrowserSortDirection(e.target.value)}
                            >
                                <option value="desc">desc</option>
                                <option value="asc">asc</option>
                            </select>
                        </div>
                        <div className="col-md-3">
                            <label htmlFor="browserPage" className="form-label">
                                Page
                            </label>
                            <input
                                id="browserPage"
                                type="number"
                                min="0"
                                className="form-control"
                                value={browserPage}
                                onChange={(e) => setBrowserPage(e.target.value)}
                            />
                        </div>
                        <div className="col-md-3">
                            <label htmlFor="browserSize" className="form-label">
                                Size
                            </label>
                            <input
                                id="browserSize"
                                type="number"
                                min="1"
                                max="100"
                                className="form-control"
                                value={browserSize}
                                onChange={(e) => setBrowserSize(e.target.value)}
                            />
                        </div>
                    </div>

                    <div className="d-flex gap-2 mt-3">
                        <button type="submit" className="btn btn-secondary" disabled={isLoadingMessages}>
                            {isLoadingMessages ? "Loading..." : "Load Messages"}
                        </button>
                        <button
                            type="button"
                            className="btn btn-outline-secondary"
                            onClick={loadPreviousPage}
                            disabled={!messagesResponse || messagesResponse.first || isLoadingMessages}
                        >
                            Previous Page
                        </button>
                        <button
                            type="button"
                            className="btn btn-outline-secondary"
                            onClick={loadNextPage}
                            disabled={!messagesResponse || messagesResponse.last || isLoadingMessages}
                        >
                            Next Page
                        </button>
                    </div>
                </form>

                {messagesResponse && (
                    <div className="mt-4">
                        <div className="card card-body mb-3">
                            <strong>
                                Page {messagesResponse.page + 1} of {messagesResponse.totalPages || 1}
                            </strong>
                            <span>
                                {messagesResponse.totalElements} stored messages total, page size {messagesResponse.size}
                            </span>
                        </div>

                        {messagesResponse.items.length === 0 ? (
                            <div className="card card-body">No stored messages found.</div>
                        ) : (
                            <div className="row g-3">
                                {messagesResponse.items.map((message) => (
                                    <div className="col-12" key={`${message.id ?? "message"}-${message.innerMessageId}`}>
                                        <div className="card card-body">
                                            <div className="d-flex justify-content-between flex-wrap gap-2">
                                                <h5 className="mb-0">{message.innerMessageId}</h5>
                                                <span className="badge text-bg-secondary">{message.deliveryMode}</span>
                                            </div>
                                            <p className="mb-1"><strong>Destination:</strong> {message.destination}</p>
                                            <p className="mb-1"><strong>Priority:</strong> {message.priority}</p>
                                            <p className="mb-1"><strong>Payload Type:</strong> {message.payload?.type}</p>
                                            <p className="mb-3"><strong>Payload Content:</strong> {message.payload?.content}</p>
                                            <div>
                                                <strong>Properties:</strong>
                                                {message.properties.length === 0 ? (
                                                    <span> none</span>
                                                ) : (
                                                    <ul className="mb-0 mt-2">
                                                        {message.properties.map((property) => (
                                                            <li key={`${property.id ?? property.propertyKey}-${property.propertyValue}`}>
                                                                {property.propertyKey}: {property.propertyValue}
                                                            </li>
                                                        ))}
                                                    </ul>
                                                )}
                                            </div>
                                        </div>
                                    </div>
                                ))}
                            </div>
                        )}
                    </div>
                )}
            </section>

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
