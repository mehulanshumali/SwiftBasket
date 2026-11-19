// user-service endpoints (1.1–1.10)

import { api } from './client'

export interface UserProfile {
  id: number
  email: string
  fullName: string | null
  phoneNumber: string | null
  address: string | null
  role: 'USER' | 'ADMIN'
  createdAt: string
}

export interface LoginResponse {
  token: string
  expiresIn: number
  userId: number
  email: string
}

export const authApi = {
  register: (body: { email: string; password: string; fullName: string }) =>
    api.post<{ id: number; email: string; fullName: string; role: string }>('/auth/register', body),

  login: (body: { email: string; password: string }) =>
    api.post<LoginResponse>('/auth/login', body),

  me: () => api.get<UserProfile>('/auth/me'),

  updateMe: (body: { fullName?: string; phoneNumber?: string; address?: string; email?: string }) =>
    api.put<UserProfile>('/auth/me', body),

  deleteMe: () => api.delete<void>('/auth/me'),

  // both endpoints return plain strings, not JSON objects
  requestPasswordReset: (email: string) =>
    api.post<string>('/auth/password-reset', { email }),

  resetPassword: (token: string, newPassword: string) =>
    api.post<string>('/auth/reset-password', { token, newPassword }),
}
