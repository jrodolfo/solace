/**
 * Represents a successful response from the Solace Broker API when a message is published.
 */
export interface SolaceBrokerAPIResponse {
    /** The destination where the message was published. */
    destination: string;
    /** The content of the published message. */
    content: string;
}
