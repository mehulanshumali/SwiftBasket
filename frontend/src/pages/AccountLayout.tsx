// Account layout — sidebar navigation for the account area (auth required)

import { Link, Outlet, useLocation } from '@tanstack/react-router'

import { Bell, Package, User } from 'lucide-react'

const links = [
  { to: '/account', label: 'Profile', icon: User, end: true },
  { to: '/account/orders', label: 'Orders', icon: Package, end: false },
  { to: '/account/notifications', label: 'Notifications', icon: Bell, end: false },
]

export function AccountLayout() {
  const { pathname } = useLocation()
  return (
    <div className="mx-auto max-w-5xl px-4 py-8">
      <h1 className="mb-6 text-2xl font-bold tracking-tight">My Account</h1>
      <div className="flex flex-col gap-8 md:flex-row">
        <nav className="flex gap-2 md:w-52 md:flex-col">
          {links.map(({ to, label, icon: Icon, end }) => {
            const isActive = end
              ? pathname === to
              : pathname.startsWith(to)
            return (
              <Link
                key={to}
                to={to}
                className={`flex items-center gap-2 rounded-md px-3 py-2 text-sm font-medium transition-colors ${
                  isActive
                    ? 'bg-primary text-primary-foreground'
                    : 'text-muted-foreground hover:bg-secondary hover:text-foreground'
                }`}
              >
                <Icon className="h-4 w-4" />
                {label}
              </Link>
            )
          })}
        </nav>
        <div className="min-w-0 flex-1">
          <Outlet />
        </div>
      </div>
    </div>
  )
}
