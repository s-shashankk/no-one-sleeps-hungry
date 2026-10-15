import { useCallback, useEffect, useMemo, useState } from 'react'
import { Link, useParams } from 'react-router-dom'
import { api, PageResponse } from '../../api'
import StatusBadge from '../../components/StatusBadge'
import { errorText, formatDateTime, whatsappLink, wholeNumber } from '../../format'
import { Coworker, FoodRequest, quantityText, TYPE_LABEL, Vendor } from '../../types'
import { RequestForm } from './Requests'

/** One request: check vendors, message coworkers, then record the result. */
export default function RequestDetail() {
  const { id } = useParams()
  const [r, setR] = useState<FoodRequest | null>(null)
  const [error, setError] = useState('')
  const [editing, setEditing] = useState(false)

  const load = useCallback(() => {
    api<FoodRequest>(`/api/v1/requests/${id}`).then((x) => { setR(x); setError('') }).catch((e) => setError(errorText(e)))
  }, [id])
  useEffect(() => { load() }, [load])

  if (error) return <div className="card"><p className="error">{error}</p><Link to="/admin/requests">← Back to requests</Link></div>
  if (!r) return <div className="card"><p>Loading…</p></div>

  const need = r.type === 'NEED_FOOD'
  const pending = r.status === 'PENDING'

  return (
    <>
      <p><Link to="/admin/requests">← All requests</Link></p>
      <div className="card">
        <div className="row-between">
          <h1>{TYPE_LABEL[r.type]}</h1>
          <StatusBadge status={r.status} />
        </div>
        {editing ? (
          <RequestForm request={r} onCancel={() => setEditing(false)} onSaved={() => { setEditing(false); load() }} />
        ) : (
          <>
            <p><b>{quantityText(r)}</b> · 📍 {r.location}</p>
            {(r.contactName || r.contactPhone) && (
              <p>
                {r.contactName}{' '}
                {r.contactPhone && <>📞 <a href={`tel:${r.contactPhone}`}>{r.contactPhone}</a> · <a href={whatsappLink(r.contactPhone)} target="_blank" rel="noreferrer">WhatsApp</a></>}
              </p>
            )}
            {r.details && <p>{r.details}</p>}
            <p className="hint">Received {formatDateTime(r.createdAt)}</p>
            <button className="btn small" onClick={() => setEditing(true)}>Correct details</button>
          </>
        )}
      </div>

      {pending ? (
        <Pending r={r} need={need} onChanged={setR} />
      ) : (
        <Closed r={r} onChanged={setR} />
      )}
    </>
  )
}

function Pending({ r, need, onChanged }: { r: FoodRequest; need: boolean; onChanged: (r: FoodRequest) => void }) {
  const [meals, setMeals] = useState('')
  const [note, setNote] = useState('')
  const [error, setError] = useState('')
  const [busy, setBusy] = useState(false)

  async function act(action: string, body: unknown) {
    setBusy(true)
    setError('')
    try {
      onChanged(await api<FoodRequest>(`/api/v1/requests/${r.id}/${action}`, { method: 'POST', body }))
    } catch (e) {
      setError(errorText(e))
    } finally {
      setBusy(false)
    }
  }

  function complete() {
    const n = wholeNumber(meals)
    if (!n) { setError('Enter how many meals were delivered (a whole number, 1 or more).'); return }
    act('complete', { mealsDelivered: n, note })
  }

  return (
    <>
      {need && <VendorList />}
      <CoworkerList request={r} />

      <div className="card">
        <h2>{need ? '3. Record the result' : '2. Your decision'}</h2>
        {need ? (
          <>
            <div className="form">
              <label>Meals delivered
                <input type="number" min="1" step="1" value={meals} onChange={(e) => setMeals(e.target.value)} placeholder={`e.g. ${r.quantity}`} />
              </label>
              <label>Note (optional)
                <input value={note} onChange={(e) => setNote(e.target.value)} maxLength={500} />
              </label>
            </div>
            {error && <p className="error">{error}</p>}
            <div className="actions">
              <button className="btn primary" disabled={busy} onClick={complete}>Mark completed</button>
              <button className="btn" disabled={busy} onClick={() => act('reject', { note })}>Unable to serve</button>
            </div>
            <p className="hint">“Unable to serve” removes this request from the pending list. Nothing is added to the meals total.</p>
          </>
        ) : (
          <>
            <div className="form">
              <label>Note (optional)
                <input value={note} onChange={(e) => setNote(e.target.value)} maxLength={500} />
              </label>
            </div>
            {error && <p className="error">{error}</p>}
            <div className="actions">
              <button className="btn primary" disabled={busy} onClick={() => act('accept', { note })}>Accept: I’ll send a team</button>
              <button className="btn" disabled={busy} onClick={() => act('reject', { note })}>Unable to collect</button>
            </div>
          </>
        )}
      </div>
    </>
  )
}

function Closed({ r, onChanged }: { r: FoodRequest; onChanged: (r: FoodRequest) => void }) {
  const [error, setError] = useState('')

  async function reopen() {
    try {
      onChanged(await api<FoodRequest>(`/api/v1/requests/${r.id}/reopen`, { method: 'POST', body: {} }))
    } catch (e) {
      setError(errorText(e))
    }
  }

  return (
    <div className="card">
      <h2>Result</h2>
      {r.status === 'COMPLETED' && <p><b>{r.mealsDelivered} meals delivered</b> · {formatDateTime(r.closedAt)}</p>}
      {r.status === 'ACCEPTED' && <p>Accepted. A team is sent to collect the food. · {formatDateTime(r.closedAt)}</p>}
      {r.status === 'REJECTED' && <p>{r.type === 'NEED_FOOD' ? 'Unable to serve' : 'Unable to collect'} · {formatDateTime(r.closedAt)}</p>}
      {r.note && <p>Note: {r.note}</p>}
      {error && <p className="error">{error}</p>}
      <button className="btn small" onClick={reopen}>Reopen (fix a mistake)</button>
    </div>
  )
}

/** Step 1 for a food request: who might have surplus? Call them. */
function VendorList() {
  const [vendors, setVendors] = useState<Vendor[] | null>(null)
  const [q, setQ] = useState('')
  const [error, setError] = useState('')

  useEffect(() => {
    api<PageResponse<Vendor>>('/api/v1/vendors?active=true&size=100')
      .then((p) => setVendors(p.content)).catch((e) => setError(errorText(e)))
  }, [])

  const shown = useMemo(() => {
    const t = q.trim().toLowerCase()
    return (vendors ?? []).filter((v) => !t || `${v.name} ${v.location} ${v.contactPerson ?? ''}`.toLowerCase().includes(t))
  }, [vendors, q])

  return (
    <div className="card">
      <h2>1. Check with vendors</h2>
      <p className="hint">Call a vendor and ask if they have surplus food.</p>
      <input type="search" placeholder="Search vendors" value={q} onChange={(e) => setQ(e.target.value)} />
      {error && <p className="error">{error}</p>}
      {vendors && vendors.length === 0 && <p className="hint">No active vendors yet. <Link to="/admin/vendors">Add one</Link>.</p>}
      {shown.map((v) => (
        <div className="item row-between" key={v.id}>
          <div>
            <strong>{v.name}</strong>
            <div className="hint">📍 {v.location}{v.contactPerson ? ` · ${v.contactPerson}` : ''}</div>
          </div>
          <a className="btn small primary" href={`tel:${v.phoneNumber}`}>📞 Call</a>
        </div>
      ))}
    </div>
  )
}

/** Step 2 for a food request / step 1 for surplus: message a coworker on WhatsApp. */
function CoworkerList({ request }: { request: FoodRequest }) {
  const need = request.type === 'NEED_FOOD'
  const [coworkers, setCoworkers] = useState<Coworker[] | null>(null)
  const [q, setQ] = useState('')
  const [error, setError] = useState('')

  const suggested = need
    ? `Food is needed for about ${request.quantity} people at ${request.location}.`
      + (request.contactName || request.contactPhone ? ` Contact: ${[request.contactName, request.contactPhone].filter(Boolean).join(' ')}.` : '')
      + (request.details ? ` ${request.details}.` : '') + ' Can you collect and distribute?'
    : `Please collect about ${request.quantity} meals from ${request.contactName ?? 'the host'} at ${request.location}.`
      + (request.contactPhone ? ` Call ${request.contactPhone}.` : '') + (request.details ? ` ${request.details}.` : '')
  const [message, setMessage] = useState(suggested)

  useEffect(() => {
    api<PageResponse<Coworker>>('/api/v1/coworkers?active=true&size=100')
      .then((p) => setCoworkers(p.content)).catch((e) => setError(errorText(e)))
  }, [])

  const shown = useMemo(() => {
    const t = q.trim().toLowerCase()
    return (coworkers ?? []).filter((c) => !t || `${c.name} ${c.location}`.toLowerCase().includes(t))
  }, [coworkers, q])

  return (
    <div className="card">
      <h2>{need ? '2. Message a coworker' : '1. Message a coworker'}</h2>
      <div className="form">
        <label>Message (you can edit it)
          <textarea rows={4} value={message} onChange={(e) => setMessage(e.target.value)} />
        </label>
      </div>
      <input type="search" placeholder="Search coworkers or areas" value={q} onChange={(e) => setQ(e.target.value)} />
      {error && <p className="error">{error}</p>}
      {coworkers && coworkers.length === 0 && <p className="hint">No active coworkers yet. <Link to="/admin/coworkers">Add one</Link>.</p>}
      {shown.map((c) => (
        <div className="item row-between" key={c.id}>
          <div>
            <strong>{c.name}</strong>
            <div className="hint">📍 {c.location}</div>
          </div>
          <div className="actions">
            <a className="btn small primary" href={whatsappLink(c.phoneNumber, message)} target="_blank" rel="noreferrer">WhatsApp</a>
            <a className="btn small" href={`tel:${c.phoneNumber}`}>📞</a>
          </div>
        </div>
      ))}
    </div>
  )
}
