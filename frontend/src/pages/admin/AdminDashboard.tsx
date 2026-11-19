// Admin dashboard — overview cards + tabs: Products / Categories / Orders / Tools

import { useState } from 'react'

import { Database, Package, Tags } from 'lucide-react'

import { AdminCategories } from '@/pages/admin/AdminCategories'
import { AdminOrders } from '@/pages/admin/AdminOrders'
import { AdminProducts } from '@/pages/admin/AdminProducts'

type Tab = 'products' | 'categories' | 'orders'

const TABS: { id: Tab; label: string; icon: typeof Package }[] = [
  { id: 'products', label: 'Products', icon: Package },
  { id: 'categories', label: 'Categories', icon: Tags },
  { id: 'orders', label: 'Orders', icon: Database },
]

export function AdminDashboard() {
  const [tab, setTab] = useState<Tab>('products')

  return (
    <div className="mx-auto max-w-6xl px-4 py-8">
      <h1 className="mb-6 text-2xl font-bold tracking-tight">Admin</h1>

      <div className="mb-6 flex gap-2 border-b pb-px">
        {TABS.map(({ id, label, icon: Icon }) => (
          <button
            key={id}
            type="button"
            onClick={() => setTab(id)}
            className={`flex items-center gap-2 border-b-2 px-4 py-2 text-sm font-medium transition-colors ${
              tab === id
                ? 'border-primary text-primary'
                : 'border-transparent text-muted-foreground hover:text-foreground'
            }`}
          >
            <Icon className="h-4 w-4" />
            {label}
          </button>
        ))}
      </div>

      {tab === 'products' && <AdminProducts />}
      {tab === 'categories' && <AdminCategories />}
      {tab === 'orders' && <AdminOrders />}
    </div>
  )
}
