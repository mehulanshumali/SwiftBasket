// Shop — Roiser shop-grid: sidebar category filter, search, product grid, pagination

import { useEffect, useState } from 'react'
import { useSearch } from '@tanstack/react-router'

import { SearchX } from 'lucide-react'

import { Pagination } from '@/components/Pagination'
import { ProductCard } from '@/components/ProductCard'
import { Skeleton } from '@/components/ui/skeleton'
import { cn } from '@/lib/utils'
import { type Page, type ProductListItem, type ProductSearchItem, productApi } from '@/api/products'
import { useCategories } from '@/hooks/useCategories'

const PAGE_SIZE = 12

interface ShopSearch {
  q?: string
  categoryId?: number
  page?: number
}

export function ShopPage({ onSearchChange }: { onSearchChange?: (s: ShopSearch) => void }) {
  const search = useSearch({ from: '/shop/' }) as ShopSearch
  const { categories } = useCategories()
  const [result, setResult] = useState<Page<ProductListItem | ProductSearchItem> | null>(null)
  const [error, setError] = useState<string | null>(null)
  const page = search.page ?? 0

  useEffect(() => {
    setResult(null)
    setError(null)
    let active = true
    const params = { page, size: PAGE_SIZE }
    const request =
      search.q !== undefined && search.q !== ''
        ? productApi.search({ q: search.q, ...params })
        : productApi.list({ categoryId: search.categoryId, ...params })
    void request.then(
      (response) => { if (active) setResult(response) },
      (error: Error) => { if (active) setError(error.message) },
    )
    return () => { active = false }
  }, [search.q, search.categoryId, page])

  const activeCat = categories.find((c) => c.id === search.categoryId)

  return (
    <div className="mx-auto max-w-7xl px-4 py-8">
      {/* Heading */}
      <div className="mb-6">
        <h1 className="text-2xl font-bold tracking-tight">
          {search.q
            ? `Search: “${search.q}”`
            : activeCat
              ? activeCat.name
              : 'All Products'}
        </h1>
        <p className="mt-1 text-sm text-muted-foreground">
          {result
            ? `${result.totalElements} product${result.totalElements === 1 ? '' : 's'}`
            : 'Loading…'}
        </p>
      </div>

      <div className="flex flex-col gap-8 lg:flex-row">
        {/* Sidebar: category filter */}
        <aside className="w-full shrink-0 lg:w-56">
          <h2 className="mb-3 text-sm font-semibold uppercase tracking-wide text-muted-foreground">
            Categories
          </h2>
          <ul className="space-y-1">
            <CategoryLink
              label="All Products"
              active={search.categoryId === undefined && !search.q}
              onClick={() => onSearchChange?.({ page: 0 })}
            />
            {categories.map((c) => (
              <CategoryLink
                key={c.id}
                label={c.name}
                active={search.categoryId === c.id}
                onClick={() => onSearchChange?.({ categoryId: c.id, page: 0 })}
              />
            ))}
          </ul>

          {search.q && (
            <button
              onClick={() => onSearchChange?.({ page: 0 })}
              className="mt-4 text-sm font-medium text-primary hover:underline"
            >
              ✕ Clear search “{search.q}”
            </button>
          )}
        </aside>

        {/* Product grid */}
        <div className="flex-1">
          {error && (
            <div className="rounded-md border border-destructive/30 bg-destructive/5 p-6 text-sm text-destructive">
              {error}
            </div>
          )}

          {!error && result === null && (
            <div className="grid grid-cols-2 gap-4 sm:grid-cols-3">
              {Array.from({ length: 9 }).map((_, i) => (
                <div key={i} className="space-y-3">
                  <Skeleton className="aspect-square w-full rounded-md" />
                  <Skeleton className="h-4 w-2/3" />
                  <Skeleton className="h-5 w-1/3" />
                </div>
              ))}
            </div>
          )}

          {result && result.content.length === 0 && (
            <div className="flex flex-col items-center gap-3 py-20 text-center">
              <SearchX className="h-10 w-10 text-muted-foreground/40" />
              <p className="font-medium">No products found</p>
              <p className="text-sm text-muted-foreground">
                {search.q
                  ? 'Try a different search term — our fuzzy search forgives small typos.'
                  : 'Try another category or check back later.'}
              </p>
            </div>
          )}

          {result && result.content.length > 0 && (
            <>
              <div className="grid grid-cols-2 gap-4 sm:grid-cols-3">
                {result.content.map((p) => (
                  <ProductCard key={p.id} product={p} />
                ))}
              </div>
              <div className="mt-8">
                <Pagination
                  page={page}
                  totalPages={result.totalPages}
                  onPage={(p) => onSearchChange?.({
                    q: search.q,
                    categoryId: search.categoryId,
                    page: p,
                  })}
                />
              </div>
            </>
          )}
        </div>
      </div>
    </div>
  )
}

function CategoryLink({
  label,
  active,
  onClick,
}: {
  label: string
  active: boolean
  onClick: () => void
}) {
  return (
    <li>
      <button
        onClick={onClick}
        className={cn(
          'w-full rounded-md px-3 py-2 text-left text-sm transition-colors',
          active
            ? 'bg-primary/10 font-semibold text-primary'
            : 'hover:bg-secondary hover:text-foreground text-muted-foreground',
        )}
      >
        {label}
      </button>
    </li>
  )
}
