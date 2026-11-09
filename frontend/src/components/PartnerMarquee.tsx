import { useCallback, useEffect, useRef, useState } from 'react'
import { Link } from 'react-router-dom'
import { api } from '../api'
import { useTexts } from '../texts'
import { logoUrl, type PublicPartner } from '../types'

/** One hotel card: its logo when it has one, otherwise its name. */
function PartnerCard({ p }: { p: PublicPartner }) {
  return (
    <li className="partner">
      {p.logoVersion ? (
        <img src={logoUrl(p.id, p.logoVersion)} alt={p.name} loading="lazy" draggable={false} />
      ) : (
        <span className="partner-name">{p.name.replace(/\s*\(sample\)\s*$/i, '')}</span>
      )}
      {p.areaName && <small>{p.areaName}</small>}
    </li>
  )
}

/**
 * "Hotels standing with us": a row of partner logos that drifts slowly on its own.
 * Visitors can swipe it, use the arrow buttons, or the arrow keys. Touching it pauses the drift for a few seconds.
 * With "reduce motion" switched on it never moves by itself.
 */
export default function PartnerMarquee() {
  const { t } = useTexts()
  const [partners, setPartners] = useState<PublicPartner[] | null>(null)
  const box = useRef<HTMLDivElement>(null)
  const pausedUntil = useRef(0)

  useEffect(() => {
    let live = true
    api<PublicPartner[]>('/api/v1/public/partners').then((x) => { if (live) setPartners(x) }).catch(() => { if (live) setPartners([]) })
    return () => { live = false }
  }, [])

  const count = partners?.length ?? 0

  // the list is shown twice; when we pass the first copy we jump back by exactly one copy, so it never ends
  const wrap = useCallback(() => {
    const el = box.current
    if (!el) return
    const half = el.scrollWidth / 2
    if (half <= 0) return
    if (el.scrollLeft >= half) el.scrollLeft -= half
    else if (el.scrollLeft <= 0) el.scrollLeft += half
  }, [])

  useEffect(() => {
    if (count === 0) return
    const el = box.current
    if (el && el.scrollLeft === 0) el.scrollLeft = 1
    if (window.matchMedia('(prefers-reduced-motion: reduce)').matches) return
    const t = setInterval(() => {
      const e = box.current
      if (!e || Date.now() < pausedUntil.current) return
      e.scrollLeft += 1
      wrap()
    }, 30)
    return () => clearInterval(t)
  }, [count, wrap])

  if (!partners || partners.length === 0) return null

  const copies = Math.max(1, Math.ceil(8 / partners.length))
  const base = Array.from({ length: copies }, () => partners).flat()

  function pause() { pausedUntil.current = Date.now() + 5000 }

  function go(dir: -1 | 1) {
    const el = box.current
    if (!el) return
    pause()
    wrap()
    el.scrollBy({ left: dir * 220, behavior: 'smooth' })
  }

  return (
    <section className="partners" aria-labelledby="partners-h">
      <h2 id="partners-h">{t('home.partners.title')}</h2>
      <p className="lead">{t('home.partners.lead')}</p>
      <div className="marquee-wrap">
        <button type="button" className="marquee-btn left" aria-label="Show earlier hotels" onClick={() => go(-1)}>‹</button>
        <div className="marquee" ref={box} tabIndex={0} role="group" aria-label="Partner hotels. Use the arrow keys to move."
             onScroll={wrap}
             onPointerDown={pause} onTouchStart={pause} onWheel={pause} onMouseEnter={pause} onFocus={pause}
             onKeyDown={(e) => { if (e.key === 'ArrowRight') { e.preventDefault(); go(1) } else if (e.key === 'ArrowLeft') { e.preventDefault(); go(-1) } }}>
          <ul className="marquee-track">
            {base.map((p, i) => <PartnerCard key={`a${i}`} p={p} />)}
            {base.map((p, i) => <PartnerCard key={`b${i}`} p={p} />)}
          </ul>
        </div>
        <button type="button" className="marquee-btn right" aria-label="Show more hotels" onClick={() => go(1)}>›</button>
      </div>
      <p className="muted">Run a hotel or a kitchen? <Link to="/join">Stand with us</Link>. We will proudly show your name and logo here.</p>
    </section>
  )
}
