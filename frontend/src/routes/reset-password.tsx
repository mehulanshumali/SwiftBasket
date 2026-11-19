import { createFileRoute } from '@tanstack/react-router'
import { z } from 'zod'

import { ResetPasswordPage } from '@/pages/ResetPasswordPage'

// token arrives via the emailed link: /reset-password?token=…
const resetSearchSchema = z.object({
  token: z.string().optional(),
})

export const Route = createFileRoute('/reset-password')({
  validateSearch: (search) => resetSearchSchema.parse(search),
  component: ResetPasswordPage,
})
