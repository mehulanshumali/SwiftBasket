import { useEffect, useState, type FormEvent, type ReactNode } from 'react'
import { Link, useNavigate } from '@tanstack/react-router'
import { Loader2, LogIn, UserPlus } from 'lucide-react'
import { toast } from 'sonner'

import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { useAuth } from '@/context/AuthContext'

const authCopy = {
  login: {
    title: 'Welcome back',
    description: 'Log in to your SwiftBasket account',
    submit: 'Log in',
    pending: 'Logging in...',
    success: 'Welcome back!',
    error: 'Login failed. Check your email and password.',
    prompt: 'New to SwiftBasket?',
    switchTo: '/register',
    switchLabel: 'Create an account',
    icon: LogIn,
  },
  register: {
    title: 'Create account',
    description: 'Join SwiftBasket',
    submit: 'Create account',
    pending: 'Creating account...',
    success: 'Account created. Welcome to SwiftBasket!',
    error: 'Registration failed. Please try again.',
    prompt: 'Already have an account?',
    switchTo: '/login',
    switchLabel: 'Log in',
    icon: UserPlus,
  },
} as const

type FieldName = 'fullName' | 'email' | 'password' | 'confirmPassword'

function AuthField({ name, label, type, autoComplete, error, action }: {
  name: FieldName
  label: string
  type?: 'email' | 'password'
  autoComplete: string
  error?: string
  action?: ReactNode
}) {
  const errorId = `${name}-error`

  return (
    <div className="space-y-2">
      <div className="flex items-center justify-between">
        <Label htmlFor={name}>{label}</Label>
        {action}
      </div>
      <Input
        id={name}
        name={name}
        type={type}
        autoComplete={autoComplete}
        aria-invalid={Boolean(error)}
        aria-describedby={error ? errorId : undefined}
      />
      {error && <p id={errorId} className="text-xs text-destructive">{error}</p>}
    </div>
  )
}

export function AuthPage({ mode, redirect }: { mode: keyof typeof authCopy; redirect?: string }) {
  const navigate = useNavigate()
  const { user, loading, login, register } = useAuth()
  const [fieldErrors, setFieldErrors] = useState<Partial<Record<FieldName, string>>>({})
  const [formError, setFormError] = useState<string | null>(null)
  const [submitting, setSubmitting] = useState(false)
  const registering = mode === 'register'
  const copy = authCopy[mode]
  const SubmitIcon = copy.icon

  useEffect(() => {
    if (!loading && user) void navigate({ to: redirect ?? '/' })
  }, [loading, user, navigate, redirect])

  async function onSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    setFormError(null)

    const form = new FormData(event.currentTarget)
    const email = String(form.get('email') ?? '').trim()
    const password = String(form.get('password') ?? '')
    const fullName = String(form.get('fullName') ?? '').trim()
    const errors: Partial<Record<FieldName, string>> = {}

    if (!email) errors.email = 'Email is required'
    else if (!/^\S+@\S+\.\S+$/.test(email)) errors.email = 'Enter a valid email'
    if (!password) errors.password = 'Password is required'

    if (registering) {
      if (!fullName) errors.fullName = 'Full name is required'
      else if (fullName.length < 2) errors.fullName = 'Name is too short'
      if (password && password.length < 8) errors.password = 'At least 8 characters'
      if (form.get('confirmPassword') !== password) errors.confirmPassword = 'Passwords do not match'
    }

    setFieldErrors(errors)
    if (Object.keys(errors).length) return

    setSubmitting(true)
    try {
      if (registering) await register(email, password, fullName)
      else await login(email, password)
      toast.success(copy.success)
    } catch (error) {
      setFormError(error instanceof Error && error.message ? error.message : copy.error)
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <div className="mx-auto flex w-full max-w-md flex-col gap-6 px-4 py-16">
      <div className="text-center">
        <h1 className="text-2xl font-bold">{copy.title}</h1>
        <p className="mt-1 text-sm text-muted-foreground">{copy.description}</p>
      </div>

      <form onSubmit={onSubmit} className="space-y-4" noValidate>
        {formError && (
          <p role="alert" className="rounded-md border border-destructive/30 bg-destructive/5 p-3 text-sm text-destructive">
            {formError}
          </p>
        )}

        {registering && (
          <AuthField name="fullName" label="Full name" autoComplete="name" error={fieldErrors.fullName} />
        )}
        <AuthField name="email" label="Email" type="email" autoComplete="email" error={fieldErrors.email} />
        <AuthField
          name="password"
          label="Password"
          type="password"
          autoComplete={registering ? 'new-password' : 'current-password'}
          error={fieldErrors.password}
          action={!registering && (
            <Link to="/forgot-password" className="text-xs font-medium text-primary hover:underline">
              Forgot password?
            </Link>
          )}
        />
        {registering && (
          <AuthField
            name="confirmPassword"
            label="Confirm password"
            type="password"
            autoComplete="new-password"
            error={fieldErrors.confirmPassword}
          />
        )}

        <Button type="submit" className="w-full" size="lg" disabled={submitting || loading}>
          {submitting ? <Loader2 className="h-4 w-4 animate-spin" /> : <SubmitIcon className="h-4 w-4" />}
          {submitting ? copy.pending : copy.submit}
        </Button>
      </form>

      <p className="text-center text-sm text-muted-foreground">
        {copy.prompt}{' '}
        <Link to={copy.switchTo} search={{ redirect }} className="font-medium text-primary hover:underline">
          {copy.switchLabel}
        </Link>
      </p>
    </div>
  )
}