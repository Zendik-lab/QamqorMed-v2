import { useEffect, useState } from 'react'
import {
  devGetWorkflowAppointment,
  devRunWorkflowAction,
  devSearchWorkflowAppointments,
} from '../api/apiClient.js'
import { translateError } from '../i18n/index.jsx'

const actions = [
  ['reminder-24h', 'devWorkflow.reminder24h'],
  ['reminder-2h', 'devWorkflow.reminder2h'],
  ['confirmation-request', 'devWorkflow.confirmation'],
  ['no-show', 'devWorkflow.noShow'],
  ['arrived', 'devWorkflow.arrived'],
  ['waitlist-check', 'devWorkflow.waitlist'],
]

export default function DevNotificationWorkflowPanel({ t }) {
  const [filters, setFilters] = useState({ appointmentId: '', patient: '', doctor: '', clinic: '' })
  const [appointments, setAppointments] = useState([])
  const [selectedId, setSelectedId] = useState('')
  const [details, setDetails] = useState(null)
  const [loading, setLoading] = useState(true)
  const [busy, setBusy] = useState('')
  const [error, setError] = useState('')
  const [actionResults, setActionResults] = useState([])

  const search = () => {
    setLoading(true)
    setError('')
    devSearchWorkflowAppointments(filters)
      .then((items) => {
        setAppointments(items || [])
        const nextId = items?.some((item) => String(item.id) === String(selectedId)) ? selectedId : items?.[0]?.id || ''
        setSelectedId(nextId)
        setActionResults([])
        return nextId ? devGetWorkflowAppointment(nextId) : null
      })
      .then(setDetails)
      .catch((requestError) => setError(translateError(requestError, t, 'common.error')))
      .finally(() => setLoading(false))
  }

  useEffect(() => { search() }, [])

  const selectAppointment = (id) => {
    setSelectedId(id)
    setActionResults([])
    setError('')
    devGetWorkflowAppointment(id)
      .then(setDetails)
      .catch((requestError) => setError(translateError(requestError, t, 'common.error')))
  }

  const runAction = (action) => {
    if (!selectedId) return
    setBusy(action)
    setError('')
    devRunWorkflowAction(selectedId, action)
      .then((next) => {
        setDetails(next)
        if (next.lastAction) setActionResults((current) => [next.lastAction, ...current].slice(0, 20))
      })
      .catch((requestError) => setError(translateError(requestError, t, 'common.error')))
      .finally(() => setBusy(''))
  }

  return (
    <section className="dev-workflow-section">
      <div className="dev-results-header">
        <div><span className="dev-section-kicker">{t('devWorkflow.kicker')}</span><h2>{t('devWorkflow.title')}</h2></div>
        <span className="dev-result-count">{t('devWorkflow.demoOnly')}</span>
      </div>
      <p className="dev-workflow-description">{t('devWorkflow.description')}</p>
      <div className="dev-workflow-search">
        {Object.entries(filters).map(([key, value]) => <label className="dev-filter" key={key}><span>{t(`devWorkflow.${key}`)}</span><input value={value} onChange={(event) => setFilters((current) => ({ ...current, [key]: event.target.value }))} placeholder={t(`devWorkflow.${key}Placeholder`)} /></label>)}
        <button className="dev-primary-button" type="button" onClick={search} disabled={loading}>{loading ? t('devWorkflow.searching') : t('devWorkflow.search')}</button>
      </div>
      {error && <p className="error-message" role="alert">{error}</p>}
      {loading && <div className="dev-loading"><span className="dev-spinner" /> {t('devWorkflow.loading')}</div>}
      {!loading && appointments.length === 0 && <div className="dev-empty"><span>⌕</span><h3>{t('devWorkflow.noAppointments')}</h3><p>{t('devWorkflow.noAppointmentsHint')}</p></div>}
      {!loading && appointments.length > 0 && <div className="dev-workflow-layout">
        <div className="dev-workflow-list">{appointments.map((item) => <button key={item.id} type="button" className={`dev-workflow-item ${String(item.id) === String(selectedId) ? 'dev-workflow-item--active' : ''}`} onClick={() => selectAppointment(item.id)}><strong>#{item.id} · {item.patientName}</strong><span>{item.doctorName} · {item.clinicName}</span><small>{item.status} · {formatDate(item.appointmentAt)}</small></button>)}</div>
        {details && <WorkflowDetails details={details} actions={actions} actionResults={actionResults} busy={busy} onAction={runAction} t={t} />}
      </div>}
    </section>
  )
}

function WorkflowDetails({ details, actions, actionResults, busy, onAction, t }) {
  const item = details.appointment
  const result = details.lastAction
  return <div className="dev-workflow-details">
    <div className="dev-workflow-summary"><div><span>{t('devWorkflow.appointment')}</span><strong>#{item.id}</strong></div><div><span>{t('devWorkflow.status')}</span><strong>{item.status}</strong></div><div><span>{t('devWorkflow.confirmationStatus')}</span><strong>{item.confirmationStatus || '—'}</strong></div><div><span>{t('devWorkflow.arrival')}</span><strong>{item.arrivedAt ? formatDate(item.arrivedAt) : '—'}</strong></div><div><span>{t('devWorkflow.patient')}</span><strong>{item.patientName}</strong></div><div><span>{t('devWorkflow.doctor')}</span><strong>{item.doctorName}</strong></div><div><span>{t('devWorkflow.clinic')}</span><strong>{item.clinicName}</strong></div><div><span>{t('devWorkflow.slot')}</span><strong>{item.slotReleased ? t('devWorkflow.released') : t('devWorkflow.occupied')}</strong></div></div>
    <div className="dev-workflow-actions"><h3>{t('devWorkflow.actions')}</h3><div>{actions.map(([action, label]) => <button className="dev-secondary-button" type="button" key={action} onClick={() => onAction(action)} disabled={Boolean(busy)}>{busy === action ? t('devWorkflow.running') : t(label)}</button>)}</div></div>
    {result && <div className={`dev-workflow-result dev-workflow-result--${result.status.toLowerCase()}`}><strong>{result.status}</strong><span>{result.message}</span>{result.details?.map((detail) => <small key={detail}>{detail}</small>)}</div>}
    {actionResults.length > 0 && <Panel title={t('devWorkflow.actions')}><div className="dev-workflow-result-list">{actionResults.map((entry, index) => <p className={`dev-workflow-result-line dev-workflow-result-line--${entry.status.toLowerCase()}`} key={`${entry.executedAt}-${index}`}><strong>{entry.status}</strong> {entry.action}: {entry.message}</p>)}</div></Panel>}
    {details.scheduler && <Panel title={t('devWorkflow.scheduler')}><p>{t('devWorkflow.schedulerAt')}: {formatDate(details.scheduler.evaluatedAt)}</p><p>24h: {details.scheduler.reminder24hAppointmentIds?.join(', ') || '—'} · 2h: {details.scheduler.reminder2hAppointmentIds?.join(', ') || '—'} · NO_SHOW: {details.scheduler.noShowAppointmentIds?.join(', ') || '—'}</p></Panel>}
    <Panel title={t('devWorkflow.history')}>{details.history?.length ? details.history.map((entry) => <p key={entry.id}><strong>{entry.oldStatus || '∅'} → {entry.newStatus}</strong> · {formatDate(entry.changedAt)}{entry.reason ? ` · ${entry.reason}` : ''}</p>) : <p>{t('devWorkflow.empty')}</p>}</Panel>
    <Panel title={t('devWorkflow.notifications')}>{details.notifications?.length ? details.notifications.map((notification) => <p key={notification.id}><strong>{notification.type}</strong> · {notification.title}<small>{notification.message}</small></p>) : <p>{t('devWorkflow.empty')}</p>}</Panel>
    <Panel title={t('devWorkflow.waitlistResults')}>{details.waitlist?.length ? details.waitlist.map((entry) => <p key={entry.id}><strong>#{entry.id} · {entry.status}</strong> · {entry.patientName}</p>) : <p>{t('devWorkflow.empty')}</p>}</Panel>
  </div>
}

function Panel({ title, children }) { return <section className="dev-workflow-panel"><h3>{title}</h3>{children}</section> }
function formatDate(value) { return value ? new Date(value).toLocaleString() : '—' }
