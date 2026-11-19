import { useCallback, useEffect, useState } from 'react'
import { Link, useParams } from '@tanstack/react-router'

import {
  AlertCircle,
  ArrowLeft,
  BadgeCheck,
  CreditCard,
  Loader2,
  RefreshCw,
  XCircle,
} from 'lucide-react'

import { Button } from '@/components/ui/button'
import { Skeleton } from '@/components/ui/skeleton'
import { ApiError } from '@/api/client'
import { orderApi, paymentApi, type OrderDetail, type PaymentIntentResponse, type PaymentStatus } from '@/api/orders'
import { notifyCartChanged } from '@/context/CartContext'
import { formatMoney } from '@/lib/utils'

type Phase =
  | { kind: 'loading' }
  | { kind: 'intent-error'; message: string }
  | { kind: 'pay'; intent: PaymentIntentResponse; order: OrderDetail }
  | { kind: 'result'; payment: PaymentStatus; order: OrderDetail }

export function PaymentPage() {
  const { orderId } = useParams({ from: '/checkout/payment/$orderId' })
  const [phase, setPhase] = useState<Phase>({ kind: 'loading' })
  const [confirming, setConfirming] = useState(false)
  const [confirmError, setConfirmError] = useState<string | null>(null)

  const load = useCallback(async () => {
    setPhase({ kind: 'loading' })
    setConfirmError(null)
    const id = Number(orderId)
    try {
      const order = await orderApi.get(id)
      try {
        const intent = await paymentApi.createIntent(id)
        setPhase(intent.status === 'REQUIRES_PAYMENT'
          ? { kind: 'pay', intent, order }
          : { kind: 'result', payment: intent, order })
      } catch (error) {
        if (!(error instanceof ApiError) || error.status !== 409) throw error
        const payment = await paymentApi.status(id)
        setPhase({ kind: 'result', payment, order })
        return
      }
    } catch (err) {
      setPhase({
        kind: 'intent-error',
        message: err instanceof Error && err.message ? err.message : 'Could not start payment',
      })
    }
  }, [orderId])

  useEffect(() => {
    void load()
  }, [load])

  async function onConfirm() {
    if (phase.kind !== 'pay' || !phase.intent.testPaymentMethodId || confirming) return
    setConfirming(true)
    setConfirmError(null)
    try {
      const payment = await paymentApi.confirm(phase.order.orderId, phase.intent.testPaymentMethodId)
      if (payment.status === 'SUCCEEDED') notifyCartChanged()
      setPhase({ kind: 'result', payment, order: phase.order })
    } catch (err) {
      setConfirmError(err instanceof Error && err.message ? err.message : 'Could not confirm payment')
    } finally {
      setConfirming(false)
    }
  }

  // ---- loading ----
  if (phase.kind === 'loading') {
    return (
      <div className="mx-auto max-w-2xl space-y-4 px-4 py-8">
        <Skeleton className="h-8 w-56" />
        <Skeleton className="h-64 w-full rounded-md" />
      </div>
    )
  }

  // ---- intent error (e.g. Stripe key missing) ----
  if (phase.kind === 'intent-error') {
    return (
      <div className="mx-auto flex max-w-2xl flex-col items-center gap-4 px-4 py-24 text-center">
        <AlertCircle className="h-12 w-12 text-destructive/60" />
        <h1 className="text-xl font-bold">Payment can’t start</h1>
        <p className="max-w-md text-sm text-muted-foreground">{phase.message}</p>
        <div className="flex gap-3">
          <Button variant="outline" onClick={() => void load()}>
            <RefreshCw className="h-4 w-4" />
            Try again
          </Button>
          <Button asChild variant="ghost">
            <Link to="/cart">Back to cart</Link>
          </Button>
        </div>
      </div>
    )
  }

  const { order } = phase

  // ---- success ----
  if (phase.kind === 'result' && phase.payment.status === 'SUCCEEDED') {
    return (
      <div className="mx-auto flex max-w-2xl flex-col items-center gap-4 px-4 py-20 text-center">
        <BadgeCheck className="h-16 w-16 text-green-600" />
        <h1 className="text-2xl font-bold tracking-tight">Payment successful!</h1>
        <p className="text-sm text-muted-foreground">
          Payment received for order <span className="font-semibold text-foreground">#{order.orderId}</span>.
        </p>
        <div className="mt-2 w-full rounded-md border bg-card p-5 text-left text-sm">
          <div className="flex justify-between">
            <span className="text-muted-foreground">Amount paid</span>
            <span className="font-bold tabular-nums">{formatMoney(phase.payment.amount, phase.payment.currency)}</span>
          </div>
          <div className="mt-2 flex justify-between">
            <span className="text-muted-foreground">Status</span>
            <span className="font-medium text-green-600">{phase.payment.status}</span>
          </div>
          <div className="mt-2 flex justify-between gap-6">
            <span className="text-muted-foreground">Shipping to</span>
            <span className="line-clamp-2 max-w-64 text-right">{order.shippingAddress}</span>
          </div>
        </div>
        <div className="mt-2 flex flex-wrap justify-center gap-3">
          <Button asChild>
            <Link to="/account/orders">View my orders</Link>
          </Button>
          <Button asChild variant="outline">
            <Link to="/receipt/$orderId" params={{ orderId: String(order.orderId) }}>
              View receipt
            </Link>
          </Button>
          <Button asChild variant="ghost">
            <Link to="/shop">Continue shopping</Link>
          </Button>
        </div>
      </div>
    )
  }

  if (phase.kind === 'result') {
    const failed = phase.payment.status === 'FAILED'
    const processing = phase.payment.status === 'PROCESSING'
    return (
      <div className="mx-auto flex max-w-2xl flex-col items-center gap-4 px-4 py-20 text-center">
        {failed ? <XCircle className="h-16 w-16 text-destructive" /> : <AlertCircle className="h-16 w-16 text-muted-foreground" />}
        <h1 className="text-2xl font-bold tracking-tight">
          {failed ? 'Payment failed' : processing ? 'Payment processing' : 'Payment incomplete'}
        </h1>
        <p className="text-sm text-muted-foreground">Order #{order.orderId}</p>
        <div className="mt-2 flex gap-3">
          {!failed && (
            <Button onClick={() => void load()}>
              <RefreshCw className="h-4 w-4" />
              Refresh status
            </Button>
          )}
          <Button asChild variant="outline">
            <Link to="/cart">Back to cart</Link>
          </Button>
        </div>
      </div>
    )
  }

  const amount = formatMoney(phase.intent.amount, phase.intent.currency)
  return (
    <div className="mx-auto max-w-2xl px-4 py-8">
      <Button asChild variant="ghost" size="sm" className="mb-4 -ml-2">
        <Link to="/cart">
          <ArrowLeft className="h-4 w-4" />
          Back to cart
        </Link>
      </Button>

      <h1 className="text-2xl font-bold tracking-tight">Payment</h1>
      <p className="mt-1 text-sm text-muted-foreground">
        Order <span className="font-semibold text-foreground">#{order.orderId}</span> ·{' '}
        <span className="font-bold tabular-nums text-foreground">{amount}</span>
      </p>

      <div className="mt-6 rounded-md border bg-card p-6">
        <h2 className="flex items-center gap-2 font-bold">
          <CreditCard className="h-5 w-5 text-primary" />
          Card
        </h2>

        {phase.intent.testPaymentMethodId ? (
          <div className="mt-4 space-y-3">
            <p className="text-sm text-muted-foreground">Stripe test mode</p>
            {confirmError && <p role="alert" className="text-sm text-destructive">{confirmError}</p>}
            <Button className="w-full" size="lg" onClick={() => void onConfirm()} disabled={confirming}>
              {confirming ? <Loader2 className="h-4 w-4 animate-spin" /> : <CreditCard className="h-4 w-4" />}
              {confirming ? 'Processing…' : `Pay ${amount}`}
            </Button>
          </div>
        ) : (
          <p className="mt-4 text-sm text-muted-foreground">Card payment is unavailable.</p>
        )}
      </div>
    </div>
  )
}
