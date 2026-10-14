import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { api } from '../api.js'

export default function Home() {
  const [stats, setStats] = useState(null)

  useEffect(() => {
    api('/api/public/stats').then(setStats).catch(() => setStats(null))
  }, [])

  return (
    <>
      <section className="hero">
        <h1>No one should sleep hungry.</h1>
        <p>
          Hotels, restaurants and families often have good food left over. Nearby, someone is
          hungry. We connect the two.
        </p>
        <div className="cta">
          <Link className="btn primary big" to="/offer-food">🍛 I have surplus food</Link>
          <Link className="btn big" to="/report-hunger">🤝 Someone needs food</Link>
        </div>
      </section>

      {stats && (
        <section className="stats">
          <div><strong>{stats.offersCompleted}</strong><span>food pickups completed</span></div>
          <div><strong>{stats.reportsServed}</strong><span>hunger reports served</span></div>
          <div><strong>{stats.volunteersApplied}</strong><span>volunteers joined</span></div>
        </section>
      )}

      <section className="card">
        <h2>How it works</h2>
        <ol>
          <li>Food donors tell us when surplus food is available.</li>
          <li>Anyone can tell us where hungry people are.</li>
          <li>A local volunteer coordinator matches the two and arranges delivery.</li>
        </ol>
        <p>Want to help in your area? <Link to="/join">Become a volunteer</Link>.</p>
      </section>
    </>
  )
}
