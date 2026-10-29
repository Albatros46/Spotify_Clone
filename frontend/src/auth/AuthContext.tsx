import { createContext, useCallback, useContext, useEffect, useMemo, useState, type ReactNode } from 'react';

import { refreshSession, tokens } from '../api/client';
import { authApi, userApi } from '../api/endpoints';
import type { User } from '../api/types';

interface AuthState {
  user: User | null;
  /** true until we know whether a stored session is still valid */
  loading: boolean;
  login: (email: string, password: string) => Promise<void>;
  register: (name: string, email: string, password: string) => Promise<void>;
  logout: () => Promise<void>;
  setUser: (user: User) => void;
}

const AuthContext = createContext<AuthState | null>(null);

export function AuthProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<User | null>(null);
  const [loading, setLoading] = useState(true);

  // After a reload only the refresh token is left: trade it for a new session
  useEffect(() => {
    tokens.onSessionExpired(() => setUser(null));
    (async () => {
      try {
        if (tokens.refreshToken() && (await refreshSession())) {
          setUser(await userApi.me());
        }
      } catch {
        tokens.clear();
      } finally {
        setLoading(false);
      }
    })();
  }, []);

  const startSession = useCallback((u: User) => {
    tokens.setSession(u.accessToken!, u.refreshToken!);
    setUser({ ...u, accessToken: undefined, refreshToken: undefined });
  }, []);

  const value = useMemo<AuthState>(() => ({
    user,
    loading,
    login: async (email, password) => startSession(await authApi.login(email, password)),
    register: async (name, email, password) => startSession(await authApi.register(name, email, password)),
    logout: async () => {
      try {
        await authApi.logout(); // revokes the refresh token on the server
      } finally {
        tokens.clear();
        setUser(null);
      }
    },
    setUser,
  }), [user, loading, startSession]);

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth(): AuthState {
  const context = useContext(AuthContext);
  if (!context) {
    throw new Error('useAuth must be used inside <AuthProvider>');
  }
  return context;
}
