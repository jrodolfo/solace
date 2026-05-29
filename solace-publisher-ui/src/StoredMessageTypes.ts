/**
 * The delivery mode of the message.
 * - DIRECT: Fast, non-persistent messaging.
 * - NON_PERSISTENT: At-most-once delivery.
 * - PERSISTENT: Guaranteed delivery.
 */
export type DeliveryMode = "DIRECT" | "NON_PERSISTENT" | "PERSISTENT";

/**
 * The type of the message payload.
 */
export type PayloadType = "TEXT" | "BINARY" | "JSON" | "XML";

/**
 * Represents the payload of a stored message.
 */
export interface StoredMessagePayload {
    /** The format of the payload. */
    type: PayloadType;
    /** The actual content of the message. */
    content: string;
    /** ISO timestamp when the payload was created. */
    createdAt?: string | null;
    /** ISO timestamp when the payload was last updated. */
    updatedAt?: string | null;
}

/**
 * Represents a message stored in the database.
 */
export interface StoredMessage {
    /** Unique identifier for the stored message. */
    id: number;
    /** Internal identifier used by the messaging system. */
    innerMessageId: string;
    /** The destination (topic or queue) of the message. */
    destination: string;
    /** Delivery mode used for this message. */
    deliveryMode: DeliveryMode;
    /** Message priority. */
    priority: number;
    /** Current status of the message publication. */
    publishStatus: "PENDING" | "PUBLISHED" | "FAILED";
    /** Whether the message is considered stale while pending. */
    stalePending: boolean;
    /** Reason for failure if the message could not be published. */
    failureReason?: string | null;
    /** ISO timestamp when the message was successfully published. */
    publishedAt?: string | null;
    /** Indicates if the message can be retried for publication. */
    retrySupported: boolean;
    /** Explanation if retrying is blocked. */
    retryBlockedReason?: string | null;
    /** Custom properties associated with the message. */
    properties: Record<string, string>;
    /** The message payload content and type. */
    payload: StoredMessagePayload;
    /** ISO timestamp when the record was created. */
    createdAt?: string | null;
    /** ISO timestamp when the record was last updated. */
    updatedAt?: string | null;
}

/**
 * Paged response containing a list of stored messages and metadata.
 */
export interface PagedStoredMessagesResponse {
    /** List of messages for the current page. */
    items: StoredMessage[];
    /** Current page number (0-indexed). */
    page: number;
    /** Number of elements per page. */
    size: number;
    /** Total number of elements across all pages. */
    totalElements: number;
    /** Total number of pages available. */
    totalPages: number;
    /** True if this is the first page. */
    first: boolean;
    /** True if this is the last page. */
    last: boolean;
    /** Counts of messages in different states of their lifecycle. */
    lifecycleCounts: {
        publishedCount: number;
        failedCount: number;
        pendingCount: number;
        stalePendingCount: number;
        retryableFailedCount: number;
        nonRetryableFailedCount: number;
    };
}

/**
 * Response for a filtered messages export request.
 */
export interface FilteredMessagesExportResponse {
    /** ISO timestamp when the export was generated. */
    exportedAt: string;
    /** Filters applied to the export. */
    filters: {
        destination?: string | null;
        deliveryMode?: DeliveryMode | null;
        payloadType?: PayloadType | null;
        innerMessageId?: string | null;
        publishStatus?: "PENDING" | "PUBLISHED" | "FAILED" | null;
        stalePendingOnly: boolean;
        createdAtFrom?: string | null;
        createdAtTo?: string | null;
        publishedAtFrom?: string | null;
        publishedAtTo?: string | null;
        sortBy: string;
        sortDirection: string;
    };
    /** Total number of elements matching the filters. */
    totalElements: number;
    /** Lifecycle counts for the filtered result set. */
    lifecycleCounts: PagedStoredMessagesResponse["lifecycleCounts"];
    /** The list of exported messages. */
    items: StoredMessage[];
}

/**
 * Result of a bulk retry operation for a single message.
 */
export interface BulkRetryResultItem {
    /** ID of the message that was retried. */
    messageId: number | null;
    /** The outcome of the retry attempt. */
    outcome: "RETRIED" | "FAILED" | "SKIPPED";
    /** Additional details about the outcome. */
    detail: string;
    /** New status of the message after the retry. */
    publishStatus?: "PENDING" | "PUBLISHED" | "FAILED" | null;
    /** API response if the retry was successful. */
    response?: {
        destination: string;
        content: string;
    } | null;
}

/**
 * Response containing the aggregate results of a bulk retry operation.
 */
export interface BulkRetryResponse {
    /** Total number of messages requested for retry. */
    totalRequested: number;
    /** Number of messages successfully retried. */
    retriedSuccessfully: number;
    /** Number of messages that failed to be retried. */
    failedToRetry: number;
    /** Number of messages that were skipped. */
    skipped: number;
    /** Detailed results for each message. */
    results: BulkRetryResultItem[];
}
