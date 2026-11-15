import {
  ApiResponse,
  AppInfo,
  HealthResponse,
  AuthResponse,
  UserProfileResponse,
  PlatformOverviewMetrics,
  OnboardingDetailResponse,
  OnboardingStatus,
  ApproveOnboardingDto,
  RejectOnboardingDto,
  ApprovalResultDto,
  TenantSummaryResponse,
  TenantDetailResponse,
  TenantStatus,
  TenantStatusDetailDto,
  CreateTenantDto,
  UpdateTenantDto,
  UpdateTenantStatusDto,
  SubscriptionPlanDto,
  CreateSubscriptionPlanDto,
  UpdateSubscriptionPlanDto,
  AdminSubscriptionDetailDto,
  SubscriptionStatus,
  SubscriptionSummaryDto,
  SubscriptionHistoryDto,
  AssignSubscriptionDto,
  ExtendSubscriptionDto,
  ChangePlanDto,
  SuspendSubscriptionDto,
  CancelSubscriptionDto,
  PageResponse,
} from './types';

const BASE_URL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080/api/v1';
const TOKEN_KEY = 'kuter_admin_token';

export class ApiClient {
  private baseUrl: string;

  constructor(baseUrl: string = BASE_URL) {
    this.baseUrl = baseUrl.replace(/\/+$/, '');
  }

  public getToken(): string | null {
    try {
      return localStorage.getItem(TOKEN_KEY);
    } catch {
      return null;
    }
  }

  public setToken(token: string | null): void {
    try {
      if (token) {
        localStorage.setItem(TOKEN_KEY, token);
      } else {
        localStorage.removeItem(TOKEN_KEY);
      }
    } catch {
      // Storage access blocked or unavailable
    }
  }

  private async request<T>(endpoint: string, options: RequestInit = {}): Promise<ApiResponse<T>> {
    const url = `${this.baseUrl}${endpoint.startsWith('/') ? '' : '/'}${endpoint}`;
    const headers = new Headers(options.headers || {});

    if (!headers.has('Accept')) {
      headers.set('Accept', 'application/json');
    }
    if (!headers.has('Content-Type') && options.body && !(options.body instanceof FormData)) {
      headers.set('Content-Type', 'application/json');
    }

    // Attach correlation ID for end-to-end tracing
    if (!headers.has('X-Correlation-ID')) {
      const correlationId = typeof crypto !== 'undefined' && crypto.randomUUID
        ? crypto.randomUUID()
        : 'admin-req-' + Date.now();
      headers.set('X-Correlation-ID', correlationId);
    }

    // Attach bearer token if authenticated
    const token = this.getToken();
    if (token && !headers.has('Authorization')) {
      headers.set('Authorization', `Bearer ${token}`);
    }

    let response: Response;
    try {
      response = await fetch(url, {
        ...options,
        headers,
      });
    } catch (networkError) {
      throw new Error(
        `Unable to connect to the backend server. Please verify the API is running at ${this.baseUrl}.`
      );
    }

    if (!response.ok) {
      let errorMessage = `HTTP ${response.status} ${response.statusText}`;
      try {
        const errorData = await response.json();
        if (errorData.message) {
          errorMessage = errorData.message;
        }
      } catch {
        // Fallback to HTTP status text
      }

      if (response.status === 401) {
        // Token expired or invalid
        this.setToken(null);
      }

      throw new Error(errorMessage);
    }

    return response.json();
  }

  // ==========================================
  // Health & Info
  // ==========================================

  async getHealth(): Promise<HealthResponse> {
    const res = await this.request<HealthResponse>('/health');
    return res.data;
  }

  async getInfo(): Promise<AppInfo> {
    const res = await this.request<AppInfo>('/info');
    return res.data;
  }

  // ==========================================
  // Authentication & Profile
  // ==========================================

  async login(credentials: { email: string; password: string }): Promise<AuthResponse> {
    const res = await this.request<AuthResponse>('/auth/login', {
      method: 'POST',
      body: JSON.stringify(credentials),
    });
    if (res.data?.accessToken) {
      this.setToken(res.data.accessToken);
    }
    return res.data;
  }

  async logout(): Promise<void> {
    try {
      await this.request<void>('/auth/logout', { method: 'POST' });
    } finally {
      this.setToken(null);
    }
  }

  async getMe(): Promise<UserProfileResponse> {
    const res = await this.request<UserProfileResponse>('/auth/me');
    return res.data;
  }

  // ==========================================
  // Platform Metrics
  // ==========================================

  async getPlatformOverview(): Promise<PlatformOverviewMetrics> {
    const res = await this.request<PlatformOverviewMetrics>('/admin/metrics/overview');
    return res.data;
  }

  // ==========================================
  // Registration / Onboarding Requests
  // ==========================================

  async listOnboardingRequests(params: {
    status?: OnboardingStatus;
    search?: string;
    page?: number;
    size?: number;
  } = {}): Promise<PageResponse<OnboardingDetailResponse>> {
    const query = new URLSearchParams();
    if (params.status) query.set('status', params.status);
    if (params.search) query.set('search', params.search);
    if (params.page !== undefined) query.set('page', params.page.toString());
    if (params.size !== undefined) query.set('size', params.size.toString());

    const queryString = query.toString();
    const endpoint = `/admin/onboarding-requests${queryString ? `?${queryString}` : ''}`;
    const res = await this.request<PageResponse<OnboardingDetailResponse>>(endpoint);
    return res.data;
  }

  async getOnboardingRequest(id: string): Promise<OnboardingDetailResponse> {
    const res = await this.request<OnboardingDetailResponse>(`/admin/onboarding-requests/${id}`);
    return res.data;
  }

  async approveOnboardingRequest(id: string, dto?: ApproveOnboardingDto): Promise<ApprovalResultDto> {
    const res = await this.request<ApprovalResultDto>(`/admin/onboarding-requests/${id}/approve`, {
      method: 'POST',
      body: dto ? JSON.stringify(dto) : JSON.stringify({}),
    });
    return res.data;
  }

  async rejectOnboardingRequest(id: string, dto: RejectOnboardingDto): Promise<OnboardingDetailResponse> {
    const res = await this.request<OnboardingDetailResponse>(`/admin/onboarding-requests/${id}/reject`, {
      method: 'POST',
      body: JSON.stringify(dto),
    });
    return res.data;
  }

  // ==========================================
  // Tenants Management
  // ==========================================

  async listTenants(params: {
    status?: TenantStatus;
    search?: string;
    page?: number;
    size?: number;
  } = {}): Promise<PageResponse<TenantSummaryResponse>> {
    const query = new URLSearchParams();
    if (params.status) query.set('status', params.status);
    if (params.search) query.set('search', params.search);
    if (params.page !== undefined) query.set('page', params.page.toString());
    if (params.size !== undefined) query.set('size', params.size.toString());

    const queryString = query.toString();
    const endpoint = `/admin/tenants${queryString ? `?${queryString}` : ''}`;
    const res = await this.request<PageResponse<TenantSummaryResponse>>(endpoint);
    return res.data;
  }

  async getTenant(id: string): Promise<TenantDetailResponse> {
    const res = await this.request<TenantDetailResponse>(`/admin/tenants/${id}`);
    return res.data;
  }

  async getTenantStatus(id: string): Promise<TenantStatusDetailDto> {
    const res = await this.request<TenantStatusDetailDto>(`/admin/tenants/${id}/status`);
    return res.data;
  }

  async createTenant(dto: CreateTenantDto): Promise<TenantDetailResponse> {
    const res = await this.request<TenantDetailResponse>('/admin/tenants', {
      method: 'POST',
      body: JSON.stringify(dto),
    });
    return res.data;
  }

  async updateTenant(id: string, dto: UpdateTenantDto): Promise<TenantDetailResponse> {
    const res = await this.request<TenantDetailResponse>(`/admin/tenants/${id}`, {
      method: 'PUT',
      body: JSON.stringify(dto),
    });
    return res.data;
  }

  async updateTenantStatus(id: string, dto: UpdateTenantStatusDto): Promise<TenantDetailResponse> {
    const res = await this.request<TenantDetailResponse>(`/admin/tenants/${id}/status`, {
      method: 'PATCH',
      body: JSON.stringify(dto),
    });
    return res.data;
  }

  // ==========================================
  // Subscription Plans
  // ==========================================

  async listPlans(params: {
    isActive?: boolean;
    isPublic?: boolean;
    search?: string;
    page?: number;
    size?: number;
  } = {}): Promise<PageResponse<SubscriptionPlanDto>> {
    const query = new URLSearchParams();
    if (params.isActive !== undefined) query.set('isActive', params.isActive.toString());
    if (params.isPublic !== undefined) query.set('isPublic', params.isPublic.toString());
    if (params.search) query.set('search', params.search);
    if (params.page !== undefined) query.set('page', params.page.toString());
    if (params.size !== undefined) query.set('size', params.size.toString());

    const queryString = query.toString();
    const endpoint = `/subscription-plans${queryString ? `?${queryString}` : ''}`;
    const res = await this.request<PageResponse<SubscriptionPlanDto>>(endpoint);
    return res.data;
  }

  async getPlan(id: string): Promise<SubscriptionPlanDto> {
    const res = await this.request<SubscriptionPlanDto>(`/subscription-plans/${id}`);
    return res.data;
  }

  async createPlan(dto: CreateSubscriptionPlanDto): Promise<SubscriptionPlanDto> {
    const res = await this.request<SubscriptionPlanDto>('/subscription-plans', {
      method: 'POST',
      body: JSON.stringify(dto),
    });
    return res.data;
  }

  async updatePlan(id: string, dto: UpdateSubscriptionPlanDto): Promise<SubscriptionPlanDto> {
    const res = await this.request<SubscriptionPlanDto>(`/subscription-plans/${id}`, {
      method: 'PUT',
      body: JSON.stringify(dto),
    });
    return res.data;
  }

  async updatePlanStatus(id: string, active: boolean): Promise<SubscriptionPlanDto> {
    const res = await this.request<SubscriptionPlanDto>(`/subscription-plans/${id}/status`, {
      method: 'PATCH',
      body: JSON.stringify({ active }),
    });
    return res.data;
  }

  async deletePlan(id: string): Promise<void> {
    await this.request<void>(`/subscription-plans/${id}`, {
      method: 'DELETE',
    });
  }

  // ==========================================
  // Tenant Subscriptions Monitoring
  // ==========================================

  async listSubscriptions(params: {
    status?: SubscriptionStatus;
    planId?: string;
    tenantId?: string;
    search?: string;
    page?: number;
    size?: number;
  } = {}): Promise<PageResponse<AdminSubscriptionDetailDto>> {
    const query = new URLSearchParams();
    if (params.status) query.set('status', params.status);
    if (params.planId) query.set('planId', params.planId);
    if (params.tenantId) query.set('tenantId', params.tenantId);
    if (params.search) query.set('search', params.search);
    if (params.page !== undefined) query.set('page', params.page.toString());
    if (params.size !== undefined) query.set('size', params.size.toString());

    const queryString = query.toString();
    const endpoint = `/admin/subscriptions${queryString ? `?${queryString}` : ''}`;
    const res = await this.request<PageResponse<AdminSubscriptionDetailDto>>(endpoint);
    return res.data;
  }

  async getSubscriptionSummary(): Promise<SubscriptionSummaryDto> {
    const res = await this.request<SubscriptionSummaryDto>('/admin/subscriptions/summary');
    return res.data;
  }

  async getSubscription(id: string): Promise<AdminSubscriptionDetailDto> {
    const res = await this.request<AdminSubscriptionDetailDto>(`/admin/subscriptions/${id}`);
    return res.data;
  }

  async getSubscriptionHistory(id: string): Promise<SubscriptionHistoryDto[]> {
    const res = await this.request<SubscriptionHistoryDto[]>(`/admin/subscriptions/${id}/history`);
    return res.data;
  }

  async assignSubscription(dto: AssignSubscriptionDto): Promise<AdminSubscriptionDetailDto> {
    const res = await this.request<AdminSubscriptionDetailDto>('/admin/subscriptions/assign', {
      method: 'POST',
      body: JSON.stringify(dto),
    });
    return res.data;
  }

  async activateSubscription(id: string): Promise<AdminSubscriptionDetailDto> {
    const res = await this.request<AdminSubscriptionDetailDto>(`/admin/subscriptions/${id}/activate`, {
      method: 'POST',
      body: JSON.stringify({}),
    });
    return res.data;
  }

  async startTrial(id: string, days?: number): Promise<AdminSubscriptionDetailDto> {
    const res = await this.request<AdminSubscriptionDetailDto>(`/admin/subscriptions/${id}/start-trial`, {
      method: 'POST',
      body: JSON.stringify(days ? { trialDays: days } : {}),
    });
    return res.data;
  }

  async changeSubscriptionPlan(id: string, dto: ChangePlanDto): Promise<AdminSubscriptionDetailDto> {
    const res = await this.request<AdminSubscriptionDetailDto>(`/admin/subscriptions/${id}/change-plan`, {
      method: 'POST',
      body: JSON.stringify(dto),
    });
    return res.data;
  }

  async extendSubscription(id: string, dto: ExtendSubscriptionDto): Promise<AdminSubscriptionDetailDto> {
    const res = await this.request<AdminSubscriptionDetailDto>(`/admin/subscriptions/${id}/extend`, {
      method: 'POST',
      body: JSON.stringify(dto),
    });
    return res.data;
  }

  async suspendSubscription(id: string, dto: SuspendSubscriptionDto): Promise<AdminSubscriptionDetailDto> {
    const res = await this.request<AdminSubscriptionDetailDto>(`/admin/subscriptions/${id}/suspend`, {
      method: 'POST',
      body: JSON.stringify(dto),
    });
    return res.data;
  }

  async reactivateSubscription(id: string, reason?: string): Promise<AdminSubscriptionDetailDto> {
    const res = await this.request<AdminSubscriptionDetailDto>(`/admin/subscriptions/${id}/reactivate`, {
      method: 'POST',
      body: JSON.stringify(reason ? { reason } : {}),
    });
    return res.data;
  }

  async cancelSubscription(id: string, dto: CancelSubscriptionDto): Promise<AdminSubscriptionDetailDto> {
    const res = await this.request<AdminSubscriptionDetailDto>(`/admin/subscriptions/${id}/cancel`, {
      method: 'POST',
      body: JSON.stringify(dto),
    });
    return res.data;
  }
}

export const apiClient = new ApiClient();
