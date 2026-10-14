import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { api, saveLogin } from '../api.js'

export default function AdminLogin() {
  const navigate = useNavigate()
  const [error, setError] = useState('')
  const [busy, setBusy] = useState(false)

  async function onSubmit(e) {
    e.preventDefault()
    setBusy(true)
    setError('')
    const f = Object.fromEntries(new FormData(e.currentTarget).entries())
    try {
      const res = await api('/api/auth/login', { method: 'POST', body: { email: f.email, password: f.password } })
      saveLogin(res)
      navigate('/admin')
    } catch (err) {
      setError(err.message)
    } finally {
      setBusy(false)
    }
  }

  return (
    <>
      <h1>Admin login</h1>
      <form onSubmit={onSubmit} className="card form">
        <label className="field">
          <span className="label">Email</span>
          <input name="email" type="email" required autoComplete="username" />
        </label>
        <label className="field">
          <span className="label">Password</span>
          <input name="password" type="password" required autoComplete="current-password" />
        </label>
        {error && <p className="error banner">{error}</p>}
        <button className="btn primary" disabled={busy}>{busy ? 'Signing in…' : 'Sign in'}</button>
      </form>
    </>
  )
}
