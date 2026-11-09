import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { api } from '../../api'
import { useAuth } from '../../auth'
import { errorText } from '../../format'
import type { DashboardSummary } from '../../types'

/** What needs attention first, then the totals; each tile links to the list behind it. */
export default function AdminHome() {
  const { me } = useAuth()
  const [s, setS] = useState<DashboardSummary | null>(null)
  const [error, setError] = useState('')

  useEffect(() => {
    const load = () => api<DashboardSummary>('/api/v1/dashboard')
      .then((x) => { setS(x); setError('') })
      .catch((e) => setError(errorText(e)))
    load()
    const t = setInterval(load, 30_000)
    return () => clearInterval(t)
  }, [])

  return (
    <>
      <section className="panel">
        <h1>Hello{me ? `, ${me.fullName}` : ''}</h1>
        <p>{s ? (s.newPickups + s.pendingRequests + s.newMessages + s.newJoinRequests > 0 ? 'A few things are waiting for you.' : 'Nothing is waiting. Every booking and message is handled.') : 'Getting today\'s picture…'}</p>
        <div className="actions">
          <Link className="btn warm" to="/admin/pickups">Open hotel pickups</Link>
          <Link className="btn light" to="/admin/requests?add=1">Add a request</Link>
          <Link className="btn light" to="/admin/texts">Edit site text</Link>
        </div>
      </section>
      <div className="card">
        {error && <p className="error">{error}</p>}
        {!s && !error && <p>Loading…</p>}
        {s && (
          <>
            <h2>Waiting for you</h2>
            <div className="tiles">
              <Link className={'tile' + (s.newPickups > 0 ? ' attention' : '')} to="/admin/pickups">
                <b>{s.newPickups}</b><span>New hotel pickups</span>
              </Link>
              <Link className={'tile' + (s.pendingRequests > 0 ? ' attention' : '')} to="/admin/requests?status=PENDING">
                <b>{s.pendingRequests}</b><span>Pending food requests</span>
              </Link>
              <Link className={'tile' + (s.newMessages > 0 ? ' attention' : '')} to="/admin/messages">
                <b>{s.newMessages}</b><span>New messages and feedback</span>
              </Link>
              <Link className={'tile' + (s.newJoinRequests > 0 ? ' attention' : '')} to="/admin/joins">
                <b>{s.newJoinRequests}</b><span>New join requests</span>
              </Link>
            </div>
            <h2>Totals</h2>
            <div className="tiles">
              <Link className="tile" to="/impact"><b>{s.totalMealsDelivered.toLocaleString('en-IN')}</b><span>Meals served</span></Link>
              <Link className="tile" to="/admin/areas"><b>{s.cities}</b><span>{s.cities === 1 ? 'City' : 'Cities'} ({s.openAreas} open {s.openAreas === 1 ? 'area' : 'areas'})</span></Link>
              <Link className="tile" to="/admin/volunteers"><b>{s.volunteers}</b><span>Volunteers</span></Link>
              <Link className="tile" to="/admin/vendors"><b>{s.activeVendors}</b><span>Hotel partners</span></Link>
              <Link className="tile" to="/admin/coworkers"><b>{s.activeCoworkers}</b><span>Active coworkers</span></Link>
            </div>
          </>
        )}
      </div>
    </>
  )
}
