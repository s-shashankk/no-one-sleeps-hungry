import { ReactNode } from 'react'
import { Navigate, useLocation } from 'react-router-dom'
import { useAuth } from './auth'
import type { Role } from './types'

/** Where each kind of person starts. */
export function homeFor(role: Role): string {
  return role === 'ADMIN' ? '/admin' : role === 'HOTEL' ? '/hotel' : '/me'
}

/**
 * Shows the page only to a signed-in person with the right role (default: admin).
 * The server enforces this too; this just sends people to the right place.
 */
export default function RequireAuth({ children, role = 'ADMIN', roles }: { children: ReactNode; role?: Role; roles?: Role[] }) {
  const { me } = useAuth()
  const location = useLocation()

  if (me === undefined) return <p>Loading…</p>
  if (me === null) return <Navigate to="/login" replace state={{ from: location.pathname + location.search }} />
  if (!(roles ?? [role]).includes(me.role)) return <Navigate to={homeFor(me.role)} replace />
  return <>{children}</>
}
