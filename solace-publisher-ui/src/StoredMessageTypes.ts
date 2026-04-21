export interface StoredMessagePayload {
    type: string;
    content: string;
    createdAt?: string | null;
    updatedAt?: string | null;
}

export interface StoredMessage {
    id: number;
    innerMessageId: string;
    destination: string;
    deliveryMode: string;
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
}
