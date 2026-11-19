// Checkout page — address form + order placement → redirects to payment page

import { useEffect, useState } from 'react'
import { Link, useNavigate } from '@tanstack/react-router'

import { AlertCircle, CreditCard, Loader2, MapPin } from 'lucide-react'

import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { Separator } from '@/components/ui/separator'
import { Skeleton } from '@/components/ui/skeleton'
import { Textarea } from '@/components/ui/textarea'
import { cartApi, type Cart } from '@/api/cart'
import { orderApi } from '@/api/orders'
import { useAuth } from '@/context/AuthContext'
import { notifyCartChanged } from '@/context/CartContext'
import { formatMoney } from '@/lib/utils'

interface AddressFields {
  fullName: string
  phone: string
  address: string
}

export function CheckoutPage() {
  const navigate = useNavigate()
  const { user } = useAuth()
  const [cart, setCart] = useState<Cart | null>(null)
  const [fields, setFields] = useState<AddressFields>({ fullName: '', phone: '', address: '' })
  const [fieldErrors, setFieldErrors] = useState<Partial<AddressFields>>({})
  const [formError, setFormError] = useState<string | null>(null)
  const [loading, setLoading] = useState(true)
  const [placing, setPlacing] = useState(false)

  // load cart + prefill from profile
  useEffect(() => {
    let cancelled = false
    async function load() {
      try {
        const c = await cartApi.get()
        if (!cancelled) setCart(c)
      } catch {
        setFormError('Could not load your cart')
      } finally {
        setLoading(false)
      }
    }
    void load()
    return () => {
      cancelled = true
    }
  }, [])

  useEffect(() => {
    if (user) {
      setFields((f) => ({
        fullName: f.fullName || (user.fullName ?? ''),
        phone: f.phone || (user.phoneNumber ?? ''),
        address: f.address || (user.address ?? ''),
      }))
    }
  }, [user])

  function validate(): boolean {
    const errors: Partial<AddressFields> = {}
    if (!fields.fullName.trim()) errors.fullName = 'Full name is required'
    if (!fields.phone.trim()) errors.phone = 'Phone number is required'
    else if (!/^\d{10}$/.test(fields.phone.replace(/\D/g, '').replace(/^91/, '')))
      errors.phone = 'Enter a valid 10-digit phone number'
    if (!fields.address.trim()) errors.address = 'Address is required'
    else if (fields.address.trim().length < 10) errors.address = 'Address is too short — include street, city and PIN code'
    setFieldErrors(errors)
    return Object.keys(errors).length === 0
  }

  async function onPlaceOrder(e: React.FormEvent) {
    e.preventDefault()
    setFormError(null)
    if (!validate()) return
    setPlacing(true)
    try {
      // backend takes a single address string — compose name + phone + address
      const shippingAddress = `${fields.fullName.trim()} · ${fields.phone.trim()}\n${fields.address.trim()}`
      const order = await orderApi.checkout({ shippingAddress })
      notifyCartChanged()
      void navigate({
        to: '/checkout/payment/$orderId',
        params: { orderId: String(order.orderId) },
      })
    } catch (err) {
      setFormError(
        err instanceof Error && err.message
          ? err.message
          : 'Could not place your order. Please try again.',
      )
      setPlacing(false)
    }
  }

  if (loading) {
    return (
      <div className="mx-auto max-w-5xl space-y-4 px-4 py-8">
        <Skeleton className="h-8 w-48" />
        <div className="flex gap-8">
          <Skeleton className="h-96 flex-1 rounded-md" />
          <Skeleton className="h-96 w-80 rounded-md" />
        </div>
      </div>
    )
  }

  if (!cart || cart.items.length === 0) {
    return (
      <div className="mx-auto flex max-w-3xl flex-col items-center gap-4 px-4 py-24 text-center">
        <CreditCard className="h-12 w-12 text-muted-foreground/40" />
        <h1 className="text-xl font-bold">Nothing to check out</h1>
        <p className="text-sm text-muted-foreground">Your cart is empty.</p>
        <Button asChild>
          <Link to="/shop">Start shopping</Link>
        </Button>
      </div>
    )
  }

  return (
    <div className="mx-auto max-w-5xl px-4 py-8">
      <h1 className="mb-6 text-2xl font-bold tracking-tight">Checkout</h1>

      {formError && (
        <p className="mb-4 flex items-start gap-2 rounded-md border border-destructive/30 bg-destructive/5 p-3 text-sm text-destructive">
          <AlertCircle className="mt-0.5 h-4 w-4 shrink-0" />
          {formError}
        </p>
      )}

      <div className="flex flex-col gap-10 lg:flex-row">
        {/* Address form */}
        <form onSubmit={onPlaceOrder} className="flex-1 space-y-5" noValidate>
          <div className="rounded-md border bg-card p-6">
            <h2 className="flex items-center gap-2 text-lg font-bold">
              <MapPin className="h-5 w-5 text-primary" />
              Shipping address
            </h2>
            <div className="mt-5 space-y-4">
              <div className="grid gap-4 sm:grid-cols-2">
                <div className="space-y-2">
                  <Label htmlFor="fullName">Full name</Label>
                  <Input
                    id="fullName"
                    autoComplete="name"
                    value={fields.fullName}
                    onChange={(e) => setFields({ ...fields, fullName: e.target.value })}
                    aria-invalid={!!fieldErrors.fullName}
                  />
                  {fieldErrors.fullName && (
                    <p className="text-xs text-destructive">{fieldErrors.fullName}</p>
                  )}
                </div>
                <div className="space-y-2">
                  <Label htmlFor="phone">Phone number</Label>
                  <Input
                    id="phone"
                    type="tel"
                    autoComplete="tel"
                    placeholder="98XXXXXXXX"
                    value={fields.phone}
                    onChange={(e) => setFields({ ...fields, phone: e.target.value })}
                    aria-invalid={!!fieldErrors.phone}
                  />
                  {fieldErrors.phone && (
                    <p className="text-xs text-destructive">{fieldErrors.phone}</p>
                  )}
                </div>
              </div>
              <div className="space-y-2">
                <Label htmlFor="address">Address</Label>
                <Textarea
                  id="address"
                  autoComplete="street-address"
                  rows={3}
                  placeholder="House/flat, street, city, state, PIN code"
                  value={fields.address}
                  onChange={(e) => setFields({ ...fields, address: e.target.value })}
                  aria-invalid={!!fieldErrors.address}
                />
                {fieldErrors.address && (
                  <p className="text-xs text-destructive">{fieldErrors.address}</p>
                )}
              </div>
            </div>
          </div>

          <Button type="submit" size="lg" className="w-full sm:w-auto" disabled={placing}>
            {placing ? (
              <Loader2 className="h-4 w-4 animate-spin" />
            ) : (
              <CreditCard className="h-4 w-4" />
            )}
            {placing ? 'Placing order…' : 'Continue to payment'}
          </Button>
        </form>

        {/* Order summary */}
        <aside className="h-fit w-full shrink-0 rounded-md border bg-card p-6 lg:w-80">
          <h2 className="text-lg font-bold">Your order</h2>
          <div className="mt-4 space-y-3">
            {cart.items.map((item) => (
              <div key={item.productId} className="flex items-center justify-between gap-3 text-sm">
                <span className="min-w-0 flex-1">
                  <span className="line-clamp-1 font-medium">{item.name}</span>
                  <span className="text-muted-foreground">× {item.quantity}</span>
                </span>
                <span className="tabular-nums">{formatMoney(item.lineTotal, cart.currency)}</span>
              </div>
            ))}
          </div>
          <Separator className="my-4" />
          <div className="flex justify-between gap-3 text-base font-bold">
            <span>Cart total</span>
            <span className="tabular-nums">{formatMoney(cart.totalPrice, cart.currency)}</span>
          </div>
        </aside>
      </div>
    </div>
  )
}
