import { FormEvent, useState } from 'react'
import { Link, Navigate, useLocation } from 'react-router-dom'
import { useAuth } from '../auth'
import { errorText } from '../format'
import { landing } from './Login'

/** A volunteer creates an account with an email and a password. They are signed in straight away. */
export default function Register() {
  const { me, register } = useAuth()
  const location = useLocation()
  const from = (location.state as { from?: string } | null)?.from ?? '/deliver'
  const [name, setName] = useState('')
  const [email, setEmail] = useState('')
  const [phone, setPhone] = useState('')
  const [password, setPassword] = useState('')
  const [consent, setConsent] = useState(false)
  const [website, setWebsite] = useState('')           // honeypot: hidden from people
  const [error, setError] = useState('')
  const [busy, setBusy] = useState(false)

  if (me) return <Navigate to={landing(me, from)} replace />

  async function submit(e: FormEvent) {
    e.preventDefault()
    setBusy(true)
    setError('')
    try {
      await register({ name, email, phone, password, consent, website })
    } catch (err) {
      setError(errorText(err))
      setBusy(false)
    }
  }

  return (
    <form className="card form" onSubmit={submit}>
      <h1>Thank you for caring</h1>
      <p className="hint">A small account helps us keep you safe, call you when you go to collect food, and count every meal you carry. You will never be charged anything.</p>
      <label>Your name
        <input value={name} onChange={(e) => setName(e.target.value)} maxLength={120} required autoComplete="name" />
      </label>
      <label>Email
        <input type="email" value={email} onChange={(e) => setEmail(e.target.value)} maxLength={254} required autoComplete="username" />
      </label>
      <label>Phone number
        <input type="tel" value={phone} onChange={(e) => setPhone(e.target.value)} placeholder="+91…" required autoComplete="tel" />
      </label>
      <label>Password (8 or more characters)
        <input type="password" value={password} onChange={(e) => setPassword(e.target.value)} minLength={8} maxLength={72} required autoComplete="new-password" />
      </label>
      <div className="hp" aria-hidden="true">
        <label>Website<input tabIndex={-1} autoComplete="off" value={website} onChange={(e) => setWebsite(e.target.value)} /></label>
      </div>
      <label className="check">
        <input type="checkbox" checked={consent} onChange={(e) => setConsent(e.target.checked)} required />
        I agree that helpsomeone.in may keep my details and share my name and phone number with a hotel when I choose to collect food from it.
      </label>
      <p className="muted">See our <Link to="/privacy">privacy notice</Link>.</p>
      {error && <p className="error" role="alert">{error}</p>}
      <button className="btn primary big" disabled={busy}>{busy ? 'Creating…' : 'Join as a volunteer'}</button>
      <p className="muted">Already have an account? <Link to="/login" state={{ from }}>Sign in</Link></p>
    </form>
  )
}
