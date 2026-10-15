import { useCallback, useEffect, useMemo, useRef, useState } from 'react'
import {
  getDoctorAppointmentDetails,
  getDoctorCalendar,
  getNurseAppointmentDetails,
  getNurseCalendar,
  startDoctorAppointment,
  startNurseAssessment,
  updateNurseAppointmentStatus,
} from '../../api/apiClient.js'
import { localeFor, translateError, useTranslation } from '../../i18n/index.jsx'
import { navigate } from '../../utils/navigation.js'
import { MotionRegion } from '../../motion/Motion.jsx'

const DAY_NAMES = ['MONDAY', 'TUESDAY', 'WEDNESDAY', 'THURSDAY', 'FRIDAY', 'SATURDAY', 'SUNDAY']
const CLINIC_ZONE = 'Asia/Qyzylorda'
const SLOT_HEIGHT = 34
const STATUS_CODES = ['BOOKED', 'CONFIRMED', 'ARRIVED', 'PREPARING', 'READY', 'IN_APPOINTMENT', 'COMPLETED', 'CANCELLED', 'NO_SHOW']

export default function StaffCalendar({ role }) {
  const { t, language } = useTranslation()
  const initial = useMemo(() => new URLSearchParams(window.location.search), [])
  const [isMobile, setIsMobile] = useState(() => typeof window !== 'undefined' && window.matchMedia?.('(max-width: 700px)').matches)
  const [view, setView] = useState(() => ['day', 'week', 'month'].includes(initial.get('calendarView')) ? initial.get('calendarView') : (isMobile ? 'day' : 'week'))
  const [viewDate, setViewDate] = useState(() => initial.get('calendarDate') || todayKey())
  const [statusFilter, setStatusFilter] = useState('')
  const [query, setQuery] = useState('')
  const [data, setData] = useState(null)
  const [selected, setSelected] = useState(null)
  const [details, setDetails] = useState(null)
  const [detailsLoading, setDetailsLoading] = useState(false)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [actionBusy, setActionBusy] = useState(false)
  const requestId = useRef(0)

  useEffect(() => {
    const media = window.matchMedia('(max-width: 700px)')
    const update = () => setIsMobile(media.matches)
    update()
    media.addEventListener?.('change', update)
    return () => media.removeEventListener?.('change', update)
  }, [])

  useEffect(() => {
    if (isMobile && !initial.get('calendarView')) setView('day')
  }, [initial, isMobile])

  const range = useMemo(() => getRange(view, viewDate), [view, viewDate])
  const load = useCallback(async (silent = false) => {
    const currentRequest = ++requestId.current
    if (!silent) setLoading(true)
    setError('')
    const loader = role === 'DOCTOR' ? getDoctorCalendar : getNurseCalendar
    try {
      const result = await loader({ from: range.from, to: range.to, status: statusFilter || undefined, query: query.trim() || undefined })
      if (currentRequest !== requestId.current) return result
      setData(result)
      return result
    } catch (requestError) {
      if (currentRequest === requestId.current) setError(messageFor(requestError, t))
      return null
    } finally {
      if (currentRequest === requestId.current) setLoading(false)
    }
  }, [role, range.from, range.to, statusFilter, query, t])

  useEffect(() => {
    load()
    const interval = window.setInterval(() => {
      if (document.visibilityState === 'visible') load(true)
    }, 30000)
    return () => window.clearInterval(interval)
  }, [load])

  useEffect(() => {
    if (!selected) return undefined
    const closeOnEscape = (event) => event.key === 'Escape' && closeDetails()
    window.addEventListener('keydown', closeOnEscape)
    return () => window.removeEventListener('keydown', closeOnEscape)
  }, [selected])

  const openEvent = async (event) => {
    setSelected(event)
    setDetails(null)
    setDetailsLoading(true)
    setError('')
    try {
      setDetails(await (role === 'DOCTOR' ? getDoctorAppointmentDetails(event.id) : getNurseAppointmentDetails(event.id)))
    } catch (requestError) {
      setError(messageFor(requestError, t))
    } finally {
      setDetailsLoading(false)
    }
  }

  const perform = async (action) => {
    setActionBusy(true)
    setError('')
    try {
      await action()
      await load(true)
      if (selected) setDetails(await (role === 'DOCTOR' ? getDoctorAppointmentDetails(selected.id) : getNurseAppointmentDetails(selected.id)))
    } catch (requestError) {
      setError(messageFor(requestError, t))
    } finally {
      setActionBusy(false)
    }
  }

  const closeDetails = () => {
    setSelected(null)
    setDetails(null)
  }
  const setCalendarState = (nextView, nextDate) => {
    setView(nextView)
    setViewDate(nextDate)
  }
  const shift = (delta) => setViewDate(view === 'month' ? addMonths(viewDate, delta) : addDays(viewDate, view === 'week' ? delta * 7 : delta))
  const title = rangeTitle(view, range, language)
  const events = data?.events || []
  const dates = view === 'month' ? monthCells(viewDate) : datesBetween(range.from, range.to)
  const timeZone = data?.schedule?.timeZone || CLINIC_ZONE
  const handleDateChange = (event) => setViewDate(event.target.value || todayKey(timeZone))

  if (loading && !data) return <div className="staff-calendar-state"><span className="loading-dot" /><p>{t('staff.calendarLoading')}</p></div>
  if (error && !data) return <div className="staff-calendar-state staff-calendar-state--error"><p>{error}</p><button className="button button--secondary button--compact" type="button" onClick={() => load()}>{t('staff.retry')}</button></div>

  return <section className="staff-calendar-shell">
    <div className="calendar-toolbar">
      <div className="calendar-toolbar__nav">
        <button className="button button--secondary button--compact" type="button" onClick={() => setViewDate(todayKey(timeZone))}>{t('staff.today')}</button>
        <button className="calendar-arrow" type="button" aria-label={t('staff.previousPeriod')} onClick={() => shift(-1)}>‹</button>
        <button className="calendar-arrow" type="button" aria-label={t('staff.nextPeriod')} onClick={() => shift(1)}>›</button>
        <label className="calendar-date-picker"><span className="sr-only">{t('calendarUi.goToDate')}</span><input type="date" value={viewDate} onChange={handleDateChange} /></label>
      </div>
      <h2>{title}</h2>
      <div className="calendar-toolbar__views">{[['day', t('staff.day')], ['week', t('staff.week')], ['month', t('staff.month')]].map(([value, label]) => <button className={view === value ? 'is-active' : ''} type="button" key={value} onClick={() => setView(value)}>{label}</button>)}</div>
    </div>
    <div className="calendar-filters">
      <label className="field-group"><span className="field-label">{t('calendarUi.searchPatient')}</span><input value={query} onChange={(event) => setQuery(event.target.value)} placeholder={t('calendarUi.searchPatientPlaceholder')} /></label>
      <label className="field-group"><span className="field-label">{t('calendarUi.statusFilter')}</span><select value={statusFilter} onChange={(event) => setStatusFilter(event.target.value)}><option value="">{t('calendarUi.allStatuses')}</option>{STATUS_CODES.map((status) => <option value={status} key={status}>{statusLabel(status, t)}</option>)}</select></label>
      <div className="calendar-context"><span>{t('calendarUi.timezone')}: <strong>{timeZone}</strong></span><span>{t('calendarUi.appointmentsCount')}: <strong>{events.length}</strong></span></div>
    </div>
    <StatusLegend t={t} />
    {error && <p className="error-message" role="alert">{error}</p>}
    {!events.length && <div className="calendar-empty-banner"><strong>{t('calendarUi.emptyRangeTitle')}</strong><span>{t('calendarUi.emptyRangeDescription')}</span></div>}
    <MotionRegion replayKey={`${view}|${viewDate}`} className="motion-calendar" aria-busy={loading}>
      {!data?.schedule?.intervals?.length ? <div className="staff-calendar-state"><h3>{t('staff.noSchedule')}</h3><p>{t('staff.noScheduleDescription')}</p></div> : view === 'month' ? <MonthView dates={dates} events={events} onSelectDay={(date) => setCalendarState('day', date)} onOpen={openEvent} t={t} language={language} /> : <TimelineView dates={dates} events={events} schedule={data.schedule} exceptions={data.exceptions} onOpen={openEvent} t={t} language={language} timeZone={timeZone} />}
    </MotionRegion>
    {selected && <AppointmentDrawer selected={selected} details={details} detailsLoading={detailsLoading} role={role} busy={actionBusy} onClose={closeDetails} onPerform={perform} view={view} viewDate={viewDate} t={t} />}
  </section>
}

function StatusLegend({ t }) {
  return <div className="calendar-legend" aria-label={t('calendarUi.statusLegend')}>{STATUS_CODES.map((status) => <span key={status}><i className={`calendar-legend__dot calendar-legend__dot--${status.toLowerCase()}`} />{statusLabel(status, t)}</span>)}</div>
}

function TimelineView({ dates, events, schedule, exceptions, onOpen, t, language, timeZone }) {
  const bounds = useMemo(() => calendarBounds(events, schedule), [events, schedule])
  const step = 30
  const rowCount = Math.max(1, Math.ceil((bounds.end - bounds.start) / step))
  const style = { '--calendar-grid-height': `${rowCount * SLOT_HEIGHT}px`, '--calendar-slot-height': `${SLOT_HEIGHT}px` }
  return <div className={`staff-calendar-timeline ${dates.length === 1 ? 'staff-calendar-timeline--day' : ''}`} style={style}><div className="timeline-header"><div className="timeline-time-label">{t('staff.time')}</div>{dates.map((date) => <div className={`timeline-day-label ${date === todayKey(timeZone) ? 'is-today' : ''}`} key={date}><span>{dates.length === 1 ? formatLongDate(date, language) : formatWeekday(date, language)}</span><strong>{date.slice(8, 10)}</strong></div>)}</div><div className="timeline-body"><div className="timeline-time-column">{timeLabels(bounds, step).map((value) => <span key={value}>{formatMinutes(value)}</span>)}</div>{dates.map((date) => <DayColumn key={date} date={date} events={events.filter((item) => item.date === date)} schedule={schedule} exceptions={exceptions} bounds={bounds} onOpen={onOpen} t={t} timeZone={timeZone} />)}</div></div>
}

function DayColumn({ date, events, schedule, exceptions, bounds, onOpen, t, timeZone }) {
  const day = dayOfWeek(date)
  const exception = exceptions?.find((item) => item.date === date)
  const intervals = exception?.type === 'CUSTOM_WORKING_HOURS' ? [exception] : schedule.intervals.filter((item) => item.dayOfWeek === day && !item.breakInterval)
  const breaks = exception ? [] : schedule.intervals.filter((item) => item.dayOfWeek === day && item.breakInterval)
  const working = intervals.length > 0 && !['DAY_OFF', 'VACATION', 'UNAVAILABLE'].includes(exception?.type)
  const rowCount = Math.max(1, Math.ceil((bounds.end - bounds.start) / 30))
  return <div className={`timeline-day-column ${working ? '' : 'is-off'}`} aria-label={working ? undefined : t(exception?.type === 'VACATION' ? 'staff.vacation' : 'staff.dayOff')}><div className="timeline-grid-lines">{Array.from({ length: rowCount }, (_, index) => <span key={index} />)}</div>{working && intervals.map((item) => <div className="timeline-working-block" key={`${item.startTime}-${item.endTime}`} style={blockStyle(item.startTime, item.endTime, bounds)} />)}{!working && <div className="timeline-off-label">{exception?.type === 'VACATION' ? t('staff.vacation') : exception?.type === 'CUSTOM_WORKING_HOURS' ? '' : t('staff.dayOff')}</div>}{breaks.map((item) => <div className="timeline-break" key={`${item.startTime}-${item.endTime}`} style={blockStyle(item.startTime, item.endTime, bounds)}><span>{t('staff.break')}</span></div>)}{events.map((event) => <button type="button" key={event.id} className={`calendar-event calendar-event--${statusClass(event.status)}`} style={blockStyle(event.startTime, event.endTime, bounds)} onClick={() => onOpen(event)} aria-label={`${event.startTime} ${event.recipient?.fullName || ''}, ${statusLabel(event.status, t)}`}><strong>{event.startTime} · {event.recipient?.fullName}</strong><span>{event.specialty || t('staff.patient')}{event.nursePrepared ? ` · ${t('staff.nursePreparedShort')}` : ''}</span><small>{statusLabel(event.status, t)}</small></button>)}{date === todayKey(timeZone) && <div className="current-time-line" style={currentTimeStyle(bounds, timeZone)}><span>{t('staff.now')}</span></div>}</div>
}

function MonthView({ dates, events, onSelectDay, onOpen, t, language }) {
  return <div className="staff-calendar-month"><div className="month-weekdays">{weekdayLabels(language).map((item) => <span key={item}>{item}</span>)}</div><div className="month-grid">{dates.map((date, index) => { const dayEvents = events.filter((item) => item.date === date); return <button type="button" className={`month-day ${date === todayKey() ? 'is-today' : ''} ${dayEvents.length ? 'has-events' : ''}`} key={date || `empty-${index}`} onClick={() => date && onSelectDay(date)} disabled={!date}><strong>{date ? Number(date.slice(8, 10)) : ''}</strong>{date && <span className="month-day__count">{dayEvents.length}</span>}{dayEvents.slice(0, 3).map((event) => <span className={`month-event month-event--${statusClass(event.status)}`} key={event.id} onClick={(click) => { click.stopPropagation(); onOpen(event) }}><b>{event.startTime}</b> {event.recipient?.fullName}</span>)}{dayEvents.length > 3 && <small>+{dayEvents.length - 3} {t('staff.more')}</small>}</button> })}</div></div>
}

function AppointmentDrawer({ selected, details, detailsLoading, role, busy, onClose, onPerform, view, viewDate, t }) {
  const closeRef = useRef(null)
  const subject = details?.subject || selected.recipient || {}
  const status = canonicalStatus(details?.status || selected.status)
  useEffect(() => closeRef.current?.focus(), [])
  const openWorkflow = () => navigate(`/${role === 'DOCTOR' ? 'doctor' : 'nurse'}/appointments?appointmentId=${selected.id}&calendarView=${view}&calendarDate=${viewDate}`)
  const openPatient = () => subject.id && navigate(`/doctor/patients/${subject.id}?type=${subject.type || 'SELF'}`)
  const nurseAction = status === 'BOOKED' || status === 'CONFIRMED' ? <button className="button button--secondary button--compact" type="button" disabled={busy} onClick={() => onPerform(() => updateNurseAppointmentStatus(selected.id, { status: 'ARRIVED' }))}>{t('staff.patientArrived')}</button> : status === 'ARRIVED' ? <button className="button button--secondary button--compact" type="button" disabled={busy} onClick={() => onPerform(() => startNurseAssessment(selected.id))}>{t('staff.startPreparation')}</button> : status === 'PREPARING' ? <button className="button button--secondary button--compact" type="button" disabled={busy} onClick={openWorkflow}>{t('staff.continuePreparation')}</button> : status === 'READY' ? <span className="drawer-hint">{t('staff.readyForDoctor')}</span> : null
  const doctorAction = status === 'READY' ? <button className="button" type="button" disabled={busy} onClick={() => onPerform(() => startDoctorAppointment(selected.id))}>{t('calendarUi.startAppointment')}</button> : status === 'IN_APPOINTMENT' ? <button className="button" type="button" disabled={busy} onClick={openWorkflow}>{t('calendarUi.continueAppointment')}</button> : null
  return <div className="drawer-backdrop" role="presentation" onMouseDown={(event) => { if (event.target === event.currentTarget) onClose() }}><aside className="appointment-drawer" role="dialog" aria-modal="true" aria-labelledby="appointment-drawer-title"><div className="appointment-drawer__header"><div><div className="eyebrow">{t('staff.appointmentPreview')}</div><h2 id="appointment-drawer-title">{subject.fullName || selected.recipient?.fullName || t('staff.patient')}</h2></div><button ref={closeRef} className="drawer-close" type="button" aria-label={t('common.close')} onClick={onClose}>×</button></div><div className="drawer-status-row"><span className={`status status--${statusClass(status)}`}>{statusLabel(status, t)}</span><span>{selected.startTime}–{selected.endTime}</span></div>{detailsLoading ? <div className="drawer-loading"><span className="loading-dot" /><p>{t('staff.calendarLoading')}</p></div> : <><dl className="drawer-details"><div><dt>{t('staff.age')}</dt><dd>{subject.age == null ? '—' : `${subject.age} ${t('staff.years')}`}</dd></div><div><dt>{t('staff.gender')}</dt><dd>{genderLabel(subject.gender, t)}</dd></div><div><dt>{t('staff.recipientType')}</dt><dd>{subject.type === 'DEPENDENT' ? t('staff.dependent') : t('staff.patient')}</dd></div><div><dt>{t('calendarUi.specialty')}</dt><dd>{details?.doctor?.specialty || selected.specialty || '—'}</dd></div><div><dt>{t('staff.clinic')}</dt><dd>{details?.clinic?.name || '—'}</dd></div><div><dt>{t('calendarUi.note')}</dt><dd>{details?.note || selected.note || '—'}</dd></div></dl>{details?.nurseAssessment && <AssessmentSummary assessment={details.nurseAssessment} t={t} />}<div className="drawer-actions">{role === 'NURSE' && nurseAction}{role === 'DOCTOR' && <>{doctorAction}<button className="button button--secondary button--compact" type="button" onClick={openPatient}>{t('common.open')} {t('staff.patient').toLowerCase()}</button><button className="button button--secondary button--compact" type="button" onClick={openWorkflow}>{status === 'COMPLETED' ? t('calendarUi.openResult') : t('staff.openAppointment')}</button></>}</div></>}</aside></div>
}

function AssessmentSummary({ assessment, t }) {
  return <div className="drawer-assessment"><strong>{t('staff.nurseAssessment')}</strong><div className="drawer-assessment__metrics"><span><small>{t('staff.pressure')}</small>{assessment.bloodPressureSystolic || '—'}/{assessment.bloodPressureDiastolic || '—'}</span><span><small>{t('staff.pulse')}</small>{assessment.pulse || '—'}</span><span><small>{t('staff.temperature')}</small>{assessment.temperature || '—'}</span><span><small>{t('calendarUi.height')}</small>{assessment.height || '—'}</span><span><small>{t('calendarUi.weight')}</small>{assessment.weight || '—'}</span></div><span>{t('staff.complaints')}: {assessment.complaintSummary || '—'}</span><span>{t('staff.notes')}: {assessment.nurseNotes || '—'}</span></div>
}

function getRange(view, value) {
  if (view === 'day') return { from: value, to: value }
  if (view === 'month') {
    const first = new Date(`${value.slice(0, 7)}-01T12:00:00`)
    const last = new Date(first.getFullYear(), first.getMonth() + 1, 0, 12)
    return { from: toKey(first), to: toKey(last) }
  }
  const date = new Date(`${value}T12:00:00`)
  const monday = new Date(date)
  const offset = (date.getDay() + 6) % 7
  monday.setDate(date.getDate() - offset)
  const sunday = new Date(monday)
  sunday.setDate(monday.getDate() + 6)
  return { from: toKey(monday), to: toKey(sunday) }
}

function datesBetween(from, to) { const result = []; for (let date = new Date(`${from}T12:00:00`); date <= new Date(`${to}T12:00:00`); date.setDate(date.getDate() + 1)) result.push(toKey(date)); return result }
function monthCells(value) { const first = new Date(`${value.slice(0, 7)}-01T12:00:00`); const offset = (first.getDay() + 6) % 7; const last = new Date(first.getFullYear(), first.getMonth() + 1, 0, 12); const cells = Array(offset).fill(null); for (let index = 1; index <= last.getDate(); index += 1) cells.push(`${value.slice(0, 7)}-${String(index).padStart(2, '0')}`); while (cells.length % 7) cells.push(null); return cells }
function calendarBounds(events, schedule) { const times = [...(schedule?.intervals || []).flatMap((item) => [item.startTime, item.endTime]), ...(schedule?.exceptions || []).flatMap((item) => [item.startTime, item.endTime]), ...events.flatMap((item) => [item.startTime, item.endTime])].filter(Boolean).map(parseMinutes); const start = Math.floor((Math.min(...times, 8 * 60) - 30) / 30) * 30; const end = Math.ceil((Math.max(...times, 18 * 60) + 30) / 30) * 30; return { start, end: Math.max(start + 60, end) } }
function timeLabels(bounds, step) { const result = []; for (let value = bounds.start; value < bounds.end; value += step) result.push(value); return result }
function blockStyle(start, end, bounds) { const startMinutes = parseMinutes(start); const endMinutes = parseMinutes(end); return { top: `${Math.max(0, startMinutes - bounds.start) / 30 * SLOT_HEIGHT}px`, height: `${Math.max(1, endMinutes - startMinutes) / 30 * SLOT_HEIGHT}px` } }
function currentTimeStyle(bounds, timeZone) { const parts = new Intl.DateTimeFormat('en-GB', { hour: '2-digit', minute: '2-digit', hourCycle: 'h23', timeZone }).formatToParts(new Date()); const hours = Number(parts.find((part) => part.type === 'hour')?.value || 0); const minutes = Number(parts.find((part) => part.type === 'minute')?.value || 0); return { top: `${Math.max(0, Math.min(bounds.end - bounds.start, hours * 60 + minutes - bounds.start)) / 30 * SLOT_HEIGHT}px` } }
function parseMinutes(value) { const [hours, minutes] = String(value || '00:00').split(':').map(Number); return hours * 60 + minutes }
function formatMinutes(value) { return `${String(Math.floor(value / 60)).padStart(2, '0')}:${String(value % 60).padStart(2, '0')}` }
function formatLongDate(value, language) { return new Intl.DateTimeFormat(localeFor(language), { day: 'numeric', month: 'short' }).format(new Date(`${value}T12:00:00`)) }
function formatWeekday(value, language) { return new Intl.DateTimeFormat(localeFor(language), { weekday: 'short' }).format(new Date(`${value}T12:00:00`)) }
function weekdayLabels(language) { return Array.from({ length: 7 }, (_, index) => formatWeekday(`2024-01-${String(index + 1).padStart(2, '0')}`, language)) }
function rangeTitle(view, range, language) { if (view === 'day') return formatLongDate(range.from, language); if (view === 'month') return new Intl.DateTimeFormat(localeFor(language), { month: 'long', year: 'numeric' }).format(new Date(`${range.from}T12:00:00`)); return `${formatLongDate(range.from, language)}–${formatLongDate(range.to, language)}` }
function addDays(value, amount) { const date = new Date(`${value}T12:00:00`); date.setDate(date.getDate() + amount); return toKey(date) }
function addMonths(value, amount) { const date = new Date(`${value.slice(0, 7)}-01T12:00:00`); date.setMonth(date.getMonth() + amount); return toKey(date) }
function toKey(date) { return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}-${String(date.getDate()).padStart(2, '0')}` }
function todayKey(timeZone = CLINIC_ZONE) { const parts = new Intl.DateTimeFormat('en-CA', { timeZone, year: 'numeric', month: '2-digit', day: '2-digit' }).formatToParts(new Date()); return `${parts.find((part) => part.type === 'year')?.value}-${parts.find((part) => part.type === 'month')?.value}-${parts.find((part) => part.type === 'day')?.value}` }
function dayOfWeek(value) { return DAY_NAMES[(new Date(`${value}T12:00:00`).getDay() + 6) % 7] }
function canonicalStatus(value) { return { SCHEDULED: 'BOOKED', CHECKED_IN: 'ARRIVED', NURSE_IN_PROGRESS: 'PREPARING', READY_FOR_DOCTOR: 'READY', DOCTOR_IN_PROGRESS: 'IN_APPOINTMENT', IN_PROGRESS: 'IN_APPOINTMENT' }[value] || value || 'BOOKED' }
function statusLabel(value, t) { const key = { BOOKED: 'booked', CONFIRMED: 'confirmed', ARRIVED: 'arrived', PREPARING: 'preparing', READY: 'ready', IN_APPOINTMENT: 'inAppointment', COMPLETED: 'completed', CANCELLED: 'cancelled', NO_SHOW: 'noShow' }[canonicalStatus(value)]; return key ? t(`appointment.status.${key}`) : t('staff.unknownStatus') }
function statusClass(value) { return String(canonicalStatus(value)).toLowerCase().replaceAll('_', '-') }
function genderLabel(value, t) { return { MALE: t('common.genderMale'), FEMALE: t('common.genderFemale'), OTHER: t('common.genderOther') }[value] || t('common.notSpecified') }
function messageFor(error, t) { return translateError(error, t, 'staff.calendarError') }
