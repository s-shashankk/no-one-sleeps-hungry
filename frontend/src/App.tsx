import { ReactNode, useEffect, useState } from 'react'
import { Link, NavLink, Route, Routes, useLocation, useNavigate } from 'react-router-dom'
import { api } from './api'
import { useAuth } from './auth'
import { useTexts } from './texts'
import EditText from './pages/admin/EditText'
import RequireAuth from './RequireAuth'
import HotelHome from './pages/HotelHome'
import Feedback from './pages/Feedback'
import Messages from './pages/admin/Messages'
import Home from './pages/Home'
import About from './pages/About'
import Privacy from './pages/Privacy'
import Login from './pages/Login'
import ReportHunger from './pages/ReportHunger'
import SurplusFood from './pages/SurplusFood'
import Deliver from './pages/Deliver'
import Register from './pages/Register'
import MyDeliveries from './pages/MyDeliveries'
import Account from './pages/Account'
import Volunteers from './pages/admin/Volunteers'
import Join from './pages/Join'
import Impact from './pages/Impact'
import AdminHome from './pages/admin/AdminHome'
import Requests from './pages/admin/Requests'
import RequestDetail from './pages/admin/RequestDetail'
import Vendors from './pages/admin/Vendors'
import Coworkers from './pages/admin/Coworkers'
import Pickups from './pages/admin/Pickups'
import JoinRequests from './pages/admin/JoinRequests'
import Areas from './pages/admin/Areas'
import type { DashboardSummary } from './types'

const navClass = ({ isActive }: { isActive: boolean }) => (isActive ? 'on' : '')

function Badge({ n }: { n: number }) {
  return n > 0 ? <span className="count" aria-label={`${n} waiting`}>{n}</span> : null
}

/** Admin menu. Polls the dashboard so new pickups, requests and join requests show up as badges (and in the tab title). */
function AdminNav({ onSignOut }: { onSignOut: () => void }) {
  const [s, setS] = useState<DashboardSummary | null>(null)

  useEffect(() => {
    const load = () => api<DashboardSummary>('/api/v1/dashboard').then(setS).catch(() => undefined)
    load()
    const t = setInterval(load, 30_000)
    return () => clearInterval(t)
  }, [])

  const waiting = s ? s.newPickups + s.pendingRequests + s.newJoinRequests + s.newMessages : 0
  useEffect(() => {
    document.title = waiting > 0 ? `(${waiting}) helpsomeone.in admin` : 'helpsomeone.in admin'
    return () => { document.title = 'helpsomeone.in - good food, shared before it is wasted' }
  }, [waiting])

  return (
    <nav aria-label="Admin">
      <NavLink to="/" end className={navClass}>Website</NavLink>
      <NavLink to="/admin" end className={navClass}>Home</NavLink>
      <NavLink to="/admin/pickups" className={navClass}>Pickups<Badge n={s?.newPickups ?? 0} /></NavLink>
      <NavLink to="/admin/messages" className={navClass}>Messages<Badge n={s?.newMessages ?? 0} /></NavLink>
      <NavLink to="/admin/requests" className={navClass}>Requests<Badge n={s?.pendingRequests ?? 0} /></NavLink>
      <NavLink to="/admin/joins" className={navClass}>Joining<Badge n={s?.newJoinRequests ?? 0} /></NavLink>
      <NavLink to="/admin/volunteers" className={navClass}>Volunteers</NavLink>
      <NavLink to="/admin/vendors" className={navClass}>Hotels</NavLink>
      <NavLink to="/admin/areas" className={navClass}>Areas</NavLink>
      <NavLink to="/admin/coworkers" className={navClass}>Coworkers</NavLink>
      <NavLink to="/admin/texts" className={navClass}>Site text</NavLink>
      <button className="linklike" onClick={onSignOut}>Sign out</button>
    </nav>
  )
}

export default function App() {
  const { me, logout } = useAuth()
  const navigate = useNavigate()
  const { pathname } = useLocation()
  const [menuOpen, setMenuOpen] = useState(false)
  const { t } = useTexts()

  useEffect(() => { window.scrollTo(0, 0); setMenuOpen(false) }, [pathname])

  async function signOut() {
    await logout()
    navigate('/')
  }

  const small = (page: ReactNode) => <div className="narrow">{page}</div>
  const isHome = pathname === '/'

  return (
    <>
      <a className="skip" href="#main">Skip to content</a>
      <header className="site-header">
        <Link to="/" className="brand"><img src="/img/logo.svg" alt="" width="34" height="34" />helpsomeone.in</Link>
        <button type="button" className="menu-btn" aria-label={menuOpen ? 'Close menu' : 'Open menu'}
                aria-expanded={menuOpen} aria-controls="site-nav" onClick={() => setMenuOpen(!menuOpen)}>
          <span /><span /><span />
        </button>
        <div id="site-nav" className={'nav-wrap' + (menuOpen ? ' open' : '')}>
        {me?.role === 'ADMIN' ? (
          <AdminNav onSignOut={signOut} />
        ) : me?.role === 'HOTEL' ? (
          <nav aria-label="Hotel">
            <NavLink to="/" end className={navClass}>Home</NavLink>
            <NavLink to="/hotel" className={navClass}>Today</NavLink>
            <NavLink to="/feedback" className={navClass}>Feedback</NavLink>
            <NavLink to="/account" className={navClass}>Account</NavLink>
            <button className="linklike" onClick={signOut}>Sign out</button>
          </nav>
        ) : me ? (
          <nav aria-label="Volunteer">
            <NavLink to="/" end className={navClass}>Home</NavLink>
            <NavLink to="/deliver" className={navClass}>Deliver food</NavLink>
            <NavLink to="/me" className={navClass}>My deliveries</NavLink>
            <NavLink to="/impact" className={navClass}>Impact</NavLink>
            <NavLink to="/feedback" className={navClass}>Feedback</NavLink>
            <NavLink to="/account" className={navClass}>Account</NavLink>
            <button className="linklike" onClick={signOut}>Sign out</button>
          </nav>
        ) : (
          <nav aria-label="Main">
            <NavLink to="/" end className={navClass}>Home</NavLink>
            <NavLink to="/deliver" className={navClass}>Deliver food</NavLink>
            <NavLink to="/report-hunger" className={navClass}>Need food</NavLink>
            <NavLink to="/impact" className={navClass}>Impact</NavLink>
            <NavLink to="/about" className={navClass}>About</NavLink>
            <NavLink to="/login" className={navClass}>Sign in</NavLink>
            <Link to="/join" className="pill">Join us</Link>
          </nav>
        )}
        </div>
      </header>
      <main id="main" className={isHome ? undefined : pathname.startsWith('/admin') ? 'container' : 'container wide'}>
        <Routes>
          <Route path="/" element={<Home />} />
          <Route path="/about" element={<About />} />
          <Route path="/privacy" element={small(<Privacy />)} />
          <Route path="/login" element={small(<Login />)} />
          <Route path="/register" element={small(<Register />)} />
          <Route path="/account" element={<RequireAuth roles={['VOLUNTEER', 'HOTEL']}>{small(<Account />)}</RequireAuth>} />
          <Route path="/feedback" element={<RequireAuth roles={['VOLUNTEER', 'HOTEL']}><Feedback /></RequireAuth>} />
          <Route path="/hotel" element={<RequireAuth role="HOTEL"><HotelHome /></RequireAuth>} />
          <Route path="/me" element={<RequireAuth role="VOLUNTEER"><MyDeliveries /></RequireAuth>} />
          <Route path="/surplus-food" element={small(<SurplusFood />)} />
          <Route path="/report-hunger" element={small(<ReportHunger />)} />
          <Route path="/deliver" element={<Deliver />} />
          <Route path="/join" element={<Join />} />
          <Route path="/impact" element={<Impact />} />
          <Route path="/admin" element={<RequireAuth><AdminHome /></RequireAuth>} />
          <Route path="/admin/pickups" element={<RequireAuth><Pickups /></RequireAuth>} />
          <Route path="/admin/requests" element={<RequireAuth><Requests /></RequireAuth>} />
          <Route path="/admin/requests/:id" element={<RequireAuth><RequestDetail /></RequireAuth>} />
          <Route path="/admin/joins" element={<RequireAuth><JoinRequests /></RequireAuth>} />
          <Route path="/admin/messages" element={<RequireAuth><Messages /></RequireAuth>} />
          <Route path="/admin/volunteers" element={<RequireAuth><Volunteers /></RequireAuth>} />
          <Route path="/admin/vendors" element={<RequireAuth><Vendors /></RequireAuth>} />
          <Route path="/admin/areas" element={<RequireAuth><Areas /></RequireAuth>} />
          <Route path="/admin/coworkers" element={<RequireAuth><Coworkers /></RequireAuth>} />
          <Route path="/admin/texts" element={<RequireAuth><EditText /></RequireAuth>} />
          <Route path="*" element={<p>Page not found. <Link to="/">Go home</Link></p>} />
        </Routes>
      </main>
      <footer className="site-footer">
        <div className="inner">
          <strong>helpsomeone.in</strong>
          <span>{t('footer.tagline')}</span>
          <span>
            <Link to="/join">Join us</Link> · <Link to="/impact">Impact</Link> · <Link to="/privacy">Privacy</Link>
            {!me && <> · <Link to="/login">Sign in</Link></>}
          </span>
          <span className="muted" style={{ color: '#9cc0a9' }}>Donated surplus food is handled on a best-effort basis by volunteers.</span>
        </div>
      </footer>
    </>
  )
}
