import { Link } from 'react-router-dom'

export default function Home() {
  return (
    <section className="hero">
      <h1>No one should go hungry while good food goes to waste.</h1>
      <p>
        MealUp helps a community collect safe surplus food from hotels, restaurants and events, and get it to
        people who need it.
      </p>
      <div className="cta">
        <Link className="btn primary big" to="/report-hunger">🤝 Report people who need food</Link>
        <Link className="btn big" to="/surplus-food">🍛 Inform us about surplus food</Link>
      </div>
    </section>
  )
}
