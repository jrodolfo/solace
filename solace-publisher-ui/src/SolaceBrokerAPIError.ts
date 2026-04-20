export interface SolaceBrokerAPIError {
    status: number;
    error: string;
    message: string;
    path: string;
    validationErrors: Record<string, string> | null;
}
