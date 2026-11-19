import { createFileRoute, redirect } from '@tanstack/react-router'

import { authApi } from '@/api/auth'
import { getToken } from '@/api/client'
import { AdminDashboard } from '@/pages/admin/AdminDashboard'

export const Route = createFileRoute('/admin/')({
  // admin area: must be logged in AND role=ADMIN
  beforeLoad: async () => {
    if (!getToken()) {
      throw redirect({ to: '/login', search: { redirect: location.pathname } })
    }
    try {
      const me = await authApi.me()
      if (me.role !== 'ADMIN') {
        throw redirect({ to: '/' })
      }
    } catch (e) {
      if (e && typeof e === 'object' && 'status' in e) throw e // network/401 — let page handle
      throw redirect({ to: '/' })
    }
  },
  component: AdminDashboard,
})
