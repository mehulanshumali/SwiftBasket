// Reset password — set new password with the emailed token (PRD 1.4)
// The reset email links here: /reset-password?token=…

import { useState } from 'react'
import { Link, useSearch } from '@tanstack/react-router'

import { Loader2 } from 'lucide-react'
import { toast } from 'sonner'

import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { authApi } from '@/api/auth'

export function ResetPasswordPage() {
  const search = useSearch({ from: '/reset-password' }) as { token?: string }
  const [newPassword, setNewPassword] = useState('')
  const [confirm, setConfirm] = useState('')
  const [submitting, setSubmitting] = useState(false)
  const [done, setDone] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const token = search.token?.trim() ?? ''

  function validate(): boolean {
    if (newPassword.length < 8) {
      setError('Password must be at least 8 characters')
      return false
    }
    if (newPassword !== confirm) {
      setError('Passwords do not match')
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
      await authApi.resetPassword(token, newPassword)
      setDone(true)
      toast.success('Password updated — log in with your new password')
    } catch (err) {
      setError(
        err instanceof Error && err.message
          ? err.message
          : 'Reset failed — the link may be invalid or expired',
      )
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <div className="mx-auto flex max-w-md flex-col gap-6 px-4 py-16">
      <div className="text-center">
        <h1 className="text-2xl font-bold tracking-tight">Choose a new password</h1>
        <p className="mt-2 text-sm text-muted-foreground">
          {token
            ? 'Your new password must be at least 8 characters.'
            : 'This link is missing its token — please request a new reset email.'}
        </p>
      </div>

      {done ? (
        <div className="flex flex-col items-center gap-4 rounded-md border bg-card p-8 text-center">
          <p className="font-medium">Password reset successfully</p>
          <Button asChild className="mt-2">
            <Link to="/login">Go to login</Link>
          </Button>
        </div>
      ) : (
        <form onSubmit={onSubmit} className="flex flex-col gap-4 rounded-md border bg-card p-6">
          <div className="space-y-2">
            <Label htmlFor="new-password">New password</Label>
            <Input
              id="new-password"
              type="password"
              value={newPassword}
              onChange={(e) => setNewPassword(e.target.value)}
              autoComplete="new-password"
            />
          </div>
          <div className="space-y-2">
            <Label htmlFor="confirm-password">Confirm password</Label>
            <Input
              id="confirm-password"
              type="password"
              value={confirm}
              onChange={(e) => setConfirm(e.target.value)}
              autoComplete="new-password"
            />
          </div>

          {error && <p className="text-sm text-destructive">{error}</p>}

          <Button type="submit" disabled={submitting || !token}>
            {submitting && <Loader2 className="h-4 w-4 animate-spin" />}
            Reset password
          </Button>

          <p className="text-center text-sm text-muted-foreground">
            Didn't request this?{' '}
            <Link to="/forgot-password" className="font-medium text-primary hover:underline">
              Request a new link
            </Link>
          </p>
        </form>
      )}
    </div>
  )
}
