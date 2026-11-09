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

/** After a 401 on a normal page call, ask the server who we are; if nobody, tell the app so it can show the sign-in page. */
function checkStillSignedIn() {
  fetch(BASE + '/api/v1/auth/me', { credentials: 'same-origin' })
    .then((r) => { if (r.status === 401) window.dispatchEvent(new Event('mealup:signed-out')) })
    .catch(() => undefined)
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

async function readResponse<T>(res: Response): Promise<T> {
  const data: unknown = await res.json().catch(() => undefined)
  if (!res.ok) {
    const e = (data ?? {}) as { message?: string; fieldErrors?: FieldIssue[] }
    throw new ApiRequestError(res.status, e.message ?? 'Something went wrong. Please try again.', e.fieldErrors ?? [])
  }
  return data as T
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
  if (res.status === 401 && !path.startsWith('/api/v1/auth/')) checkStillSignedIn()
  return readResponse<T>(res)
}

/** Uploads one file (form field "file"). The browser sets the multipart content type itself. */
export async function apiUpload<T>(path: string, file: File): Promise<T> {
  const form = new FormData()
  form.append('file', file)
  const res = await fetch(BASE + path, {
    method: 'POST',
    credentials: 'same-origin',
    headers: { 'X-XSRF-TOKEN': await csrfToken() },
    body: form,
  })
  return readResponse<T>(res)
}
