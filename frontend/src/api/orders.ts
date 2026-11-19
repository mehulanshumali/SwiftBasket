// order-service (4.1–4.5) + payment-service (5.1–5.4) endpoints

import { api, type Page } from './client'

interface OrderItem {
  productId: number
  productName: string
  unitPrice: number
  quantity: number
}

export interface OrderDetail {
  orderId: number
  userId: number
  status: string
  allowedAdminStatuses?: string[]
  totalAmount: number
  currency: string
  shippingAddress: string
  items: OrderItem[]
  createdAt: string
}

interface TrackingEntry {
  fromStatus: string | null
  toStatus: string
  note: string | null
  changedAt: string
}

export interface Tracking {
  orderId: number
  currentStatus: string
  // backend field is "timeline" (not "history")
  timeline: TrackingEntry[]
}

export const orderApi = {
  checkout: (body: { shippingAddress: string }) =>
    api.post<OrderDetail>('/orders', body),

  history: (page = 0, size = 10) =>
    api.get<Page<OrderDetail>>(`/orders?page=${page}&size=${size}`),

  get: (id: number) => api.get<OrderDetail>(`/orders/${id}`),

  tracking: (id: number) => api.get<Tracking>(`/orders/${id}/tracking`),

  updateStatus: (id: number, body: { status: string; note?: string }) =>
    api.patch<OrderDetail>(`/orders/${id}/status`, body),
}

export interface PaymentStatus {
  paymentId: number
  orderId: number
  amount: number
  currency: string
  status: string
}

export interface PaymentIntentResponse extends PaymentStatus {
  testPaymentMethodId?: string | null
}

export interface Receipt {
  receiptNumber: string
  paymentId: number
  orderId: number
  amount: number
  currency: string
  // backend field name
  paymentMethod: string
  paidAt: string
  stripePaymentIntentId: string
}

export const paymentApi = {
  createIntent: (orderId: number) =>
    api.post<PaymentIntentResponse>(`/payments/orders/${orderId}/intent`),

  confirm: (orderId: number, paymentMethodId: string) =>
    api.post<PaymentStatus>(`/payments/orders/${orderId}/confirm`, { paymentMethodId }),

  status: (orderId: number) =>
    api.get<PaymentStatus>(`/payments/orders/${orderId}`),

  receipt: (orderId: number) =>
    api.get<Receipt>(`/payments/orders/${orderId}/receipt`),
}

// notification-service (6.1)
export interface NotificationItem {
  id: number
  recipientEmail: string
  type: string
  status: string
  subject: string
  kafkaTopic: string | null
  createdAt: string
  sentAt: string | null
}

export const notificationApi = {
  list: (page = 0, size = 10) =>
    api.get<Page<NotificationItem>>(`/notifications?page=${page}&size=${size}`),
}
