export type BillingInterval = 'MONTHLY' | 'YEARLY';

export type OnboardingStatus = 'PENDING' | 'APPROVED' | 'REJECTED' | 'MORE_INFO_REQUESTED';

export interface PublicSubscriptionPlanDto {
  id: string;
  code: string;
  name: string;
  description: string;
  price: number;
  currency: string;
  billingInterval: BillingInterval;
  productLimit: number;
  userLimit: number;
  branchLimit: number;
  trialDays: number;
  featuresSummary?: string;
}

export interface CreateOnboardingRequestDto {
  shopName: string;
  ownerFullName: string;
  ownerEmail: string;
  ownerPhone?: string;
  requestedPlanCode: string;
  preferredShopCode?: string;
  businessAddress?: string;
  currency?: string;
  applicantNotes?: string;
  password?: string;
}

export interface OnboardingPublicResponse {
  referenceCode: string;
  shopName: string;
  ownerEmail: string;
  requestedPlanCode: string;
  status: OnboardingStatus;
  createdAt: string;
}

export interface ApiResponse<T> {
  success: boolean;
  message?: string;
  data: T;
  timestamp?: string;
  correlationId?: string;
}

export interface ApiErrorResponse {
  success: boolean;
  message: string;
  errorCode?: string;
  timestamp?: string;
  fieldErrors?: Record<string, string>;
}
