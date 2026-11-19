import { Link } from '@tanstack/react-router'

import { useAuth } from '@/context/AuthContext'

export function Footer({ categories }: { categories: { id: number; name: string }[] }) {
  const { user } = useAuth()

  return (
    <footer className="bg-foreground text-background">
      <div className="mx-auto grid max-w-7xl grid-cols-2 gap-x-6 gap-y-8 px-4 py-10 md:grid-cols-4 md:py-12">
        <Link to="/" className="col-span-2 w-fit self-start text-xl font-bold md:col-span-1">
          SwiftBasket
        </Link>
        <div>
          <p className="mb-3 text-sm font-semibold">Shop</p>
          <ul className="space-y-2 text-sm text-background/70">
            <li>
              <Link to="/shop" className="hover:text-background">
                All products
              </Link>
            </li>
            {categories.slice(0, 5).map((category) => (
              <li key={category.id}>
                <Link to="/shop" search={{ categoryId: category.id }} className="hover:text-background">
                  {category.name}
                </Link>
              </li>
            ))}
          </ul>
        </div>
        <div>
          <p className="mb-3 text-sm font-semibold">Orders</p>
          <ul className="space-y-2 text-sm text-background/70">
            <li>
              <Link to="/cart" className="hover:text-background">
                Your cart
              </Link>
            </li>
            <li>
              <Link to="/checkout" className="hover:text-background">
                Checkout
              </Link>
            </li>
            <li>
              <Link to="/account/orders" className="hover:text-background">
                {user ? 'Order history' : 'Track orders'}
              </Link>
            </li>
          </ul>
        </div>
        <div>
          <p className="mb-3 text-sm font-semibold">Account</p>
          <ul className="space-y-2 text-sm text-background/70">
            {user ? (
              <li>
                <Link to="/account/notifications" className="hover:text-background">
                  Notifications
                </Link>
              </li>
            ) : (
              <>
                <li>
                  <Link to="/login" className="hover:text-background">
                    Sign in
                  </Link>
                </li>
                <li>
                  <Link to="/register" className="hover:text-background">
                    Create account
                  </Link>
                </li>
              </>
            )}
          </ul>
        </div>
      </div>

      <div className="border-t border-background/10">
        <div className="mx-auto flex max-w-7xl flex-wrap items-center justify-between gap-2 px-4 py-4 text-xs text-background/60">
          <span>© 2026 SwiftBasket</span>
        </div>
      </div>
    </footer>
  )
}
