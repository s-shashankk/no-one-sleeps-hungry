import { useCallback, useEffect, useMemo, useState } from 'react'
import { api } from '../api.js'

const when = (v) => (v ? new Date(v).toLocaleString() : '—')

const NEXT = {
  ASSIGNED: [['PICKED_UP', 'Food picked up'], ['CANCELLED', 'Cancel']],
  PICKED_UP: [['DELIVERED', 'Delivered'], ['FAILED', 'Could not deliver']],
}

export default function DeliveriesPanel({ localities, prefillOfferId, onUnauthorized }) {
  const [deliveries, setDeliveries] = useState(null)
  const [offers, setOffers] = useState([])
  const [reports, setReports] = useState([])
  const [offerId, setOfferId] = useState(prefillOfferId ? String(prefillOfferId) : '')
  const [picked, setPicked] = useState([])
  const [safety, setSafety] = useState(false)
  const [error, setError] = useState('')
  const [busy, setBusy] = useState(false)

  const load = useCallback(async () => {
    try {
      const [d, o, r] = await Promise.all([
        api('/api/admin/deliveries'),
        api('/api/admin/food-offers?status=OPEN'),
        api('/api/admin/hunger-reports'),
      ])
      setDeliveries(d)
      setOffers(o.filter((x) => new Date(x.expiresAt) > new Date()))
      setReports(r.filter((x) => ['NEW', 'VERIFIED', 'RECURRING'].includes(x.status)))
    } catch (err) {
      if (err.status === 401) return onUnauthorized()
      setDeliveries([])
      setError(err.message)
    }
  }, [onUnauthorized])

  useEffect(() => { load() }, [load])

  const offer = offers.find((o) => String(o.id) === offerId)
  const candidates = useMemo(
    () => (offer ? reports.filter((r) => r.localityId === offer.localityId) : []),
    [offer, reports])
  const peopleSelected = candidates.filter((r) => picked.includes(r.id)).reduce((n, r) => n + r.peopleCount, 0)
  const locName = (id) => localities.find((l) => l.id === id)?.name

  function chooseOffer(id) {
    setOfferId(id)
    setPicked([])
  }
  const togglePicked = (id) => setPicked((p) => (p.includes(id) ? p.filter((x) => x !== id) : [...p, id]))

  async function create(e) {
    e.preventDefault()
    const form = e.currentTarget
    const f = Object.fromEntries(new FormData(form).entries())
    setBusy(true)
    setError('')
    try {
      await api('/api/admin/deliveries', {
        method: 'POST',
        body: {
          foodOfferId: Number(offerId),
          hungerReportIds: picked,
          volunteerName: f.volunteerName,
          safetyChecked: safety,
          notes: f.notes,
        },
      })
      form.reset()
      setOfferId('')
      setPicked([])
      setSafety(false)
      await load()
    } catch (err) {
      if (err.status === 401) return onUnauthorized()
      setError(err.fields ? Object.values(err.fields).join(' ') || err.message : err.message)
    } finally {
      setBusy(false)
    }
  }

  async function move(d, status) {
    const body = { status }
    if (status === 'DELIVERED') {
      const total = d.reports.reduce((n, r) => n + r.peopleCount, 0)
      const answer = window.prompt('How many people were served?', String(total))
      if (answer === null) return
      body.peopleServed = Math.max(0, Number(answer) || 0)
    }
    if (status === 'FAILED') {
      const why = window.prompt('What went wrong? (e.g. food spoiled, people not found)')
      if (why === null) return
      body.notes = why
    }
    if (status === 'CANCELLED' && !window.confirm('Cancel this delivery? The food goes back on the open list.')) return
    setError('')
    try {
      await api(`/api/admin/deliveries/${d.id}/status`, { method: 'PATCH', body })
      await load()
    } catch (err) {
      if (err.status === 401) return onUnauthorized()
      setError(err.message)
    }
  }

  return (
    <>
      <form onSubmit={create} className="card form">
        <h2>New delivery</h2>
        <label className="field"><span className="label">Food offer (open, still safe to collect)</span>
          <select value={offerId} onChange={(e) => chooseOffer(e.target.value)} required>
            <option value="">— choose —</option>
            {offers.map((o) => (
              <option key={o.id} value={o.id}>
                #{o.id} · {o.servings} servings · {o.area}{locName(o.localityId) ? ` (${locName(o.localityId)})` : ' (no locality!)'}
              </option>
            ))}
          </select>
        </label>

        {offer && (
          <>
            <p className="hint">
              {offer.description} — pickup: {offer.pickupAddress} — safe until {when(offer.expiresAt)}
            </p>
            {!offer.localityId && <p className="error">Assign a locality to this offer first (Food offers tab).</p>}
            <div className="field">
              <span className="label">Who will receive it? (hunger reports in the same locality)</span>
              {candidates.length === 0 && <p className="hint">No open hunger reports in this locality.</p>}
              {candidates.map((r) => (
                <label key={r.id} className="check">
                  <input type="checkbox" checked={picked.includes(r.id)} onChange={() => togglePicked(r.id)} />
                  <span>#{r.id} · {r.peopleCount} people · {r.locationText} <small>({r.status})</small></span>
                </label>
              ))}
              {picked.length > 0 && (
                <p className={peopleSelected > offer.servings ? 'error' : 'hint'}>
                  {peopleSelected} people selected · {offer.servings} servings available
                  {peopleSelected > offer.servings && ' — not enough food for everyone'}
                </p>
              )}
            </div>
          </>
        )}

        <label className="field"><span className="label">Who will collect and deliver it?</span>
          <input name="volunteerName" required maxLength={120} placeholder="Volunteer name" /></label>
        <label className="field"><span className="label">Notes (optional)</span>
          <textarea name="notes" rows={2} maxLength={1000} /></label>
        <label className="check">
          <input type="checkbox" checked={safety} onChange={(e) => setSafety(e.target.checked)} />
          <span>I checked the food looks and smells fine and is within its safe time.</span>
        </label>
        {error && <p className="error banner">{error}</p>}
        <button className="btn primary" disabled={busy || !offer || picked.length === 0 || !safety || !offer.localityId}>
          Create delivery
        </button>
      </form>

      <h2>Deliveries</h2>
      {deliveries === null && <p>Loading…</p>}
      {deliveries?.length === 0 && <p className="lead">No deliveries yet.</p>}
      {deliveries?.map((d) => (
        <article key={d.id} className="card record">
          <header>
            <strong>Delivery #{d.id} · {d.status.replace('_', ' ')}</strong>
            <small>{when(d.createdAt)}</small>
          </header>
          {d.offer && (
            <p><b>Food:</b> {d.offer.servings} servings — {d.offer.description}<br />
              <b>Pickup:</b> {d.offer.pickupAddress}</p>
          )}
          <p><b>To:</b></p>
          <ul>
            {d.reports.map((r) => (
              <li key={r.id}>
                {r.peopleCount} people · {r.locationText}
                {r.latitude != null && (
                  <> · <a href={`https://www.google.com/maps?q=${r.latitude},${r.longitude}`} target="_blank" rel="noreferrer">map</a></>
                )}
              </li>
            ))}
          </ul>
          <p><b>Volunteer:</b> {d.volunteerName || '—'} · <b>Locality:</b> {locName(d.localityId) || '—'}</p>
          {d.peopleServed != null && <p><b>People served:</b> {d.peopleServed}</p>}
          {d.notes && <p><b>Notes:</b> {d.notes}</p>}
          <p className="hint">
            Picked up: {when(d.pickedUpAt)} · Delivered: {when(d.deliveredAt)} · Created by {d.createdBy}
          </p>
          {NEXT[d.status] && (
            <div className="toolbar">
              {NEXT[d.status].map(([s, label]) => (
                <button key={s} className={s === 'PICKED_UP' || s === 'DELIVERED' ? 'btn primary' : 'btn'}
                  onClick={() => move(d, s)}>{label}</button>
              ))}
            </div>
          )}
        </article>
      ))}
    </>
  )
}
