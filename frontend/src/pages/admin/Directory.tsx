import { FormEvent, useCallback, useEffect, useState } from 'react'
import { api, PageResponse } from '../../api'
import Pager from '../../components/Pager'
import { errorText, whatsappLink } from '../../format'

/** What differs between the vendor list and the coworker list. */
export interface DirectoryConfig {
  title: string
  singular: string                 // "vendor" / "coworker"
  path: string                     // API path
  nameLabel: string
  locationLabel: string
  hasContactPerson: boolean        // vendors have a contact person, coworkers are the contact
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
  active: boolean
}

interface FormState {
  name: string
  contactPerson: string
  phoneNumber: string
  location: string
  notes: string
}

const BLANK: FormState = { name: '', contactPerson: '', phoneNumber: '', location: '', notes: '' }

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
      await api(`${config.path}/${p.id}`, { method: 'PUT', body: { ...toBody(p), active: !p.active } })
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
          {p.location && <div>📍 {p.location}</div>}
          <div>
            📞 <a href={`tel:${p.phoneNumber}`}>{p.phoneNumber}</a>
            {config.whatsapp && <> · <a href={whatsappLink(p.phoneNumber)} target="_blank" rel="noreferrer">WhatsApp</a></>}
          </div>
          {p.notes && <div className="hint">{p.notes}</div>}
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

function toBody(p: Person) {
  return { name: p.name, contactPerson: p.contactPerson ?? '', phoneNumber: p.phoneNumber, location: p.location ?? '', notes: p.notes ?? '' }
}

function PersonForm({ config, person, onCancel, onSaved }: {
  config: DirectoryConfig
  person: Person | null
  onCancel: () => void
  onSaved: () => void
}) {
  const [f, setF] = useState<FormState>(person ? { ...BLANK, ...toBody(person) } : BLANK)
  const [error, setError] = useState('')
  const [busy, setBusy] = useState(false)
  const set = (k: keyof FormState) => (e: { target: { value: string } }) => setF({ ...f, [k]: e.target.value })

  async function save(e: FormEvent) {
    e.preventDefault()
    setBusy(true)
    setError('')
    try {
      const body = { ...f, contactPerson: config.hasContactPerson ? f.contactPerson : undefined }
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
      <label>{config.locationLabel}
        <input value={f.location} onChange={set('location')} maxLength={500} required />
      </label>
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
