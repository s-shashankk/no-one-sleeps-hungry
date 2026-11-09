import { FormEvent, useState } from 'react'
import { Link, Navigate, useLocation, useNavigate } from 'react-router-dom'
import { ApiRequestError } from '../api'
import { useAuth } from '../auth'
import type { Me } from '../types'

/** Where a person goes after signing in: back to the page they wanted, if it suits their role. */
export function landing(me: Me, from?: string): string {
  if (me.role === 'ADMIN') return from && from.startsWith('/admin') ? from : '/admin'
  if (me.role === 'HOTEL') return '/hotel'
  return from && !from.startsWith('/admin') && from !== '/login' ? from : '/me'
}

export default function Login() {
  const { me, login } = useAuth()
  const navigate = useNavigate()
  const location = useLocation()
  const from = (location.state as { from?: string } | null)?.from
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [error, setError] = useState('')
  const [busy, setBusy] = useState(false)

  if (me) return <Navigate to={landing(me, from)} replace />

  async function submit(e: FormEvent) {
    e.preventDefault()
    setBusy(true)
    setError('')
    try {
      await login(email, password)   // the redirect above runs once the account is known
    } catch (err) {
      setError(err instanceof ApiRequestError ? err.message : 'Could not reach the server.')
      setBusy(false)
    }
  }

  return (
    <form className="card form" onSubmit={submit}>
      <h1>Welcome back 🙏</h1>
      <p className="hint">Good to see you again. Someone is waiting for your help.</p>
      <label>Email
        <input type="email" value={email} onChange={(e) => setEmail(e.target.value)} autoComplete="username" required />
      </label>
      <label>Password
        <input type="password" value={password} onChange={(e) => setPassword(e.target.value)} autoComplete="current-password" required />
      </label>
      {error && <p className="error">{error}</p>}
      <button className="btn primary big" disabled={busy}>{busy ? 'Signing in…' : 'Sign in'}</button>
      <p className="muted">New here? <Link to="/register" state={{ from }}>Become a volunteer</Link>. Forgot your password? Ask the helpsomeone.in team to reset it.</p>
    </form>
  )
}
