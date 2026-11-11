/**
 * Global and Domain TypeScript definitions for Kuter Platform Administration.
 * Fully aligned with the frozen Spring Boot backend API contracts.
 */

// ==========================================
// Generic API Envelopes
// ==========================================

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
  last: boolean;
  first: boolean;
  empty: boolean;
}

export interface HealthResponse {
  status: 'UP' | 'DOWN';
  application: string;
  timestamp: string;
  database: string;
  environment: string;
}

export interface AppInfo {
  application: string;
  version: string;
  timestamp: string;
  profiles: string[];
}

// ==========================================
// Authentication & Identity
// ==========================================

export type UserStatus = 'ACTIVE' | 'SUSPENDED' | 'PENDING';

export interface UserSummary {
  id: string;
  email: string;
  fullName: string;
  phoneNumber?: string;
  isPlatformAdmin: boolean;
  status: UserStatus;
}

export interface TenantMembership {
  id: string;
  tenantId: string;
  tenantCode: string;
  tenantName: string;
  role: 'OWNER' | 'MANAGER' | 'CASHIER';
  status: string;
}

export interface AuthResponse {
  accessToken: string;
  tokenType: string;
  expiresIn: number;
  user: UserSummary;
  memberships: TenantMembership[];
}

export interface UserProfileResponse {
  id: string;
  email: string;
  fullName: string;
  phoneNumber?: string;
  isPlatformAdmin: boolean;
  status: UserStatus;
  memberships: TenantMembership[];
  activeTenantId?: string;
}

// ==========================================
// Platform Overview Metrics
// ==========================================

export interface PlatformOverviewMetrics {
  totalTenants: number;
  activeTenants: number;
  suspendedTenants: number;
  pendingOnboardingRequests: number;

  totalSubscriptions: number;
  activeSubscriptions: number;
  trialingSubscriptions: number;
  expiredSubscriptions: number;
  suspendedSubscriptions: number;
  cancelledSubscriptions: number;

  estimatedMonthlyRevenue: number;
  currency: string;
  generatedAt: string;
}

// ==========================================
// Onboarding Requests
// ==========================================

export type OnboardingStatus = 'PENDING' | 'APPROVED' | 'REJECTED' | 'CANCELLED';

export interface OnboardingDetailResponse {
  id: string;
  referenceCode: string;
  shopName: string;
  ownerFullName: string;
  ownerEmail: string;
  ownerPhone: string;
  requestedPlanCode?: string;
  preferredShopCode?: string;
  businessAddress?: string;
  currency?: string;
  applicantNotes?: string;
  status: OnboardingStatus;
  reviewerId?: string;
  reviewerNotes?: string;
  reviewedAt?: string;
  createdTenantId?: string;
  createdAt: string;
}

export interface ApproveOnboardingDto {
  customShopCode?: string;
  reviewerNotes?: string;
}

export interface RejectOnboardingDto {
  reason: string;
}

export interface ApprovalResultDto {
  onboardingRequestId: string;
  tenantId: string;
  tenantCode: string;
  ownerUserId: string;
  ownerEmail: string;
  subscriptionId?: string;
  planCode?: string;
  status: string;
  message: string;
}

// ==========================================
// Tenants
// ==========================================

export type TenantStatus = 'ACTIVE' | 'SUSPENDED' | 'INACTIVE';

export interface TenantSummaryResponse {
  id: string;
  code: string;
  name: string;
  status: TenantStatus;
  currency: string;
  timezone: string;
  ownerEmail?: string;
  ownerFullName?: string;
  activePlanCode?: string;
  subscriptionStatus?: string;
  createdAt: string;
}

export interface TenantDetailResponse {
  id: string;
  code: string;
  name: string;
  status: TenantStatus;
  currency: string;
  timezone: string;
  taxIdentificationNumber?: string;
  registrationNumber?: string;
  createdAt: string;
  updatedAt: string;
}

export interface TenantStatusDetailDto {
  tenantId: string;
  code: string;
  name: string;
  operationalStatus: TenantStatus;
  createdAt: string;
  onboardingReference?: string;

  subscriptionId?: string;
  planCode?: string;
  planName?: string;
  lifecycleStatus?: SubscriptionStatus;
  effectiveStatus?: SubscriptionStatus;
  currentPeriodStart?: string;
  currentPeriodEnd?: string;
  trialStart?: string;
  trialEnd?: string;
  daysRemaining?: number;
  cancelAtPeriodEnd?: boolean;
}

export interface CreateTenantDto {
  code: string;
  name: string;
  currency: string;
  timezone: string;
  initialOwnerEmail: string;
  initialOwnerFullName: string;
  initialOwnerPhone?: string;
  initialOwnerPassword?: string;
  planCode?: string;
}

export interface UpdateTenantDto {
  name: string;
  currency: string;
  timezone: string;
  taxIdentificationNumber?: string;
  registrationNumber?: string;
}

export interface UpdateTenantStatusDto {
  status: TenantStatus;
  reason?: string;
}

// ==========================================
// Subscription Plans
// ==========================================

export type BillingInterval = 'MONTHLY' | 'ANNUAL';

export interface SubscriptionPlanDto {
  id: string;
  code: string;
  name: string;
  description?: string;
  price: number;
  currency: string;
  billingInterval: BillingInterval;
  productLimit?: number;
  userLimit?: number;
  branchLimit?: number;
  trialDays: number;
  isPublic: boolean;
  isActive: boolean;
  featuresSummary?: string;
  createdAt: string;
  updatedAt: string;
}

export interface CreateSubscriptionPlanDto {
  code: string;
  name: string;
  description?: string;
  price: number;
  currency?: string;
  billingInterval: BillingInterval;
  productLimit?: number;
  userLimit?: number;
  branchLimit?: number;
  trialDays?: number;
  isPublic?: boolean;
  isActive?: boolean;
  featuresSummary?: string;
}

export interface UpdateSubscriptionPlanDto {
  name: string;
  description?: string;
  price: number;
  currency?: string;
  billingInterval: BillingInterval;
  productLimit?: number;
  userLimit?: number;
  branchLimit?: number;
  trialDays?: number;
  isPublic?: boolean;
  isActive?: boolean;
  featuresSummary?: string;
}

// ==========================================
// Tenant Subscriptions
// ==========================================

export type SubscriptionStatus =
  | 'PENDING'
  | 'TRIALING'
  | 'ACTIVE'
  | 'EXPIRED'
  | 'SUSPENDED'
  | 'CANCELLED'
  | 'PAST_DUE';

export interface AdminSubscriptionDetailDto {
  id: string;
  tenantId: string;
  tenantCode: string;
  tenantName: string;
  planId: string;
  planCode: string;
  planName: string;
  status: SubscriptionStatus;
  effectiveStatus: SubscriptionStatus;
  billingInterval: BillingInterval;
  currency: string;
  price: number;
  currentPeriodStart: string;
  currentPeriodEnd: string;
  trialStart?: string;
  trialEnd?: string;
  daysRemaining: number;
  cancelAtPeriodEnd: boolean;
  cancelledAt?: string;
  cancelReason?: string;
  suspendedAt?: string;
  suspensionReason?: string;
  productLimit?: number;
  userLimit?: number;
  featuresSummary?: string;
  version: number;
  createdAt: string;
  updatedAt: string;
}

export interface SubscriptionSummaryDto {
  totalSubscriptions: number;
  activeSubscriptions: number;
  trialingSubscriptions: number;
  expiredSubscriptions: number;
  suspendedSubscriptions: number;
  cancelledSubscriptions: number;
  estimatedMonthlyRevenue: number;
  currency: string;
}

export interface SubscriptionHistoryDto {
  id: string;
  subscriptionId: string;
  tenantId: string;
  action: string;
  fromStatus?: string;
  toStatus: string;
  reason?: string;
  actorId?: string;
  actorEmail?: string;
  createdAt: string;
}

export interface AssignSubscriptionDto {
  tenantId: string;
  planId: string;
  startTrial?: boolean;
  trialDays?: number;
}

export interface ExtendSubscriptionDto {
  days: number;
  reason: string;
}

export interface ChangePlanDto {
  newPlanId: string;
  immediate?: boolean;
  reason?: string;
}

export interface SuspendSubscriptionDto {
  reason: string;
}

export interface CancelSubscriptionDto {
  cancelAtPeriodEnd: boolean;
  reason: string;
}

// ==========================================
// Audit Logs
// ==========================================

export interface AuditLogResponseDto {
  id: string;
  tenantId?: string;
  actorId?: string;
  actorEmail?: string;
  action: string;
  entityType?: string;
  entityId?: string;
  details?: string;
  ipAddress?: string;
  correlationId?: string;
  outcome: string;
  createdAt: string;
}
