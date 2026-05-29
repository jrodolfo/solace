/**
 * A map of message payload validation errors where keys are field names and values are error messages.
 */
export type MessagePayloadValidationErrorMap = Record<string, string>;

/**
 * Represents an error response from the Solace Broker API.
 */
export interface SolaceBrokerAPIError {
    /** HTTP status code of the error. */
    status: number;
    /** Short error description or type. */
    error: string;
    /** Detailed error message. */
    message: string;
    /** The API path where the error occurred. */
    path: string;
    /** A map of validation errors, if any; otherwise null. */
    validationErrors: MessagePayloadValidationErrorMap | null;
}
