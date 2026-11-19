import { useEffect, useSyncExternalStore } from 'react'

import { productApi, type Category } from '@/api/products'

let snapshot = { categories: [] as Category[], loading: true, unavailable: false }
let pending: Promise<void> | null = null
const listeners = new Set<() => void>()

function publish(next: typeof snapshot) {
  snapshot = next
  listeners.forEach((listener) => listener())
}

function subscribe(listener: () => void) {
  listeners.add(listener)
  return () => { listeners.delete(listener) }
}

function getSnapshot() {
  return snapshot
}

function refresh() {
  if (!pending) {
    pending = productApi.categories().then(
      (categories) => publish({ categories, loading: false, unavailable: false }),
      () => publish({ ...snapshot, loading: false, unavailable: true }),
    ).finally(() => { pending = null })
    publish({ ...snapshot, loading: true, unavailable: false })
  }
  return pending
}

export function useCategories() {
  const categories = useSyncExternalStore(subscribe, getSnapshot)
  useEffect(() => {
    if (snapshot.loading) void refresh()
  }, [])

  return { ...categories, refresh }
}
