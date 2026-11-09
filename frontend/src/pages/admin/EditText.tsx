import { useEffect, useMemo, useState } from 'react'
import { Link } from 'react-router-dom'
import { api } from '../../api'
import { errorText } from '../../format'
import { DEFAULT_TEXT, TEXT_GROUPS } from '../../textDefaults'
import { useTexts } from '../../texts'

/**
 * Admin: change the words on the public pages. Only the words change, never the size or layout.
 * Saving sends only what differs; putting the original wording back removes the change in the database.
 */
export default function EditText() {
  const { t, edited, setEdited } = useTexts()
  const [draft, setDraft] = useState<Record<string, string>>({})
  const [error, setError] = useState('')
  const [saved, setSaved] = useState('')
  const [busy, setBusy] = useState(false)

  // start (and restart after saving) from what visitors see right now
  useEffect(() => {
    const now: Record<string, string> = {}
    TEXT_GROUPS.forEach((g) => g.fields.forEach((f) => { now[f.key] = t(f.key) }))
    setDraft(now)
  }, [edited]) // eslint-disable-line

  const changed = useMemo(
    () => TEXT_GROUPS.flatMap((g) => g.fields).filter((f) => draft[f.key] !== undefined && draft[f.key] !== t(f.key)),
    [draft, t],
  )

  function set(key: string, value: string) {
    setSaved('')
    setDraft((d) => ({ ...d, [key]: value }))
  }

  async function save() {
    setBusy(true); setError(''); setSaved('')
    const values: Record<string, string> = {}
    changed.forEach((f) => {
      const v = draft[f.key].trim()
      values[f.key] = v === '' || v === DEFAULT_TEXT[f.key] ? '' : v     // '' puts the original wording back
    })
    try {
      const all = await api<Record<string, string>>('/api/v1/texts', { method: 'PUT', body: { values } })
      setEdited(all)
      setSaved('Saved. Visitors see the new words now.')
    } catch (e) {
      setError(errorText(e))
    } finally {
      setBusy(false)
    }
  }

  return (
    <>
      <div className="card">
        <div className="row-between">
          <div>
            <h1>Site text</h1>
            <p className="hint">Change any words on the public pages. Sizes and layout stay exactly the same. Clear a box, or press “Original”, to bring the first wording back.</p>
          </div>
          <Link className="btn" to="/" target="_blank" rel="noreferrer">Open the website</Link>
        </div>
      </div>

      {TEXT_GROUPS.map((g) => (
        <section className="card text-group" key={g.title}>
          <h2>{g.title}</h2>
          {g.hint && <p className="hint">{g.hint}</p>}
          {g.fields.map((f) => {
            const isEdited = edited[f.key] !== undefined
            const value = draft[f.key] ?? t(f.key)
            return (
              <div className="text-field" key={f.key}>
                <div className="text-field-top">
                  <label htmlFor={f.key}>{f.label}</label>
                  {isEdited && <em className="badge on">Edited</em>}
                  {value !== f.text && <button type="button" className="linklike dark small" onClick={() => set(f.key, f.text)}>Original</button>}
                </div>
                {f.long
                  ? <textarea id={f.key} rows={3} maxLength={1500} value={value} onChange={(e) => set(f.key, e.target.value)} />
                  : <input id={f.key} maxLength={300} value={value} onChange={(e) => set(f.key, e.target.value)} />}
              </div>
            )
          })}
        </section>
      ))}

      <div className="savebar" role="region" aria-label="Save changes">
        <span>
          {error ? <span className="error">{error}</span>
            : saved ? <span className="ok-text">{saved}</span>
            : changed.length === 0 ? 'No unsaved changes.'
            : `${changed.length} ${changed.length === 1 ? 'change' : 'changes'} not saved yet.`}
        </span>
        <button className="btn primary" disabled={busy || changed.length === 0} onClick={save}>{busy ? 'Saving…' : 'Save changes'}</button>
      </div>
    </>
  )
}
