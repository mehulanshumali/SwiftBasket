import { useEffect, useState } from 'react'
import { Link } from '@tanstack/react-router'

import { ArrowRight, ArrowUpRight, LayoutGrid, RotateCw } from 'lucide-react'

import { ProductCard } from '@/components/ProductCard'
import { Skeleton } from '@/components/ui/skeleton'
import { type ProductListItem, productApi } from '@/api/products'
import { useCategories } from '@/hooks/useCategories'

type Featured = { status: 'loading' } | { status: 'unavailable' } | { status: 'ready'; products: ProductListItem[] }

export function HomePage() {
  const { categories, loading, unavailable, refresh } = useCategories()
  const [featured, setFeatured] = useState<Featured>({ status: 'loading' })
  const [attempt, setAttempt] = useState(0)

  useEffect(() => {
    let active = true
    void productApi.list({ size: 8 }).then(
      ({ content }) => {
        if (active) setFeatured({ status: 'ready', products: content })
      },
      () => {
        if (active) setFeatured({ status: 'unavailable' })
      },
    )
    return () => { active = false }
  }, [attempt])

  return (
    <>
      <section className="storefront-hero" aria-labelledby="storefront-title">
        <img
          className="storefront-hero-photo"
          src={`${import.meta.env.BASE_URL}storefront/everyday-tech.webp`}
          width={1600}
          height={1067}
          fetchPriority="high"
          alt=""
        />
        <div className="storefront-container storefront-hero-inner">
          <div className="py-7 sm:py-10">
            <h1 id="storefront-title" className="storefront-hero-title">
              SwiftBasket
            </h1>
            <Link
              to="/shop"
              className="mt-5 inline-flex min-h-11 items-center gap-3 rounded-sm bg-primary px-4 text-sm font-bold text-primary-foreground transition-colors hover:bg-primary/90 focus-visible:outline-2 focus-visible:outline-offset-4 focus-visible:outline-current sm:mt-6 sm:px-5"
            >
              Shop now <ArrowRight className="size-4" aria-hidden="true" />
            </Link>
          </div>
        </div>
      </section>

      <section aria-labelledby="category-title">
        <div className="storefront-container py-6 sm:py-11">
          <SectionHeading id="category-title" title="Shop by category" linkLabel="Browse all" />
          {unavailable && <CatalogUnavailable message="Categories are temporarily unavailable." onRetry={() => void refresh()} />}
          {!loading && !unavailable && !categories.length && (
            <p className="border-y border-dashed py-6 text-sm text-muted-foreground">No categories to show yet.</p>
          )}
          <div className="grid grid-cols-2 gap-3 sm:grid-cols-3 lg:grid-cols-4" aria-busy={loading}>
            {loading && !categories.length && Array.from({ length: 4 }, (_, index) => (
              <Skeleton key={index} className="h-14 w-full rounded-sm" aria-hidden="true" />
            ))}
            {categories.map((category) => (
              <Link
                key={category.id}
                to="/shop"
                search={{ categoryId: category.id }}
                className="group flex min-h-14 min-w-0 items-center gap-2 rounded-sm border bg-card px-2.5 py-2 text-sm font-semibold hover:border-primary hover:text-primary focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-primary sm:gap-3 sm:px-4"
              >
                <LayoutGrid className="size-4 shrink-0 text-primary" aria-hidden="true" />
                <span className="min-w-0 grow wrap-anywhere">{category.name}</span>
                <ArrowUpRight className="size-4 shrink-0 text-muted-foreground group-hover:text-primary" aria-hidden="true" />
              </Link>
            ))}
          </div>
        </div>
      </section>

      <section className="border-t bg-muted/50" aria-labelledby="featured-title">
        <div className="storefront-container py-8 sm:py-12">
          <SectionHeading id="featured-title" title="Products" linkLabel="Shop all" />
          {featured.status === 'unavailable' && (
            <CatalogUnavailable
              message="The catalog is temporarily unavailable."
              onRetry={() => {
                setFeatured({ status: 'loading' })
                setAttempt((previous) => previous + 1)
              }}
            />
          )}
          {featured.status === 'ready' && !featured.products.length && (
            <p className="border-y border-dashed py-6 text-sm text-muted-foreground">No products to show yet.</p>
          )}
          <div className="grid grid-cols-2 gap-3 sm:gap-5 lg:grid-cols-4" aria-busy={featured.status === 'loading'}>
            {featured.status === 'loading' && Array.from({ length: 4 }, (_, index) => (
              <div key={index} className="space-y-3" aria-hidden="true">
                <Skeleton className="aspect-square w-full rounded-sm" />
                <Skeleton className="h-4 w-2/3" />
                <Skeleton className="h-5 w-1/3" />
              </div>
            ))}
            {featured.status === 'ready' && featured.products.map((product) => (
              <ProductCard key={product.id} product={product} />
            ))}
          </div>
        </div>
      </section>
    </>
  )
}

function CatalogUnavailable({ message, onRetry }: { message: string; onRetry: () => void }) {
  return (
    <div className="flex flex-wrap items-center justify-between gap-4 border-y border-dashed py-6">
      <p className="text-sm text-muted-foreground" role="status">{message}</p>
      <button
        type="button"
        onClick={onRetry}
        className="inline-flex min-h-10 items-center gap-2 rounded-sm border bg-background px-3 text-sm font-semibold hover:bg-secondary focus-visible:outline-2 focus-visible:outline-primary"
      >
        <RotateCw className="size-4" aria-hidden="true" /> Try again
      </button>
    </div>
  )
}

function SectionHeading({
  id,
  title,
  linkLabel,
}: {
  id: string
  title: string
  linkLabel: string
}) {
  return (
    <div className="mb-4 flex flex-wrap items-end justify-between gap-x-4 gap-y-2 sm:mb-6">
      <h2 id={id} className="min-w-0 text-xl font-extrabold sm:text-3xl lg:text-4xl">{title}</h2>
      <Link
        to="/shop"
        className="inline-flex items-center gap-1.5 border-b border-primary pb-0.5 text-sm font-bold text-primary transition-colors hover:border-current hover:text-foreground focus-visible:outline-2 focus-visible:outline-offset-4"
      >
        {linkLabel} <ArrowRight className="size-4" aria-hidden="true" />
      </Link>
    </div>
  )
}
