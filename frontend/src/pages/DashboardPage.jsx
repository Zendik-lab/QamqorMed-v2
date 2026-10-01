import { useEffect, useMemo, useState } from 'react'
import { getDoctorCalendar, getDoctorProfile, getNurseCalendar, getNurseProfile } from '../api/apiClient.js'
import { useAuth } from '../auth/AuthContext.jsx'
import { localizeSpecialty, useTranslation } from '../i18n/index.jsx'
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
          {user.role === 'PATIENT' && <button className="button button--compact" type="button" onClick={() => navigate('/patient/appointments')}>{t('dashboard.findDoctor')}</button>}
          {user.role === 'ADMIN' && <button className="button button--compact" type="button" onClick={() => navigate('/admin')}>{t('dashboard.adminManagement')}</button>}
        </div>
        <div className="welcome-avatar" aria-label={fullName}>{initials}</div>
      </div>

      {(user.role === 'DOCTOR' || user.role === 'NURSE') && (
        <div className="profile-summary">
          <div><span>{t('common.profile')}</span><strong>{fullName}</strong></div>
          <div><span>{t('dashboard.specialty')}</span><strong>{localizeSpecialty(user.specialty?.name, t) || t('common.notSpecified')}</strong></div>
          <div><span>{t('dashboard.clinic')}</span><strong>{user.clinic?.name || t('common.notSpecified')}</strong></div>
        </div>
      )}

      {user.role === 'DOCTOR' && <StatsGrid items={['todayAppointments', 'nextPatient', 'dailyPatients']} t={t} />}
      {user.role === 'NURSE' && <StatsGrid items={['todaySchedule', 'clinicPatients', 'attention']} t={t} />}
      {user.role === 'PATIENT' && <PatientInfoCards t={t} city={user.preferredCity?.name} />}
      {user.role === 'ADMIN' && <InfoCards t={t} admin />}
    </section>
  )
}

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

function PatientInfoCards({ t, city }) {
  return <div className="info-grid"><article className="info-card"><span className="info-number">01</span><h2>📍 {city || 'Ваш город'}</h2><p>Основной город для поиска врачей.</p></article><article className="info-card"><span className="info-number">02</span><h2>{t('nav.myAppointments')}</h2><p>Записи для вас и подопечных.</p></article><article className="info-card"><span className="info-number">03</span><h2>{t('nav.medicalRecord')}</h2><p>История обращений и рекомендации.</p></article></div>
}

function StatsGrid({ items, t }) {
  return <div className="stat-grid">{items.map((item, index) => <article className="stat-card" key={item}><span className={`stat-icon stat-icon--${index + 1}`} aria-hidden="true">{index === 0 ? '◷' : index === 1 ? '＋' : '↗'}</span><div><strong>—</strong><span>{t(`dashboard.${item}`)}</span><small>{t('dashboard.statPlaceholder')}</small></div></article>)}</div>
}

function statusLabel(value, t) { const key = { BOOKED: 'booked', CONFIRMED: 'confirmed', ARRIVED: 'arrived', PREPARING: 'preparing', READY: 'ready', IN_APPOINTMENT: 'inAppointment', COMPLETED: 'completed', CANCELLED: 'cancelled', NO_SHOW: 'noShow', NURSE_IN_PROGRESS: 'preparing', READY_FOR_DOCTOR: 'ready', DOCTOR_IN_PROGRESS: 'inAppointment' }[value]; return key ? t(`appointment.status.${key}`) : t('staff.unknownStatus') }
function dayOfWeek(value) { return ['SUNDAY', 'MONDAY', 'TUESDAY', 'WEDNESDAY', 'THURSDAY', 'FRIDAY', 'SATURDAY'][new Date(`${value}T12:00:00`).getDay()] }
function shiftForDay(calendar, date) { if (!calendar?.schedule) return ''; const exception = calendar.exceptions?.find((item) => item.date === date); if (exception && exception.type !== 'CUSTOM_WORKING_HOURS') return ''; const intervals = exception?.type === 'CUSTOM_WORKING_HOURS' ? [exception] : calendar.schedule.intervals.filter((item) => item.dayOfWeek === dayOfWeek(date) && !item.breakInterval); return intervals.map((item) => `${item.startTime}–${item.endTime}`).join(', ') }
function todayKey() { const date = new Date(); return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}-${String(date.getDate()).padStart(2, '0')}` }
function formatLongDate(value, language) { const locale = language === 'kk' ? 'kk-KZ' : language === 'en' ? 'en-US' : 'ru-RU'; return new Intl.DateTimeFormat(locale, { day: 'numeric', month: 'long', year: 'numeric' }).format(new Date(`${value}T12:00:00`)) }

function InfoCards({ t, admin = false }) {
  const items = admin ? ['regions', 'clinics', 'doctors'] : ['bookAppointment', 'myAppointments', 'medicalRecord']
  return <div className="info-grid">{items.map((item, index) => <article className="info-card" key={item}><span className="info-number">0{index + 1}</span><h2>{t(`nav.${item}`)}</h2><p>{t('dashboard.statPlaceholder')}</p></article>)}</div>
}

export default DashboardPage
