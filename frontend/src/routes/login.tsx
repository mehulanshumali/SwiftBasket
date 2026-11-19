import { createFileRoute } from '@tanstack/react-router'
import { z } from 'zod'

import { AuthPage } from '@/pages/AuthPage'

// redirect param — where to go after successful login (e.g. /login?redirect=/checkout)
const loginSearchSchema = z.object({
  redirect: z.string().optional(),
})

export const Route = createFileRoute('/login')({
  validateSearch: (search) => loginSearchSchema.parse(search),
  component: LoginRoute,
})

function LoginRoute() {
  const { redirect } = Route.useSearch()
  return <AuthPage mode="login" redirect={redirect} />
}
