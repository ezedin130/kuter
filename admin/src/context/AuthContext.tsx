import React, { createContext, useContext, useState, useEffect, useCallback } from 'react';
import { UserProfileResponse } from '../api/types';
import { apiClient } from '../api/client';

interface AuthContextType {
  user: UserProfileResponse | null;
  token: string | null;
  isAuthenticated: boolean;
  isLoading: boolean;
  error: string | null;
  login: (email: string, password: string) => Promise<void>;
  logout: () => Promise<void>;
  refreshUser: () => Promise<void>;
}

const AuthContext = createContext<AuthContextType | undefined>(undefined);

export const AuthProvider: React.FC<{ children: React.ReactNode }> = ({ children }) => {
  const [token, setTokenState] = useState<string | null>(() => apiClient.getToken());
  const [user, setUser] = useState<UserProfileResponse | null>(null);
  const [isLoading, setIsLoading] = useState<boolean>(true);
  const [error, setError] = useState<string | null>(null);

  const loadProfile = useCallback(async () => {
    const existingToken = apiClient.getToken();
    if (!existingToken) {
      setUser(null);
      setTokenState(null);
      setIsLoading(false);
      return;
    }

    try {
      setIsLoading(true);
      setError(null);
      const profile = await apiClient.getMe();

      if (!profile.isPlatformAdmin) {
        apiClient.setToken(null);
        setTokenState(null);
        setUser(null);
        setError('Access denied: Platform Administrator privileges are required to access this portal.');
        return;
      }

      setUser(profile);
      setTokenState(existingToken);
    } catch (err: unknown) {
      apiClient.setToken(null);
      setTokenState(null);
      setUser(null);
      setError(err instanceof Error ? err.message : 'Session expired or invalid.');
    } finally {
      setIsLoading(false);
    }
  }, []);

  useEffect(() => {
    loadProfile();
  }, [loadProfile]);

  const login = async (email: string, password: string) => {
    setIsLoading(true);
    setError(null);
    try {
      const auth = await apiClient.login({ email, password });
      if (!auth.user.isPlatformAdmin) {
        apiClient.setToken(null);
        setTokenState(null);
        setUser(null);
        throw new Error('Access denied: You do not have Platform Administrator permissions.');
      }
      setTokenState(auth.accessToken);
      // Fetch full profile
      const profile = await apiClient.getMe();
      setUser(profile);
    } catch (err: unknown) {
      setError(err instanceof Error ? err.message : 'Authentication failed.');
      throw err;
    } finally {
      setIsLoading(false);
    }
  };

  const logout = async () => {
    setIsLoading(true);
    try {
      await apiClient.logout();
    } catch {
      // Ignore network errors during logout
    } finally {
      apiClient.setToken(null);
      setTokenState(null);
      setUser(null);
      setIsLoading(false);
    }
  };

  const refreshUser = async () => {
    await loadProfile();
  };

  return (
    <AuthContext.Provider
      value={{
        user,
        token,
        isAuthenticated: !!token && !!user && user.isPlatformAdmin,
        isLoading,
        error,
        login,
        logout,
        refreshUser,
      }}
    >
      {children}
    </AuthContext.Provider>
  );
};

export function useAuth(): AuthContextType {
  const context = useContext(AuthContext);
  if (!context) {
    throw new Error('useAuth must be used within an AuthProvider');
  }
  return context;
}
