import { Link } from '@tanstack/react-router'

import type { OrderDetail } from '@/api/orders'
import { Badge } from '@/components/ui/badge'
import { formatMoney } from '@/lib/utils'

const statusColors: Record<string, string> = {
  PENDING: 'bg-amber-500/15 text-amber-600',
  PAID: 'bg-green-500/15 text-green-600',
  PROCESSING: 'bg-blue-500/15 text-blue-600',
  SHIPPED: 'bg-blue-500/15 text-blue-600',
  DELIVERED: 'bg-emerald-500/15 text-emerald-600',
  CANCELLED: 'bg-red-500/15 text-red-600',
  PAYMENT_FAILED: 'bg-red-500/15 text-red-600',
}

export function OrderStatusBadge({ status }: { status: string }) {
  return (
    <Badge variant="secondary" className={statusColors[status] ?? ''}>
      {status.replaceAll('_', ' ')}
    </Badge>
  )
}

export function OrderItems({ items, currency, linkProducts = false }: Pick<OrderDetail, 'items' | 'currency'> & {
  linkProducts?: boolean
}) {
  return (
    <ul className="space-y-2 text-sm">
      {items.map((item) => (
        <li key={item.productId} className="flex justify-between gap-3">
          <span className="min-w-0 flex-1 wrap-anywhere">
            {linkProducts ? (
              <Link to="/products/$id" params={{ id: String(item.productId) }} className="hover:text-primary">
                {item.productName}
              </Link>
            ) : item.productName}{' '}
            <span className="whitespace-nowrap text-muted-foreground">&times; {item.quantity}</span>
          </span>
          <span className="text-right tabular-nums">{formatMoney(item.unitPrice, currency)} each</span>
        </li>
      ))}
    </ul>
  )
}