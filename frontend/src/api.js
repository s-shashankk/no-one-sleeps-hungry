// Tiny fetch wrapper. In dev, Vite proxies /api to the backend.
const BASE = import.meta.env.VITE_API_BASE || ''
const TOKEN_KEY = 'nsh_token'
const USER_KEY = 'nsh_user'

// localStorage can throw (private mode, blocked) - never let that break the page.
export function getToken() {
  try { return localStorage.getItem(TOKEN_KEY) } catch { return null }
}
export function getUser() {
  try { return JSON.parse(localStorage.getItem(USER_KEY) || 'null') } catch { return null }
}
export function saveLogin({ token, name, role }) {
  try {
    localStorage.setItem(TOKEN_KEY, token)
    localStorage.setItem(USER_KEY, JSON.stringify({ name, role }))
  } catch { /* ignore */ }
}
export function clearLogin() {
  try {
    localStorage.removeItem(TOKEN_KEY)
    localStorage.removeItem(USER_KEY)
  } catch { /* ignore */ }
}

export async function api(path, { method = 'GET', body } = {}) {
  const headers = {}
  if (body) headers['Content-Type'] = 'application/json'
  const token = getToken()
  if (token) headers.Authorization = `Bearer ${token}`

  const res = await fetch(BASE + path, {
    method,
    headers,
    body: body ? JSON.stringify(body) : undefined,
  })
  const data = await res.json().catch(() => ({}))
  if (!res.ok) {
    const err = new Error(data.message || 'Something went wrong. Please try again.')
    err.fields = data.fields || {}
    err.status = res.status
    throw err
  }
  return data
}
