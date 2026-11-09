import { createContext, ReactNode, useCallback, useContext, useEffect, useMemo, useState } from 'react'
import { api } from './api'
import { DEFAULT_TEXT } from './textDefaults'

const CACHE_KEY = 'mealup.texts'

interface TextsValue {
  /** The wording for a key: what the admin wrote, or the built-in wording. */
  t: (key: string) => string
  /** Only the texts the admin has changed. */
  edited: Record<string, string>
  setEdited: (all: Record<string, string>) => void
}

const Ctx = createContext<TextsValue>({ t: (k) => DEFAULT_TEXT[k] ?? '', edited: {}, setEdited: () => undefined })

function remembered(): Record<string, string> {
  try {
    const raw = localStorage.getItem(CACHE_KEY)
    return raw ? (JSON.parse(raw) as Record<string, string>) : {}
  } catch {
    return {}
  }
}

/** Loads the admin's edited wording once, and keeps a copy so the next visit does not flash the old words. */
export function TextsProvider({ children }: { children: ReactNode }) {
  const [edited, setEditedState] = useState<Record<string, string>>(remembered)

  const setEdited = useCallback((all: Record<string, string>) => {
    setEditedState(all)
    try { localStorage.setItem(CACHE_KEY, JSON.stringify(all)) } catch { /* storage may be blocked; that is fine */ }
  }, [])

  useEffect(() => {
    api<Record<string, string>>('/api/v1/public/texts').then(setEdited).catch(() => undefined)
  }, [setEdited])

  const value = useMemo<TextsValue>(() => ({
    t: (key) => edited[key] ?? DEFAULT_TEXT[key] ?? '',
    edited,
    setEdited,
  }), [edited, setEdited])

  return <Ctx.Provider value={value}>{children}</Ctx.Provider>
}

export function useTexts(): TextsValue {
  return useContext(Ctx)
}
