import { describe, it, expect, beforeEach, vi } from 'vitest';
import { ApiClient } from '../api/client';

describe('Platform Audit Logs Integration Contract', () => {
  let client: ApiClient;

  beforeEach(() => {
    client = new ApiClient('http://test-kuter.local/api/v1');
    client.setToken('admin-valid-token');
    vi.restoreAllMocks();
  });

  it('queries platform audit logs with multi-attribute filters', async () => {
    const fetchMock = vi.fn().mockResolvedValue({
      ok: true,
      json: async () => ({
        success: true,
        data: {
          content: [
            {
              id: 'log-1',
              action: 'TENANT_ONBOARDING_APPROVED',
              entityType: 'Tenant',
              entityId: 'tenant-1',
              actorEmail: 'admin@kuterplatform.com',
              outcome: 'SUCCESS',
              details: '{"tenantCode":"al-madina"}',
              createdAt: '2026-10-29T11:00:00Z',
            },
          ],
          totalElements: 1,
          totalPages: 1,
        },
      }),
    });
    globalThis.fetch = fetchMock;

    const res = await client.getAdminAuditLogs({
      outcome: 'SUCCESS',
      action: 'TENANT_ONBOARDING_APPROVED',
      actorEmail: 'admin@kuterplatform.com',
      page: 0,
      size: 15,
    });

    expect(fetchMock).toHaveBeenCalledTimes(1);
    const [url] = fetchMock.mock.calls[0];
    expect(url).toContain('/admin/audit-logs?action=TENANT_ONBOARDING_APPROVED&actorEmail=admin%40kuterplatform.com&outcome=SUCCESS&page=0&size=15');
    expect(res.content.length).toBe(1);
    expect(res.content[0].action).toBe('TENANT_ONBOARDING_APPROVED');
    expect(res.content[0].outcome).toBe('SUCCESS');
  });

  it('queries platform overview high-level metrics', async () => {
    const fetchMock = vi.fn().mockResolvedValue({
      ok: true,
      json: async () => ({
        success: true,
        data: {
          totalTenants: 12,
          activeTenants: 11,
          suspendedTenants: 1,
          pendingOnboardingRequests: 3,
          totalSubscriptions: 12,
          activeSubscriptions: 10,
          trialingSubscriptions: 1,
          expiredSubscriptions: 1,
          estimatedMonthlyRevenue: 1540.0,
          currency: 'USD',
          generatedAt: '2026-10-29T11:30:00Z',
        },
      }),
    });
    globalThis.fetch = fetchMock;

    const overview = await client.getPlatformOverview();
    expect(overview.totalTenants).toBe(12);
    expect(overview.activeTenants).toBe(11);
    expect(overview.pendingOnboardingRequests).toBe(3);
    expect(overview.estimatedMonthlyRevenue).toBe(1540.0);
  });
});
