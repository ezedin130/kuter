import { describe, it, expect, beforeEach, vi } from 'vitest';
import { ShopApiClient } from '../api/client';

describe('Inventory Operations & Ledger Management', () => {
  let client: ShopApiClient;

  beforeEach(() => {
    client = new ShopApiClient('http://localhost:8080/api/v1');
    client.setToken('auth-token-xyz');
    client.setActiveTenantId('tenant-uuid');
    vi.restoreAllMocks();
  });

  it('records stock receiving with supplier delivery note and updates costs', async () => {
    const fetchMock = vi.fn().mockResolvedValue({
      ok: true,
      headers: new Headers({ 'content-type': 'application/json' }),
      json: async () => ({
        success: true,
        data: {
          id: 'recv-001',
          receivingNumber: 'REC-2026-0001',
          supplierName: 'National Steel Mill',
          totalCost: 18000.0,
          receivedByName: 'Tariq Al-Amoudi',
          items: [
            {
              id: 'recv-item-1',
              productId: 'prod-steel-1',
              receivedQuantity: 20,
              toBaseFactor: 50,
              baseQuantity: 1000,
              unitCost: 900.0,
              totalCost: 18000.0,
            },
          ],
        },
      }),
    });
    globalThis.fetch = fetchMock;

    const res = await client.receiveStock({
      supplierName: 'National Steel Mill',
      deliveryNoteRef: 'DN-8921',
      items: [
        {
          productId: 'prod-steel-1',
          receivedUnitId: 'unit-bundle-uuid',
          receivedQuantity: 20,
          unitCost: 900.0,
        },
      ],
    });

    expect(fetchMock).toHaveBeenCalledTimes(1);
    const [url, init] = fetchMock.mock.calls[0];
    expect(url).toContain('/inventory/receipts');
    expect(init.method).toBe('POST');
    expect(res.data.receivingNumber).toBe('REC-2026-0001');
    expect(res.data.items[0].baseQuantity).toBe(1000);
  });

  it('records stock adjustments with mandatory reason code', async () => {
    const fetchMock = vi.fn().mockResolvedValue({
      ok: true,
      headers: new Headers({ 'content-type': 'application/json' }),
      json: async () => ({
        success: true,
        data: {
          id: 'adj-001',
          adjustmentNumber: 'ADJ-2026-0001',
          reasonCode: 'DAMAGED',
          items: [
            {
              id: 'adj-item-1',
              productId: 'prod-cement-1',
              quantityBefore: 100,
              quantityChange: -5,
              quantityAfter: 95,
            },
          ],
        },
      }),
    });
    globalThis.fetch = fetchMock;

    const res = await client.adjustStock({
      reasonCode: 'DAMAGED',
      notes: 'Water leak damaged 5 bags',
      items: [
        {
          productId: 'prod-cement-1',
          quantityChange: -5,
        },
      ],
    });

    expect(fetchMock).toHaveBeenCalledTimes(1);
    const [, init] = fetchMock.mock.calls[0];
    expect(init.method).toBe('POST');
    const body = JSON.parse(init.body as string);
    expect(body.reasonCode).toBe('DAMAGED');
    expect(res.data.items[0].quantityChange).toBe(-5);
  });

  it('queries stock movements ledger with type filter', async () => {
    const fetchMock = vi.fn().mockResolvedValue({
      ok: true,
      headers: new Headers({ 'content-type': 'application/json' }),
      json: async () => ({
        success: true,
        data: {
          content: [
            {
              id: 'mov-1',
              productName: 'Cement 50kg',
              movementType: 'SALE',
              quantityChange: -10,
              balanceAfter: 90,
              createdAt: '2026-10-29T12:00:00Z',
            },
          ],
          pageNumber: 0,
          pageSize: 25,
          totalElements: 1,
          totalPages: 1,
        },
      }),
    });
    globalThis.fetch = fetchMock;

    const res = await client.listStockMovements({ movementType: 'SALE' });

    expect(fetchMock).toHaveBeenCalledTimes(1);
    const [url] = fetchMock.mock.calls[0];
    expect(url).toContain('movementType=SALE');
    expect(res.data.content[0].quantityChange).toBe(-10);
  });
});
