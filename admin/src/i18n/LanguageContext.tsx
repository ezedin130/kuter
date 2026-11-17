import React, { createContext, useContext, useState, useEffect, useMemo, useCallback } from 'react';
import { Language, TranslationDictionary } from './types';
import { en } from './locales/en';
import { am } from './locales/am';

const STORAGE_KEY = 'kuter_admin_language';

interface LanguageContextType {
  language: Language;
  setLanguage: (lang: Language) => void;
  toggleLanguage: () => void;
  t: (path: string, params?: Record<string, string | number>) => string;
  formatDate: (isoStr?: string) => string;
  formatCurrency: (val?: number) => string;
  translateStatus: (status?: string) => string;
}

const dictionaries: Record<Language, TranslationDictionary> = { en, am };

const LanguageContext = createContext<LanguageContextType | undefined>(undefined);

export const LanguageProvider: React.FC<{ children: React.ReactNode }> = ({ children }) => {
  const [language, setLanguageState] = useState<Language>(() => {
    const saved = localStorage.getItem(STORAGE_KEY);
    if (saved === 'en' || saved === 'am') {
      return saved;
    }
    // Default to Amharic as requested
    return 'am';
  });

  const setLanguage = useCallback((newLang: Language) => {
    setLanguageState(newLang);
    try {
      localStorage.setItem(STORAGE_KEY, newLang);
      document.documentElement.lang = newLang;
    } catch {
      // Storage quota or private mode fallback
    }
  }, []);

  const toggleLanguage = useCallback(() => {
    setLanguage(language === 'am' ? 'en' : 'am');
  }, [language, setLanguage]);

  useEffect(() => {
    document.documentElement.lang = language;
  }, [language]);

  const t = useCallback(
    (path: string, params?: Record<string, string | number>): string => {
      const parts = path.split('.');
      let current: unknown = dictionaries[language];

      for (const part of parts) {
        if (current && typeof current === 'object' && part in current) {
          current = (current as Record<string, unknown>)[part];
        } else {
          current = undefined;
          break;
        }
      }

      // Fallback to English if missing in Amharic
      if (typeof current !== 'string') {
        let fallback: unknown = dictionaries.en;
        for (const part of parts) {
          if (fallback && typeof fallback === 'object' && part in fallback) {
            fallback = (fallback as Record<string, unknown>)[part];
          } else {
            fallback = undefined;
            break;
          }
        }
        current = typeof fallback === 'string' ? fallback : path;
      }

      let result = current as string;
      if (params) {
        for (const [k, v] of Object.entries(params)) {
          result = result.replace(new RegExp(`\\{${k}\\}`, 'g'), String(v));
        }
      }
      return result;
    },
    [language]
  );

  const formatCurrency = useCallback(
    (val?: number): string => {
      if (val === undefined || val === null) {
        return language === 'am' ? '0.00 ብር' : 'Br 0.00';
      }
      const formatted = Number(val).toLocaleString('en-US', {
        minimumFractionDigits: 2,
        maximumFractionDigits: 2,
      });
      return language === 'am' ? `${formatted} ብር` : `Br ${formatted}`;
    },
    [language]
  );

  const formatDate = useCallback(
    (isoStr?: string): string => {
      if (!isoStr) return '—';
      try {
        const d = new Date(isoStr);
        if (isNaN(d.getTime())) return isoStr;

        if (language === 'am') {
          // Amharic formatted date representation
          return d.toLocaleDateString('am-ET', {
            year: 'numeric',
            month: 'short',
            day: 'numeric',
          });
        }
        return d.toLocaleDateString('en-US', {
          year: 'numeric',
          month: 'short',
          day: 'numeric',
        });
      } catch {
        return isoStr;
      }
    },
    [language]
  );

  const translateStatus = useCallback(
    (status?: string): string => {
      if (!status) return '—';
      const upper = status.toUpperCase();
      const statusDict = dictionaries[language].status;
      if (upper in statusDict) {
        return statusDict[upper as keyof typeof statusDict];
      }
      return status;
    },
    [language]
  );

  const value = useMemo(
    () => ({
      language,
      setLanguage,
      toggleLanguage,
      t,
      formatDate,
      formatCurrency,
      translateStatus,
    }),
    [language, setLanguage, toggleLanguage, t, formatDate, formatCurrency, translateStatus]
  );

  return <LanguageContext.Provider value={value}>{children}</LanguageContext.Provider>;
};

export const useLanguage = (): LanguageContextType => {
  const context = useContext(LanguageContext);
  if (!context) {
    throw new Error('useLanguage must be used within a LanguageProvider');
  }
  return context;
};
