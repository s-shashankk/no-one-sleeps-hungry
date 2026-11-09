import { useCallback, useEffect, useState } from 'react'
import { api, PageResponse } from '../../api'
import { GChat, GCheck, GPhone } from '../../components/Glyphs'
import Pager from '../../components/Pager'
import { errorText, formatDateTime, whatsappLink } from '../../format'
import type { AdminMessage } from '../../types'

const TABS: { key: '' | 'NEW' | 'DONE'; label: string }[] = [
  { key: 'NEW', label: 'New' },
  { key: 'DONE', label: 'Done' },
  { key: '', label: 'All' },
]

/** Feedback and messages that volunteers and hotel staff sent to you. */
export default function Messages() {
  const [status, setStatus] = useState<'' | 'NEW' | 'DONE'>('NEW')
  const [pageNo, setPageNo] = useState(0)
  const [page, setPage] = useState<PageResponse<AdminMessage> | null>(null)
  const [error, setError] = useState('')

  const load = useCallback(() => {
    const params = new URLSearchParams({ page: String(pageNo), size: '20' })
    if (status) params.set('status', status)
    api<PageResponse<AdminMessage>>(`/api/v1/messages?${params}`)
      .then((p) => { setPage(p); setError('') })
      .catch((e) => setError(errorText(e)))
  }, [status, pageNo])

  useEffect(() => {
    load()
    const t = setInterval(load, 30_000)
    return () => clearInterval(t)
  }, [load])

  async function act(id: number, action: 'done' | 'reopen') {
    try {
      await api(`/api/v1/messages/${id}/${action}`, { method: 'POST', body: {} })
      load()
    } catch (e) {
      setError(errorText(e))
    }
  }

  const waiting = status === 'NEW' && page ? page.totalElements : null

  return (
    <div className="inbox">
      <header className="page-head">
        <h1>Feedback and messages</h1>
        <p className="hint">Notes from volunteers and hotel staff. Answer by phone or WhatsApp, then mark them done.</p>
      </header>
      <div className="mtabs" role="tablist">
        {TABS.map((t) => (
          <button key={t.key} role="tab" aria-selected={status === t.key} className={status === t.key ? 'on' : ''}
                  onClick={() => { setStatus(t.key); setPageNo(0) }}>
            {t.label}{t.key === 'NEW' && waiting ? <span className="n">{waiting}</span> : null}
          </button>
        ))}
      </div>
      {error && <p className="error">{error}</p>}
      {!page && !error && <p className="muted">Loading…</p>}
      {page && page.content.length === 0 && (
        <div className="mempty">
          <strong>{status === 'NEW' ? 'All caught up' : 'Nothing here yet'}</strong>
          <span>{status === 'NEW' ? 'New feedback and messages will appear here.' : 'Nothing to show in this list.'}</span>
        </div>
      )}
      {page && page.content.length > 0 && (
        <ul className="mlist">
          {page.content.map((m) => (
            <li className={'mrow' + (m.status === 'NEW' ? ' new' : '')} key={m.id}>
              <div className="mtop">
                <span className="mname">{m.senderName}</span>
                <span className="mmeta">
                  {m.senderRole === 'HOTEL' ? `Hotel staff${m.hotelName ? `, ${m.hotelName}` : ''}` : 'Volunteer'} · {formatDateTime(m.createdAt)}
                </span>
                <span className="mkind">
                  {m.rating ? <span className="mstars" aria-label={`${m.rating} of 5 stars`}>{'★'.repeat(m.rating)}<span className="dim">{'★'.repeat(5 - m.rating)}</span></span> : null}
                  {m.kind === 'FEEDBACK' ? 'Feedback' : 'Message'}
                </span>
              </div>
              <p className="mbody">{m.body}</p>
              <div className="mactions">
                <a href={`tel:${m.senderPhone}`}><GPhone />Call</a>
                <a href={whatsappLink(m.senderPhone, `Hello ${m.senderName.split(' ')[0]}, this is helpsomeone.in. Thank you for your message. `)} target="_blank" rel="noreferrer"><GChat />WhatsApp</a>
                {m.status === 'NEW'
                  ? <button className="mdone" onClick={() => act(m.id, 'done')}><GCheck />Mark done</button>
                  : <button className="mreopen" onClick={() => act(m.id, 'reopen')}>Reopen</button>}
              </div>
            </li>
          ))}
        </ul>
      )}
      {page && <Pager page={page} onPage={setPageNo} />}
    </div>
  )
}
