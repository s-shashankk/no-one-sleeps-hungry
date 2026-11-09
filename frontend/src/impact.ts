import { useEffect, useState } from 'react'
import { api } from './api'
import type { Impact } from './types'

/** The public numbers (meals served, areas, hotels). `failed` is true when the server could not be reached. */
export function useImpact(): { impact: Impact | null; failed: boolean } {
  const [impact, setImpact] = useState<Impact | null>(null)
  const [failed, setFailed] = useState(false)
  useEffect(() => {
    let live = true
    api<Impact>('/api/v1/public/impact')
      .then((x) => { if (live) setImpact(x) })
      .catch(() => { if (live) setFailed(true) })
    return () => { live = false }
  }, [])
  return { impact, failed }
}
