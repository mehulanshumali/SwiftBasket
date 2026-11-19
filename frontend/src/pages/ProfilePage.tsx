import { useEffect, useState } from 'react'
import { useNavigate } from '@tanstack/react-router'

import { Check, Loader2, Pencil, Trash2 } from 'lucide-react'
import { Dialog as DialogPrimitive } from 'radix-ui'
import { toast } from 'sonner'

import { Button } from '@/components/ui/button'
import { Dialog, DialogContent, DialogFooter, DialogHeader, DialogTitle } from '@/components/ui/dialog'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { Separator } from '@/components/ui/separator'
import { Skeleton } from '@/components/ui/skeleton'
import { Textarea } from '@/components/ui/textarea'
import { authApi } from '@/api/auth'
import { useAuth } from '@/context/AuthContext'
import { formatDate } from '@/lib/utils'

interface ProfileFields {
  fullName: string
  email: string
  phoneNumber: string
  address: string
}

export function ProfilePage() {
  const { user, loading, refreshUser, logout } = useAuth()
  const navigate = useNavigate()
  const [editing, setEditing] = useState(false)
  const [fields, setFields] = useState<ProfileFields>({ fullName: '', email: '', phoneNumber: '', address: '' })
  const [fieldErrors, setFieldErrors] = useState<Partial<ProfileFields>>({})
  const [formError, setFormError] = useState<string | null>(null)
  const [saving, setSaving] = useState(false)
  const [saved, setSaved] = useState(false)
  const [deleteOpen, setDeleteOpen] = useState(false)
  const [deleting, setDeleting] = useState(false)
  const [deleteError, setDeleteError] = useState<string | null>(null)

  useEffect(() => {
    if (user) {
      setFields({
        fullName: user.fullName ?? '',
        email: user.email,
        phoneNumber: user.phoneNumber ?? '',
        address: user.address ?? '',
      })
    }
  }, [user])

  function validate(): boolean {
    const errors: Partial<ProfileFields> = {}
    if (!fields.fullName.trim()) errors.fullName = 'Name is required'
    if (!/^\S+@\S+\.\S+$/.test(fields.email.trim())) errors.email = 'Enter a valid email address'
    if (fields.phoneNumber && !/^\d{10}$/.test(fields.phoneNumber.replace(/\D/g, '').replace(/^91/, '')))
      errors.phoneNumber = 'Enter a valid 10-digit phone number'
    setFieldErrors(errors)
    return Object.keys(errors).length === 0
  }

  async function onSave(e: React.FormEvent) {
    e.preventDefault()
    setFormError(null)
    setSaved(false)
    if (!validate()) return
    setSaving(true)
    try {
      await authApi.updateMe({
        fullName: fields.fullName.trim(),
        email: fields.email.trim(),
        phoneNumber: fields.phoneNumber.trim(),
        address: fields.address.trim(),
      })
      await refreshUser()
      setEditing(false)
      setSaved(true)
    } catch (err) {
      setFormError(err instanceof Error && err.message ? err.message : 'Could not save profile')
    } finally {
      setSaving(false)
    }
  }

  async function deleteAccount() {
    setDeleteError(null)
    setDeleting(true)
    try {
      await authApi.deleteMe()
      setDeleteOpen(false)
      logout()
      toast.success('Account deleted')
      void navigate({ to: '/', replace: true })
    } catch (error) {
      setDeleteError(error instanceof Error ? error.message : 'Could not delete account')
    } finally {
      setDeleting(false)
    }
  }

  if (loading || !user) {
    return (
      <div className="space-y-4">
        <Skeleton className="h-40 w-full rounded-md" />
      </div>
    )
  }

  return (
    <div className="space-y-4">
      {saved && (
        <p className="flex items-center gap-2 rounded-md border border-green-500/30 bg-green-500/5 p-3 text-sm text-green-600">
          <Check className="h-4 w-4" />
          Profile updated
        </p>
      )}
      {formError && (
        <p className="rounded-md border border-destructive/30 bg-destructive/5 p-3 text-sm text-destructive">
          {formError}
        </p>
      )}

      {!editing ? (
        <div className="rounded-md border bg-card p-6">
          <div className="flex items-start justify-between">
            <h2 className="font-bold">Profile</h2>
            <Button variant="outline" size="sm" onClick={() => setEditing(true)}>
              <Pencil className="h-4 w-4" />
              Edit
            </Button>
          </div>
          <dl className="mt-4 space-y-3 text-sm">
            <div className="flex justify-between gap-6">
              <dt className="text-muted-foreground">Full name</dt>
              <dd className="font-medium">{user.fullName || '—'}</dd>
            </div>
            <div className="flex justify-between gap-6">
              <dt className="text-muted-foreground">Phone</dt>
              <dd className="font-medium">{user.phoneNumber || '—'}</dd>
            </div>
            <div className="flex justify-between gap-6">
              <dt className="text-muted-foreground">Address</dt>
              <dd className="max-w-72 text-right font-medium">{user.address || '—'}</dd>
            </div>
          </dl>
          <Separator className="my-5" />
          <dl className="space-y-3 text-sm">
            <div className="flex justify-between gap-6">
              <dt className="text-muted-foreground">Email</dt>
              <dd className="font-medium">{user.email}</dd>
            </div>
            <div className="flex justify-between gap-6">
              <dt className="text-muted-foreground">Role</dt>
              <dd className="font-medium">{user.role}</dd>
            </div>
            <div className="flex justify-between gap-6">
              <dt className="text-muted-foreground">Member since</dt>
              <dd className="font-medium">{formatDate(user.createdAt)}</dd>
            </div>
          </dl>
        </div>
      ) : (
        <form onSubmit={onSave} className="space-y-4 rounded-md border bg-card p-6" noValidate>
          <h2 className="font-bold">Edit profile</h2>
          <div className="space-y-2">
            <Label htmlFor="fullName">Full name</Label>
            <Input
              id="fullName"
              value={fields.fullName}
              onChange={(e) => setFields({ ...fields, fullName: e.target.value })}
              aria-invalid={!!fieldErrors.fullName}
            />
            {fieldErrors.fullName && (
              <p className="text-xs text-destructive">{fieldErrors.fullName}</p>
            )}
          </div>
          <div className="space-y-2">
            <Label htmlFor="profile-email">Email</Label>
            <Input
              id="profile-email"
              type="email"
              autoComplete="email"
              value={fields.email}
              onChange={(event) => setFields({ ...fields, email: event.target.value })}
              aria-invalid={Boolean(fieldErrors.email)}
              aria-describedby={fieldErrors.email ? 'profile-email-error' : undefined}
            />
            {fieldErrors.email && <p id="profile-email-error" className="text-xs text-destructive">{fieldErrors.email}</p>}
          </div>
          <div className="space-y-2">
            <Label htmlFor="phone">Phone number</Label>
            <Input
              id="phone"
              type="tel"
              value={fields.phoneNumber}
              onChange={(e) => setFields({ ...fields, phoneNumber: e.target.value })}
              aria-invalid={!!fieldErrors.phoneNumber}
            />
            {fieldErrors.phoneNumber && (
              <p className="text-xs text-destructive">{fieldErrors.phoneNumber}</p>
            )}
          </div>
          <div className="space-y-2">
            <Label htmlFor="address">Address</Label>
            <Textarea
              id="address"
              rows={3}
              value={fields.address}
              onChange={(e) => setFields({ ...fields, address: e.target.value })}
              aria-invalid={!!fieldErrors.address}
            />
            {fieldErrors.address && (
              <p className="text-xs text-destructive">{fieldErrors.address}</p>
            )}
          </div>
          <div className="flex gap-3">
            <Button type="submit" disabled={saving}>
              {saving ? <Loader2 className="h-4 w-4 animate-spin" /> : <Check className="h-4 w-4" />}
              Save changes
            </Button>
            <Button type="button" variant="ghost" onClick={() => setEditing(false)} disabled={saving}>
              Cancel
            </Button>
          </div>
        </form>
      )}

      {!editing && (
        <Dialog open={deleteOpen} onOpenChange={(open) => {
          if (deleting) return
          setDeleteOpen(open)
          setDeleteError(null)
        }}>
          <div className="flex justify-end border-t pt-4">
            <DialogPrimitive.Trigger asChild>
              <Button variant="outline" className="text-destructive">
                <Trash2 aria-hidden="true" /> Delete account
              </Button>
            </DialogPrimitive.Trigger>
          </div>
          <DialogContent showCloseButton={false}>
            <DialogHeader>
              <DialogTitle>Delete your account?</DialogTitle>
              <DialogPrimitive.Description className="text-muted-foreground">
                You will be signed out and can no longer use this account.
              </DialogPrimitive.Description>
            </DialogHeader>
            {deleteError && <p role="alert" className="text-destructive">{deleteError}</p>}
            <DialogFooter>
              <DialogPrimitive.Close asChild>
                <Button variant="outline" disabled={deleting}>Cancel</Button>
              </DialogPrimitive.Close>
              <Button variant="destructive" disabled={deleting} onClick={() => void deleteAccount()}>
                {deleting && <Loader2 className="animate-spin" aria-hidden="true" />}
                {deleting ? 'Deleting account...' : 'Delete account'}
              </Button>
            </DialogFooter>
          </DialogContent>
        </Dialog>
      )}
    </div>
  )
}
