import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { api } from '../../api'
import { useAuth } from '../../auth'
import { errorText } from '../../format'
import type { DashboardSummary } from '../../types'

/** The four numbers, each linking to the list behind it. */
export default function AdminHome() {
  const { me } = useAuth()
  const [s, setS] = useState<DashboardSummary | null>(null)
  const [error, setError] = useState('')

  useEffect(() => {
    const load = () => api<DashboardSummary>('/api/v1/dashboard')
      .then((x) => { setS(x); setError('') })
      .catch((e) => setError(errorText(e)))
    load()
    const t = setInterval(load, 60_000)
    return () => clearInterval(t)
  }, [])

  return (
    <>
      <div className="card">
        <h1>Hello{me ? `, ${me.fullName}` : ''}</h1>
        {error && <p className="error">{error}</p>}
        {!s && !error && <p>Loading…</p>}
        {s && (
          <div className="tiles">
            <Link className="tile" to="/admin/requests?status=COMPLETED&type=NEED_FOOD">
              <b>{s.totalMealsDelivered}</b><span>Total meals delivered</span>
            </Link>
            <Link className="tile" to="/admin/vendors">
              <b>{s.activeVendors}</b><span>Active vendors</span>
            </Link>
            <Link className="tile" to="/admin/coworkers">
              <b>{s.activeCoworkers}</b><span>Active coworkers</span>
            </Link>
            <Link className={'tile' + (s.pendingRequests > 0 ? ' attention' : '')} to="/admin/requests?status=PENDING">
              <b>{s.pendingRequests}</b><span>Pending requests</span>
            </Link>
          </div>
        )}
      </div>
      <div className="actions">
        <Link className="btn primary" to="/admin/requests?status=PENDING">Open pending requests</Link>
        <Link className="btn" to="/admin/requests?add=1">+ Add a request</Link>
      </div>
    </>
  )
}
