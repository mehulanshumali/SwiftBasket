// Global cart state: item count for the header badge + fetch helpers.
// Listens to the 'cart:changed' window event so any mutation anywhere
// (add-to-cart button, cart page, login merge) refreshes the badge.

import {
  createContext,
  useCallback,
  useContext,
  useEffect,
  useMemo,
  useState,
  type ReactNode,
} from 'react'

import { cartApi, type Cart } from '@/api/cart'

interface CartState {
  itemCount: number
  totalPrice: number | null
  currency: string | null
  refresh: () => Promise<void>
}

const CartContext = createContext<CartState | null>(null)

export function CartProvider({ children }: { children: ReactNode }) {
  const [cart, setCart] = useState<Cart | null>(null)

  const refresh = useCallback(async () => {
    try {
      setCart(await cartApi.get())
    } catch {
      setCart(null)
    }
  }, [])

  useEffect(() => {
    const onChange = () => void refresh()
    window.addEventListener('cart:changed', onChange)
    void refresh()
    return () => window.removeEventListener('cart:changed', onChange)
  }, [refresh])

  const value = useMemo(
    () => ({
      itemCount: cart?.items.reduce((count, item) => count + item.quantity, 0) ?? 0,
      totalPrice: cart?.totalPrice ?? null,
      currency: cart?.currency ?? null,
      refresh,
    }),
    [cart, refresh],
  )

  return <CartContext.Provider value={value}>{children}</CartContext.Provider>
}

export function useCart(): CartState {
  const ctx = useContext(CartContext)
  if (!ctx) throw new Error('useCart must be used inside CartProvider')
  return ctx
}

// fire from anywhere after a cart mutation
export function notifyCartChanged() {
  window.dispatchEvent(new Event('cart:changed'))
}
