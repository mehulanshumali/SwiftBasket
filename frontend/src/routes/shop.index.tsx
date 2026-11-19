import { createFileRoute, useNavigate } from '@tanstack/react-router'
import { z } from 'zod'

import { ShopPage } from '@/pages/ShopPage'

// typed search params — zod validated (all optional; defaults handled in page)
const shopSearchSchema = z.object({
  q: z.string().optional(),
  categoryId: z.number().optional(),
  page: z.number().optional(),
})

export const Route = createFileRoute('/shop/')({
  validateSearch: (search) => shopSearchSchema.parse(search),
  component: ShopRoute,
})

function ShopRoute() {
  const navigate = useNavigate({ from: '/shop/' })

  return (
    <ShopPage
      onSearchChange={(s) => {
        void navigate({
          search: { q: s.q, categoryId: s.categoryId, page: s.page ?? 0 },
        })
      }}
    />
  )
}
