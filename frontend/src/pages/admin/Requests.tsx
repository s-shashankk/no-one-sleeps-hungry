import { FormEvent, useCallback, useEffect, useState } from 'react'
import { Link, useSearchParams } from 'react-router-dom'
import { api, PageResponse } from '../../api'
import Pager from '../../components/Pager'
import StatusBadge from '../../components/StatusBadge'
import { errorText, formatDateTime, wholeNumber } from '../../format'
import { FoodRequest, quantityText, RequestType, TYPE_LABEL } from '../../types'

const TABS: { value: string; label: string }[] = [
  { value: 'PENDING', label: 'Pending' },
  { value: 'COMPLETED', label: 'Completed' },
  { value: 'ACCEPTED', label: 'Accepted' },
  { value: 'REJECTED', label: 'Closed (unable)' },
  { value: '', label: 'All' },
]

export default function Requests() {
  const [params, setParams] = useSearchParams()
  const status = params.get('status') ?? 'PENDING'
  const type = params.get('type') ?? ''
  const [adding, setAdding] = useState(params.get('add') === '1')
  const [q, setQ] = useState('')
  const [pageNo, setPageNo] = useState(0)
  const [page, setPage] = useState<PageResponse<FoodRequest> | null>(null)
  const [error, setError] = useState('')

  const load = useCallback(() => {
    const p = new URLSearchParams({ page: String(pageNo), size: '20', sort: status === 'PENDING' ? 'oldest' : 'newest' })
    if (status) p.set('status', status)
    if (type) p.set('type', type)
    if (q.trim()) p.set('q', q.trim())
    api<PageResponse<FoodRequest>>(`/api/v1/requests?${p}`)
      .then((x) => { setPage(x); setError('') })
      .catch((e) => setError(errorText(e)))
  }, [status, type, q, pageNo])

  useEffect(() => { load() }, [load])

  function choose(key: 'status' | 'type', value: string) {
    const next = new URLSearchParams(params)
    next.delete('add')
    if (value || key === 'status') next.set(key, value)
    else next.delete(key)
    setParams(next)
    setPageNo(0)
  }

  return (
    <div className="card">
      <div className="row-between">
        <h1>Requests</h1>
        <button className="btn primary" onClick={() => setAdding(true)}>+ Add request</button>
      </div>

      {adding && <RequestForm onCancel={() => setAdding(false)} onSaved={() => { setAdding(false); load() }} />}

      <div className="tabs">
        {TABS.map((t) => (
          <button key={t.value} className={'tab' + (status === t.value ? ' on' : '')} onClick={() => choose('status', t.value)}>
            {t.label}
          </button>
        ))}
      </div>
      <div className="filters">
        <select value={type} onChange={(e) => choose('type', e.target.value)}>
          <option value="">Both kinds</option>
          <option value="NEED_FOOD">{TYPE_LABEL.NEED_FOOD}</option>
          <option value="SURPLUS_FOOD">{TYPE_LABEL.SURPLUS_FOOD}</option>
        </select>
        <input type="search" placeholder="Search place, name or phone" value={q}
               onChange={(e) => { setQ(e.target.value); setPageNo(0) }} />
      </div>

      {error && <p className="error">{error}</p>}
      {!page && !error && <p>Loading…</p>}
      {page && page.content.length === 0 && <p className="hint">Nothing here.</p>}

      {page && page.content.map((r) => (
        <Link className="item request-row" key={r.id} to={`/admin/requests/${r.id}`}>
          <div className="row-between">
            <strong>{TYPE_LABEL[r.type]} · {quantityText(r)}</strong>
            <StatusBadge status={r.status} />
          </div>
          <div>📍 {r.location}</div>
          {(r.contactName || r.contactPhone) && <div className="hint">{[r.contactName, r.contactPhone].filter(Boolean).join(' · ')}</div>}
          <div className="hint">
            {formatDateTime(r.createdAt)}
            {r.status === 'COMPLETED' && r.mealsDelivered != null && <> · <b>{r.mealsDelivered} meals delivered</b></>}
          </div>
        </Link>
      ))}

      {page && <Pager page={page} onPage={setPageNo} />}
    </div>
  )
}

/** Add a request that arrived by phone or WhatsApp, or correct an existing one. */
export function RequestForm({ request, onCancel, onSaved }: {
  request?: FoodRequest
  onCancel: () => void
  onSaved: () => void
}) {
  const [type, setType] = useState<RequestType>(request?.type ?? 'NEED_FOOD')
  const [location, setLocation] = useState(request?.location ?? '')
  const [quantity, setQuantity] = useState(request ? String(request.quantity) : '')
  const [name, setName] = useState(request?.contactName ?? '')
  const [phone, setPhone] = useState(request?.contactPhone ?? '')
  const [details, setDetails] = useState(request?.details ?? '')
  const [error, setError] = useState('')
  const [busy, setBusy] = useState(false)

  async function save(e: FormEvent) {
    e.preventDefault()
    const n = wholeNumber(quantity)
    if (!n) { setError('Enter a whole number, 1 or more.'); return }
    setBusy(true)
    setError('')
    try {
      const body = { location, quantity: n, contactName: name, contactPhone: phone, details }
      if (request) await api(`/api/v1/requests/${request.id}`, { method: 'PUT', body })
      else await api('/api/v1/requests', { method: 'POST', body: { ...body, type } })
      onSaved()
    } catch (err) {
      setError(errorText(err))
    } finally {
      setBusy(false)
    }
  }

  const need = type === 'NEED_FOOD'
  return (
    <form className="form notice card" onSubmit={save}>
      <h2>{request ? 'Correct details' : 'Add a request'}</h2>
      {!request && (
        <label>What is it?
          <select value={type} onChange={(e) => setType(e.target.value as RequestType)}>
            <option value="NEED_FOOD">{TYPE_LABEL.NEED_FOOD} (people need food)</option>
            <option value="SURPLUS_FOOD">{TYPE_LABEL.SURPLUS_FOOD} (food is available)</option>
          </select>
        </label>
      )}
      <label>{need ? 'Where are they?' : 'Where is the food?'}
        <input value={location} onChange={(e) => setLocation(e.target.value)} maxLength={500} required />
      </label>
      <label>{need ? 'How many people?' : 'How many meals?'}
        <input type="number" min="1" step="1" value={quantity} onChange={(e) => setQuantity(e.target.value)} required />
      </label>
      <label>{need ? 'Contact name (optional)' : 'Hotel / host name (optional)'}
        <input value={name} onChange={(e) => setName(e.target.value)} maxLength={120} />
      </label>
      <label>Contact phone (optional)
        <input type="tel" value={phone} onChange={(e) => setPhone(e.target.value)} placeholder="+91…" />
      </label>
      <label>Details (optional)
        <input value={details} onChange={(e) => setDetails(e.target.value)} maxLength={1000}
               placeholder={need ? 'e.g. families with children' : 'e.g. rice and curry, ready till 10 pm'} />
      </label>
      {error && <p className="error">{error}</p>}
      <div className="actions">
        <button className="btn primary" disabled={busy}>{busy ? 'Saving…' : 'Save'}</button>
        <button type="button" className="btn" onClick={onCancel}>Cancel</button>
      </div>
    </form>
  )
}
