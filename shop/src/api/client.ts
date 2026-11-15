import {
  ApiResponse,
  PageResponse,
  UserProfile,
  LoginResponseData,
  ShopTenantDetail,
  TenantMembership,
  TenantMemberDetailResponse,
  CategoryResponse,
  CreateCategoryDto,
  UpdateCategoryDto,
  UnitDefinition,
  ProductUnitConversion,
  CreateProductUnitConversionDto,
  ProductSummary,
  ProductDetail,
  CreateProductDto,
  UpdateProductDto,
  ProductStatus,
  StockBalanceResponse,
  StockMovementResponse,
  MovementType,
  CreateStockReceivingDto,
  StockReceivingResponse,
  CreateStockAdjustmentDto,
  StockAdjustmentResponse,
  CreateSaleRequestDto,
  SaleResponseDto,
  SaleReceiptResponseDto,
  VoidSaleRequestDto,
  SalesSummaryReportDto,
  DailySalesSummaryDto,
  InventoryValuationReport,
  TenantSubscriptionInfo,
  HealthResponse,
  AddMemberDto,
  UpdateMemberDto,
  TransferOwnershipDto,
  ChangePasswordDto,
  UpdateSellingPriceDto,
  StockLoanResponse,
  CreateBorrowingRequestDto,
  CreateLoanRequestDto,
  RecordLoanReturnDto,
  CancelLoanDto,
  LendingObligationsSummary,
} from './types';

const BASE_URL = import.meta.env.VITE_API_BASE_URL || '/api/v1';
const TOKEN_KEY = 'kuter_shop_token';
const TENANT_KEY = 'kuter_shop_tenant_id';

export class ShopApiClient {
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

  public getActiveTenantId(): string | null {
    try {
      return localStorage.getItem(TENANT_KEY);
    } catch {
      return null;
    }
  }

  public setActiveTenantId(tenantId: string | null): void {
    try {
      if (tenantId) {
        localStorage.setItem(TENANT_KEY, tenantId);
      } else {
        localStorage.removeItem(TENANT_KEY);
      }
    } catch {
      // Storage blocked
    }
  }

  public clearSession(): void {
    this.setToken(null);
    this.setActiveTenantId(null);
  }

  private async request<T>(endpoint: string, options: RequestInit = {}): Promise<ApiResponse<T>> {
    const cleanEndpoint = endpoint.startsWith('/') ? endpoint : `/${endpoint}`;
    // If baseUrl is relative (/api/v1) and starts with /api/v1, avoid double prefix
    const path = this.baseUrl.endsWith('/api/v1') && cleanEndpoint.startsWith('/api/v1')
      ? cleanEndpoint
      : `${this.baseUrl}${cleanEndpoint}`;

    const headers = new Headers(options.headers || {});

    if (!headers.has('Accept')) {
      headers.set('Accept', 'application/json');
    }
    if (!headers.has('Content-Type') && options.body && !(options.body instanceof FormData)) {
      headers.set('Content-Type', 'application/json');
    }

    // Attach correlation ID for traceability
    if (!headers.has('X-Correlation-ID')) {
      const cid = typeof crypto !== 'undefined' && crypto.randomUUID
        ? crypto.randomUUID()
        : 'shop-req-' + Date.now();
      headers.set('X-Correlation-ID', cid);
    }

    // Attach bearer token if authenticated
    const token = this.getToken();
    if (token && !headers.has('Authorization')) {
      headers.set('Authorization', `Bearer ${token}`);
    }

    // Attach active tenant context header
    const activeTenantId = this.getActiveTenantId();
    if (activeTenantId && !headers.has('X-Tenant-ID')) {
      headers.set('X-Tenant-ID', activeTenantId);
    }

    let response: Response;
    try {
      response = await fetch(path, {
        ...options,
        headers,
      });
    } catch (err: any) {
      throw new Error(`Network failure connecting to Kuter API: ${err?.message || err}`);
    }

    let data: any;
    const contentType = response.headers.get('content-type') || '';
    if (contentType.includes('application/json')) {
      try {
        data = await response.json();
      } catch {
        data = null;
      }
    } else {
      const text = await response.text();
      data = { message: text };
    }

    if (!response.ok) {
      const message = data?.message || data?.error || `HTTP ${response.status}: ${response.statusText}`;
      const error = new Error(message) as any;
      error.status = response.status;
      error.data = data;
      error.correlationId = data?.correlationId;

      if (response.status === 401) {
        // Expired or invalid token
        this.clearSession();
      }

      throw error;
    }

    return data as ApiResponse<T>;
  }

  // ==========================================
  // System & Health
  // ==========================================
  public async getHealth(): Promise<ApiResponse<HealthResponse>> {
    return this.request<HealthResponse>('/health');
  }

  // ==========================================
  // Authentication & Identity
  // ==========================================
  public async login(email: string, password: string): Promise<ApiResponse<LoginResponseData>> {
    const res = await this.request<LoginResponseData>('/auth/login', {
      method: 'POST',
      body: JSON.stringify({ email, password }),
    });

    if (res.data?.accessToken) {
      this.setToken(res.data.accessToken);
      // Auto-set tenant if user has memberships
      if (res.data.memberships && res.data.memberships.length > 0) {
        this.setActiveTenantId(res.data.memberships[0].tenantId);
      }
    }

    return res;
  }

  public async getMe(): Promise<ApiResponse<UserProfile>> {
    const res = await this.request<UserProfile>('/auth/me');
    if (res.data && res.data.memberships && res.data.memberships.length > 0) {
      // If current active tenant is not set, set it to the first membership
      if (!this.getActiveTenantId()) {
        this.setActiveTenantId(res.data.memberships[0].tenantId);
      }
    }
    return res;
  }

  public async getMemberships(): Promise<ApiResponse<TenantMembership[]>> {
    return this.request<TenantMembership[]>('/auth/memberships');
  }

  public async changePassword(dto: ChangePasswordDto): Promise<ApiResponse<void>> {
    return this.request<void>('/auth/change-password', {
      method: 'POST',
      body: JSON.stringify(dto),
    }) as unknown as Promise<ApiResponse<void>>;
  }

  // ==========================================
  // Shop Tenant Profile & Settings
  // ==========================================
  public async getCurrentTenant(): Promise<ApiResponse<ShopTenantDetail>> {
    return this.request<ShopTenantDetail>('/tenants/me');
  }

  public async getCurrentTenantStatus(): Promise<ApiResponse<any>> {
    return this.request<any>('/tenants/me/status');
  }

  public async updateCurrentTenant(dto: Partial<ShopTenantDetail>): Promise<ApiResponse<ShopTenantDetail>> {
    return this.request<ShopTenantDetail>('/tenants/me', {
      method: 'PUT',
      body: JSON.stringify(dto),
    });
  }

  public async getTenantStaffMemberships(): Promise<ApiResponse<TenantMemberDetailResponse[]>> {
    const tenantId = this.getActiveTenantId();
    if (tenantId) {
      return this.request<TenantMemberDetailResponse[]>(`/tenants/${tenantId}/members`);
    }
    return this.request<TenantMemberDetailResponse[]>('/tenants/me/members');
  }

  public async addTenantMember(dto: AddMemberDto): Promise<ApiResponse<TenantMemberDetailResponse>> {
    return this.request<TenantMemberDetailResponse[]>('/tenants/me/members', {
      method: 'POST',
      body: JSON.stringify(dto),
    }) as unknown as Promise<ApiResponse<TenantMemberDetailResponse>>;
  }

  public async updateTenantMember(memberId: string, dto: UpdateMemberDto): Promise<ApiResponse<TenantMemberDetailResponse>> {
    return this.request<TenantMemberDetailResponse>(`/tenants/me/members/${memberId}`, {
      method: 'PUT',
      body: JSON.stringify(dto),
    });
  }

  public async updateTenantMemberStatus(memberId: string, status: 'ACTIVE' | 'SUSPENDED'): Promise<ApiResponse<TenantMemberDetailResponse>> {
    return this.request<TenantMemberDetailResponse>(`/tenants/me/members/${memberId}/status?status=${status}`, {
      method: 'PATCH',
    });
  }

  public async removeTenantMember(memberId: string): Promise<ApiResponse<void>> {
    return this.request<void>(`/tenants/me/members/${memberId}`, {
      method: 'DELETE',
    });
  }

  public async transferOwnership(dto: TransferOwnershipDto): Promise<ApiResponse<TenantMemberDetailResponse>> {
    return this.request<TenantMemberDetailResponse>('/tenants/me/transfer-ownership', {
      method: 'POST',
      body: JSON.stringify(dto),
    });
  }

  // ==========================================
  // Product Catalog
  // ==========================================
  public async listCategories(active?: boolean, search?: string, page = 0, size = 100): Promise<ApiResponse<PageResponse<CategoryResponse>>> {
    const params = new URLSearchParams();
    if (active !== undefined) params.set('active', String(active));
    if (search) params.set('search', search);
    params.set('page', String(page));
    params.set('size', String(size));
    params.set('sort', 'name,asc');
    return this.request<PageResponse<CategoryResponse>>(`/categories?${params.toString()}`);
  }

  public async getCategory(id: string): Promise<ApiResponse<CategoryResponse>> {
    return this.request<CategoryResponse>(`/categories/${id}`);
  }

  public async createCategory(dto: CreateCategoryDto): Promise<ApiResponse<CategoryResponse>> {
    return this.request<CategoryResponse>('/categories', {
      method: 'POST',
      body: JSON.stringify(dto),
    });
  }

  public async updateCategory(id: string, dto: UpdateCategoryDto): Promise<ApiResponse<CategoryResponse>> {
    return this.request<CategoryResponse>(`/categories/${id}`, {
      method: 'PUT',
      body: JSON.stringify(dto),
    });
  }

  public async deleteCategory(id: string): Promise<ApiResponse<void>> {
    return this.request<void>(`/categories/${id}`, {
      method: 'DELETE',
    });
  }

  public async listUnits(): Promise<ApiResponse<UnitDefinition[]>> {
    return this.request<UnitDefinition[]>('/units');
  }

  public async createUnit(dto: { code: string; name: string; symbol?: string; description?: string }): Promise<ApiResponse<UnitDefinition>> {
    return this.request<UnitDefinition>('/units', {
      method: 'POST',
      body: JSON.stringify(dto),
    });
  }

  public async listProducts(params: {
    categoryId?: string;
    status?: ProductStatus;
    search?: string;
    lowStockOnly?: boolean;
    page?: number;
    size?: number;
  } = {}): Promise<ApiResponse<PageResponse<ProductSummary>>> {
    const q = new URLSearchParams();
    if (params.categoryId) q.set('categoryId', params.categoryId);
    if (params.status) q.set('status', params.status);
    if (params.search) q.set('search', params.search);
    if (params.lowStockOnly !== undefined) q.set('lowStockOnly', String(params.lowStockOnly));
    q.set('page', String(params.page ?? 0));
    q.set('size', String(params.size ?? 20));
    q.set('sort', 'name,asc');
    return this.request<PageResponse<ProductSummary>>(`/products?${q.toString()}`);
  }

  public async getProduct(id: string): Promise<ApiResponse<ProductDetail>> {
    return this.request<ProductDetail>(`/products/${id}`);
  }

  public async createProduct(dto: CreateProductDto): Promise<ApiResponse<ProductDetail>> {
    return this.request<ProductDetail>('/products', {
      method: 'POST',
      body: JSON.stringify(dto),
    });
  }

  public async updateProduct(id: string, dto: UpdateProductDto): Promise<ApiResponse<ProductDetail>> {
    return this.request<ProductDetail>(`/products/${id}`, {
      method: 'PUT',
      body: JSON.stringify(dto),
    });
  }

  public async updateProductSellingPrice(id: string, dto: UpdateSellingPriceDto): Promise<ApiResponse<ProductDetail>> {
    return this.request<ProductDetail>(`/products/${id}/selling-price`, {
      method: 'PATCH',
      body: JSON.stringify(dto),
    });
  }

  public async deleteProduct(id: string): Promise<ApiResponse<void>> {
    return this.request<void>(`/products/${id}`, {
      method: 'DELETE',
    });
  }

  public async listProductConversions(productId: string): Promise<ApiResponse<ProductUnitConversion[]>> {
    return this.request<ProductUnitConversion[]>(`/products/${productId}/conversions`);
  }

  public async addProductConversion(productId: string, dto: CreateProductUnitConversionDto): Promise<ApiResponse<ProductUnitConversion>> {
    return this.request<ProductUnitConversion>(`/products/${productId}/conversions`, {
      method: 'POST',
      body: JSON.stringify(dto),
    });
  }

  public async deleteProductConversion(productId: string, unitId: string): Promise<ApiResponse<void>> {
    return this.request<void>(`/products/${productId}/conversions/${unitId}`, {
      method: 'DELETE',
    });
  }

  // ==========================================
  // Inventory Management & Movements
  // ==========================================
  public async listStockBalances(params: {
    categoryId?: string;
    lowStockOnly?: boolean;
    search?: string;
    page?: number;
    size?: number;
  } = {}): Promise<ApiResponse<PageResponse<StockBalanceResponse>>> {
    const q = new URLSearchParams();
    if (params.categoryId) q.set('categoryId', params.categoryId);
    if (params.lowStockOnly !== undefined) q.set('lowStockOnly', String(params.lowStockOnly));
    if (params.search) q.set('search', params.search);
    q.set('page', String(params.page ?? 0));
    q.set('size', String(params.size ?? 20));
    return this.request<PageResponse<StockBalanceResponse>>(`/inventory/balances?${q.toString()}`);
  }

  public async getProductStockBalance(productId: string): Promise<ApiResponse<StockBalanceResponse>> {
    return this.request<StockBalanceResponse>(`/inventory/balances/${productId}`);
  }

  public async receiveStock(dto: CreateStockReceivingDto): Promise<ApiResponse<StockReceivingResponse>> {
    return this.request<StockReceivingResponse>('/inventory/receipts', {
      method: 'POST',
      body: JSON.stringify(dto),
    });
  }

  public async listStockReceipts(page = 0, size = 20): Promise<ApiResponse<PageResponse<StockReceivingResponse>>> {
    return this.request<PageResponse<StockReceivingResponse>>(`/inventory/receipts?page=${page}&size=${size}&sort=receivedAt,desc`);
  }

  public async getStockReceipt(id: string): Promise<ApiResponse<StockReceivingResponse>> {
    return this.request<StockReceivingResponse>(`/inventory/receipts/${id}`);
  }

  public async adjustStock(dto: CreateStockAdjustmentDto): Promise<ApiResponse<StockAdjustmentResponse>> {
    return this.request<StockAdjustmentResponse>('/inventory/adjustments', {
      method: 'POST',
      body: JSON.stringify(dto),
    });
  }

  public async listStockAdjustments(page = 0, size = 20): Promise<ApiResponse<PageResponse<StockAdjustmentResponse>>> {
    return this.request<PageResponse<StockAdjustmentResponse>>(`/inventory/adjustments?page=${page}&size=${size}&sort=createdAt,desc`);
  }

  public async listStockMovements(params: {
    productId?: string;
    movementType?: MovementType;
    referenceType?: string;
    startDate?: string;
    endDate?: string;
    search?: string;
    page?: number;
    size?: number;
  } = {}): Promise<ApiResponse<PageResponse<StockMovementResponse>>> {
    const q = new URLSearchParams();
    if (params.productId) q.set('productId', params.productId);
    if (params.movementType) q.set('movementType', params.movementType);
    if (params.referenceType) q.set('referenceType', params.referenceType);
    if (params.startDate) q.set('startDate', params.startDate);
    if (params.endDate) q.set('endDate', params.endDate);
    if (params.search) q.set('search', params.search);
    q.set('page', String(params.page ?? 0));
    q.set('size', String(params.size ?? 25));
    q.set('sort', 'createdAt,desc');
    return this.request<PageResponse<StockMovementResponse>>(`/inventory/movements?${q.toString()}`);
  }

  public async getInventoryValuationReport(): Promise<ApiResponse<InventoryValuationReport>> {
    return this.request<InventoryValuationReport>('/inventory/reports/valuation');
  }

  public async getLowStockReport(page = 0, size = 50): Promise<ApiResponse<PageResponse<StockBalanceResponse>>> {
    return this.request<PageResponse<StockBalanceResponse>>(`/inventory/reports/low-stock?page=${page}&size=${size}`);
  }

  // ==========================================
  // Counter Sales & Checkout
  // ==========================================
  public async createSale(dto: CreateSaleRequestDto): Promise<ApiResponse<SaleResponseDto>> {
    return this.request<SaleResponseDto>('/sales', {
      method: 'POST',
      body: JSON.stringify(dto),
    });
  }

  public async listSales(params: {
    startDate?: string;
    endDate?: string;
    status?: string;
    productId?: string;
    search?: string;
    page?: number;
    size?: number;
  } = {}): Promise<ApiResponse<PageResponse<SaleResponseDto>>> {
    const q = new URLSearchParams();
    if (params.startDate) q.set('startDate', params.startDate);
    if (params.endDate) q.set('endDate', params.endDate);
    if (params.status) q.set('status', params.status);
    if (params.productId) q.set('productId', params.productId);
    if (params.search) q.set('search', params.search);
    q.set('page', String(params.page ?? 0));
    q.set('size', String(params.size ?? 20));
    q.set('sort', 'soldAt,desc');
    return this.request<PageResponse<SaleResponseDto>>(`/sales?${q.toString()}`);
  }

  public async getSale(id: string): Promise<ApiResponse<SaleResponseDto>> {
    return this.request<SaleResponseDto>(`/sales/${id}`);
  }

  public async getSaleReceipt(id: string): Promise<ApiResponse<SaleReceiptResponseDto>> {
    return this.request<SaleReceiptResponseDto>(`/sales/${id}/receipt`);
  }

  public async voidSale(id: string, dto: VoidSaleRequestDto): Promise<ApiResponse<SaleResponseDto>> {
    return this.request<SaleResponseDto>(`/sales/${id}/void`, {
      method: 'POST',
      body: JSON.stringify(dto),
    });
  }

  // ==========================================
  // Operational Sales Reports
  // ==========================================
  public async getSalesSummary(startDate?: string, endDate?: string): Promise<ApiResponse<SalesSummaryReportDto>> {
    const q = new URLSearchParams();
    if (startDate) q.set('startDate', startDate);
    if (endDate) q.set('endDate', endDate);
    return this.request<SalesSummaryReportDto>(`/sales/reports/summary?${q.toString()}`);
  }

  public async getDailySales(startDate?: string, endDate?: string): Promise<ApiResponse<DailySalesSummaryDto[]>> {
    const q = new URLSearchParams();
    if (startDate) q.set('startDate', startDate);
    if (endDate) q.set('endDate', endDate);
    return this.request<DailySalesSummaryDto[]>(`/sales/reports/daily?${q.toString()}`);
  }

  // ==========================================
  // Subscriptions & Quotas
  // ==========================================
  public async getSubscription(): Promise<ApiResponse<TenantSubscriptionInfo>> {
    return this.request<TenantSubscriptionInfo>('/me/subscription');
  }

  // ==========================================
  // Inter-Shop Lending & Borrowing
  // ==========================================
  public async createBorrowing(dto: CreateBorrowingRequestDto): Promise<ApiResponse<StockLoanResponse>> {
    return this.request<StockLoanResponse>('/inventory/borrowings', {
      method: 'POST',
      body: JSON.stringify(dto),
    });
  }

  public async listBorrowings(params: {
    status?: string;
    page?: number;
    size?: number;
  } = {}): Promise<ApiResponse<PageResponse<StockLoanResponse>>> {
    const q = new URLSearchParams();
    if (params.status) q.set('status', params.status);
    q.set('page', String(params.page ?? 0));
    q.set('size', String(params.size ?? 20));
    q.set('sort', 'transactionDate,desc');
    return this.request<PageResponse<StockLoanResponse>>(`/inventory/borrowings?${q.toString()}`);
  }

  public async createLoan(dto: CreateLoanRequestDto): Promise<ApiResponse<StockLoanResponse>> {
    return this.request<StockLoanResponse>('/inventory/loans', {
      method: 'POST',
      body: JSON.stringify(dto),
    });
  }

  public async listLoans(params: {
    status?: string;
    page?: number;
    size?: number;
  } = {}): Promise<ApiResponse<PageResponse<StockLoanResponse>>> {
    const q = new URLSearchParams();
    if (params.status) q.set('status', params.status);
    q.set('page', String(params.page ?? 0));
    q.set('size', String(params.size ?? 20));
    q.set('sort', 'transactionDate,desc');
    return this.request<PageResponse<StockLoanResponse>>(`/inventory/loans?${q.toString()}`);
  }

  public async getLoan(id: string): Promise<ApiResponse<StockLoanResponse>> {
    return this.request<StockLoanResponse>(`/inventory/lending/${id}`);
  }

  public async recordLoanReturn(id: string, dto: RecordLoanReturnDto): Promise<ApiResponse<StockLoanResponse>> {
    return this.request<StockLoanResponse>(`/inventory/lending/${id}/returns`, {
      method: 'POST',
      body: JSON.stringify(dto),
    });
  }

  public async cancelLoan(id: string, dto: CancelLoanDto): Promise<ApiResponse<StockLoanResponse>> {
    return this.request<StockLoanResponse>(`/inventory/lending/${id}/cancel`, {
      method: 'POST',
      body: JSON.stringify(dto),
    });
  }

  public async getLendingObligations(): Promise<ApiResponse<LendingObligationsSummary>> {
    return this.request<LendingObligationsSummary>('/inventory/lending/obligations');
  }
}

export const shopApi = new ShopApiClient();
