import { FormEvent, useState } from 'react'
import { api } from '../api'
import { useAuth } from '../auth'
import { GMail, GPhone } from '../components/Glyphs'
import { errorText } from '../format'

/** A volunteer's account: who they are, and a way to change their password. */
export default function Account() {
  const { me } = useAuth()
  return (
    <div className="narrow">
      <h1>My account</h1>
      <div className="card">
        <h2>{me?.fullName}</h2>
        <div className="contacts">
          <span className="contact plain"><GMail />{me?.email}</span>
          <span className="contact plain"><GPhone />{me?.phoneNumber}</span>
        </div>
        <p className="hint">To change your name, email or phone number, please ask the helpsomeone.in team.</p>
      </div>
      <ChangePassword />
    </div>
  )
}

function ChangePassword() {
  const [current, setCurrent] = useState('')
  const [next, setNext] = useState('')
  const [msg, setMsg] = useState('')
  const [error, setError] = useState('')
  const [busy, setBusy] = useState(false)

  async function submit(e: FormEvent) {
    e.preventDefault()
    setBusy(true); setError(''); setMsg('')
    try {
      await api('/api/v1/me/password', { method: 'POST', body: { currentPassword: current, newPassword: next } })
      setCurrent(''); setNext(''); setMsg('Password changed.')
    } catch (err) {
      setError(errorText(err))
    } finally {
      setBusy(false)
    }
  }

  return (
    <form className="card form" onSubmit={submit}>
      <h2>Change my password</h2>
      <label>Current password
        <input type="password" value={current} onChange={(e) => setCurrent(e.target.value)} autoComplete="current-password" required />
      </label>
      <label>New password (8 or more characters)
        <input type="password" value={next} onChange={(e) => setNext(e.target.value)} minLength={8} maxLength={72} autoComplete="new-password" required />
      </label>
      {error && <p className="error">{error}</p>}
      {msg && <p className="ok">{msg}</p>}
      <button className="btn primary" disabled={busy}>Change password</button>
    </form>
  )
}
