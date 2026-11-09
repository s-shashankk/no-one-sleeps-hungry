import { FormEvent, useCallback, useEffect, useState } from 'react'
import { api, PageResponse } from '../../api'
import Pager from '../../components/Pager'
import { errorText, formatDateTime } from '../../format'
import { GMail, GPhone } from '../../components/Glyphs'
import { Pickup, PICKUP_LABEL, Vendor, VolunteerRow } from '../../types'

const SORTS = [
  { key: 'recent', label: 'Newest' },
  { key: 'deliveries', label: 'Most deliveries' },
  { key: 'plates', label: 'Most plates' },
  { key: 'name', label: 'Name' },
]

/** Everyone who has an account: how many pickups, deliveries done, plates served and people they reported. */
export default function Volunteers() {
  const [q, setQ] = useState('')
  const [query, setQuery] = useState('')
  const [sort, setSort] = useState('recent')
  const [pageNo, setPageNo] = useState(0)
  const [page, setPage] = useState<PageResponse<VolunteerRow> | null>(null)
  const [error, setError] = useState('')

  const load = useCallback(() => {
    const params = new URLSearchParams({ page: String(pageNo), size: '20', sort })
    if (query) params.set('q', query)
    api<PageResponse<VolunteerRow>>(`/api/v1/volunteers?${params}`)
      .then((p) => { setPage(p); setError('') })
      .catch((e) => setError(errorText(e)))
  }, [pageNo, sort, query])

  useEffect(() => { load() }, [load])

  function search(e: FormEvent) {
    e.preventDefault()
    setPageNo(0)
    setQuery(q.trim())
  }

  return (
    <div className="card">
      <h1>Volunteers</h1>
      <p className="hint">
        {page ? `${page.totalElements} ${page.totalElements === 1 ? 'account' : 'accounts'}. ` : ''}
        “Deliveries” are pickups you marked as done. “People” is what they told us before collecting.
      </p>
      <form className="inline-form" onSubmit={search}>
        <label>Search name, email or phone
          <input value={q} onChange={(e) => setQ(e.target.value)} />
        </label>
        <label>Sort by
          <select value={sort} onChange={(e) => { setSort(e.target.value); setPageNo(0) }}>
            {SORTS.map((s) => <option key={s.key} value={s.key}>{s.label}</option>)}
          </select>
        </label>
        <button className="btn small primary">Search</button>
        <a className="btn small" href="/api/v1/volunteers/export.csv">Download CSV</a>
      </form>
      {error && <p className="error">{error}</p>}
      {!page && !error && <p>Loading…</p>}
      {page && page.content.length === 0 && <p className="hint">No volunteers yet.</p>}
      {page?.content.map((v) => <VolunteerItem key={v.id} v={v} onChanged={load} />)}
      {page && <Pager page={page} onPage={setPageNo} />}
    </div>
  )
}

function VolunteerItem({ v, onChanged }: { v: VolunteerRow; onChanged: () => void }) {
  const [open, setOpen] = useState(false)
  const [pickups, setPickups] = useState<PageResponse<Pickup> | null>(null)
  const [temp, setTemp] = useState('')
  const [hotels, setHotels] = useState<Vendor[] | null>(null)
  const [hotelId, setHotelId] = useState('')
  const [picking, setPicking] = useState(false)
  const [error, setError] = useState('')
  const [busy, setBusy] = useState(false)

  useEffect(() => {
    if (!open) return
    api<PageResponse<Pickup>>(`/api/v1/volunteers/${v.id}/pickups?size=50`)
      .then(setPickups).catch((e) => setError(errorText(e)))
  }, [open, v.id])

  async function act(action: string) {
    setBusy(true); setError('')
    try {
      const r = await api<{ temporaryPassword?: string } | undefined>(`/api/v1/volunteers/${v.id}/${action}`, { method: 'POST', body: {} })
      if (action === 'reset-password' && r?.temporaryPassword) setTemp(r.temporaryPassword)
      else onChanged()
    } catch (e) {
      setError(errorText(e))
    } finally {
      setBusy(false)
    }
  }

  function openHotelPicker() {
    setPicking(true)
    if (!hotels) {
      api<PageResponse<Vendor>>('/api/v1/vendors?size=200&active=true')
        .then((p) => setHotels(p.content)).catch((e) => setError(errorText(e)))
    }
  }

  async function makeHotel() {
    if (!hotelId) { setError('Choose the hotel first.'); return }
    setBusy(true); setError('')
    try {
      await api(`/api/v1/volunteers/${v.id}/make-hotel`, { method: 'POST', body: { vendorId: Number(hotelId) } })
      setPicking(false)
      onChanged()
    } catch (e) {
      setError(errorText(e))
    } finally {
      setBusy(false)
    }
  }

  function resetPassword() {
    if (window.confirm(`Make a new temporary password for ${v.name}? Their old password stops working.`)) act('reset-password')
  }

  return (
    <div className={'item vol' + (v.active ? '' : ' off')}>
      <div className="vol-head">
        <span className="avatar" aria-hidden="true">{v.name.trim().charAt(0).toUpperCase()}</span>
        <div className="person">
          <h3>
            {v.name}
            {v.role === 'HOTEL' && <em className="badge on">Hotel staff{v.hotelName ? `: ${v.hotelName}` : ''}</em>}
            {!v.active && <em className="badge off">Switched off</em>}
          </h3>
          <div className="contacts">
            <span className="contact plain"><GMail />{v.email}</span>
            <a className="contact" href={`tel:${v.phone}`}><GPhone />{v.phone}</a>
          </div>
        </div>
      </div>
      <div className="stat-row">
        <div className="stat"><b>{v.deliveries}</b><span>Deliveries done</span></div>
        <div className="stat"><b>{v.pickups}</b><span>Pickups asked</span></div>
        <div className="stat"><b>{v.platesServed}</b><span>Plates served</span></div>
        <div className="stat"><b>{v.peopleReported}</b><span>People reported</span></div>
      </div>
      <div className="hint">Joined {formatDateTime(v.joinedAt)} · last sign-in {formatDateTime(v.lastLoginAt)}</div>
      {error && <p className="error">{error}</p>}
      {temp && (
        <p className="ok">
          Temporary password for {v.name}: <code>{temp}</code> — tell them now. It is not shown again, and they should change it after signing in.
        </p>
      )}
      <div className="actions">
        <button className="btn small" onClick={() => setOpen(!open)}>{open ? 'Hide pickups' : 'Show pickups'}</button>
        <button className="btn small" disabled={busy} onClick={resetPassword}>Reset password</button>
        {v.role === 'HOTEL'
          ? <button className="btn small" disabled={busy} onClick={() => act('make-volunteer')}>Make ordinary volunteer</button>
          : <button className="btn small" disabled={busy} onClick={openHotelPicker}>Make hotel staff</button>}
        <button className={'btn small' + (v.active ? ' danger' : '')} disabled={busy} onClick={() => act(v.active ? 'disable' : 'enable')}>
          {v.active ? 'Switch off account' : 'Switch on account'}
        </button>
      </div>
      {picking && v.role !== 'HOTEL' && (
        <div className="inline-form">
          <label>Which hotel does {v.name.split(' ')[0]} work at?
            <select value={hotelId} onChange={(e) => setHotelId(e.target.value)}>
              <option value="">Choose a hotel</option>
              {hotels?.map((h) => <option key={h.id} value={h.id}>{h.name}{h.areaName ? `, ${h.areaName}` : ''}</option>)}
            </select>
          </label>
          <button className="btn small primary" disabled={busy} onClick={makeHotel}>Give hotel access</button>
          <button className="btn small" onClick={() => setPicking(false)}>Back</button>
          <span className="hint">They keep the same email and password. They will see only this hotel's bookings.</span>
        </div>
      )}
      {open && (
        <div>
          {!pickups && !error && <p>Loading…</p>}
          {pickups && pickups.content.length === 0 && <p className="hint">No pickups yet.</p>}
          {pickups?.content.map((p) => (
            <div key={p.id} className="hint">
              {formatDateTime(p.createdAt)} · {p.vendorName}, {p.areaName} · {p.peopleCount} people · {PICKUP_LABEL[p.status]}
              {p.status === 'COMPLETED' ? ` · ${p.platesServed} plates` : ''}
            </div>
          ))}
        </div>
      )}
    </div>
  )
}
