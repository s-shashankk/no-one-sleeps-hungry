import { useCallback, useEffect, useState } from 'react'
import { api } from '../api.js'
import LocalityChecks from './LocalityChecks.jsx'

const when = (v) => (v ? new Date(v).toLocaleString() : '—')
const Tel = ({ n }) => (n ? <a href={`tel:${n}`}>{n}</a> : <>—</>)

// One config per kind of record: where to load it, its statuses, and how a card looks.
export const KINDS = {
  offers: {
    label: 'Food offers',
    path: '/api/admin/food-offers',
    assignable: true,
    statuses: ['OPEN', 'ASSIGNED', 'IN_TRANSIT', 'COMPLETED', 'REJECTED', 'EXPIRED', 'CANCELLED'],
    title: (r) => `${r.servings} servings · ${r.foodType.replace('_', '-')} · ${r.area}`,
    body: (r) => (
      <>
        <p>{r.description}</p>
        <p><b>Pickup:</b> {r.pickupAddress}</p>
        <p><b>From:</b> {r.vendorName ? `${r.vendorName} — ` : ''}{r.submitterName}, <Tel n={r.submitterPhone} /></p>
        <p><b>Cooked:</b> {when(r.cookedAt)} · <b>Safe until:</b> {when(r.expiresAt)}</p>
      </>
    ),
  },
  reports: {
    label: 'Hunger reports',
    path: '/api/admin/hunger-reports',
    assignable: true,
    statuses: ['NEW', 'VERIFIED', 'SCHEDULED', 'SERVED', 'INVALID', 'RECURRING'],
    title: (r) => `${r.peopleCount} people · ${r.area}`,
    body: (r) => (
      <>
        <p><b>Where:</b> {r.locationText}
          {r.latitude != null && (
            <> · <a href={`https://www.google.com/maps?q=${r.latitude},${r.longitude}`} target="_blank" rel="noreferrer">map</a></>
          )}
        </p>
        {r.notes && <p>{r.notes}</p>}
        <p><b>Reporter:</b> {r.reporterName || 'anonymous'}, <Tel n={r.reporterPhone} /></p>
      </>
    ),
  },
  volunteers: {
    label: 'Volunteers',
    path: '/api/admin/volunteer-applications',
    assignable: false,
    statuses: ['PENDING', 'APPROVED', 'REJECTED'],
    title: (r) => `${r.name} · ${r.area}`,
    body: (r) => (
      <>
        <p><Tel n={r.phone} />{r.email && <> · {r.email}</>}</p>
        {r.collegeOrOccupation && <p>{r.collegeOrOccupation}</p>}
        {r.message && <p>{r.message}</p>}
      </>
    ),
  },
}

// Turn a volunteer into a coordinator: pick login email, starting password and localities.
function ApproveForm({ r, localities, onDone, onCancel }) {
  const [locs, setLocs] = useState(r.localityId ? [r.localityId] : [])
  const [error, setError] = useState('')
  const [busy, setBusy] = useState(false)

  async function submit(e) {
    e.preventDefault()
    const f = Object.fromEntries(new FormData(e.currentTarget).entries())
    setBusy(true)
    setError('')
    try {
      await api(`/api/admin/volunteer-applications/${r.id}/approve`, {
        method: 'POST', body: { email: f.email, password: f.password, localityIds: locs },
      })
      onDone()
    } catch (err) {
      setError(err.fields?.password ? 'Password must be at least 8 characters.' : err.message)
    } finally {
      setBusy(false)
    }
  }

  return (
    <form onSubmit={submit} className="form subform">
      <label className="field"><span className="label">Login email</span>
        <input name="email" type="email" required defaultValue={r.email || ''} /></label>
      <label className="field"><span className="label">Starting password (8+ characters)</span>
        <input name="password" type="text" required minLength={8} maxLength={72} autoComplete="off" /></label>
      <div className="field"><span className="label">Localities they will manage</span>
        <LocalityChecks localities={localities} value={locs} onChange={setLocs} /></div>
      {error && <p className="error banner">{error}</p>}
      <div className="toolbar">
        <button className="btn primary" disabled={busy}>Approve &amp; create account</button>
        <button type="button" className="btn" onClick={onCancel}>Cancel</button>
      </div>
    </form>
  )
}

export default function AdminList({ kind, localities, isSuper, onUnauthorized, onCreateDelivery }) {
  const cfg = KINDS[kind]
  const [rows, setRows] = useState(null)
  const [filter, setFilter] = useState('')
  const [error, setError] = useState('')
  const [approving, setApproving] = useState(null)

  const locName = (id) => localities.find((l) => l.id === id)
  const locLabel = (id) => { const l = locName(id); return l ? `${l.name}, ${l.city}` : null }

  const load = useCallback(async () => {
    setError('')
    try {
      setRows(await api(cfg.path + (filter ? `?status=${filter}` : '')))
    } catch (err) {
      if (err.status === 401) return onUnauthorized()
      setRows([])
      setError(err.status === 403 ? 'You do not have access to this section.' : err.message)
    }
  }, [cfg.path, filter, onUnauthorized])

  useEffect(() => { setRows(null); load() }, [load])

  async function send(id, suffix, body) {
    try {
      const updated = await api(`${cfg.path}/${id}/${suffix}`, { method: 'PATCH', body })
      setRows((rs) => rs.map((r) => (r.id === id ? updated : r)))
    } catch (err) {
      if (err.status === 401) return onUnauthorized()
      setError(err.message)
    }
  }

  return (
    <>
      <div className="toolbar">
        <select value={filter} onChange={(e) => setFilter(e.target.value)} aria-label="Filter by status">
          <option value="">All statuses</option>
          {cfg.statuses.map((s) => <option key={s} value={s}>{s}</option>)}
        </select>
        <button className="btn" onClick={load}>Refresh</button>
      </div>
      {error && <p className="error banner">{error}</p>}
      {rows === null && <p>Loading…</p>}
      {rows?.length === 0 && !error && <p className="lead">Nothing here yet.</p>}
      {rows?.map((r) => (
        <article key={r.id} className="card record">
          <header>
            <strong>{cfg.title(r)}</strong>
            <small>{when(r.createdAt)}</small>
          </header>
          {cfg.body(r)}
          <p><b>Locality:</b> {locLabel(r.localityId) || <span className="error">not assigned</span>}</p>

          {isSuper && cfg.assignable && (
            <label className="status-row">
              <span>Assign to</span>
              <select value={r.localityId ?? ''}
                onChange={(e) => send(r.id, 'locality', { localityId: e.target.value ? Number(e.target.value) : null })}>
                <option value="">— none —</option>
                {localities.map((l) => <option key={l.id} value={l.id}>{l.name}, {l.city}</option>)}
              </select>
            </label>
          )}

          <label className="status-row">
            <span>Status</span>
            <select value={r.status} onChange={(e) => send(r.id, 'status', { status: e.target.value })}>
              {cfg.statuses.map((s) => <option key={s} value={s}>{s}</option>)}
            </select>
          </label>

          {kind === 'offers' && r.status === 'OPEN' && onCreateDelivery && (
            <button className="btn primary" onClick={() => onCreateDelivery(r.id)}>Create delivery…</button>
          )}

          {kind === 'volunteers' && r.status !== 'APPROVED' && (
            approving === r.id
              ? <ApproveForm r={r} localities={localities} onCancel={() => setApproving(null)}
                  onDone={() => { setApproving(null); load() }} />
              : <button className="btn primary" onClick={() => setApproving(r.id)}>Approve as coordinator…</button>
          )}
        </article>
      ))}
    </>
  )
}
