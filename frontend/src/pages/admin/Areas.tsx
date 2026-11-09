import { FormEvent, useCallback, useEffect, useState } from 'react'
import { api } from '../../api'
import { errorText } from '../../format'
import type { Area } from '../../types'

/** Mark which areas are open for pickups and which are "coming soon". */
export default function Areas() {
  const [areas, setAreas] = useState<Area[] | null>(null)
  const [city, setCity] = useState('Bengaluru')
  const [name, setName] = useState('')
  const [open, setOpen] = useState(false)
  const [error, setError] = useState('')
  const [busy, setBusy] = useState(false)
  const [editId, setEditId] = useState<number | null>(null)
  const [editCity, setEditCity] = useState('')
  const [editName, setEditName] = useState('')

  const load = useCallback(() => {
    api<Area[]>('/api/v1/areas').then((a) => { setAreas(a); setError('') }).catch((e) => setError(errorText(e)))
  }, [])
  useEffect(() => { load() }, [load])

  async function add(e: FormEvent) {
    e.preventDefault()
    setBusy(true)
    try {
      await api('/api/v1/areas', { method: 'POST', body: { city, name, open } })
      setName(''); setOpen(false); load()
    } catch (err) {
      setError(errorText(err))
    } finally {
      setBusy(false)
    }
  }

  function startEdit(a: Area) {
    setEditId(a.id); setEditCity(a.city); setEditName(a.name); setError('')
  }

  async function saveEdit(a: Area) {
    try {
      await api(`/api/v1/areas/${a.id}`, { method: 'PUT', body: { city: editCity, name: editName, open: a.open } })
      setEditId(null)
      load()
    } catch (err) {
      setError(errorText(err))
    }
  }

  async function toggle(a: Area) {
    try {
      await api(`/api/v1/areas/${a.id}`, { method: 'PUT', body: { city: a.city, name: a.name, open: !a.open } })
      load()
    } catch (err) {
      setError(errorText(err))
    }
  }

  return (
    <div className="card">
      <h1>Areas</h1>
      <p className="hint">
        Volunteers can only choose hotels in <strong>open</strong> areas. Areas marked “coming soon” are shown on the site but cannot be picked.
        Add hotels to an area from the Hotels page.
      </p>
      {error && <p className="error">{error}</p>}
      {!areas && !error && <p>Loading…</p>}
      <ul className="list">
        {areas?.map((a) => (
          <li key={a.id}>
            {editId === a.id ? (
              <form className="inline-form" onSubmit={(e) => { e.preventDefault(); saveEdit(a) }}>
                <label>City<input value={editCity} onChange={(e) => setEditCity(e.target.value)} maxLength={100} required /></label>
                <label>Area name<input value={editName} onChange={(e) => setEditName(e.target.value)} maxLength={120} required autoFocus /></label>
                <button className="btn small primary">Save</button>
                <button type="button" className="btn small" onClick={() => setEditId(null)}>Cancel</button>
              </form>
            ) : (
              <>
                <span><strong>{a.name}</strong>, {a.city} <em className={'badge ' + (a.open ? 'on' : '')}>{a.open ? 'Open' : 'Coming soon'}</em></span>
                <span className="actions">
                  <button className="btn small" onClick={() => startEdit(a)}>Edit name</button>
                  <button className="btn small" onClick={() => toggle(a)}>{a.open ? 'Mark coming soon' : 'Open this area'}</button>
                </span>
              </>
            )}
          </li>
        ))}
      </ul>

      <form className="form notice card" onSubmit={add}>
        <h2>Add an area</h2>
        <label>City<input value={city} onChange={(e) => setCity(e.target.value)} maxLength={100} required /></label>
        <label>Area name<input value={name} onChange={(e) => setName(e.target.value)} maxLength={120} required placeholder="e.g. Koramangala" /></label>
        <label className="check"><input type="checkbox" checked={open} onChange={(e) => setOpen(e.target.checked)} /> Open for pickups right away</label>
        <button className="btn primary" disabled={busy}>{busy ? 'Adding…' : 'Add area'}</button>
      </form>
    </div>
  )
}
