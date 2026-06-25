import {render, screen} from "@testing-library/react";
import "@testing-library/jest-dom";
import ShowOutput from "./ShowOutput";
import {AxiosResponse, InternalAxiosRequestConfig} from "axios";

describe("ShowOutput Component", () => {

  test("renders 'No response to show' when res is null", () => {
        render(<ShowOutput res={null}/>);
        expect(screen.getByText(/No response to show/i)).toBeInTheDocument();
    });

    test("renders response details when res is provided", () => {

        // mock an AxiosResponse
        const DEFAULT_EMPTY_HEADERS = {} as InternalAxiosRequestConfig<never>;
        const mockResponse: AxiosResponse = {
            status: 200,
            data: {message: "Hello World"},
            headers: {"content-type": "application/json"},
            config: DEFAULT_EMPTY_HEADERS,
            statusText: "OK",
            request: {},
        };

        render(<ShowOutput res={mockResponse}/>);

        // check for status
        expect(screen.getByText(/Status: 200/i)).toBeInTheDocument();
        // check for headers
        expect(screen.getByText(/content-type/i)).toBeInTheDocument();
        // check for data
        expect(screen.getByText(/Hello World/i)).toBeInTheDocument();
    });

    test("renders validation errors when present in the response body", () => {
        const DEFAULT_EMPTY_HEADERS = {} as InternalAxiosRequestConfig<never>;
        const mockResponse: AxiosResponse = {
            status: 400,
            data: {
                message: "Request validation failed",
                validationErrors: {
                    "message.innerMessageId": "message.innerMessageId is required",
                },
            },
            headers: {"content-type": "application/json"},
            config: DEFAULT_EMPTY_HEADERS,
            statusText: "Bad Request",
            request: {},
        };

        render(<ShowOutput res={mockResponse}/>);

        expect(screen.getByText(/Request validation failed/i)).toBeInTheDocument();
        expect(screen.getByText(/message\.innerMessageId/i)).toBeInTheDocument();
    });

    test("masks sensitive config values before rendering", () => {
        const mockResponse: AxiosResponse = {
            status: 201,
            data: {message: "Published"},
            headers: {"content-type": "application/json"},
            config: {
                headers: {},
                data: JSON.stringify({
                    password: "secret123",
                    nested: {
                        authorization: "Bearer abc",
                    },
                }),
            } as unknown as InternalAxiosRequestConfig,
            statusText: "Created",
            request: {},
        };

        render(<ShowOutput res={mockResponse}/>);

        expect(screen.getByText(/Config \(sanitized\)/i)).toBeInTheDocument();
        expect(screen.getByText(/"password": "\*{9}"/i)).toBeInTheDocument();
        expect(screen.getByText(/"authorization": "\*{10}"/i)).toBeInTheDocument();
        expect(screen.queryByText(/secret123/i)).not.toBeInTheDocument();
        expect(screen.queryByText(/Bearer abc/i)).not.toBeInTheDocument();
    });

    test("renders response json blocks with the scrollable class", () => {
        const DEFAULT_EMPTY_HEADERS = {} as InternalAxiosRequestConfig<never>;
        const mockResponse: AxiosResponse = {
            status: 200,
            data: {message: "Hello World"},
            headers: {"content-type": "application/json"},
            config: DEFAULT_EMPTY_HEADERS,
            statusText: "OK",
            request: {},
        };

        const {container} = render(<ShowOutput res={mockResponse}/>);

        expect(container.querySelectorAll("pre.response-json")).toHaveLength(3);
    });
});
