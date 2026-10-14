import { useState } from 'react'
import { api } from '../api.js'

export default function ChangePassword() {
  const [open, setOpen] = useState(false)
  const [msg, setMsg] = useState('')
  const [isError, setIsError] = useState(false)

  async function submit(e) {
    e.preventDefault()
    const form = e.currentTarget
    const f = Object.fromEntries(new FormData(form).entries())
    setMsg('')
    try {
      const res = await api('/api/admin/me/password', {
        method: 'POST', body: { currentPassword: f.currentPassword, newPassword: f.newPassword },
      })
      setIsError(false)
      setMsg(res.message)
      form.reset()
    } catch (err) {
      setIsError(true)
      setMsg(err.fields?.newPassword ? 'New password must be at least 8 characters.' : err.message)
    }
  }

  if (!open) return <button className="btn" onClick={() => setOpen(true)}>Change my password</button>
  return (
    <form onSubmit={submit} className="card form">
      <label className="field"><span className="label">Current password</span>
        <input name="currentPassword" type="password" required autoComplete="current-password" /></label>
      <label className="field"><span className="label">New password (8+ characters)</span>
        <input name="newPassword" type="password" required minLength={8} maxLength={72} autoComplete="new-password" /></label>
      {msg && <p className={isError ? 'error banner' : 'hint'}>{msg}</p>}
      <div className="toolbar">
        <button className="btn primary">Save</button>
        <button type="button" className="btn" onClick={() => { setOpen(false); setMsg('') }}>Close</button>
      </div>
    </form>
  )
}
