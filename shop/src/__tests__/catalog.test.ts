import { describe, it, expect, beforeEach, vi } from 'vitest';
import { ShopApiClient } from '../api/client';

describe('Product Catalog & Dual-UOM Conversions', () => {
  let client: ShopApiClient;

  beforeEach(() => {
    client = new ShopApiClient('http://localhost:8080/api/v1');
    client.setToken('auth-token-xyz');
    client.setActiveTenantId('tenant-uuid');
    vi.restoreAllMocks();
  });

  it('queries product categories with active and search parameters', async () => {
    const fetchMock = vi.fn().mockResolvedValue({
      ok: true,
      headers: new Headers({ 'content-type': 'application/json' }),
      json: async () => ({
        success: true,
        data: {
          content: [
            {
              id: 'cat-1',
              name: 'Cement & Concrete',
              productCount: 12,
              active: true,
            },
          ],
          pageNumber: 0,
          pageSize: 20,
          totalElements: 1,
          totalPages: 1,
        },
      }),
    });
    globalThis.fetch = fetchMock;

    const res = await client.listCategories(true, 'Cement');

    expect(fetchMock).toHaveBeenCalledTimes(1);
    const [url] = fetchMock.mock.calls[0];
    expect(url).toContain('/categories?active=true&search=Cement');
    expect(res.data.content[0].name).toBe('Cement & Concrete');
  });

  it('creates product with base unit, price, and initial threshold', async () => {
    const fetchMock = vi.fn().mockResolvedValue({
      ok: true,
      headers: new Headers({ 'content-type': 'application/json' }),
      json: async () => ({
        success: true,
        data: {
          id: 'prod-new-1',
          sku: 'STEEL-12MM-12M',
          name: 'Steel Rebar 12mm',
          baseUnitCode: 'PCS',
          sellingPrice: 18.5,
          costPrice: 14.0,
          reorderThreshold: 50,
          currentStock: 0,
          status: 'ACTIVE',
        },
      }),
    });
    globalThis.fetch = fetchMock;

    const res = await client.createProduct({
      sku: 'STEEL-12MM-12M',
      name: 'Steel Rebar 12mm',
      baseUnitId: 'unit-pcs-uuid',
      sellingPrice: 18.5,
      costPrice: 14.0,
      reorderThreshold: 50,
    });

    expect(fetchMock).toHaveBeenCalledTimes(1);
    const [, init] = fetchMock.mock.calls[0];
    expect(init.method).toBe('POST');
    const body = JSON.parse(init.body as string);
    expect(body.sku).toBe('STEEL-12MM-12M');
    expect(body.sellingPrice).toBe(18.5);
    expect(res.data.sku).toBe('STEEL-12MM-12M');
  });

  it('manages dual-UOM conversions for a product', async () => {
    const fetchMock = vi.fn().mockResolvedValue({
      ok: true,
      headers: new Headers({ 'content-type': 'application/json' }),
      json: async () => ({
        success: true,
        data: {
          id: 'conv-bundle-1',
          productId: 'prod-steel-1',
          unitDefinitionId: 'unit-bundle-uuid',
          unitCode: 'BUNDLE',
          unitName: 'Bundle of 50',
          toBaseFactor: 50,
          customSellingPrice: 900.0,
        },
      }),
    });
    globalThis.fetch = fetchMock;

    const res = await client.addProductConversion('prod-steel-1', {
      unitDefinitionId: 'unit-bundle-uuid',
      toBaseFactor: 50,
      customSellingPrice: 900.0,
    });

    expect(fetchMock).toHaveBeenCalledTimes(1);
    const [url, init] = fetchMock.mock.calls[0];
    expect(url).toContain('/products/prod-steel-1/conversions');
    expect(init.method).toBe('POST');
    expect(res.data.toBaseFactor).toBe(50);
  });
});
