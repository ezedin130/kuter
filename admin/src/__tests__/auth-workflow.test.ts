import { describe, it, expect, beforeEach, vi } from 'vitest';
import { ApiClient } from '../api/client';

describe('Admin Authentication & Access Control Workflow', () => {
  let client: ApiClient;

  beforeEach(() => {
    client = new ApiClient('http://test-kuter.local/api/v1');
    localStorage.clear();
    vi.restoreAllMocks();
  });

  it('authenticates platform administrator successfully and stores token', async () => {
    const fetchMock = vi.fn().mockResolvedValue({
      ok: true,
      json: async () => ({
        success: true,
        data: {
          accessToken: 'admin-jwt-token-12345',
          tokenType: 'Bearer',
          expiresIn: 86400,
          user: {
            id: 'admin-1',
            email: 'admin@kuterplatform.com',
            fullName: 'Super Administrator',
            isPlatformAdmin: true,
            status: 'ACTIVE',
          },
          memberships: [],
        },
      }),
    });
    globalThis.fetch = fetchMock;

    const res = await client.login({
      email: 'admin@kuterplatform.com',
      password: 'Admin@123456',
    });

    expect(fetchMock).toHaveBeenCalledTimes(1);
    const [url, init] = fetchMock.mock.calls[0];
    expect(url).toBe('http://test-kuter.local/api/v1/auth/login');
    expect(init.method).toBe('POST');
    expect(res.user.isPlatformAdmin).toBe(true);
    expect(client.getToken()).toBe('admin-jwt-token-12345');
  });

  it('rejects invalid credentials with backend error message', async () => {
    const fetchMock = vi.fn().mockResolvedValue({
      ok: false,
      status: 401,
      statusText: 'Unauthorized',
      json: async () => ({
        success: false,
        message: 'Invalid email or password',
      }),
    });
    globalThis.fetch = fetchMock;

    await expect(
      client.login({ email: 'bad@user.com', password: 'wrong' })
    ).rejects.toThrow('Invalid email or password');
    expect(client.getToken()).toBeNull();
  });

  it('signs out and destroys client token', async () => {
    client.setToken('admin-active-token');

    const fetchMock = vi.fn().mockResolvedValue({
      ok: true,
      json: async () => ({
        success: true,
        message: 'Logout successful',
      }),
    });
    globalThis.fetch = fetchMock;

    await client.logout();
    expect(fetchMock).toHaveBeenCalledTimes(1);
    expect(client.getToken()).toBeNull();
  });
});
