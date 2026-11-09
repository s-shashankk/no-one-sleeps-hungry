import { ReactNode, useState } from 'react'
import { useTexts } from '../texts'

/**
 * Shows a real photo if photos are switched on (VITE_STORY_PHOTOS=true) and the file exists in public/img/photos/,
 * otherwise the drawn scene. Only use photos of people who agreed to be photographed.
 */
function StoryImage({ photo, alt, children }: { photo: string; alt: string; children: ReactNode }) {
  const [failed, setFailed] = useState(false)
  const usePhotos = import.meta.env.VITE_STORY_PHOTOS === 'true'
  if (!usePhotos || failed) return <>{children}</>
  return <img src={`/img/photos/${photo}.jpg`} alt={alt} loading="lazy" onError={() => setFailed(true)} />
}

const sky = (id: string, top: string, bottom: string) => (
  <linearGradient id={id} x1="0" y1="0" x2="0" y2="1">
    <stop offset="0" stopColor={top} />
    <stop offset="1" stopColor={bottom} />
  </linearGradient>
)

/** A small child sits on the pavement at dusk with an empty bowl. */
function Child() {
  return (
    <svg viewBox="0 0 400 300" role="img" aria-label="Drawing of a small child sitting alone on a pavement at dusk, holding an empty bowl">
      <defs>
        {sky('s1', '#2d3561', '#f08a4b')}
        <radialGradient id="g1" cx=".5" cy=".5" r=".5"><stop offset="0" stopColor="#ffd37a" stopOpacity=".9" /><stop offset="1" stopColor="#ffd37a" stopOpacity="0" /></radialGradient>
      </defs>
      <rect width="400" height="300" fill="url(#s1)" />
      <g fill="#1d2142" opacity=".85">
        <rect x="14" y="150" width="40" height="100" /><rect x="60" y="120" width="34" height="130" /><rect x="100" y="165" width="46" height="85" />
        <rect x="290" y="135" width="38" height="115" /><rect x="334" y="160" width="52" height="90" />
      </g>
      <g fill="#ffd37a" opacity=".8">
        <rect x="70" y="135" width="6" height="8" /><rect x="80" y="150" width="6" height="8" /><rect x="300" y="150" width="6" height="8" /><rect x="345" y="175" width="6" height="8" />
      </g>
      <circle cx="320" cy="150" r="70" fill="url(#g1)" />
      <rect x="318" y="150" width="4" height="100" fill="#14183a" /><circle cx="320" cy="148" r="7" fill="#ffe7a8" />
      <rect y="238" width="400" height="62" fill="#14183a" />
      <path d="M0 238h400" stroke="#2d3561" strokeWidth="3" />
      {/* child */}
      <g fill="#0f1230">
        <circle cx="190" cy="168" r="18" />
        <path d="M168 232c-4-34 4-52 22-52s26 18 22 52z" />
        <path d="M172 232l-22 8 6 6 30-6zM208 232l24 6-4 8-30-4z" />
        <path d="M176 196c-10 8-14 20-12 30l8-2c0-8 4-16 10-22zM204 196c10 6 18 14 22 24l-8 4c-4-8-8-14-18-18z" />
      </g>
      <path d="M205 224c8 10 34 10 42 0z" fill="#e8b84a" opacity=".95" />
      <path d="M205 224c8-4 34-4 42 0" stroke="#fff3c4" strokeWidth="2" fill="none" />
    </svg>
  )
}

/** An elderly person sits on a step under a blanket, leaning on a stick. */
function Elder() {
  return (
    <svg viewBox="0 0 400 300" role="img" aria-label="Drawing of an elderly person wrapped in a blanket, sitting alone on a step in the evening with a walking stick">
      <defs>
        {sky('s2', '#3a2b5c', '#e9795a')}
        <radialGradient id="g2" cx=".5" cy=".5" r=".5"><stop offset="0" stopColor="#ffd9a0" stopOpacity=".85" /><stop offset="1" stopColor="#ffd9a0" stopOpacity="0" /></radialGradient>
      </defs>
      <rect width="400" height="300" fill="url(#s2)" />
      <g fill="#241a3d" opacity=".9">
        <rect x="0" y="110" width="70" height="140" /><rect x="76" y="140" width="46" height="110" />
        <rect x="270" y="100" width="60" height="150" /><rect x="336" y="130" width="64" height="120" />
      </g>
      <g fill="#ffd9a0" opacity=".85">
        <rect x="14" y="130" width="8" height="10" /><rect x="38" y="160" width="8" height="10" /><rect x="286" y="122" width="8" height="10" /><rect x="350" y="150" width="8" height="10" />
      </g>
      <circle cx="105" cy="200" r="62" fill="url(#g2)" />
      <rect y="240" width="400" height="60" fill="#1b1330" />
      <rect x="120" y="226" width="200" height="16" fill="#2b2046" />
      <rect x="140" y="212" width="170" height="16" fill="#33275a" />
      {/* elder */}
      <g fill="#120c24">
        <circle cx="236" cy="136" r="17" />
        <path d="M206 214c-10-36 0-64 30-64s42 26 36 64z" />
        <path d="M214 214l-10 20h66l-8-20z" />
      </g>
      <path d="M212 168c8-20 32-24 48-8-6 14-30 24-48 8z" fill="#c9553a" opacity=".9" />
      <path d="M284 172l16 62" stroke="#120c24" strokeWidth="5" strokeLinecap="round" />
      <path d="M280 172c4-6 12-6 14 0" stroke="#120c24" strokeWidth="5" fill="none" strokeLinecap="round" />
      <circle cx="104" cy="196" r="7" fill="#ffe9bd" /><rect x="102" y="196" width="4" height="40" fill="#120c24" />
    </svg>
  )
}

/** A family sits close together under a flyover at night, around a small light. */
function Family() {
  return (
    <svg viewBox="0 0 400 300" role="img" aria-label="Drawing of a family of three sitting close together under a flyover at night, around a small warm light">
      <defs>
        {sky('s3', '#0f1736', '#33407a')}
        <radialGradient id="g3" cx=".5" cy=".5" r=".5"><stop offset="0" stopColor="#ffc861" stopOpacity=".95" /><stop offset="1" stopColor="#ffc861" stopOpacity="0" /></radialGradient>
      </defs>
      <rect width="400" height="300" fill="url(#s3)" />
      <g fill="#fff" opacity=".7"><circle cx="40" cy="30" r="1.5" /><circle cx="120" cy="50" r="1.5" /><circle cx="300" cy="24" r="1.5" /><circle cx="360" cy="60" r="1.5" /></g>
      {/* flyover */}
      <rect x="0" y="70" width="400" height="34" fill="#0a0f26" />
      <rect x="0" y="100" width="400" height="8" fill="#161d44" />
      <rect x="60" y="104" width="22" height="140" fill="#0a0f26" /><rect x="318" y="104" width="22" height="140" fill="#0a0f26" />
      <g fill="#ffd37a"><rect x="30" y="82" width="12" height="4" /><rect x="120" y="82" width="12" height="4" /><rect x="210" y="82" width="12" height="4" /><rect x="300" y="82" width="12" height="4" /></g>
      <rect y="240" width="400" height="60" fill="#080c1f" />
      <circle cx="200" cy="228" r="80" fill="url(#g3)" />
      {/* family */}
      <g fill="#05081a">
        <circle cx="150" cy="176" r="15" /><path d="M126 240c-6-34 2-52 24-52s30 18 24 52z" />
        <circle cx="252" cy="178" r="14" /><path d="M230 240c-6-32 2-50 22-50s28 18 22 50z" />
        <circle cx="202" cy="204" r="11" /><path d="M186 240c-4-22 2-34 16-34s20 12 16 34z" />
      </g>
      <path d="M194 240c2-12 6-14 8-22 2 8 6 10 8 22z" fill="#ffb64a" />
      <circle cx="202" cy="236" r="5" fill="#fff0b8" />
    </svg>
  )
}

export function Stories() {
  const { t } = useTexts()
  return (
    <div className="stories">
      <figure className="story">
        <div className="story-art"><StoryImage photo="child" alt="A child waiting for a meal"><Child /></StoryImage></div>
        <figcaption>
          <h3>{t('home.story1.title')}</h3>
          <p>{t('home.story1.text')}</p>
        </figcaption>
      </figure>
      <figure className="story">
        <div className="story-art"><StoryImage photo="elder" alt="An elderly person sitting alone"><Elder /></StoryImage></div>
        <figcaption>
          <h3>{t('home.story2.title')}</h3>
          <p>{t('home.story2.text')}</p>
        </figcaption>
      </figure>
      <figure className="story">
        <div className="story-art"><StoryImage photo="family" alt="A family sheltering under a flyover"><Family /></StoryImage></div>
        <figcaption>
          <h3>{t('home.story3.title')}</h3>
          <p>{t('home.story3.text')}</p>
        </figcaption>
      </figure>
    </div>
  )
}
