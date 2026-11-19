// Payment receipt — PRD 5.3: receipt view after successful payment
// GET /payments/orders/{orderId}/receipt (409 until payment SUCCEEDED)

import { useEffect, useState } from 'react'
import { Link, useParams } from '@tanstack/react-router'

import { Printer } from 'lucide-react'

import { OrderItems } from '@/components/OrderDetails'
import { Button } from '@/components/ui/button'
import { Separator } from '@/components/ui/separator'
import { Skeleton } from '@/components/ui/skeleton'
import { orderApi, paymentApi, type OrderDetail, type Receipt } from '@/api/orders'
import { formatDate, formatMoney } from '@/lib/utils'

export function ReceiptPage() {
  const { orderId } = useParams({ from: '/receipt/$orderId' })
  const [receipt, setReceipt] = useState<Receipt | null>(null)
  const [order, setOrder] = useState<OrderDetail | null>(null)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    const id = Number(orderId)
    Promise.all([paymentApi.receipt(id), orderApi.get(id)])
      .then(([r, o]) => {
        setReceipt(r)
        setOrder(o)
      })
      .catch((e: Error) =>
        setError(
          /no payment found|not found/i.test(e.message)
            ? 'No payment found for this order yet — complete payment first.'
            : /not.*succeed|409/i.test(e.message)
              ? 'This order has no successful payment yet — complete payment first.'
              : e.message,
        ),
      )
  }, [orderId])

  if (error) {
    return (
      <div className="mx-auto flex max-w-md flex-col items-center gap-4 px-4 py-24 text-center">
        <p className="text-sm text-destructive">{error}</p>
        <Button asChild variant="outline">
          <Link
            to="/checkout/payment/$orderId"
            params={{ orderId }}
          >
            Go to payment
          </Link>
        </Button>
      </div>
    )
  }

  if (!receipt || !order) {
    return (
      <div className="mx-auto max-w-lg space-y-4 px-4 py-8">
        <Skeleton className="h-8 w-48" />
        {Array.from({ length: 4 }).map((_, i) => (
          <Skeleton key={i} className="h-12 w-full rounded-md" />
        ))}
      </div>
    )
  }

  return (
    <div className="mx-auto max-w-lg px-4 py-8">
      <div className="flex items-center justify-between">
        <h1 className="text-2xl font-bold tracking-tight">Payment Receipt</h1>
        <Button variant="outline" size="sm" onClick={() => window.print()}>
          <Printer className="h-4 w-4" />
          Print
        </Button>
      </div>

      <div className="mt-6 rounded-md border bg-card p-6">
        {/* header */}
        <div className="flex items-start justify-between">
          <div>
            <p className="text-lg font-bold">
              Swift<span className="text-primary">Basket</span>
            </p>
            <p className="text-xs text-muted-foreground">Order #{receipt.orderId}</p>
          </div>
          <div className="text-right">
            <p className="font-mono text-sm font-semibold">{receipt.receiptNumber}</p>
            <p className="text-xs text-muted-foreground">{formatDate(receipt.paidAt)}</p>
          </div>
        </div>

        <Separator className="my-4" />

        <OrderItems items={order.items} currency={order.currency} />

        <Separator className="my-4" />

        <div className="flex justify-between text-base font-bold">
          <span>Total paid</span>
          <span className="tabular-nums">{formatMoney(receipt.amount, receipt.currency)}</span>
        </div>
        <p className="mt-1 text-right text-xs text-muted-foreground">{receipt.currency}</p>

        <Separator className="my-4" />

        {/* meta */}
        <dl className="space-y-1.5 text-sm">
          <div className="flex justify-between">
            <dt className="text-muted-foreground">Payment method</dt>
            <dd>{receipt.paymentMethod}</dd>
          </div>
          <div className="flex justify-between">
            <dt className="text-muted-foreground">Payment ID</dt>
            <dd>#{receipt.paymentId}</dd>
          </div>
          <div className="flex justify-between">
            <dt className="text-muted-foreground">Stripe reference</dt>
            <dd className="font-mono text-xs">{receipt.stripePaymentIntentId}</dd>
          </div>
          <div className="flex justify-between">
            <dt className="text-muted-foreground">Delivered to</dt>
            <dd className="whitespace-pre-line text-right">{order.shippingAddress}</dd>
          </div>
        </dl>
      </div>

      <div className="mt-6 flex justify-center gap-3">
        <Button asChild variant="outline">
          <Link to="/account/orders">View my orders</Link>
        </Button>
        <Button asChild>
          <Link to="/shop">Continue shopping</Link>
        </Button>
      </div>
    </div>
  )
}
