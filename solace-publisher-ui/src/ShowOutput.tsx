import {AxiosResponse} from "axios";

const SENSITIVE_KEY_PATTERN = /(password|authorization|token|secret|credential|api[-_]?key)/i;

const maskSecret = (value: unknown): string => {
    if (typeof value === "string") {
        return "*".repeat(value.length);
    }
    if (value == null) {
        return "";
    }
    return "*".repeat(String(value).length);
};

const tryParseJsonString = (value: string): unknown => {
    const trimmedValue = value.trim();
    if (!trimmedValue.startsWith("{") && !trimmedValue.startsWith("[")) {
        return value;
    }

    try {
        return JSON.parse(trimmedValue);
    } catch {
        return value;
    }
};

const sanitizeForDisplay = (value: unknown, keyName = ""): unknown => {
    if (typeof value === "string" && keyName === "data") {
        return sanitizeForDisplay(tryParseJsonString(value));
    }

    if (Array.isArray(value)) {
        return value.map((item) => sanitizeForDisplay(item));
    }

    if (value && typeof value === "object") {
        return Object.fromEntries(
            Object.entries(value).map(([key, entryValue]) => [
                key,
                SENSITIVE_KEY_PATTERN.test(key) ? maskSecret(entryValue) : sanitizeForDisplay(entryValue, key),
            ])
        );
    }

    return value;
};

/**
 * Properties for the ShowOutput component.
 */
interface ShowOutputProps {
    /** The axios response to display. */
    res: AxiosResponse<unknown, unknown> | null;
}

/**
 * Component that displays the details of an Axios response, including status, headers, data, and configuration.
 * 
 * @param props - Component properties.
 * @returns A JSX element displaying the response details or a message if no response is provided.
 */
const ShowOutput: React.FC<ShowOutputProps> = ({ res }) => {
    if (!res) {
        return <div>No response to show</div>;
    }

    const sanitizedConfig = sanitizeForDisplay(res.config);

    return (

        <div>

            <h2 className="mb-4 align-content-lg-center">Response</h2>

        <div className="card card-body mb-4">
            <h5>Status: {res.status}</h5>
        </div>

        <div className="card mt-3">
            <div className="card-header">
                Headers
            </div>
            <div className="card-body">
                <pre className="response-json">{JSON.stringify(res.headers, null, 2)}</pre>
            </div>
        </div>

        <div className="card mt-3">
            <div className="card-header">
                Data
            </div>
            <div className="card-body">
                <pre className="response-json">{JSON.stringify(res.data, null, 2)}</pre>
            </div>
        </div>

        <div className="card mt-3">
            <div className="card-header">
                Config (sanitized)
            </div>
            <div className="card-body">
                <pre className="response-json">{JSON.stringify(sanitizedConfig, null, 2)}</pre>
            </div>
        </div>
    </div>
    );
};

export default ShowOutput;
