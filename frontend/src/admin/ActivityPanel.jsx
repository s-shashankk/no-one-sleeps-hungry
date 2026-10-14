import { useEffect, useState } from 'react'
import { api } from '../api.js'

export default function ActivityPanel({ onUnauthorized }) {
  const [rows, setRows] = useState(null)
  const [error, setError] = useState('')

  useEffect(() => {
    api('/api/admin/activity').then(setRows).catch((err) => {
      if (err.status === 401) return onUnauthorized()
      setRows([])
      setError(err.message)
    })
  }, [onUnauthorized])

  return (
    <>
      <p className="lead">The latest 200 changes, newest first.</p>
      {error && <p className="error banner">{error}</p>}
      {rows === null && <p>Loading…</p>}
      {rows?.length === 0 && !error && <p className="lead">Nothing recorded yet.</p>}
      {rows?.map((r) => (
        <article key={r.id} className="card record">
          <header>
            <strong>{r.entity.replace('_', ' ')} #{r.entityId ?? '—'} · {r.action.replace('_', ' ')}</strong>
            <small>{new Date(r.createdAt).toLocaleString()}</small>
          </header>
          {r.details && <p>{r.details}</p>}
          <p className="hint">by {r.actor}</p>
        </article>
      ))}
    </>
  )
}
