import { useState } from 'react'
import { api } from '../api.js'

export default function LocalitiesPanel({ localities, reload }) {
  const [error, setError] = useState('')
  const [busy, setBusy] = useState(false)

  async function add(e) {
    e.preventDefault()
    const form = e.currentTarget
    const f = Object.fromEntries(new FormData(form).entries())
    setBusy(true)
    setError('')
    try {
      await api('/api/admin/localities', { method: 'POST', body: { name: f.name, city: f.city } })
      form.reset()
      await reload()
    } catch (err) {
      setError(err.message)
    } finally {
      setBusy(false)
    }
  }

  return (
    <>
      <form onSubmit={add} className="card form">
        <h2>Add a locality</h2>
        <label className="field"><span className="label">Locality / area name</span>
          <input name="name" required maxLength={120} /></label>
        <label className="field"><span className="label">City</span>
          <input name="city" required maxLength={120} /></label>
        {error && <p className="error banner">{error}</p>}
        <button className="btn primary" disabled={busy}>Add locality</button>
      </form>
      {localities.length === 0 && <p className="lead">No localities yet. Add the first one above.</p>}
      {localities.map((l) => (
        <article key={l.id} className="card record"><strong>{l.name}</strong>, {l.city}</article>
      ))}
    </>
  )
}
