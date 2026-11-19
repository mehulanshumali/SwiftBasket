// Product detail — Roiser shop-details: gallery, stock/SKU info, qty stepper, add to cart, related

import { useEffect, useState } from 'react'
import { Link, useParams } from '@tanstack/react-router'

import { Loader2, ShoppingCart } from 'lucide-react'
import { toast } from 'sonner'

import { ProductCard } from '@/components/ProductCard'
import { QuantityStepper } from '@/components/QuantityStepper'
import { Badge } from '@/components/ui/badge'
import { Button } from '@/components/ui/button'
import { Skeleton } from '@/components/ui/skeleton'
import { cartApi } from '@/api/cart'
import { type ProductDetail, type ProductListItem, productApi } from '@/api/products'
import { notifyCartChanged } from '@/context/CartContext'
import { formatMoney } from '@/lib/utils'

export function ProductDetailPage() {
  const { id } = useParams({ from: '/products/$id' })
  const productId = Number(id)

  const [product, setProduct] = useState<ProductDetail | null>(null)
  const [related, setRelated] = useState<ProductListItem[]>([])
  const [error, setError] = useState<string | null>(null)
  const [qty, setQty] = useState(1)
  const [cartState, setCartState] = useState<'idle' | 'adding' | 'added'>('idle')

  useEffect(() => {
    setProduct(null)
    setError(null)
    setQty(1)
    setCartState('idle')
    productApi
      .get(productId)
      .then((p) => {
        setProduct(p)
        return productApi.list({ categoryId: p.categoryId, size: 8 })
      })
      .then((page) => setRelated(page.content.filter((p) => p.id !== productId).slice(0, 4)))
      .catch((e: Error) => setError(e.message))
  }, [productId])

  async function addToCart() {
    if (!product || cartState !== 'idle') return
    setCartState('adding')
    try {
      await cartApi.addItem({ productId: product.id, quantity: qty })
      notifyCartChanged()
      toast.success(`${product.name} added to cart`)
      setCartState('added')
      setTimeout(() => setCartState('idle'), 2000)
    } catch (e) {
      toast.error(e instanceof Error ? e.message : 'Could not add to cart')
      setCartState('idle')
    }
  }

  if (error) {
    return (
      <div className="mx-auto max-w-3xl px-4 py-20 text-center">
        <h1 className="text-2xl font-bold">Product not found</h1>
        <p className="mt-2 text-sm text-muted-foreground">{error}</p>
        <Link to="/shop" className="mt-4 inline-block text-sm font-semibold text-primary hover:underline">
          ← Back to shop
        </Link>
      </div>
    )
  }

  return (
    <div className="mx-auto max-w-7xl px-4 py-8">
      {/* Breadcrumb */}
      <nav className="mb-6 text-sm text-muted-foreground">
        <Link to="/" className="hover:text-primary">Home</Link>
        <span className="mx-2">/</span>
        <Link to="/shop" className="hover:text-primary">Shop</Link>
        {product && (
          <>
            <span className="mx-2">/</span>
            <Link to="/shop" search={{ categoryId: product.categoryId }} className="hover:text-primary">
              {product.categoryName}
            </Link>
          </>
        )}
      </nav>

      {!product ? (
        <div className="grid gap-8 md:grid-cols-2">
          <Skeleton className="aspect-square w-full rounded-md" />
          <div className="space-y-4">
            <Skeleton className="h-4 w-24" />
            <Skeleton className="h-8 w-3/4" />
            <Skeleton className="h-7 w-28" />
            <Skeleton className="h-24 w-full" />
          </div>
        </div>
      ) : (
        <div className="grid gap-10 md:grid-cols-2">
          {/* Image */}
          <div className="aspect-square overflow-hidden rounded-md border bg-secondary">
            {product.imageUrl ? (
              <img src={product.imageUrl} alt={product.name} className="h-full w-full object-cover" />
            ) : (
              <div className="flex h-full w-full items-center justify-center text-8xl font-bold text-muted-foreground/20">
                {product.name.charAt(0)}
              </div>
            )}
          </div>

          {/* Info */}
          <div className="flex flex-col">
            <Link
              to="/shop"
              search={{ categoryId: product.categoryId }}
              className="text-sm font-medium text-primary hover:underline"
            >
              {product.categoryName}
            </Link>
            <h1 className="mt-1 text-3xl font-bold tracking-tight">{product.name}</h1>

            <div className="mt-4 flex items-baseline gap-3">
              <span className="text-3xl font-bold text-primary">{formatMoney(product.price, product.currency)}</span>
              {product.active ? (
                <Badge variant="outline" className="text-xs">
                  SKU: {product.sku}
                </Badge>
              ) : (
                <Badge variant="destructive">Inactive</Badge>
              )}
            </div>

            <div className="mt-3">
              {product.stockQuantity > 5 && (
                <Badge className="bg-emerald-100 text-emerald-800 hover:bg-emerald-100">
                  In stock ({product.stockQuantity} available)
                </Badge>
              )}
              {product.stockQuantity === 0 && (
                <Badge variant="destructive">Out of stock</Badge>
              )}
            </div>

            <p className="mt-5 leading-relaxed text-muted-foreground">{product.description}</p>

            {/* Add to cart */}
            {product.active && product.stockQuantity > 0 && (
              <div className="mt-8 flex flex-wrap items-center gap-3">
                <QuantityStepper
                  value={qty}
                  onChange={setQty}
                  max={Math.min(product.stockQuantity, 99)}
                />
                <Button
                  size="lg"
                  onClick={addToCart}
                  disabled={cartState !== 'idle'}
                  className="flex-1 min-w-48"
                >
                  {cartState === 'adding' ? (
                    <Loader2 className="h-4 w-4 animate-spin" />
                  ) : cartState === 'added' ? (
                    'Added to cart!'
                  ) : (
                    <>
                      <ShoppingCart className="h-4 w-4" /> Add to cart
                    </>
                  )}
                </Button>
              </div>
            )}
            {product.active && product.stockQuantity === 0 && (
              <p className="mt-8 rounded-md bg-secondary p-4 text-sm text-muted-foreground">
                This product is out of stock — check back soon.
              </p>
            )}
          </div>
        </div>
      )}

      {/* Related products */}
      {related.length > 0 && product && (
        <section className="mt-16">
          <h2 className="mb-6 text-xl font-bold tracking-tight">
            More from {product.categoryName}
          </h2>
          <div className="grid grid-cols-2 gap-4 sm:grid-cols-4">
            {related.map((item) => (
              <ProductCard key={item.id} product={item} showAddToCart={false} />
            ))}
          </div>
        </section>
      )}
    </div>
  )
}
