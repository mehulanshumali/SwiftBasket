import { createFileRoute, redirect } from '@tanstack/react-router'

import { getToken } from '@/api/client'
import { ReceiptPage } from '@/pages/ReceiptPage'

export const Route = createFileRoute('/receipt/$orderId')({
  beforeLoad: () => {
    if (!getToken()) {
      throw redirect({ to: '/login', search: { redirect: location.pathname } })
    }
  },
  component: ReceiptPage,
})
