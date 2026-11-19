// Admin categories — list + inline create/edit + delete (409 handling for in-use)

import { useCallback, useEffect, useState } from 'react'

import { AlertCircle, Check, Loader2, Plus, Trash2, X } from 'lucide-react'

import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Skeleton } from '@/components/ui/skeleton'
import { adminApi, type Category } from '@/api/admin'
import { useCategories } from '@/hooks/useCategories'

interface Row {
  id: number | null
  name: string
  description: string
}

export function AdminCategories() {
  const { categories, loading, refresh } = useCategories()
  const [rows, setRows] = useState<Row[]>([])
  const [error, setError] = useState<string | null>(null)
  const [notice, setNotice] = useState<string | null>(null)
  const [busy, setBusy] = useState<number | 'new' | null>(null)

  const syncRows = useCallback(
    (cats: Category[]) => {
      setRows(cats.map((c) => ({ id: c.id, name: c.name, description: c.description ?? '' })))
    },
    [],
  )

  useEffect(() => {
    if (!loading) syncRows(categories)
  }, [loading, categories, syncRows])

  function addRow() {
    setRows((r) => [...r, { id: null, name: '', description: '' }])
  }

  function removeRow(i: number) {
    setRows((r) => r.filter((_, idx) => idx !== i))
  }

  function setRow(i: number, patch: Partial<Row>) {
    setRows((r) => r.map((row, idx) => (idx === i ? { ...row, ...patch } : row)))
  }

  async function saveRow(i: number) {
    const row = rows[i]
    if (row.name.trim().length < 3) {
      setError('Category name must be at least 3 characters')
      return
    }
    setError(null)
    setBusy(row.id ?? 'new')
    try {
      if (row.id) {
        await adminApi.updateCategory(row.id, {
          name: row.name.trim(),
          description: row.description.trim() || undefined,
        })
        setNotice(`Category "${row.name}" updated`)
      } else {
        await adminApi.createCategory({
          name: row.name.trim(),
          description: row.description.trim() || undefined,
        })
        setNotice(`Category "${row.name}" created`)
      }
      await refresh()
    } catch (e) {
      setError(e instanceof Error ? e.message : 'Could not save category')
    } finally {
      setBusy(null)
    }
  }

  async function deleteRow(i: number) {
    const row = rows[i]
    if (!row.id) {
      removeRow(i)
      return
    }
    setError(null)
    setBusy(row.id)
    try {
      await adminApi.deleteCategory(row.id)
      setNotice(`Category "${row.name}" deleted`)
      await refresh()
    } catch (e) {
      setError(e instanceof Error ? e.message : 'Could not delete category')
    } finally {
      setBusy(null)
    }
  }

  if (loading) {
    return (
      <div className="space-y-2">
        {Array.from({ length: 4 }).map((_, i) => (
          <Skeleton key={i} className="h-14 w-full rounded-md" />
        ))}
      </div>
    )
  }

  return (
    <div className="space-y-4">
      <div className="flex items-center justify-between">
        <p className="text-sm text-muted-foreground">
          Deleting a category fails with 409 while products reference it.
        </p>
        <Button size="sm" onClick={addRow}>
          <Plus className="h-4 w-4" />
          New category
        </Button>
      </div>

      {notice && (
        <p className="rounded-md border border-green-500/30 bg-green-500/5 p-3 text-sm text-green-600">
          {notice}
        </p>
      )}
      {error && (
        <p className="flex items-start gap-2 rounded-md border border-destructive/30 bg-destructive/5 p-3 text-sm text-destructive">
          <AlertCircle className="mt-0.5 h-4 w-4 shrink-0" />
          {error}
        </p>
      )}

      <div className="space-y-2">
        {rows.map((row, i) => {
          const isBusy = (row.id !== null && busy === row.id) || (row.id === null && busy === 'new')
          return (
            <div
              key={row.id ?? `new-${i}`}
              className="flex flex-col gap-2 rounded-md border bg-card p-3 sm:flex-row sm:items-center"
            >
              <Input
                value={row.name}
                onChange={(e) => setRow(i, { name: e.target.value })}
                placeholder="Category name"
                className="sm:w-56"
                aria-label="Category name"
              />
              <Input
                value={row.description}
                onChange={(e) => setRow(i, { description: e.target.value })}
                placeholder="Description (optional)"
                className="flex-1"
                aria-label="Category description"
              />
              <div className="flex gap-1 sm:ml-auto">
                <Button
                  size="sm"
                  variant="ghost"
                  aria-label="Save category"
                  disabled={isBusy}
                  onClick={() => void saveRow(i)}
                >
                  {isBusy ? <Loader2 className="h-4 w-4 animate-spin" /> : <Check className="h-4 w-4 text-green-600" />}
                </Button>
                <Button
                  size="sm"
                  variant="ghost"
                  aria-label="Delete category"
                  disabled={isBusy}
                  onClick={() => void deleteRow(i)}
                >
                  {row.id === null ? (
                    <X className="h-4 w-4" />
                  ) : (
                    <Trash2 className="h-4 w-4 text-destructive" />
                  )}
                </Button>
              </div>
            </div>
          )
        })}
        {rows.length === 0 && (
          <p className="rounded-md border p-8 text-center text-sm text-muted-foreground">
            No categories.
          </p>
        )}
      </div>
    </div>
  )
}
