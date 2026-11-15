// Uniform API envelopes matching Spring Boot backend
export interface ApiResponse<T> {
  success: boolean;
  message?: string;
  data: T;
  timestamp: string;
  correlationId?: string;
}

export interface PageResponse<T> {
  content: T[];
  pageNumber: number;
  pageSize: number;
  totalElements: number;
  totalPages: number;
  first: boolean;
  last: boolean;
  empty?: boolean;
}

// Identity & Session Types
export type UserRole = 'OWNER' | 'MANAGER' | 'STOREKEEPER' | 'CASHIER';

export type ShopPermission =
  | 'PRODUCTS_VIEW'
  | 'PRODUCTS_MANAGE'
  | 'PRICES_MANAGE'
  | 'INVENTORY_RECEIVE'
  | 'INVENTORY_ADJUST'
  | 'INVENTORY_VIEW'
  | 'SALES_CREATE'
  | 'SALES_VIEW'
  | 'SALES_REPORTS_VIEW'
  | 'USERS_MANAGE'
  | 'LENDING_REQUEST'
  | 'LENDING_MANAGE'
  | 'LENDING_VIEW'
  | 'SUBSCRIPTION_VIEW';

export interface TenantMembership {
  id: string;
  tenantId: string;
  tenantCode: string;
  tenantName: string;
  role: UserRole;
  status: 'ACTIVE' | 'INVITED' | 'SUSPENDED';
  permissions?: ShopPermission[];
}

export interface TenantMemberDetailResponse {
  id: string;
  tenantId: string;
  userId: string;
  email: string;
  fullName: string;
  phoneNumber?: string;
  role: UserRole;
  status: 'ACTIVE' | 'INVITED' | 'SUSPENDED';
  permissions?: ShopPermission[];
  createdAt?: string;
}

export interface AddMemberDto {
  email: string;
  fullName: string;
  role: UserRole;
  password?: string;
  phoneNumber?: string;
  permissions?: ShopPermission[];
}

export interface ChangePasswordDto {
  currentPassword: string;
  newPassword: string;
}

export interface UpdateMemberDto {
  role: UserRole;
  status?: 'ACTIVE' | 'INVITED' | 'SUSPENDED';
  permissions?: ShopPermission[];
}

export interface TransferOwnershipDto {
  newOwnerUserId: string;
  currentOwnerPassword: string;
}

export interface UserProfile {
  id: string;
  email: string;
  fullName: string;
  phoneNumber?: string;
  status: 'ACTIVE' | 'SUSPENDED';
  isPlatformAdmin: boolean;
  memberships: TenantMembership[];
  activeTenantId?: string;
}

export interface LoginResponseData {
  accessToken: string;
  tokenType: string;
  expiresIn: number;
  user: {
    id: string;
    email: string;
    fullName: string;
    phoneNumber?: string;
    status: string;
    isPlatformAdmin: boolean;
  };
  memberships: TenantMembership[];
}

export interface ShopTenantDetail {
  id: string;
  code: string;
  name: string;
  registrationNumber?: string;
  vatNumber?: string;
  currency: string;
  timezone: string;
  status: 'ACTIVE' | 'SUSPENDED' | 'TERMINATED';
  phoneNumber?: string;
  email?: string;
  address?: string;
  createdAt?: string;
}

// Category Types
export interface CategoryResponse {
  id: string;
  tenantId: string;
  name: string;
  description?: string;
  active: boolean;
  productCount: number;
  createdAt: string;
}

export interface CreateCategoryDto {
  name: string;
  description?: string;
}

export interface UpdateCategoryDto {
  name: string;
  description?: string;
  active?: boolean;
}

// Unit Definitions & Conversions
export interface UnitDefinition {
  id: string;
  tenantId?: string;
  code: string;
  name: string;
  description?: string;
  symbol?: string;
  systemDefault?: boolean;
}

export interface ProductUnitConversion {
  id: string;
  productId: string;
  unitDefinitionId: string;
  unitCode: string;
  unitName: string;
  toBaseFactor: number;
  isDefaultSaleUnit?: boolean;
  isDefaultPurchaseUnit?: boolean;
  customSellingPrice?: number;
}

export interface CreateProductUnitConversionDto {
  unitDefinitionId: string;
  toBaseFactor: number;
  isDefaultSaleUnit?: boolean;
  isDefaultPurchaseUnit?: boolean;
  customSellingPrice?: number;
}

// Product Types
export type ProductStatus = 'ACTIVE' | 'INACTIVE' | 'ARCHIVED';

export interface ProductSummary {
  id: string;
  sku: string;
  name: string;
  categoryId?: string;
  categoryName?: string;
  baseUnitId: string;
  baseUnitCode: string;
  baseUnitName: string;
  sellingPrice: number;
  costPrice?: number; // Redacted for cashier
  reorderThreshold: number;
  currentStock: number;
  quantityOnHand?: number;
  lowStock: boolean;
  isLowStock?: boolean;
  status: ProductStatus;
  barcode?: string;
  brand?: string;
  imageUrl?: string;
}

export interface ProductDetail extends ProductSummary {
  description?: string;
  averageCost?: number;
  totalValuation?: number;
  conversions: ProductUnitConversion[];
  createdAt: string;
  updatedAt?: string;
}

export interface CreateProductDto {
  sku: string;
  name: string;
  description?: string;
  categoryId?: string;
  baseUnitId: string;
  costPrice?: number;
  sellingPrice?: number;
  reorderThreshold?: number;
  imageUrl?: string;
  imageStorageKey?: string;
  barcode?: string;
  brand?: string;
  initialQuantity?: number;
}

export interface UpdateProductDto {
  name: string;
  description?: string;
  categoryId?: string;
  baseUnitId?: string;
  costPrice?: number;
  sellingPrice?: number;
  reorderThreshold?: number;
  status?: ProductStatus;
  imageUrl?: string;
  imageStorageKey?: string;
  barcode?: string;
  brand?: string;
}

// Inventory Balances & Movements
export interface StockBalanceResponse {
  productId: string;
  sku: string;
  productSku?: string;
  productName: string;
  categoryId?: string;
  categoryName?: string;
  baseUnitId: string;
  baseUnitCode: string;
  baseUnitName: string;
  quantityOnHand: number;
  averageCost?: number; // Redacted for Cashier
  totalValuation?: number; // Redacted for Cashier
  reorderThreshold: number;
  isLowStock: boolean;
  lastCountedAt?: string;
}

export type MovementType = 'RECEIPT' | 'SALE' | 'ADJUSTMENT' | 'RETURN' | 'TRANSFER_IN' | 'TRANSFER_OUT';

export interface StockMovementResponse {
  id: string;
  productId: string;
  productSku: string;
  productName: string;
  baseUnitCode: string;
  movementType: MovementType;
  quantityChange: number;
  balanceAfter: number;
  unitCost?: number;
  unitCostSnapshot?: number;
  costAtTimeOfTransaction?: number;
  referenceType?: string;
  referenceId?: string;
  notes?: string;
  actorId?: string;
  actorEmail?: string;
  createdAt: string;
}

// Inbound Stock Receipts
export interface CreateStockReceivingItemDto {
  productId: string;
  receivedUnitId: string;
  receivedQuantity: number;
  unitCost: number;
}

export interface CreateStockReceivingDto {
  supplierName: string;
  deliveryNoteRef?: string;
  notes?: string;
  idempotencyKey?: string;
  receivedAt?: string;
  items: CreateStockReceivingItemDto[];
}

export interface StockReceivingItemResponse {
  id: string;
  productId: string;
  productSku: string;
  productName: string;
  receivedUnitId: string;
  receivedUnitCode: string;
  receivedQuantity: number;
  toBaseFactor: number;
  baseQuantity: number;
  baseUnitCode?: string;
  unitCost: number;
  totalCost: number;
}

export interface StockReceivingResponse {
  id: string;
  receivingNumber: string;
  supplierName: string;
  deliveryNoteRef?: string;
  notes?: string;
  receivedAt: string;
  totalCost: number;
  receivedByName: string;
  items: StockReceivingItemResponse[];
}

// Stock Adjustments
export type AdjustmentReason = 'DAMAGED' | 'EXPIRED' | 'BREAKAGE' | 'COUNT_DISCREPANCY' | 'CORRECTION' | 'OTHER';

export interface CreateStockAdjustmentItemDto {
  productId: string;
  quantityChange?: number;
  targetQuantity?: number;
  unitId?: string;
  enteredQuantity?: number;
  unitCost?: number;
}

export interface CreateStockAdjustmentDto {
  reasonCode: string;
  notes?: string;
  idempotencyKey?: string;
  items: CreateStockAdjustmentItemDto[];
}

export interface StockAdjustmentItemResponse {
  id: string;
  productId: string;
  productSku: string;
  productName: string;
  adjustmentType?: string;
  enteredQuantity?: number;
  unitId?: string;
  unitCode?: string;
  conversionFactor?: number;
  baseQuantity?: number;
  baseUnitCode: string;
  quantityBefore: number;
  quantityChange: number;
  quantityAfter: number;
  unitCost?: number;
  valuationImpact?: number;
  financialValue?: number;
}

export interface StockAdjustmentResponse {
  id: string;
  adjustmentNumber: string;
  reasonCode: string;
  notes?: string;
  createdAt?: string;
  adjustedAt?: string;
  createdByName?: string;
  adjustedByName?: string;
  items: StockAdjustmentItemResponse[];
}

// Sales & Checkout
export type SaleStatus = 'COMPLETED' | 'VOIDED';
export type PaymentMethod = 'CASH' | 'CARD' | 'BANK_TRANSFER' | 'CREDIT';

export interface CreateSaleItemRequestDto {
  productId: string;
  unitDefinitionId: string;
  quantity: number;
  unitPrice?: number;
  discountAmount?: number;
}

export interface CreateSaleRequestDto {
  customerName?: string;
  customerPhone?: string;
  notes?: string;
  discountAmount?: number;
  idempotencyKey?: string;
  items: CreateSaleItemRequestDto[];
}

export interface SaleItemResponseDto {
  id: string;
  productId: string;
  productSku: string;
  productName: string;
  soldUnitId?: string;
  unitDefinitionId?: string;
  unitCode: string;
  soldUnitCode?: string;
  quantity: number;
  soldQuantity?: number;
  toBaseFactor?: number;
  conversionFactor?: number;
  baseQuantity: number;
  unitPrice: number;
  discountAmount: number;
  lineTotal: number;
  totalPrice?: number;
  costPriceSnapshot?: number; // Redacted for cashier
  unitCost?: number;
  totalCost?: number;
  grossProfit?: number;
}

export interface SaleResponseDto {
  id: string;
  invoiceNumber: string;
  soldAt: string;
  cashierId: string;
  cashierName: string;
  customerName?: string;
  customerPhone?: string;
  subtotal: number;
  discountAmount: number;
  taxAmount: number;
  totalAmount: number;
  totalRevenue?: number;
  totalCost?: number;
  grossProfit?: number;
  notes?: string;
  status: SaleStatus;
  voidReason?: string;
  voidedAt?: string;
  voidedByName?: string;
  items: SaleItemResponseDto[];
}

export interface SaleReceiptItemDto {
  productSku: string;
  productName: string;
  unitCode: string;
  quantity: number;
  unitPrice: number;
  discountAmount: number;
  lineTotal: number;
}

export interface SaleReceiptResponseDto {
  shopName: string;
  shopPhone?: string;
  shopEmail?: string;
  shopAddress?: string;
  shopCity?: string;
  shopCountry?: string;
  invoiceNumber: string;
  soldAt: string;
  cashierName: string;
  customerName?: string;
  customerPhone?: string;
  items: SaleReceiptItemDto[];
  subtotal: number;
  discountAmount: number;
  taxAmount: number;
  totalAmount: number;
  notes?: string;
  status: SaleStatus;
  voidReason?: string;
  voidedAt?: string;
  disclaimer: string;
}

export interface VoidSaleRequestDto {
  reason: string;
}

// Operational & Valuation Reports
export interface SalesSummaryReportDto {
  totalSalesCount: number;
  totalSalesAmount?: number;
  totalRevenue: number;
  totalCostOfGoodsSold?: number;
  totalCost?: number; // Redacted for cashier
  grossProfit?: number; // Redacted for cashier
  grossMarginPercentage?: number; // Redacted for cashier
  averageBasketValue: number;
  voidedSalesCount: number;
  voidedSalesAmount: number;
}

export interface DailySalesSummaryDto {
  date: string;
  salesCount: number;
  salesAmount?: number;
  revenue: number;
  costOfGoodsSold?: number;
  cost?: number; // Redacted for cashier
  grossProfit?: number; // Redacted for cashier
}

export interface CategoryValuationSummary {
  categoryId?: string;
  categoryName: string;
  productCount: number;
  totalQuantity: number;
  totalValuation: number;
}

export interface InventoryValuationReport {
  totalProducts: number;
  totalSkus?: number;
  totalStockUnits: number;
  totalValuation: number;
  categories: CategoryValuationSummary[];
  categoryBreakdowns?: CategoryValuationSummary[];
}

// Tenant Subscriptions & Quotas
export interface TenantSubscriptionInfo {
  id: string;
  tenantId: string;
  planId: string;
  planCode: string;
  planName: string;
  status: 'ACTIVE' | 'TRIALING' | 'PAST_DUE' | 'CANCELED' | 'EXPIRED';
  currentPeriodStart: string;
  currentPeriodEnd: string;
  cancelAtPeriodEnd: boolean;
  daysRemaining: number;
  productLimit?: number | null;
  activeProductsCount?: number;
  userLimit?: number | null;
  activeUsersCount?: number;
  limits: {
    maxProducts: number;
    maxUsers: number;
    allowApiAccess: boolean;
    allowAdvancedReports: boolean;
  };
  usage: {
    productCount: number;
    userCount: number;
  };
}

// System Health
export interface HealthResponse {
  status: 'UP' | 'DOWN';
  application: string;
  timestamp: string;
  database: string;
  environment: string;
}

// Pricing Types
export interface UpdateSellingPriceDto {
  sellingPrice: number;
}

// Lending & Borrowing Types
export type LoanDirection = 'BORROW' | 'LOAN';
export type LoanStatus = 'ACTIVE' | 'PARTIALLY_RETURNED' | 'RETURNED' | 'CANCELLED';

export interface StockLoanReturnResponse {
  id: string;
  stockLoanId: string;
  unitId: string;
  unitCode: string;
  quantity: number;
  baseQuantity: number;
  returnDate: string;
  receivedByUserId: string;
  receivedByUserName?: string;
  notes?: string;
  createdAt: string;
}

export interface StockLoanResponse {
  id: string;
  tenantId: string;
  loanReference: string;
  direction: LoanDirection;
  status: LoanStatus;
  counterpartyName: string;
  counterpartyContactPerson?: string;
  counterpartyPhone?: string;
  counterpartyAddress?: string;
  productId: string;
  productSku: string;
  productName: string;
  unitId: string;
  unitCode: string;
  conversionFactor: number;
  initialQuantity: number;
  initialBaseQuantity: number;
  returnedQuantity: number;
  returnedBaseQuantity: number;
  outstandingBaseQuantity: number;
  unitCostSnapshot?: number;
  transactionDate: string;
  expectedReturnDate?: string;
  notes?: string;
  cancellationReason?: string;
  createdByUserId: string;
  createdAt: string;
  updatedAt: string;
  returns: StockLoanReturnResponse[];
}

export interface CreateBorrowingRequestDto {
  counterpartyName: string;
  counterpartyContactPerson?: string;
  counterpartyPhone?: string;
  counterpartyAddress?: string;
  productId: string;
  unitDefinitionId: string;
  quantity: number;
  transactionDate?: string;
  expectedReturnDate?: string;
  notes?: string;
}

export interface CreateLoanRequestDto {
  counterpartyName: string;
  counterpartyContactPerson?: string;
  counterpartyPhone?: string;
  counterpartyAddress?: string;
  productId: string;
  unitDefinitionId: string;
  quantity: number;
  transactionDate?: string;
  expectedReturnDate?: string;
  notes?: string;
}

export interface RecordLoanReturnDto {
  quantity: number;
  unitDefinitionId?: string;
  returnDate?: string;
  notes?: string;
}

export interface CancelLoanDto {
  reason: string;
}

export interface ProductObligation {
  productId: string;
  productSku: string;
  productName: string;
  baseUnitCode: string;
  totalBorrowedBaseQuantity: number;
  totalLoanedBaseQuantity: number;
  netObligationBaseQuantity: number;
}

export interface LendingObligationsSummary {
  totalActiveBorrowings: number;
  totalActiveLoans: number;
  totalOutstandingBorrowedBaseUnits: number;
  totalOutstandingLoanedBaseUnits: number;
  productObligations: ProductObligation[];
}
