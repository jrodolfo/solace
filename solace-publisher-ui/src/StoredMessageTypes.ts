export interface StoredMessageProperty {
    id?: number;
    propertyKey: string;
    propertyValue: string;
    createdAt?: string | null;
    updatedAt?: string | null;
}

export interface StoredMessagePayload {
    id?: number;
    type: string;
    content: string;
    createdAt?: string | null;
    updatedAt?: string | null;
}

export interface StoredMessage {
    id?: number;
    innerMessageId: string;
    destination: string;
    deliveryMode: string;
    priority: number;
    properties: StoredMessageProperty[];
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
