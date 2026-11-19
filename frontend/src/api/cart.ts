// cart-service endpoints (3.1–3.6)

import { api } from './client'

interface CartItem {
  productId: number
  name: string
  imageUrl: string | null
  unitPrice: number
  quantity: number
  lineTotal: number
}

export interface Cart {
  cartId: string
  items: CartItem[]
  totalPrice: number
  currency: string
}

export const cartApi = {
  get: () => api.get<Cart>('/cart'),

  addItem: (body: { productId: number; quantity: number }) =>
    api.post<Cart>('/cart/items', body),

  // NOTE: backend expects quantity as a query param, not a JSON body
  updateQuantity: (productId: number, quantity: number) =>
    api.patch<Cart>(`/cart/items/${productId}?quantity=${quantity}`),

  removeItem: (productId: number) =>
    api.delete<Cart>(`/cart/items/${productId}`),

  clear: () => api.delete<void>('/cart'),

  merge: () => api.post<Cart>('/cart/merge'),
}
