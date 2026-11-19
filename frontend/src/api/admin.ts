// Admin API — product/category CRUD, order status, ES reindex (ADMIN role)

import { api } from './client'
import type { Category, ProductDetail } from './products'

export type { Category }

export interface ProductInput {
  name: string
  description: string
  price: number
  stockQuantity: number
  imageUrl?: string
  sku: string
  categoryId: number
}

// JSON body shape for the wire (imageUrl optional)
function toBody(body: ProductInput) {
  return {
    name: body.name,
    description: body.description,
    price: body.price,
    stockQuantity: body.stockQuantity,
    imageUrl: body.imageUrl?.trim() || null,
    sku: body.sku,
    categoryId: body.categoryId,
  }
}

export const adminApi = {
  // categories
  createCategory: (body: { name: string; description?: string }) =>
    api.post<Category>('/categories', body),

  updateCategory: (id: number, body: { name: string; description?: string }) =>
    api.put<Category>(`/categories/${id}`, body),

  deleteCategory: (id: number) => api.delete<void>(`/categories/${id}`),

  // products
  createProduct: (body: ProductInput) => api.post<ProductDetail>('/products', toBody(body)),

  updateProduct: (id: number, body: ProductInput) =>
    api.put<ProductDetail>(`/products/${id}`, toBody(body)),

  deleteProduct: (id: number) => api.delete<void>(`/products/${id}`),

  // Elasticsearch reindex
  reindex: () => api.post<number>('/products/reindex'),
}
