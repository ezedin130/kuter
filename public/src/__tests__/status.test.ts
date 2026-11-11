import { describe, it, expect } from 'vitest';
import { OnboardingStatus } from '../types/api';

describe('Application Status Tracker Logic Contract', () => {
  it('formats reference code for status API lookup', () => {
    const cleanReference = (raw: string) => raw.trim().toUpperCase();

    expect(cleanReference('req-2026-00001')).toBe('REQ-2026-00001');
    expect(cleanReference('  REQ-2026-00042  ')).toBe('REQ-2026-00042');
  });

  it('determines correct progress step based on onboarding status', () => {
    const getActiveStep = (status: OnboardingStatus) => {
      switch (status) {
        case 'PENDING':
        case 'MORE_INFO_REQUESTED':
          return 2; // Step 2: Administrator review in progress
        case 'APPROVED':
          return 3; // Step 3: Account setup ready
        case 'REJECTED':
          return 2; // Halted at review
        default:
          return 1;
      }
    };

    expect(getActiveStep('PENDING')).toBe(2);
    expect(getActiveStep('MORE_INFO_REQUESTED')).toBe(2);
    expect(getActiveStep('APPROVED')).toBe(3);
    expect(getActiveStep('REJECTED')).toBe(2);
  });

  it('validates status badge class selection', () => {
    const getBadgeClass = (status: string) => {
      const s = status.toUpperCase();
      if (s === 'APPROVED' || s === 'ACTIVE') return 'badge badge-approved';
      if (s === 'PENDING' || s === 'TRIALING') return 'badge badge-pending';
      if (s === 'REJECTED' || s === 'SUSPENDED') return 'badge badge-rejected';
      if (s === 'MORE_INFO_REQUESTED') return 'badge badge-more-info';
      return 'badge';
    };

    expect(getBadgeClass('PENDING')).toBe('badge badge-pending');
    expect(getBadgeClass('APPROVED')).toBe('badge badge-approved');
    expect(getBadgeClass('REJECTED')).toBe('badge badge-rejected');
    expect(getBadgeClass('MORE_INFO_REQUESTED')).toBe('badge badge-more-info');
  });
});
