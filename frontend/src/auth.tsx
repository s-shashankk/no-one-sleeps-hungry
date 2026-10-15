import { createContext, ReactNode, useCallback, useContext, useEffect, useState } from 'react'
import { api } from './api'
import type { Me } from './types'

interface AuthState {
  /** undefined while we are still asking the server; null when nobody is signed in */
  me: Me | null | undefined
  login: (email: string, password: string) => Promise<void>
  logout: () => Promise<void>
}

const AuthContext = createContext<AuthState | undefined>(undefined)

export function AuthProvider({ children }: { children: ReactNode }) {
  const [me, setMe] = useState<Me | null | undefined>(undefined)

  useEffect(() => {
    api<Me>('/api/v1/auth/me').then(setMe).catch(() => setMe(null))
  }, [])

  const login = useCallback(async (email: string, password: string) => {
    setMe(await api<Me>('/api/v1/auth/login', { method: 'POST', body: { email, password } }))
  }, [])

  const logout = useCallback(async () => {
    await api<void>('/api/v1/auth/logout', { method: 'POST', body: {} }).catch(() => undefined)
    setMe(null)
  }, [])

  return <AuthContext.Provider value={{ me, login, logout }}>{children}</AuthContext.Provider>
}

export function useAuth(): AuthState {
  const ctx = useContext(AuthContext)
  if (!ctx) throw new Error('useAuth must be used inside AuthProvider')
  return ctx
}
