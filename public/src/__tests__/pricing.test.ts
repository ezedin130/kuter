import { describe, it, expect } from 'vitest';
import { PublicSubscriptionPlanDto } from '../types/api';

describe('Pricing calculations and plan metadata', () => {
  const samplePlans: PublicSubscriptionPlanDto[] = [
    {
      id: 'plan-starter',
      code: 'starter',
      name: 'Starter',
      description: 'Single yard building materials shops.',
      price: 49.0,
      currency: 'USD',
      billingInterval: 'MONTHLY',
      productLimit: 500,
      userLimit: 3,
      branchLimit: 1,
      trialDays: 14,
      featuresSummary: 'Up to 500 products, 3 staff seats, counter checkout, basic UOM conversion',
    },
    {
      id: 'plan-growth',
      code: 'growth',
      name: 'Growth',
      description: 'High-volume materials distribution.',
      price: 119.0,
      currency: 'USD',
      billingInterval: 'MONTHLY',
      productLimit: 2500,
      userLimit: 10,
      branchLimit: 3,
      trialDays: 14,
      featuresSummary: 'Up to 2,500 products, 10 staff seats, delivery note receiving, multi-location stock ledger',
    },
    {
      id: 'plan-enterprise',
      code: 'enterprise',
      name: 'Enterprise',
      description: 'Custom limits and dedicated support.',
      price: 279.0,
      currency: 'USD',
      billingInterval: 'MONTHLY',
      productLimit: 10000,
      userLimit: 50,
      branchLimit: 10,
      trialDays: 14,
      featuresSummary: 'Up to 10,000 products, 50 staff seats, unlimited branches, priority phone support',
    },
  ];

  it('calculates 15% discount for yearly billing', () => {
    const calcYearlyMonthlyRate = (base: number) => Math.round(base * 0.85);

    expect(calcYearlyMonthlyRate(49)).toBe(42);
    expect(calcYearlyMonthlyRate(119)).toBe(101);
    expect(calcYearlyMonthlyRate(279)).toBe(237);
  });

  it('correctly maps plan quotas and trial days', () => {
    const growth = samplePlans.find((p) => p.code === 'growth');
    expect(growth).toBeDefined();
    expect(growth?.productLimit).toBe(2500);
    expect(growth?.userLimit).toBe(10);
    expect(growth?.branchLimit).toBe(3);
    expect(growth?.trialDays).toBe(14);
  });

  it('splits features summary cleanly for bullet rendering', () => {
    const starter = samplePlans.find((p) => p.code === 'starter');
    const bullets = starter?.featuresSummary?.split(',').map((f) => f.trim()) || [];
    expect(bullets.length).toBe(4);
    expect(bullets[0]).toBe('Up to 500 products');
    expect(bullets[1]).toBe('3 staff seats');
  });
});
