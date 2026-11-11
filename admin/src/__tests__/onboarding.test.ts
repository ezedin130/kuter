import { describe, it, expect, beforeEach, vi } from 'vitest';
import { ApiClient } from '../api/client';

describe('Onboarding Requests Integration Contract', () => {
  let client: ApiClient;

  beforeEach(() => {
    client = new ApiClient('http://test-kuter.local/api/v1');
    client.setToken('admin-valid-token');
    vi.restoreAllMocks();
  });

  it('queries onboarding requests with status and pagination parameters', async () => {
    const fetchMock = vi.fn().mockResolvedValue({
      ok: true,
      json: async () => ({
        success: true,
        data: {
          content: [
            {
              id: 'req-1',
              referenceCode: 'REQ-2026-0001',
              shopName: 'Al-Madina Materials',
              ownerFullName: 'Rashid Al-Khatib',
              ownerEmail: 'rashid@almadina.com',
              status: 'PENDING',
              createdAt: '2026-10-29T10:00:00Z',
            },
          ],
          pageNumber: 0,
          pageSize: 10,
          totalElements: 1,
          totalPages: 1,
        },
      }),
    });
    globalThis.fetch = fetchMock;

    const res = await client.listOnboardingRequests({
      status: 'PENDING',
      search: 'Al-Madina',
      page: 0,
      size: 10,
    });

    expect(fetchMock).toHaveBeenCalledTimes(1);
    const [url] = fetchMock.mock.calls[0];
    expect(url).toContain('/admin/onboarding-requests?status=PENDING&search=Al-Madina&page=0&size=10');
    expect(res.content.length).toBe(1);
    expect(res.content[0].referenceCode).toBe('REQ-2026-0001');
  });

  it('submits approval payload to approve onboarding request', async () => {
    const fetchMock = vi.fn().mockResolvedValue({
      ok: true,
      json: async () => ({
        success: true,
        data: {
          onboardingRequestId: 'req-1',
          tenantId: 'tenant-1',
          tenantCode: 'al-madina-custom',
          ownerUserId: 'user-1',
          ownerEmail: 'rashid@almadina.com',
          status: 'APPROVED',
          message: 'Provisioned successfully',
        },
      }),
    });
    globalThis.fetch = fetchMock;

    const result = await client.approveOnboardingRequest('req-1', {
      customShopCode: 'al-madina-custom',
      reviewerNotes: 'Verified commercial registry',
    });

    expect(fetchMock).toHaveBeenCalledTimes(1);
    const [url, init] = fetchMock.mock.calls[0];
    expect(url).toBe('http://test-kuter.local/api/v1/admin/onboarding-requests/req-1/approve');
    expect(init.method).toBe('POST');
    const body = JSON.parse(init.body as string);
    expect(body.customShopCode).toBe('al-madina-custom');
    expect(body.reviewerNotes).toBe('Verified commercial registry');
    expect(result.tenantCode).toBe('al-madina-custom');
  });

  it('submits rejection payload with mandatory reason', async () => {
    const fetchMock = vi.fn().mockResolvedValue({
      ok: true,
      json: async () => ({
        success: true,
        data: {
          id: 'req-1',
          referenceCode: 'REQ-2026-0001',
          shopName: 'Incomplete Yard',
          status: 'REJECTED',
          reviewerNotes: 'Trade license document unreadable',
        },
      }),
    });
    globalThis.fetch = fetchMock;

    const result = await client.rejectOnboardingRequest('req-1', {
      reason: 'Trade license document unreadable',
    });

    expect(fetchMock).toHaveBeenCalledTimes(1);
    const [url, init] = fetchMock.mock.calls[0];
    expect(url).toBe('http://test-kuter.local/api/v1/admin/onboarding-requests/req-1/reject');
    expect(init.method).toBe('POST');
    const body = JSON.parse(init.body as string);
    expect(body.reason).toBe('Trade license document unreadable');
    expect(result.status).toBe('REJECTED');
  });
});
