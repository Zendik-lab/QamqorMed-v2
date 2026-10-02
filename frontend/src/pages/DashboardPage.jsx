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
import { useTranslation } from '../i18n/index.jsx'
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

  if (user.role === 'PATIENT') return <PatientDashboard user={user} t={t} />
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

function PatientDashboard({ user, t }) {
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
      .catch((requestError) => { if (active) setError(requestError?.message || 'Не удалось загрузить данные кабинета') })
      .finally(() => { if (active) setLoading(false) })
    return () => { active = false }
  }, [])

  const upcoming = useMemo(() => data.appointments
    .filter((item) => isUpcomingAppointment(item))
    .sort((left, right) => String(left.appointmentAt).localeCompare(String(right.appointmentAt))), [data.appointments])
  const latest = useMemo(() => [
    data.records[0] && { key: 'visit', icon: '◷', title: 'Последний приём', value: data.records[0].diagnosis || 'Приём завершён', meta: data.records[0].doctor?.fullName },
    data.diagnoses[0] && { key: 'diagnosis', icon: '◇', title: 'Последний диагноз', value: data.diagnoses[0].icd10Code?.name, meta: data.diagnoses[0].icd10Code?.code },
    data.prescriptions[0] && { key: 'prescription', icon: '⌁', title: 'Последнее назначение', value: data.prescriptions[0].title, meta: data.prescriptions[0].instructions },
    data.labs[0] && { key: 'lab', icon: '▥', title: 'Последний анализ', value: data.labs[0].testName, meta: data.labs[0].result || data.labs[0].status },
    data.certificates[0] && { key: 'certificate', icon: '□', title: 'Последняя справка', value: data.certificates[0].type, meta: data.certificates[0].description },
  ].filter(Boolean).slice(0, 4), [data])

  const firstName = data.profile?.firstName || user.firstName || 'пациент'
  const profileName = [data.profile?.firstName, data.profile?.lastName].filter(Boolean).join(' ')
  const initials = `${data.profile?.firstName?.[0] || user.firstName?.[0] || ''}${data.profile?.lastName?.[0] || user.lastName?.[0] || ''}`.toUpperCase()

  if (loading) return <PatientDashboardSkeleton />

  return (
    <section className="page-shell patient-dashboard">
      <div className="patient-dashboard__hero">
        <div>
          <div className="eyebrow">{t('roles.PATIENT')} · QamqorMed</div>
          <h1>Здравствуйте, {firstName}</h1>
          <p>Вот актуальная информация о вашем здоровье и записях.</p>
          <div className="patient-dashboard__hero-actions">
            <button className="button button--compact" type="button" onClick={() => navigate('/patient/appointments')}>＋ Записаться к врачу</button>
            <button className="button button--ghost button--compact" type="button" onClick={() => navigate('/patient/profile')}>Открыть профиль</button>
          </div>
        </div>
        <div className="welcome-avatar" aria-label={profileName || firstName}>{initials}</div>
      </div>

      {error && <p className="error-message" role="alert">{error}</p>}

      <div className="patient-dashboard__columns">
        <NextAppointmentCard appointment={upcoming[0]} t={t} />
        <section className="patient-card patient-quick-actions" aria-labelledby="quick-actions-title">
          <div className="section-heading"><div><div className="eyebrow">Навигация</div><h2 id="quick-actions-title">Быстрые действия</h2></div></div>
          <div className="patient-quick-actions__grid">
            <QuickAction icon="＋" title="Записаться к врачу" description="Найдите специалиста и время" onClick={() => navigate('/patient/appointments')} />
            <QuickAction icon="◷" title="Мои записи" description="Предстоящие приёмы и история" onClick={() => navigate('/patient/my-appointments')} />
            <QuickAction icon="▤" title="Медицинская карта" description="История и результаты" onClick={() => navigate('/patient/medical-record')} />
            <QuickAction icon="♧" title="Мои подопечные" description={`${data.dependents.length || 'Нет'} профилей`} onClick={() => navigate('/patient/dependents')} />
          </div>
        </section>
      </div>

      <section className="patient-card patient-dashboard__appointments">
        <div className="section-heading"><div><div className="eyebrow">Ваш график</div><h2>Ближайшие записи</h2></div><button className="text-button" type="button" onClick={() => navigate('/patient/my-appointments')}>Все записи →</button></div>
        {upcoming.length ? <div className="patient-appointment-list">{upcoming.slice(0, 5).map((item) => <PatientAppointmentItem item={item} key={item.id} t={t} />)}</div> : <EmptyState title="У вас пока нет предстоящих приёмов" description="Запишитесь к врачу, чтобы выбрать удобное время." action="Записаться к врачу" onAction={() => navigate('/patient/appointments')} compact />}
      </section>

      <section className="patient-card patient-dashboard__medical">
        <div className="section-heading"><div><div className="eyebrow">Ваша история</div><h2>Последняя медицинская информация</h2></div><button className="text-button" type="button" onClick={() => navigate('/patient/medical-record')}>Открыть карту →</button></div>
        {latest.length ? <div className="patient-medical-grid">{latest.map((item) => <article className="patient-medical-card" key={item.key}><span className="patient-icon patient-icon--soft">{item.icon}</span><div><span>{item.title}</span><strong>{item.value}</strong>{item.meta && <small>{item.meta}</small>}</div></article>)}</div> : <EmptyState title="Медицинская информация появится здесь" description="После завершения приёма здесь будут диагнозы, назначения и результаты." onAction={() => navigate('/patient/appointments')} action="Записаться к врачу" compact />}
      </section>
    </section>
  )
}

function NextAppointmentCard({ appointment, t }) {
  return <section className="patient-card patient-next-card"><div className="section-heading"><div><div className="eyebrow">Следующий приём</div><h2>Ближайший приём</h2></div><span className="patient-icon">◷</span></div>{appointment ? <><div className="patient-next-card__time">{formatAppointmentDate(appointment.appointmentAt)}<strong>{formatAppointmentTime(appointment.appointmentAt)}</strong></div><div className="patient-next-card__doctor"><div className="profile-avatar">{appointment.doctor?.fullName?.split(' ').map((part) => part[0]).join('').slice(0, 2)}</div><div><strong>{appointment.doctor?.fullName || 'Врач'}</strong><span>{appointment.doctor?.specialty || 'Специалист'}</span></div></div><dl className="patient-meta-list"><div><dt>Клиника</dt><dd>{appointment.clinic?.name || '—'}</dd></div><div><dt>Пациент</dt><dd>{appointment.subject?.fullName || 'Вы'}</dd></div></dl><div className="patient-next-card__footer"><span className={`status status--${String(appointment.status).toLowerCase()}`}>{statusLabel(appointment.status, t)}</span><button className="text-button" type="button" onClick={() => navigate('/patient/my-appointments')}>Подробнее →</button></div></> : <EmptyState title="У вас пока нет предстоящих приёмов" description="Выберите врача и удобное время для записи." action="Записаться к врачу" onAction={() => navigate('/patient/appointments')} compact />}</section>
}

function PatientAppointmentItem({ item, t }) {
  return <article className="patient-appointment-item"><div className="patient-appointment-item__date"><strong>{formatAppointmentDay(item.appointmentAt)}</strong><span>{formatAppointmentMonth(item.appointmentAt)}</span></div><div className="patient-appointment-item__body"><strong>{item.doctor?.fullName || 'Врач'}</strong><span>{item.doctor?.specialty || 'Специалист'} · {item.clinic?.name || 'Клиника'}</span><small>{formatAppointmentDate(item.appointmentAt)} · {formatAppointmentTime(item.appointmentAt)} · {item.subject?.fullName || 'Вы'}</small></div><span className={`status status--${String(item.status).toLowerCase()}`}>{statusLabel(item.status, t)}</span></article>
}

function QuickAction({ icon, title, description, onClick }) {
  return <button className="patient-quick-action" type="button" onClick={onClick}><span className="patient-icon patient-icon--soft">{icon}</span><span><strong>{title}</strong><small>{description}</small></span><span className="patient-quick-action__arrow">→</span></button>
}

function EmptyState({ title, description, action, onAction, compact = false }) {
  return <div className={`empty-state patient-empty-state ${compact ? 'patient-empty-state--compact' : ''}`}><span className="patient-empty-state__icon">＋</span><h3>{title}</h3><p>{description}</p>{action && <button className="button button--secondary button--compact" type="button" onClick={onAction}>{action}</button>}</div>
}

function PatientDashboardSkeleton() {
  return <section className="page-shell patient-dashboard"><div className="patient-dashboard__skeleton-hero"><div><span className="skeleton skeleton--eyebrow" /><span className="skeleton skeleton--title" /><span className="skeleton skeleton--line" /></div><span className="skeleton skeleton--avatar" /></div><div className="patient-dashboard__columns"><div className="patient-card patient-skeleton-card"><span className="skeleton skeleton--line" /><span className="skeleton skeleton--title" /><span className="skeleton skeleton--line" /><span className="skeleton skeleton--line" /></div><div className="patient-card patient-skeleton-card"><span className="skeleton skeleton--line" /><span className="skeleton skeleton--title" /><span className="skeleton skeleton--line" /><span className="skeleton skeleton--line" /></div></div></section>
}

function isUpcomingAppointment(item) { return !['COMPLETED', 'CANCELLED', 'NO_SHOW'].includes(item.status) && String(item.appointmentAt || '').slice(0, 16) >= clinicNowKey() }
function clinicNowKey() { const parts = new Intl.DateTimeFormat('en-CA', { timeZone: 'Asia/Qyzylorda', year: 'numeric', month: '2-digit', day: '2-digit', hour: '2-digit', minute: '2-digit', hourCycle: 'h23' }).formatToParts(new Date()).reduce((result, item) => ({ ...result, [item.type]: item.value }), {}); return `${parts.year}-${parts.month}-${parts.day}T${parts.hour}:${parts.minute}` }
function formatAppointmentDate(value) { const dateKey = String(value || '').slice(0, 10); if (!dateKey) return 'Дата не указана'; const [year, month, day] = dateKey.split('-').map(Number); return new Intl.DateTimeFormat('ru-RU', { day: 'numeric', month: 'long', year: 'numeric' }).format(new Date(year, month - 1, day, 12)) }
function formatAppointmentDay(value) { return String(value || '').slice(8, 10) || '—' }
function formatAppointmentMonth(value) { const dateKey = String(value || '').slice(0, 10); if (!dateKey) return ''; const [year, month, day] = dateKey.split('-').map(Number); return new Intl.DateTimeFormat('ru-RU', { month: 'short' }).format(new Date(year, month - 1, day, 12)).replace('.', '') }
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
function formatLongDate(value, language) { const locale = language === 'kk' ? 'kk-KZ' : language === 'en' ? 'en-US' : 'ru-RU'; return new Intl.DateTimeFormat(locale, { day: 'numeric', month: 'long', year: 'numeric' }).format(new Date(`${value}T12:00:00`)) }

export default DashboardPage
