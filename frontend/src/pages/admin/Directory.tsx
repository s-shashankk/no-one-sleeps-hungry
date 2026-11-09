import { FormEvent, useCallback, useEffect, useState } from 'react'
import { api, apiUpload, PageResponse } from '../../api'
import Pager from '../../components/Pager'
import { errorText, whatsappLink } from '../../format'
import { logoUrl, type Area } from '../../types'

/** What differs between the vendor list and the coworker list. */
export interface DirectoryConfig {
  title: string
  singular: string                 // "vendor" / "coworker"
  path: string                     // API path
  nameLabel: string
  locationLabel: string
  hasContactPerson: boolean        // vendors have a contact person, coworkers are the contact
  hasArea?: boolean                // vendors belong to an area (neighbourhood)
  whatsapp: boolean
  emptyText: string
}

interface Person {
  id: number
  name: string
  contactPerson?: string
  phoneNumber: string
  location?: string
  notes?: string
  areaId?: number
  areaName?: string
  logoVersion?: number
  dailyLimit?: number
  mapUrl?: string
  active: boolean
}

interface FormState {
  name: string
  contactPerson: string
  phoneNumber: string
  location: string
  notes: string
  areaId: string
  dailyLimit: string
  mapUrl: string
}

const BLANK: FormState = { name: '', contactPerson: '', phoneNumber: '', location: '', notes: '', areaId: '', dailyLimit: '5', mapUrl: '' }

/** One screen to add, view, edit and switch on/off: used for both vendors and coworkers. */
export default function Directory({ config }: { config: DirectoryConfig }) {
  const [page, setPage] = useState<PageResponse<Person> | null>(null)
  const [pageNo, setPageNo] = useState(0)
  const [q, setQ] = useState('')
  const [show, setShow] = useState<'true' | 'false' | ''>('true')
  const [editing, setEditing] = useState<Person | 'new' | null>(null)
  const [error, setError] = useState('')

  const load = useCallback(() => {
    const params = new URLSearchParams({ page: String(pageNo), size: '20' })
    if (q.trim()) params.set('q', q.trim())
    if (show) params.set('active', show)
    api<PageResponse<Person>>(`${config.path}?${params}`)
      .then((p) => { setPage(p); setError('') })
      .catch((e) => setError(errorText(e)))
  }, [config.path, pageNo, q, show])

  useEffect(() => { load() }, [load])

  async function toggleActive(p: Person) {
    try {
      await api(`${config.path}/${p.id}`, { method: 'PUT', body: { ...toBody(p, config.hasArea), active: !p.active } })
      load()
    } catch (e) {
      setError(errorText(e))
    }
  }

  return (
    <div className="card">
      <div className="row-between">
        <h1>{config.title}</h1>
        <button className="btn primary" onClick={() => setEditing('new')}>+ Add {config.singular}</button>
      </div>

      {editing && (
        <PersonForm
          key={editing === 'new' ? 'new' : editing.id}
          config={config}
          person={editing === 'new' ? null : editing}
          onCancel={() => setEditing(null)}
          onSaved={() => { setEditing(null); load() }}
        />
      )}

      <div className="filters">
        <input type="search" placeholder="Search name, place or phone" value={q}
               onChange={(e) => { setQ(e.target.value); setPageNo(0) }} />
        <select value={show} onChange={(e) => { setShow(e.target.value as typeof show); setPageNo(0) }}>
          <option value="true">Active only</option>
          <option value="false">Switched off</option>
          <option value="">All</option>
        </select>
      </div>

      {error && <p className="error">{error}</p>}
      {!page && !error && <p>Loading…</p>}
      {page && page.content.length === 0 && <p className="hint">{config.emptyText}</p>}

      {page && page.content.map((p) => (
        <div className="item" key={p.id}>
          <h3>
            {p.name}
            {!p.active && <em className="badge off">Switched off</em>}
          </h3>
          {p.contactPerson && <div>Contact: {p.contactPerson}</div>}
          {p.areaName && <div>🏙️ {p.areaName}{config.hasArea && p.dailyLimit ? ` · up to ${p.dailyLimit} plates a day` : ''}</div>}
          {p.location && <div>📍 {p.location}</div>}
          <div>
            📞 <a href={`tel:${p.phoneNumber}`}>{p.phoneNumber}</a>
            {config.whatsapp && <> · <a href={whatsappLink(p.phoneNumber)} target="_blank" rel="noreferrer">WhatsApp</a></>}
          </div>
          {p.notes && <div className="hint">{p.notes}</div>}
          {config.hasArea && <LogoControls hotel={p} onChanged={load} />}
          <div className="actions">
            <button className="btn small" onClick={() => setEditing(p)}>Edit</button>
            <button className="btn small" onClick={() => toggleActive(p)}>{p.active ? 'Switch off' : 'Switch on'}</button>
          </div>
        </div>
      ))}

      {page && <Pager page={page} onPage={setPageNo} />}
    </div>
  )
}

/** Shows the hotel's logo and lets the admin upload, replace or remove it. */
function LogoControls({ hotel, onChanged }: { hotel: Person; onChanged: () => void }) {
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState('')

  async function pick(file: File | undefined) {
    if (!file) return
    setBusy(true)
    setError('')
    try {
      await apiUpload(`/api/v1/vendors/${hotel.id}/logo`, file)
      onChanged()
    } catch (e) {
      setError(errorText(e))
    } finally {
      setBusy(false)
    }
  }

  async function remove() {
    setBusy(true)
    setError('')
    try {
      await api(`/api/v1/vendors/${hotel.id}/logo`, { method: 'DELETE' })
      onChanged()
    } catch (e) {
      setError(errorText(e))
    } finally {
      setBusy(false)
    }
  }

  return (
    <div>
      <div className="logo-row">
        <div className="logo-thumb">
          {hotel.logoVersion
            ? <img src={logoUrl(hotel.id, hotel.logoVersion)} alt={`${hotel.name} logo`} />
            : <span>No logo yet</span>}
        </div>
        <label className="btn small file-btn">
          {busy ? 'Working…' : hotel.logoVersion ? 'Change logo' : 'Upload logo'}
          <input type="file" accept="image/png,image/jpeg,image/webp" disabled={busy}
                 onChange={(e) => { pick(e.target.files?.[0]); e.target.value = '' }} />
        </label>
        {hotel.logoVersion && <button className="btn small" disabled={busy} onClick={remove}>Remove logo</button>}
      </div>
      <div className="hint">PNG, JPG or WebP, under 500 KB. Shown on the home page slider so visitors see who supports us.</div>
      {error && <p className="error">{error}</p>}
    </div>
  )
}

function toBody(p: Person, withArea = false) {
  return {
    name: p.name, contactPerson: p.contactPerson ?? '', phoneNumber: p.phoneNumber,
    location: p.location ?? '', notes: p.notes ?? '',
    ...(withArea ? { areaId: p.areaId ?? null, mapUrl: p.mapUrl ?? '' } : {}),
  }
}

function PersonForm({ config, person, onCancel, onSaved }: {
  config: DirectoryConfig
  person: Person | null
  onCancel: () => void
  onSaved: () => void
}) {
  const [f, setF] = useState<FormState>(
    person ? { ...BLANK, ...toBody(person), areaId: person.areaId ? String(person.areaId) : '', dailyLimit: String(person.dailyLimit ?? 5), mapUrl: person.mapUrl ?? '' } : BLANK)
  const [areas, setAreas] = useState<Area[]>([])
  useEffect(() => {
    if (config.hasArea) api<Area[]>('/api/v1/areas').then(setAreas).catch(() => setAreas([]))
  }, [config.hasArea])
  const [error, setError] = useState('')
  const [busy, setBusy] = useState(false)
  const set = (k: keyof FormState) => (e: { target: { value: string } }) => setF({ ...f, [k]: e.target.value })

  async function save(e: FormEvent) {
    e.preventDefault()
    setBusy(true)
    setError('')
    try {
      const { areaId, dailyLimit, mapUrl, ...rest } = f
      const body = {
        ...rest,
        ...(config.hasArea ? { mapUrl: mapUrl.trim() } : {}),
        ...(config.hasArea ? { dailyLimit: Number(dailyLimit) || 5 } : {}),
        contactPerson: config.hasContactPerson ? f.contactPerson : undefined,
        ...(config.hasArea ? { areaId: areaId ? Number(areaId) : null } : {}),
      }
      if (person) await api(`${config.path}/${person.id}`, { method: 'PUT', body })
      else await api(config.path, { method: 'POST', body })
      onSaved()
    } catch (err) {
      setError(errorText(err))
    } finally {
      setBusy(false)
    }
  }

  return (
    <form className="form notice card" onSubmit={save}>
      <h2>{person ? `Edit ${config.singular}` : `Add ${config.singular}`}</h2>
      <label>{config.nameLabel}
        <input value={f.name} onChange={set('name')} maxLength={160} required />
      </label>
      {config.hasContactPerson && (
        <label>Contact person (optional)
          <input value={f.contactPerson} onChange={set('contactPerson')} maxLength={120} />
        </label>
      )}
      <label>Phone number
        <input type="tel" value={f.phoneNumber} onChange={set('phoneNumber')} placeholder="+91…" required />
      </label>
      {config.hasArea && (
        <label>Area
          <select value={f.areaId} onChange={set('areaId')}>
            <option value="">No area yet</option>
            {areas.map((a) => <option key={a.id} value={a.id}>{a.name}, {a.city}{a.open ? '' : ' (coming soon)'}</option>)}
          </select>
        </label>
      )}
      {config.hasArea && (
        <label>Plates this hotel gives per day (each booking uses one)
          <input type="number" min="1" max="500" step="1" value={f.dailyLimit} onChange={set('dailyLimit')} required />
        </label>
      )}
      <label>{config.locationLabel}
        <input value={f.location} onChange={set('location')} maxLength={500} required />
      </label>
      {config.hasArea && (
        <label>Google Maps link (optional)
          <input type="url" inputMode="url" value={f.mapUrl} onChange={set('mapUrl')} maxLength={500} placeholder="https://maps.app.goo.gl/…" />
          <span className="hint">In Google Maps open the hotel, press Share, then Copy link. Volunteers are taken to this exact place.</span>
        </label>
      )}
      <label>Notes (optional)
        <input value={f.notes} onChange={set('notes')} maxLength={1000} />
      </label>
      {error && <p className="error">{error}</p>}
      <div className="actions">
        <button className="btn primary" disabled={busy}>{busy ? 'Saving…' : 'Save'}</button>
        <button type="button" className="btn" onClick={onCancel}>Cancel</button>
      </div>
    </form>
  )
}
