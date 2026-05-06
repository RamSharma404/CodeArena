import React, { createContext, useContext, useState, useEffect } from 'react';
import API from '../api/axios';

const AuthContext = createContext(null);

export function AuthProvider({ children }) {
  const [user, setUser] = useState(null);
  const [loading, setLoading] = useState(true);

  // Restore session on mount by calling /api/auth/me
  // This checks if the HttpOnly cookie is still valid
  useEffect(() => {
    const checkAuth = async () => {
      try {
        const res = await API.get('/api/auth/me');
        const { username, email, role } = res.data;
        setUser({ username, email, role });
      } catch {
        // No valid cookie — user is not logged in
        setUser(null);
        localStorage.removeItem('user');
      } finally {
        setLoading(false);
      }
    };
    checkAuth();
  }, []);

  // Called after successful login/verify-otp
  // Token is already set as HttpOnly cookie by the server
  const login = (authResponse) => {
    const { username, email, role } = authResponse;
    const userData = { username, email, role };
    setUser(userData);
    localStorage.setItem('user', JSON.stringify(userData));
  };

  const logout = async () => {
    try {
      await API.post('/api/auth/logout');
    } catch (e) {
      // ignore — server might be down
    }
    setUser(null);
    localStorage.removeItem('user');
  };

  const isAuthenticated = !!user;

  return (
    <AuthContext.Provider value={{ user, loading, isAuthenticated, login, logout }}>
      {children}
    </AuthContext.Provider>
  );
}

export function useAuth() {
  const ctx = useContext(AuthContext);
  if (!ctx) throw new Error('useAuth must be used within AuthProvider');
  return ctx;
}
