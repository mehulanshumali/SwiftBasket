// Shared helpers

import { clsx, type ClassValue } from 'clsx'
import { twMerge } from 'tailwind-merge'

// shadcn class helper
export function cn(...inputs: ClassValue[]) {
  return twMerge(clsx(inputs))
}

export function formatMoney(amount: number, currency: string | null | undefined): string {
  if (!currency || !/^[a-z]{3}$/i.test(currency)) {
    return new Intl.NumberFormat().format(amount)
  }
  return new Intl.NumberFormat(undefined, { style: 'currency', currency }).format(amount)
}

export function formatDate(iso: string): string {
  return new Intl.DateTimeFormat('en-IN', {
    dateStyle: 'medium',
    timeStyle: 'short',
  }).format(new Date(iso))
}
