import { EmptyState } from '../components/ui/Feedback.jsx'
import { useEffect, useMemo, useState } from 'react'
import {
  getDependents,
  getDoctorCalendar,
  getDoctorProfile,
  getNurseCalendar,
  getNurseProfile,
  getPatientAppointments,
  getPatientCertificates,
  getPatientDiagnoses,
  getPatientLabResults,
  getPatientMedicalRecords,
  getPatientPrescriptions,
  getPatientProfile,
} from '../api/apiClient.js'
import { useAuth } from '../auth/AuthContext.jsx'
import { localeFor, translateError, useTranslation } from '../i18n/index.jsx'
import { navigate } from '../utils/navigation.js'

const roleContent = {
  PATIENT: { title: 'dashboard.patientTitle', description: 'dashboard.patientDescription' },
  DOCTOR: { title: 'dashboard.doctorTitle', description: 'dashboard.doctorDescription' },
  NURSE: { title: 'dashboard.nurseTitle', description: 'dashboard.nurseDescription' },
  ADMIN: { title: 'dashboard.adminTitle', description: 'dashboard.adminDescription' },
}

function DashboardPage() {
  const { user } = useAuth()
  const { t, language } = useTranslation()

  if (user.role === 'PATIENT') return <PatientDashboard user={user} t={t} language={language} />
  if (user.role === 'DOCTOR' || user.role === 'NURSE') return <StaffDashboard role={user.role} user={user} t={t} language={language} />

  const content = roleContent[user.role] || roleContent.PATIENT
  const fullName = `${user.firstName} ${user.lastName}`.trim()
  const initials = `${user.firstName?.[0] || ''}${user.lastName?.[0] || ''}`.toUpperCase()

  return (
    <section className="page-shell dashboard-home">
      <div className="welcome-panel">
        <div className="welcome-copy">
          <div className="eyebrow">{t(`roles.${user.role}`)}</div>
          <h1>{t(content.title)}</h1>
          <p className="dashboard-welcome">{t('dashboard.welcome', { name: user.firstName })}</p>
          <p className="form-description">{t(content.description)}</p>
          {user.role === 'ADMIN' && <button className="button button--compact" type="button" onClick={() => navigate('/admin')}>{t('dashboard.adminManagement')}</button>}
        </div>
        <div className="welcome-avatar" aria-label={fullName}>{initials}</div>
      </div>

      {user.role === 'ADMIN' && <InfoCards t={t} admin />}
    </section>
  )
}

function PatientDashboard({ user, t, language }) {
  const [data, setData] = useState({ profile: null, appointments: [], dependents: [], records: [], diagnoses: [], prescriptions: [], labs: [], certificates: [] })
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')

  useEffect(() => {
    let active = true
    const optional = (request, fallback = []) => request.then((value) => value).catch(() => fallback)
    Promise.all([
      getPatientProfile(),
      optional(getPatientAppointments()),
      optional(getDependents()),
      optional(getPatientMedicalRecords()),
      optional(getPatientDiagnoses()),
      optional(getPatientPrescriptions()),
      optional(getPatientLabResults()),
      optional(getPatientCertificates()),
    ])
      .then(([profile, appointments, dependents, records, diagnoses, prescriptions, labs, certificates]) => {
        if (active) setData({ profile, appointments, dependents, records, diagnoses, prescriptions, labs, certificates })
      })
      .catch((requestError) => { if (active) setError(translateError(requestError, t, 'patientDashboard.dataLoadError')) })
      .finally(() => { if (active) setLoading(false) })
    return () => { active = false }
  }, [])

  const upcoming = useMemo(() => data.appointments
    .filter((item) => isUpcomingAppointment(item))
    .sort((left, right) => String(left.appointmentAt).localeCompare(String(right.appointmentAt))), [data.appointments])
  const latest = useMemo(() => [
    data.records[0] && { key: 'visit', icon: '◷', title: t('patientDashboard.lastVisit'), value: data.records[0].diagnosis || t('patientDashboard.visitCompleted'), meta: data.records[0].doctor?.fullName },
    data.diagnoses[0] && { key: 'diagnosis', icon: '◇', title: t('patientDashboard.lastDiagnosis'), value: data.diagnoses[0].icd10Code?.name, meta: data.diagnoses[0].icd10Code?.code },
    data.prescriptions[0] && { key: 'prescription', icon: '⌁', title: t('patientDashboard.lastPrescription'), value: data.prescriptions[0].title, meta: data.prescriptions[0].instructions },
    data.labs[0] && { key: 'lab', icon: '▥', title: t('patientDashboard.lastLab'), value: data.labs[0].testName, meta: data.labs[0].result || data.labs[0].status },
    data.certificates[0] && { key: 'certificate', icon: '□', title: t('patientDashboard.lastCertificate'), value: data.certificates[0].type, meta: data.certificates[0].description },
  ].filter(Boolean).slice(0, 4), [data, t])

  const firstName = data.profile?.firstName || user.firstName || t('roles.PATIENT').toLowerCase()
  const profileName = [data.profile?.firstName, data.profile?.lastName].filter(Boolean).join(' ')
  const initials = `${data.profile?.firstName?.[0] || user.firstName?.[0] || ''}${data.profile?.lastName?.[0] || user.lastName?.[0] || ''}`.toUpperCase()

  if (loading) return <PatientDashboardSkeleton />

  return (
    <section className="page-shell patient-dashboard">
      <div className="patient-dashboard__hero">
        <div>
          <div className="eyebrow">{t('roles.PATIENT')} · QamqorMed</div>
          <h1>{t('patientDashboard.patientGreeting', { name: firstName })}</h1>
          <p>{t('patientDashboard.patientSummary')}</p>
          <div className="patient-dashboard__hero-actions">
            <button className="button button--compact" type="button" onClick={() => navigate('/patient/appointments')}>{t('patientDashboard.bookDoctor')}</button>
            <button className="button button--ghost button--compact" type="button" onClick={() => navigate('/patient/profile')}>{t('patientDashboard.openProfile')}</button>
          </div>
        </div>
        <div className="welcome-avatar" aria-label={profileName || firstName}>{initials}</div>
      </div>

      {error && <p className="error-message" role="alert">{error}</p>}

      <div className="patient-dashboard__columns">
        <NextAppointmentCard appointment={upcoming[0]} t={t} language={language} />
        <section className="patient-card patient-quick-actions" aria-labelledby="quick-actions-title">
          <div className="section-heading"><div><div className="eyebrow">{t('patientDashboard.navigation')}</div><h2 id="quick-actions-title">{t('patientDashboard.quickActions')}</h2></div></div>
          <div className="patient-quick-actions__grid">
            <QuickAction icon="＋" title={t('patientDashboard.bookDoctor')} description={t('patientDashboard.findSpecialist')} onClick={() => navigate('/patient/appointments')} />
            <QuickAction icon="◷" title={t('nav.myAppointments')} description={t('patientDashboard.myAppointmentsDescription')} onClick={() => navigate('/patient/my-appointments')} />
            <QuickAction icon="▤" title={t('nav.medicalRecord')} description={t('patientDashboard.medicalRecordDescription')} onClick={() => navigate('/patient/medical-record')} />
            <QuickAction icon="♧" title={t('nav.dependents')} description={t('patientDashboard.dependentsDescription', { count: data.dependents.length || t('patientDashboard.none') })} onClick={() => navigate('/patient/dependents')} />
          </div>
        </section>
      </div>

      <section className="patient-card patient-dashboard__appointments">
        <div className="section-heading"><div><div className="eyebrow">{t('patientDashboard.yourSchedule')}</div><h2>{t('patientDashboard.upcomingAppointments')}</h2></div><button className="text-button" type="button" onClick={() => navigate('/patient/my-appointments')}>{t('patientDashboard.allAppointments')}</button></div>
        {upcoming.length ? <div className="patient-appointment-list">{upcoming.slice(0, 5).map((item) => <PatientAppointmentItem item={item} key={item.id} t={t} language={language} />)}</div> : <EmptyState title={t('patientDashboard.noUpcoming')} description={t('patientDashboard.bookToChooseTime')} action={t('patientDashboard.bookDoctor')} onAction={() => navigate('/patient/appointments')} compact />}
      </section>

      <section className="patient-card patient-dashboard__medical">
        <div className="section-heading"><div><div className="eyebrow">{t('patientDashboard.yourHistory')}</div><h2>{t('patientDashboard.latestMedicalInfo')}</h2></div><button className="text-button" type="button" onClick={() => navigate('/patient/medical-record')}>{t('patientDashboard.openRecord')}</button></div>
        {latest.length ? <div className="patient-medical-grid">{latest.map((item) => <article className="patient-medical-card" key={item.key}><span className="patient-icon patient-icon--soft">{item.icon}</span><div><span>{item.title}</span><strong>{item.value}</strong>{item.meta && <small>{item.meta}</small>}</div></article>)}</div> : <EmptyState title={t('patientDashboard.medicalInfoEmpty')} description={t('patientDashboard.medicalInfoDescription')} onAction={() => navigate('/patient/appointments')} action={t('patientDashboard.bookDoctor')} compact />}
      </section>
    </section>
  )
}

function NextAppointmentCard({ appointment, t, language }) {
  return <section className="patient-card patient-next-card"><div className="section-heading"><div><div className="eyebrow">{t('patientDashboard.nextAppointment')}</div><h2>{t('patientDashboard.nearestAppointment')}</h2></div><span className="patient-icon">◷</span></div>{appointment ? <><div className="patient-next-card__time">{formatAppointmentDate(appointment.appointmentAt, language)}<strong>{formatAppointmentTime(appointment.appointmentAt)}</strong></div><div className="patient-next-card__doctor"><div className="profile-avatar">{appointment.doctor?.fullName?.split(' ').map((part) => part[0]).join('').slice(0, 2)}</div><div><strong>{appointment.doctor?.fullName || t('patientDashboard.doctor')}</strong><span>{appointment.doctor?.specialty || t('patientDashboard.specialist')}</span></div></div><dl className="patient-meta-list"><div><dt>{t('patientDashboard.clinic')}</dt><dd>{appointment.clinic?.name || '—'}</dd></div><div><dt>{t('patientDashboard.patient')}</dt><dd>{appointment.subject?.fullName || t('patientDashboard.you')}</dd></div></dl><div className="patient-next-card__footer"><span className={`status status--${String(appointment.status).toLowerCase()}`}>{statusLabel(appointment.status, t)}</span><button className="text-button" type="button" onClick={() => navigate('/patient/my-appointments')}>{t('patientDashboard.details')}</button></div></> : <EmptyState title={t('patientDashboard.noUpcoming')} description={t('patientDashboard.bookToChooseTime')} action={t('patientDashboard.bookDoctor')} onAction={() => navigate('/patient/appointments')} compact />}</section>
}

function PatientAppointmentItem({ item, t, language }) {
  return <article className="patient-appointment-item"><div className="patient-appointment-item__date"><strong>{formatAppointmentDay(item.appointmentAt)}</strong><span>{formatAppointmentMonth(item.appointmentAt, language)}</span></div><div className="patient-appointment-item__body"><strong>{item.doctor?.fullName || t('patientDashboard.doctor')}</strong><span>{item.doctor?.specialty || t('patientDashboard.specialist')} · {item.clinic?.name || t('patientDashboard.clinic')}</span><small>{formatAppointmentDate(item.appointmentAt, language)} · {formatAppointmentTime(item.appointmentAt)} · {item.subject?.fullName || t('patientDashboard.you')}</small></div><span className={`status status--${String(item.status).toLowerCase()}`}>{statusLabel(item.status, t)}</span></article>
}

function QuickAction({ icon, title, description, onClick }) {
  return <button className="patient-quick-action" type="button" onClick={onClick}><span className="patient-icon patient-icon--soft">{icon}</span><span><strong>{title}</strong><small>{description}</small></span><span className="patient-quick-action__arrow">→</span></button>
}


function PatientDashboardSkeleton() {
  return <section className="page-shell patient-dashboard"><div className="patient-dashboard__skeleton-hero"><div><span className="skeleton skeleton--eyebrow" /><span className="skeleton skeleton--title" /><span className="skeleton skeleton--line" /></div><span className="skeleton skeleton--avatar" /></div><div className="patient-dashboard__columns"><div className="patient-card patient-skeleton-card"><span className="skeleton skeleton--line" /><span className="skeleton skeleton--title" /><span className="skeleton skeleton--line" /><span className="skeleton skeleton--line" /></div><div className="patient-card patient-skeleton-card"><span className="skeleton skeleton--line" /><span className="skeleton skeleton--title" /><span className="skeleton skeleton--line" /><span className="skeleton skeleton--line" /></div></div></section>
}

function isUpcomingAppointment(item) { return !['COMPLETED', 'CANCELLED', 'NO_SHOW'].includes(item.status) && String(item.appointmentAt || '').slice(0, 16) >= clinicNowKey() }
function clinicNowKey() { const parts = new Intl.DateTimeFormat('en-CA', { timeZone: 'Asia/Qyzylorda', year: 'numeric', month: '2-digit', day: '2-digit', hour: '2-digit', minute: '2-digit', hourCycle: 'h23' }).formatToParts(new Date()).reduce((result, item) => ({ ...result, [item.type]: item.value }), {}); return `${parts.year}-${parts.month}-${parts.day}T${parts.hour}:${parts.minute}` }
function formatAppointmentDate(value, language = 'ru') { const dateKey = String(value || '').slice(0, 10); if (!dateKey) return '—'; const [year, month, day] = dateKey.split('-').map(Number); return new Intl.DateTimeFormat(localeFor(language), { day: 'numeric', month: 'long', year: 'numeric' }).format(new Date(year, month - 1, day, 12)) }
function formatAppointmentDay(value) { return String(value || '').slice(8, 10) || '—' }
function formatAppointmentMonth(value, language = 'ru') { const dateKey = String(value || '').slice(0, 10); if (!dateKey) return ''; const [year, month, day] = dateKey.split('-').map(Number); return new Intl.DateTimeFormat(localeFor(language), { month: 'short' }).format(new Date(year, month - 1, day, 12)).replace('.', '') }
function formatAppointmentTime(value) { return String(value || '').slice(11, 16) || '—' }
function statusLabel(value, t) { const key = { BOOKED: 'booked', CONFIRMED: 'confirmed', ARRIVED: 'arrived', PREPARING: 'preparing', READY: 'ready', IN_APPOINTMENT: 'inAppointment', COMPLETED: 'completed', CANCELLED: 'cancelled', NO_SHOW: 'noShow', NURSE_IN_PROGRESS: 'preparing', READY_FOR_DOCTOR: 'ready', DOCTOR_IN_PROGRESS: 'inAppointment' }[value]; return key ? t(`appointment.status.${key}`) : t('staff.unknownStatus') }

function StaffDashboard({ role, user, t, language }) {
  const [profile, setProfile] = useState(null)
  const [calendar, setCalendar] = useState(null)
  const [error, setError] = useState('')
  const today = todayKey()
  const load = () => Promise.all([role === 'DOCTOR' ? getDoctorProfile() : getNurseProfile(), role === 'DOCTOR' ? getDoctorCalendar({ from: today, to: today }) : getNurseCalendar({ from: today, to: today })]).then(([profileValue, calendarValue]) => { setProfile(profileValue); setCalendar(calendarValue) }).catch(() => setError(t('staff.calendarError')))
  useEffect(() => { load() }, [role])
  const events = calendar?.events || []
  const counts = useMemo(() => ({ total: events.length, waiting: events.filter((item) => ['BOOKED', 'CONFIRMED', 'ARRIVED'].includes(item.status)).length, preparing: events.filter((item) => ['PREPARING', 'READY'].includes(item.status)).length, inAppointment: events.filter((item) => item.status === 'IN_APPOINTMENT').length, completed: events.filter((item) => item.status === 'COMPLETED').length }), [events])
  const next = events.find((item) => !['COMPLETED', 'CANCELLED', 'NO_SHOW'].includes(item.status))
  const name = role === 'DOCTOR' ? [profile?.lastName, profile?.firstName].filter(Boolean).join(' ') : [user.firstName, user.lastName].filter(Boolean).join(' ')
  const linkedDoctor = role === 'NURSE' ? profile?.doctor : null
  const shift = shiftForDay(calendar, today)
  return <section className="page-shell staff-dashboard-page"><div className="staff-dashboard-hero"><div><div className="eyebrow">{t(`roles.${role}`)}</div><h1>{t('dashboard.welcome', { name: name || user.firstName })}</h1><p>{formatLongDate(today, language)}</p><div className="staff-shift"><span>{t('staff.workShift')}</span><strong>{shift || t('staff.dayOff')}</strong></div></div><div className="welcome-avatar">{`${profile?.firstName?.[0] || user.firstName?.[0] || ''}${profile?.lastName?.[0] || user.lastName?.[0] || ''}`.toUpperCase()}</div></div>{error && <p className="error-message" role="alert">{error}</p>}<div className="staff-dashboard-context">{role === 'NURSE' && <div><span>{t('staff.myDoctor')}</span><strong>{linkedDoctor ? `${linkedDoctor.lastName} ${linkedDoctor.firstName}` : t('staff.noDoctor')}</strong><small>{linkedDoctor?.specialty || ''}</small></div>}{role === 'DOCTOR' && <div><span>{t('staff.clinic')}</span><strong>{profile?.clinic?.name || '—'}</strong><small>{profile?.clinic?.cityName || ''}</small></div>}<div><span>{t('staff.workShift')}</span><strong>{shift || t('staff.dayOff')}</strong><small>{calendar?.schedule?.slotDurationMinutes ? `${calendar.schedule.slotDurationMinutes} ${t('staff.minutes')}` : ''}</small></div></div><div className="staff-metrics">{(role === 'DOCTOR' ? [[t('staff.patientsToday'), counts.total], [t('staff.waiting'), counts.waiting], [t('staff.prepared'), counts.preparing], [t('staff.inAppointment'), counts.inAppointment], [t('staff.completed'), counts.completed]] : [[t('staff.totalPatients'), counts.total], [t('staff.arrived'), events.filter((item) => item.status === 'ARRIVED').length], [t('staff.waitingPreparation'), events.filter((item) => item.status === 'BOOKED' || item.status === 'CONFIRMED').length], [t('staff.readyForDoctor'), events.filter((item) => item.status === 'READY').length], [t('staff.completed'), counts.completed]]).map(([label, value]) => <article className="staff-metric" key={label}><strong>{value}</strong><span>{label}</span></article>)}</div><div className="staff-dashboard-columns"><section className="dashboard-schedule-card"><div className="section-heading"><div><span className="eyebrow">{t('staff.today')}</span><h2>{role === 'DOCTOR' ? t('staff.todaySchedule') : t('staff.todayPatients')}</h2></div><button className="text-button" type="button" onClick={() => navigate(`/${role === 'DOCTOR' ? 'doctor' : 'nurse'}/schedule`)}>{t('staff.openFullSchedule')}</button></div>{events.length ? <div className="mini-schedule">{events.map((item) => <button type="button" className="mini-schedule__row" key={item.id} onClick={() => navigate(`/${role === 'DOCTOR' ? 'doctor' : 'nurse'}/appointments?appointmentId=${item.id}`)}><strong>{item.startTime}</strong><span>{item.recipient?.fullName}</span><span className={`status status--${String(item.status).toLowerCase()}`}>{statusLabel(item.status, t)}</span></button>)}</div> : <div className="empty-state"><p>{t('staff.noAppointments')}</p></div>}</section><section className="next-patient-card"><span className="eyebrow">{t('staff.nextPatient')}</span>{next ? <><strong>{next.startTime}</strong><h2>{next.recipient?.fullName}</h2><p>{next.recipient?.age == null ? '—' : `${next.recipient.age} ${t('staff.years')}`} · {next.recipient?.type === 'DEPENDENT' ? t('staff.dependent') : t('staff.patient')}</p><span className={`status status--${String(next.status).toLowerCase()}`}>{statusLabel(next.status, t)}</span><button className="button" type="button" onClick={() => navigate(`/${role === 'DOCTOR' ? 'doctor' : 'nurse'}/appointments?appointmentId=${next.id}`)}>{t('staff.openAppointment')}</button></> : <p>{t('staff.noAppointments')}</p>}</section></div></section>
}

function InfoCards({ t, admin = false }) {
  const items = admin ? ['regions', 'clinics', 'doctors'] : ['bookAppointment', 'myAppointments', 'medicalRecord']
  return <div className="info-grid">{items.map((item, index) => <article className="info-card" key={item}><span className="info-number">0{index + 1}</span><h2>{t(`nav.${item}`)}</h2><p>{t('dashboard.statPlaceholder')}</p></article>)}</div>
}

function todayKey() { const date = new Date(); return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}-${String(date.getDate()).padStart(2, '0')}` }
function dayOfWeek(value) { return ['SUNDAY', 'MONDAY', 'TUESDAY', 'WEDNESDAY', 'THURSDAY', 'FRIDAY', 'SATURDAY'][new Date(`${value}T12:00:00`).getDay()] }
function shiftForDay(calendar, date) { if (!calendar?.schedule) return ''; const exception = calendar.exceptions?.find((item) => item.date === date); if (exception && exception.type !== 'CUSTOM_WORKING_HOURS') return ''; const intervals = exception?.type === 'CUSTOM_WORKING_HOURS' ? [exception] : calendar.schedule.intervals.filter((item) => item.dayOfWeek === dayOfWeek(date) && !item.breakInterval); return intervals.map((item) => `${item.startTime}–${item.endTime}`).join(', ') }
function formatLongDate(value, language) { return new Intl.DateTimeFormat(localeFor(language), { day: 'numeric', month: 'long', year: 'numeric' }).format(new Date(`${value}T12:00:00`)) }

export default DashboardPage
