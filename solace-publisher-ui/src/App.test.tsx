import App from "./App";
import '@testing-library/jest-dom';
import {fireEvent, render, screen, waitFor} from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import "@testing-library/jest-dom";
import axios from "axios";
import {beforeEach, Mock, vi} from "vitest";
import {AxiosHeaders} from "axios";


test('it shows 5 inputs and 1 button', () => {
    // render the component
    render(<App/>);
    // manipulate the component or find an element in it
    const inputs = screen.getAllByRole('textbox');
    const button = screen.getByRole('button');
    // Assertion - make sure the component is doing what we expect it to do
    expect(inputs).toHaveLength(4);
    expect(button).toBeInTheDocument();
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

        await userEvent.type(screen.getByLabelText(/User Name/i), "testUser");
        await userEvent.type(screen.getByLabelText(/Password/i), "testPass");
        await userEvent.type(screen.getByLabelText(/Host/i), "localhost");
        await userEvent.type(screen.getByLabelText(/VPN Name/i), "testVPN");
        // we deal with the message text input in a different way because its
        // content is a json string that is not properly handled by screen.getByLabelText
        const messageInput = screen.getByLabelText(/Message/i);
        fireEvent.input(messageInput, {target: {value: '{"key":"value"}'}});

        fireEvent.submit(screen.getByRole("button", {name: /publish message/i}));

        await waitFor(() => expect(mockedAxios.post).toHaveBeenCalledTimes(1));

        expect(mockedAxios.post).toHaveBeenCalledWith(
            "http://localhost:8081/api/v1/messages/message",
            {
                userName: "testUser",
                password: "testPass",
                host: "localhost",
                vpnName: "testVPN",
                message: {key: "value"}, // JSON parsed message
            },
            {
                headers: {"Content-Type": "application/json"},
            }
        );

        expect(await screen.findByRole("alert")).toHaveTextContent("Message published successfully.");
        expect(screen.getByText(/Status: 201/i)).toBeInTheDocument();
        expect(screen.getByText(/solace\/java\/direct\/system-01/i)).toBeInTheDocument();
    });

    test("Handles typed validation failure", async () => {
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

        await userEvent.type(screen.getByLabelText(/User Name/i), "testUser");
        await userEvent.type(screen.getByLabelText(/Password/i), "testPass");
        await userEvent.type(screen.getByLabelText(/Host/i), "localhost");
        await userEvent.type(screen.getByLabelText(/VPN Name/i), "testVPN");
        const messageInput = screen.getByLabelText(/Message/i);
        fireEvent.input(messageInput, {target: {value: '{"payload":{"content":"x"}}'}});

        await userEvent.click(screen.getByRole("button", {name: /publish message/i}));

        await waitFor(() => expect(mockedAxios.post).toHaveBeenCalledTimes(1));
        expect(await screen.findByRole("alert")).toHaveTextContent("Request validation failed");
        expect(screen.getByText(/message\.innermessageid is required/i)).toBeInTheDocument();
        expect(screen.getByText(/Status: 400/i)).toBeInTheDocument();
    });

    test("Handles invalid message json before calling the api", async () => {
        render(<App/>);

        await userEvent.type(screen.getByLabelText(/User Name/i), "testUser");
        await userEvent.type(screen.getByLabelText(/Password/i), "testPass");
        await userEvent.type(screen.getByLabelText(/Host/i), "localhost");
        await userEvent.type(screen.getByLabelText(/VPN Name/i), "testVPN");
        fireEvent.input(screen.getByLabelText(/Message/i), {target: {value: '{"invalidJson"'}});

        await userEvent.click(screen.getByRole("button", {name: /publish message/i}));

        expect(mockedAxios.post).not.toHaveBeenCalled();
        expect(await screen.findByRole("alert")).toHaveTextContent("Message must be valid JSON");
        expect(screen.getByText(/Status: 400/i)).toBeInTheDocument();
    });
});
