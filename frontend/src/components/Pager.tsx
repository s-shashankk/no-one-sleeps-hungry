import type { PageResponse } from '../api'

/** Previous / next buttons under a list. Shows nothing when everything fits on one page. */
export default function Pager({ page, onPage }: { page: PageResponse<unknown>; onPage: (p: number) => void }) {
  if (page.totalPages <= 1) return null
  return (
    <div className="actions pager">
      <button className="btn small" disabled={page.page <= 0} onClick={() => onPage(page.page - 1)}>← Previous</button>
      <span className="hint">Page {page.page + 1} of {page.totalPages} · {page.totalElements} in total</span>
      <button className="btn small" disabled={page.page + 1 >= page.totalPages} onClick={() => onPage(page.page + 1)}>Next →</button>
    </div>
  )
}
