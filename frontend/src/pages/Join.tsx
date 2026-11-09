import { FormEvent, useState } from 'react'
import { Link } from 'react-router-dom'
import { api } from '../api'
import { errorText } from '../format'

/** Public form: anyone can ask to join the community. It lands in the admin's join-requests inbox. */
export default function Join() {
  const [name, setName] = useState('')
  const [phone, setPhone] = useState('')
  const [email, setEmail] = useState('')
  const [location, setLocation] = useState('')
  const [message, setMessage] = useState('')
  const [consent, setConsent] = useState(false)
  const [website, setWebsite] = useState('')            // honeypot: hidden from people
  const [error, setError] = useState('')
  const [busy, setBusy] = useState(false)
  const [done, setDone] = useState('')

  async function submit(e: FormEvent) {
    e.preventDefault()
    setBusy(true)
    setError('')
    try {
      const r = await api<{ message: string }>('/api/v1/public/join-requests', {
        method: 'POST',
        body: { name, phone, email: email || undefined, location: location || undefined, message: message || undefined, consent, website },
      })
      setDone(r.message)
    } catch (err) {
      setError(errorText(err))
    } finally {
      setBusy(false)
    }
  }

  if (done) {
    return (
      <div className="narrow card">
        <h1>Welcome, {name.split(' ')[0] || 'friend'} 🙏</h1>
        <p>{done}</p>
        <Link className="btn primary" to="/">Back to home</Link>
      </div>
    )
  }

  return (
    <div className="narrow">
      <h1>Walk with us</h1>
      <p className="lead">Volunteer, hotel owner, neighbour or friend: there is a way for every kind heart to help. Leave your name and we will call you. We ask nothing in return; their blessings are enough.</p>
      <form className="card form" onSubmit={submit}>
        <label>Your name
          <input value={name} onChange={(e) => setName(e.target.value)} maxLength={120} required autoComplete="name" />
        </label>
        <label>Your phone number
          <input type="tel" value={phone} onChange={(e) => setPhone(e.target.value)} placeholder="+91…" required autoComplete="tel" />
        </label>
        <label>Email (optional)
          <input type="email" value={email} onChange={(e) => setEmail(e.target.value)} maxLength={150} autoComplete="email" />
        </label>
        <label>Where are you? (area or neighbourhood)
          <input value={location} onChange={(e) => setLocation(e.target.value)} maxLength={200} placeholder="e.g. Domlur, Bengaluru" />
        </label>
        <label>How would you like to help? (optional)
          <textarea value={message} onChange={(e) => setMessage(e.target.value)} maxLength={1000}
                    placeholder="e.g. I own a restaurant / I have a bike and weekends free" />
        </label>

        <div className="hp" aria-hidden="true">
          <label>Website<input tabIndex={-1} autoComplete="off" value={website} onChange={(e) => setWebsite(e.target.value)} /></label>
        </div>

        <label className="check">
          <input type="checkbox" checked={consent} onChange={(e) => setConsent(e.target.checked)} required />
          I agree that helpsomeone.in may contact me about volunteering. My details stay private.
        </label>
        {error && <p className="error" role="alert">{error}</p>}
        <p className="muted">See our <Link to="/privacy">privacy notice</Link>.</p>
        <button className="btn primary big" disabled={busy}>{busy ? 'Sending…' : 'Walk with us'}</button>
      </form>
    </div>
  )
}
