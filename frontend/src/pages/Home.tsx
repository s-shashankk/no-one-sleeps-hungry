import type { ReactNode } from 'react'
import { Link } from 'react-router-dom'
import CountUp from '../components/CountUp'
import { GArrow } from '../components/Glyphs'
import { IconDeliver, IconNeed, IconSurplus } from '../components/Icons'
import PartnerMarquee from '../components/PartnerMarquee'
import { Stories } from '../components/Stories'
import { useImpact } from '../impact'
import { useTexts } from '../texts'

/** A number that shows a soft placeholder while it loads, and a dash if the server cannot be reached. */
function Figure({ ready, failed, children }: { ready: boolean; failed: boolean; children: ReactNode }) {
  if (ready) return <b>{children}</b>
  return <b className={failed ? undefined : 'skel'}>{failed ? '–' : ' '}</b>
}

export default function Home() {
  const { impact, failed } = useImpact()
  const { t } = useTexts()
  const open = impact?.areas.filter((a) => a.open) ?? []
  const soon = impact?.areas.filter((a) => !a.open) ?? []
  const ready = impact !== null

  return (
    <>
      <section className="hero">
        <div className="hero-inner">
          <div>
            <h1>{t('home.hero.title')}</h1>
            <p>{t('home.hero.text')}</p>
            <div className="cta">
              <Link className="btn warm big" to="/deliver">{t('home.hero.feed')}</Link>
              <Link className="btn big" to="/report-hunger">{t('home.hero.hungry')}</Link>
            </div>
            <p className="hero-note">{t('home.hero.note')}</p>
          </div>
          <div className="hero-art-wrap">
            <img className="hero-art" src="/img/hero-meal.svg" width="640" height="560" alt="A meal on a banana leaf: rice, sambar, vegetables, papad and a sweet" />
            {impact && impact.hotelPartners > 0 && (
              <p className="live-chip"><i aria-hidden="true" />{impact.hotelPartners} partner {impact.hotelPartners === 1 ? 'kitchen is' : 'kitchens are'} ready to share food tonight</p>
            )}
          </div>
        </div>
      </section>

      <section className="ledger" aria-label="Our impact so far">
        <div className="ledger-inner">
          <div><Figure ready={ready} failed={failed}>{impact && <CountUp value={impact.mealsServed} />}</Figure><span>meals served</span></div>
          <div><Figure ready={ready} failed={failed}>{impact?.cities}</Figure><span>{impact?.cities === 1 ? 'city' : 'cities'}</span></div>
          <div><Figure ready={ready} failed={failed}>{impact?.areasOpen}</Figure><span>{impact?.areasOpen === 1 ? 'area open' : 'areas open'}</span></div>
          <div><Figure ready={ready} failed={failed}>{impact?.hotelPartners}</Figure><span>hotel partners</span></div>
        </div>
      </section>

      <section className="who" aria-labelledby="who-h">
        <div className="who-inner">
          <h2 id="who-h">{t('home.who.title')}</h2>
          <p className="who-lead">{t('home.who.lead')}</p>
          <Stories />
          <p className="who-foot">{t('home.who.foot')}</p>
        </div>
      </section>

      <div className="container wide">
        <section className="section" aria-labelledby="paths-h">
          <h2 id="paths-h">{t('home.paths.title')}</h2>
          <div className="paths">
            <Link className="path" to="/deliver">
              <IconDeliver />
              <h3>{t('home.path1.title')}</h3>
              <p>{t('home.path1.text')}</p>
              <span className="go">{t('home.path1.go')}<GArrow /></span>
            </Link>
            <Link className="path" to="/report-hunger">
              <IconNeed />
              <h3>{t('home.path2.title')}</h3>
              <p>{t('home.path2.text')}</p>
              <span className="go">{t('home.path2.go')}<GArrow /></span>
            </Link>
            <Link className="path" to="/surplus-food">
              <IconSurplus />
              <h3>{t('home.path3.title')}</h3>
              <p>{t('home.path3.text')}</p>
              <span className="go">{t('home.path3.go')}<GArrow /></span>
            </Link>
          </div>
        </section>

        <section className="section" aria-labelledby="how-h">
          <h2 id="how-h">{t('home.how.title')}</h2>
          <ol className="steps">
            <li><h3>{t('home.how1.title')}</h3><p>{t('home.how1.text')}</p></li>
            <li><h3>{t('home.how2.title')}</h3><p>{t('home.how2.text')}</p></li>
            <li><h3>{t('home.how3.title')}</h3><p>{t('home.how3.text')}</p></li>
          </ol>
        </section>

        <PartnerMarquee />

        <section className="section" aria-labelledby="areas-h">
          <h2 id="areas-h">{t('home.areas.title')}</h2>
          <p className="lead">{t('home.areas.lead')}</p>
          <div className="area-grid">
            {open.map((a) => (
              <div className="area" key={a.id}>
                <span className="chip open">Open now</span>
                <h3>{a.name}</h3>
                <span className="where">{a.city} · {a.hotels} partner {a.hotels === 1 ? 'hotel' : 'hotels'}</span>
              </div>
            ))}
            {soon.map((a) => (
              <div className="area" key={a.id}>
                <span className="chip soon">Coming soon</span>
                <h3>{a.name}</h3>
                <span className="where">{a.city}</span>
              </div>
            ))}
          </div>
          <p className="after-grid"><Link className="link-btn" to="/impact">{t('home.areas.link')}</Link></p>
        </section>

        <section className="band" aria-labelledby="join-h">
          <h2 id="join-h">{t('home.band.title')}</h2>
          <p>{t('home.band.text')}</p>
          <Link className="btn warm big" to="/join">{t('home.band.button')}</Link>
        </section>
      </div>
    </>
  )
}
