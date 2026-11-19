import { createFileRoute } from '@tanstack/react-router'
import { z } from 'zod'

import { AuthPage } from '@/pages/AuthPage'

const registerSearchSchema = z.object({
  redirect: z.string().optional(),
})

export const Route = createFileRoute('/register')({
  validateSearch: (search) => registerSearchSchema.parse(search),
  component: RegisterRoute,
})

function RegisterRoute() {
  const { redirect } = Route.useSearch()
  return <AuthPage mode="register" redirect={redirect} />
}
