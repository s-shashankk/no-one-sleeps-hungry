import { Link } from 'react-router-dom'
import { useTexts } from '../texts'

export default function About() {
  const { t } = useTexts()
  return (
    <div className="narrow">
      <h1>About helpsomeone.in</h1>
      <div className="card">
        <p>{t('about.intro')}</p>
        <p>{t('about.start')}</p>
        <h2>Who we serve</h2>
        <p>{t('about.serve')}</p>
        <h2>Food safety</h2>
        <p>{t('about.safety')}</p>
        <h2>Privacy</h2>
        <p>{t('about.privacy')}</p>
        <div className="actions">
          <Link className="btn primary" to="/join">Walk with us</Link>
          <Link className="btn" to="/impact">See our impact</Link>
        </div>
      </div>
    </div>
  )
}
