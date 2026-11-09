import { useCallback, useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { api, PageResponse } from '../api'
import { useAuth } from '../auth'
import { GClock, GPeople, GPin, GRoute } from '../components/Glyphs'
import Pager from '../components/Pager'
import { errorText, formatDateTime } from '../format'
import { MySummary, VolunteerPickup } from '../types'

/** A volunteer's own page: totals, every pickup they asked for, and a way to change their password. */
export default function MyDeliveries() {
  const { me } = useAuth()
  const [summary, setSummary] = useState<MySummary | null>(null)
  const [pageNo, setPageNo] = useState(0)
  const [page, setPage] = useState<PageResponse<VolunteerPickup> | null>(null)
  const [error, setError] = useState('')

  const load = useCallback(() => {
    api<MySummary>('/api/v1/me/summary').then(setSummary).catch((e) => setError(errorText(e)))
    api<PageResponse<VolunteerPickup>>(`/api/v1/me/pickups?page=${pageNo}&size=10`)
      .then((p) => { setPage(p); setError('') })
      .catch((e) => setError(errorText(e)))
  }, [pageNo])

  useEffect(() => { load() }, [load])

  async function cancel(id: number) {
    try {
      await api(`/api/v1/me/pickups/${id}/cancel`, { method: 'POST', body: {} })
      load()
    } catch (e) {
      setError(errorText(e))
    }
  }

  return (
    <div className="narrow">
      <section className="panel">
        <h1>Hello, {me?.fullName.split(' ')[0]} 🙏</h1>
        <p>Every number below is a person who went to sleep with a full stomach because of you. Thank you.</p>
        {summary && (
          <div className="panel-stats four">
            <div className="pstat"><b>{summary.deliveries}</b><span>{summary.deliveries === 1 ? 'Delivery done' : 'Deliveries done'}</span></div>
            <div className="pstat"><b>{summary.platesServed.toLocaleString('en-IN')}</b><span>Plates served</span></div>
            <div className="pstat"><b>{summary.peopleReported.toLocaleString('en-IN')}</b><span>People you planned for</span></div>
            <div className={'pstat' + (summary.inProgress > 0 ? ' hot' : '')}><b>{summary.inProgress}</b><span>In progress</span></div>
          </div>
        )}
        <Link className="btn warm big" to="/deliver">Feed someone today</Link>
      </section>

      <div className="card">
        <h2>My pickups</h2>
        {error && <p className="error">{error}</p>}
        {!page && !error && <p>Loading…</p>}
        {page && page.content.length === 0 && <p className="hint">Nothing yet. When you collect food for someone, it will be remembered here.</p>}
        {page?.content.map((p) => {
          const open = p.status === 'PLEDGED' || p.status === 'CONFIRMED'
          const state = p.status === 'COMPLETED' ? 'done' : p.status === 'CANCELLED' ? 'cancelled' : 'waiting'
          const label = p.status === 'COMPLETED' ? 'Food collected' : p.status === 'CANCELLED' ? 'Cancelled' : 'Booked: go and collect'
          return (
            <article className={'mine-card ' + state} key={p.id}>
              <h3>
                {p.hotelName}
                <em className={'badge ' + (state === 'done' ? 'on' : state === 'cancelled' ? 'off' : 'new')}>{label}</em>
              </h3>
              <ul className="facts">
                <li><GPin /><span>{p.hotelLocation}, {p.areaName}</span></li>
                <li><GPeople /><span>For about <strong>{p.peopleCount}</strong> {p.peopleCount === 1 ? 'person' : 'people'}
                  {p.status === 'COMPLETED' && p.platesServed != null && <> · <strong>{p.platesServed}</strong> plates</>}</span></li>
                {p.deliverTo && <li><GRoute /><span>{p.deliverTo}</span></li>}
                <li><GClock /><span>Booked {formatDateTime(p.createdAt)}{p.closedAt ? ` · closed ${formatDateTime(p.closedAt)}` : ''}</span></li>
              </ul>
              {open && (
                <>
                  <p className="hint">The hotel can see your booking. Just tell them your name when you arrive.</p>
                  <div className="actions">
                    <a className="btn map" target="_blank" rel="noreferrer"
                       href={p.hotelMapUrl || `https://www.google.com/maps/search/?api=1&query=${encodeURIComponent(p.hotelName + ' ' + p.hotelLocation + ' Bengaluru')}`}><GPin />Open in Maps</a>
                    <button className="btn small" onClick={() => cancel(p.id)}>I cannot go, cancel</button>
                  </div>
                </>
              )}
            </article>
          )
        })}
        {page && <Pager page={page} onPage={setPageNo} />}
      </div>

    </div>
  )
}
