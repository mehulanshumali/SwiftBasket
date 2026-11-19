// Cart page — Roiser cart: line items with qty steppers, remove, clear, totals

import { useEffect, useState } from 'react'
import { Link, useNavigate } from '@tanstack/react-router'

import { Loader2, Package, ShoppingCart, Trash2 } from 'lucide-react'
import { toast } from 'sonner'

import { QuantityStepper } from '@/components/QuantityStepper'
import { Button } from '@/components/ui/button'
import { Skeleton } from '@/components/ui/skeleton'
import { cartApi, type Cart } from '@/api/cart'
import { notifyCartChanged, useCart } from '@/context/CartContext'
import { formatMoney } from '@/lib/utils'

export function CartPage() {
  const navigate = useNavigate()
  const { refresh } = useCart()
  const [cart, setCart] = useState<Cart | null>(null)
  const [error, setError] = useState<string | null>(null)
  const [busyLine, setBusyLine] = useState<number | null>(null)
  const [clearing, setClearing] = useState(false)

  async function reload() {
    try {
      setCart(await cartApi.get())
    } catch (e) {
      setError(e instanceof Error ? e.message : 'Could not load cart')
    }
  }

  useEffect(() => {
    void reload()
  }, [])

  async function updateQty(productId: number, quantity: number) {
    if (quantity < 1) return
    setBusyLine(productId)
    try {
      const updated = await cartApi.updateQuantity(productId, quantity)
      setCart(updated)
      notifyCartChanged()
    } catch (e) {
      setError(e instanceof Error ? e.message : 'Could not update quantity')
      toast.error('Could not update quantity')
    } finally {
      setBusyLine(null)
    }
  }

  async function removeItem(productId: number) {
    setBusyLine(productId)
    try {
      const updated = await cartApi.removeItem(productId)
      setCart(updated)
      notifyCartChanged()
    } catch (e) {
      setError(e instanceof Error ? e.message : 'Could not remove item')
      toast.error('Could not remove item')
    } finally {
      setBusyLine(null)
    }
  }

  async function clearCart() {
    setClearing(true)
    try {
      await cartApi.clear()
      await refresh()
      await reload()
      toast.success('Cart cleared')
    } catch (e) {
      setError(e instanceof Error ? e.message : 'Could not clear cart')
      toast.error('Could not clear cart')
    } finally {
      setClearing(false)
    }
  }

  if (error && !cart) {
    return (
      <div className="mx-auto max-w-3xl px-4 py-20 text-center">
        <p className="text-sm text-destructive">{error}</p>
      </div>
    )
  }

  if (!cart) {
    return (
      <div className="mx-auto max-w-5xl space-y-4 px-4 py-8">
        <Skeleton className="h-8 w-40" />
        {Array.from({ length: 2 }).map((_, i) => (
          <Skeleton key={i} className="h-28 w-full rounded-md" />
        ))}
      </div>
    )
  }

  if (cart.items.length === 0) {
    return (
      <div className="mx-auto flex max-w-3xl flex-col items-center gap-4 px-4 py-24 text-center">
        <ShoppingCart className="h-12 w-12 text-muted-foreground/40" />
        <h1 className="text-xl font-bold">Your cart is empty</h1>
        <p className="text-sm text-muted-foreground">
          Browse the catalog and add something you like.
        </p>
        <Button asChild>
          <Link to="/shop">Start shopping</Link>
        </Button>
      </div>
    )
  }

  return (
    <div className="mx-auto max-w-5xl px-4 py-8">
      <div className="mb-6 flex items-center justify-between">
        <h1 className="text-2xl font-bold tracking-tight">
          Shopping Cart <span className="text-base font-normal text-muted-foreground">({cart.items.length} {cart.items.length === 1 ? 'item' : 'items'})</span>
        </h1>
        <Button variant="ghost" size="sm" onClick={clearCart} disabled={clearing}>
          {clearing ? <Loader2 className="h-4 w-4 animate-spin" /> : <Trash2 className="h-4 w-4" />}
          Clear cart
        </Button>
      </div>

      {error && (
        <p className="mb-4 rounded-md border border-destructive/30 bg-destructive/5 p-3 text-sm text-destructive">
          {error}
        </p>
      )}

      <div className="flex flex-col gap-10 lg:flex-row">
        {/* Lines */}
        <div className="flex-1 space-y-4">
          {cart.items.map((item) => (
            <div
              key={item.productId}
              className="grid grid-cols-[auto_minmax(0,1fr)] items-center gap-3 rounded-sm border bg-card p-3 sm:flex sm:gap-4 sm:p-4"
            >
              {/* Thumbnail */}
              <Link
                to="/products/$id"
                params={{ id: String(item.productId) }}
                aria-label={`View ${item.name}`}
                className="grid size-16 shrink-0 place-items-center overflow-hidden rounded-sm bg-secondary sm:size-20"
              >
                {item.imageUrl ? (
                  <img src={item.imageUrl} alt="" className="size-full object-contain" />
                ) : (
                  <Package className="size-6 text-muted-foreground/45" aria-hidden="true" />
                )}
              </Link>

              {/* Name + unit price */}
              <div className="min-w-0 flex-1">
                <Link
                  to="/products/$id"
                  params={{ id: String(item.productId) }}
                  className="block font-medium wrap-anywhere hover:text-primary"
                >
                  {item.name}
                </Link>
                <p className="mt-1 text-sm text-muted-foreground">
                  {formatMoney(item.unitPrice, cart.currency)} each
                </p>
              </div>

              {/* Qty + line total */}
              <div className="col-span-2 flex items-center gap-2 sm:gap-6">
                <QuantityStepper
                  value={item.quantity}
                  onChange={(q) => void updateQty(item.productId, q)}
                  disabled={busyLine === item.productId}
                />
                <span className="ml-auto min-w-0 text-right font-semibold wrap-anywhere tabular-nums sm:w-24">
                  {formatMoney(item.lineTotal, cart.currency)}
                </span>
                <Button
                  variant="ghost"
                  size="icon"
                  aria-label={`Remove ${item.name}`}
                  disabled={busyLine === item.productId}
                  onClick={() => void removeItem(item.productId)}
                >
                  {busyLine === item.productId ? (
                    <Loader2 className="h-4 w-4 animate-spin" />
                  ) : (
                    <Trash2 className="h-4 w-4 text-muted-foreground" />
                  )}
                </Button>
              </div>
            </div>
          ))}
        </div>

        {/* Summary */}
        <aside className="h-fit w-full shrink-0 rounded-md border bg-card p-6 lg:w-80">
          <h2 className="text-lg font-bold">Order Summary</h2>
          <div className="mt-4 flex justify-between gap-3 text-base font-bold">
            <span>Cart total</span>
            <span className="tabular-nums">{formatMoney(cart.totalPrice, cart.currency)}</span>
          </div>
          <Button
            className="mt-6 w-full"
            size="lg"
            onClick={() => void navigate({ to: '/checkout' })}
          >
            Proceed to Checkout
          </Button>
          <Button asChild variant="ghost" className="mt-2 w-full">
            <Link to="/shop">Continue shopping</Link>
          </Button>
        </aside>
      </div>
    </div>
  )
}
