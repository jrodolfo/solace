import {useState} from "react";
import axios, {AxiosHeaders, AxiosResponse, InternalAxiosRequestConfig} from "axios";
import "bootstrap/dist/css/bootstrap.min.css";
import "./App.css";
import ShowOutput from "./ShowOutput.tsx";
import type {SolaceBrokerAPIError} from "./SolaceBrokerAPIError.ts";
import type {MessagePayloadValidationErrorMap} from "./SolaceBrokerAPIError.ts";
import type {SolaceBrokerAPIResponse} from "./SolaceBrokerAPIResponse.ts";
import type {DeliveryMode, PagedStoredMessagesResponse, PayloadType} from "./StoredMessageTypes.ts";

/**
 * Represents a row in the message properties form.
 */
type MessagePropertyFormRow = {
    /** The property key. */
    key: string;
    /** The property value. */
    value: string;
};

const DEFAULT_BROWSER_SORT_BY = "createdAt";
const DEFAULT_BROWSER_SORT_DIRECTION = "desc";
const DEFAULT_BROWSER_PAGE = "0";
const DEFAULT_BROWSER_SIZE = "20";
const DEFAULT_DELIVERY_MODE = "PERSISTENT";
const DEFAULT_PAYLOAD_TYPE = "";
const DEFAULT_BROWSER_PUBLISH_STATUS = "";
const DEFAULT_BROWSER_DELIVERY_MODE = "";
const DEFAULT_BROWSER_PAYLOAD_TYPE = "";
const DEFAULT_BROWSER_CREATED_AT_FROM = "";
const DEFAULT_BROWSER_CREATED_AT_TO = "";
const DEFAULT_BROWSER_PUBLISHED_AT_FROM = "";
const DEFAULT_BROWSER_PUBLISHED_AT_TO = "";
const DEFAULT_BROWSER_STALE_PENDING_ONLY = false;
const MAX_MESSAGE_PRIORITY = 255;
const DELIVERY_MODE_OPTIONS: DeliveryMode[] = ["DIRECT", "NON_PERSISTENT", "PERSISTENT"];
const PAYLOAD_TYPE_OPTIONS: PayloadType[] = ["TEXT", "BINARY", "JSON", "XML"];

const isAllowedDeliveryMode = (value: unknown): value is DeliveryMode =>
    typeof value === "string" && DELIVERY_MODE_OPTIONS.includes(value as DeliveryMode);

const isAllowedPayloadType = (value: unknown): value is PayloadType =>
    typeof value === "string" && PAYLOAD_TYPE_OPTIONS.includes(value as PayloadType);

/**
 * Query contract shared by the stored-message browser controls.
 */
type BrowserQueryState = {
    /** Page number to fetch. */
    page: number;
    /** Number of items per page. */
    size: number;
    /** Destination filter. */
    destination: string;
    /** Delivery mode filter. */
    deliveryMode: DeliveryMode | "";
    /** Payload type filter. */
    payloadType?: PayloadType | "";
    /** Inner message ID filter. */
    innerMessageId: string;
    /** Publish status filter. */
    publishStatus: string;
    /** Filter for stale pending messages only. */
    stalePendingOnly: boolean;
    /** Creation date from filter (ISO). */
    createdAtFrom: string;
    /** Creation date to filter (ISO). */
    createdAtTo: string;
    /** Published date from filter (ISO). */
    publishedAtFrom: string;
    /** Published date to filter (ISO). */
    publishedAtTo: string;
    /** Field to sort by. */
    sortBy: string;
    /** Sort direction ("asc" or "desc"). */
    sortDirection: string;
};

/**
 * Sections of the workspace.
 */
type WorkspaceSection = "PUBLISH" | "BROWSER";

const brokerApiBaseUrl = (import.meta.env.VITE_BROKER_API_BASE_URL ?? "http://localhost:8081").replace(/\/+$/, "");

/**
 * The main application component for the Solace Publisher UI.
 * Provides functionality for publishing messages and browsing stored messages.
 */
function App() {
    const apiUrl = `${brokerApiBaseUrl}/api/v1/messages/message`;
    const messagesBaseUrl = `${brokerApiBaseUrl}/api/v1/messages`;
    const messagesApiUrl = `${brokerApiBaseUrl}/api/v1/messages/all`;

    // Broker credentials are kept only in React state for the current publish request.
    const [userName, setUserName] = useState("");
    const [password, setPassword] = useState("");
    const [vpnName, setVpnName] = useState("");
    const [host, setHost] = useState("");
    const [innerMessageId, setInnerMessageId] = useState("");
    const [destination, setDestination] = useState("");
    const [deliveryMode, setDeliveryMode] = useState<DeliveryMode>(DEFAULT_DELIVERY_MODE);
    const [priority, setPriority] = useState("0");
    const [payloadType, setPayloadType] = useState<PayloadType | "">(DEFAULT_PAYLOAD_TYPE);
    const [payloadContent, setPayloadContent] = useState("");
    const [properties, setProperties] = useState<MessagePropertyFormRow[]>([{key: "", value: ""}]);
    const [response, setResponse] = useState<AxiosResponse<SolaceBrokerAPIResponse | SolaceBrokerAPIError> | null>(null);
    const [showResponse, setShowResponse] = useState(false);
    const [submissionMessage, setSubmissionMessage] = useState<string | null>(null);
    const [submissionVariant, setSubmissionVariant] = useState<"success" | "danger" | null>(null);
    const [filterDestination, setFilterDestination] = useState("");
    const [filterDeliveryMode, setFilterDeliveryMode] = useState<DeliveryMode | "">(DEFAULT_BROWSER_DELIVERY_MODE);
    const [filterPayloadType, setFilterPayloadType] = useState<PayloadType | "">(DEFAULT_BROWSER_PAYLOAD_TYPE);
    const [filterInnerMessageId, setFilterInnerMessageId] = useState("");
    const [filterPublishStatus, setFilterPublishStatus] = useState(DEFAULT_BROWSER_PUBLISH_STATUS);
    const [filterStalePendingOnly, setFilterStalePendingOnly] = useState(DEFAULT_BROWSER_STALE_PENDING_ONLY);
    const [filterCreatedAtFrom, setFilterCreatedAtFrom] = useState(DEFAULT_BROWSER_CREATED_AT_FROM);
    const [filterCreatedAtTo, setFilterCreatedAtTo] = useState(DEFAULT_BROWSER_CREATED_AT_TO);
    const [filterPublishedAtFrom, setFilterPublishedAtFrom] = useState(DEFAULT_BROWSER_PUBLISHED_AT_FROM);
    const [filterPublishedAtTo, setFilterPublishedAtTo] = useState(DEFAULT_BROWSER_PUBLISHED_AT_TO);
    const [browserSortBy, setBrowserSortBy] = useState(DEFAULT_BROWSER_SORT_BY);
    const [browserSortDirection, setBrowserSortDirection] = useState(DEFAULT_BROWSER_SORT_DIRECTION);
    const [browserPage, setBrowserPage] = useState(DEFAULT_BROWSER_PAGE);
    const [browserSize, setBrowserSize] = useState(DEFAULT_BROWSER_SIZE);
    const [messagesResponse, setMessagesResponse] = useState<PagedStoredMessagesResponse | null>(null);
    const [browserMessage, setBrowserMessage] = useState<string | null>(null);
    const [browserVariant, setBrowserVariant] = useState<"success" | "danger" | "info" | null>(null);
    const [browserStatusCode, setBrowserStatusCode] = useState<number | null>(null);
    const [isLoadingMessages, setIsLoadingMessages] = useState(false);
    const [retryingMessageId, setRetryingMessageId] = useState<string | null>(null);
    const [reconcilingMessageId, setReconcilingMessageId] = useState<string | null>(null);
    const [expandedMessageId, setExpandedMessageId] = useState<string | null>(null);
    const [hasLoadedMessages, setHasLoadedMessages] = useState(false);
    const [copyFeedback, setCopyFeedback] = useState<string | null>(null);
    const [activeWorkspaceSection, setActiveWorkspaceSection] = useState<WorkspaceSection>("PUBLISH");

    /**
     * Builds a complete browser query from current control state plus optional
     * overrides used by pagination and quick filters.
     */
    const currentBrowserQuery = (overrides?: Partial<BrowserQueryState>): BrowserQueryState => ({
        page: overrides?.page ?? Number(browserPage),
        size: overrides?.size ?? Number(browserSize),
        destination: overrides?.destination ?? filterDestination.trim(),
        deliveryMode: overrides?.deliveryMode ?? filterDeliveryMode,
        payloadType: overrides?.payloadType ?? filterPayloadType,
        innerMessageId: overrides?.innerMessageId ?? filterInnerMessageId.trim(),
        publishStatus: overrides?.publishStatus ?? filterPublishStatus,
        stalePendingOnly: overrides?.stalePendingOnly ?? filterStalePendingOnly,
        createdAtFrom: overrides?.createdAtFrom ?? filterCreatedAtFrom,
        createdAtTo: overrides?.createdAtTo ?? filterCreatedAtTo,
        publishedAtFrom: overrides?.publishedAtFrom ?? filterPublishedAtFrom,
        publishedAtTo: overrides?.publishedAtTo ?? filterPublishedAtTo,
        sortBy: overrides?.sortBy ?? browserSortBy,
        sortDirection: overrides?.sortDirection ?? browserSortDirection
    });

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

    /**
     * Fetches messages from the Solace Broker API based on current filters and pagination.
     * 
     * @param overrides - Optional partial query state to override current filters.
     * @returns A promise that resolves when the messages are fetched and state is updated.
     */
    const fetchMessages = async (overrides?: Partial<BrowserQueryState>) => {
        const query = currentBrowserQuery(overrides);
        const nextPage = query.page;
        const nextSize = query.size;

        if (!Number.isInteger(nextPage) || nextPage < 0) {
            setBrowserMessage("Page must be greater than or equal to 0.");
            setBrowserVariant("danger");
            setBrowserStatusCode(400);
            return;
        }

        if (!Number.isInteger(nextSize) || nextSize < 1 || nextSize > 100) {
            setBrowserMessage("Messages per page must be between 1 and 100.");
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
                    ...(query.destination ? {destination: query.destination} : {}),
                    ...(query.deliveryMode ? {deliveryMode: query.deliveryMode} : {}),
                    ...(query.payloadType ? {payloadType: query.payloadType} : {}),
                    ...(query.innerMessageId ? {innerMessageId: query.innerMessageId} : {}),
                    ...(query.publishStatus ? {publishStatus: query.publishStatus} : {}),
                    ...(query.stalePendingOnly ? {stalePendingOnly: true} : {}),
                    ...(query.createdAtFrom ? {createdAtFrom: toIsoLocalDateTime(query.createdAtFrom)} : {}),
                    ...(query.createdAtTo ? {createdAtTo: toIsoLocalDateTime(query.createdAtTo)} : {}),
                    ...(query.publishedAtFrom ? {publishedAtFrom: toIsoLocalDateTime(query.publishedAtFrom)} : {}),
                    ...(query.publishedAtTo ? {publishedAtTo: toIsoLocalDateTime(query.publishedAtTo)} : {}),
                    sortBy: query.sortBy,
                    sortDirection: query.sortDirection
                }
            });

            setMessagesResponse(response.data);
            setBrowserPage(String(response.data.page));
            setBrowserSize(String(response.data.size));
            setExpandedMessageId(null);
            setHasLoadedMessages(true);
            setBrowserMessage(formatLoadedMessages(response.data.items.length));
            setBrowserVariant("info");
            setBrowserStatusCode(response.status);
        } catch (error) {
            setHasLoadedMessages(true);
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
        setBrowserPage(DEFAULT_BROWSER_PAGE);
        await fetchMessages({page: Number(DEFAULT_BROWSER_PAGE)});
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

    const resetBrowserFilters = () => {
        setFilterDestination("");
        setFilterDeliveryMode("");
        setFilterPayloadType("");
        setFilterInnerMessageId("");
        setFilterPublishStatus(DEFAULT_BROWSER_PUBLISH_STATUS);
        setFilterStalePendingOnly(DEFAULT_BROWSER_STALE_PENDING_ONLY);
        setFilterCreatedAtFrom(DEFAULT_BROWSER_CREATED_AT_FROM);
        setFilterCreatedAtTo(DEFAULT_BROWSER_CREATED_AT_TO);
        setFilterPublishedAtFrom(DEFAULT_BROWSER_PUBLISHED_AT_FROM);
        setFilterPublishedAtTo(DEFAULT_BROWSER_PUBLISHED_AT_TO);
        setBrowserSortBy(DEFAULT_BROWSER_SORT_BY);
        setBrowserSortDirection(DEFAULT_BROWSER_SORT_DIRECTION);
        setBrowserPage(DEFAULT_BROWSER_PAGE);
        setBrowserSize(DEFAULT_BROWSER_SIZE);
        setExpandedMessageId(null);
        setBrowserMessage(null);
        setBrowserVariant(null);
        setBrowserStatusCode(null);
        setMessagesResponse(null);
        setHasLoadedMessages(false);
    };

    const refreshBrowserResults = async () => {
        await fetchMessages();
    };

    const updateBrowserSize = (value: string) => {
        setBrowserSize(value);
        setBrowserPage(DEFAULT_BROWSER_PAGE);
    };

    /**
     * Retries publishing a failed message.
     * 
     * @param message - The message object to retry.
     * @returns A promise that resolves when the retry operation is complete.
     */
    const retryFailedMessage = async (message: PagedStoredMessagesResponse["items"][number]) => {
        if (!message.id) {
            setBrowserMessage("Only stored messages with ids can be retried.");
            setBrowserVariant("danger");
            setBrowserStatusCode(400);
            return;
        }

        const messageKey = String(message.id ?? message.innerMessageId);
        setRetryingMessageId(messageKey);
        setBrowserMessage(null);
        setBrowserVariant(null);
        setBrowserStatusCode(null);

        try {
            const response = await axios.post<SolaceBrokerAPIResponse>(`${messagesBaseUrl}/${message.id}/retry`);
            await fetchMessages(messagesResponse ? {page: messagesResponse.page, size: messagesResponse.size} : undefined);
            setBrowserMessage(`Retried message ${message.innerMessageId} successfully.`);
            setBrowserVariant("success");
            setBrowserStatusCode(response.status);
        } catch (error) {
            if (axios.isAxiosError<SolaceBrokerAPIError>(error) && error.response) {
                setBrowserMessage(error.response.data?.message ?? "Failed to retry the stored message.");
                setBrowserVariant("danger");
                setBrowserStatusCode(error.response.status);
            } else {
                console.error("Failed to retry the stored message.", error);
                setBrowserMessage("Failed to retry the stored message.");
                setBrowserVariant("danger");
                setBrowserStatusCode(500);
            }
        } finally {
            setRetryingMessageId(null);
        }
    };

    /**
     * Reconciles a message that the backend has already marked as stale pending.
     *
     * The UI does not decide staleness; it only calls the reconciliation
     * endpoint for rows whose DTO exposes stalePending=true.
     * 
     * @param message - The message object to reconcile.
     * @returns A promise that resolves when the reconciliation is complete.
     */
    const reconcileStalePendingMessage = async (message: PagedStoredMessagesResponse["items"][number]) => {
        const messageKey = String(message.id ?? message.innerMessageId);
        setReconcilingMessageId(messageKey);
        setBrowserMessage(null);
        setBrowserVariant(null);
        setBrowserStatusCode(null);

        try {
            const response = await axios.post(`${messagesBaseUrl}/${message.id}/reconcile-stale-pending`);
            await fetchMessages(messagesResponse ? {page: messagesResponse.page, size: messagesResponse.size} : undefined);
            setBrowserMessage(`Reconciled stale pending message ${message.innerMessageId} successfully.`);
            setBrowserVariant("success");
            setBrowserStatusCode(response.status);
        } catch (error) {
            if (axios.isAxiosError<SolaceBrokerAPIError>(error) && error.response) {
                setBrowserMessage(error.response.data?.message ?? "Failed to reconcile the stale pending message.");
                setBrowserVariant("danger");
                setBrowserStatusCode(error.response.status);
            } else {
                console.error("Failed to reconcile the stale pending message.", error);
                setBrowserMessage("Failed to reconcile the stale pending message.");
                setBrowserVariant("danger");
                setBrowserStatusCode(500);
            }
        } finally {
            setReconcilingMessageId(null);
        }
    };

    /**
     * Copies a string value to the system clipboard and shows feedback.
     *
     * @param label - A label for the value being copied (used for feedback).
     * @param value - The string value to copy.
     * @returns A promise that resolves when the copy operation is complete.
     */
    const copyToClipboard = async (label: string, value: string) => {
        try {
            await navigator.clipboard.writeText(value);
            setCopyFeedback(`${label} copied.`);
        } catch (error) {
            console.error(`Failed to copy ${label}.`, error);
            setCopyFeedback(`Failed to copy ${label}.`);
        }
    };

    /**
     * Builds a shareable stored-message query URL from the current filters.
     */
    const pageLifecycleCounts = (messagesResponse?.items ?? []).reduce(
        (counts, message) => {
            if (message.publishStatus === "PUBLISHED") {
                counts.published += 1;
            } else if (message.publishStatus === "FAILED") {
                counts.failed += 1;
            } else if (message.publishStatus === "PENDING") {
                counts.pending += 1;
                if (message.stalePending) {
                    counts.stalePending += 1;
                }
            }
            return counts;
        },
        {published: 0, failed: 0, pending: 0, stalePending: 0}
    );
    const visibleMessageCount = messagesResponse?.items.length ?? 0;
    const matchingMessageCount = messagesResponse?.totalElements ?? 0;
    const visibleStartIndex =
        messagesResponse && visibleMessageCount > 0 ? messagesResponse.page * messagesResponse.size + 1 : 0;
    const visibleEndIndex =
        messagesResponse && visibleMessageCount > 0
            ? Math.min(messagesResponse.page * messagesResponse.size + visibleMessageCount, messagesResponse.totalElements)
            : 0;

    const applyLifecycleQuickFilter = async (publishStatus: "PUBLISHED" | "FAILED" | "PENDING") => {
        setFilterPublishStatus(publishStatus);
        setFilterStalePendingOnly(false);
        setBrowserPage(DEFAULT_BROWSER_PAGE);
        setExpandedMessageId(null);
        setBrowserMessage(null);
        setBrowserVariant(null);
        setBrowserStatusCode(null);
        await fetchMessages({page: Number(DEFAULT_BROWSER_PAGE), publishStatus, stalePendingOnly: false});
    };

    const applyStalePendingQuickFilter = async () => {
        setFilterPublishStatus("PENDING");
        setFilterStalePendingOnly(true);
        setBrowserPage(DEFAULT_BROWSER_PAGE);
        setExpandedMessageId(null);
        setBrowserMessage(null);
        setBrowserVariant(null);
        setBrowserStatusCode(null);
        await fetchMessages({page: Number(DEFAULT_BROWSER_PAGE), publishStatus: "PENDING", stalePendingOnly: true});
    };

    /**
     * Handles the form submission for publishing a new message.
     * 
     * @param e - The form event.
     * @returns A promise that resolves when the submission is complete.
     */
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
        const propertyMap = Object.fromEntries(
            trimmedProperties.map((property) => [property.key, property.value])
        );
        const messagePayload = {
            innerMessageId,
            destination,
            deliveryMode,
            priority: parsedPriority,
            payload: {
                type: payloadType,
                content: payloadContent
            },
            ...(trimmedProperties.length > 0 ? {properties: propertyMap} : {})
        };

        const validationErrors = {
            ...validateMessagePayload(messagePayload),
            ...validatePropertyRows(properties)
        };
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
        <div className="publisher-app">

            <meta name="viewport" content="width=device-width, initial-scale=1"/>

            <div className="container-fluid px-4 px-lg-5 py-4 py-lg-5">
                <header className="publisher-hero mb-4">
                    <h1 className="publisher-title mb-0">Solace Workspace</h1>
                    <p className="publisher-subtitle mb-0">
                        Publish messages and inspect stored results
                    </p>
                    <p className="publisher-meta mb-0">
                        © 2026{" "}
                        <a href="https://jrodolfo.net" target="_blank" rel="noreferrer">
                            Rod Oliveira
                        </a>{" "}
                        <span aria-hidden="true">|</span>{" "}
                        <a
                            href="https://github.com/jrodolfo/solace/blob/main/LICENSE"
                            target="_blank"
                            rel="noreferrer"
                        >
                            MIT License
                        </a>{" "}
                        <span aria-hidden="true">|</span>{" "}
                        <a href="https://github.com/jrodolfo/solace" target="_blank" rel="noreferrer">
                            GitHub Repo
                        </a>
                    </p>
                </header>

                <div className="workspace-tabs mb-4" role="tablist" aria-label="Workspace sections">
                    <button
                        type="button"
                        role="tab"
                        id="workspace-tab-publish"
                        aria-selected={activeWorkspaceSection === "PUBLISH"}
                        aria-controls="workspace-panel-publish"
                        className={`workspace-tab${activeWorkspaceSection === "PUBLISH" ? " is-active" : ""}`}
                        onClick={() => setActiveWorkspaceSection("PUBLISH")}
                    >
                        Write
                    </button>
                    <button
                        type="button"
                        role="tab"
                        id="workspace-tab-browser"
                        aria-selected={activeWorkspaceSection === "BROWSER"}
                        aria-controls="workspace-panel-browser"
                        className={`workspace-tab${activeWorkspaceSection === "BROWSER" ? " is-active" : ""}`}
                        onClick={() => setActiveWorkspaceSection("BROWSER")}
                    >
                        Read
                    </button>
                </div>

                <div className="row g-4 align-items-start">
                    <div
                        className={`col-12 workspace-pane${activeWorkspaceSection === "PUBLISH" ? " is-active" : ""}`}
                        id="workspace-panel-publish"
                        role="tabpanel"
                        aria-labelledby="workspace-tab-publish"
                        hidden={activeWorkspaceSection !== "PUBLISH"}
                    >
                        <section className="workspace-card h-100">
                            <div className="workspace-card-header">
                                <div>
                                    <p className="workspace-kicker mb-1">write</p>
                                    <h2 className="workspace-title mb-1">Publish Message</h2>
                                    <p className="workspace-copy mb-0">Connection details, message fields, payload, and optional properties.</p>
                                </div>
                            </div>

                            {submissionMessage && submissionVariant && (
                                <div className={`alert alert-${submissionVariant}`} role="alert">
                                    {submissionMessage}
                                </div>
                            )}

                            <form onSubmit={handleSubmit}>
                                <div className="form-section-grid">
                                    <section className="form-section-block">
                                        <div className="form-section-heading">
                                            <p className="workspace-kicker mb-1">connection</p>
                                            <h3 className="form-section-title">Broker Access</h3>
                                        </div>
                                        <div className="row g-3">
                                            <div className="col-md-6">
                                                <label htmlFor="userName" className="form-label">
                                                    Cloud Username
                                                </label>
                                                <input
                                                    id="userName"
                                                    type="text"
                                                    className="form-control"
                                                    value={userName}
                                                    onChange={(e) => setUserName(e.target.value)}
                                                    placeholder="e.g. solace-cloud-client"
                                                    required
                                                />
                                            </div>
                                            <div className="col-md-6">
                                                <label htmlFor="password" className="form-label">
                                                    Cloud Password
                                                </label>
                                                <input
                                                    id="password"
                                                    type="password"
                                                    className="form-control"
                                                    value={password}
                                                    onChange={(e) => setPassword(e.target.value)}
                                                    placeholder="e.g. ujce53s4s2adjqhgb5sdn3ixxx"
                                                    required
                                                />
                                            </div>
                                            <div className="col-md-6">
                                                <label htmlFor="host" className="form-label">
                                                    Cloud Host
                                                </label>
                                                <input
                                                    id="host"
                                                    type="text"
                                                    className="form-control"
                                                    value={host}
                                                    onChange={(e) => setHost(e.target.value)}
                                                    placeholder="e.g. wss://mr-connection-xqriomoixxx.messaging.solace.cloud:443"
                                                    required
                                                />
                                            </div>
                                            <div className="col-md-6">
                                                <label htmlFor="vpnName" className="form-label">
                                                    Cloud VPN
                                                </label>
                                                <input
                                                    id="vpnName"
                                                    type="text"
                                                    className="form-control"
                                                    value={vpnName}
                                                    onChange={(e) => setVpnName(e.target.value)}
                                                    placeholder="e.g. my-solace-service"
                                                    required
                                            />
                                        </div>
                                    </div>
                                </section>

                                    <section className="form-section-block">
                                        <div className="form-section-heading">
                                            <p className="workspace-kicker mb-1">message</p>
                                            <h3 className="form-section-title">Message Details</h3>
                                        </div>
                                        <div className="row g-3">
                                            <div className="col-md-6">
                                                <label htmlFor="innerMessageId" className="form-label">
                                                    Inner Message Id
                                                </label>
                                                <input
                                                    id="innerMessageId"
                                                    type="text"
                                                    className="form-control"
                                                    value={innerMessageId}
                                                    onChange={(e) => setInnerMessageId(e.target.value)}
                                                    placeholder="Enter the inner message id"
                                                    required
                                                />
                                            </div>
                                            <div className="col-md-6">
                                                <label htmlFor="destination" className="form-label">
                                                    Destination
                                                </label>
                                                <input
                                                    id="destination"
                                                    type="text"
                                                    className="form-control"
                                                    value={destination}
                                                    onChange={(e) => setDestination(e.target.value)}
                                                    placeholder="Enter the destination topic"
                                                    required
                                                />
                                            </div>
                                            <div className="col-md-6">
                                                <label htmlFor="deliveryMode" className="form-label">
                                                    Delivery Mode
                                                </label>
                                                <select
                                                    id="deliveryMode"
                                                    className="form-select"
                                                    value={deliveryMode}
                                                    onChange={(e) => setDeliveryMode(e.target.value as DeliveryMode)}
                                                    required
                                                >
                                                    {DELIVERY_MODE_OPTIONS.map((deliveryModeOption) => (
                                                        <option key={deliveryModeOption} value={deliveryModeOption}>
                                                            {deliveryModeOption}
                                                        </option>
                                                    ))}
                                                </select>
                                            </div>
                                            <div className="col-md-6">
                                                <label htmlFor="priority" className="form-label">
                                                    Priority
                                                </label>
                                                <input
                                                    id="priority"
                                                    type="number"
                                                    min="0"
                                                    className="form-control"
                                                    value={priority}
                                                    onChange={(e) => setPriority(e.target.value)}
                                                    placeholder="Enter the priority"
                                                    required
                                                />
                                            </div>
                                            <div className="col-md-6">
                                                <label htmlFor="payloadType" className="form-label">
                                                    Payload Type
                                                </label>
                                                <select
                                                    id="payloadType"
                                                    className="form-select"
                                                    value={payloadType}
                                                    onChange={(e) => setPayloadType(e.target.value as PayloadType | "")}
                                                    required
                                                >
                                                    <option value="">Select a payload type</option>
                                                    {PAYLOAD_TYPE_OPTIONS.map((option) => (
                                                        <option key={option} value={option}>
                                                            {option}
                                                        </option>
                                                    ))}
                                                </select>
                                            </div>
                                            <div className="col-12">
                                                <label htmlFor="payloadContent" className="form-label">
                                                    Payload Content
                                                </label>
                                                <textarea
                                                    id="payloadContent"
                                                    className="form-control publisher-textarea"
                                                    value={payloadContent}
                                                    onChange={(e) => setPayloadContent(e.target.value)}
                                                    rows={6}
                                                    placeholder="Enter the payload content"
                                                    required
                                                ></textarea>
                                            </div>
                                        </div>
                                    </section>

                                    <section className="form-section-block">
                                        <div className="form-section-heading">
                                            <p className="workspace-kicker mb-1">optional</p>
                                            <h3 className="form-section-title">Message Properties</h3>
                                        </div>
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
                                            className="btn btn-outline-primary"
                                            onClick={addPropertyRow}
                                        >
                                            Add Property
                                        </button>
                                    </section>
                                </div>

                                <div className="publish-actions mt-4">
                                    <button type="submit" className="workspace-action-button workspace-action-button-primary">
                                        Publish Message
                                    </button>
                                </div>
                            </form>

                            <div className="response-panel mt-4">
                                {showResponse && <ShowOutput res={response}></ShowOutput>}
                            </div>
                        </section>
                    </div>

                    <div
                        className={`col-12 workspace-pane${activeWorkspaceSection === "BROWSER" ? " is-active" : ""}`}
                        id="workspace-panel-browser"
                        role="tabpanel"
                        aria-labelledby="workspace-tab-browser"
                        hidden={activeWorkspaceSection !== "BROWSER"}
                    >
                        <section className="workspace-card h-100">
                            <div className="workspace-card-header">
                                <div>
                                    <p className="workspace-kicker mb-1">read</p>
                                    <h2 className="workspace-title mb-1">Stored Messages</h2>
                                    <p className="workspace-copy mb-0">Filter, sort, and page through persisted broker messages.</p>
                                </div>
                            </div>

                            {browserMessage && browserVariant && (
                                <div className={`alert alert-${browserVariant}`} role="alert">
                                    <div>
                                        {browserMessage}
                                        {browserVariant === "danger" && browserStatusCode !== null && ` (status: ${browserStatusCode})`}
                                    </div>
                                </div>
                            )}

                            {copyFeedback && (
                                <div className="alert alert-secondary browser-copy-feedback" role="status" aria-live="polite">
                                    {copyFeedback}
                                </div>
                            )}

                            <form onSubmit={handleBrowseMessages}>
                                <div className="browser-control-stack">
                                    <section className="form-section-block browser-control-section">
                                        <div className="browser-control-section-header">
                                            <p className="workspace-kicker mb-1">filters</p>
                                            <h3 className="browser-control-title mb-0">Message Filters</h3>
                                        </div>
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
                                            <select
                                                id="filterDeliveryMode"
                                                className="form-select"
                                                value={filterDeliveryMode}
                                                onChange={(e) => setFilterDeliveryMode(e.target.value as DeliveryMode | "")}
                                            >
                                                <option value="">all delivery modes</option>
                                                {DELIVERY_MODE_OPTIONS.map((deliveryModeOption) => (
                                                    <option key={deliveryModeOption} value={deliveryModeOption}>
                                                        {deliveryModeOption}
                                                    </option>
                                                ))}
                                            </select>
                                        </div>
                                        <div className="col-md-4">
                                            <label htmlFor="filterPayloadType" className="form-label">
                                                Filter Payload Type
                                            </label>
                                            <select
                                                id="filterPayloadType"
                                                className="form-select"
                                                value={filterPayloadType}
                                                onChange={(e) => setFilterPayloadType(e.target.value as PayloadType | "")}
                                            >
                                                <option value="">all payload types</option>
                                                {PAYLOAD_TYPE_OPTIONS.map((payloadTypeOption) => (
                                                    <option key={payloadTypeOption} value={payloadTypeOption}>
                                                        {payloadTypeOption}
                                                    </option>
                                                ))}
                                            </select>
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
                                        <div className="col-md-4">
                                            <label htmlFor="filterPublishStatus" className="form-label">
                                                Filter Publish Status
                                            </label>
                                            <select
                                                id="filterPublishStatus"
                                                className="form-select"
                                                value={filterPublishStatus}
                                                onChange={(e) => {
                                                    const nextStatus = e.target.value;
                                                    setFilterPublishStatus(nextStatus);
                                                    if (nextStatus !== "PENDING") {
                                                        setFilterStalePendingOnly(false);
                                                    }
                                                }}
                                            >
                                                <option value="">all statuses</option>
                                                <option value="PENDING">PENDING</option>
                                                <option value="PUBLISHED">PUBLISHED</option>
                                                <option value="FAILED">FAILED</option>
                                            </select>
                                        </div>
                                        <div className="col-md-2">
                                            <label htmlFor="filterStalePendingOnly" className="form-label">
                                                Stale Pending Only
                                            </label>
                                            <div className="form-check mt-2">
                                                <input
                                                    id="filterStalePendingOnly"
                                                    type="checkbox"
                                                    className="form-check-input"
                                                    checked={filterStalePendingOnly}
                                                    onChange={(e) => {
                                                        const checked = e.target.checked;
                                                        setFilterStalePendingOnly(checked);
                                                        if (checked) {
                                                            setFilterPublishStatus("PENDING");
                                                        }
                                                    }}
                                                />
                                                <label className="form-check-label" htmlFor="filterStalePendingOnly">
                                                    only stale pending
                                                </label>
                                            </div>
                                        </div>
                                        <div className="col-md-3">
                                            <label htmlFor="filterCreatedAtFrom" className="form-label">
                                                Created At From
                                            </label>
                                            <input
                                                id="filterCreatedAtFrom"
                                                type="datetime-local"
                                                className="form-control"
                                                value={filterCreatedAtFrom}
                                                onChange={(e) => setFilterCreatedAtFrom(e.target.value)}
                                            />
                                        </div>
                                        <div className="col-md-3">
                                            <label htmlFor="filterCreatedAtTo" className="form-label">
                                                Created At To
                                            </label>
                                            <input
                                                id="filterCreatedAtTo"
                                                type="datetime-local"
                                                className="form-control"
                                                value={filterCreatedAtTo}
                                                onChange={(e) => setFilterCreatedAtTo(e.target.value)}
                                            />
                                        </div>
                                        <div className="col-md-3">
                                            <label htmlFor="filterPublishedAtFrom" className="form-label">
                                                Published At From
                                            </label>
                                            <input
                                                id="filterPublishedAtFrom"
                                                type="datetime-local"
                                                className="form-control"
                                                value={filterPublishedAtFrom}
                                                onChange={(e) => setFilterPublishedAtFrom(e.target.value)}
                                            />
                                        </div>
                                        <div className="col-md-3">
                                            <label htmlFor="filterPublishedAtTo" className="form-label">
                                                Published At To
                                            </label>
                                            <input
                                                id="filterPublishedAtTo"
                                                type="datetime-local"
                                                className="form-control"
                                                value={filterPublishedAtTo}
                                                onChange={(e) => setFilterPublishedAtTo(e.target.value)}
                                            />
                                        </div>
                                        </div>
                                    </section>

                                    <section className="form-section-block browser-control-section">
                                        <div className="browser-control-section-header">
                                            <p className="workspace-kicker mb-1">browse</p>
                                            <h3 className="browser-control-title mb-0">Sort & Paging</h3>
                                        </div>
                                        <div className="row g-3 align-items-end">
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
                                                <label htmlFor="browserSize" className="form-label">
                                                    Messages Per Page
                                                </label>
                                                <input
                                                    id="browserSize"
                                                    type="number"
                                                    min="1"
                                                    max="100"
                                                    className="form-control"
                                                    value={browserSize}
                                                    onChange={(e) => updateBrowserSize(e.target.value)}
                                                />
                                            </div>
                                        </div>
                                    </section>

                                    <section className="form-section-block browser-control-section">
                                        <div className="browser-control-section-header">
                                            <p className="workspace-kicker mb-1">results</p>
                                            <h3 className="browser-control-title mb-0">Load Results</h3>
                                        </div>
                                        <div className="browser-button-row">
                                            <button type="submit" className="btn btn-outline-primary" disabled={isLoadingMessages}>
                                                {isLoadingMessages ? "Loading..." : "Load Messages"}
                                            </button>
                                            <button
                                                type="button"
                                                className="btn btn-outline-primary"
                                                onClick={refreshBrowserResults}
                                                disabled={isLoadingMessages}
                                            >
                                                Refresh Results
                                            </button>
                                            <button
                                                type="button"
                                                className="btn btn-outline-primary"
                                                onClick={resetBrowserFilters}
                                                disabled={isLoadingMessages}
                                            >
                                                Reset Filters
                                            </button>
                                        </div>
                                    </section>

                                </div>
                            </form>

                            {isLoadingMessages && (
                                <div className="mt-4 browser-feedback-card" role="status" aria-live="polite">
                                    <strong>Loading stored messages...</strong>
                                    <span>Fetching the latest results for the current filters.</span>
                                </div>
                            )}

                            {!isLoadingMessages && !hasLoadedMessages && (
                                <div className="mt-4 browser-feedback-card">
                                    <strong>No results loaded yet.</strong>
                                    <span>Choose filters and page size above, then select Load Messages to fetch the first visible page.</span>
                                </div>
                            )}

                            {!isLoadingMessages && messagesResponse && (
                                <div className="mt-4">
                                    <div className="browser-summary mb-3" data-testid="browser-summary">
                                        <div>
                                            <strong>
                                                Showing {visibleMessageCount === 0 ? "0" : `${visibleStartIndex}-${visibleEndIndex}`} of{" "}
                                                {matchingMessageCount} matching messages
                                            </strong>
                                        </div>
                                        <div className="browser-summary-actions">
                                            <span>
                                                Visible page {messagesResponse.page + 1} of {messagesResponse.totalPages || 1}, page size{" "}
                                                {messagesResponse.size}
                                            </span>
                                            <div className="browser-button-row">
                                                <button
                                                    type="button"
                                                    className="btn btn-outline-primary"
                                                    onClick={loadPreviousPage}
                                                    disabled={messagesResponse.first || isLoadingMessages}
                                                >
                                                    Previous Page
                                                </button>
                                                <button
                                                    type="button"
                                                    className="btn btn-outline-primary"
                                                    onClick={loadNextPage}
                                                    disabled={messagesResponse.last || isLoadingMessages}
                                                >
                                                    Next Page
                                                </button>
                                            </div>
                                        </div>
                                    </div>
                                    <div className="browser-lifecycle-summary browser-lifecycle-summary-aggregate mb-2" data-testid="browser-lifecycle-summary-aggregate">
                                        <div className="browser-lifecycle-pill">
                                            <span className="meta-label">filtered published</span>
                                            <strong>{messagesResponse.lifecycleCounts.publishedCount}</strong>
                                        </div>
                                        <div className="browser-lifecycle-pill">
                                            <span className="meta-label">filtered failed</span>
                                            <strong>{messagesResponse.lifecycleCounts.failedCount}</strong>
                                        </div>
                                        <div className="browser-lifecycle-pill">
                                            <span className="meta-label">filtered pending</span>
                                            <strong>{messagesResponse.lifecycleCounts.pendingCount}</strong>
                                        </div>
                                        <div className="browser-lifecycle-pill">
                                            <span className="meta-label">filtered stale pending</span>
                                            <strong>{messagesResponse.lifecycleCounts.stalePendingCount}</strong>
                                        </div>
                                        <div className="browser-lifecycle-pill">
                                            <span className="meta-label">retryable failed</span>
                                            <strong>{messagesResponse.lifecycleCounts.retryableFailedCount}</strong>
                                        </div>
                                        <div className="browser-lifecycle-pill">
                                            <span className="meta-label">non-retryable failed</span>
                                            <strong>{messagesResponse.lifecycleCounts.nonRetryableFailedCount}</strong>
                                        </div>
                                    </div>
                                    <div className="browser-lifecycle-summary mb-3" data-testid="browser-lifecycle-summary">
                                        <button
                                            type="button"
                                            className={`browser-lifecycle-pill browser-lifecycle-pill-button${filterPublishStatus === "PUBLISHED" ? " is-active" : ""}`}
                                            onClick={() => applyLifecycleQuickFilter("PUBLISHED")}
                                            disabled={isLoadingMessages || retryingMessageId !== null || reconcilingMessageId !== null}
                                        >
                                            <span className="meta-label">published</span>
                                            <strong>{pageLifecycleCounts.published}</strong>
                                        </button>
                                        <button
                                            type="button"
                                            className={`browser-lifecycle-pill browser-lifecycle-pill-button${filterPublishStatus === "FAILED" ? " is-active" : ""}`}
                                            onClick={() => applyLifecycleQuickFilter("FAILED")}
                                            disabled={isLoadingMessages || retryingMessageId !== null || reconcilingMessageId !== null}
                                        >
                                            <span className="meta-label">failed</span>
                                            <strong>{pageLifecycleCounts.failed}</strong>
                                        </button>
                                        <button
                                            type="button"
                                            className={`browser-lifecycle-pill browser-lifecycle-pill-button${filterPublishStatus === "PENDING" ? " is-active" : ""}`}
                                            onClick={() => applyLifecycleQuickFilter("PENDING")}
                                            disabled={isLoadingMessages || retryingMessageId !== null || reconcilingMessageId !== null}
                                        >
                                            <span className="meta-label">pending</span>
                                            <strong>{pageLifecycleCounts.pending}</strong>
                                        </button>
                                        <button
                                            type="button"
                                            className={`browser-lifecycle-pill browser-lifecycle-pill-button${filterPublishStatus === "PENDING" && filterStalePendingOnly ? " is-active" : ""}`}
                                            onClick={applyStalePendingQuickFilter}
                                            disabled={isLoadingMessages || retryingMessageId !== null || reconcilingMessageId !== null}
                                        >
                                            <span className="meta-label">stale pending</span>
                                            <strong>{pageLifecycleCounts.stalePending}</strong>
                                        </button>
                                    </div>

                                    {messagesResponse.items.length === 0 ? (
                                        <div className="browser-feedback-card">
                                            <strong>No stored messages matched these filters.</strong>
                                            <span>Adjust the filters or reset them, then load the browser again.</span>
                                        </div>
                                    ) : (
                                        <div className="row g-3">
                                            {messagesResponse.items.map((message) => (
                                                <div className="col-12" key={`${message.id ?? "message"}-${message.innerMessageId}`}>
                                                    {(() => {
                                                        const messageKey = String(message.id ?? message.innerMessageId);
                                                        const isExpanded = expandedMessageId === messageKey;

                                                        return (
                                                    <article className="message-browser-card">
                                                        <div className="message-browser-topline">
                                                            <div>
                                                                <h5 className="mb-1">{message.innerMessageId}</h5>
                                                                <p className="message-browser-destination mb-0">{message.destination}</p>
                                                            </div>
                                                            <div className="message-browser-badges">
                                                                <span className="badge text-bg-secondary">{message.deliveryMode}</span>
                                                                <span className="badge text-bg-light">priority {message.priority}</span>
                                                                <span className={`badge text-bg-${publishStatusVariant(message.publishStatus)}`}>{message.publishStatus}</span>
                                                                {message.stalePending && (
                                                                    <span className="badge text-bg-dark">stale pending</span>
                                                                )}
                                                            </div>
                                                        </div>
                                                        <div className="message-browser-copy-actions">
                                                            <button
                                                                type="button"
                                                                className="btn btn-sm btn-outline-secondary"
                                                                onClick={() => copyToClipboard("Destination", message.destination)}
                                                            >
                                                                Copy Destination
                                                            </button>
                                                            <button
                                                                type="button"
                                                                className="btn btn-sm btn-outline-secondary"
                                                                onClick={() => copyToClipboard("Payload content", message.payload?.content ?? "")}
                                                            >
                                                                Copy Payload
                                                            </button>
                                                        </div>
                                                        <div className="message-browser-meta">
                                                            <div>
                                                                <span className="meta-label">payload type</span>
                                                                <strong>{message.payload?.type}</strong>
                                                            </div>
                                                            <div>
                                                                <span className="meta-label">properties</span>
                                                                <strong>{propertyCount(message.properties)}</strong>
                                                            </div>
                                                            <div>
                                                                <span className="meta-label">published</span>
                                                                <strong>{formatTimestamp(message.publishedAt)}</strong>
                                                            </div>
                                                            <div>
                                                                <span className="meta-label">created</span>
                                                                <strong>{formatTimestamp(message.createdAt)}</strong>
                                                            </div>
                                                        </div>
                                                        <div className="message-browser-actions">
                                                            {message.publishStatus === "FAILED" && message.retrySupported && (
                                                                <button
                                                                    type="button"
                                                                    className="btn btn-sm btn-outline-danger"
                                                                    onClick={() => retryFailedMessage(message)}
                                                                    disabled={isLoadingMessages || retryingMessageId === messageKey || reconcilingMessageId === messageKey}
                                                                >
                                                                    {retryingMessageId === messageKey ? "Retrying..." : "Retry Failed Message"}
                                                                </button>
                                                            )}
                                                            {message.publishStatus === "PENDING" && message.stalePending && (
                                                                <button
                                                                    type="button"
                                                                    className="btn btn-sm btn-outline-warning"
                                                                    onClick={() => reconcileStalePendingMessage(message)}
                                                                    disabled={isLoadingMessages || retryingMessageId === messageKey || reconcilingMessageId === messageKey}
                                                                >
                                                                    {reconcilingMessageId === messageKey ? "Reconciling..." : "Mark Stale Pending As Failed"}
                                                                </button>
                                                            )}
                                                            <button
                                                                type="button"
                                                                className="btn btn-sm btn-outline-primary"
                                                                aria-expanded={isExpanded}
                                                                aria-controls={`message-details-${messageKey}`}
                                                                onClick={() => setExpandedMessageId(isExpanded ? null : messageKey)}
                                                            >
                                                                {isExpanded ? "Hide Details" : "Show Details"}
                                                            </button>
                                                        </div>
                                                        {isExpanded && (
                                                            <div
                                                                className="message-browser-details"
                                                                id={`message-details-${messageKey}`}
                                                            >
                                                                <div className="message-browser-timestamps">
                                                                    <div>
                                                                        <span className="meta-label">created at</span>
                                                                        <p className="mb-0">{formatTimestamp(message.createdAt)}</p>
                                                                    </div>
                                                                    <div>
                                                                        <span className="meta-label">updated at</span>
                                                                        <p className="mb-0">{formatTimestamp(message.updatedAt)}</p>
                                                                    </div>
                                                                    <div>
                                                                        <span className="meta-label">published at</span>
                                                                        <p className="mb-0">{formatTimestamp(message.publishedAt)}</p>
                                                                    </div>
                                                                </div>
                                                                <div className="message-browser-content">
                                                                    <span className="meta-label">publish status</span>
                                                                    <p className="mb-0">{message.publishStatus}</p>
                                                                </div>
                                                                {message.stalePending && (
                                                                    <div className="message-browser-content">
                                                                        <span className="meta-label">pending state</span>
                                                                        <p className="mb-0">This pending message is older than the stale threshold and may need review.</p>
                                                                    </div>
                                                                )}
                                                                {message.failureReason && (
                                                                    <div className="message-browser-content">
                                                                        <span className="meta-label">failure reason</span>
                                                                        <p className="mb-0">{message.failureReason}</p>
                                                                    </div>
                                                                )}
                                                                {!message.retrySupported && message.retryBlockedReason && (
                                                                    <div className="message-browser-content">
                                                                        <span className="meta-label">retry unavailable</span>
                                                                        <p className="mb-0">{message.retryBlockedReason}</p>
                                                                    </div>
                                                                )}
                                                                <div className="message-browser-content">
                                                                    <span className="meta-label">payload content</span>
                                                                    <p className="mb-0">{message.payload?.content}</p>
                                                                </div>
                                                                <div className="message-browser-properties">
                                                                    <span className="meta-label">properties</span>
                                                                    {Object.keys(message.properties ?? {}).length === 0 ? (
                                                                        <p className="mb-0">none</p>
                                                                    ) : (
                                                                        <>
                                                                            <button
                                                                                type="button"
                                                                                className="btn btn-sm btn-outline-secondary mb-2"
                                                                                onClick={() =>
                                                                                    copyToClipboard(
                                                                                        "Properties",
                                                                                        Object.entries(message.properties)
                                                                                            .map(([key, value]) => `${key}: ${value}`)
                                                                                            .join("\n")
                                                                                    )
                                                                                }
                                                                            >
                                                                                Copy Properties
                                                                            </button>
                                                                            <ul className="mb-0 mt-2">
                                                                                {Object.entries(message.properties).map(([key, value]) => (
                                                                                    <li key={`${key}-${value}`}>
                                                                                        {key}: {value}
                                                                                    </li>
                                                                                ))}
                                                                            </ul>
                                                                        </>
                                                                    )}
                                                                </div>
                                                            </div>
                                                        )}
                                                    </article>
                                                        );
                                                    })()}
                                                </div>
                                            ))}
                                        </div>
                                    )}
                                </div>
                            )}
                        </section>
                    </div>
                </div>
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
    } else if (!isAllowedDeliveryMode(payload.deliveryMode)) {
        validationErrors["message.deliveryMode"] = "message.deliveryMode must be one of DIRECT, NON_PERSISTENT, PERSISTENT";
    }
    if (!isNonNegativeNumber(payload.priority)) {
        validationErrors["message.priority"] = "message.priority is required";
    } else if (!isMessagePriority(payload.priority)) {
        validationErrors["message.priority"] = "message.priority must be less than or equal to 255";
    }

    if (!isRecord(payload.payload)) {
        validationErrors["message.payload"] = "message.payload is required";
        return validationErrors;
    }

    if (!hasNonBlankString(payload.payload.type)) {
        validationErrors["message.payload.type"] = "payload.type is required";
    } else if (!isAllowedPayloadType(payload.payload.type)) {
        validationErrors["message.payload.type"] = "payload.type must be one of TEXT, BINARY, JSON, XML";
    }
    if (!hasNonBlankString(payload.payload.content)) {
        validationErrors["message.payload.content"] = "payload.content is required";
    }

    return validationErrors;
}

function validatePropertyRows(rows: MessagePropertyFormRow[]): MessagePayloadValidationErrorMap {
    const validationErrors: MessagePayloadValidationErrorMap = {};

    rows.forEach((property, index) => {
        const hasKey = property.key.trim().length > 0;
        const hasValue = property.value.trim().length > 0;

        if (hasKey !== hasValue) {
            if (!hasKey) {
                validationErrors[`message.properties[${index}].key`] = `message.properties[${index}].key is required`;
            }
            if (!hasValue) {
                validationErrors[`message.properties[${index}].value`] = `message.properties[${index}].value is required`;
            }
        }
    });

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

function isMessagePriority(value: unknown): value is number {
    return isNonNegativeNumber(value) && value <= MAX_MESSAGE_PRIORITY;
}

function propertyCount(properties?: Record<string, string>): number {
    return Object.keys(properties ?? {}).length;
}

function publishStatusVariant(status: "PENDING" | "PUBLISHED" | "FAILED"): string {
    if (status === "PUBLISHED") {
        return "success";
    }
    if (status === "FAILED") {
        return "danger";
    }
    return "warning";
}

/**
 * Formats the stored-message load result with correct singular and plural wording.
 *
 * @param count - Number of messages loaded into the visible page.
 * @returns A user-facing load result message.
 */
function formatLoadedMessages(count: number): string {
    return `Loaded ${count} ${count === 1 ? "message" : "messages"}.`;
}

/**
 * Converts a datetime-local input value to the backend ISO local date-time format.
 *
 * @param value - The datetime-local input value.
 * @returns The ISO local date-time value expected by the API.
 */
function toIsoLocalDateTime(value: string): string {
    return value.length === 16 ? `${value}:00` : value;
}

/**
 * Formats an ISO timestamp into a human-readable date and time string.
 *
 * @param value - The ISO timestamp to format.
 * @returns A formatted date string, or "not available" if the value is missing.
 */
function formatTimestamp(value?: string | null): string {
    if (!value) {
        return "not available";
    }

    const timestamp = new Date(value);
    if (Number.isNaN(timestamp.getTime())) {
        return value;
    }

    return new Intl.DateTimeFormat(undefined, {
        year: "numeric",
        month: "short",
        day: "numeric",
        hour: "numeric",
        minute: "2-digit"
    }).format(timestamp);
}

export default App;
