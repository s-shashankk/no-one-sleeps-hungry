import { useCallback, useEffect, useRef, useState } from 'react'
import { Link } from 'react-router-dom'
import { api, PageResponse } from '../api'
import { useAuth } from '../auth'
import { GCheck, GClock, GPhone, GRoute } from '../components/Glyphs'
import Pager from '../components/Pager'
import { errorText, formatDateTime } from '../format'
import { HotelPickup, HotelSummary } from '../types'

/**
 * Hotel staff: who is coming to collect food. One big Accept button per person.
 * Accepting means "we gave the food", and the meal counts as served. The helpsomeone.in team can also accept for a busy hotel.
 */
export default function HotelHome() {
  const { me } = useAuth()
  const [summary, setSummary] = useState<HotelSummary | null>(null)
  const [tab, setTab] = useState<'open' | 'all'>('open')
  const [pageNo, setPageNo] = useState(0)
  const [page, setPage] = useState<PageResponse<HotelPickup> | null>(null)
  const [error, setError] = useState('')
  const [fresh, setFresh] = useState(false)
  const lastWaiting = useRef<number | null>(null)

  const load = useCallback(() => {
    api<HotelSummary>('/api/v1/hotel/summary').then((s) => {
      if (lastWaiting.current !== null && s.waiting > lastWaiting.current) setFresh(true)
      lastWaiting.current = s.waiting
      setSummary(s)
    }).catch((e) => setError(errorText(e)))
    api<PageResponse<HotelPickup>>(`/api/v1/hotel/pickups?open=${tab === 'open'}&page=${pageNo}&size=20`)
      .then((p) => { setPage(p); setError('') })
      .catch((e) => setError(errorText(e)))
  }, [tab, pageNo])

  useEffect(() => {
    load()
    const t = setInterval(load, 15_000)          // a new booking shows up on its own
    return () => clearInterval(t)
  }, [load])

  const waiting = summary?.waiting ?? 0
  useEffect(() => {
    document.title = waiting > 0 ? `(${waiting}) Someone is coming - helpsomeone.in` : 'helpsomeone.in hotel'
    return () => { document.title = 'helpsomeone.in - good food, shared before it is wasted' }
  }, [waiting])

  return (
    <div className="narrow">
      <section className="panel">
        <h1>{summary?.hotelName ?? 'Your hotel'}</h1>
        <p>Hello {me?.fullName.split(' ')[0]} 🙏 Thank you for sharing your kitchen's food with people who have nothing. May it come back to you as blessings.</p>
        {summary && (
          <>
            <div className="panel-stats">
              <div className={'pstat' + (summary.waiting > 0 ? ' hot' : '')}>
                <b>{summary.waiting}</b><span>{summary.waiting === 1 ? 'person coming' : 'people coming'}</span>
              </div>
              <div className="pstat">
                <b>{summary.bookedToday}<small> / {summary.dailyLimit}</small></b><span>plates given today</span>
              </div>
            </div>
            <div className="meter" role="img" aria-label={`${summary.bookedToday} of ${summary.dailyLimit} plates given today`}>
              <i style={{ width: `${Math.min(100, Math.round((summary.bookedToday / Math.max(1, summary.dailyLimit)) * 100))}%` }} />
            </div>
          </>
        )}
      </section>

      {fresh && <p className="ok" role="status">🙏 Someone new is coming to collect food. <button className="linklike dark" onClick={() => setFresh(false)}>OK</button></p>}

      <div className="tabs" role="tablist">
        <button role="tab" aria-selected={tab === 'open'} className={'tab' + (tab === 'open' ? ' on' : '')} onClick={() => { setTab('open'); setPageNo(0) }}>Coming to collect</button>
        <button role="tab" aria-selected={tab === 'all'} className={'tab' + (tab === 'all' ? ' on' : '')} onClick={() => { setTab('all'); setPageNo(0) }}>History</button>
      </div>

      {error && <p className="error">{error}</p>}
      {!page && !error && <p>Loading…</p>}
      {page && page.content.length === 0 && (
        <p className="hint">{tab === 'open' ? 'Nobody is waiting right now. A booking will appear here the moment someone makes one.' : 'Nothing yet.'}</p>
      )}
      <div className="booking-list">
        {page?.content.map((p) => <Booking key={p.id} p={p} onChanged={load} />)}
      </div>
      {page && <Pager page={page} onPage={setPageNo} />}

      <p className="muted">Something to tell the helpsomeone.in team? <Link to="/feedback">Send feedback or a message</Link>.</p>
    </div>
  )
}

function Booking({ p, onChanged }: { p: HotelPickup; onChanged: () => void }) {
  const open = p.status === 'PLEDGED' || p.status === 'CONFIRMED'
  const [declining, setDeclining] = useState(false)
  const [note, setNote] = useState('')
  const [error, setError] = useState('')
  const [busy, setBusy] = useState(false)

  async function call(action: string, body: unknown) {
    setBusy(true); setError('')
    try {
      await api(`/api/v1/hotel/pickups/${p.id}/${action}`, { method: 'POST', body })
      setDeclining(false)
      onChanged()
    } catch (e) {
      setError(errorText(e))
    } finally {
      setBusy(false)
    }
  }

  const state = p.status === 'COMPLETED' ? 'done' : p.status === 'CANCELLED' ? 'cancelled' : 'waiting'

  return (
    <article className={'booking ' + state}>
      <header>
        <span className="avatar" aria-hidden="true">{p.volunteerName.trim().charAt(0).toUpperCase()}</span>
        <div className="person">
          <h3>{p.volunteerName}</h3>
          <a className="contact" href={`tel:${p.volunteerPhone}`}><GPhone />{p.volunteerPhone}</a>
        </div>
        <span className="people"><b>{p.peopleCount}</b>{p.peopleCount === 1 ? 'person' : 'people'}</span>
      </header>

      {p.deliverTo && <p className="to"><GRoute />Taking the food to: {p.deliverTo}</p>}
      <p className="when"><GClock />Booked {formatDateTime(p.createdAt)}</p>

      {state === 'done' && (
        <p className="result ok-line"><GCheck />Food given{p.closedBy === 'ADMIN' ? ' (accepted for you by the helpsomeone.in team)' : ''} · {formatDateTime(p.closedAt)}</p>
      )}
      {state === 'cancelled' && <p className="result off-line">Cancelled</p>}

      {error && <p className="error">{error}</p>}

      {open && !declining && (
        <div className="booking-actions">
          <button className="btn primary big" disabled={busy} onClick={() => call('accept', {})}><GCheck />{busy ? 'Saving…' : 'Accept: food given'}</button>
          <button className="btn small" onClick={() => setDeclining(true)}>Cannot give today</button>
        </div>
      )}

      {open && declining && (
        <form className="inline-form" onSubmit={(e) => { e.preventDefault(); call('decline', { note: note || undefined }) }}>
          <label>Reason (optional, the helpsomeone.in team will see it)
            <input value={note} onChange={(e) => setNote(e.target.value)} maxLength={500} autoFocus />
          </label>
          <button className="btn small primary" disabled={busy}>Cancel this booking</button>
          <button type="button" className="btn small" onClick={() => setDeclining(false)}>Back</button>
        </form>
      )}
    </article>
  )
}
