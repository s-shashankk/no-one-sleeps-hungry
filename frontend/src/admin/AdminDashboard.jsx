import { useCallback, useEffect, useState } from 'react'
import { Navigate, useNavigate } from 'react-router-dom'
import { api, clearLogin, getToken, getUser } from '../api.js'
import AdminList, { KINDS } from './AdminList.jsx'
import CoordinatorsPanel from './CoordinatorsPanel.jsx'
import LocalitiesPanel from './LocalitiesPanel.jsx'
import DeliveriesPanel from './DeliveriesPanel.jsx'
import ActivityPanel from './ActivityPanel.jsx'
import ChangePassword from './ChangePassword.jsx'

const SUPER_ONLY = ['volunteers', 'coordinators', 'localities', 'activity']
const LABELS = { ...Object.fromEntries(Object.entries(KINDS).map(([k, v]) => [k, v.label])),
  deliveries: 'Deliveries', coordinators: 'Coordinators', localities: 'Localities', activity: 'Activity' }

export default function AdminDashboard() {
  const navigate = useNavigate()
  const user = getUser()
  const isSuper = user?.role === 'SUPER_ADMIN'
  const [tab, setTab] = useState('offers')
  const [localities, setLocalities] = useState([])
  const [prefillOfferId, setPrefillOfferId] = useState(null)

  const logout = useCallback(() => {
    clearLogin()
    navigate('/admin/login')
  }, [navigate])

  const loadLocalities = useCallback(async () => {
    try {
      setLocalities(await api('/api/admin/localities'))
    } catch (err) {
      if (err.status === 401) logout()
    }
  }, [logout])

  const signedIn = Boolean(getToken())
  useEffect(() => { if (signedIn) loadLocalities() }, [signedIn, loadLocalities])

  if (!signedIn) return <Navigate to="/admin/login" replace />

  const tabs = ['offers', 'reports', 'deliveries', 'volunteers', 'coordinators', 'localities', 'activity']
    .filter((k) => isSuper || !SUPER_ONLY.includes(k))

  return (
    <>
      <div className="toolbar between">
        <h1>Dashboard</h1>
        <div>
          <small>{user?.name} ({isSuper ? 'Super Admin' : 'Coordinator'})</small>{' '}
          <button className="btn" onClick={logout}>Log out</button>
        </div>
      </div>
      <ChangePassword />
      <div className="tabs" role="tablist">
        {tabs.map((k) => (
          <button key={k} role="tab" aria-selected={tab === k}
            className={tab === k ? 'tab active' : 'tab'} onClick={() => setTab(k)}>
            {LABELS[k]}
          </button>
        ))}
      </div>
      {tab === 'deliveries' && (
        <DeliveriesPanel key={prefillOfferId ?? 'none'} localities={localities}
          prefillOfferId={prefillOfferId} onUnauthorized={logout} />
      )}
      {tab === 'activity' && <ActivityPanel onUnauthorized={logout} />}
      {tab === 'coordinators' && <CoordinatorsPanel localities={localities} onUnauthorized={logout} />}
      {tab === 'localities' && <LocalitiesPanel localities={localities} reload={loadLocalities} />}
      {KINDS[tab] && <AdminList key={tab} kind={tab} localities={localities} isSuper={isSuper} onUnauthorized={logout}
        onCreateDelivery={(id) => { setPrefillOfferId(id); setTab('deliveries') }} />}
    </>
  )
}
