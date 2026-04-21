import App from "./App";
import '@testing-library/jest-dom';
import {render, screen, waitFor} from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import "@testing-library/jest-dom";
import axios from "axios";
import {beforeEach, Mock, vi} from "vitest";
import {AxiosHeaders} from "axios";


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
});

// Mock Axios
vi.mock("axios");

const mockedAxios = axios as unknown as {
    post: Mock;
    get: Mock;
    isAxiosError: Mock;
};

beforeEach(() => {
    mockedAxios.post.mockReset();
    mockedAxios.get.mockReset();
    mockedAxios.isAxiosError.mockImplementation((error: unknown) => Boolean((error as { isAxiosError?: boolean })?.isAxiosError));
});

function buildMessagesPage(page: number, overrides?: Partial<{
    size: number;
    totalElements: number;
    totalPages: number;
    first: boolean;
    last: boolean;
    items: Array<Record<string, unknown>>;
}>) {
    return {
        items: overrides?.items ?? [
            {
                id: 1,
                innerMessageId: "001",
                destination: "solace/java/direct/system-01",
                deliveryMode: "PERSISTENT",
                priority: 3,
                payload: {
                    type: "binary",
                    content: "01001000 01100101 01101100",
                },
                properties: [
                    {
                        propertyKey: "region",
                        propertyValue: "ca-east",
                    },
                ],
            },
        ],
        page,
        size: overrides?.size ?? 20,
        totalElements: overrides?.totalElements ?? 2,
        totalPages: overrides?.totalPages ?? 2,
        first: overrides?.first ?? page === 0,
        last: overrides?.last ?? false,
    };
}

describe("Form Submission Tests", () => {

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

    test("Submits form and handles API response", async () => {
        mockedAxios.post.mockResolvedValue({
            data: {
                destination: "solace/java/direct/system-01",
                content: "01001000 01100101 01101100",
            },
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
            data: {
                destination: "solace/java/direct/system-01",
                content: "01001000 01100101 01101100",
            },
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
                    properties: [
                        {key: "region", value: "ca-east"},
                        {key: "source", value: "publisher-ui"},
                    ],
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
                data: {
                    status: 400,
                    error: "Bad Request",
                    message: "Request validation failed",
                    path: "/api/v1/messages/message",
                    validationErrors: {
                        "message.innerMessageId": "message.innerMessageId is required",
                    },
                },
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

describe("Stored Messages Browser", () => {
    test("Loads and renders paginated stored messages", async () => {
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
        expect(await screen.findByText(/Loaded 1 messages\./i)).toBeInTheDocument();
        expect(screen.getByText(/Page 1 of 1/i)).toBeInTheDocument();
        expect(screen.getByText(/solace\/java\/direct\/system-01/i)).toBeInTheDocument();
        expect(screen.getByText(/^binary$/i)).toBeInTheDocument();
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
                    sortBy: "priority",
                    sortDirection: "asc",
                },
            }
        );
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
                        {
                            id: 2,
                            innerMessageId: "002",
                            destination: "solace/java/direct/system-02",
                            deliveryMode: "DIRECT",
                            priority: 1,
                            payload: {
                                type: "text",
                                content: "hello world",
                            },
                            properties: [],
                        },
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
        expect(screen.getByText(/region: ca-east/i)).toBeInTheDocument();

        await userEvent.click(screen.getByRole("button", {name: /hide details/i}));

        expect(screen.getByRole("button", {name: /show details/i})).toHaveAttribute("aria-expanded", "false");
        expect(screen.queryByText(/region: ca-east/i)).not.toBeInTheDocument();
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
                    sortBy: "priority",
                    sortDirection: "asc",
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
        expect(screen.getByLabelText(/Sort By/i)).toHaveValue("createdAt");
        expect(screen.getByLabelText(/Sort Direction/i)).toHaveValue("desc");
        expect(screen.getByLabelText(/^Page$/i)).toHaveValue(0);
        expect(screen.getByLabelText(/^Size$/i)).toHaveValue(20);
        expect(screen.queryByRole("button", {name: /hide details/i})).not.toBeInTheDocument();
    });
});
