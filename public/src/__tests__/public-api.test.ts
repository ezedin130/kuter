import { describe, it, expect, beforeEach, vi } from 'vitest';
import { PublicApiClient } from '../api/client';
import { CreateOnboardingRequestDto } from '../types/api';

describe('PublicApiClient Integration Contract', () => {
  let client: PublicApiClient;

  beforeEach(() => {
    client = new PublicApiClient('http://test-kuter.local/api/v1');
    vi.restoreAllMocks();
  });

  describe('getPublicPlans', () => {
    it('successfully queries public subscription plans directory', async () => {
      const mockPlans = [
        {
          id: 'plan-1',
          code: 'starter',
          name: 'Starter',
          description: 'Essential dual-unit inventory control.',
          price: 49.0,
          currency: 'USD',
          billingInterval: 'MONTHLY',
          productLimit: 500,
          userLimit: 3,
          branchLimit: 1,
          trialDays: 14,
        },
        {
          id: 'plan-2',
          code: 'growth',
          name: 'Growth',
          description: 'High volume materials distribution.',
          price: 119.0,
          currency: 'USD',
          billingInterval: 'MONTHLY',
          productLimit: 2500,
          userLimit: 10,
          branchLimit: 3,
          trialDays: 14,
        },
      ];

      const fetchMock = vi.fn().mockResolvedValue({
        ok: true,
        json: async () => ({
          success: true,
          message: 'Public subscription plans retrieved successfully',
          data: mockPlans,
        }),
      });
      globalThis.fetch = fetchMock;

      const plans = await client.getPublicPlans();
      expect(fetchMock).toHaveBeenCalledTimes(1);
      const [url, init] = fetchMock.mock.calls[0];
      expect(url).toBe('http://test-kuter.local/api/v1/public/subscription-plans');
      expect(init?.headers).toEqual(
        expect.objectContaining({
          'Content-Type': 'application/json',
          Accept: 'application/json',
        })
      );
      expect(plans.length).toBe(2);
      expect(plans[0].code).toBe('starter');
      expect(plans[1].productLimit).toBe(2500);
    });

    it('propagates error message when backend returns an error payload', async () => {
      const fetchMock = vi.fn().mockResolvedValue({
        ok: false,
        status: 500,
        statusText: 'Internal Server Error',
        json: async () => ({
          success: false,
          message: 'Database query failed during plans lookup',
        }),
      });
      globalThis.fetch = fetchMock;

      await expect(client.getPublicPlans()).rejects.toThrow(
        'Database query failed during plans lookup'
      );
    });
  });

  describe('submitOnboardingRequest', () => {
    it('submits valid shop registration payload and receives reference code', async () => {
      const fetchMock = vi.fn().mockResolvedValue({
        ok: true,
        status: 201,
        json: async () => ({
          success: true,
          message: 'Registration request submitted successfully',
          data: {
            referenceCode: 'REQ-2026-00088',
            shopName: 'Atlas Timber & Iron',
            ownerEmail: 'owner@atlastimber.com',
            requestedPlanCode: 'growth',
            status: 'PENDING',
            createdAt: '2026-10-29T14:00:00Z',
          },
        }),
      });
      globalThis.fetch = fetchMock;

      const payload: CreateOnboardingRequestDto = {
        shopName: 'Atlas Timber & Iron',
        ownerFullName: 'David Atlas',
        ownerEmail: 'owner@atlastimber.com',
        ownerPhone: '+1 555 0199',
        requestedPlanCode: 'growth',
        preferredShopCode: 'atlas-timber',
        businessAddress: 'Industrial Zone Yard 4',
        currency: 'USD',
        applicantNotes: 'Migrating from spreadsheet catalog of 1,200 products',
      };

      const result = await client.submitOnboardingRequest(payload);
      expect(fetchMock).toHaveBeenCalledTimes(1);
      const [url, init] = fetchMock.mock.calls[0];
      expect(url).toBe('http://test-kuter.local/api/v1/onboarding-requests');
      expect(init?.method).toBe('POST');
      const body = JSON.parse(init?.body as string);
      expect(body.shopName).toBe('Atlas Timber & Iron');
      expect(body.ownerEmail).toBe('owner@atlastimber.com');
      expect(body.currency).toBe('USD');

      expect(result.referenceCode).toBe('REQ-2026-00088');
      expect(result.status).toBe('PENDING');
    });

    it('handles 409 conflict when duplicate email request is pending', async () => {
      const fetchMock = vi.fn().mockResolvedValue({
        ok: false,
        status: 409,
        statusText: 'Conflict',
        json: async () => ({
          success: false,
          message: 'An active onboarding request already exists for this email address',
        }),
      });
      globalThis.fetch = fetchMock;

      const payload: CreateOnboardingRequestDto = {
        shopName: 'Duplicate Yard',
        ownerFullName: 'Duplicate User',
        ownerEmail: 'duplicate@test.com',
        requestedPlanCode: 'starter',
      };

      await expect(client.submitOnboardingRequest(payload)).rejects.toThrow(
        'An active onboarding request already exists for this email address'
      );
    });
  });

  describe('getOnboardingStatus', () => {
    it('queries application status by reference code and normalizes case', async () => {
      const fetchMock = vi.fn().mockResolvedValue({
        ok: true,
        json: async () => ({
          success: true,
          data: {
            referenceCode: 'REQ-2026-00001',
            shopName: 'Nile Valley Concrete & Sand',
            ownerEmail: 'ahmed@nileconcrete.com',
            requestedPlanCode: 'growth',
            status: 'PENDING',
            createdAt: '2026-10-29T13:02:05.636017Z',
          },
        }),
      });
      globalThis.fetch = fetchMock;

      const status = await client.getOnboardingStatus('  req-2026-00001  ');
      expect(fetchMock).toHaveBeenCalledTimes(1);
      const [url] = fetchMock.mock.calls[0];
      expect(url).toBe('http://test-kuter.local/api/v1/onboarding-requests/status/REQ-2026-00001');
      expect(status.shopName).toBe('Nile Valley Concrete & Sand');
      expect(status.status).toBe('PENDING');
    });

    it('rejects with 404 when reference does not exist', async () => {
      const fetchMock = vi.fn().mockResolvedValue({
        ok: false,
        status: 404,
        statusText: 'Not Found',
        json: async () => ({
          success: false,
          message: 'Onboarding request not found with reference REQ-9999-00000',
        }),
      });
      globalThis.fetch = fetchMock;

      await expect(client.getOnboardingStatus('REQ-9999-00000')).rejects.toThrow(
        'Onboarding request not found with reference REQ-9999-00000'
      );
    });
  });
});
