import { FormEvent, useState } from 'react'
import { Link } from 'react-router-dom'
import { api } from '../api'
import { errorText, wholeNumber } from '../format'

/** Public form: anyone can tell us about people who need food. It only creates a private, pending request. */
export default function ReportHunger() {
  const [location, setLocation] = useState('')
  const [people, setPeople] = useState('')
  const [details, setDetails] = useState('')
  const [name, setName] = useState('')
  const [phone, setPhone] = useState('')
  const [consent, setConsent] = useState(false)
  const [website, setWebsite] = useState('')        // honeypot: hidden from people
  const [error, setError] = useState('')
  const [busy, setBusy] = useState(false)
  const [done, setDone] = useState('')

  async function submit(e: FormEvent) {
    e.preventDefault()
    const n = wholeNumber(people)
    if (!n) { setError('Please enter about how many people (a whole number).'); return }
    setBusy(true)
    setError('')
    try {
      const r = await api<{ message: string }>('/api/v1/public/hunger-reports', {
        method: 'POST',
        body: { location, people: n, details: details || undefined, name: name || undefined, phone: phone || undefined, consent, website },
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
      <div className="card">
        <h1>Thank you for caring 🙏</h1>
        <p>{done}</p>
        <p className="hint">If someone is in immediate danger or needs urgent medical help, please call your local emergency number.</p>
        <div className="recap">
          <strong>Can you carry food there yourself?</strong> If you can, choose a kind hotel and bring a meal. It would mean the world to them.
        </div>
        <p className="actions">
          <Link className="btn primary" to={`/deliver?to=${encodeURIComponent(location)}`}>Yes, I will take food</Link>
          <Link className="btn" to="/">Back to home</Link>
        </p>
      </div>
    )
  }

  return (
    <form className="card form" onSubmit={submit}>
      <h1>Have you seen someone hungry?</h1>
      <p className="lead">Thank you for not looking away. Tell us where they are, and we will do everything we can to get a warm meal to them.</p>
      <p className="hint">We may call you once to be sure we reach the right place. What you share is private and never shown publicly.</p>

      <label>Where are they? (street, landmark or address)
        <input value={location} onChange={(e) => setLocation(e.target.value)} maxLength={500} required />
      </label>
      <label>About how many people are hungry?
        <input type="number" min="1" max="10000" step="1" value={people} onChange={(e) => setPeople(e.target.value)} required />
      </label>
      <label>Anything else we should know? (optional)
        <input value={details} onChange={(e) => setDetails(e.target.value)} maxLength={1000} placeholder="e.g. a family with small children, no place to cook" />
      </label>

      <fieldset>
        <legend>Your details (optional)</legend>
        <p className="hint">Only the helpsomeone.in team sees these, so we can call you to check.</p>
        <label>Your name<input value={name} onChange={(e) => setName(e.target.value)} maxLength={120} /></label>
        <label>Your phone number<input type="tel" value={phone} onChange={(e) => setPhone(e.target.value)} placeholder="+91…" /></label>
      </fieldset>

      {/* Honeypot: people never see this; bots fill it in. */}
      <div className="hp" aria-hidden="true">
        <label>Website<input tabIndex={-1} autoComplete="off" value={website} onChange={(e) => setWebsite(e.target.value)} /></label>
      </div>

      <label className="check">
        <input type="checkbox" checked={consent} onChange={(e) => setConsent(e.target.checked)} required />
        I agree that helpsomeone.in may use this information only to check the report and arrange food. It will be kept private.
      </label>
      {error && <p className="error">{error}</p>}
      <p className="muted">See our <Link to="/privacy">privacy notice</Link>.</p>
      <button className="btn primary" disabled={busy}>{busy ? 'Sending…' : 'Send this to the team'}</button>
    </form>
  )
}
