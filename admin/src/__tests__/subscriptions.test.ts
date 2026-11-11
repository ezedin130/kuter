import { describe, it, expect, beforeEach, vi } from 'vitest';
import { ApiClient } from '../api/client';

describe('Tenant Subscriptions Monitoring Integration Contract', () => {
  let client: ApiClient;

  beforeEach(() => {
    client = new ApiClient('http://test-kuter.local/api/v1');
    client.setToken('admin-valid-token');
    vi.restoreAllMocks();
  });

  it('queries subscription summary KPI metrics', async () => {
    const fetchMock = vi.fn().mockResolvedValue({
      ok: true,
      json: async () => ({
        success: true,
        data: {
          totalSubscriptions: 24,
          activeSubscriptions: 20,
          trialingSubscriptions: 2,
          expiredSubscriptions: 1,
          suspendedSubscriptions: 1,
          cancelledSubscriptions: 0,
          estimatedMonthlyRevenue: 3450.0,
          currency: 'USD',
        },
      }),
    });
    globalThis.fetch = fetchMock;

    const summary = await client.getSubscriptionSummary();
    expect(summary.activeSubscriptions).toBe(20);
    expect(summary.estimatedMonthlyRevenue).toBe(3450.0);
  });

  it('submits extend subscription request with mandatory reason', async () => {
    const fetchMock = vi.fn().mockResolvedValue({
      ok: true,
      json: async () => ({
        success: true,
        data: {
          id: 'sub-1',
          tenantName: 'Al-Madina Materials',
          status: 'ACTIVE',
          daysRemaining: 45,
        },
      }),
    });
    globalThis.fetch = fetchMock;

    const result = await client.extendSubscription('sub-1', {
      days: 30,
      reason: 'Confirmed promotional extension with owner',
    });

    expect(fetchMock).toHaveBeenCalledTimes(1);
    const [url, init] = fetchMock.mock.calls[0];
    expect(url).toBe('http://test-kuter.local/api/v1/admin/subscriptions/sub-1/extend');
    expect(init.method).toBe('POST');
    const body = JSON.parse(init.body as string);
    expect(body.days).toBe(30);
    expect(body.reason).toBe('Confirmed promotional extension with owner');
    expect(result.daysRemaining).toBe(45);
  });

  it('submits plan switch payload', async () => {
    const fetchMock = vi.fn().mockResolvedValue({
      ok: true,
      json: async () => ({
        success: true,
        data: {
          id: 'sub-1',
          planCode: 'enterprise',
          planName: 'Enterprise Depot',
          status: 'ACTIVE',
        },
      }),
    });
    globalThis.fetch = fetchMock;

    const result = await client.changeSubscriptionPlan('sub-1', {
      newPlanId: 'plan-enterprise',
      reason: 'Owner requested quota expansion',
    });

    expect(fetchMock).toHaveBeenCalledTimes(1);
    const [url, init] = fetchMock.mock.calls[0];
    expect(url).toBe('http://test-kuter.local/api/v1/admin/subscriptions/sub-1/change-plan');
    const body = JSON.parse(init.body as string);
    expect(body.newPlanId).toBe('plan-enterprise');
    expect(result.planCode).toBe('enterprise');
  });

  it('fetches subscription lifecycle history ledger', async () => {
    const fetchMock = vi.fn().mockResolvedValue({
      ok: true,
      json: async () => ({
        success: true,
        data: [
          {
            id: 'hist-1',
            action: 'SUBSCRIPTION_EXTENDED',
            fromStatus: 'ACTIVE',
            toStatus: 'ACTIVE',
            reason: 'Extended by 30 days',
            actorEmail: 'admin@kuterplatform.com',
            createdAt: '2026-10-29T10:30:00Z',
          },
        ],
      }),
    });
    globalThis.fetch = fetchMock;

    const history = await client.getSubscriptionHistory('sub-1');
    expect(history.length).toBe(1);
    expect(history[0].action).toBe('SUBSCRIPTION_EXTENDED');
    expect(history[0].actorEmail).toBe('admin@kuterplatform.com');
  });
});
