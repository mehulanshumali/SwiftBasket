import { api, type Page } from './client'

export type { Page }

export interface Category {
  id: number
  name: string
  description?: string | null
}

export interface ProductListItem {
  id: number
  sku: string
  name: string
  description: string
  price: number
  currency: string
  imageUrl: string | null
  stockQuantity: number
  categoryName: string
  categoryId: number
  active: boolean
}

export type ProductDetail = ProductListItem

export type ProductSearchItem = Pick<
  ProductListItem,
  'name' | 'description' | 'price' | 'currency' | 'imageUrl' | 'categoryName' | 'active'
> & { id: string }

export const productApi = {
  list: (params: { categoryId?: number; page?: number; size?: number }) => {
    const q = new URLSearchParams()
    if (params.categoryId !== undefined) q.set('categoryId', String(params.categoryId))
    if (params.page !== undefined) q.set('page', String(params.page))
    if (params.size !== undefined) q.set('size', String(params.size))
    return api.get<Page<ProductListItem>>(`/products?${q.toString()}`)
  },

  search: (params: { q: string; page?: number; size?: number }) => {
    const q = new URLSearchParams({ q: params.q })
    if (params.page !== undefined) q.set('page', String(params.page))
    if (params.size !== undefined) q.set('size', String(params.size))
    return api.get<Page<ProductSearchItem>>(`/products/search?${q.toString()}`)
  },

  get: (id: number) => api.get<ProductDetail>(`/products/${id}`),

  categories: () => api.get<Category[]>('/categories'),
}
