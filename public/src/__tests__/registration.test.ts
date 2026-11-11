import { describe, it, expect } from 'vitest';
import { CreateOnboardingRequestDto } from '../types/api';

describe('Shop Registration Form Validation Contract', () => {
  const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;

  it('validates shop name requirements', () => {
    const isValidShopName = (name: string) => name.trim().length >= 2;

    expect(isValidShopName('')).toBe(false);
    expect(isValidShopName(' ')).toBe(false);
    expect(isValidShopName('A')).toBe(false);
    expect(isValidShopName('Kuter Yard')).toBe(true);
  });

  it('validates owner email format', () => {
    expect(emailRegex.test('')).toBe(false);
    expect(emailRegex.test('not-an-email')).toBe(false);
    expect(emailRegex.test('owner@')).toBe(false);
    expect(emailRegex.test('owner@domain')).toBe(false);
    expect(emailRegex.test('owner@business.com')).toBe(true);
    expect(emailRegex.test('maya.okafor@northstar.co.uk')).toBe(true);
  });

  it('normalizes preferred shop identifier to lowercase alphanumeric and hyphens', () => {
    const sanitizeShopCode = (input: string) =>
      input.toLowerCase().replace(/[^a-z0-9-]/g, '');

    expect(sanitizeShopCode('NorthStar Building Yard #1')).toBe('northstarbuildingyard1');
    expect(sanitizeShopCode('delta-timber-2026')).toBe('delta-timber-2026');
  });

  it('verifies complete onboarding payload structure', () => {
    const payload: CreateOnboardingRequestDto = {
      shopName: 'Greenland Concrete & Blocks',
      ownerFullName: 'Kwame Mensah',
      ownerEmail: 'kwame@greenlandconcrete.com',
      ownerPhone: '+233 24 000 0000',
      requestedPlanCode: 'starter',
      preferredShopCode: 'greenland-blocks',
      businessAddress: 'Tema Heavy Industrial Area, Accra, Ghana',
      currency: 'GHS',
      applicantNotes: 'Require support for cement bags to metric ton conversion',
    };

    expect(payload.shopName.length).toBeGreaterThanOrEqual(2);
    expect(payload.ownerFullName.length).toBeGreaterThanOrEqual(2);
    expect(emailRegex.test(payload.ownerEmail)).toBe(true);
    expect(['starter', 'growth', 'enterprise']).toContain(payload.requestedPlanCode);
    expect(['USD', 'GHS', 'NGN', 'KES']).toContain(payload.currency);
  });
});
