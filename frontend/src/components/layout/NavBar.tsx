import { useState } from 'react'
import { Link, useNavigate } from '@tanstack/react-router'

import { Bell, ChevronDown, LayoutGrid, LogOut, Menu, Package, Shield, UserRound } from 'lucide-react'
import { DropdownMenu } from 'radix-ui'

import {
  Sheet,
  SheetContent,
  SheetHeader,
  SheetTitle,
  SheetTrigger,
} from '@/components/ui/sheet'
import { useAuth } from '@/context/AuthContext'

const menuItemClassName = 'flex cursor-pointer items-center gap-2 rounded-sm px-3 py-2 text-sm wrap-anywhere outline-none data-highlighted:bg-secondary data-highlighted:text-primary'
const accountControlClassName = 'flex min-h-11 items-center gap-1.5 rounded-sm px-2 text-sm font-semibold text-muted-foreground transition-colors hover:text-primary focus-visible:outline-2 focus-visible:outline-primary sm:gap-2'
const links = [
  { to: '/', label: 'Home' },
  { to: '/shop', label: 'Shop' },
] as const

export function NavBar({
  categories,
}: {
  categories: { id: number; name: string; description?: string | null }[]
}) {
  const { user, logout } = useAuth()
  const navigate = useNavigate()
  const [open, setOpen] = useState(false)

  const accountLinks = [
    { to: '/account', label: 'My profile', icon: UserRound },
    { to: '/account/orders', label: 'Order history', icon: Package },
    { to: '/account/notifications', label: 'Notifications', icon: Bell },
    ...(user?.role === 'ADMIN' ? [{ to: '/admin', label: 'Admin', icon: Shield }] as const : []),
  ] as const

  function signOut() {
    logout()
    void navigate({ to: '/', replace: true })
  }

  return (
    <nav aria-label="Main navigation" className="border-y bg-background">
      <div className="storefront-container flex min-h-11 items-center gap-3 sm:gap-5 md:gap-7">
        <Sheet open={open} onOpenChange={setOpen}>
          <SheetTrigger asChild>
            <button
              type="button"
              aria-label="Open shopping menu"
              className="flex min-h-11 items-center gap-2 bg-primary px-3 text-sm font-bold text-primary-foreground sm:hidden"
            >
              <Menu className="size-4" aria-hidden="true" /> Menu
            </button>
          </SheetTrigger>
          <SheetContent side="left" aria-describedby={undefined} className="w-72 overflow-y-auto">
            <SheetHeader>
              <SheetTitle>Shop SwiftBasket</SheetTitle>
            </SheetHeader>
            <div className="grid gap-1 px-4 pb-4">
              {links.map(({ to, label }) => (
                <Link
                  key={to}
                  to={to}
                  onClick={() => setOpen(false)}
                  className="rounded-sm px-2 py-2 text-sm font-semibold hover:bg-secondary"
                >
                  {label}
                </Link>
              ))}
              {categories.length > 0 && (
                <p className="mt-3 border-t px-2 pt-3 text-xs font-bold uppercase text-muted-foreground">Categories</p>
              )}
              {categories.map(({ id, name }) => (
                <Link
                  key={id}
                  to="/shop"
                  search={{ categoryId: id }}
                  onClick={() => setOpen(false)}
                  className="rounded-sm px-2 py-2 text-sm wrap-anywhere hover:bg-secondary"
                >
                  {name}
                </Link>
              ))}
            </div>
          </SheetContent>
        </Sheet>

        <DropdownMenu.Root>
          <DropdownMenu.Trigger asChild>
            <button type="button" className="hidden min-h-11 items-center gap-2 bg-primary px-4 text-sm font-bold text-primary-foreground transition-colors hover:bg-primary/90 sm:flex">
              <LayoutGrid className="size-4" aria-hidden="true" /> All categories
              <ChevronDown className="size-4" aria-hidden="true" />
            </button>
          </DropdownMenu.Trigger>
          <DropdownMenu.Portal>
            <DropdownMenu.Content
              align="start"
              sideOffset={5}
              className="z-50 max-h-72 w-56 overflow-y-auto rounded-sm border bg-popover p-1 text-popover-foreground shadow-md"
            >
              <DropdownMenu.Item asChild>
                <Link to="/shop" className={menuItemClassName}>All products</Link>
              </DropdownMenu.Item>
              {categories.map(({ id, name }) => (
                <DropdownMenu.Item key={id} asChild>
                  <Link to="/shop" search={{ categoryId: id }} className={menuItemClassName}>{name}</Link>
                </DropdownMenu.Item>
              ))}
            </DropdownMenu.Content>
          </DropdownMenu.Portal>
        </DropdownMenu.Root>

        {links.map(({ to, label }) => (
          <Link
            key={to}
            to={to}
            activeOptions={{ exact: true }}
            className="flex min-h-11 items-center border-b-2 border-transparent py-2 text-sm font-semibold text-muted-foreground transition-colors hover:text-primary data-[status=active]:border-primary data-[status=active]:text-primary"
          >
            {label}
          </Link>
        ))}

        <div className="ml-auto shrink-0">
          {user ? (
            <DropdownMenu.Root>
              <DropdownMenu.Trigger asChild>
                <button type="button" aria-label="Account menu" title="Account menu" className={accountControlClassName}>
                  <UserRound className="size-5" aria-hidden="true" />
                  <span className="hidden sm:inline">Account</span>
                  <ChevronDown className="size-4" aria-hidden="true" />
                </button>
              </DropdownMenu.Trigger>
              <DropdownMenu.Portal>
                <DropdownMenu.Content
                  align="end"
                  sideOffset={5}
                  collisionPadding={12}
                  className="z-50 max-h-[var(--radix-dropdown-menu-content-available-height)] w-56 max-w-[calc(100vw-1.5rem)] overflow-y-auto rounded-sm border bg-popover p-1 text-popover-foreground shadow-md"
                >
                  <DropdownMenu.Label className="px-3 py-2 text-xs font-semibold wrap-anywhere text-muted-foreground">
                    {user.fullName || user.email}
                  </DropdownMenu.Label>
                  <DropdownMenu.Separator className="my-1 border-t" />
                  {accountLinks.map(({ to, label, icon: Icon }) => (
                    <DropdownMenu.Item key={to} asChild>
                      <Link to={to} className={menuItemClassName}>
                        <Icon className="size-4 shrink-0" aria-hidden="true" /> {label}
                      </Link>
                    </DropdownMenu.Item>
                  ))}
                  <DropdownMenu.Separator className="my-1 border-t" />
                  <DropdownMenu.Item onSelect={signOut} className={menuItemClassName}>
                    <LogOut className="size-4 shrink-0" aria-hidden="true" /> Sign out
                  </DropdownMenu.Item>
                </DropdownMenu.Content>
              </DropdownMenu.Portal>
            </DropdownMenu.Root>
          ) : (
            <Link to="/login" aria-label="Sign in" title="Sign in" className={accountControlClassName}>
              <UserRound className="size-5" aria-hidden="true" />
              <span className="hidden sm:inline">Sign in</span>
            </Link>
          )}
        </div>
      </div>
    </nav>
  )
}
