import { describe, it, expect, beforeEach, vi } from 'vitest';
import { ApiClient } from '../api/client';

describe('Tenant Management Integration Contract', () => {
  let client: ApiClient;

  beforeEach(() => {
    client = new ApiClient('http://test-kuter.local/api/v1');
    client.setToken('admin-valid-token');
    vi.restoreAllMocks();
  });

  it('queries tenants with operational status filter', async () => {
    const fetchMock = vi.fn().mockResolvedValue({
      ok: true,
      json: async () => ({
        success: true,
        data: {
          content: [
            {
              id: 'tenant-1',
              code: 'al-madina',
              name: 'Al-Madina Materials',
              status: 'ACTIVE',
              currency: 'USD',
              timezone: 'UTC',
              activePlanCode: 'PROFESSIONAL',
            },
          ],
          totalElements: 1,
          totalPages: 1,
        },
      }),
    });
    globalThis.fetch = fetchMock;

    const res = await client.listTenants({ status: 'ACTIVE', page: 0, size: 20 });

    expect(fetchMock).toHaveBeenCalledTimes(1);
    const [url] = fetchMock.mock.calls[0];
    expect(url).toContain('/admin/tenants?status=ACTIVE&page=0&size=20');
    expect(res.content[0].name).toBe('Al-Madina Materials');
    expect(res.content[0].status).toBe('ACTIVE');
  });

  it('submits create tenant payload directly', async () => {
    const fetchMock = vi.fn().mockResolvedValue({
      ok: true,
      json: async () => ({
        success: true,
        data: {
          id: 'tenant-2',
          code: 'metro-steel',
          name: 'Metro Steel Yard',
          status: 'ACTIVE',
          currency: 'USD',
          timezone: 'UTC',
        },
      }),
    });
    globalThis.fetch = fetchMock;

    const result = await client.createTenant({
      name: 'Metro Steel Yard',
      code: 'metro-steel',
      currency: 'USD',
      timezone: 'UTC',
      initialOwnerEmail: 'owner@metrosteel.com',
      initialOwnerFullName: 'John Steel',
      planCode: 'GROWTH',
    });

    expect(fetchMock).toHaveBeenCalledTimes(1);
    const [url, init] = fetchMock.mock.calls[0];
    expect(url).toBe('http://test-kuter.local/api/v1/admin/tenants');
    expect(init.method).toBe('POST');
    const body = JSON.parse(init.body as string);
    expect(body.code).toBe('metro-steel');
    expect(body.initialOwnerEmail).toBe('owner@metrosteel.com');
    expect(result.id).toBe('tenant-2');
  });

  it('updates operational tenant status with reason', async () => {
    const fetchMock = vi.fn().mockResolvedValue({
      ok: true,
      json: async () => ({
        success: true,
        data: {
          id: 'tenant-1',
          code: 'al-madina',
          status: 'SUSPENDED',
        },
      }),
    });
    globalThis.fetch = fetchMock;

    const result = await client.updateTenantStatus('tenant-1', {
      status: 'SUSPENDED',
      reason: 'Requested seasonal shutdown',
    });

    expect(fetchMock).toHaveBeenCalledTimes(1);
    const [url, init] = fetchMock.mock.calls[0];
    expect(url).toBe('http://test-kuter.local/api/v1/admin/tenants/tenant-1/status');
    expect(init.method).toBe('PATCH');
    const body = JSON.parse(init.body as string);
    expect(body.status).toBe('SUSPENDED');
    expect(body.reason).toBe('Requested seasonal shutdown');
    expect(result.status).toBe('SUSPENDED');
  });

  it('retrieves decoupled operational status and subscription lifecycle metadata', async () => {
    const fetchMock = vi.fn().mockResolvedValue({
      ok: true,
      json: async () => ({
        success: true,
        data: {
          tenantId: 'tenant-1',
          code: 'al-madina',
          name: 'Al-Madina Materials',
          operationalStatus: 'ACTIVE',
          subscriptionId: 'sub-1',
          planCode: 'PROFESSIONAL',
          planName: 'Professional Depot',
          lifecycleStatus: 'ACTIVE',
          effectiveStatus: 'ACTIVE',
          daysRemaining: 24,
          cancelAtPeriodEnd: false,
        },
      }),
    });
    globalThis.fetch = fetchMock;

    const statusDetail = await client.getTenantStatus('tenant-1');

    expect(fetchMock).toHaveBeenCalledTimes(1);
    const [url] = fetchMock.mock.calls[0];
    expect(url).toBe('http://test-kuter.local/api/v1/admin/tenants/tenant-1/status');
    expect(statusDetail.operationalStatus).toBe('ACTIVE');
    expect(statusDetail.effectiveStatus).toBe('ACTIVE');
    expect(statusDetail.daysRemaining).toBe(24);
  });
});
