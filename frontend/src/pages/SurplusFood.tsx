import { FormEvent, useState } from 'react'
import { Link } from 'react-router-dom'
import { api } from '../api'
import { errorText, wholeNumber } from '../format'

/** Public form: anyone can tell us about surplus food. It only creates a pending request for the team. */
export default function SurplusFood() {
  const [name, setName] = useState('')
  const [phone, setPhone] = useState('')
  const [location, setLocation] = useState('')
  const [meals, setMeals] = useState('')
  const [details, setDetails] = useState('')
  const [consent, setConsent] = useState(false)
  const [website, setWebsite] = useState('')        // honeypot: hidden from people
  const [error, setError] = useState('')
  const [busy, setBusy] = useState(false)
  const [done, setDone] = useState('')

  async function submit(e: FormEvent) {
    e.preventDefault()
    const n = wholeNumber(meals)
    if (!n) { setError('Please enter about how many meals (a whole number).'); return }
    setBusy(true)
    setError('')
    try {
      const r = await api<{ message: string }>('/api/v1/public/surplus-food-reports', {
        method: 'POST',
        body: { name, phone, location, meals: n, details: details || undefined, consent, website },
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
        <Link to="/">Back to home</Link>
      </div>
    )
  }

  return (
    <form className="card form" onSubmit={submit}>
      <h1>Tell us about surplus food</h1>
      <p className="hint">We will call you to confirm before anyone comes to collect it. Please only offer food that is fresh and safe to eat.</p>

      <label>Hotel, caterer or host name
        <input value={name} onChange={(e) => setName(e.target.value)} maxLength={160} required />
      </label>
      <label>Phone number
        <input type="tel" value={phone} onChange={(e) => setPhone(e.target.value)} placeholder="+91…" required />
      </label>
      <label>Where should we collect it? (address)
        <input value={location} onChange={(e) => setLocation(e.target.value)} maxLength={500} required />
      </label>
      <label>About how many meals?
        <input type="number" min="1" max="100000" step="1" value={meals} onChange={(e) => setMeals(e.target.value)} required />
      </label>
      <label>What food is it, and until what time can it be collected? (optional)
        <input value={details} onChange={(e) => setDetails(e.target.value)} maxLength={1000} placeholder="e.g. veg biryani, packed, collect before 10 pm" />
      </label>

      {/* Honeypot: people never see this; bots fill it in. */}
      <div className="hp" aria-hidden="true">
        <label>Website<input tabIndex={-1} autoComplete="off" value={website} onChange={(e) => setWebsite(e.target.value)} /></label>
      </div>

      <label className="check">
        <input type="checkbox" checked={consent} onChange={(e) => setConsent(e.target.checked)} required />
        I agree that MealUp may use these details only to arrange the pickup.
      </label>
      {error && <p className="error">{error}</p>}
      <p className="muted">See our <Link to="/privacy">privacy notice</Link>.</p>
      <button className="btn primary" disabled={busy}>{busy ? 'Sending…' : 'Send'}</button>
    </form>
  )
}
