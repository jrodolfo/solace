export type DeliveryMode = "DIRECT" | "NON_PERSISTENT" | "PERSISTENT";
export type PayloadType = "TEXT" | "BINARY" | "JSON" | "XML";

export interface StoredMessagePayload {
    type: PayloadType;
    content: string;
    createdAt?: string | null;
    updatedAt?: string | null;
}

export interface StoredMessage {
    id: number;
    innerMessageId: string;
    destination: string;
    deliveryMode: DeliveryMode;
    priority: number;
    publishStatus: "PENDING" | "PUBLISHED" | "FAILED";
    stalePending: boolean;
    failureReason?: string | null;
    publishedAt?: string | null;
    retrySupported: boolean;
    retryBlockedReason?: string | null;
    properties: Record<string, string>;
    payload: StoredMessagePayload;
    createdAt?: string | null;
    updatedAt?: string | null;
}

export interface PagedStoredMessagesResponse {
    items: StoredMessage[];
    page: number;
    size: number;
    totalElements: number;
    totalPages: number;
    first: boolean;
    last: boolean;
    lifecycleCounts: {
        publishedCount: number;
        failedCount: number;
        pendingCount: number;
        stalePendingCount: number;
        retryableFailedCount: number;
        nonRetryableFailedCount: number;
    };
}

export interface FilteredMessagesExportResponse {
    exportedAt: string;
    filters: {
        destination?: string | null;
        deliveryMode?: DeliveryMode | null;
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
    totalElements: number;
    lifecycleCounts: PagedStoredMessagesResponse["lifecycleCounts"];
    items: StoredMessage[];
}

export interface BulkRetryResultItem {
    messageId: number | null;
    outcome: "RETRIED" | "FAILED" | "SKIPPED";
    detail: string;
    publishStatus?: "PENDING" | "PUBLISHED" | "FAILED" | null;
    response?: {
        destination: string;
        content: string;
    } | null;
}

export interface BulkRetryResponse {
    totalRequested: number;
    retriedSuccessfully: number;
    failedToRetry: number;
    skipped: number;
    results: BulkRetryResultItem[];
}
