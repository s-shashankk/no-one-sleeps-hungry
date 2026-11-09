import { FormEvent, useEffect, useState } from 'react'
import { Link, Navigate, useLocation, useSearchParams } from 'react-router-dom'
import { api } from '../api'
import { useAuth } from '../auth'
import { GPin } from '../components/Glyphs'
import { errorText, wholeNumber } from '../format'
import type { PublicArea, PublicHotel } from '../types'

/**
 * A signed-in volunteer says "I will collect food from this hotel for this many people".
 * One question per screen: area, then hotel, then how many people. The admin gets it in the pickups inbox.
 */
export default function Deliver() {
  const { me } = useAuth()
  const location = useLocation()
  const [params] = useSearchParams()
  const [step, setStep] = useState<1 | 2 | 3>(1)
  const [areas, setAreas] = useState<PublicArea[] | null>(null)
  const [area, setArea] = useState<PublicArea | null>(null)
  const [hotels, setHotels] = useState<PublicHotel[] | null>(null)
  const [hotel, setHotel] = useState<PublicHotel | null>(null)
  const [people, setPeople] = useState('')
  const [deliverTo, setDeliverTo] = useState(params.get('to') ?? '')
  const [error, setError] = useState('')
  const [busy, setBusy] = useState(false)
  const [done, setDone] = useState('')

  useEffect(() => {
    if (!me || me.role !== 'VOLUNTEER') return
    api<PublicArea[]>('/api/v1/public/areas').then(setAreas).catch((e) => setError(errorText(e)))
  }, [me])

  // every new screen starts at the top, so nobody has to scroll to find the next question
  useEffect(() => { window.scrollTo(0, 0) }, [step, done])

  // a hotel that has given all its plates for today is not offered at all (the server also refuses the booking)
  const available = (hotels ?? []).filter((h) => h.spotsLeft > 0)

  function pickArea(a: PublicArea) {
    setArea(a); setHotel(null); setHotels(null); setError(''); setStep(2)
    api<PublicHotel[]>(`/api/v1/public/areas/${a.id}/hotels`).then(setHotels).catch((e) => setError(errorText(e)))
  }

  function pickHotel(h: PublicHotel) {
    setHotel(h); setError(''); setStep(3)
  }

  function back() {
    setError('')
    setStep(step === 3 ? 2 : 1)
  }

  async function submit(e: FormEvent) {
    e.preventDefault()
    if (!hotel) return
    const count = wholeNumber(people)
    if (!count) { setError('Please tell us how many people are waiting (a whole number).'); return }
    if (count > hotel.spotsLeft) {
      setError(`${hotel.name} has only ${hotel.spotsLeft} ${hotel.spotsLeft === 1 ? 'plate' : 'plates'} left today. Please enter ${hotel.spotsLeft} or fewer.`)
      return
    }
    setBusy(true)
    setError('')
    try {
      await api('/api/v1/me/pickups', {
        method: 'POST',
        body: { vendorId: hotel.id, peopleCount: count, deliverTo: deliverTo || undefined },
      })
      setDone(`${hotel.name} has been told you are coming. Please go when you are ready and tell them your name; they will see your booking and hand over the food. Please carry food for about ${count} ${count === 1 ? 'person' : 'people'}.`)
    } catch (err) {
      setError(errorText(err))
    } finally {
      setBusy(false)
    }
  }

  if (me === undefined) return <p>Loading…</p>
  if (me === null) {
    const here = location.pathname + location.search
    return (
      <div className="narrow">
        <div className="card">
          <h1>Thank you for wanting to help</h1>
          <p className="lead">Before you go, make a small account. It only takes a minute, and it lets us call you, keep you safe, and count every meal you carry to someone who needed it.</p>
          <div className="actions">
            <Link className="btn primary big" to="/register" state={{ from: here }}>Create my account</Link>
            <Link className="btn big" to="/login" state={{ from: here }}>I already have one</Link>
          </div>
        </div>
      </div>
    )
  }
  if (me.role === 'HOTEL') return <Navigate to="/hotel" replace />
  if (me.role !== 'VOLUNTEER') return <Navigate to="/admin/pickups" replace />

  if (done) {
    return (
      <div className="narrow">
        <div className="card thanks">
          <h1>God bless you, {me.fullName.split(' ')[0] || 'friend'} 🙏</h1>
          <p>{done}</p>
          {hotel && area && (
            <p className="recap">
              {hotel.name}, {hotel.location} ({area.name})<br />
              <a className="btn map small" target="_blank" rel="noreferrer"
                 href={hotel.mapUrl || `https://www.google.com/maps/search/?api=1&query=${encodeURIComponent(hotel.name + ' ' + hotel.location + ' Bengaluru')}`}><GPin />Open in Maps</a>
            </p>
          )}
          <p>Somebody will eat tonight because you said yes. That is the only thanks we need.</p>
          <div className="actions"><Link className="btn primary" to="/me">My deliveries</Link><Link className="btn" to="/">Back to home</Link></div>
        </div>
      </div>
    )
  }

  return (
    <div className="narrow wizard">
      <ol className="stepper" aria-label="Steps">
        {['Area', 'Hotel', 'People'].map((t, i) => (
          <li key={t} className={step === i + 1 ? 'on' : step > i + 1 ? 'done' : ''} aria-current={step === i + 1 ? 'step' : undefined}>
            {i + 1}. {t}
          </li>
        ))}
      </ol>

      {error && <p className="error" role="alert">{error}</p>}

      {step === 1 && (
        <section>
          <h1>Where can you go?</h1>
          <p className="lead">Pick the area closest to you. Someone nearby is waiting for a warm meal.</p>
          {!areas && !error && <p>Loading areas…</p>}
          <div className="area-grid">
            {areas?.map((a) => (
              <button key={a.id} type="button" className="area" disabled={!a.open} onClick={() => pickArea(a)}>
                <span className={'chip ' + (a.open ? 'open' : 'soon')}>{a.open ? 'Open now' : 'Coming soon'}</span>
                <h3>{a.name}</h3>
                <span className="where">{a.city}{a.open ? ` · ${a.hotels} ${a.hotels === 1 ? 'hotel' : 'hotels'}` : ''}</span>
              </button>
            ))}
          </div>
        </section>
      )}

      {step === 2 && area && (
        <section>
          <h1>Which kitchen will you visit?</h1>
          <p className="lead">These hotels in {area.name} are happy to share what they have left.</p>
          {!hotels && !error && <p>Loading hotels…</p>}
          {hotels && hotels.length === 0 && (
            <p className="hint">No partner hotel here just yet. <Link to="/join">Join the community</Link> and we will call you the day one is ready.</p>
          )}
          {hotels && hotels.length > 0 && available.length === 0 && (
            <p className="hint">All hotels here have given their plates for today. Please come back tomorrow, and thank you for wanting to help.</p>
          )}
          <div className="hotel-list">
            {available.map((h) => (
              <button key={h.id} type="button" className="area" onClick={() => pickHotel(h)}>
                <span className={'chip ' + (h.spotsLeft > 0 ? 'open' : 'soon')}>
                  {`${h.spotsLeft} ${h.spotsLeft === 1 ? 'plate' : 'plates'} left today`}
                </span>
                <h3>{h.name}</h3>
                <span className="where">{h.location}</span>
              </button>
            ))}
          </div>
          <button type="button" className="btn" onClick={back}>← Back</button>
        </section>
      )}

      {step === 3 && hotel && (
        <form className="form" onSubmit={submit}>
          <h1>How many plates will you take?</h1>
          <p className="recap">You will collect from <strong>{hotel.name}</strong>, {hotel.location}.{hotel.mapUrl && <> <a href={hotel.mapUrl} target="_blank" rel="noreferrer">See it on the map</a></>}</p>
          <label>How many plates will you take? (one plate for each person you will feed)
            <input type="number" inputMode="numeric" min="1" max={hotel.spotsLeft} step="1" value={people}
                   onChange={(e) => setPeople(e.target.value)} placeholder={`1 to ${hotel.spotsLeft}`} required autoFocus />
          </label>
          <p className="hint"><strong>{hotel.spotsLeft} {hotel.spotsLeft === 1 ? 'plate is' : 'plates are'} left</strong> at this hotel today.</p>
          <p className="muted">An honest number helps the kitchen keep just enough ready, so nothing is wasted and nobody is left out.</p>
          <label>Where will you take the food? (optional)
            <input value={deliverTo} onChange={(e) => setDeliverTo(e.target.value)} maxLength={500} placeholder="e.g. the shelter near the market" />
          </label>
          <p className="muted">The hotel will get your name and phone number so they know who to expect. See our <Link to="/privacy">privacy notice</Link>.</p>
          <div className="actions">
            <button type="button" className="btn" onClick={back}>← Back</button>
            <button className="btn primary big" disabled={busy}>{busy ? 'Sending…' : 'Yes, I will go'}</button>
          </div>
        </form>
      )}
    </div>
  )
}
