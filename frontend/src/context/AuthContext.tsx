// Global auth state: JWT lifecycle + cart merge on login + role for admin guards.

import {
  createContext,
  useCallback,
  useContext,
  useEffect,
  useMemo,
  useState,
  type ReactNode,
} from 'react'

import { authApi, type UserProfile } from '@/api/auth'
import { cartApi } from '@/api/cart'
import { clearSessionId, clearToken, getToken, hasSessionId, setToken } from '@/api/client'

interface AuthState {
  user: UserProfile | null
  loading: boolean // true while restoring session on app boot
  login: (email: string, password: string) => Promise<void>
  register: (email: string, password: string, fullName: string) => Promise<void>
  logout: () => void
  refreshUser: () => Promise<void>
}

const AuthContext = createContext<AuthState | null>(null)

export function AuthProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<UserProfile | null>(null)
  const [loading, setLoading] = useState(true)

  const refreshUser = useCallback(async () => {
    if (!getToken()) {
      setUser(null)
      return
    }
    try {
      setUser(await authApi.me())
    } catch {
      // token invalid/expired — drop it
      clearToken()
      setUser(null)
    }
  }, [])

  // restore session on first mount
  useEffect(() => {
    refreshUser().finally(() => setLoading(false))
  }, [refreshUser])

  const login = useCallback(async (email: string, password: string) => {
    const res = await authApi.login({ email, password })
    setToken(res.token)

    // merge guest cart into the user cart, then drop the guest session
    if (hasSessionId()) {
      try {
        await cartApi.merge()
      } catch {
        // merge is best-effort — login itself succeeded
      }
      clearSessionId()
    }

    setUser(await authApi.me())
    window.dispatchEvent(new Event('cart:changed'))
  }, [])

  const register = useCallback(async (email: string, password: string, fullName: string) => {
    await authApi.register({ email, password, fullName })
    // auto-login after registration (same session model as login)
    await login(email, password)
  }, [login])

  const logout = useCallback(() => {
    clearToken()
    setUser(null)
    window.dispatchEvent(new Event('cart:changed'))
  }, [])

  const value = useMemo(
    () => ({ user, loading, login, register, logout, refreshUser }),
    [user, loading, login, register, logout, refreshUser],
  )

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}

export function useAuth(): AuthState {
  const ctx = useContext(AuthContext)
  if (!ctx) throw new Error('useAuth must be used inside AuthProvider')
  return ctx
}
