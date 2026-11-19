import { useState } from 'react'

import { ChevronDown, Loader2, RefreshCw } from 'lucide-react'

import { OrderItems, OrderStatusBadge } from '@/components/OrderDetails'
import { Pagination } from '@/components/Pagination'
import { Button } from '@/components/ui/button'
import { Dialog, DialogContent, DialogFooter, DialogHeader, DialogTitle } from '@/components/ui/dialog'
import { Label } from '@/components/ui/label'
import { Skeleton } from '@/components/ui/skeleton'
import { Textarea } from '@/components/ui/textarea'
import { orderApi, type Tracking } from '@/api/orders'
import { usePagedList } from '@/hooks/usePagedList'
import { formatDate, formatMoney } from '@/lib/utils'

export function AdminOrders() {
  const { page, pageIndex, error, loading, goToPage, reload } = usePagedList(orderApi.history, 50)
  const [notice, setNotice] = useState<string | null>(null)
  const [expanded, setExpanded] = useState<number | null>(null)
  const [tracking, setTracking] = useState<Record<number, Tracking>>({})
  const [change, setChange] = useState<{ orderId: number; status: string } | null>(null)
  const [note, setNote] = useState('')
  const [updating, setUpdating] = useState(false)
  const [updateError, setUpdateError] = useState<string | null>(null)

  function openStatusChange(orderId: number, status: string) {
    setChange({ orderId, status })
    setNote('')
    setUpdateError(null)
  }

  async function saveStatus() {
    if (!change || updating) return
    setUpdating(true)
    setUpdateError(null)
    setNotice(null)
    try {
      const updated = await orderApi.updateStatus(change.orderId, {
        status: change.status,
        note: note.trim() || undefined,
      })
      setNotice(`Order #${updated.orderId}: ${updated.status.replaceAll('_', ' ')}`)
      setExpanded(null)
      setTracking((previous) => {
        const remaining = { ...previous }
        delete remaining[change.orderId]
        return remaining
      })
      setChange(null)
      reload()
    } catch (error) {
      setUpdateError(error instanceof Error ? error.message : 'Status change failed')
    } finally {
      setUpdating(false)
    }
  }

  async function toggleExpand(orderId: number) {
    if (expanded === orderId) {
      setExpanded(null)
      return
    }
    setExpanded(orderId)
    if (!tracking[orderId]) {
      try {
        const t = await orderApi.tracking(orderId)
        setTracking((prev) => ({ ...prev, [orderId]: t }))
      } catch {
        // supplementary
      }
    }
  }

  return (
    <div className="space-y-4">
      <div className="flex items-center justify-between gap-3">
        <h2 className="font-bold">Orders for this account</h2>
        <Button variant="ghost" size="sm" onClick={reload} disabled={loading}>
          <RefreshCw className="h-4 w-4" />
          Refresh
        </Button>
      </div>

      {notice && (
        <p className="rounded-md border border-green-500/30 bg-green-500/5 p-3 text-sm text-green-600">
          {notice}
        </p>
      )}
      {error && (
        <p role="alert" className="rounded-md border border-destructive/30 bg-destructive/5 p-3 text-sm text-destructive">
          {error}
        </p>
      )}

      {loading && (
        <div className="space-y-2">
          {Array.from({ length: 5 }).map((_, i) => (
            <Skeleton key={i} className="h-16 w-full rounded-md" />
          ))}
        </div>
      )}
      {page?.content.length === 0 && (
        <p className="rounded-md border p-8 text-center text-sm text-muted-foreground">
          No orders yet.
        </p>
      )}
      {!!page?.content.length && (
        <div className="space-y-2">
          {page.content.map((order) => {
            const track = tracking[order.orderId]
            const itemCount = order.items.reduce((n, i) => n + i.quantity, 0)
            return (
              <div key={order.orderId} className="rounded-md border bg-card">
                <div className="flex flex-wrap items-center gap-3 p-4">
                  <button
                    type="button"
                    onClick={() => void toggleExpand(order.orderId)}
                    className="flex min-w-0 flex-1 items-center gap-3 text-left"
                  >
                    <ChevronDown
                      className={`h-4 w-4 shrink-0 text-muted-foreground transition-transform ${
                        expanded === order.orderId ? 'rotate-180' : ''
                      }`}
                    />
                    <span className="font-semibold">#{order.orderId}</span>
                    <span className="text-xs text-muted-foreground">
                      user {order.userId} · {formatDate(order.createdAt)} · {itemCount} item
                      {itemCount === 1 ? '' : 's'}
                    </span>
                    <OrderStatusBadge status={order.status} />
                  </button>
                  <span className="font-bold tabular-nums">
                    {formatMoney(order.totalAmount, order.currency)}
                  </span>
                  {!!order.allowedAdminStatuses?.length && (
                    <div className="flex flex-wrap gap-1">
                      {order.allowedAdminStatuses.map((status) => (
                        <Button
                          key={status}
                          size="sm"
                          variant={status === 'CANCELLED' ? 'destructive' : 'outline'}
                          onClick={() => openStatusChange(order.orderId, status)}
                        >
                          {status === 'CANCELLED' ? 'Cancel' : `Mark ${status.toLowerCase().replaceAll('_', ' ')}`}
                        </Button>
                      ))}
                    </div>
                  )}
                </div>

                {expanded === order.orderId && (
                  <div className="border-t px-4 pb-4 pt-3 text-sm">
                    <OrderItems items={order.items} currency={order.currency} />
                    <p className="mt-3 whitespace-pre-line text-xs text-muted-foreground">
                      {order.shippingAddress}
                    </p>
                    {track && (
                      <div className="mt-3">
                        <h4 className="text-xs font-semibold uppercase tracking-wide text-muted-foreground">
                          History
                        </h4>
                        <ul className="mt-1 space-y-1 text-xs text-muted-foreground">
                          {track.timeline
                            .slice()
                            .reverse()
                            .map((e, i) => (
                              <li key={i}>
                                {formatDate(e.changedAt)} — {e.note ?? e.toStatus} ({e.toStatus})
                              </li>
                            ))}
                        </ul>
                      </div>
                    )}
                  </div>
                )}
              </div>
            )
          })}
        </div>
      )}
      {page && <Pagination page={pageIndex} totalPages={page.totalPages} onPage={(index) => {
        setExpanded(null)
        goToPage(index)
      }} />}

      <Dialog open={change !== null} onOpenChange={(open) => {
        if (!open && !updating) setChange(null)
      }}>
        {change && (
          <DialogContent aria-describedby={undefined} showCloseButton={false}>
            <DialogHeader>
              <DialogTitle>Update order #{change.orderId}</DialogTitle>
              <div><OrderStatusBadge status={change.status} /></div>
            </DialogHeader>
            <div className="space-y-2">
              <Label htmlFor="status-note">Tracking note (optional)</Label>
              <Textarea id="status-note" rows={3} maxLength={255} value={note}
                onChange={(event) => setNote(event.target.value)} disabled={updating} />
            </div>
            {updateError && <p role="alert" className="text-destructive">{updateError}</p>}
            <DialogFooter>
              <Button variant="outline" disabled={updating} onClick={() => setChange(null)}>Cancel</Button>
              <Button variant={change.status === 'CANCELLED' ? 'destructive' : 'default'}
                disabled={updating} onClick={() => void saveStatus()}>
                {updating && <Loader2 className="animate-spin" aria-hidden="true" />}
                Save status
              </Button>
            </DialogFooter>
          </DialogContent>
        )}
      </Dialog>
    </div>
  )
}
