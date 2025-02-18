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

        // check for statusq
        expect(screen.getByText(/Status: 200/i)).toBeInTheDocument();
        // check for headers
        expect(screen.getByText(/content-type/i)).toBeInTheDocument();
        // check for data
        expect(screen.getByText(/Hello World/i)).toBeInTheDocument();
    });
});