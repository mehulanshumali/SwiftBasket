// Order history — list orders, expandable detail with items and tracking timeline

import { useState } from 'react'
import { Link } from '@tanstack/react-router'

import { ChevronDown, Package, RefreshCw } from 'lucide-react'

import { OrderItems, OrderStatusBadge } from '@/components/OrderDetails'
import { Pagination } from '@/components/Pagination'
import { Button } from '@/components/ui/button'
import { Skeleton } from '@/components/ui/skeleton'
import { orderApi, type Tracking } from '@/api/orders'
import { usePagedList } from '@/hooks/usePagedList'
import { formatDate, formatMoney } from '@/lib/utils'

export function OrdersPage() {
  const { page, pageIndex, error, goToPage, reload } = usePagedList(orderApi.history, 20)
  const [expanded, setExpanded] = useState<number | null>(null)
  const [tracking, setTracking] = useState<Record<number, Tracking>>({})
  const [loadingTracking, setLoadingTracking] = useState(false)

  async function toggleExpand(orderId: number) {
    if (expanded === orderId) {
      setExpanded(null)
      return
    }
    setExpanded(orderId)
    if (!tracking[orderId]) {
      setLoadingTracking(true)
      try {
        const t = await orderApi.tracking(orderId)
        setTracking((prev) => ({ ...prev, [orderId]: t }))
      } catch {
        // tracking is supplementary — leave it hidden on error
      } finally {
        setLoadingTracking(false)
      }
    }
  }

  if (error) {
    return (
      <div className="flex flex-wrap items-center justify-between gap-3">
        <p role="alert" className="text-sm text-destructive">{error}</p>
        <Button variant="outline" size="sm" onClick={reload}>Try again</Button>
      </div>
    )
  }

  if (!page) {
    return (
      <div className="space-y-3">
        {Array.from({ length: 3 }).map((_, i) => (
          <Skeleton key={i} className="h-24 w-full rounded-md" />
        ))}
      </div>
    )
  }

  if (page.content.length === 0) {
    return (
      <div className="flex flex-col items-center gap-4 rounded-md border bg-card py-16 text-center">
        <Package className="h-12 w-12 text-muted-foreground/40" />
        <div>
          <h2 className="font-bold">No orders yet</h2>
          <p className="mt-1 text-sm text-muted-foreground">Your orders will appear here.</p>
        </div>
        <Button asChild>
          <Link to="/shop">Start shopping</Link>
        </Button>
      </div>
    )
  }

  return (
    <div className="space-y-3">
      <div className="flex items-center justify-between">
        <h2 className="font-bold">Order history</h2>
        <Button variant="ghost" size="sm" onClick={() => {
          setExpanded(null)
          setTracking({})
          reload()
        }}>
          <RefreshCw className="h-4 w-4" />
          Refresh
        </Button>
      </div>

      {page.content.map((order) => {
        const isOpen = expanded === order.orderId
        const itemCount = order.items.reduce((n, i) => n + i.quantity, 0)
        const track = tracking[order.orderId]
        return (
          <div key={order.orderId} className="rounded-md border bg-card">
            <button
              type="button"
              onClick={() => void toggleExpand(order.orderId)}
              className="flex w-full items-center gap-4 p-4 text-left"
            >
              <div className="min-w-0 flex-1">
                <div className="flex flex-wrap items-center gap-2">
                  <span className="font-semibold">#{order.orderId}</span>
                  <OrderStatusBadge status={order.status} />
                  {order.status === 'PENDING' && (
                    <Link
                      to="/checkout/payment/$orderId"
                      params={{ orderId: String(order.orderId) }}
                      onClick={(e) => e.stopPropagation()}
                      className="text-xs font-medium text-primary hover:underline"
                    >
                      Complete payment →
                    </Link>
                  )}
                  {order.status !== 'PENDING' && order.status !== 'CANCELLED' && order.status !== 'PAYMENT_FAILED' && (
                    <Link
                      to="/receipt/$orderId"
                      params={{ orderId: String(order.orderId) }}
                      onClick={(e) => e.stopPropagation()}
                      className="text-xs font-medium text-primary hover:underline"
                    >
                      Receipt
                    </Link>
                  )}
                </div>
                <p className="mt-1 text-xs text-muted-foreground">
                  {formatDate(order.createdAt)} · {itemCount} item{itemCount === 1 ? '' : 's'}
                </p>
              </div>
              <span className="font-bold tabular-nums">{formatMoney(order.totalAmount, order.currency)}</span>
              <ChevronDown
                className={`h-5 w-5 shrink-0 text-muted-foreground transition-transform ${isOpen ? 'rotate-180' : ''}`}
              />
            </button>

            {isOpen && (
              <div className="border-t px-4 pb-4 pt-3">
                <OrderItems items={order.items} currency={order.currency} linkProducts />
                <p className="mt-3 whitespace-pre-line text-xs text-muted-foreground">
                  {order.shippingAddress}
                </p>

                <div className="mt-4">
                  <h3 className="text-xs font-semibold uppercase tracking-wide text-muted-foreground">
                    Tracking
                  </h3>
                  {loadingTracking && !track ? (
                    <Skeleton className="mt-2 h-12 w-full" />
                  ) : track ? (
                    <ol className="mt-2 space-y-0">
                      {track.timeline.map((entry, i) => (
                        <li key={i} className="relative flex gap-3 pb-4 last:pb-0">
                          {i < track.timeline.length - 1 && (
                            <span className="absolute left-[5px] top-3 h-full w-px bg-border" />
                          )}
                          <span
                            className={`mt-1.5 h-[11px] w-[11px] shrink-0 rounded-full border-2 ${
                              i === track.timeline.length - 1
                                ? 'border-primary bg-primary'
                                : 'border-border bg-background'
                            }`}
                          />
                          <div className="min-w-0 text-sm">
                            <p className="font-medium">{entry.note ?? entry.toStatus}</p>
                            <p className="text-xs text-muted-foreground">
                              {formatDate(entry.changedAt)} · {entry.toStatus}
                            </p>
                          </div>
                        </li>
                      ))}
                    </ol>
                  ) : (
                    <p className="mt-1 text-xs text-muted-foreground">
                      Tracking unavailable for this order.
                    </p>
                  )}
                </div>
              </div>
            )}
          </div>
        )
      })}
      <Pagination page={pageIndex} totalPages={page.totalPages} onPage={(index) => {
        setExpanded(null)
        goToPage(index)
      }} />
    </div>
  )
}
