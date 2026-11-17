import { describe, it, expect } from 'vitest';
import { en } from '../i18n/locales/en';
import { am } from '../i18n/locales/am';

describe('Shop Localization (i18n)', () => {
  it('has identical top-level namespaces in both English and Amharic', () => {
    const enKeys = Object.keys(en).sort();
    const amKeys = Object.keys(am).sort();
    expect(amKeys).toEqual(enKeys);
  });

  it('provides all common shop navigation items in Amharic', () => {
    expect(am.nav.dashboard).toBe('ዳሽቦርድ');
    expect(am.nav.pos).toBe('የሽያጭ መመዝገቢያ (POS)');
    expect(am.nav.salesHistory).toBe('የሽያጭ ታሪክ');
    expect(am.nav.products).toBe('የምርቶች ዝርዝር');
    expect(am.nav.categories).toBe('የምርት ምድቦች');
    expect(am.nav.receiveStock).toBe('እቃ መረከቢያ');
    expect(am.nav.adjustments).toBe('የክምችት ማስተካከያ');
    expect(am.nav.lending).toBe('የእቃ ብድር እና ልውውጥ');
    expect(am.nav.movements).toBe('የእንቅስቃሴ መዝገብ');
    expect(am.nav.reports).toBe('ሪፖርቶች');
    expect(am.nav.subscription).toBe('የደንበኝነት እቅድ');
    expect(am.nav.settings).toBe('የሱቅ ቅንብሮች');
  });

  it('provides all common buttons and actions in Amharic', () => {
    expect(am.common.save).toBe('አስቀምጥ');
    expect(am.common.cancel).toBe('ይቅር');
    expect(am.common.delete).toBe('ሰርዝ');
    expect(am.common.edit).toBe('አስተካክል');
    expect(am.common.confirm).toBe('አረጋግጥ');
    expect(am.common.close).toBe('ዝጋ');
    expect(am.common.print).toBe('አትም');
    expect(am.common.signOut).toBe('ውጣ');
  });

  it('provides all domain stock and sale statuses in Amharic', () => {
    expect(am.status.IN_STOCK).toBe('በክምችት አለ');
    expect(am.status.LOW_STOCK).toBe('ያነሰ');
    expect(am.status.OUT_OF_STOCK).toBe('ያለቀ');
    expect(am.status.COMPLETED).toBe('ተጠናቋል');
    expect(am.status.VOIDED).toBe('ተሰርዟል');
    expect(am.status.ACTIVE).toBe('ንቁ');
  });

  it('has non-empty string values for all translated keys in Amharic', () => {
    const checkNested = (obj: Record<string, unknown>, path = '') => {
      for (const [k, v] of Object.entries(obj)) {
        const currentPath = path ? `${path}.${k}` : k;
        if (typeof v === 'string') {
          expect(v.trim().length, `Empty translation for key ${currentPath}`).toBeGreaterThan(0);
        } else if (typeof v === 'object' && v !== null) {
          checkNested(v as Record<string, unknown>, currentPath);
        }
      }
    };

    checkNested(am as unknown as Record<string, unknown>);
  });
});
