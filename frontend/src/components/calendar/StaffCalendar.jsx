import { useEffect, useMemo, useState } from 'react'
import { ApiError, getDoctorAppointmentDetails, getDoctorCalendar, getNurseAppointmentDetails, getNurseCalendar, readyNurseAppointment, startNurseAssessment, updateNurseAppointmentStatus } from '../../api/apiClient.js'
import { useTranslation } from '../../i18n/index.jsx'
import { navigate } from '../../utils/navigation.js'

const DAY_NAMES = ['MONDAY', 'TUESDAY', 'WEDNESDAY', 'THURSDAY', 'FRIDAY', 'SATURDAY', 'SUNDAY']
const START_MINUTE = 8 * 60
const END_MINUTE = 20 * 60
const SLOT_MINUTES = 30
const SLOT_HEIGHT = 34

export default function StaffCalendar({ role }) {
  const { t, language } = useTranslation()
  const [view, setView] = useState('week')
  const [viewDate, setViewDate] = useState(todayKey())
  const [data, setData] = useState(null)
  const [selected, setSelected] = useState(null)
  const [details, setDetails] = useState(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [actionBusy, setActionBusy] = useState(false)

  const range = useMemo(() => getRange(view, viewDate), [view, viewDate])
  const load = () => {
    setLoading(true); setError('')
    const loader = role === 'DOCTOR' ? getDoctorCalendar : getNurseCalendar
    loader(range).then(setData).catch((requestError) => setError(messageFor(requestError, t))).finally(() => setLoading(false))
  }
  useEffect(() => { load() }, [role, range.from, range.to])

  const openEvent = async (event) => {
    setSelected(event); setDetails(null); setError('')
    try { setDetails(await (role === 'DOCTOR' ? getDoctorAppointmentDetails(event.id) : getNurseAppointmentDetails(event.id))) } catch (requestError) { setError(messageFor(requestError, t)) }
  }
  const perform = async (action) => {
    setActionBusy(true); setError('')
    try { await action(); load(); if (selected) setDetails(await (role === 'DOCTOR' ? getDoctorAppointmentDetails(selected.id) : getNurseAppointmentDetails(selected.id))) } catch (requestError) { setError(messageFor(requestError, t)) } finally { setActionBusy(false) }
  }
  const shift = (delta) => setViewDate(view === 'month' ? addMonths(viewDate, delta) : addDays(viewDate, view === 'week' ? delta * 7 : delta))
  const title = rangeTitle(view, range, language)
  const events = data?.events || []
  const dates = view === 'month' ? monthCells(viewDate) : datesBetween(range.from, range.to)

  if (loading && !data) return <div className="staff-calendar-state"><span className="loading-dot" /><p>{t('staff.calendarLoading')}</p></div>
  if (error && !data) return <div className="staff-calendar-state staff-calendar-state--error"><p>{error}</p><button className="button button--secondary button--compact" type="button" onClick={load}>{t('staff.retry')}</button></div>
  return <section className="staff-calendar-shell">
    <div className="calendar-toolbar"><div className="calendar-toolbar__nav"><button className="button button--secondary button--compact" type="button" onClick={() => setViewDate(todayKey())}>{t('staff.today')}</button><button className="calendar-arrow" type="button" aria-label={t('staff.previousPeriod')} onClick={() => shift(-1)}>‹</button><button className="calendar-arrow" type="button" aria-label={t('staff.nextPeriod')} onClick={() => shift(1)}>›</button></div><h2>{title}</h2><div className="calendar-toolbar__views">{[['day', t('staff.day')], ['week', t('staff.week')], ['month', t('staff.month')]].map(([value, label]) => <button className={view === value ? 'is-active' : ''} type="button" key={value} onClick={() => setView(value)}>{label}</button>)}</div></div>
    {error && <p className="error-message" role="alert">{error}</p>}
    {!data?.schedule?.intervals?.length ? <div className="staff-calendar-state"><h3>{t('staff.noSchedule')}</h3><p>{t('staff.noScheduleDescription')}</p></div> : view === 'month' ? <MonthView dates={dates} events={events} onSelectDay={(date) => { setViewDate(date); setView('day') }} onOpen={openEvent} t={t} language={language} /> : <TimelineView dates={dates} events={events} schedule={data.schedule} exceptions={data.exceptions} onOpen={openEvent} t={t} language={language} />}
    {selected && <AppointmentDrawer selected={selected} details={details} role={role} busy={actionBusy} onClose={() => { setSelected(null); setDetails(null) }} onPerform={perform} t={t} />}
  </section>
}

function TimelineView({ dates, events, schedule, exceptions, onOpen, t, language }) {
  return <div className={`staff-calendar-timeline ${dates.length === 1 ? 'staff-calendar-timeline--day' : ''}`}><div className="timeline-header"><div className="timeline-time-label">{t('staff.time')}</div>{dates.map((date) => <div className={`timeline-day-label ${date === todayKey() ? 'is-today' : ''}`} key={date}><span>{dates.length === 1 ? formatLongDate(date, language) : formatWeekday(date, language)}</span><strong>{date.slice(8, 10)}</strong></div>)}</div><div className="timeline-body"><div className="timeline-time-column">{Array.from({ length: (END_MINUTE - START_MINUTE) / SLOT_MINUTES + 1 }, (_, index) => <span key={index}>{formatMinutes(START_MINUTE + index * SLOT_MINUTES)}</span>)}</div>{dates.map((date) => <DayColumn key={date} date={date} events={events.filter((item) => item.date === date)} schedule={schedule} exceptions={exceptions} onOpen={onOpen} t={t} />)}</div></div>
}

function DayColumn({ date, events, schedule, exceptions, onOpen, t }) {
  const day = dayOfWeek(date)
  const exception = exceptions?.find((item) => item.date === date)
  const intervals = exception?.type === 'CUSTOM_WORKING_HOURS' ? [exception] : schedule.intervals.filter((item) => item.dayOfWeek === day && !item.breakInterval)
  const breaks = exception ? [] : schedule.intervals.filter((item) => item.dayOfWeek === day && item.breakInterval)
  const working = intervals.length > 0 && !['DAY_OFF', 'VACATION', 'UNAVAILABLE'].includes(exception?.type)
  return <div className={`timeline-day-column ${working ? '' : 'is-off'}`}><div className="timeline-grid-lines">{Array.from({ length: (END_MINUTE - START_MINUTE) / SLOT_MINUTES }, (_, index) => <span key={index} />)}</div>{!working && <div className="timeline-off-label">{exception?.type === 'VACATION' ? t('staff.vacation') : exception?.type === 'CUSTOM_WORKING_HOURS' ? '' : t('staff.dayOff')}</div>}{breaks.map((item) => <div className="timeline-break" key={`${item.startTime}-${item.endTime}`} style={blockStyle(item.startTime, item.endTime)}><span>{t('staff.break')}</span></div>)}{events.map((event) => <button type="button" key={event.id} className={`calendar-event calendar-event--${String(event.status).toLowerCase()}`} style={blockStyle(event.startTime, event.endTime)} onClick={() => onOpen(event)}><strong>{event.startTime} · {event.recipient?.fullName}</strong><span>{statusLabel(event.status, t)}{event.nursePrepared ? ` · ${t('staff.nursePreparedShort')}` : ''}</span></button>)}{date === todayKey() && <div className="current-time-line" style={currentTimeStyle()}><span>{t('staff.now')}</span></div>}</div>
}

function MonthView({ dates, events, onSelectDay, onOpen, t, language }) { return <div className="staff-calendar-month"><div className="month-weekdays">{weekdayLabels(language).map((item) => <span key={item}>{item}</span>)}</div><div className="month-grid">{dates.map((date, index) => { const dayEvents = events.filter((item) => item.date === date); return <button type="button" className={`month-day ${date === todayKey() ? 'is-today' : ''}`} key={date || `empty-${index}`} onClick={() => date && onSelectDay(date)} disabled={!date}><strong>{date ? Number(date.slice(8, 10)) : ''}</strong>{dayEvents.slice(0, 3).map((event) => <span className={`month-event month-event--${String(event.status).toLowerCase()}`} key={event.id} onClick={(click) => { click.stopPropagation(); onOpen(event) }}>{event.startTime} {event.recipient?.fullName}</span>)}{dayEvents.length > 3 && <small>+{dayEvents.length - 3} {t('staff.more')}</small>}</button> })}</div></div> }

function AppointmentDrawer({ selected, details, role, busy, onClose, onPerform, t }) {
  const subject = details?.subject || selected.recipient || {}
  const status = details?.status || selected.status
  const openWorkflow = () => navigate(`/${role === 'DOCTOR' ? 'doctor' : 'nurse'}/appointments?appointmentId=${selected.id}`)
  const openPatient = () => subject.id && navigate(`/doctor/patients/${subject.id}?type=${subject.type || 'SELF'}`)
  const nurseAction = status === 'BOOKED' || status === 'CONFIRMED' ? <button className="button button--secondary button--compact" type="button" disabled={busy} onClick={() => onPerform(() => updateNurseAppointmentStatus(selected.id, { status: 'ARRIVED' }))}>{t('staff.patientArrived')}</button> : status === 'ARRIVED' ? <button className="button button--secondary button--compact" type="button" disabled={busy} onClick={() => onPerform(() => startNurseAssessment(selected.id))}>{t('staff.startPreparation')}</button> : status === 'PREPARING' ? <button className="button button--secondary button--compact" type="button" onClick={openWorkflow}>{t('staff.continuePreparation')}</button> : status === 'READY' ? <span className="drawer-hint">{t('staff.readyForDoctor')}</span> : null
  return <div className="drawer-backdrop" role="presentation" onMouseDown={(event) => { if (event.target === event.currentTarget) onClose() }}><aside className="appointment-drawer" aria-label={t('staff.appointmentPreview')}><div className="appointment-drawer__header"><div><div className="eyebrow">{t('staff.appointmentPreview')}</div><h2>{subject.fullName || selected.recipient?.fullName}</h2></div><button className="drawer-close" type="button" onClick={onClose}>×</button></div><div className="drawer-status-row"><span className={`status status--${String(status).toLowerCase()}`}>{statusLabel(status, t)}</span><span>{selected.startTime}–{selected.endTime}</span></div><dl className="drawer-details"><div><dt>{t('staff.age')}</dt><dd>{subject.age == null ? '—' : `${subject.age} ${t('staff.years')}`}</dd></div><div><dt>{t('staff.gender')}</dt><dd>{genderLabel(subject.gender, t)}</dd></div><div><dt>{t('staff.recipientType')}</dt><dd>{subject.type === 'DEPENDENT' ? t('staff.dependent') : t('staff.patient')}</dd></div><div><dt>{t('staff.clinic')}</dt><dd>{details?.clinic?.name || '—'}</dd></div></dl>{details?.nurseAssessment && <div className="drawer-assessment"><strong>{t('staff.nurseAssessment')}</strong><span>{t('staff.pressure')}: {details.nurseAssessment.bloodPressureSystolic || '—'}/{details.nurseAssessment.bloodPressureDiastolic || '—'} · {t('staff.temperature')}: {details.nurseAssessment.temperature || '—'} · {t('staff.pulse')}: {details.nurseAssessment.pulse || '—'}</span><span>{t('staff.complaints')}: {details.nurseAssessment.complaintSummary || '—'}</span><span>{t('staff.notes')}: {details.nurseAssessment.nurseNotes || '—'}</span></div>}<div className="drawer-actions">{role === 'NURSE' && nurseAction}{role === 'DOCTOR' && <><button className="button button--secondary button--compact" type="button" onClick={openPatient}>Открыть пациента</button><button className="button button--secondary button--compact" type="button" onClick={openWorkflow}>{t('staff.openAppointment')}</button></>}</div></aside></div>
}

function getRange(view, value) { if (view === 'day') return { from: value, to: value }; if (view === 'month') { const first = new Date(`${value.slice(0, 7)}-01T12:00:00`); const last = new Date(first.getFullYear(), first.getMonth() + 1, 0, 12); return { from: toKey(first), to: toKey(last) } } const date = new Date(`${value}T12:00:00`); const monday = new Date(date); const offset = (date.getDay() + 6) % 7; monday.setDate(date.getDate() - offset); const sunday = new Date(monday); sunday.setDate(monday.getDate() + 6); return { from: toKey(monday), to: toKey(sunday) } }
function datesBetween(from, to) { const result = []; for (let date = new Date(`${from}T12:00:00`); date <= new Date(`${to}T12:00:00`); date.setDate(date.getDate() + 1)) result.push(toKey(date)); return result }
function monthCells(value) { const first = new Date(`${value.slice(0, 7)}-01T12:00:00`); const offset = (first.getDay() + 6) % 7; const last = new Date(first.getFullYear(), first.getMonth() + 1, 0, 12); const cells = Array(offset).fill(null); for (let index = 1; index <= last.getDate(); index += 1) cells.push(`${value.slice(0, 7)}-${String(index).padStart(2, '0')}`); while (cells.length % 7) cells.push(null); return cells }
function blockStyle(start, end) { const startMinutes = parseMinutes(start); const endMinutes = parseMinutes(end); return { top: `${Math.max(0, startMinutes - START_MINUTE) / SLOT_MINUTES * SLOT_HEIGHT}px`, height: `${Math.max(1, endMinutes - startMinutes) / SLOT_MINUTES * SLOT_HEIGHT}px` } }
function currentTimeStyle() { const now = new Date(); const minutes = now.getHours() * 60 + now.getMinutes(); return { top: `${Math.max(0, Math.min(END_MINUTE - START_MINUTE, minutes - START_MINUTE)) / SLOT_MINUTES * SLOT_HEIGHT}px` } }
function parseMinutes(value) { const [hours, minutes] = String(value || '00:00').split(':').map(Number); return hours * 60 + minutes }
function formatMinutes(value) { return `${String(Math.floor(value / 60)).padStart(2, '0')}:${String(value % 60).padStart(2, '0')}` }
function formatLongDate(value, language) { return new Intl.DateTimeFormat(localeFor(language), { day: 'numeric', month: 'short' }).format(new Date(`${value}T12:00:00`)) }
function formatWeekday(value, language) { return new Intl.DateTimeFormat(localeFor(language), { weekday: 'short' }).format(new Date(`${value}T12:00:00`)) }
function weekdayLabels(language) { return Array.from({ length: 7 }, (_, index) => formatWeekday(`2024-01-${String(index + 1).padStart(2, '0')}`, language)) }
function localeFor(language) { return language === 'kk' ? 'kk-KZ' : language === 'en' ? 'en-US' : 'ru-RU' }
function rangeTitle(view, range, language) { if (view === 'day') return formatLongDate(range.from, language); if (view === 'month') return new Intl.DateTimeFormat(localeFor(language), { month: 'long', year: 'numeric' }).format(new Date(`${range.from}T12:00:00`)); return `${formatLongDate(range.from, language)}–${formatLongDate(range.to, language)}` }
function addDays(value, amount) { const date = new Date(`${value}T12:00:00`); date.setDate(date.getDate() + amount); return toKey(date) }
function addMonths(value, amount) { const date = new Date(`${value.slice(0, 7)}-01T12:00:00`); date.setMonth(date.getMonth() + amount); return toKey(date) }
function toKey(date) { return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}-${String(date.getDate()).padStart(2, '0')}` }
function todayKey() { return toKey(new Date()) }
function dayOfWeek(value) { return DAY_NAMES[(new Date(`${value}T12:00:00`).getDay() + 6) % 7] }
function statusLabel(value, t) { const key = { BOOKED: 'booked', CONFIRMED: 'confirmed', ARRIVED: 'arrived', PREPARING: 'preparing', READY: 'ready', IN_APPOINTMENT: 'inAppointment', COMPLETED: 'completed', CANCELLED: 'cancelled', NO_SHOW: 'noShow', NURSE_IN_PROGRESS: 'preparing', READY_FOR_DOCTOR: 'ready', DOCTOR_IN_PROGRESS: 'inAppointment' }[value]; return key ? t(`appointment.status.${key}`) : t('staff.unknownStatus') }
function genderLabel(value, t) { return { MALE: t('common.genderMale'), FEMALE: t('common.genderFemale'), OTHER: t('common.genderOther') }[value] || '—' }
function messageFor(error, t) { return error instanceof ApiError ? error.message : error?.message || t('staff.calendarError') }

