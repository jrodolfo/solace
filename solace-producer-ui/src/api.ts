import axios, {AxiosInstance} from "axios";
import {SolaceBrokerAPIResponse} from "./SolaceBrokerAPIResponse";

class API {
    private apiInstance: AxiosInstance;

    constructor() {
        this.apiInstance = axios.create({
            baseURL: "http://localhost:8081",
        });

        this.apiInstance.interceptors.request.use((config) => {
            console.log("Request:", `${config.method?.toUpperCase()} ${config.url}`);
            return config;
        });

        this.apiInstance.interceptors.response.use(
            (response) => {
                console.log("Response:", response.data);
                return response;
            },
            (error) => {
                console.log("Error:", error);
                return Promise.reject(error);
            }
        );
    }

    postMessage(data: string) {
        return this.apiInstance.post<SolaceBrokerAPIResponse>(
            `/api/v1/messages/message`,
            JSON.stringify(data),
            {
                headers: {
                    "Content-Type": "application/json", // Explicitly set Content-Type
                },
            }
        );
    }
}

export default new API();
