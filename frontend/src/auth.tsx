import { createContext, ReactNode, useCallback, useContext, useEffect, useState } from 'react'
import { api } from './api'
import type { Me } from './types'

interface AuthState {
  /** undefined while we are still asking the server; null when nobody is signed in */
  me: Me | null | undefined
  login: (email: string, password: string) => Promise<void>
  register: (input: RegisterInput) => Promise<void>
  logout: () => Promise<void>
}

export interface RegisterInput {
  name: string
  email: string
  phone: string
  password: string
  consent: boolean
  website?: string
}

const AuthContext = createContext<AuthState | undefined>(undefined)

export function AuthProvider({ children }: { children: ReactNode }) {
  const [me, setMe] = useState<Me | null | undefined>(undefined)

  useEffect(() => {
    api<Me>('/api/v1/auth/me').then(setMe).catch(() => setMe(null))
  }, [])

  // the session ended (for example after hours of an open page): go back to signed-out so the sign-in page shows
  useEffect(() => {
    const out = () => setMe(null)
    window.addEventListener('mealup:signed-out', out)
    return () => window.removeEventListener('mealup:signed-out', out)
  }, [])

  const login = useCallback(async (email: string, password: string) => {
    setMe(await api<Me>('/api/v1/auth/login', { method: 'POST', body: { email, password } }))
  }, [])

  const register = useCallback(async (input: RegisterInput) => {
    await api('/api/v1/public/register', { method: 'POST', body: input })
    setMe(await api<Me>('/api/v1/auth/me'))
  }, [])

  const logout = useCallback(async () => {
    await api<void>('/api/v1/auth/logout', { method: 'POST', body: {} }).catch(() => undefined)
    setMe(null)
  }, [])

  return <AuthContext.Provider value={{ me, login, register, logout }}>{children}</AuthContext.Provider>
}

export function useAuth(): AuthState {
  const ctx = useContext(AuthContext)
  if (!ctx) throw new Error('useAuth must be used inside AuthProvider')
  return ctx
}
