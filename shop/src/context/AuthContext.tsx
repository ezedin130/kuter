import React, { createContext, useContext, useState, useEffect, useCallback, useMemo } from 'react';
import { shopApi } from '../api/client';
import { UserProfile, TenantMembership, UserRole, ShopPermission } from '../api/types';

interface AuthContextValue {
  user: UserProfile | null;
  activeTenant: TenantMembership | null;
  role: UserRole | null;
  isAuthenticated: boolean;
  isLoading: boolean;
  error: string | null;
  login: (email: string, password: string) => Promise<void>;
  logout: () => void;
  switchTenant: (tenantId: string) => void;
  refreshUser: () => Promise<void>;
  isOwner: boolean;
  hasPermission: (permission: ShopPermission) => boolean;
  canManageCatalog: boolean;
  canManagePrices: boolean;
  canManageUsers: boolean;
  canManageInventory: boolean;
  canReadInventory: boolean;
  canProcessSales: boolean;
  canVoidSales: boolean;
  canViewFinancialReports: boolean;
  canManageSettings: boolean;
  canViewLending: boolean;
  canManageLending: boolean;
}

const AuthContext = createContext<AuthContextValue | undefined>(undefined);

export const AuthProvider: React.FC<{ children: React.ReactNode }> = ({ children }) => {
  const [user, setUser] = useState<UserProfile | null>(null);
  const [activeTenantId, setActiveTenantIdState] = useState<string | null>(() => shopApi.getActiveTenantId());
  const [isLoading, setIsLoading] = useState<boolean>(true);
  const [error, setError] = useState<string | null>(null);

  const activeTenant = useMemo(() => {
    if (!user || !user.memberships || user.memberships.length === 0) return null;
    if (activeTenantId) {
      const match = user.memberships.find((m) => m.tenantId === activeTenantId);
      if (match) return match;
    }
    return user.memberships[0];
  }, [user, activeTenantId]);

  const role: UserRole | null = activeTenant ? activeTenant.role : null;

  const refreshUser = useCallback(async () => {
    const token = shopApi.getToken();
    if (!token) {
      setUser(null);
      setIsLoading(false);
      return;
    }

    try {
      setIsLoading(true);
      setError(null);
      const res = await shopApi.getMe();
      if (res.data) {
        setUser(res.data);
        const currentActive = shopApi.getActiveTenantId();
        if (!currentActive && res.data.memberships.length > 0) {
          const firstTenant = res.data.memberships[0].tenantId;
          shopApi.setActiveTenantId(firstTenant);
          setActiveTenantIdState(firstTenant);
        } else if (currentActive) {
          setActiveTenantIdState(currentActive);
        }
      }
    } catch (err: any) {
      console.error('Failed to load user session:', err);
      shopApi.clearSession();
      setUser(null);
      setError(err.message || 'Session expired. Please log in again.');
    } finally {
      setIsLoading(false);
    }
  }, []);

  useEffect(() => {
    refreshUser();
  }, [refreshUser]);

  const login = async (email: string, password: string) => {
    setIsLoading(true);
    setError(null);
    try {
      const res = await shopApi.login(email, password);
      if (res.data?.user) {
        await refreshUser();
      }
    } catch (err: any) {
      const msg = err.message || 'Login failed. Please check credentials.';
      setError(msg);
      throw err;
    } finally {
      setIsLoading(false);
    }
  };

  const logout = () => {
    shopApi.clearSession();
    setUser(null);
    setActiveTenantIdState(null);
    setError(null);
  };

  const switchTenant = (tenantId: string) => {
    if (!user) return;
    const membership = user.memberships.find((m) => m.tenantId === tenantId);
    if (membership) {
      shopApi.setActiveTenantId(tenantId);
      setActiveTenantIdState(tenantId);
    }
  };

  // Role & Granular Permissions
  const isOwner = role === 'OWNER';
  const permissions: ShopPermission[] = useMemo(() => activeTenant?.permissions || [], [activeTenant]);

  const hasPermission = useCallback((permission: ShopPermission): boolean => {
    if (role === 'OWNER') return true;
    return permissions.includes(permission);
  }, [role, permissions]);

  const canManageCatalog = isOwner || hasPermission('PRODUCTS_MANAGE') || role === 'MANAGER';
  const canManagePrices = isOwner; // Non-delegable owner privilege
  const canManageUsers = isOwner;  // Non-delegable owner privilege
  const canManageInventory = isOwner || hasPermission('INVENTORY_RECEIVE') || hasPermission('INVENTORY_ADJUST') || role === 'MANAGER' || role === 'STOREKEEPER';
  const canReadInventory = true;
  const canProcessSales = isOwner || hasPermission('SALES_CREATE') || role === 'CASHIER' || role === 'MANAGER';
  const canVoidSales = isOwner || role === 'MANAGER';
  const canViewFinancialReports = isOwner || hasPermission('SALES_REPORTS_VIEW') || role === 'MANAGER';
  const canManageSettings = isOwner || role === 'MANAGER';
  const canViewLending = isOwner || hasPermission('LENDING_VIEW') || hasPermission('LENDING_REQUEST') || hasPermission('LENDING_MANAGE') || role === 'MANAGER' || role === 'STOREKEEPER';
  const canManageLending = isOwner || hasPermission('LENDING_MANAGE') || hasPermission('LENDING_REQUEST') || role === 'MANAGER' || role === 'STOREKEEPER';

  const value: AuthContextValue = {
    user,
    activeTenant,
    role,
    isAuthenticated: !!user && !!activeTenant,
    isLoading,
    error,
    login,
    logout,
    switchTenant,
    refreshUser,
    isOwner,
    hasPermission,
    canManageCatalog,
    canManagePrices,
    canManageUsers,
    canManageInventory,
    canReadInventory,
    canProcessSales,
    canVoidSales,
    canViewFinancialReports,
    canManageSettings,
    canViewLending,
    canManageLending,
  };

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
};

export const useAuth = (): AuthContextValue => {
  const context = useContext(AuthContext);
  if (!context) {
    throw new Error('useAuth must be used within an AuthProvider');
  }
  return context;
};
