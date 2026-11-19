// Forgot password — request reset link (PRD 1.4)
// Backend always answers 200 "If the email exists…" (no user enumeration).

import { useState } from 'react'
import { Link } from '@tanstack/react-router'

import { Loader2, MailCheck } from 'lucide-react'
import { toast } from 'sonner'

import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { authApi } from '@/api/auth'

export function ForgotPasswordPage() {
  const [email, setEmail] = useState('')
  const [submitting, setSubmitting] = useState(false)
  const [sent, setSent] = useState(false)
  const [error, setError] = useState<string | null>(null)

  function validate(): boolean {
    if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email.trim())) {
      setError('Enter a valid email address')
      return false
    }
    setError(null)
    return true
  }

  async function onSubmit(e: React.FormEvent) {
    e.preventDefault()
    if (!validate() || submitting) return
    setSubmitting(true)
    try {
      await authApi.requestPasswordReset(email.trim())
      setSent(true)
      toast.success('Reset link sent — check your inbox')
    } catch (err) {
      setError(err instanceof Error && err.message ? err.message : 'Could not send reset email')
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <div className="mx-auto flex max-w-md flex-col gap-6 px-4 py-16">
      <div className="text-center">
        <h1 className="text-2xl font-bold tracking-tight">Forgot your password?</h1>
        <p className="mt-2 text-sm text-muted-foreground">
          Enter your account email and we'll send you a reset link.
        </p>
      </div>

      {sent ? (
        <div className="flex flex-col items-center gap-4 rounded-md border bg-card p-8 text-center">
          <MailCheck className="h-10 w-10 text-primary" />
          <p className="font-medium">If the email exists, a reset link is on its way</p>
          <p className="text-sm text-muted-foreground">
            The link expires after 30 minutes. Check your spam folder if it doesn't arrive.
          </p>
          <Button asChild variant="outline" className="mt-2">
            <Link to="/login">Back to login</Link>
          </Button>
        </div>
      ) : (
        <form onSubmit={onSubmit} className="flex flex-col gap-4 rounded-md border bg-card p-6">
          <div className="space-y-2">
            <Label htmlFor="email">Email</Label>
            <Input
              id="email"
              type="email"
              value={email}
              onChange={(e) => setEmail(e.target.value)}
              placeholder="you@example.com"
              autoComplete="email"
            />
          </div>

          {error && <p className="text-sm text-destructive">{error}</p>}

          <Button type="submit" disabled={submitting}>
            {submitting && <Loader2 className="h-4 w-4 animate-spin" />}
            Send reset link
          </Button>

          <p className="text-center text-sm text-muted-foreground">
            Remembered it?{' '}
            <Link to="/login" className="font-medium text-primary hover:underline">
              Back to login
            </Link>
          </p>
        </form>
      )}
    </div>
  )
}
