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
        <h1>Thank you 🙏</h1>
        <p>{done}</p>
        <p className="hint">If someone is in immediate danger or needs urgent medical help, please call your local emergency number.</p>
        <Link to="/">Back to home</Link>
      </div>
    )
  }

  return (
    <form className="card form" onSubmit={submit}>
      <h1>Tell us about people who need food</h1>
      <p className="hint">Our team will check the report and may call before any food is sent. Reports are private and are never shown publicly.</p>

      <label>Where are they? (place, landmark or address)
        <input value={location} onChange={(e) => setLocation(e.target.value)} maxLength={500} required />
      </label>
      <label>About how many people?
        <input type="number" min="1" max="10000" step="1" value={people} onChange={(e) => setPeople(e.target.value)} required />
      </label>
      <label>Anything we should know? (optional)
        <input value={details} onChange={(e) => setDetails(e.target.value)} maxLength={1000} placeholder="e.g. families with children, no cooking facilities" />
      </label>

      <fieldset>
        <legend>Your details (optional)</legend>
        <p className="hint">Only the MealUp team sees these, so we can call you to check.</p>
        <label>Your name<input value={name} onChange={(e) => setName(e.target.value)} maxLength={120} /></label>
        <label>Your phone number<input type="tel" value={phone} onChange={(e) => setPhone(e.target.value)} placeholder="+91…" /></label>
      </fieldset>

      {/* Honeypot: people never see this; bots fill it in. */}
      <div className="hp" aria-hidden="true">
        <label>Website<input tabIndex={-1} autoComplete="off" value={website} onChange={(e) => setWebsite(e.target.value)} /></label>
      </div>

      <label className="check">
        <input type="checkbox" checked={consent} onChange={(e) => setConsent(e.target.checked)} required />
        I agree that MealUp may use this information only to check the report and arrange food. It will be kept private.
      </label>
      {error && <p className="error">{error}</p>}
      <p className="muted">See our <Link to="/privacy">privacy notice</Link>.</p>
      <button className="btn primary" disabled={busy}>{busy ? 'Sending…' : 'Send report'}</button>
    </form>
  )
}
