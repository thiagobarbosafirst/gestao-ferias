import { createContext, useCallback, useContext, useEffect, useState, type ReactNode } from 'react';
import { authApi, setUnauthorizedHandler, tokenStorage } from './api';
import type { User } from './types';

/**
 * Estado de autenticação partilhado por toda a app (React Context).
 * O token fica no localStorage para sobreviver a um refresh da página.
 */
interface AuthState {
  user: User | null;
  loading: boolean;
  login: (email: string, password: string) => Promise<void>;
  logout: () => void;
}

const AuthContext = createContext<AuthState | null>(null);

export function AuthProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<User | null>(null);
  const [loading, setLoading] = useState(true);

  const logout = useCallback(() => {
    tokenStorage.clear();
    setUser(null);
  }, []);

  // Ao abrir a app: se já existir token, valida-o pedindo /auth/me.
  useEffect(() => {
    setUnauthorizedHandler(logout);
    if (!tokenStorage.get()) {
      setLoading(false);
      return;
    }
    authApi
      .me()
      .then(setUser)
      .catch(() => tokenStorage.clear())
      .finally(() => setLoading(false));
  }, [logout]);

  const login = async (email: string, password: string) => {
    const res = await authApi.login(email, password);
    tokenStorage.set(res.token);
    setUser(res.user);
  };

  return <AuthContext.Provider value={{ user, loading, login, logout }}>{children}</AuthContext.Provider>;
}

export function useAuth(): AuthState {
  const ctx = useContext(AuthContext);
  if (!ctx) throw new Error('useAuth tem de ser usado dentro de <AuthProvider>');
  return ctx;
}
