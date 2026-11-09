import { FormEvent, useCallback, useEffect, useState } from 'react'
import { api, PageResponse } from '../api'
import { GChat, GStar } from '../components/Glyphs'
import Pager from '../components/Pager'
import { errorText, formatDateTime } from '../format'
import type { FeedbackKind, MyMessage } from '../types'

const WORDS = ['', 'It was hard', 'Could be better', 'It was fine', 'Good', 'Loved it']

/** Volunteers and hotel staff write to the helpsomeone.in team: feedback (with stars) or any message. */
export default function Feedback() {
  const [kind, setKind] = useState<FeedbackKind>('FEEDBACK')
  const [rating, setRating] = useState(0)
  const [body, setBody] = useState('')
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState('')
  const [sent, setSent] = useState(false)
  const [pageNo, setPageNo] = useState(0)
  const [page, setPage] = useState<PageResponse<MyMessage> | null>(null)

  const load = useCallback(() => {
    api<PageResponse<MyMessage>>(`/api/v1/account/messages?page=${pageNo}&size=10`).then(setPage).catch(() => setPage(null))
  }, [pageNo])
  useEffect(() => { load() }, [load])

  async function submit(e: FormEvent) {
    e.preventDefault()
    setBusy(true); setError(''); setSent(false)
    try {
      await api('/api/v1/account/messages', {
        method: 'POST',
        body: { kind, rating: kind === 'FEEDBACK' && rating > 0 ? rating : undefined, body },
      })
      setBody(''); setRating(0); setSent(true)
      setPageNo(0); load()
    } catch (err) {
      setError(errorText(err))
    } finally {
      setBusy(false)
    }
  }

  return (
    <div className="narrow">
      <section className="panel">
        <h1>Tell the helpsomeone.in team</h1>
        <p>Your words help us do this better. Share what went well, what was hard, or anything you want us to know.</p>
      </section>

      <form className="card form fb-form" onSubmit={submit}>
        <div className="choice-row" role="radiogroup" aria-label="What are you sending?">
          <button type="button" role="radio" aria-checked={kind === 'FEEDBACK'} className={'choice' + (kind === 'FEEDBACK' ? ' on' : '')} onClick={() => setKind('FEEDBACK')}>
            <GStar /><strong>Feedback</strong><span>How did it go? Give stars and tell us.</span>
          </button>
          <button type="button" role="radio" aria-checked={kind === 'MESSAGE'} className={'choice' + (kind === 'MESSAGE' ? ' on' : '')} onClick={() => setKind('MESSAGE')}>
            <GChat /><strong>Message</strong><span>Ask something, or tell us about a problem.</span>
          </button>
        </div>

        {kind === 'FEEDBACK' && (
          <div className="rate">
            <div className="stars" role="radiogroup" aria-label="How was your experience?">
              {[1, 2, 3, 4, 5].map((n) => (
                <button key={n} type="button" role="radio" aria-checked={rating === n} aria-label={`${n} of 5`}
                        className={'star' + (n <= rating ? ' on' : '')} onClick={() => setRating(rating === n ? 0 : n)}>★</button>
              ))}
            </div>
            <span className="rate-word">{rating ? WORDS[rating] : 'Tap a star (optional)'}</span>
          </div>
        )}

        <label>{kind === 'FEEDBACK' ? 'What went well? What can we improve?' : 'Your message to the team'}
          <textarea value={body} onChange={(e) => setBody(e.target.value)} maxLength={2000} rows={5} required
                    placeholder={kind === 'FEEDBACK' ? 'Tell us in your own words…' : 'Write here…'} />
        </label>
        <div className="form-foot">
          <span className="hint">{body.length} / 2000</span>
          <button className="btn primary big" disabled={busy}>{busy ? 'Sending…' : 'Send to the team'}</button>
        </div>
        {error && <p className="error" role="alert">{error}</p>}
        {sent && <p className="ok" role="status">Thank you 🙏 We have received it and will read it with care.</p>}
      </form>

      {page && page.content.length > 0 && (
        <section>
          <h2 className="section-title">What you sent</h2>
          {page.content.map((m) => (
            <article className={'msg mine' + (m.status === 'DONE' ? '' : ' new')} key={m.id}>
              <header>
                <div className="person">
                  <h3>{m.kind === 'FEEDBACK' ? 'Feedback' : 'Message'}</h3>
                  <span className="sub">{formatDateTime(m.createdAt)}</span>
                </div>
                <span className={'chip ' + (m.status === 'DONE' ? 'open' : 'soon')}>{m.status === 'DONE' ? 'Read by the team' : 'Sent'}</span>
              </header>
              {m.rating ? <div className="rating" aria-label={`${m.rating} of 5 stars`}>{'★'.repeat(m.rating)}<span className="dim">{'★'.repeat(5 - m.rating)}</span></div> : null}
              <p className="body">{m.body}</p>
            </article>
          ))}
          <Pager page={page} onPage={setPageNo} />
        </section>
      )}
    </div>
  )
}
