import { useCallback, useEffect, useState } from 'react'
import { api, PageResponse } from '../../api'
import Pager from '../../components/Pager'
import { errorText, formatDateTime, whatsappLink } from '../../format'
import type { JoinRequest, JoinStatus } from '../../types'

const TABS: { key: JoinStatus | ''; label: string }[] = [
  { key: 'NEW', label: 'New' },
  { key: 'CONTACTED', label: 'Contacted' },
  { key: '', label: 'All' },
]

/** People who asked to join the community from the public site. */
export default function JoinRequests() {
  const [status, setStatus] = useState<JoinStatus | ''>('NEW')
  const [pageNo, setPageNo] = useState(0)
  const [page, setPage] = useState<PageResponse<JoinRequest> | null>(null)
  const [error, setError] = useState('')

  const load = useCallback(() => {
    const params = new URLSearchParams({ page: String(pageNo), size: '20' })
    if (status) params.set('status', status)
    api<PageResponse<JoinRequest>>(`/api/v1/join-requests?${params}`)
      .then((p) => { setPage(p); setError('') })
      .catch((e) => setError(errorText(e)))
  }, [status, pageNo])

  useEffect(() => {
    load()
    const t = setInterval(load, 30_000)
    return () => clearInterval(t)
  }, [load])

  async function act(id: number, action: 'contacted' | 'reopen') {
    try {
      await api(`/api/v1/join-requests/${id}/${action}`, { method: 'POST', body: {} })
      load()
    } catch (e) {
      setError(errorText(e))
    }
  }

  return (
    <div className="card">
      <h1>Join requests</h1>
      <p className="hint">People who want to be part of the community. Call them, then mark them as contacted.</p>
      <div className="tabs" role="tablist">
        {TABS.map((t) => (
          <button key={t.key} role="tab" aria-selected={status === t.key} className={'tab' + (status === t.key ? ' on' : '')}
                  onClick={() => { setStatus(t.key); setPageNo(0) }}>{t.label}</button>
        ))}
      </div>
      {error && <p className="error">{error}</p>}
      {!page && !error && <p>Loading…</p>}
      {page && page.content.length === 0 && <p className="hint">Nothing here right now.</p>}
      {page?.content.map((j) => (
        <div className="item" key={j.id}>
          <h3>
            {j.name}
            {j.status === 'NEW' ? <em className="badge new">New</em> : <em className="badge on">Contacted</em>}
          </h3>
          <div>
            📞 <a href={`tel:${j.phoneNumber}`}>{j.phoneNumber}</a> ·{' '}
            <a href={whatsappLink(j.phoneNumber, `Hi ${j.name}, this is helpsomeone.in. Thank you for wanting to join us!`)} target="_blank" rel="noreferrer">WhatsApp</a>
          </div>
          {j.email && <div>✉️ <a href={`mailto:${j.email}`}>{j.email}</a></div>}
          {j.location && <div>📍 {j.location}</div>}
          {j.message && <div>“{j.message}”</div>}
          <div className="hint">Asked {formatDateTime(j.createdAt)}{j.contactedAt ? ` · contacted ${formatDateTime(j.contactedAt)}` : ''}</div>
          <div className="actions">
            {j.status === 'NEW'
              ? <button className="btn small primary" onClick={() => act(j.id, 'contacted')}>Mark as contacted</button>
              : <button className="btn small" onClick={() => act(j.id, 'reopen')}>Mark as new</button>}
          </div>
        </div>
      ))}
      {page && <Pager page={page} onPage={setPageNo} />}
    </div>
  )
}
