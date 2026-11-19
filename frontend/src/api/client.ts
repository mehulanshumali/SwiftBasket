// Single fetch wrapper for all SwiftBasket API calls.
// - Base path /api/v1 (Vite dev proxy → Kong :8000)
// - JWT (Authorization) when logged in, X-Session-ID for guest carts
// - Throws ApiError with status + message from the backend ErrorResponse shape

const TOKEN_KEY = 'sb_token'
const SESSION_KEY = 'sb_session'

// thrown with status + message from the backend ErrorResponse shape
export class ApiError extends Error {
  status: number

  constructor(status: number, message: string) {
    super(message)
    this.status = status
  }
}

export function getToken(): string | null {
  return localStorage.getItem(TOKEN_KEY)
}

export function setToken(token: string) {
  localStorage.setItem(TOKEN_KEY, token)
}

export function clearToken() {
  localStorage.removeItem(TOKEN_KEY)
}

// Guest cart identity — created on first visit, sent on every cart call.
// Dropped after login (cart is merged server-side).
function getSessionId(): string {
  let sid = localStorage.getItem(SESSION_KEY)
  if (!sid) {
    sid = crypto.randomUUID()
    localStorage.setItem(SESSION_KEY, sid)
  }
  return sid
}

export function hasSessionId(): boolean {
  return localStorage.getItem(SESSION_KEY) !== null
}

export function clearSessionId() {
  localStorage.removeItem(SESSION_KEY)
}

type Json = Record<string, unknown> | unknown[] | string | number | boolean | null

async function request<T>(method: string, path: string, body?: Json): Promise<T> {
  const headers: Record<string, string> = {}
  if (body !== undefined) headers['Content-Type'] = 'application/json'

  const token = getToken()
  if (token) headers['Authorization'] = `Bearer ${token}`

  // guest session id for cart paths (getSessionId creates one on demand for
  // new visitors; during login-merge both headers are sent on purpose:
  // Authorization identifies the target user cart, X-Session-ID the guest cart)
  if (path.startsWith('/cart')) {
    headers['X-Session-ID'] = getSessionId()
  }

  let res: Response
  try {
    res = await fetch(`/api/v1${path}`, {
      method,
      headers,
      body: body !== undefined ? JSON.stringify(body) : undefined,
    })
  } catch {
    throw new ApiError(0, 'Network error — is the gateway running?')
  }

  // 204 No Content (e.g. DELETE /cart)
  if (res.status === 204) return undefined as T

  const data = (await res.json().catch(() => null)) as
    | (T & { message?: string })
    | null

  if (!res.ok) {
    throw new ApiError(res.status, data?.message ?? `Request failed (${res.status})`)
  }
  return data as T
}

export const api = {
  get: <T>(path: string) => request<T>('GET', path),
  post: <T>(path: string, body?: Json) => request<T>('POST', path, body),
  put: <T>(path: string, body?: Json) => request<T>('PUT', path, body),
  patch: <T>(path: string, body?: Json) => request<T>('PATCH', path, body),
  delete: <T>(path: string) => request<T>('DELETE', path),
}

// Paged response shape shared by products, orders, notifications
export interface Page<T> {
  content: T[]
  totalElements: number
  totalPages: number
}
