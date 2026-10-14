import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { api } from './api.js'

export function Field({ label, name, errors, children, hint }) {
  return (
    <label className="field">
      <span className="label">{label}</span>
      {children}
      {hint && <span className="hint">{hint}</span>}
      {errors?.[name] && <span className="error">{errors[name]}</span>}
    </label>
  )
}

// Optional "nearest locality" dropdown, filled from the localities the Super Admin has created.
export function LocalitySelect({ errors }) {
  const [list, setList] = useState([])
  useEffect(() => {
    api('/api/public/localities').then(setList).catch(() => setList([]))
  }, [])
  if (list.length === 0) return null
  return (
    <Field label="Nearest locality" name="localityId" errors={errors}>
      <select name="localityId" defaultValue="">
        <option value="">Not sure / not listed</option>
        {list.map((l) => <option key={l.id} value={l.id}>{l.name}, {l.city}</option>)}
      </select>
    </Field>
  )
}

// Hidden honeypot: humans never see/fill it, bots do.
export function Honeypot() {
  return (
    <div style={{ position: 'absolute', left: '-9999px' }} aria-hidden="true">
      <input type="text" name="website" tabIndex={-1} autoComplete="off" />
    </div>
  )
}

/**
 * Generic form: collects every named input, lets `transform` shape the
 * JSON body, posts it, and shows a thank-you message on success.
 */
export default function ApiForm({ endpoint, transform, submitLabel, children }) {
  const [busy, setBusy] = useState(false)
  const [errors, setErrors] = useState({})
  const [message, setMessage] = useState('')
  const [done, setDone] = useState(false)

  async function onSubmit(e) {
    e.preventDefault()
    setBusy(true)
    setErrors({})
    setMessage('')
    const raw = Object.fromEntries(new FormData(e.currentTarget).entries())
    try {
      const res = await api(endpoint, { method: 'POST', body: transform(raw) })
      setMessage(res.message)
      setDone(true)
    } catch (err) {
      setErrors(err.fields || {})
      setMessage(err.message)
    } finally {
      setBusy(false)
    }
  }

  if (done) {
    return (
      <div className="card success">
        <h2>Thank you 🙏</h2>
        <p>{message}</p>
        <Link className="btn" to="/">Back to home</Link>
      </div>
    )
  }

  return (
    <form onSubmit={onSubmit} className="card form">
      {typeof children === 'function' ? children({ errors }) : children}
      <Honeypot />
      {message && <p className="error banner">{message}</p>}
      <button className="btn primary" disabled={busy}>{busy ? 'Sending…' : submitLabel}</button>
    </form>
  )
}

export const num = (v) => (v === '' || v == null ? null : Number(v))
export const text = (v) => (v == null ? '' : v)
