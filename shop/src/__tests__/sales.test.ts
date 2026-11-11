import { describe, it, expect, beforeEach, vi } from 'vitest';
import { ShopApiClient } from '../api/client';

describe('Sales Processing & Counter POS Checkout', () => {
  let client: ShopApiClient;

  beforeEach(() => {
    client = new ShopApiClient('http://localhost:8080/api/v1');
    client.setToken('auth-token-xyz');
    client.setActiveTenantId('tenant-uuid');
    vi.restoreAllMocks();
  });

  it('finalizes counter sale with line items and customer information', async () => {
    const fetchMock = vi.fn().mockResolvedValue({
      ok: true,
      headers: new Headers({ 'content-type': 'application/json' }),
      json: async () => ({
        success: true,
        data: {
          id: 'sale-101',
          invoiceNumber: 'INV-2026-0001',
          soldAt: '2026-10-29T12:00:00Z',
          cashierName: 'Tariq Al-Amoudi',
          customerName: 'Ahmad Construction',
          subtotal: 1800.0,
          discountAmount: 100.0,
          taxAmount: 0.0,
          totalAmount: 1700.0,
          status: 'COMPLETED',
          items: [
            {
              id: 'item-1',
              productId: 'prod-steel-1',
              unitCode: 'BUNDLE',
              quantity: 2,
              unitPrice: 900.0,
              discountAmount: 100.0,
              lineTotal: 1700.0,
            },
          ],
        },
      }),
    });
    globalThis.fetch = fetchMock;

    const res = await client.createSale({
      customerName: 'Ahmad Construction',
      discountAmount: 100.0,
      idempotencyKey: 'test-idemp-123',
      items: [
        {
          productId: 'prod-steel-1',
          unitDefinitionId: 'unit-bundle-uuid',
          quantity: 2,
          unitPrice: 900.0,
          discountAmount: 100.0,
        },
      ],
    });

    expect(fetchMock).toHaveBeenCalledTimes(1);
    const [url, init] = fetchMock.mock.calls[0];
    expect(url).toContain('/sales');
    expect(init.method).toBe('POST');
    const body = JSON.parse(init.body as string);
    expect(body.idempotencyKey).toBe('test-idemp-123');
    expect(res.data.invoiceNumber).toBe('INV-2026-0001');
    expect(res.data.totalAmount).toBe(1700.0);
  });

  it('retrieves printable operational receipt with non-payment disclaimer', async () => {
    const fetchMock = vi.fn().mockResolvedValue({
      ok: true,
      headers: new Headers({ 'content-type': 'application/json' }),
      json: async () => ({
        success: true,
        data: {
          shopName: 'Al-Amoudi Building Materials',
          invoiceNumber: 'INV-2026-0001',
          soldAt: '2026-10-29T12:00:00Z',
          cashierName: 'Tariq Al-Amoudi',
          subtotal: 1800.0,
          totalAmount: 1700.0,
          status: 'COMPLETED',
          disclaimer: 'Operational Sales Record - Not a Proof of Payment',
          items: [
            {
              productSku: 'STEEL-12MM',
              productName: 'Steel Rebar 12mm',
              unitCode: 'BUNDLE',
              quantity: 2,
              unitPrice: 900.0,
              lineTotal: 1800.0,
            },
          ],
        },
      }),
    });
    globalThis.fetch = fetchMock;

    const res = await client.getSaleReceipt('sale-101');

    expect(fetchMock).toHaveBeenCalledTimes(1);
    const [url] = fetchMock.mock.calls[0];
    expect(url).toContain('/sales/sale-101/receipt');
    expect(res.data.disclaimer).toBe('Operational Sales Record - Not a Proof of Payment');
  });

  it('voids sale with mandatory cancellation reason', async () => {
    const fetchMock = vi.fn().mockResolvedValue({
      ok: true,
      headers: new Headers({ 'content-type': 'application/json' }),
      json: async () => ({
        success: true,
        data: {
          id: 'sale-101',
          invoiceNumber: 'INV-2026-0001',
          status: 'VOIDED',
          voidReason: 'CUSTOMER_CANCEL',
          voidedAt: '2026-10-29T12:30:00Z',
        },
      }),
    });
    globalThis.fetch = fetchMock;

    const res = await client.voidSale('sale-101', { reason: 'CUSTOMER_CANCEL' });

    expect(fetchMock).toHaveBeenCalledTimes(1);
    const [url, init] = fetchMock.mock.calls[0];
    expect(url).toContain('/sales/sale-101/void');
    expect(init.method).toBe('POST');
    const body = JSON.parse(init.body as string);
    expect(body.reason).toBe('CUSTOMER_CANCEL');
    expect(res.data.status).toBe('VOIDED');
  });
});
