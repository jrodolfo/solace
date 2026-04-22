import App from "./App";
import '@testing-library/jest-dom';
import {render, screen, waitFor, within} from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import "@testing-library/jest-dom";
import axios from "axios";
import {beforeEach, Mock, vi} from "vitest";
import {AxiosHeaders} from "axios";
import type {SolaceBrokerAPIResponse} from "./SolaceBrokerAPIResponse";
import type {SolaceBrokerAPIError} from "./SolaceBrokerAPIError";
import type {PagedStoredMessagesResponse, StoredMessage} from "./StoredMessageTypes";

const writeTextMock = vi.fn();
const createObjectUrlMock = vi.fn();
const revokeObjectUrlMock = vi.fn();
const anchorClickMock = vi.fn();

class MockBlob {
    readonly parts: unknown[];
    readonly type: string;

    constructor(parts: unknown[], options?: { type?: string }) {
        this.parts = parts;
        this.type = options?.type ?? "";
    }
}

test('it shows 5 inputs and 1 button', () => {
    render(<App/>);
    const textboxes = screen.getAllByRole('textbox');
    const spinbuttons = screen.getAllByRole('spinbutton');
    expect(textboxes).toHaveLength(13);
    expect(spinbuttons).toHaveLength(3);
    expect(screen.getByRole('button', {name: /publish message/i})).toBeInTheDocument();
    expect(screen.getByRole('button', {name: /load messages/i})).toBeInTheDocument();
    expect(screen.getByRole('button', {name: /refresh results/i})).toBeInTheDocument();
    expect(screen.getByRole('button', {name: /reset filters/i})).toBeInTheDocument();
    expect(screen.getByLabelText(/Created At From/i)).toBeInTheDocument();
    expect(screen.getByLabelText(/Created At To/i)).toBeInTheDocument();
    expect(screen.getByLabelText(/Published At From/i)).toBeInTheDocument();
    expect(screen.getByLabelText(/Published At To/i)).toBeInTheDocument();
    expect(screen.getByText(/No results loaded yet\./i)).toBeInTheDocument();
});

function buildBulkRetryResponse(overrides?: Partial<{
    totalRequested: number;
    retriedSuccessfully: number;
    failedToRetry: number;
    skipped: number;
    results: Array<{
        messageId: number | null;
        outcome: "RETRIED" | "FAILED" | "SKIPPED";
        detail: string;
        publishStatus?: "PENDING" | "PUBLISHED" | "FAILED" | null;
        response?: SolaceBrokerAPIResponse | null;
    }>;
}>) {
    return {
        totalRequested: overrides?.totalRequested ?? 2,
        retriedSuccessfully: overrides?.retriedSuccessfully ?? 2,
        failedToRetry: overrides?.failedToRetry ?? 0,
        skipped: overrides?.skipped ?? 0,
        results: overrides?.results ?? [
            {
                messageId: 7,
                outcome: "RETRIED" as const,
                detail: "Message retried successfully",
                publishStatus: "PUBLISHED" as const,
                response: buildPublishSuccessResponse({destination: "solace/java/direct/system-07"}),
            },
            {
                messageId: 8,
                outcome: "RETRIED" as const,
                detail: "Message retried successfully",
                publishStatus: "PUBLISHED" as const,
                response: buildPublishSuccessResponse({destination: "solace/java/direct/system-08"}),
            },
        ],
    };
}

// Mock Axios
vi.mock("axios");

const mockedAxios = axios as unknown as {
    post: Mock;
    get: Mock;
    isAxiosError: Mock;
};

beforeEach(() => {
    vi.useRealTimers();
    mockedAxios.post.mockReset();
    mockedAxios.get.mockReset();
    mockedAxios.isAxiosError.mockImplementation((error: unknown) => Boolean((error as { isAxiosError?: boolean })?.isAxiosError));
    writeTextMock.mockReset();
    createObjectUrlMock.mockReset();
    createObjectUrlMock.mockReturnValue("blob:test-export");
    revokeObjectUrlMock.mockReset();
    anchorClickMock.mockReset();
});

Object.defineProperty(navigator, "clipboard", {
    value: {
        writeText: writeTextMock,
    },
    configurable: true,
});

Object.defineProperty(URL, "createObjectURL", {
    value: createObjectUrlMock,
    configurable: true,
});

Object.defineProperty(URL, "revokeObjectURL", {
    value: revokeObjectUrlMock,
    configurable: true,
});

Object.defineProperty(globalThis, "Blob", {
    value: MockBlob,
    configurable: true,
});

HTMLAnchorElement.prototype.click = anchorClickMock;

function buildPublishSuccessResponse(overrides?: Partial<SolaceBrokerAPIResponse>): SolaceBrokerAPIResponse {
    return {
        destination: "solace/java/direct/system-01",
        content: "01001000 01100101 01101100",
        ...overrides,
    };
}

function buildValidationErrorResponse(overrides?: Partial<SolaceBrokerAPIError>): SolaceBrokerAPIError {
    return {
        status: 400,
        error: "Bad Request",
        message: "Request validation failed",
        path: "/api/v1/messages/message",
        validationErrors: {
            "message.innerMessageId": "message.innerMessageId is required",
        },
        ...overrides,
    };
}

function buildStoredMessage(overrides?: Partial<StoredMessage>): StoredMessage {
    return {
        id: 1,
        innerMessageId: "001",
        destination: "solace/java/direct/system-01",
        deliveryMode: "PERSISTENT",
        priority: 3,
        publishStatus: "PUBLISHED",
        stalePending: false,
        failureReason: null,
        publishedAt: "2026-04-21T08:00:00Z",
        retrySupported: true,
        retryBlockedReason: null,
        payload: {
            type: "binary",
            content: "01001000 01100101 01101100",
        },
        properties: {
            region: "ca-east",
        },
        ...overrides,
    };
}

function buildMessagesPage(page: number, overrides?: Partial<{
    size: number;
    totalElements: number;
    totalPages: number;
    first: boolean;
    last: boolean;
    items: StoredMessage[];
    lifecycleCounts: PagedStoredMessagesResponse["lifecycleCounts"];
}>): PagedStoredMessagesResponse {
    const items = overrides?.items ?? [buildStoredMessage()];
    return {
        items,
        page,
        size: overrides?.size ?? 20,
        totalElements: overrides?.totalElements ?? 2,
        totalPages: overrides?.totalPages ?? 2,
        first: overrides?.first ?? page === 0,
        last: overrides?.last ?? false,
        lifecycleCounts: overrides?.lifecycleCounts ?? {
            publishedCount: items.filter((item) => item.publishStatus === "PUBLISHED").length,
            failedCount: items.filter((item) => item.publishStatus === "FAILED").length,
            pendingCount: items.filter((item) => item.publishStatus === "PENDING").length,
            stalePendingCount: items.filter((item) => item.stalePending).length,
            retryableFailedCount: items.filter((item) => item.publishStatus === "FAILED" && item.retrySupported).length,
            nonRetryableFailedCount: items.filter((item) => item.publishStatus === "FAILED" && !item.retrySupported).length,
        },
    };
}

function formatExpectedTimestamp(value: string): string {
    return new Intl.DateTimeFormat(undefined, {
        year: "numeric",
        month: "short",
        day: "numeric",
        hour: "numeric",
        minute: "2-digit"
    }).format(new Date(value));
}

async function fillRequiredFormFields() {
    await userEvent.type(screen.getByLabelText(/User Name/i), "testUser");
    await userEvent.type(screen.getByLabelText(/Password/i), "testPass");
    await userEvent.type(screen.getByLabelText(/^Host$/i), "localhost");
    await userEvent.type(screen.getByLabelText(/VPN Name/i), "testVPN");
    await userEvent.type(screen.getByLabelText(/^Inner Message Id$/i), "001");
    await userEvent.type(screen.getByLabelText(/^Destination$/i), "solace/java/direct/system-01");
    await userEvent.type(screen.getByLabelText(/^Delivery Mode$/i), "PERSISTENT");
    await userEvent.clear(screen.getByLabelText(/^Priority$/i));
    await userEvent.type(screen.getByLabelText(/^Priority$/i), "3");
    await userEvent.type(screen.getByLabelText(/^Payload Type$/i), "binary");
    await userEvent.type(screen.getByLabelText(/^Payload Content$/i), "01001000 01100101 01101100");
}

describe("Form Submission Tests", () => {

    test("Submits form and handles API response", async () => {
        mockedAxios.post.mockResolvedValue({
            data: buildPublishSuccessResponse(),
            status: 201,
            statusText: "Created",
            headers: new AxiosHeaders(),
            config: {headers: new AxiosHeaders()},
        });

        render(<App/>);

        await fillRequiredFormFields();

        await userEvent.click(screen.getByRole("button", {name: /publish message/i}));

        await waitFor(() => expect(mockedAxios.post).toHaveBeenCalledTimes(1));

        expect(mockedAxios.post).toHaveBeenCalledWith(
            "http://localhost:8081/api/v1/messages/message",
            {
                userName: "testUser",
                password: "testPass",
                host: "localhost",
                vpnName: "testVPN",
                message: {
                    innerMessageId: "001",
                    destination: "solace/java/direct/system-01",
                    deliveryMode: "PERSISTENT",
                    priority: 3,
                    payload: {
                        type: "binary",
                        content: "01001000 01100101 01101100",
                    },
                },
            },
            {
                headers: {"Content-Type": "application/json"},
            }
        );

        expect(await screen.findByRole("alert")).toHaveTextContent("Message published successfully.");
        expect(screen.getByText(/Status: 201/i)).toBeInTheDocument();
        expect(screen.getByText(/"content": "01001000 01100101 01101100"/i)).toBeInTheDocument();
    });

    test("Submits form with message properties", async () => {
        mockedAxios.post.mockResolvedValue({
            data: buildPublishSuccessResponse(),
            status: 201,
            statusText: "Created",
            headers: new AxiosHeaders(),
            config: {headers: new AxiosHeaders()},
        });

        render(<App/>);

        await fillRequiredFormFields();
        await userEvent.type(screen.getByLabelText(/Property Key 1/i), "region");
        await userEvent.type(screen.getByLabelText(/Property Value 1/i), "ca-east");
        await userEvent.click(screen.getByRole("button", {name: /add property/i}));
        await userEvent.type(screen.getByLabelText(/Property Key 2/i), "source");
        await userEvent.type(screen.getByLabelText(/Property Value 2/i), "publisher-ui");

        await userEvent.click(screen.getByRole("button", {name: /publish message/i}));

        await waitFor(() => expect(mockedAxios.post).toHaveBeenCalledTimes(1));

        expect(mockedAxios.post).toHaveBeenCalledWith(
            "http://localhost:8081/api/v1/messages/message",
            {
                userName: "testUser",
                password: "testPass",
                host: "localhost",
                vpnName: "testVPN",
                message: {
                    innerMessageId: "001",
                    destination: "solace/java/direct/system-01",
                    deliveryMode: "PERSISTENT",
                    priority: 3,
                    payload: {
                        type: "binary",
                        content: "01001000 01100101 01101100",
                    },
                    properties: {
                        region: "ca-east",
                        source: "publisher-ui",
                    },
                },
            },
            {
                headers: {"Content-Type": "application/json"},
            }
        );
    });

    test("Handles typed validation failure returned by the backend", async () => {
        mockedAxios.post.mockRejectedValue({
            response: {
                data: buildValidationErrorResponse(),
                status: 400,
                statusText: "Bad Request",
                headers: new AxiosHeaders(),
                config: {headers: new AxiosHeaders()},
            },
            isAxiosError: true,
        });

        render(<App/>);

        await fillRequiredFormFields();

        await userEvent.click(screen.getByRole("button", {name: /publish message/i}));

        await waitFor(() => expect(mockedAxios.post).toHaveBeenCalledTimes(1));
        expect(await screen.findByRole("alert")).toHaveTextContent("Request validation failed");
        expect(screen.getByText(/message\.innermessageid is required/i)).toBeInTheDocument();
        expect(screen.getByText(/Status: 400/i)).toBeInTheDocument();
    });

    test("Handles missing required message fields before calling the api", async () => {
        render(<App/>);

        await userEvent.type(screen.getByLabelText(/User Name/i), "testUser");
        await userEvent.type(screen.getByLabelText(/Password/i), "testPass");
        await userEvent.type(screen.getByLabelText(/^Host$/i), "localhost");
        await userEvent.type(screen.getByLabelText(/VPN Name/i), "testVPN");
        await userEvent.type(screen.getByLabelText(/^Inner Message Id$/i), " ");
        await userEvent.type(screen.getByLabelText(/^Destination$/i), "solace/java/direct/system-01");
        await userEvent.type(screen.getByLabelText(/^Delivery Mode$/i), " ");
        await userEvent.clear(screen.getByLabelText(/^Priority$/i));
        await userEvent.type(screen.getByLabelText(/^Priority$/i), "0");
        await userEvent.type(screen.getByLabelText(/^Payload Type$/i), " ");
        await userEvent.type(screen.getByLabelText(/^Payload Content$/i), "01001000 01100101 01101100");

        await userEvent.click(screen.getByRole("button", {name: /publish message/i}));

        expect(mockedAxios.post).not.toHaveBeenCalled();
        expect(await screen.findByRole("alert")).toHaveTextContent("Request validation failed");
        expect(screen.getByText(/message\.innermessageid is required/i)).toBeInTheDocument();
        expect(screen.getByText(/message\.deliverymode is required/i)).toBeInTheDocument();
        expect(screen.getByText(/payload\.type is required/i)).toBeInTheDocument();
        expect(screen.getByText(/Status: 400/i)).toBeInTheDocument();
    });

    test("Handles incomplete message property rows before calling the api", async () => {
        render(<App/>);

        await fillRequiredFormFields();
        await userEvent.type(screen.getByLabelText(/Property Key 1/i), "region");

        await userEvent.click(screen.getByRole("button", {name: /publish message/i}));

        expect(mockedAxios.post).not.toHaveBeenCalled();
        expect(await screen.findByRole("alert")).toHaveTextContent("Request validation failed");
        expect(screen.getByText(/message\.properties\[0\]\.value is required/i)).toBeInTheDocument();
        expect(screen.getByText(/Status: 400/i)).toBeInTheDocument();
    });
});

describe("Broker API Contract Tests", () => {
    test("renders the typed publish success dto returned by the broker api", async () => {
        mockedAxios.post.mockResolvedValue({
            data: buildPublishSuccessResponse({
                destination: "solace/java/direct/system-42",
                content: "payload from typed dto",
            }),
            status: 201,
            statusText: "Created",
            headers: new AxiosHeaders(),
            config: {headers: new AxiosHeaders()},
        });

        render(<App/>);

        await fillRequiredFormFields();
        await userEvent.click(screen.getByRole("button", {name: /publish message/i}));

        await waitFor(() => expect(mockedAxios.post).toHaveBeenCalledTimes(1));
        expect(await screen.findByRole("alert")).toHaveTextContent("Message published successfully.");
        expect(screen.getByText(/"destination": "solace\/java\/direct\/system-42"/i)).toBeInTheDocument();
        expect(screen.getByText(/"content": "payload from typed dto"/i)).toBeInTheDocument();
    });

    test("renders the typed validation error dto returned by the broker api", async () => {
        mockedAxios.post.mockRejectedValue({
            response: {
                data: buildValidationErrorResponse({
                    validationErrors: {
                        "message.destination": "message.destination is required",
                        "message.payload.content": "payload.content is required",
                    },
                }),
                status: 400,
                statusText: "Bad Request",
                headers: new AxiosHeaders(),
                config: {headers: new AxiosHeaders()},
            },
            isAxiosError: true,
        });

        render(<App/>);

        await fillRequiredFormFields();
        await userEvent.click(screen.getByRole("button", {name: /publish message/i}));

        await waitFor(() => expect(mockedAxios.post).toHaveBeenCalledTimes(1));
        expect(await screen.findByRole("alert")).toHaveTextContent("Request validation failed");
        expect(screen.getByText(/message\.destination is required/i)).toBeInTheDocument();
        expect(screen.getByText(/payload\.content is required/i)).toBeInTheDocument();
    });

    test("renders the normalized stored message read dto from the broker api", async () => {
        mockedAxios.get.mockResolvedValue({
            data: buildMessagesPage(0, {
                totalElements: 1,
                totalPages: 1,
                last: true,
                items: [
                    buildStoredMessage({
                        properties: {
                            region: "ca-east",
                            source: "broker-api-contract",
                        },
                    }),
                ],
            }),
            status: 200,
            statusText: "OK",
            headers: new AxiosHeaders(),
            config: {headers: new AxiosHeaders()},
        });

        render(<App/>);

        await userEvent.click(screen.getByRole("button", {name: /load messages/i}));
        await waitFor(() => expect(mockedAxios.get).toHaveBeenCalledTimes(1));
        await userEvent.click(screen.getByRole("button", {name: /show details/i}));

        expect(screen.getByText(/region: ca-east/i)).toBeInTheDocument();
        expect(screen.getByText(/source: broker-api-contract/i)).toBeInTheDocument();
    });
});

describe("Stored Messages Browser", () => {
    test("Shows loading feedback while stored messages are being fetched", async () => {
        let resolveRequest: ((value: unknown) => void) | undefined;
        mockedAxios.get.mockImplementation(
            () =>
                new Promise((resolve) => {
                    resolveRequest = resolve;
                })
        );

        render(<App/>);

        await userEvent.click(screen.getByRole("button", {name: /load messages/i}));

        expect(await screen.findByRole("status")).toHaveTextContent("Loading stored messages...");

        resolveRequest?.({
            data: buildMessagesPage(0, {
                totalElements: 1,
                totalPages: 1,
                last: true,
            }),
            status: 200,
            statusText: "OK",
            headers: new AxiosHeaders(),
            config: {headers: new AxiosHeaders()},
        });

        await waitFor(() => expect(screen.queryByRole("status")).not.toBeInTheDocument());
    });

    test("Loads and renders paginated stored messages", async () => {
        const createdAt = "2026-04-20T21:30:00Z";
        mockedAxios.get.mockResolvedValue({
            data: buildMessagesPage(0, {
                totalElements: 1,
                totalPages: 1,
                last: true,
                items: [
                    buildStoredMessage({
                        createdAt,
                        updatedAt: null,
                        publishedAt: createdAt,
                    }),
                ],
            }),
            status: 200,
            statusText: "OK",
            headers: new AxiosHeaders(),
            config: {headers: new AxiosHeaders()},
        });

        render(<App/>);

        await userEvent.click(screen.getByRole("button", {name: /load messages/i}));

        await waitFor(() => expect(mockedAxios.get).toHaveBeenCalledTimes(1));
        expect(await screen.findByText(/Loaded 1 messages\./i)).toBeInTheDocument();
        expect(screen.getByText(/Page 1 of 1/i)).toBeInTheDocument();
        expect(screen.getByText(/solace\/java\/direct\/system-01/i)).toBeInTheDocument();
        expect(screen.getByText(/^binary$/i)).toBeInTheDocument();
        expect(screen.getAllByText(formatExpectedTimestamp(createdAt)).length).toBeGreaterThanOrEqual(1);
        expect(screen.getByText(/^PUBLISHED$/i, {selector: ".badge"})).toBeInTheDocument();
        expect(screen.getByRole("button", {name: /show details/i})).toBeInTheDocument();
        expect(screen.queryByText(/region: ca-east/i)).not.toBeInTheDocument();
    });

    test("Builds the expected filter and sort query parameters", async () => {
        mockedAxios.get.mockResolvedValue({
            data: buildMessagesPage(2, {
                size: 10,
                totalPages: 3,
                first: false,
            }),
            status: 200,
            statusText: "OK",
            headers: new AxiosHeaders(),
            config: {headers: new AxiosHeaders()},
        });

        render(<App/>);

        await userEvent.type(screen.getByLabelText(/Filter Destination/i), "system-02");
        await userEvent.type(screen.getByLabelText(/Filter Delivery Mode/i), "DIRECT");
        await userEvent.type(screen.getByLabelText(/Filter Inner Message Id/i), "002");
        await userEvent.selectOptions(screen.getByLabelText(/Filter Publish Status/i), "PENDING");
        await userEvent.click(screen.getByLabelText(/only stale pending/i));
        await userEvent.type(screen.getByLabelText(/Created At From/i), "2026-04-20T09:30");
        await userEvent.type(screen.getByLabelText(/Created At To/i), "2026-04-20T18:45");
        await userEvent.type(screen.getByLabelText(/Published At From/i), "2026-04-21T07:00");
        await userEvent.type(screen.getByLabelText(/Published At To/i), "2026-04-21T08:15");
        await userEvent.selectOptions(screen.getByLabelText(/Sort By/i), "priority");
        await userEvent.selectOptions(screen.getByLabelText(/Sort Direction/i), "asc");
        await userEvent.clear(screen.getByLabelText(/^Page$/i));
        await userEvent.type(screen.getByLabelText(/^Page$/i), "2");
        await userEvent.clear(screen.getByLabelText(/^Size$/i));
        await userEvent.type(screen.getByLabelText(/^Size$/i), "10");

        await userEvent.click(screen.getByRole("button", {name: /load messages/i}));

        await waitFor(() => expect(mockedAxios.get).toHaveBeenCalledTimes(1));
        expect(mockedAxios.get).toHaveBeenCalledWith(
            "http://localhost:8081/api/v1/messages/all",
            {
                params: {
                    page: 2,
                    size: 10,
                    destination: "system-02",
                    deliveryMode: "DIRECT",
                    innerMessageId: "002",
                    publishStatus: "PENDING",
                    stalePendingOnly: true,
                    createdAtFrom: "2026-04-20T09:30:00",
                    createdAtTo: "2026-04-20T18:45:00",
                    publishedAtFrom: "2026-04-21T07:00:00",
                    publishedAtTo: "2026-04-21T08:15:00",
                    sortBy: "priority",
                    sortDirection: "asc",
                },
            }
        );
    });

    test("Copies the current filter query url with active parameters only", async () => {
        render(<App/>);

        await userEvent.type(screen.getByLabelText(/Filter Destination/i), "system-02");
        await userEvent.type(screen.getByLabelText(/Filter Delivery Mode/i), "DIRECT");
        await userEvent.type(screen.getByLabelText(/Filter Inner Message Id/i), "002");
        await userEvent.selectOptions(screen.getByLabelText(/Filter Publish Status/i), "PENDING");
        await userEvent.click(screen.getByLabelText(/only stale pending/i));
        await userEvent.type(screen.getByLabelText(/Created At From/i), "2026-04-20T09:30");
        await userEvent.type(screen.getByLabelText(/Created At To/i), "2026-04-20T18:45");
        await userEvent.type(screen.getByLabelText(/Published At From/i), "2026-04-21T07:00");
        await userEvent.type(screen.getByLabelText(/Published At To/i), "2026-04-21T08:15");
        await userEvent.selectOptions(screen.getByLabelText(/Sort By/i), "priority");
        await userEvent.selectOptions(screen.getByLabelText(/Sort Direction/i), "asc");
        await userEvent.clear(screen.getByLabelText(/^Page$/i));
        await userEvent.type(screen.getByLabelText(/^Page$/i), "2");
        await userEvent.clear(screen.getByLabelText(/^Size$/i));
        await userEvent.type(screen.getByLabelText(/^Size$/i), "10");

        await userEvent.click(screen.getByRole("button", {name: /copy filter query/i}));

        expect(writeTextMock).toHaveBeenCalledWith(
            "http://localhost:8081/api/v1/messages/all?page=2&size=10&destination=system-02&deliveryMode=DIRECT&innerMessageId=002&publishStatus=PENDING&stalePendingOnly=true&createdAtFrom=2026-04-20T09%3A30%3A00&createdAtTo=2026-04-20T18%3A45%3A00&publishedAtFrom=2026-04-21T07%3A00%3A00&publishedAtTo=2026-04-21T08%3A15%3A00&sortBy=priority&sortDirection=asc"
        );
        expect(await screen.findByRole("status")).toHaveTextContent("Current filter query copied.");
    });

    test("Loads the next page when pagination advances", async () => {
        mockedAxios.get
            .mockResolvedValueOnce({
                data: buildMessagesPage(0, {
                    first: true,
                    last: false,
                }),
                status: 200,
                statusText: "OK",
                headers: new AxiosHeaders(),
                config: {headers: new AxiosHeaders()},
            })
            .mockResolvedValueOnce({
                data: buildMessagesPage(1, {
                    items: [
                        buildStoredMessage({
                            id: 2,
                            innerMessageId: "002",
                            destination: "solace/java/direct/system-02",
                            deliveryMode: "DIRECT",
                            priority: 1,
                            publishStatus: "FAILED",
                            failureReason: "Failed to publish message to Solace broker",
                            publishedAt: null,
                            payload: {
                                type: "text",
                                content: "hello world",
                            },
                            properties: {},
                        }),
                    ],
                    first: false,
                    last: true,
                }),
                status: 200,
                statusText: "OK",
                headers: new AxiosHeaders(),
                config: {headers: new AxiosHeaders()},
            });

        render(<App/>);

        await userEvent.click(screen.getByRole("button", {name: /load messages/i}));
        await waitFor(() => expect(mockedAxios.get).toHaveBeenCalledTimes(1));

        await userEvent.click(screen.getByRole("button", {name: /next page/i}));

        await waitFor(() => expect(mockedAxios.get).toHaveBeenCalledTimes(2));
        expect(mockedAxios.get).toHaveBeenLastCalledWith(
            "http://localhost:8081/api/v1/messages/all",
            {
                params: {
                    page: 1,
                    size: 20,
                    sortBy: "createdAt",
                    sortDirection: "desc",
                },
            }
        );
        expect(await screen.findByText(/002/i)).toBeInTheDocument();
        expect(screen.getByText(/Page 2 of 2/i)).toBeInTheDocument();
    });

    test("Expands and collapses stored message details", async () => {
        mockedAxios.get.mockResolvedValue({
            data: buildMessagesPage(0, {
                totalElements: 1,
                totalPages: 1,
                last: true,
            }),
            status: 200,
            statusText: "OK",
            headers: new AxiosHeaders(),
            config: {headers: new AxiosHeaders()},
        });

        render(<App/>);

        await userEvent.click(screen.getByRole("button", {name: /load messages/i}));
        await waitFor(() => expect(mockedAxios.get).toHaveBeenCalledTimes(1));

        const toggleButton = screen.getByRole("button", {name: /show details/i});
        expect(toggleButton).toHaveAttribute("aria-expanded", "false");
        expect(screen.queryByText(/01001000 01100101 01101100/i)).not.toBeInTheDocument();

        await userEvent.click(toggleButton);

        expect(screen.getByRole("button", {name: /hide details/i})).toHaveAttribute("aria-expanded", "true");
        expect(screen.getByText(/01001000 01100101 01101100/i)).toBeInTheDocument();
        expect(screen.getByText(/^publish status$/i, {selector: ".meta-label"})).toBeInTheDocument();
        expect(screen.getByText(/region: ca-east/i)).toBeInTheDocument();

        await userEvent.click(screen.getByRole("button", {name: /hide details/i}));

        expect(screen.getByRole("button", {name: /show details/i})).toHaveAttribute("aria-expanded", "false");
        expect(screen.queryByText(/region: ca-east/i)).not.toBeInTheDocument();
    });

    test("Shows a fallback when message timestamps are not available", async () => {
        mockedAxios.get.mockResolvedValue({
            data: buildMessagesPage(0, {
                totalElements: 1,
                totalPages: 1,
                last: true,
                items: [
                    buildStoredMessage({
                        createdAt: null,
                        updatedAt: null,
                        publishedAt: null,
                        properties: {},
                    }),
                ],
            }),
            status: 200,
            statusText: "OK",
            headers: new AxiosHeaders(),
            config: {headers: new AxiosHeaders()},
        });

        render(<App/>);

        await userEvent.click(screen.getByRole("button", {name: /load messages/i}));
        await waitFor(() => expect(mockedAxios.get).toHaveBeenCalledTimes(1));
        expect(screen.getAllByText(/not available/i).length).toBeGreaterThanOrEqual(1);

        await userEvent.click(screen.getByRole("button", {name: /show details/i}));
        expect(screen.getAllByText(/not available/i).length).toBeGreaterThanOrEqual(3);
    });

    test("Shows failure lifecycle details for failed publish attempts", async () => {
        mockedAxios.get.mockResolvedValue({
            data: buildMessagesPage(0, {
                totalElements: 1,
                totalPages: 1,
                last: true,
                items: [
                    buildStoredMessage({
                        publishStatus: "FAILED",
                        failureReason: "Failed to publish message to Solace broker",
                        publishedAt: null,
                    }),
                ],
            }),
            status: 200,
            statusText: "OK",
            headers: new AxiosHeaders(),
            config: {headers: new AxiosHeaders()},
        });

        render(<App/>);

        await userEvent.click(screen.getByRole("button", {name: /load messages/i}));
        await waitFor(() => expect(mockedAxios.get).toHaveBeenCalledTimes(1));
        expect(screen.getByText(/^FAILED$/i, {selector: ".badge"})).toBeInTheDocument();

        await userEvent.click(screen.getByRole("button", {name: /show details/i}));
        expect(screen.getByText(/Failed to publish message to Solace broker/i)).toBeInTheDocument();
        expect(screen.getByText(/^publish status$/i, {selector: ".meta-label"})).toBeInTheDocument();
    });

    test("Hides retry actions for failed messages that are not retryable under the current policy", async () => {
        mockedAxios.get.mockResolvedValue({
            data: buildMessagesPage(0, {
                totalElements: 1,
                totalPages: 1,
                last: true,
                items: [
                    buildStoredMessage({
                        publishStatus: "FAILED",
                        failureReason: "Failed to publish message to Solace broker",
                        publishedAt: null,
                        retrySupported: false,
                        retryBlockedReason: "Retries are supported only for messages published with server-side broker configuration.",
                    }),
                ],
            }),
            status: 200,
            statusText: "OK",
            headers: new AxiosHeaders(),
            config: {headers: new AxiosHeaders()},
        });

        render(<App/>);

        await userEvent.click(screen.getByRole("button", {name: /load messages/i}));
        await waitFor(() => expect(mockedAxios.get).toHaveBeenCalledTimes(1));
        expect(screen.queryByRole("button", {name: /retry failed message/i})).not.toBeInTheDocument();
        expect(screen.queryByRole("button", {name: /retry visible failed messages/i})).not.toBeInTheDocument();

        await userEvent.click(screen.getByRole("button", {name: /show details/i}));
        expect(screen.getByText(/Retries are supported only for messages published with server-side broker configuration\./i)).toBeInTheDocument();
    });

    test("Highlights stale pending messages in the browser", async () => {
        mockedAxios.get.mockResolvedValue({
            data: buildMessagesPage(0, {
                totalElements: 1,
                totalPages: 1,
                last: true,
                items: [
                    buildStoredMessage({
                        publishStatus: "PENDING",
                        stalePending: true,
                        publishedAt: null,
                        createdAt: "2026-04-21T07:30:00Z",
                    }),
                ],
            }),
            status: 200,
            statusText: "OK",
            headers: new AxiosHeaders(),
            config: {headers: new AxiosHeaders()},
        });

        render(<App/>);

        await userEvent.click(screen.getByRole("button", {name: /load messages/i}));
        await waitFor(() => expect(mockedAxios.get).toHaveBeenCalledTimes(1));
        expect(screen.getByText(/^stale pending$/i, {selector: ".badge"})).toBeInTheDocument();

        await userEvent.click(screen.getByRole("button", {name: /show details/i}));
        expect(screen.getByText("This pending message is older than the stale threshold and may need review.")).toBeInTheDocument();
    });

    test("Shows page-level lifecycle counts for the current browser results", async () => {
        mockedAxios.get.mockResolvedValue({
            data: buildMessagesPage(0, {
                totalElements: 4,
                totalPages: 1,
                last: true,
                lifecycleCounts: {
                    publishedCount: 10,
                    failedCount: 5,
                    pendingCount: 3,
                    stalePendingCount: 2,
                    retryableFailedCount: 4,
                    nonRetryableFailedCount: 1,
                },
                items: [
                    buildStoredMessage({
                        id: 1,
                        publishStatus: "PUBLISHED",
                        stalePending: false,
                    }),
                    buildStoredMessage({
                        id: 2,
                        publishStatus: "FAILED",
                        publishedAt: null,
                        stalePending: false,
                    }),
                    buildStoredMessage({
                        id: 3,
                        publishStatus: "PENDING",
                        publishedAt: null,
                        stalePending: false,
                    }),
                    buildStoredMessage({
                        id: 4,
                        publishStatus: "PENDING",
                        publishedAt: null,
                        stalePending: true,
                    }),
                ],
            }),
            status: 200,
            statusText: "OK",
            headers: new AxiosHeaders(),
            config: {headers: new AxiosHeaders()},
        });

        render(<App/>);

        await userEvent.click(screen.getByRole("button", {name: /load messages/i}));
        await waitFor(() => expect(mockedAxios.get).toHaveBeenCalledTimes(1));
        expect(screen.getByText(/filtered totals cover the full matching result set/i)).toBeInTheDocument();
        expect(screen.getByText(/retryable failed messages can be retried under the current server-side policy/i)).toBeInTheDocument();

        const aggregateSummary = screen.getByTestId("browser-lifecycle-summary-aggregate");
        const filteredPublishedLabel = within(aggregateSummary).getByText(/^filtered published$/i, {selector: ".meta-label"});
        const filteredFailedLabel = within(aggregateSummary).getByText(/^filtered failed$/i, {selector: ".meta-label"});
        const filteredPendingLabel = within(aggregateSummary).getByText(/^filtered pending$/i, {selector: ".meta-label"});
        const filteredStalePendingLabel = within(aggregateSummary).getByText(/^filtered stale pending$/i, {selector: ".meta-label"});
        const retryableFailedLabel = within(aggregateSummary).getByText(/^retryable failed$/i, {selector: ".meta-label"});
        const nonRetryableFailedLabel = within(aggregateSummary).getByText(/^non-retryable failed$/i, {selector: ".meta-label"});

        expect(filteredPublishedLabel.nextElementSibling).toHaveTextContent("10");
        expect(filteredFailedLabel.nextElementSibling).toHaveTextContent("5");
        expect(filteredPendingLabel.nextElementSibling).toHaveTextContent("3");
        expect(filteredStalePendingLabel.nextElementSibling).toHaveTextContent("2");
        expect(retryableFailedLabel.nextElementSibling).toHaveTextContent("4");
        expect(nonRetryableFailedLabel.nextElementSibling).toHaveTextContent("1");

        const lifecycleSummary = screen.getByTestId("browser-lifecycle-summary");
        const publishedLabel = within(lifecycleSummary).getByText(/^published$/i, {selector: ".meta-label"});
        const failedLabel = within(lifecycleSummary).getByText(/^failed$/i, {selector: ".meta-label"});
        const pendingLabel = within(lifecycleSummary).getByText(/^pending$/i, {selector: ".meta-label"});
        const stalePendingLabel = within(lifecycleSummary).getByText(/^stale pending$/i, {selector: ".meta-label"});

        expect(publishedLabel.nextElementSibling).toHaveTextContent("1");
        expect(failedLabel.nextElementSibling).toHaveTextContent("1");
        expect(pendingLabel.nextElementSibling).toHaveTextContent("2");
        expect(stalePendingLabel.nextElementSibling).toHaveTextContent("1");
    });

    test("Applies the lifecycle quick filter and reloads browser results", async () => {
        mockedAxios.get
            .mockResolvedValueOnce({
                data: buildMessagesPage(0, {
                    totalElements: 3,
                    totalPages: 1,
                    last: true,
                    items: [
                        buildStoredMessage({id: 1, publishStatus: "PUBLISHED"}),
                        buildStoredMessage({id: 2, publishStatus: "FAILED", publishedAt: null}),
                        buildStoredMessage({id: 3, publishStatus: "PENDING", publishedAt: null}),
                    ],
                }),
                status: 200,
                statusText: "OK",
                headers: new AxiosHeaders(),
                config: {headers: new AxiosHeaders()},
            })
            .mockResolvedValueOnce({
                data: buildMessagesPage(0, {
                    totalElements: 1,
                    totalPages: 1,
                    last: true,
                    items: [
                        buildStoredMessage({id: 2, publishStatus: "FAILED", publishedAt: null}),
                    ],
                }),
                status: 200,
                statusText: "OK",
                headers: new AxiosHeaders(),
                config: {headers: new AxiosHeaders()},
            });

        render(<App/>);

        await userEvent.click(screen.getByRole("button", {name: /load messages/i}));
        await waitFor(() => expect(mockedAxios.get).toHaveBeenCalledTimes(1));

        const lifecycleSummary = screen.getByTestId("browser-lifecycle-summary");
        const failedQuickFilter = within(lifecycleSummary).getByText(/^failed$/i, {selector: ".meta-label"}).closest("button");
        expect(failedQuickFilter).not.toBeNull();

        await userEvent.click(failedQuickFilter!);

        await waitFor(() => expect(mockedAxios.get).toHaveBeenCalledTimes(2));
        expect(mockedAxios.get).toHaveBeenLastCalledWith(
            "http://localhost:8081/api/v1/messages/all",
            expect.objectContaining({
                params: expect.objectContaining({
                    page: 0,
                    publishStatus: "FAILED",
                    sortBy: "createdAt",
                    sortDirection: "desc",
                }),
            }),
        );
        expect(screen.getByLabelText(/filter publish status/i)).toHaveValue("FAILED");
        expect(screen.getByLabelText(/only stale pending/i)).not.toBeChecked();
    });

    test("Applies the stale pending quick filter and reloads browser results", async () => {
        mockedAxios.get
            .mockResolvedValueOnce({
                data: buildMessagesPage(0, {
                    totalElements: 2,
                    totalPages: 1,
                    last: true,
                    items: [
                        buildStoredMessage({id: 3, publishStatus: "PENDING", publishedAt: null, stalePending: true}),
                        buildStoredMessage({id: 4, publishStatus: "PENDING", publishedAt: null, stalePending: false}),
                    ],
                }),
                status: 200,
                statusText: "OK",
                headers: new AxiosHeaders(),
                config: {headers: new AxiosHeaders()},
            })
            .mockResolvedValueOnce({
                data: buildMessagesPage(0, {
                    totalElements: 1,
                    totalPages: 1,
                    last: true,
                    items: [
                        buildStoredMessage({id: 3, publishStatus: "PENDING", publishedAt: null, stalePending: true}),
                    ],
                }),
                status: 200,
                statusText: "OK",
                headers: new AxiosHeaders(),
                config: {headers: new AxiosHeaders()},
            });

        render(<App/>);

        await userEvent.click(screen.getByRole("button", {name: /load messages/i}));
        await waitFor(() => expect(mockedAxios.get).toHaveBeenCalledTimes(1));

        const lifecycleSummary = screen.getByTestId("browser-lifecycle-summary");
        const stalePendingQuickFilter = within(lifecycleSummary).getByText(/^stale pending$/i, {selector: ".meta-label"}).closest("button");
        expect(stalePendingQuickFilter).not.toBeNull();

        await userEvent.click(stalePendingQuickFilter!);

        await waitFor(() => expect(mockedAxios.get).toHaveBeenCalledTimes(2));
        expect(mockedAxios.get).toHaveBeenLastCalledWith(
            "http://localhost:8081/api/v1/messages/all",
            expect.objectContaining({
                params: expect.objectContaining({
                    page: 0,
                    publishStatus: "PENDING",
                    stalePendingOnly: true,
                    sortBy: "createdAt",
                    sortDirection: "desc",
                }),
            }),
        );
        expect(screen.getByLabelText(/filter publish status/i)).toHaveValue("PENDING");
        expect(screen.getByLabelText(/only stale pending/i)).toBeChecked();
    });

    test("Exports the currently loaded page as json", async () => {
        mockedAxios.get.mockResolvedValue({
            data: buildMessagesPage(0, {
                totalElements: 2,
                totalPages: 1,
                last: true,
                lifecycleCounts: {
                    publishedCount: 1,
                    failedCount: 1,
                    pendingCount: 0,
                    stalePendingCount: 0,
                    retryableFailedCount: 1,
                    nonRetryableFailedCount: 0,
                },
                items: [
                    buildStoredMessage({id: 1, publishStatus: "PUBLISHED"}),
                    buildStoredMessage({id: 2, publishStatus: "FAILED", publishedAt: null}),
                ],
            }),
            status: 200,
            statusText: "OK",
            headers: new AxiosHeaders(),
            config: {headers: new AxiosHeaders()},
        });

        render(<App/>);

        await userEvent.click(screen.getByRole("button", {name: /load messages/i}));
        await waitFor(() => expect(mockedAxios.get).toHaveBeenCalledTimes(1));

        await userEvent.click(screen.getByRole("button", {name: /export current page/i}));

        expect(createObjectUrlMock).toHaveBeenCalledTimes(1);
        const exportBlob = createObjectUrlMock.mock.calls[0][0] as MockBlob;
        const exportJson = JSON.parse(String(exportBlob.parts[0]));
        expect(exportJson.page).toBe(0);
        expect(exportJson.totalElements).toBe(2);
        expect(exportJson.lifecycleCounts.retryableFailedCount).toBe(1);
        expect(exportJson.items).toHaveLength(2);
        expect(anchorClickMock).toHaveBeenCalledTimes(1);
        expect(revokeObjectUrlMock).toHaveBeenCalledWith("blob:test-export");
        expect(screen.getByRole("alert")).toHaveTextContent("Exported 2 messages from the current page.");
    });

    test("Reconciles a stale pending message and refreshes the browser results", async () => {
        mockedAxios.get
            .mockResolvedValueOnce({
                data: buildMessagesPage(0, {
                    totalElements: 1,
                    totalPages: 1,
                    last: true,
                    items: [
                        buildStoredMessage({
                            id: 3,
                            innerMessageId: "003",
                            publishStatus: "PENDING",
                            stalePending: true,
                            publishedAt: null,
                            createdAt: "2026-04-21T07:30:00Z",
                        }),
                    ],
                }),
                status: 200,
                statusText: "OK",
                headers: new AxiosHeaders(),
                config: {headers: new AxiosHeaders()},
            })
            .mockResolvedValueOnce({
                data: buildMessagesPage(0, {
                    totalElements: 1,
                    totalPages: 1,
                    last: true,
                    items: [
                        buildStoredMessage({
                            id: 3,
                            innerMessageId: "003",
                            publishStatus: "FAILED",
                            stalePending: false,
                            failureReason: "Marked as FAILED after manual reconciliation of a stale PENDING message",
                            publishedAt: null,
                            createdAt: "2026-04-21T07:30:00Z",
                        }),
                    ],
                }),
                status: 200,
                statusText: "OK",
                headers: new AxiosHeaders(),
                config: {headers: new AxiosHeaders()},
            });
        mockedAxios.post.mockResolvedValue({
            data: buildStoredMessage({
                id: 3,
                innerMessageId: "003",
                publishStatus: "FAILED",
                stalePending: false,
                failureReason: "Marked as FAILED after manual reconciliation of a stale PENDING message",
            }),
            status: 200,
            statusText: "OK",
            headers: new AxiosHeaders(),
            config: {headers: new AxiosHeaders()},
        });

        render(<App/>);

        await userEvent.click(screen.getByRole("button", {name: /load messages/i}));
        await waitFor(() => expect(mockedAxios.get).toHaveBeenCalledTimes(1));

        await userEvent.click(screen.getByRole("button", {name: /mark stale pending as failed/i}));

        await waitFor(() => expect(mockedAxios.post).toHaveBeenCalledWith(
            "http://localhost:8081/api/v1/messages/3/reconcile-stale-pending"
        ));
        await waitFor(() => expect(mockedAxios.get).toHaveBeenCalledTimes(2));
        expect(screen.getByText(/Reconciled stale pending message 003 successfully\./i)).toBeInTheDocument();
    });

    test("Retries a failed stored message and refreshes the browser results", async () => {
        mockedAxios.get
            .mockResolvedValueOnce({
                data: buildMessagesPage(0, {
                    totalElements: 1,
                    totalPages: 1,
                    last: true,
                    items: [
                        buildStoredMessage({
                            id: 7,
                            publishStatus: "FAILED",
                            failureReason: "Failed to publish message to Solace broker",
                            publishedAt: null,
                        }),
                    ],
                }),
                status: 200,
                statusText: "OK",
                headers: new AxiosHeaders(),
                config: {headers: new AxiosHeaders()},
            })
            .mockResolvedValueOnce({
                data: buildMessagesPage(0, {
                    items: [],
                    totalElements: 0,
                    totalPages: 0,
                    last: true,
                }),
                status: 200,
                statusText: "OK",
                headers: new AxiosHeaders(),
                config: {headers: new AxiosHeaders()},
            });
        mockedAxios.post.mockResolvedValue({
            data: buildPublishSuccessResponse(),
            status: 200,
            statusText: "OK",
            headers: new AxiosHeaders(),
            config: {headers: new AxiosHeaders()},
        });

        render(<App/>);

        await userEvent.click(screen.getByRole("button", {name: /load messages/i}));
        await waitFor(() => expect(mockedAxios.get).toHaveBeenCalledTimes(1));

        await userEvent.click(screen.getByRole("button", {name: /retry failed message/i}));

        await waitFor(() => expect(mockedAxios.post).toHaveBeenCalledWith("http://localhost:8081/api/v1/messages/7/retry"));
        await waitFor(() => expect(mockedAxios.get).toHaveBeenCalledTimes(2));
        expect(await screen.findByRole("alert")).toHaveTextContent("Retried message 001 successfully.");
    });

    test("Retries all visible failed messages and refreshes the browser once", async () => {
        mockedAxios.get
            .mockResolvedValueOnce({
                data: buildMessagesPage(0, {
                    totalElements: 2,
                    totalPages: 1,
                    last: true,
                    items: [
                        buildStoredMessage({
                            id: 7,
                            innerMessageId: "007",
                            publishStatus: "FAILED",
                            failureReason: "Failed to publish message to Solace broker",
                            publishedAt: null,
                        }),
                        buildStoredMessage({
                            id: 8,
                            innerMessageId: "008",
                            publishStatus: "FAILED",
                            failureReason: "Failed to publish message to Solace broker",
                            publishedAt: null,
                        }),
                    ],
                }),
                status: 200,
                statusText: "OK",
                headers: new AxiosHeaders(),
                config: {headers: new AxiosHeaders()},
            })
            .mockResolvedValueOnce({
                data: buildMessagesPage(0, {
                    items: [],
                    totalElements: 0,
                    totalPages: 0,
                    last: true,
                }),
                status: 200,
                statusText: "OK",
                headers: new AxiosHeaders(),
                config: {headers: new AxiosHeaders()},
            });
        mockedAxios.post.mockResolvedValue({
            data: buildBulkRetryResponse(),
            status: 200,
            statusText: "OK",
            headers: new AxiosHeaders(),
            config: {headers: new AxiosHeaders()},
        });

        render(<App/>);

        await userEvent.click(screen.getByRole("button", {name: /load messages/i}));
        await waitFor(() => expect(mockedAxios.get).toHaveBeenCalledTimes(1));
        expect(screen.getByRole("button", {name: /retry visible failed messages/i})).toBeInTheDocument();

        await userEvent.click(screen.getByRole("button", {name: /retry visible failed messages/i}));

        await waitFor(() => expect(mockedAxios.post).toHaveBeenCalledTimes(1));
        expect(mockedAxios.post).toHaveBeenCalledWith("http://localhost:8081/api/v1/messages/retry", {
            messageIds: [7, 8],
        });
        await waitFor(() => expect(mockedAxios.get).toHaveBeenCalledTimes(2));
        expect(await screen.findByRole("alert")).toHaveTextContent("Bulk retry completed successfully. 2 retried, 0 failed, 0 skipped.");
        expect(screen.getByText(/Bulk Retry Results/i)).toBeInTheDocument();
        expect(screen.getByText(/message id 7/i)).toBeInTheDocument();
        expect(screen.getByText(/message id 8/i)).toBeInTheDocument();
    });

    test("Shows a mixed bulk retry summary when some visible retries fail", async () => {
        mockedAxios.get
            .mockResolvedValueOnce({
                data: buildMessagesPage(0, {
                    totalElements: 2,
                    totalPages: 1,
                    last: true,
                    items: [
                        buildStoredMessage({
                            id: 7,
                            innerMessageId: "007",
                            publishStatus: "FAILED",
                            failureReason: "Failed to publish message to Solace broker",
                            publishedAt: null,
                        }),
                        buildStoredMessage({
                            id: 8,
                            innerMessageId: "008",
                            publishStatus: "FAILED",
                            failureReason: "Failed to publish message to Solace broker",
                            publishedAt: null,
                        }),
                    ],
                }),
                status: 200,
                statusText: "OK",
                headers: new AxiosHeaders(),
                config: {headers: new AxiosHeaders()},
            })
            .mockResolvedValueOnce({
                data: buildMessagesPage(0, {
                    totalElements: 1,
                    totalPages: 1,
                    last: true,
                    items: [
                        buildStoredMessage({
                            id: 8,
                            innerMessageId: "008",
                            publishStatus: "FAILED",
                            failureReason: "Failed to publish message to Solace broker",
                            publishedAt: null,
                        }),
                    ],
                }),
                status: 200,
                statusText: "OK",
                headers: new AxiosHeaders(),
                config: {headers: new AxiosHeaders()},
            });
        mockedAxios.post
            .mockResolvedValueOnce({
                data: buildBulkRetryResponse({
                    totalRequested: 2,
                    retriedSuccessfully: 1,
                    failedToRetry: 1,
                    skipped: 0,
                    results: [
                        {
                            messageId: 7,
                            outcome: "RETRIED",
                            detail: "Message retried successfully",
                            publishStatus: "PUBLISHED",
                            response: buildPublishSuccessResponse({destination: "solace/java/direct/system-07"}),
                        },
                        {
                            messageId: 8,
                            outcome: "FAILED",
                            detail: "Failed to publish message to Solace broker",
                            publishStatus: "FAILED",
                            response: null,
                        },
                    ],
                }),
                status: 200,
                statusText: "OK",
                headers: new AxiosHeaders(),
                config: {headers: new AxiosHeaders()},
            });

        render(<App/>);

        await userEvent.click(screen.getByRole("button", {name: /load messages/i}));
        await waitFor(() => expect(mockedAxios.get).toHaveBeenCalledTimes(1));

        await userEvent.click(screen.getByRole("button", {name: /retry visible failed messages/i}));

        await waitFor(() => expect(mockedAxios.post).toHaveBeenCalledTimes(1));
        await waitFor(() => expect(mockedAxios.get).toHaveBeenCalledTimes(2));
        expect(await screen.findByRole("alert")).toHaveTextContent("Bulk retry completed with mixed results. 1 retried, 1 failed, 0 skipped.");
        expect(screen.getByText(/status: 200/i)).toBeInTheDocument();
        expect(screen.getByText(/Bulk Retry Results/i)).toBeInTheDocument();
        expect(screen.getByText(/message id 7/i)).toBeInTheDocument();
        expect(screen.getByText(/Message retried successfully/i)).toBeInTheDocument();
        expect(screen.getByText(/message id 8/i)).toBeInTheDocument();
        expect(screen.getByText(/Failed to publish message to Solace broker/i)).toBeInTheDocument();
    });

    test("Clears bulk retry results when browser filters are reset", async () => {
        mockedAxios.get
            .mockResolvedValueOnce({
                data: buildMessagesPage(0, {
                    totalElements: 2,
                    totalPages: 1,
                    last: true,
                    items: [
                        buildStoredMessage({
                            id: 7,
                            innerMessageId: "007",
                            publishStatus: "FAILED",
                            failureReason: "Failed to publish message to Solace broker",
                            publishedAt: null,
                        }),
                        buildStoredMessage({
                            id: 8,
                            innerMessageId: "008",
                            publishStatus: "FAILED",
                            failureReason: "Failed to publish message to Solace broker",
                            publishedAt: null,
                        }),
                    ],
                }),
                status: 200,
                statusText: "OK",
                headers: new AxiosHeaders(),
                config: {headers: new AxiosHeaders()},
            })
            .mockResolvedValueOnce({
                data: buildMessagesPage(0, {
                    items: [],
                    totalElements: 0,
                    totalPages: 0,
                    last: true,
                }),
                status: 200,
                statusText: "OK",
                headers: new AxiosHeaders(),
                config: {headers: new AxiosHeaders()},
            });
        mockedAxios.post.mockResolvedValue({
            data: buildBulkRetryResponse(),
            status: 200,
            statusText: "OK",
            headers: new AxiosHeaders(),
            config: {headers: new AxiosHeaders()},
        });

        render(<App/>);

        await userEvent.click(screen.getByRole("button", {name: /load messages/i}));
        await waitFor(() => expect(mockedAxios.get).toHaveBeenCalledTimes(1));

        await userEvent.click(screen.getByRole("button", {name: /retry visible failed messages/i}));

        await waitFor(() => expect(mockedAxios.post).toHaveBeenCalledTimes(1));
        await waitFor(() => expect(mockedAxios.get).toHaveBeenCalledTimes(2));
        expect(screen.getByText(/Bulk Retry Results/i)).toBeInTheDocument();

        await userEvent.click(screen.getByRole("button", {name: /reset filters/i}));

        expect(screen.queryByText(/Bulk Retry Results/i)).not.toBeInTheDocument();
    });

    test("Shows a retry error when retrying a failed stored message fails", async () => {
        mockedAxios.get.mockResolvedValue({
            data: buildMessagesPage(0, {
                totalElements: 1,
                totalPages: 1,
                last: true,
                items: [
                    buildStoredMessage({
                        id: 7,
                        publishStatus: "FAILED",
                        failureReason: "Failed to publish message to Solace broker",
                        publishedAt: null,
                    }),
                ],
            }),
            status: 200,
            statusText: "OK",
            headers: new AxiosHeaders(),
            config: {headers: new AxiosHeaders()},
        });
        mockedAxios.post.mockRejectedValue({
            response: {
                data: {
                    status: 502,
                    error: "Bad Gateway",
                    message: "Failed to publish message to Solace broker",
                    path: "/api/v1/messages/7/retry",
                    validationErrors: null,
                },
                status: 502,
                statusText: "Bad Gateway",
                headers: new AxiosHeaders(),
                config: {headers: new AxiosHeaders()},
            },
            isAxiosError: true,
        });

        render(<App/>);

        await userEvent.click(screen.getByRole("button", {name: /load messages/i}));
        await waitFor(() => expect(mockedAxios.get).toHaveBeenCalledTimes(1));

        await userEvent.click(screen.getByRole("button", {name: /retry failed message/i}));

        await waitFor(() => expect(mockedAxios.post).toHaveBeenCalledWith("http://localhost:8081/api/v1/messages/7/retry"));
        expect(await screen.findByRole("alert")).toHaveTextContent("Failed to publish message to Solace broker");
    });

    test("Shows an improved empty state when no stored messages match the filters", async () => {
        mockedAxios.get.mockResolvedValue({
            data: buildMessagesPage(0, {
                items: [],
                totalElements: 0,
                totalPages: 0,
                last: true,
            }),
            status: 200,
            statusText: "OK",
            headers: new AxiosHeaders(),
            config: {headers: new AxiosHeaders()},
        });

        render(<App/>);

        await userEvent.click(screen.getByRole("button", {name: /load messages/i}));

        await waitFor(() => expect(mockedAxios.get).toHaveBeenCalledTimes(1));
        expect(screen.getByText(/No stored messages matched these filters\./i)).toBeInTheDocument();
        expect(screen.getByText(/Adjust the filters or reset them/i)).toBeInTheDocument();
    });

    test("Shows a backend error when loading stored messages fails", async () => {
        mockedAxios.get.mockRejectedValue({
            response: {
                data: {
                    status: 400,
                    error: "Bad Request",
                    message: "sortBy must be one of createdAt, priority, destination, innerMessageId",
                    path: "/api/v1/messages/all",
                    validationErrors: null,
                },
                status: 400,
                statusText: "Bad Request",
                headers: new AxiosHeaders(),
                config: {headers: new AxiosHeaders()},
            },
            isAxiosError: true,
        });

        render(<App/>);

        await userEvent.click(screen.getByRole("button", {name: /load messages/i}));

        expect(await screen.findByRole("alert")).toHaveTextContent("sortBy must be one of createdAt, priority, destination, innerMessageId");
        expect(screen.getByText(/status: 400/i)).toBeInTheDocument();
    });

    test("Refreshes stored messages with the current query state", async () => {
        mockedAxios.get
            .mockResolvedValueOnce({
                data: buildMessagesPage(1, {
                    size: 15,
                    first: false,
                    last: false,
                }),
                status: 200,
                statusText: "OK",
                headers: new AxiosHeaders(),
                config: {headers: new AxiosHeaders()},
            })
            .mockResolvedValueOnce({
                data: buildMessagesPage(1, {
                    size: 15,
                    first: false,
                    last: false,
                }),
                status: 200,
                statusText: "OK",
                headers: new AxiosHeaders(),
                config: {headers: new AxiosHeaders()},
            });

        render(<App/>);

        await userEvent.type(screen.getByLabelText(/Filter Destination/i), "system-03");
        await userEvent.type(screen.getByLabelText(/Filter Delivery Mode/i), "PERSISTENT");
        await userEvent.type(screen.getByLabelText(/Filter Inner Message Id/i), "003");
        await userEvent.selectOptions(screen.getByLabelText(/Filter Publish Status/i), "FAILED");
        await userEvent.type(screen.getByLabelText(/Created At From/i), "2026-04-20T09:30");
        await userEvent.type(screen.getByLabelText(/Created At To/i), "2026-04-20T18:45");
        await userEvent.type(screen.getByLabelText(/Published At From/i), "2026-04-21T07:00");
        await userEvent.type(screen.getByLabelText(/Published At To/i), "2026-04-21T08:15");
        await userEvent.selectOptions(screen.getByLabelText(/Sort By/i), "priority");
        await userEvent.selectOptions(screen.getByLabelText(/Sort Direction/i), "asc");
        await userEvent.clear(screen.getByLabelText(/^Page$/i));
        await userEvent.type(screen.getByLabelText(/^Page$/i), "1");
        await userEvent.clear(screen.getByLabelText(/^Size$/i));
        await userEvent.type(screen.getByLabelText(/^Size$/i), "15");

        await userEvent.click(screen.getByRole("button", {name: /load messages/i}));
        await waitFor(() => expect(mockedAxios.get).toHaveBeenCalledTimes(1));

        await userEvent.click(screen.getByRole("button", {name: /refresh results/i}));
        await waitFor(() => expect(mockedAxios.get).toHaveBeenCalledTimes(2));

        expect(mockedAxios.get).toHaveBeenLastCalledWith(
            "http://localhost:8081/api/v1/messages/all",
            {
                params: {
                    page: 1,
                    size: 15,
                    destination: "system-03",
                    deliveryMode: "PERSISTENT",
                    innerMessageId: "003",
                    publishStatus: "FAILED",
                    createdAtFrom: "2026-04-20T09:30:00",
                    createdAtTo: "2026-04-20T18:45:00",
                    publishedAtFrom: "2026-04-21T07:00:00",
                    publishedAtTo: "2026-04-21T08:15:00",
                    sortBy: "priority",
                    sortDirection: "asc",
                },
            }
        );
    });

    test("Applies the failed today preset to the existing browser controls", async () => {
        mockedAxios.get.mockResolvedValue({
            data: buildMessagesPage(0, {
                totalElements: 0,
                totalPages: 0,
                items: [],
                last: true,
            }),
            status: 200,
            statusText: "OK",
            headers: new AxiosHeaders(),
            config: {headers: new AxiosHeaders()},
        });

        render(<App/>);

        await userEvent.click(screen.getByRole("button", {name: /failed today/i}));

        const createdAtFrom = screen.getByLabelText(/Created At From/i) as HTMLInputElement;
        const createdAtTo = screen.getByLabelText(/Created At To/i) as HTMLInputElement;

        expect(screen.getByLabelText(/Filter Publish Status/i)).toHaveValue("FAILED");
        expect(createdAtFrom.value).toMatch(/T00:00$/);
        expect(createdAtTo.value).toMatch(/T23:59$/);
        expect(screen.getByLabelText(/Published At From/i)).toHaveValue("");
        expect(screen.getByLabelText(/Published At To/i)).toHaveValue("");

        await userEvent.click(screen.getByRole("button", {name: /load messages/i}));
        await waitFor(() => expect(mockedAxios.get).toHaveBeenCalledTimes(1));

        expect(mockedAxios.get).toHaveBeenCalledWith(
            "http://localhost:8081/api/v1/messages/all",
            {
                params: {
                    page: 0,
                    size: 20,
                    publishStatus: "FAILED",
                    createdAtFrom: `${createdAtFrom.value}:00`,
                    createdAtTo: `${createdAtTo.value}:00`,
                    sortBy: "createdAt",
                    sortDirection: "desc",
                },
            }
        );
    });

    test("Applies the published today preset to published-at filters", async () => {
        render(<App/>);

        await userEvent.click(screen.getByRole("button", {name: /published today/i}));

        const publishedAtFrom = screen.getByLabelText(/Published At From/i) as HTMLInputElement;
        const publishedAtTo = screen.getByLabelText(/Published At To/i) as HTMLInputElement;

        expect(screen.getByLabelText(/Filter Publish Status/i)).toHaveValue("PUBLISHED");
        expect(screen.getByLabelText(/Created At From/i)).toHaveValue("");
        expect(screen.getByLabelText(/Created At To/i)).toHaveValue("");
        expect(publishedAtFrom.value).toMatch(/T00:00$/);
        expect(publishedAtTo.value).toMatch(/T23:59$/);
    });

    test("Applies the pending now preset and clears date ranges", async () => {
        render(<App/>);

        await userEvent.type(screen.getByLabelText(/Created At From/i), "2026-04-20T09:30");
        await userEvent.type(screen.getByLabelText(/Published At From/i), "2026-04-21T07:00");

        await userEvent.click(screen.getByRole("button", {name: /pending now/i}));

        expect(screen.getByLabelText(/Filter Publish Status/i)).toHaveValue("PENDING");
        expect(screen.getByLabelText(/Created At From/i)).toHaveValue("");
        expect(screen.getByLabelText(/Created At To/i)).toHaveValue("");
        expect(screen.getByLabelText(/Published At From/i)).toHaveValue("");
        expect(screen.getByLabelText(/Published At To/i)).toHaveValue("");
        expect(screen.getByLabelText(/^Page$/i)).toHaveValue(0);
    });

    test("Applies the failed last 24h preset and refresh preserves it", async () => {
        mockedAxios.get
            .mockResolvedValueOnce({
                data: buildMessagesPage(0, {
                    totalElements: 0,
                    totalPages: 0,
                    items: [],
                    last: true,
                }),
                status: 200,
                statusText: "OK",
                headers: new AxiosHeaders(),
                config: {headers: new AxiosHeaders()},
            })
            .mockResolvedValueOnce({
                data: buildMessagesPage(0, {
                    totalElements: 0,
                    totalPages: 0,
                    items: [],
                    last: true,
                }),
                status: 200,
                statusText: "OK",
                headers: new AxiosHeaders(),
                config: {headers: new AxiosHeaders()},
            });

        render(<App/>);

        await userEvent.click(screen.getByRole("button", {name: /failed last 24h/i}));
        const createdAtFrom = screen.getByLabelText(/Created At From/i) as HTMLInputElement;
        const createdAtTo = screen.getByLabelText(/Created At To/i) as HTMLInputElement;
        expect(screen.getByLabelText(/Filter Publish Status/i)).toHaveValue("FAILED");
        expect(createdAtFrom.value).not.toBe("");
        expect(createdAtTo.value).not.toBe("");

        await userEvent.click(screen.getByRole("button", {name: /load messages/i}));
        await waitFor(() => expect(mockedAxios.get).toHaveBeenCalledTimes(1));
        await userEvent.click(screen.getByRole("button", {name: /refresh results/i}));
        await waitFor(() => expect(mockedAxios.get).toHaveBeenCalledTimes(2));

        expect(mockedAxios.get).toHaveBeenLastCalledWith(
            "http://localhost:8081/api/v1/messages/all",
            {
                params: {
                    page: 0,
                    size: 20,
                    publishStatus: "FAILED",
                    createdAtFrom: `${createdAtFrom.value}:00`,
                    createdAtTo: `${createdAtTo.value}:00`,
                    sortBy: "createdAt",
                    sortDirection: "desc",
                },
            }
        );
    });

    test("Resets browser filters back to defaults", async () => {
        mockedAxios.get.mockResolvedValue({
            data: buildMessagesPage(0, {
                totalElements: 1,
                totalPages: 1,
                last: true,
            }),
            status: 200,
            statusText: "OK",
            headers: new AxiosHeaders(),
            config: {headers: new AxiosHeaders()},
        });

        render(<App/>);

        await userEvent.type(screen.getByLabelText(/Filter Destination/i), "system-03");
        await userEvent.type(screen.getByLabelText(/Filter Delivery Mode/i), "PERSISTENT");
        await userEvent.type(screen.getByLabelText(/Filter Inner Message Id/i), "003");
        await userEvent.selectOptions(screen.getByLabelText(/Filter Publish Status/i), "PUBLISHED");
        await userEvent.click(screen.getByLabelText(/only stale pending/i));
        await userEvent.type(screen.getByLabelText(/Created At From/i), "2026-04-20T09:30");
        await userEvent.type(screen.getByLabelText(/Created At To/i), "2026-04-20T18:45");
        await userEvent.type(screen.getByLabelText(/Published At From/i), "2026-04-21T07:00");
        await userEvent.type(screen.getByLabelText(/Published At To/i), "2026-04-21T08:15");
        await userEvent.selectOptions(screen.getByLabelText(/Sort By/i), "priority");
        await userEvent.selectOptions(screen.getByLabelText(/Sort Direction/i), "asc");
        await userEvent.clear(screen.getByLabelText(/^Page$/i));
        await userEvent.type(screen.getByLabelText(/^Page$/i), "2");
        await userEvent.clear(screen.getByLabelText(/^Size$/i));
        await userEvent.type(screen.getByLabelText(/^Size$/i), "15");

        await userEvent.click(screen.getByRole("button", {name: /load messages/i}));
        await waitFor(() => expect(mockedAxios.get).toHaveBeenCalledTimes(1));
        await userEvent.click(screen.getByRole("button", {name: /show details/i}));
        expect(screen.getByRole("button", {name: /hide details/i})).toBeInTheDocument();

        await userEvent.click(screen.getByRole("button", {name: /reset filters/i}));

        expect(screen.getByLabelText(/Filter Destination/i)).toHaveValue("");
        expect(screen.getByLabelText(/Filter Delivery Mode/i)).toHaveValue("");
        expect(screen.getByLabelText(/Filter Inner Message Id/i)).toHaveValue("");
        expect(screen.getByLabelText(/Filter Publish Status/i)).toHaveValue("");
        expect(screen.getByLabelText(/only stale pending/i)).not.toBeChecked();
        expect(screen.getByLabelText(/Created At From/i)).toHaveValue("");
        expect(screen.getByLabelText(/Created At To/i)).toHaveValue("");
        expect(screen.getByLabelText(/Published At From/i)).toHaveValue("");
        expect(screen.getByLabelText(/Published At To/i)).toHaveValue("");
        expect(screen.getByLabelText(/Sort By/i)).toHaveValue("createdAt");
        expect(screen.getByLabelText(/Sort Direction/i)).toHaveValue("desc");
        expect(screen.getByLabelText(/^Page$/i)).toHaveValue(0);
        expect(screen.getByLabelText(/^Size$/i)).toHaveValue(20);
        expect(screen.queryByRole("button", {name: /hide details/i})).not.toBeInTheDocument();
    });

    test("Copies destination and payload content from a stored message", async () => {
        mockedAxios.get.mockResolvedValue({
            data: buildMessagesPage(0, {
                totalElements: 1,
                totalPages: 1,
                last: true,
            }),
            status: 200,
            statusText: "OK",
            headers: new AxiosHeaders(),
            config: {headers: new AxiosHeaders()},
        });
        writeTextMock.mockResolvedValue(undefined);

        render(<App/>);

        await userEvent.click(screen.getByRole("button", {name: /load messages/i}));
        await waitFor(() => expect(mockedAxios.get).toHaveBeenCalledTimes(1));

        await userEvent.click(screen.getByRole("button", {name: /copy destination/i}));
        expect(writeTextMock).toHaveBeenCalledWith("solace/java/direct/system-01");
        expect(await screen.findByRole("status")).toHaveTextContent("Destination copied.");

        await userEvent.click(screen.getByRole("button", {name: /copy payload/i}));
        expect(writeTextMock).toHaveBeenLastCalledWith("01001000 01100101 01101100");
        expect(await screen.findByRole("status")).toHaveTextContent("Payload content copied.");
    });

    test("Copies serialized properties from expanded message details", async () => {
        mockedAxios.get.mockResolvedValue({
            data: buildMessagesPage(0, {
                totalElements: 1,
                totalPages: 1,
                last: true,
            }),
            status: 200,
            statusText: "OK",
            headers: new AxiosHeaders(),
            config: {headers: new AxiosHeaders()},
        });
        writeTextMock.mockResolvedValue(undefined);

        render(<App/>);

        await userEvent.click(screen.getByRole("button", {name: /load messages/i}));
        await waitFor(() => expect(mockedAxios.get).toHaveBeenCalledTimes(1));
        await userEvent.click(screen.getByRole("button", {name: /show details/i}));
        await userEvent.click(screen.getByRole("button", {name: /copy properties/i}));

        expect(writeTextMock).toHaveBeenCalledWith("region: ca-east");
        expect(await screen.findByRole("status")).toHaveTextContent("Properties copied.");
    });
});
