// Small typed fetch wrapper for the backend. In development Vite proxies /api to Spring Boot.
const BASE: string = import.meta.env.VITE_API_BASE ?? ''

export interface FieldIssue {
  field: string
  message: string
}

/** Same shape as the backend's standard error JSON. */
export class ApiRequestError extends Error {
  readonly status: number
  readonly fieldErrors: FieldIssue[]

  constructor(status: number, message: string, fieldErrors: FieldIssue[] = []) {
    super(message)
    this.status = status
    this.fieldErrors = fieldErrors
  }
}

export interface PageResponse<T> {
  content: T[]
  page: number
  size: number
  totalElements: number
  totalPages: number
}

interface RequestOptions {
  method?: 'GET' | 'POST' | 'PUT' | 'DELETE'
  body?: unknown
}

function readCookie(name: string): string | undefined {
  const hit = document.cookie.split('; ').find((c) => c.startsWith(name + '='))
  return hit ? decodeURIComponent(hit.slice(name.length + 1)) : undefined
}

/** The server hands out a CSRF token in the readable XSRF-TOKEN cookie; changing requests must echo it in a header. */
async function csrfToken(): Promise<string> {
  const existing = readCookie('XSRF-TOKEN')
  if (existing) return existing
  const res = await fetch(BASE + '/api/v1/auth/csrf', { credentials: 'same-origin' })
  const data = (await res.json()) as { token: string }
  return data.token
}

export async function api<T>(path: string, { method = 'GET', body }: RequestOptions = {}): Promise<T> {
  const headers: Record<string, string> = {}
  if (body !== undefined) headers['Content-Type'] = 'application/json'
  if (method !== 'GET') headers['X-XSRF-TOKEN'] = await csrfToken()

  const res = await fetch(BASE + path, {
    method,
    credentials: 'same-origin',
    headers,
    body: body === undefined ? undefined : JSON.stringify(body),
  })

  const data: unknown = await res.json().catch(() => undefined)
  if (!res.ok) {
    const e = (data ?? {}) as { message?: string; fieldErrors?: FieldIssue[] }
    throw new ApiRequestError(res.status, e.message ?? 'Something went wrong. Please try again.', e.fieldErrors ?? [])
  }
  return data as T
}
