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
  const { t } = useTranslation()
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

function PatientInfoCards({ t, city }) {
  return <div className="info-grid"><article className="info-card"><span className="info-number">01</span><h2>📍 {city || 'Ваш город'}</h2><p>Основной город для поиска врачей.</p></article><article className="info-card"><span className="info-number">02</span><h2>{t('nav.myAppointments')}</h2><p>Записи для вас и подопечных.</p></article><article className="info-card"><span className="info-number">03</span><h2>{t('nav.medicalRecord')}</h2><p>История обращений и рекомендации.</p></article></div>
}

function StatsGrid({ items, t }) {
  return <div className="stat-grid">{items.map((item, index) => <article className="stat-card" key={item}><span className={`stat-icon stat-icon--${index + 1}`} aria-hidden="true">{index === 0 ? '◷' : index === 1 ? '＋' : '↗'}</span><div><strong>—</strong><span>{t(`dashboard.${item}`)}</span><small>{t('dashboard.statPlaceholder')}</small></div></article>)}</div>
}

function InfoCards({ t, admin = false }) {
  const items = admin ? ['regions', 'clinics', 'doctors'] : ['bookAppointment', 'myAppointments', 'medicalRecord']
  return <div className="info-grid">{items.map((item, index) => <article className="info-card" key={item}><span className="info-number">0{index + 1}</span><h2>{t(`nav.${item}`)}</h2><p>{t('dashboard.statPlaceholder')}</p></article>)}</div>
}

export default DashboardPage
