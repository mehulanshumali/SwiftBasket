import { useState } from 'react'
import { Link, useNavigate } from '@tanstack/react-router'

import { Search, ShoppingBag, ShoppingBasket } from 'lucide-react'

import { useCart } from '@/context/CartContext'
import { formatMoney } from '@/lib/utils'

export function Header() {
  const [query, setQuery] = useState('')
  const navigate = useNavigate()
  const { itemCount, totalPrice, currency } = useCart()

  function submitSearch(event: React.FormEvent<HTMLFormElement>) {
    event.preventDefault()
    const q = query.trim()
    void navigate({ to: '/shop', search: q ? { q } : {} })
  }

  return (
    <header className="bg-background">
      <div className="storefront-container grid grid-cols-[minmax(0,1fr)_auto] items-center gap-x-3 gap-y-2 py-2.5 md:grid-cols-[auto_minmax(0,1fr)_auto] md:gap-x-7 md:py-4 lg:gap-x-12">
        <Link to="/" className="inline-flex w-fit max-w-full items-center gap-2 rounded-sm text-lg font-extrabold sm:text-2xl">
          <span className="flex size-8 shrink-0 items-center justify-center rounded-sm bg-primary text-primary-foreground sm:size-10">
            <ShoppingBasket className="size-5 sm:size-6" aria-hidden="true" />
          </span>
          <span className="min-w-0 wrap-anywhere">SwiftBasket</span>
        </Link>

        <form
          onSubmit={submitSearch}
          role="search"
          className="col-span-2 row-start-2 flex min-w-0 items-center gap-1 rounded-sm border bg-muted/40 p-0.5 focus-within:border-primary md:col-span-1 md:col-start-2 md:row-start-1"
        >
          <label htmlFor="storefront-search" className="sr-only">Search products</label>
          <div className="flex min-w-0 flex-1 items-center gap-2 pl-2.5 sm:pl-3">
            <Search className="hidden size-4 shrink-0 text-muted-foreground sm:block" aria-hidden="true" />
            <input
              id="storefront-search"
              type="search"
              value={query}
              onChange={(event) => setQuery(event.target.value)}
              placeholder="Search products..."
              className="h-9 min-w-0 flex-1 bg-transparent pr-2 text-base outline-none md:h-10 md:text-sm"
            />
          </div>
          <button
            type="submit"
            aria-label="Search"
            title="Search"
            className="flex h-9 min-w-10 shrink-0 items-center justify-center rounded-sm bg-primary px-3 text-sm font-bold text-primary-foreground transition-colors hover:bg-primary/90 md:h-10 md:px-5"
          >
            <Search className="size-4 md:hidden" aria-hidden="true" />
            <span className="hidden md:inline">Search</span>
          </button>
        </form>

        <Link
          to="/cart"
          aria-label={`Cart, ${itemCount} ${itemCount === 1 ? 'item' : 'items'}`}
          title="View cart"
          className="col-start-2 row-start-1 flex min-h-10 min-w-10 items-center justify-center gap-2.5 rounded-sm transition-colors hover:text-primary md:col-start-3"
        >
          <span className="relative">
            <ShoppingBag className="size-5 sm:size-6" aria-hidden="true" />
            {itemCount > 0 && (
              <span className="absolute -right-2 -top-1.5 flex h-4 min-w-4 items-center justify-center rounded-full bg-primary px-1 text-[10px] font-bold text-primary-foreground" aria-hidden="true">
                {itemCount > 99 ? '99+' : itemCount}
              </span>
            )}
          </span>
          <span className="hidden flex-col leading-tight lg:flex">
            <span className="text-xs text-muted-foreground">Your cart</span>
            {totalPrice !== null && (
              <span className="text-sm font-bold tabular-nums">{formatMoney(totalPrice, currency)}</span>
            )}
          </span>
        </Link>
      </div>
    </header>
  )
}
