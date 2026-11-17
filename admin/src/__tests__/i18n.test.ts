import { describe, it, expect } from 'vitest';
import { en } from '../i18n/locales/en';
import { am } from '../i18n/locales/am';

describe('Admin Localization (i18n)', () => {
  it('has identical top-level namespaces in both English and Amharic', () => {
    const enKeys = Object.keys(en).sort();
    const amKeys = Object.keys(am).sort();
    expect(amKeys).toEqual(enKeys);
  });

  it('provides all common action buttons in Amharic', () => {
    expect(am.common.save).toBe('አስቀምጥ');
    expect(am.common.cancel).toBe('ይቅር');
    expect(am.common.delete).toBe('ሰርዝ');
    expect(am.common.edit).toBe('አስተካክል');
    expect(am.common.confirm).toBe('አረጋግጥ');
    expect(am.common.close).toBe('ዝጋ');
    expect(am.common.actions).toBe('ተግባራት');
  });

  it('provides sidebar and header navigation in Amharic', () => {
    expect(am.sidebar.overview).toBe('አጠቃላይ እይታ');
    expect(am.sidebar.requests).toBe('የምዝገባ ጥያቄዎች');
    expect(am.sidebar.tenants).toBe('ሱቆች እና ተከራዮች');
    expect(am.sidebar.plans).toBe('የደንበኝነት እቅዶች');
    expect(am.sidebar.subscriptions).toBe('የሱቆች ደንበኝነት');
    expect(am.header.platformAdministration).toBe('የፕላትፎርም አስተዳደር');
    expect(am.header.signOut).toBe('ውጣ');
  });

  it('provides all domain statuses in Amharic', () => {
    expect(am.status.ACTIVE).toBe('ንቁ');
    expect(am.status.PENDING).toBe('በመጠባበቅ ላይ');
    expect(am.status.APPROVED).toBe('የፀደቀ');
    expect(am.status.REJECTED).toBe('ውድቅ የተደረገ');
    expect(am.status.SUSPENDED).toBe('የታገደ');
    expect(am.status.CANCELLED).toBe('የተሰረዘ');
    expect(am.status.EXPIRED).toBe('ጊዜው ያለፈበት');
    expect(am.status.TRIAL).toBe('የሙከራ ጊዜ');
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
