import { ReactNode } from 'react'
import { Navigate, useLocation } from 'react-router-dom'
import { useAuth } from './auth'

/** Shows the page only to a signed-in admin. The server enforces this too. */
export default function RequireAuth({ children }: { children: ReactNode }) {
  const { me } = useAuth()
  const location = useLocation()

  if (me === undefined) return <p>Loading…</p>
  if (me === null) return <Navigate to="/login" replace state={{ from: location.pathname + location.search }} />
  return <>{children}</>
}
