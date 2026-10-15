import { Link, Route, Routes, useNavigate } from 'react-router-dom'
import { useAuth } from './auth'
import RequireAuth from './RequireAuth'
import Home from './pages/Home'
import About from './pages/About'
import Privacy from './pages/Privacy'
import Login from './pages/Login'
import ReportHunger from './pages/ReportHunger'
import SurplusFood from './pages/SurplusFood'
import AdminHome from './pages/admin/AdminHome'
import Requests from './pages/admin/Requests'
import RequestDetail from './pages/admin/RequestDetail'
import Vendors from './pages/admin/Vendors'
import Coworkers from './pages/admin/Coworkers'

export default function App() {
  const { me, logout } = useAuth()
  const navigate = useNavigate()

  async function signOut() {
    await logout()
    navigate('/')
  }

  return (
    <>
      <header className="site-header">
        <Link to={me ? '/admin' : '/'} className="brand">🍲 MealUp</Link>
        <nav>
          {me ? (
            <>
              <Link to="/admin/requests">Requests</Link>
              <Link to="/admin/vendors">Vendors</Link>
              <Link to="/admin/coworkers">Coworkers</Link>
              <button className="linklike" onClick={signOut}>Sign out</button>
            </>
          ) : (
            <>
              <Link to="/about">About</Link>
              {me === null && <Link to="/login">Admin login</Link>}
            </>
          )}
        </nav>
      </header>
      <main className="container">
        <Routes>
          <Route path="/" element={<Home />} />
          <Route path="/about" element={<About />} />
          <Route path="/privacy" element={<Privacy />} />
          <Route path="/login" element={<Login />} />
          <Route path="/surplus-food" element={<SurplusFood />} />
          <Route path="/report-hunger" element={<ReportHunger />} />
          <Route path="/admin" element={<RequireAuth><AdminHome /></RequireAuth>} />
          <Route path="/admin/requests" element={<RequireAuth><Requests /></RequireAuth>} />
          <Route path="/admin/requests/:id" element={<RequireAuth><RequestDetail /></RequireAuth>} />
          <Route path="/admin/vendors" element={<RequireAuth><Vendors /></RequireAuth>} />
          <Route path="/admin/coworkers" element={<RequireAuth><Coworkers /></RequireAuth>} />
          <Route path="*" element={<p>Page not found. <Link to="/">Go home</Link></p>} />
        </Routes>
      </main>
      <footer className="site-footer">
        Donated surplus food is handled on a best-effort basis by volunteers. <Link to="/privacy">Privacy</Link>
      </footer>
    </>
  )
}
