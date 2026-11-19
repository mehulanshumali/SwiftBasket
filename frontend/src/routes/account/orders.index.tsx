import { createFileRoute } from '@tanstack/react-router'

import { OrdersPage } from '@/pages/OrdersPage'

export const Route = createFileRoute('/account/orders/')({
  component: OrdersPage,
})
