import { FormEvent, useCallback, useEffect, useState } from 'react'
import { GMail, GPeople, GPhone, GPin, GRoute, GUser } from '../../components/Glyphs'
import { api, PageResponse } from '../../api'
import Pager from '../../components/Pager'
import { errorText, formatDateTime, whatsappLink } from '../../format'
import { Pickup, PICKUP_LABEL, PickupStatus } from '../../types'

const TABS: { key: PickupStatus | ''; label: string }[] = [
  { key: 'PLEDGED', label: 'New' },
  { key: 'CONFIRMED', label: 'Hotel informed' },
  { key: 'COMPLETED', label: 'Done' },
  { key: 'CANCELLED', label: 'Cancelled' },
  { key: '', label: 'All' },
]

/** Volunteers who said "I will collect food from this hotel": inform the hotel, then record plates served. */
export default function Pickups() {
  const [status, setStatus] = useState<PickupStatus | ''>('PLEDGED')
  const [pageNo, setPageNo] = useState(0)
  const [page, setPage] = useState<PageResponse<Pickup> | null>(null)
  const [error, setError] = useState('')

  const load = useCallback(() => {
    const params = new URLSearchParams({ page: String(pageNo), size: '20' })
    if (status) params.set('status', status)
    api<PageResponse<Pickup>>(`/api/v1/pickups?${params}`)
      .then((p) => { setPage(p); setError('') })
      .catch((e) => setError(errorText(e)))
  }, [status, pageNo])

  useEffect(() => {
    load()
    const t = setInterval(load, 30_000)          // new pledges show up without refreshing
    return () => clearInterval(t)
  }, [load])

  return (
    <div className="card">
      <h1>Hotel pickups</h1>
      <p className="hint">
        A volunteer booked a hotel. The hotel's own staff see the booking and press Accept, which counts the meal as served. If a busy hotel has not accepted, press “Accept for the hotel” and it shows as done on their screen too. Whoever accepts first wins.
      </p>
      <div className="tabs" role="tablist">
        {TABS.map((t) => (
          <button key={t.key} role="tab" aria-selected={status === t.key} className={'tab' + (status === t.key ? ' on' : '')}
                  onClick={() => { setStatus(t.key); setPageNo(0) }}>{t.label}</button>
        ))}
      </div>
      {error && <p className="error">{error}</p>}
      {!page && !error && <p>Loading…</p>}
      {page && page.content.length === 0 && <p className="hint">Nothing here right now.</p>}
      {page?.content.map((p) => <PickupItem key={p.id} p={p} onChanged={load} />)}
      {page && <Pager page={page} onPage={setPageNo} />}
    </div>
  )
}

function PickupItem({ p, onChanged }: { p: Pickup; onChanged: () => void }) {
  const [mode, setMode] = useState<'' | 'complete' | 'cancel'>('')
  const [plates, setPlates] = useState('')
  const [note, setNote] = useState('')
  const [error, setError] = useState('')
  const [busy, setBusy] = useState(false)

  async function call(action: string, body?: unknown) {
    setBusy(true)
    setError('')
    try {
      await api(`/api/v1/pickups/${p.id}/${action}`, { method: 'POST', body: body ?? {} })
      setMode('')
      onChanged()
    } catch (e) {
      setError(errorText(e))
    } finally {
      setBusy(false)
    }
  }

  function complete(e: FormEvent) {
    e.preventDefault()
    const n = Number(plates)
    if (!Number.isInteger(n) || n < 1) { setError('Enter the number of plates served (a whole number).'); return }
    call('complete', { platesServed: n, note: note || undefined })
  }

  const toHotel = `Hello ${p.vendorName}, this is helpsomeone.in. ${p.volunteerName} (${p.volunteerPhone}) will come to collect surplus food from you for about ${p.peopleCount} ${p.peopleCount === 1 ? 'person' : 'people'}. Please keep it ready. Thank you!`
  const toVolunteer = `Hi ${p.volunteerName}, thank you for volunteering with helpsomeone.in! We have informed ${p.vendorName} (${p.vendorLocation}). Please go there and collect food for about ${p.peopleCount} ${p.peopleCount === 1 ? 'person' : 'people'}.`
  const open = p.status === 'PLEDGED' || p.status === 'CONFIRMED'

  return (
    <div className="item">
      <h3>
        {p.vendorName}, {p.areaName}
        <em className={'badge ' + (p.status === 'PLEDGED' ? 'new' : p.status === 'COMPLETED' ? 'on' : p.status === 'CANCELLED' ? 'off' : '')}>
          {PICKUP_LABEL[p.status]}
        </em>
      </h3>
      <ul className="facts">
        <li><GPin /><span>Hotel: {p.vendorLocation}</span><a className="contact" href={`tel:${p.vendorPhone}`}><GPhone />{p.vendorPhone}</a></li>
        <li><GUser /><span>Volunteer: <strong>{p.volunteerName}</strong></span><a className="contact" href={`tel:${p.volunteerPhone}`}><GPhone />{p.volunteerPhone}</a>
          {p.volunteerEmail ? <span className="contact plain"><GMail />{p.volunteerEmail}</span> : null}</li>
        <li><GPeople /><span>For about <strong>{p.peopleCount}</strong> {p.peopleCount === 1 ? 'person' : 'people'}</span></li>
        {p.deliverTo && <li><GRoute /><span>Taking the food to: {p.deliverTo}</span></li>}
      </ul>
      {p.status === 'COMPLETED' && <div><strong>{p.platesServed}</strong> plates served{p.closedBy === 'HOTEL' ? ' · accepted by the hotel' : p.closedBy === 'ADMIN' ? ' · accepted by you' : ''}</div>}
      {p.note && <div className="hint">Note: {p.note}</div>}
      <div className="hint">Asked {formatDateTime(p.createdAt)}{p.closedAt ? ` · closed ${formatDateTime(p.closedAt)}` : ''}</div>

      {error && <p className="error">{error}</p>}

      {open && mode === '' && (
        <div className="actions">
          <a className="btn small" href={whatsappLink(p.vendorPhone, toHotel)} target="_blank" rel="noreferrer">Message hotel on WhatsApp</a>
          {p.status === 'PLEDGED' && (
            <button className="btn small primary" disabled={busy} onClick={() => call('confirm')}>I informed the hotel</button>
          )}
          <a className="btn small" href={whatsappLink(p.volunteerPhone, toVolunteer)} target="_blank" rel="noreferrer">Message volunteer</a>
          <button className="btn small primary" disabled={busy} onClick={() => call('accept')}>Accept for the hotel</button>
          <button className="btn small warm" onClick={() => setMode('complete')}>Different number of plates</button>
          <button className="btn small" onClick={() => setMode('cancel')}>Cancel</button>
        </div>
      )}

      {open && mode === 'complete' && (
        <form className="inline-form" onSubmit={complete}>
          <label>Plates served
            <input type="number" min="1" step="1" value={plates} onChange={(e) => setPlates(e.target.value)} required autoFocus />
          </label>
          <label>Note (optional)
            <input value={note} onChange={(e) => setNote(e.target.value)} maxLength={500} />
          </label>
          <button className="btn small primary" disabled={busy}>Save</button>
          <button type="button" className="btn small" onClick={() => setMode('')}>Back</button>
          <span className="hint">Do not also enter these meals on a food request.</span>
        </form>
      )}

      {open && mode === 'cancel' && (
        <form className="inline-form" onSubmit={(e) => { e.preventDefault(); call('cancel', { note: note || undefined }) }}>
          <label>Reason (optional)
            <input value={note} onChange={(e) => setNote(e.target.value)} maxLength={500} autoFocus />
          </label>
          <button className="btn small primary" disabled={busy}>Cancel this pickup</button>
          <button type="button" className="btn small" onClick={() => setMode('')}>Back</button>
        </form>
      )}

      {!open && (
        <div className="actions">
          <button className="btn small" disabled={busy} onClick={() => call('reopen')}>Reopen</button>
        </div>
      )}
    </div>
  )
}
