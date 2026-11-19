import { createFileRoute, redirect } from '@tanstack/react-router'

import { getToken } from '@/api/client'
import { CheckoutPage } from '@/pages/CheckoutPage'

export const Route = createFileRoute('/checkout/')({
  // guest checkout blocked — JWT must exist (page APIs surface 401 if stale)
  beforeLoad: () => {
    if (!getToken()) {
      throw redirect({
        to: '/login',
        search: { redirect: '/checkout' },
      })
    }
  },
  component: CheckoutPage,
})
