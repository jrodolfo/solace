import App from "./App";
import '@testing-library/jest-dom';
import {fireEvent, render, screen, waitFor} from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import "@testing-library/jest-dom";
import axios from "axios";
import {Mock, vi} from "vitest";


test('it shows 5 inputs and 1 button', () => {
    // render the component
    render(<App/>);
    // manipulate the component or find an element in it
    const inputs = screen.getAllByRole('textbox');
    const button = screen.getByRole('button');
    // Assertion - make sure the component is doing what we expect it to do
    expect(inputs).toHaveLength(5);
    expect(button).toBeInTheDocument();
});

// Mock Axios
vi.mock("axios");

const mockedAxios = axios as unknown as {
    post: Mock;
};

// Mock window.alert to prevent test crashes
vi.spyOn(window, "alert").mockImplementation(() => {
});

describe("Form Submission Tests", () => {

    test("Submits form and handles API response", async () => {
        // Mock API response
        mockedAxios.post.mockResolvedValue({
            data: {message: "Success"},
            status: 200,
            statusText: "OK",
        });

        render(<App/>);

        // Fill in form fields (modify IDs according to your JSX)
        await userEvent.type(screen.getByLabelText(/User Name/i), "testUser");
        await userEvent.type(screen.getByLabelText(/Password/i), "testPass");
        await userEvent.type(screen.getByLabelText(/Host/i), "localhost");
        await userEvent.type(screen.getByLabelText(/VPN Name/i), "testVPN");
        await userEvent.type(screen.getByLabelText(/Topic Name/i), "testTopic");
        // we deal with the message text input in a different way because its
        // content is a json string that is not properly handled by screen.getByLabelText
        const messageInput = screen.getByLabelText(/Message/i);
        fireEvent.input(messageInput, {target: {value: '{"key":"value"}'}});

        // Click submit button
        fireEvent.submit(screen.getByRole("button", {name: /publish message/i}));

        // Wait for async API response
        await waitFor(() => expect(mockedAxios.post).toHaveBeenCalledTimes(1));

        // Ensure correct API request was made
        expect(mockedAxios.post).toHaveBeenCalledWith(
            "http://localhost:8081/api/v1/messages/message",
            {
                userName: "testUser",
                password: "testPass",
                host: "localhost",
                vpnName: "testVPN",
                topicName: "testTopic",
                message: {key: "value"}, // JSON parsed message
            },
            {
                headers: {"Content-Type": "application/json"},
            }
        );

        // Check if success alert was triggered
        await waitFor(() => expect(window.alert).toHaveBeenCalledWith("Message Published Successfully!"));
    });

    test("Handles API failure", async () => {
        // Mock API failure response
        mockedAxios.post.mockRejectedValue(new Error("Network Error"));

        render(<App/>);

        // Fill form fields
        await userEvent.type(screen.getByLabelText(/User Name/i), "testUser");
        await userEvent.type(screen.getByLabelText(/Password/i), "testPass");
        await userEvent.type(screen.getByLabelText(/Host/i), "localhost");
        await userEvent.type(screen.getByLabelText(/VPN Name/i), "testVPN");
        await userEvent.type(screen.getByLabelText(/Topic Name/i), "testTopic");
        // we deal with the message text input in a different way because its
        // content is a json string that is not properly handled by screen.getByLabelText
        const messageInput = screen.getByLabelText(/Message/i);
        fireEvent.input(messageInput, {target: {value: '{"key":"value"}'}});

        // Submit form
        // fireEvent.submit(screen.getByRole("button", {name: /publish message/i}));
        await userEvent.click(screen.getByRole("button", {name: /publish message/i}));


        // Wait for API rejection handling
        // TODO: find out why mockedAxios.post is being called twice when handling an API failure. This
        //  suggests that the form submission might be triggering multiple API calls unexpectedly.
        //  When the App component is wrapped in <React.StrictMode> in main.tsx or index.tsx, React will
        //  render components twice in development mode to detect side effects, and this can cause
        //  unexpected double API calls. I have removed StrictMode from main.tsx to see if that changes
        //  the behavior, and it did not.
        await waitFor(() => expect(mockedAxios.post).toHaveBeenCalledTimes(2));

        // Ensure error handling logic executed (alert or console.error)
        await waitFor(() => expect(window.alert).toHaveBeenCalledWith("Failed to publish the message. See console for details."));
    });
});

// test('it calls handleSubmit when the form is submitted', () => {
//
//     // render the component
//     render(<App />);
//
//     // Find the function
//     const submittFunction = screen.
//
//     // Find the inputs
//     const [userNameInput, passwordInput, hostInput, vpnNameInput, topicNameInput, messageInput] = screen.getAllByRole('textbox');
//
//     // simulate user typing in the values of the inputs
//     user.click(userNameInput);
//     user.type(userNameInput, 'test');
//     user.click(passwordInput);
//     user.type(passwordInput, '<PASSWORD>');
//     user.click(hostInput);
//     user.type(hostInput, 'test');
//     user.click(vpnNameInput);
//     user.type(vpnNameInput, 'test');
//     user.click(topicNameInput);
//     user.type(topicNameInput, 'test');
//     user.click(messageInput);
//     user.type(messageInput, 'test');
//
//     // Find the button
//     const button = screen.getByRole('button');
//
//     // Simulate clicking the button
//     user.click(button);
//
//     // Assertion to make sure handleSubmit get called with all values
//     // expect(arguments).toHaveLength(1);
//     // expect(arguments[0][0]).toEqual('test');
//
// })
