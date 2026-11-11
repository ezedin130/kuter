import { describe, it, expect, beforeEach, vi } from 'vitest';
import { ShopApiClient } from '../api/client';

describe('Shop Authentication & Tenant Session', () => {
  let client: ShopApiClient;

  beforeEach(() => {
    client = new ShopApiClient('http://localhost:8080/api/v1');
    localStorage.clear();
    vi.restoreAllMocks();
  });

  it('authenticates shop owner and stores token and active tenant ID', async () => {
    const fetchMock = vi.fn().mockResolvedValue({
      ok: true,
      headers: new Headers({ 'content-type': 'application/json' }),
      json: async () => ({
        success: true,
        data: {
          accessToken: 'shop-jwt-token-9988',
          tokenType: 'Bearer',
          expiresIn: 86400,
          user: {
            id: 'user-amoudi-1',
            email: 'owner@amoudi.com',
            fullName: 'Tariq Al-Amoudi',
            status: 'ACTIVE',
            isPlatformAdmin: false,
          },
          memberships: [
            {
              id: 'mem-1',
              tenantId: 'tenant-amoudi-uuid',
              tenantCode: 'amoudi-materials',
              tenantName: 'Al-Amoudi Building Materials',
              role: 'OWNER',
              status: 'ACTIVE',
            },
          ],
        },
      }),
    });
    globalThis.fetch = fetchMock;

    const res = await client.login('owner@amoudi.com', 'Admin@123456');

    expect(fetchMock).toHaveBeenCalledTimes(1);
    const [url, init] = fetchMock.mock.calls[0];
    expect(url).toBe('http://localhost:8080/api/v1/auth/login');
    expect(init.method).toBe('POST');
    expect(res.data.accessToken).toBe('shop-jwt-token-9988');
    expect(client.getToken()).toBe('shop-jwt-token-9988');
    expect(client.getActiveTenantId()).toBe('tenant-amoudi-uuid');
  });

  it('attaches Authorization and X-Tenant-ID headers to subsequent requests', async () => {
    client.setToken('test-token-abc');
    client.setActiveTenantId('test-tenant-123');

    const fetchMock = vi.fn().mockResolvedValue({
      ok: true,
      headers: new Headers({ 'content-type': 'application/json' }),
      json: async () => ({
        success: true,
        data: {
          id: 'test-tenant-123',
          name: 'Al-Amoudi Materials',
          currency: 'USD',
          timezone: 'UTC',
          status: 'ACTIVE',
        },
      }),
    });
    globalThis.fetch = fetchMock;

    await client.getCurrentTenant();

    expect(fetchMock).toHaveBeenCalledTimes(1);
    const [, init] = fetchMock.mock.calls[0];
    const headers = init.headers as Headers;
    expect(headers.get('Authorization')).toBe('Bearer test-token-abc');
    expect(headers.get('X-Tenant-ID')).toBe('test-tenant-123');
    expect(headers.has('X-Correlation-ID')).toBe(true);
  });

  it('clears session on 401 Unauthorized response', async () => {
    client.setToken('expired-token');
    client.setActiveTenantId('tenant-1');

    const fetchMock = vi.fn().mockResolvedValue({
      ok: false,
      status: 401,
      statusText: 'Unauthorized',
      headers: new Headers({ 'content-type': 'application/json' }),
      json: async () => ({
        success: false,
        message: 'Token expired or invalid',
      }),
    });
    globalThis.fetch = fetchMock;

    await expect(client.getCurrentTenant()).rejects.toThrow('Token expired or invalid');
    expect(client.getToken()).toBeNull();
    expect(client.getActiveTenantId()).toBeNull();
  });
});
