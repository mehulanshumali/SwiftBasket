import { createFileRoute, redirect } from '@tanstack/react-router'

import { getToken } from '@/api/client'
import { AccountLayout } from '@/pages/AccountLayout'

export const Route = createFileRoute('/account')({
  // whole account area requires auth
  beforeLoad: () => {
    if (!getToken()) {
      throw redirect({ to: '/login', search: { redirect: location.pathname } })
    }
  },
  component: AccountLayout,
})
