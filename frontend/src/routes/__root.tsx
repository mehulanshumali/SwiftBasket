import {
  createRootRouteWithContext,
  Link,
  Outlet,
} from '@tanstack/react-router'
import { TanStackRouterDevtools } from '@tanstack/react-router-devtools'

import { Footer } from '@/components/layout/Footer'
import { Header } from '@/components/layout/Header'
import { NavBar } from '@/components/layout/NavBar'
import { useCategories } from '@/hooks/useCategories'

export const Route = createRootRouteWithContext()({
  component: RootLayout,

  notFoundComponent: () => (
    <div className="mx-auto flex max-w-xl flex-col items-center gap-6 px-4 py-24 text-center">
      <p className="text-7xl font-extrabold tracking-tight text-primary">404</p>
      <div>
        <h1 className="text-2xl font-bold">This page could not be found</h1>
        <p className="mt-2 text-sm text-muted-foreground">
          The link may be broken or the page may have moved.
        </p>
      </div>
      <div className="flex gap-3">
        <Link
          to="/"
          className="rounded-md bg-primary px-6 py-2.5 text-sm font-semibold text-primary-foreground transition-colors hover:bg-primary/90"
        >
          Back to home
        </Link>
        <Link
          to="/shop"
          className="rounded-md border px-6 py-2.5 text-sm font-semibold transition-colors hover:bg-secondary"
        >
          Browse the shop
        </Link>
      </div>
    </div>
  ),
})

function RootLayout() {
  const { categories } = useCategories()

  return (
    <div className="flex min-h-screen flex-col">
      <Header />
      <NavBar categories={categories} />
      <main className="flex-1">
        <Outlet />
      </main>
      <Footer categories={categories} />
      {import.meta.env.DEV && import.meta.env.VITE_ROUTER_DEVTOOLS === 'true' && (
        <TanStackRouterDevtools position="bottom-right" />
      )}
    </div>
  )
}
