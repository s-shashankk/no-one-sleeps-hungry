import { Link, Route, Routes } from 'react-router-dom'
import Home from './pages/Home.jsx'
import OfferFood from './pages/OfferFood.jsx'
import ReportHunger from './pages/ReportHunger.jsx'
import Join from './pages/Join.jsx'
import About from './pages/About.jsx'
import AdminLogin from './admin/AdminLogin.jsx'
import AdminDashboard from './admin/AdminDashboard.jsx'

export default function App() {
  return (
    <>
      <header className="site-header">
        <Link to="/" className="brand">🍲 No One Sleeps Hungry</Link>
        <nav>
          <Link to="/about">About</Link>
          <Link to="/join">Volunteer</Link>
        </nav>
      </header>
      <main className="container">
        <Routes>
          <Route path="/" element={<Home />} />
          <Route path="/offer-food" element={<OfferFood />} />
          <Route path="/report-hunger" element={<ReportHunger />} />
          <Route path="/join" element={<Join />} />
          <Route path="/about" element={<About />} />
          <Route path="/admin/login" element={<AdminLogin />} />
          <Route path="/admin" element={<AdminDashboard />} />
          <Route path="*" element={<p>Page not found. <Link to="/">Go home</Link></p>} />
        </Routes>
      </main>
      <footer className="site-footer">
        Donated surplus food is handled on a best-effort basis by volunteers.
        {' · '}<Link to="/admin">Admin</Link>
      </footer>
    </>
  )
}
