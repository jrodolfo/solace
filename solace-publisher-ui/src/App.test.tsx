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
    const spinbutton = screen.getByRole('spinbutton');
    const buttons = screen.getAllByRole('button');
    expect(textboxes).toHaveLength(10);
    expect(spinbutton).toBeInTheDocument();
    expect(buttons).toHaveLength(3);
});

// Mock Axios
vi.mock("axios");

const mockedAxios = axios as unknown as {
    post: Mock;
    isAxiosError: Mock;
};

beforeEach(() => {
    mockedAxios.post.mockReset();
    mockedAxios.isAxiosError.mockImplementation((error: unknown) => Boolean((error as { isAxiosError?: boolean })?.isAxiosError));
});

describe("Form Submission Tests", () => {

    async function fillRequiredFormFields() {
        await userEvent.type(screen.getByLabelText(/User Name/i), "testUser");
        await userEvent.type(screen.getByLabelText(/Password/i), "testPass");
        await userEvent.type(screen.getByLabelText(/^Host$/i), "localhost");
        await userEvent.type(screen.getByLabelText(/VPN Name/i), "testVPN");
        await userEvent.type(screen.getByLabelText(/Inner Message Id/i), "001");
        await userEvent.type(screen.getByLabelText(/Destination/i), "solace/java/direct/system-01");
        await userEvent.type(screen.getByLabelText(/Delivery Mode/i), "PERSISTENT");
        await userEvent.clear(screen.getByLabelText(/Priority/i));
        await userEvent.type(screen.getByLabelText(/Priority/i), "3");
        await userEvent.type(screen.getByLabelText(/Payload Type/i), "binary");
        await userEvent.type(screen.getByLabelText(/Payload Content/i), "01001000 01100101 01101100");
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
        await userEvent.type(screen.getByLabelText(/Inner Message Id/i), " ");
        await userEvent.type(screen.getByLabelText(/Destination/i), "solace/java/direct/system-01");
        await userEvent.type(screen.getByLabelText(/Delivery Mode/i), " ");
        await userEvent.clear(screen.getByLabelText(/Priority/i));
        await userEvent.type(screen.getByLabelText(/Priority/i), "0");
        await userEvent.type(screen.getByLabelText(/Payload Type/i), " ");
        await userEvent.type(screen.getByLabelText(/Payload Content/i), "01001000 01100101 01101100");

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
