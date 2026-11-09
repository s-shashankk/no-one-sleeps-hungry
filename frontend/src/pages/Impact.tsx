import { Link } from 'react-router-dom'
import CountUp from '../components/CountUp'
import { useImpact } from '../impact'

/** The public dashboard: counts only, never names or phone numbers of people. */
export default function Impact() {
  const { impact, failed } = useImpact()

  return (
    <div className="narrow">
      <h1>Our impact</h1>
      <p className="lead">Every number here is entered by our team after the food has reached people.</p>
      {failed && <p className="error">The numbers could not be loaded. Please try again in a moment.</p>}
      {!impact && !failed && <p>Loading…</p>}
      {impact && (
        <>
          <div className="card">
            <p className="big-number"><CountUp value={impact.mealsServed} /></p>
            <p className="lead">meals served so far</p>
          </div>
          <div className="ledger" style={{ borderRadius: 14 }}>
            <div className="ledger-inner">
              <div><b>{impact.cities}</b><span>{impact.cities === 1 ? 'city' : 'cities'}</span></div>
              <div><b>{impact.areasOpen}</b><span>{impact.areasOpen === 1 ? 'area open' : 'areas open'}</span></div>
              <div><b>{impact.hotelPartners}</b><span>hotel partners</span></div>
              <div><b>{impact.pickupsCompleted}</b><span>hotel pickups done</span></div>
            </div>
          </div>
          <div className="card">
            <h2>By area</h2>
            <div className="table-wrap">
              <table className="plain">
                <thead>
                  <tr><th>Area</th><th>Status</th><th className="num">Hotels</th><th className="num">Meals from hotel pickups</th></tr>
                </thead>
                <tbody>
                  {impact.areas.map((a) => (
                    <tr key={a.id}>
                      <td>{a.name}, {a.city}</td>
                      <td><span className={'chip ' + (a.open ? 'open' : 'soon')}>{a.open ? 'Open' : 'Coming soon'}</span></td>
                      <td className="num">{a.hotels}</td>
                      <td className="num">{a.pickupMeals.toLocaleString('en-IN')}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          </div>
          <p><Link className="btn primary" to="/join">Help us grow these numbers</Link></p>
        </>
      )}
    </div>
  )
}
