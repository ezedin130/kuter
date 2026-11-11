import { describe, it, expect, beforeEach, vi } from 'vitest';
import { ApiClient } from '../api/client';

describe('Subscription Plans Management Integration Contract', () => {
  let client: ApiClient;

  beforeEach(() => {
    client = new ApiClient('http://test-kuter.local/api/v1');
    client.setToken('admin-valid-token');
    vi.restoreAllMocks();
  });

  it('queries subscription plans directory', async () => {
    const fetchMock = vi.fn().mockResolvedValue({
      ok: true,
      json: async () => ({
        success: true,
        data: {
          content: [
            {
              id: 'plan-starter',
              code: 'starter',
              name: 'Starter Yard',
              price: 49.0,
              currency: 'USD',
              billingInterval: 'MONTHLY',
              productLimit: 500,
              userLimit: 3,
              isPublic: true,
              isActive: true,
            },
          ],
          totalElements: 1,
        },
      }),
    });
    globalThis.fetch = fetchMock;

    const res = await client.listPlans({ isActive: true });
    expect(res.content.length).toBe(1);
    expect(res.content[0].code).toBe('starter');
    expect(res.content[0].price).toBe(49.0);
  });

  it('submits valid create plan payload with quota entitlements', async () => {
    const fetchMock = vi.fn().mockResolvedValue({
      ok: true,
      json: async () => ({
        success: true,
        data: {
          id: 'plan-enterprise',
          code: 'enterprise',
          name: 'Enterprise Depot',
          price: 299.0,
          currency: 'USD',
          billingInterval: 'MONTHLY',
          productLimit: 10000,
          userLimit: 25,
          trialDays: 30,
        },
      }),
    });
    globalThis.fetch = fetchMock;

    const created = await client.createPlan({
      code: 'enterprise',
      name: 'Enterprise Depot',
      price: 299.0,
      currency: 'USD',
      billingInterval: 'MONTHLY',
      productLimit: 10000,
      userLimit: 25,
      trialDays: 30,
      isPublic: true,
      isActive: true,
    });

    expect(fetchMock).toHaveBeenCalledTimes(1);
    const [url, init] = fetchMock.mock.calls[0];
    expect(url).toBe('http://test-kuter.local/api/v1/subscription-plans');
    expect(init.method).toBe('POST');
    const body = JSON.parse(init.body as string);
    expect(body.code).toBe('enterprise');
    expect(body.productLimit).toBe(10000);
    expect(created.id).toBe('plan-enterprise');
  });

  it('handles plan deletion rejection when referenced by active subscriptions', async () => {
    const fetchMock = vi.fn().mockResolvedValue({
      ok: false,
      status: 409,
      statusText: 'Conflict',
      json: async () => ({
        success: false,
        message: 'Cannot delete plan referenced by active tenant subscriptions',
      }),
    });
    globalThis.fetch = fetchMock;

    await expect(client.deletePlan('plan-in-use')).rejects.toThrow(
      'Cannot delete plan referenced by active tenant subscriptions'
    );
  });
});
