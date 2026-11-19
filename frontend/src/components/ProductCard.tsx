import { useState } from 'react'
import { Link } from '@tanstack/react-router'

import { Loader2, Package, ShoppingCart } from 'lucide-react'
import { toast } from 'sonner'

import { cartApi } from '@/api/cart'
import type { ProductListItem, ProductSearchItem } from '@/api/products'
import { notifyCartChanged } from '@/context/CartContext'
import { formatMoney } from '@/lib/utils'

export function ProductCard({ product, showAddToCart = true }: {
  product: ProductListItem | ProductSearchItem
  showAddToCart?: boolean
}) {
  const [adding, setAdding] = useState(false)
  const [failedImage, setFailedImage] = useState<string | null>(null)
  const image = product.imageUrl !== failedImage ? product.imageUrl : null
  const catalogProduct = 'stockQuantity' in product ? product : null
  const stockQuantity = catalogProduct?.stockQuantity
  const inStock = typeof stockQuantity === 'number' && stockQuantity > 0

  async function addToCart() {
    if (adding || !catalogProduct || !inStock) return
    setAdding(true)
    try {
      await cartApi.addItem({ productId: catalogProduct.id, quantity: 1 })
      notifyCartChanged()
      toast.success(`${product.name} added to cart`)
    } catch (error) {
      toast.error(error instanceof Error ? error.message : 'Could not add to cart')
    } finally {
      setAdding(false)
    }
  }

  return (
    <article className="flex min-w-0 flex-col overflow-hidden rounded-sm border bg-card transition-colors hover:border-foreground/30">
      <Link
        to="/products/$id"
        params={{ id: String(product.id) }}
        className="group flex grow flex-col focus-visible:outline-2 focus-visible:-outline-offset-2 focus-visible:outline-primary"
      >
        <div className="relative flex aspect-square items-center justify-center overflow-hidden bg-secondary">
          {image ? (
            <img
              src={image}
              alt=""
              loading="lazy"
              onError={() => setFailedImage(image)}
              className="size-full object-contain transition-transform duration-300 motion-safe:group-hover:scale-105"
            />
          ) : (
            <Package className="size-11 text-muted-foreground/45" aria-hidden="true" />
          )}
          {stockQuantity === 0 && (
            <span className="absolute top-2 left-2 max-w-[calc(100%-1rem)] rounded-sm bg-foreground px-2 py-0.5 text-xs font-semibold text-background">
              Out of stock
            </span>
          )}
        </div>
        <div className="space-y-1 px-3 pt-3 sm:px-4">
          <p className="truncate text-xs font-medium text-muted-foreground" title={product.categoryName}>{product.categoryName}</p>
          <h3 className="line-clamp-2 min-h-10 text-sm font-bold leading-5 wrap-anywhere group-hover:text-primary sm:text-base">{product.name}</h3>
        </div>
      </Link>

      <div className="flex min-h-12 items-center justify-between gap-1 px-3 pb-2 sm:gap-2 sm:px-4 sm:pb-3">
        <p className="min-w-0 text-sm font-extrabold wrap-anywhere tabular-nums sm:text-lg">{formatMoney(product.price, product.currency)}</p>
        {showAddToCart && inStock && (
          <button
            type="button"
            onClick={addToCart}
            disabled={adding}
            aria-busy={adding}
            aria-label={`Add ${product.name} to cart`}
            title="Add to cart"
            className="grid size-9 shrink-0 place-items-center rounded-sm border text-foreground transition-colors hover:border-primary hover:bg-primary hover:text-primary-foreground focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-primary disabled:opacity-60 sm:size-10"
          >
            {adding ? <Loader2 className="size-4 animate-spin" aria-hidden="true" /> : <ShoppingCart className="size-4" aria-hidden="true" />}
          </button>
        )}
      </div>
    </article>
  )
}
