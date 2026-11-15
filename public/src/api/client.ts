import {
  PublicSubscriptionPlanDto,
  CreateOnboardingRequestDto,
  OnboardingPublicResponse,
  ApiResponse,
  ApiErrorResponse,
} from '../types/api';

export class PublicApiClient {
  private baseUrl: string;

  constructor(baseUrl = (import.meta.env?.VITE_API_BASE_URL as string) || '/api/v1') {
    this.baseUrl = baseUrl.replace(/\/$/, '');
  }

  private async request<T>(endpoint: string, options?: RequestInit): Promise<T> {
    const url = `${this.baseUrl}${endpoint.startsWith('/') ? endpoint : `/${endpoint}`}`;
    
    let response: Response;
    try {
      response = await fetch(url, {
        headers: {
          'Content-Type': 'application/json',
          Accept: 'application/json',
          ...options?.headers,
        },
        ...options,
      });
    } catch (err: unknown) {
      const message = err instanceof Error ? err.message : 'Network error occurred';
      throw new Error(`Unable to connect to Kuter server: ${message}`);
    }

    let json: ApiResponse<T> | ApiErrorResponse | null = null;
    try {
      json = await response.json();
    } catch {
      // response might not be JSON (e.g. 502/504 gateway error)
    }

    if (!response.ok) {
      if (json && 'message' in json && json.message) {
        const error = new Error(json.message) as Error & {
          status?: number;
          fieldErrors?: Record<string, string>;
        };
        error.status = response.status;
        if ('fieldErrors' in json && json.fieldErrors) {
          error.fieldErrors = json.fieldErrors;
        }
        throw error;
      }
      // Surface status code so callers can distinguish 403/404/500
      const error = new Error(`Server returned HTTP ${response.status}: ${response.statusText}`) as Error & { status?: number };
      error.status = response.status;
      throw error;
    }

    if (json && 'data' in json) {
      return (json as ApiResponse<T>).data;
    }

    return json as unknown as T;
  }

  async getPublicPlans(): Promise<PublicSubscriptionPlanDto[]> {
    return this.request<PublicSubscriptionPlanDto[]>('/public/subscription-plans');
  }

  async submitOnboardingRequest(dto: CreateOnboardingRequestDto): Promise<OnboardingPublicResponse> {
    return this.request<OnboardingPublicResponse>('/onboarding-requests', {
      method: 'POST',
      body: JSON.stringify(dto),
    });
  }

  async getOnboardingStatus(referenceCode: string): Promise<OnboardingPublicResponse> {
    const cleanRef = referenceCode.trim().toUpperCase();
    return this.request<OnboardingPublicResponse>(
      `/onboarding-requests/status/${encodeURIComponent(cleanRef)}`
    );
  }
}

export const publicApi = new PublicApiClient();
