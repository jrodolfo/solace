export type MessagePayloadValidationErrorMap = Record<string, string>;

export interface SolaceBrokerAPIError {
    status: number;
    error: string;
    message: string;
    path: string;
    validationErrors: MessagePayloadValidationErrorMap | null;
}
