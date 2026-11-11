import { describe, it, expect, beforeEach, afterEach, vi } from 'vitest';
import { ApiClient } from '../api/client';

// Mock localStorage for node test environment
const storageMock = (() => {
  let store: Record<string, string> = {};
  return {
    getItem: (key: string) => store[key] || null,
    setItem: (key: string, value: string) => {
      store[key] = value.toString();
    },
    removeItem: (key: string) => {
      delete store[key];
    },
    clear: () => {
      store = {};
    },
  };
})();

Object.defineProperty(globalThis, 'localStorage', {
  value: storageMock,
  writable: true,
});

describe('ApiClient Core Infrastructure', () => {
  let client: ApiClient;
  const mockBaseUrl = 'http://test-kuter.local/api/v1';

  beforeEach(() => {
    client = new ApiClient(mockBaseUrl);
    localStorage.clear();
    vi.restoreAllMocks();
  });

  afterEach(() => {
    localStorage.clear();
  });

  it('manages authentication token in localStorage', () => {
    expect(client.getToken()).toBeNull();
    client.setToken('jwt-admin-token-xyz');
    expect(client.getToken()).toBe('jwt-admin-token-xyz');
    expect(localStorage.getItem('kuter_admin_token')).toBe('jwt-admin-token-xyz');

    client.setToken(null);
    expect(client.getToken()).toBeNull();
    expect(localStorage.getItem('kuter_admin_token')).toBeNull();
  });

  it('attaches Bearer token and correlation ID to outgoing requests', async () => {
    client.setToken('test-secret-token');

    const fetchMock = vi.fn().mockResolvedValue({
      ok: true,
      json: async () => ({
        success: true,
        message: 'Success',
        data: { status: 'UP', application: 'Kuter' },
      }),
    });
    globalThis.fetch = fetchMock;

    await client.getHealth();

    expect(fetchMock).toHaveBeenCalledTimes(1);
    const [url, init] = fetchMock.mock.calls[0];
    expect(url).toBe('http://test-kuter.local/api/v1/health');

    const headers = init.headers as Headers;
    expect(headers.get('Authorization')).toBe('Bearer test-secret-token');
    expect(headers.get('Accept')).toBe('application/json');
    expect(headers.get('X-Correlation-ID')).toBeDefined();
    expect(headers.get('X-Correlation-ID')?.length).toBeGreaterThan(5);
  });

  it('clears token on HTTP 401 Unauthorized', async () => {
    client.setToken('expired-token');

    const fetchMock = vi.fn().mockResolvedValue({
      ok: false,
      status: 401,
      statusText: 'Unauthorized',
      json: async () => ({
        success: false,
        message: 'JWT token has expired or is invalid',
      }),
    });
    globalThis.fetch = fetchMock;

    await expect(client.getMe()).rejects.toThrow('JWT token has expired or is invalid');
    expect(client.getToken()).toBeNull();
  });

  it('formats friendly error on network connectivity failure', async () => {
    const fetchMock = vi.fn().mockRejectedValue(new Error('Failed to fetch'));
    globalThis.fetch = fetchMock;

    await expect(client.getHealth()).rejects.toThrow(
      'Unable to connect to the backend server. Please verify the API is running at http://test-kuter.local/api/v1.'
    );
  });
});
