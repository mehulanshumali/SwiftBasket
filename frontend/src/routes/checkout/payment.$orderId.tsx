import { createFileRoute, redirect } from '@tanstack/react-router'

import { getToken } from '@/api/client'
import { PaymentPage } from '@/pages/PaymentPage'

export const Route = createFileRoute('/checkout/payment/$orderId')({
  // payment requires an authenticated user (order ownership is checked server-side too)
  beforeLoad: () => {
    if (!getToken()) {
      throw redirect({
        to: '/login',
        search: { redirect: location.pathname },
      })
    }
  },
  component: PaymentPage,
})
