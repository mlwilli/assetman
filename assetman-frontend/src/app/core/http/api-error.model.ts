export interface ApiValidationError {
  field: string;
  message: string;
  rejectedValue?: unknown;
}

export interface ApiErrorResponse {
  timestamp: string; // Instant serialized as ISO string
  status: number;
  error: string;
  message?: string;
  path?: string;
  validationErrors?: ApiValidationError[];
}
