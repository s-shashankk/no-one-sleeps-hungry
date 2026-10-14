import { useCallback, useEffect, useState } from 'react'
import { api } from '../api.js'
import LocalityChecks from './LocalityChecks.jsx'

export default function CoordinatorsPanel({ localities, onUnauthorized }) {
  const [list, setList] = useState(null)
  const [error, setError] = useState('')
  const [newLocs, setNewLocs] = useState([])
  const [busy, setBusy] = useState(false)

  const load = useCallback(async () => {
    try {
      setList(await api('/api/admin/coordinators'))
    } catch (err) {
      if (err.status === 401) return onUnauthorized()
      setList([])
      setError(err.message)
    }
  }, [onUnauthorized])

  useEffect(() => { load() }, [load])

  async function patch(id, body) {
    setError('')
    try {
      const updated = await api(`/api/admin/coordinators/${id}`, { method: 'PATCH', body })
      setList((l) => l.map((c) => (c.id === id ? updated : c)))
    } catch (err) {
      if (err.status === 401) return onUnauthorized()
      setError(err.message)
    }
  }

  async function add(e) {
    e.preventDefault()
    const form = e.currentTarget
    const f = Object.fromEntries(new FormData(form).entries())
    setBusy(true)
    setError('')
    try {
      await api('/api/admin/coordinators', {
        method: 'POST',
        body: { name: f.name, email: f.email, phone: f.phone, password: f.password, localityIds: newLocs },
      })
      form.reset()
      setNewLocs([])
      await load()
    } catch (err) {
      setError(err.message)
    } finally {
      setBusy(false)
    }
  }

  function resetPassword(c) {
    const pw = window.prompt(`New password for ${c.name} (at least 8 characters):`)
    if (pw) patch(c.id, { newPassword: pw })
  }

  return (
    <>
      <form onSubmit={add} className="card form">
        <h2>Add a coordinator</h2>
        <p className="hint">Or approve a volunteer in the Volunteers tab. Share the password with them personally.</p>
        <label className="field"><span className="label">Name</span><input name="name" required maxLength={120} /></label>
        <label className="field"><span className="label">Email (their login)</span><input name="email" type="email" required /></label>
        <label className="field"><span className="label">Phone</span><input name="phone" type="tel" /></label>
        <label className="field"><span className="label">Starting password (8+ characters)</span>
          <input name="password" type="text" required minLength={8} maxLength={72} autoComplete="off" /></label>
        <div className="field"><span className="label">Localities</span>
          <LocalityChecks localities={localities} value={newLocs} onChange={setNewLocs} /></div>
        <button className="btn primary" disabled={busy}>Add coordinator</button>
      </form>

      {error && <p className="error banner">{error}</p>}
      {list === null && <p>Loading…</p>}
      {list?.length === 0 && <p className="lead">No coordinators yet.</p>}
      {list?.map((c) => (
        <article key={c.id} className="card record">
          <header>
            <strong>{c.name}</strong>
            <small className={c.status === 'ACTIVE' ? '' : 'error'}>{c.status}</small>
          </header>
          <p>{c.email}{c.phone && <> · <a href={`tel:${c.phone}`}>{c.phone}</a></>}</p>
          <div className="field"><span className="label">Localities</span>
            <LocalityChecks localities={localities} value={c.localityIds}
              onChange={(ids) => patch(c.id, { localityIds: ids })} /></div>
          <div className="toolbar">
            <button className="btn" onClick={() => patch(c.id, { status: c.status === 'ACTIVE' ? 'SUSPENDED' : 'ACTIVE' })}>
              {c.status === 'ACTIVE' ? 'Suspend' : 'Re-activate'}
            </button>
            <button className="btn" onClick={() => resetPassword(c)}>Set new password</button>
          </div>
        </article>
      ))}
    </>
  )
}
