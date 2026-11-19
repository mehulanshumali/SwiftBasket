import { useEffect, useState } from 'react'

import type { Page } from '@/api/client'

interface PageRequest {
  index: number
}

interface PageResult<T> {
  request: PageRequest
  page: Page<T> | null
  error: string | null
}

export function usePagedList<T>(loadPage: (page: number, size: number) => Promise<Page<T>>, size: number) {
  const [request, setRequest] = useState<PageRequest>({ index: 0 })
  const [result, setResult] = useState<PageResult<T> | null>(null)

  useEffect(() => {
    let active = true
    void loadPage(request.index, size).then(
      (page) => {
        if (!active) return
        if (request.index > 0 && request.index >= page.totalPages) {
          setRequest({ index: Math.max(0, page.totalPages - 1) })
          return
        }
        setResult({ request, page, error: null })
      },
      (error: unknown) => {
        if (active) {
          setResult({ request, page: null, error: error instanceof Error ? error.message : 'Could not load results' })
        }
      },
    )
    return () => { active = false }
  }, [loadPage, request, size])

  const current = result?.request === request ? result : null

  return {
    page: current?.page ?? null,
    error: current?.error ?? null,
    loading: current === null,
    pageIndex: request.index,
    goToPage: (index: number) => setRequest({ index }),
    reload: () => setRequest((previous) => ({ index: previous.index })),
  }
}