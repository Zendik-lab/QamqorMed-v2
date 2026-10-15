import { useEffect, useMemo, useState } from 'react'
import { localeFor, useTranslation } from '../../i18n/index.jsx'
import { Input, Select } from './Fields.jsx'
import { Button, EmptyState, PageSkeleton, ErrorState } from './Feedback.jsx'
import { MotionRegion } from '../../motion/Motion.jsx'

export function Pagination({ page, totalPages, onChange, total }) {
  const { t } = useTranslation()
  const pages = Math.max(1, totalPages)
  const current = Math.max(0, Math.min(page, pages - 1))
  return <nav className="ui-pagination" aria-label={t('ui.pagination')}>
    <span className="ui-muted" aria-live="polite">{total !== undefined && `${t('ui.results', { count: total })} · `}{t('ui.page', { page: current + 1, total: pages })}</span>
    <div><Button variant="secondary" disabled={current === 0} onClick={() => onChange(current - 1)} aria-label={t('ui.previous')}>←</Button><Button variant="secondary" disabled={current >= pages - 1} onClick={() => onChange(current + 1)} aria-label={t('ui.next')}>→</Button></div>
  </nav>
}

// For already-loaded directories. Server-paginated staff lists must keep
// their backend filters/pagination rather than sorting a single page locally.
export default function Table({ rows = [], columns, caption, getRowKey = (row) => row.id, searchable = true, pageSize = 10, loading, error, onRetry }) {
  const { t, language } = useTranslation()
  const [query, setQuery] = useState('')
  const [sort, setSort] = useState({ key: '', direction: 'ascending' })
  const [page, setPage] = useState(0)
  const [compact, setCompact] = useState(() => window.matchMedia('(max-width: 600px)').matches)
  useEffect(() => {
    const media = window.matchMedia('(max-width: 600px)')
    const update = () => setCompact(media.matches)
    media.addEventListener('change', update)
    return () => media.removeEventListener('change', update)
  }, [])
  const visible = useMemo(() => {
    const term = query.trim().toLocaleLowerCase(localeFor(language))
    const valueFor = (column, row) => column.value ? column.value(row) : row[column.key]
    const result = rows.filter((row) => !term || columns.some((column) => column.searchable !== false && String(valueFor(column, row) ?? '').toLocaleLowerCase(localeFor(language)).includes(term)))
    const column = columns.find((item) => item.key === sort.key)
    if (column) result.sort((a, b) => {
      const left = valueFor(column, a), right = valueFor(column, b)
      const comparison = typeof left === 'number' && typeof right === 'number' ? left - right : String(left ?? '').localeCompare(String(right ?? ''), localeFor(language), { numeric: true })
      return sort.direction === 'ascending' ? comparison : -comparison
    })
    return result
  }, [rows, columns, query, sort, language])
  const size = Math.max(1, pageSize)
  const pages = Math.ceil(visible.length / size)
  const current = Math.min(page, Math.max(0, pages - 1))
  if (loading) return <PageSkeleton />
  if (error) return <ErrorState description={error} onRetry={onRetry} />
  return <div className="ui-table-shell">
    {searchable && <Input label={t('ui.searchTable')} type="search" value={query} onChange={(event) => { setQuery(event.target.value); setPage(0) }} />}
    {compact && <Select label={t('ui.sortBy')} value={sort.key ? `${sort.key}|${sort.direction}` : ''} onChange={(event) => { const [key, direction] = event.target.value.split('|'); setSort({ key, direction }); setPage(0) }} options={columns.filter((column) => column.sortable !== false).flatMap((column) => ['ascending', 'descending'].map((direction) => [`${column.key}|${direction}`, t(direction === 'ascending' ? 'ui.sortAscending' : 'ui.sortDescending', { column: column.label })]))} />}
    {!visible.length ? <EmptyState title={t(query ? 'ui.noResults' : 'ui.emptyTitle')} description={t(query ? 'ui.noResultsDescription' : 'ui.emptyDescription')} /> : <>
      <MotionRegion replayKey={`${current}|${sort.key}|${sort.direction}|${query}`} variant="results" className="motion-table-results">
      <div className="ui-table-scroll" role="region" aria-label={caption} tabIndex={0}><table className="ui-table" role="table"><caption className="sr-only">{caption}</caption><thead role="rowgroup"><tr role="row">{columns.map((column) => <th role="columnheader" scope="col" key={column.key} aria-sort={sort.key === column.key ? sort.direction : undefined}>{column.sortable === false ? column.label : <button type="button" tabIndex={compact ? -1 : 0} aria-label={t('ui.sort', { column: column.label })} onClick={() => { setSort({ key: column.key, direction: sort.key === column.key && sort.direction === 'ascending' ? 'descending' : 'ascending' }); setPage(0) }}>{column.label}<span aria-hidden="true">{sort.key === column.key ? sort.direction === 'ascending' ? ' ↑' : ' ↓' : ' ↕'}</span></button>}</th>)}</tr></thead><tbody role="rowgroup">{visible.slice(current * size, (current + 1) * size).map((row) => <tr role="row" key={getRowKey(row)}>{columns.map((column) => <td role="cell" key={column.key} data-label={column.label}>{column.render ? column.render(row) : column.value ? column.value(row) : row[column.key]}</td>)}</tr>)}</tbody></table></div>
      </MotionRegion>
      <Pagination page={current} totalPages={pages} total={visible.length} onChange={setPage} />
    </>}
  </div>
}
