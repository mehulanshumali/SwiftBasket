// Notifications page — account notification feed (email dispatch log)

import { Bell, CheckCheck, MailX, RefreshCw } from 'lucide-react'

import { Pagination } from '@/components/Pagination'
import { Badge } from '@/components/ui/badge'
import { Button } from '@/components/ui/button'
import { Skeleton } from '@/components/ui/skeleton'
import { notificationApi } from '@/api/orders'
import { usePagedList } from '@/hooks/usePagedList'
import { formatDate } from '@/lib/utils'

function StatusBadge({ status }: { status: string }) {
  if (status === 'SENT')
    return (
      <Badge variant="secondary" className="gap-1 bg-green-500/15 text-green-600">
        <CheckCheck className="h-3 w-3" />
        Sent
      </Badge>
    )
  if (status === 'FAILED')
    return (
      <Badge variant="secondary" className="gap-1 bg-red-500/15 text-red-600">
        <MailX className="h-3 w-3" />
        Failed
      </Badge>
    )
  return <Badge variant="secondary">{status}</Badge>
}

export function NotificationsPage() {
  const { page, pageIndex, error, loading, goToPage, reload } = usePagedList(notificationApi.list, 30)
  const items = page?.content

  return (
    <div className="space-y-3">
      <div className="flex items-center justify-between">
        <h2 className="font-bold">Notifications</h2>
        <Button variant="ghost" size="sm" onClick={reload} disabled={loading}>
          <RefreshCw className="h-4 w-4" />
          Refresh
        </Button>
      </div>

      {error && <p role="alert" className="text-sm text-destructive">{error}</p>}

      {loading && (
        <div className="space-y-3">
          {Array.from({ length: 4 }).map((_, i) => (
            <Skeleton key={i} className="h-16 w-full rounded-md" />
          ))}
        </div>
      )}

      {items && items.length === 0 && (
        <div className="flex flex-col items-center gap-4 rounded-md border bg-card py-16 text-center">
          <Bell className="h-12 w-12 text-muted-foreground/40" />
          <div>
            <h3 className="font-bold">No notifications yet</h3>
            <p className="mt-1 text-sm text-muted-foreground">
              Order confirmations and payment receipts land here.
            </p>
          </div>
        </div>
      )}

      {items && items.length > 0 && (
        <div className="space-y-2">
          {items.map((n) => (
            <div
              key={n.id}
              className="flex items-center gap-3 rounded-md border bg-card p-4 text-sm"
            >
              <div className="min-w-0 flex-1">
                <p className="line-clamp-1 font-medium">{n.subject}</p>
                <p className="mt-0.5 text-xs text-muted-foreground">
                  {n.type.replaceAll('_', ' ')} · {formatDate(n.createdAt)}
                </p>
              </div>
              <StatusBadge status={n.status} />
            </div>
          ))}
        </div>
      )}
      {page && <Pagination page={pageIndex} totalPages={page.totalPages} onPage={goToPage} />}
    </div>
  )
}
