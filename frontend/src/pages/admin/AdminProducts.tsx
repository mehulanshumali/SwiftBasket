// Admin products — table + create/edit dialog + delete + ES reindex

import { useState } from 'react'
import { Link } from '@tanstack/react-router'

import { Loader2, Pencil, Plus, RefreshCw, Search, Trash2 } from 'lucide-react'

import { Pagination } from '@/components/Pagination'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import {
  Dialog,
  DialogContent,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from '@/components/ui/dialog'
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '@/components/ui/select'
import { Skeleton } from '@/components/ui/skeleton'
import { Textarea } from '@/components/ui/textarea'
import { adminApi, type ProductInput } from '@/api/admin'
import { productApi, type ProductListItem } from '@/api/products'
import { useCategories } from '@/hooks/useCategories'
import { usePagedList } from '@/hooks/usePagedList'
import { formatMoney } from '@/lib/utils'

const EMPTY_FORM: ProductInput = {
  name: '',
  description: '',
  price: 0,
  stockQuantity: 0,
  sku: '',
  categoryId: 0,
}

const loadProducts = (page: number, size: number) => productApi.list({ page, size })

export function AdminProducts() {
  const { categories, loading: catLoading } = useCategories()
  const { page, pageIndex, error: loadError, loading, goToPage, reload } = usePagedList(loadProducts, 50)
  const [actionError, setActionError] = useState<string | null>(null)
  const [notice, setNotice] = useState<string | null>(null)
  const [query, setQuery] = useState('')

  const [dialogOpen, setDialogOpen] = useState(false)
  const [editingId, setEditingId] = useState<number | null>(null)
  const [form, setForm] = useState<ProductInput>(EMPTY_FORM)
  const [formError, setFormError] = useState<string | null>(null)
  const [saving, setSaving] = useState(false)
  const [deleting, setDeleting] = useState<number | null>(null)
  const [reindexing, setReindexing] = useState(false)

  function openCreate() {
    setEditingId(null)
    setForm({ ...EMPTY_FORM, categoryId: categories[0]?.id ?? 0 })
    setFormError(null)
    setDialogOpen(true)
  }

  function openEdit(p: ProductListItem) {
    setEditingId(p.id)
    setForm({
      name: p.name,
      description: p.description ?? '',
      price: p.price,
      stockQuantity: p.stockQuantity,
      imageUrl: p.imageUrl ?? undefined,
      sku: p.sku,
      categoryId: p.categoryId,
    })
    setFormError(null)
    setDialogOpen(true)
  }

  function validate(): boolean {
    if (form.name.trim().length < 3) {
      setFormError('Name must be at least 3 characters')
      return false
    }
    if (!form.description.trim()) {
      setFormError('Description is required')
      return false
    }
    if (form.price <= 0) {
      setFormError('Price must be greater than 0')
      return false
    }
    if (form.stockQuantity < 0) {
      setFormError('Stock cannot be negative')
      return false
    }
    if (!form.sku.trim()) {
      setFormError('SKU is required')
      return false
    }
    if (!form.categoryId) {
      setFormError('Pick a category')
      return false
    }
    setFormError(null)
    return true
  }

  async function onSave() {
    if (!validate()) return
    setSaving(true)
    setActionError(null)
    try {
      if (editingId) {
        await adminApi.updateProduct(editingId, form)
        setNotice(`Product "${form.name}" updated`)
      } else {
        await adminApi.createProduct(form)
        setNotice(`Product "${form.name}" created`)
      }
      setDialogOpen(false)
      reload()
    } catch (e) {
      setFormError(e instanceof Error ? e.message : 'Could not save product')
    } finally {
      setSaving(false)
    }
  }

  async function onDelete(p: ProductListItem) {
    setDeleting(p.id)
    setActionError(null)
    try {
      await adminApi.deleteProduct(p.id)
      setNotice(`Product "${p.name}" removed`)
      reload()
    } catch (e) {
      setActionError(e instanceof Error ? e.message : 'Could not delete product')
    } finally {
      setDeleting(null)
    }
  }

  async function onReindex() {
    setReindexing(true)
    setActionError(null)
    try {
      const n = await adminApi.reindex()
      setNotice(`Search index rebuilt — ${n} products indexed`)
    } catch (e) {
      setActionError(e instanceof Error ? e.message : 'Reindex failed')
    } finally {
      setReindexing(false)
    }
  }

  const error = actionError ?? loadError
  const visible = page?.content.filter(
    (p) =>
      !query ||
      p.name.toLowerCase().includes(query.toLowerCase()) ||
      p.sku.toLowerCase().includes(query.toLowerCase()),
  )

  return (
    <div className="space-y-4">
      <div className="flex flex-wrap items-center gap-3">
        <div className="relative">
          <Search className="absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-muted-foreground" />
          <Input
            value={query}
            onChange={(e) => setQuery(e.target.value)}
            aria-label="Filter this page by name or SKU"
            placeholder="Filter this page by name or SKU"
            className="w-64 max-w-full pl-9"
          />
        </div>
        <div className="ml-auto flex gap-2">
          <Button variant="outline" onClick={() => void onReindex()} disabled={reindexing}>
            {reindexing ? (
              <Loader2 className="h-4 w-4 animate-spin" />
            ) : (
              <RefreshCw className="h-4 w-4" />
            )}
            Reindex search
          </Button>
          <Button onClick={openCreate} disabled={catLoading || categories.length === 0}>
            <Plus className="h-4 w-4" />
            New product
          </Button>
        </div>
      </div>

      {notice && (
        <p className="rounded-md border border-green-500/30 bg-green-500/5 p-3 text-sm text-green-600">
          {notice}
        </p>
      )}
      {error && (
        <p role="alert" className="rounded-md border border-destructive/30 bg-destructive/5 p-3 text-sm text-destructive">
          {error}
        </p>
      )}
      {loadError && <Button variant="outline" size="sm" onClick={reload}>Try again</Button>}

      {loading ? (
        <div className="space-y-2">
          {Array.from({ length: 5 }).map((_, i) => (
            <Skeleton key={i} className="h-14 w-full rounded-md" />
          ))}
        </div>
      ) : page && (
        <div className="overflow-x-auto rounded-md border">
          <table className="w-full text-sm">
            <thead>
              <tr className="border-b bg-secondary/50 text-left text-xs uppercase tracking-wide text-muted-foreground">
                <th className="p-3">Product</th>
                <th className="p-3">SKU</th>
                <th className="p-3">Category</th>
                <th className="p-3 text-right">Price</th>
                <th className="p-3 text-right">Stock</th>
                <th className="p-3 text-right">Actions</th>
              </tr>
            </thead>
            <tbody>
              {visible?.map((p) => (
                <tr key={p.id} className="border-b last:border-0">
                  <td className="max-w-56 p-3">
                    <Link
                      to="/products/$id"
                      params={{ id: String(p.id) }}
                      className="line-clamp-1 font-medium hover:text-primary"
                    >
                      {p.name}
                    </Link>
                    {!p.active && (
                      <span className="ml-2 text-xs text-destructive">(inactive)</span>
                    )}
                  </td>
                  <td className="p-3 font-mono text-xs">{p.sku}</td>
                  <td className="p-3 text-muted-foreground">{p.categoryName}</td>
                  <td className="p-3 text-right tabular-nums">{formatMoney(p.price, p.currency)}</td>
                  <td className="p-3 text-right tabular-nums">
                    <span className={p.stockQuantity === 0 ? 'text-destructive' : ''}>
                      {p.stockQuantity}
                    </span>
                  </td>
                  <td className="p-3">
                    <div className="flex justify-end gap-1">
                      <Button variant="ghost" size="icon" aria-label={`Edit ${p.name}`} onClick={() => openEdit(p)}>
                        <Pencil className="h-4 w-4" />
                      </Button>
                      <Button
                        variant="ghost"
                        size="icon"
                        aria-label={`Delete ${p.name}`}
                        disabled={deleting === p.id}
                        onClick={() => void onDelete(p)}
                      >
                        {deleting === p.id ? (
                          <Loader2 className="h-4 w-4 animate-spin" />
                        ) : (
                          <Trash2 className="h-4 w-4 text-destructive" />
                        )}
                      </Button>
                    </div>
                  </td>
                </tr>
              ))}
              {visible?.length === 0 && (
                <tr>
                  <td colSpan={6} className="p-8 text-center text-muted-foreground">
                    No products match.
                  </td>
                </tr>
              )}
            </tbody>
          </table>
        </div>
      )}
      {page && <Pagination page={pageIndex} totalPages={page.totalPages} onPage={goToPage} />}

      {/* Create / Edit dialog */}
      <Dialog open={dialogOpen} onOpenChange={setDialogOpen}>
        <DialogContent aria-describedby={undefined} className="max-h-[calc(100dvh-2rem)] overflow-y-auto sm:max-w-lg">
          <DialogHeader>
            <DialogTitle>{editingId ? `Edit product #${editingId}` : 'New product'}</DialogTitle>
          </DialogHeader>
          <div className="space-y-4">
            {formError && (
              <p className="rounded-md border border-destructive/30 bg-destructive/5 p-3 text-sm text-destructive">
                {formError}
              </p>
            )}
            <div className="space-y-2">
              <Label htmlFor="p-name">Name</Label>
              <Input
                id="p-name"
                value={form.name}
                onChange={(e) => setForm({ ...form, name: e.target.value })}
              />
            </div>
            <div className="space-y-2">
              <Label htmlFor="p-desc">Description</Label>
              <Textarea
                id="p-desc"
                rows={3}
                value={form.description}
                onChange={(e) => setForm({ ...form, description: e.target.value })}
              />
            </div>
            <div className="grid grid-cols-3 gap-3">
              <div className="space-y-2">
                <Label htmlFor="p-price">Price</Label>
                <Input
                  id="p-price"
                  type="number"
                  min={0}
                  step="0.01"
                  value={form.price}
                  onChange={(e) => setForm({ ...form, price: Number(e.target.value) })}
                />
              </div>
              <div className="space-y-2">
                <Label htmlFor="p-stock">Stock</Label>
                <Input
                  id="p-stock"
                  type="number"
                  min={0}
                  value={form.stockQuantity}
                  onChange={(e) => setForm({ ...form, stockQuantity: Number(e.target.value) })}
                />
              </div>
              <div className="space-y-2">
                <Label htmlFor="p-sku">SKU</Label>
                <Input
                  id="p-sku"
                  value={form.sku}
                  onChange={(e) => setForm({ ...form, sku: e.target.value })}
                />
              </div>
            </div>
            <div className="space-y-2">
              <Label htmlFor="p-image">Image URL</Label>
              <Input
                id="p-image"
                inputMode="url"
                maxLength={500}
                value={form.imageUrl ?? ''}
                onChange={(event) => setForm({ ...form, imageUrl: event.target.value })}
              />
            </div>
            <div className="space-y-2">
              <Label>Category</Label>
              <Select
                value={String(form.categoryId)}
                onValueChange={(v) => setForm({ ...form, categoryId: Number(v) })}
              >
                <SelectTrigger>
                  <SelectValue placeholder="Pick a category" />
                </SelectTrigger>
                <SelectContent>
                  {categories.map((c) => (
                    <SelectItem key={c.id} value={String(c.id)}>
                      {c.name}
                    </SelectItem>
                  ))}
                </SelectContent>
              </Select>
            </div>
          </div>
          <DialogFooter>
            <Button variant="ghost" onClick={() => setDialogOpen(false)} disabled={saving}>
              Cancel
            </Button>
            <Button onClick={() => void onSave()} disabled={saving}>
              {saving && <Loader2 className="h-4 w-4 animate-spin" />}
              {editingId ? 'Save changes' : 'Create product'}
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>
    </div>
  )
}
