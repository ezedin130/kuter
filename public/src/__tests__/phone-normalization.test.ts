import { describe, it, expect } from 'vitest';
import { normalizeEthiopianPhone } from '../pages/RegistrationPage';

describe('Ethiopian Phone Number Normalization', () => {
  it('normalizes standard 9-digit input starting with 9', () => {
    const res = normalizeEthiopianPhone('982112233');
    expect(res.valid).toBe(true);
    expect(res.normalized).toBe('+251982112233');
  });

  it('normalizes 10-digit input starting with 09', () => {
    const res = normalizeEthiopianPhone('0982112233');
    expect(res.valid).toBe(true);
    expect(res.normalized).toBe('+251982112233');
  });

  it('normalizes full international format with +251', () => {
    const res = normalizeEthiopianPhone('+251982112233');
    expect(res.valid).toBe(true);
    expect(res.normalized).toBe('+251982112233');
  });

  it('normalizes format with 251 prefix without plus', () => {
    const res = normalizeEthiopianPhone('251982112233');
    expect(res.valid).toBe(true);
    expect(res.normalized).toBe('+251982112233');
  });

  it('normalizes formatted phone number with spaces', () => {
    const res = normalizeEthiopianPhone('098 211 2233');
    expect(res.valid).toBe(true);
    expect(res.normalized).toBe('+251982112233');
  });

  it('normalizes formatted phone number with hyphens', () => {
    const res = normalizeEthiopianPhone('098-211-2233');
    expect(res.valid).toBe(true);
    expect(res.normalized).toBe('+251982112233');
  });

  it('supports Ethio Telecom 7-series numbers (Safaricom / new ranges)', () => {
    const res = normalizeEthiopianPhone('0712345678');
    expect(res.valid).toBe(true);
    expect(res.normalized).toBe('+251712345678');
  });

  it('rejects invalid short or non-digit numbers', () => {
    const res = normalizeEthiopianPhone('12345');
    expect(res.valid).toBe(false);
    expect(res.error).toBeDefined();
  });

  it('rejects invalid numbers starting with wrong prefix', () => {
    const res = normalizeEthiopianPhone('0312345678');
    expect(res.valid).toBe(false);
  });
});
