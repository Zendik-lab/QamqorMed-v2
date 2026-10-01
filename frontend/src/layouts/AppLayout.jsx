import { useState } from 'react'
import { useAuth } from '../auth/AuthContext.jsx'
import { useTranslation } from '../i18n/index.jsx'
import { navigate, useLocation } from '../utils/navigation.js'
import { isDevelopment } from '../config/environment.js'
import DevToolbar from '../components/DevToolbar.jsx'

const menus = {
  PATIENT: [
    ['dashboard', '/patient', '⌂'], ['bookAppointment', '/doctors', '＋'], ['myAppointments', '/patient/my-appointments', '◷'],
    ['dependents', '/patient/dependents', '♧'], ['medicalRecord', '/patient/medical-record', '▤'], ['documents', '/patient/documents', '□'], ['profile', '/patient/profile', '◉'],
  ],
  DOCTOR: [
    ['dashboard', '/doctor', '⌂'], ['schedule', '/doctor/schedule', '◷'], ['patients', '/doctor/patients', '♧'], ['appointments', '/doctor/appointments', '＋'],
    ['medicalRecords', '/doctor/medical-records', '▤'], ['referrals', '/doctor/referrals', '↗'], ['profile', '/doctor/profile', '◉'],
  ],
  NURSE: [
    ['dashboard', '/nurse', '⌂'], ['schedule', '/nurse/schedule', '◷'], ['todayPatients', '/nurse/today', '♧'], ['vitals', '/nurse/vitals', '⌁'],
    ['appointments', '/nurse/appointments', '＋'], ['profile', '/nurse/profile', '◉'],
  ],
  ADMIN: [
    ['dashboard', '/admin', '⌂'], ['regions', '/admin/regions', '⌖'], ['cities', '/admin/cities', '▦'], ['clinics', '/admin/clinics', '＋'],
    ['specialties', '/admin/specialties', '✦'], ['doctors', '/admin/doctors', '♧'], ['nurses', '/admin/nurses', '♧'], ['users', '/admin/users', '◉'], ['schedule', '/admin/schedule', '◷'],
  ],
}

function AppLayout({ children }) {
  const { user, isAuthenticated, logout } = useAuth()
  const { t, language, languages, changeLanguage } = useTranslation()
  const location = useLocation()
  const [sidebarOpen, setSidebarOpen] = useState(false)
  const dashboard = isAuthenticated && user && !['/login', '/register'].includes(location.pathname) && !(isDevelopment && ['/dev', '/dev/login'].includes(location.pathname))

  if (!dashboard) {
    return (
      <div className="app-shell app-shell--public">
        <header className="app-header public-header">
          <Brand />
          <LanguageSwitcher language={language} languages={languages} changeLanguage={changeLanguage} t={t} />
        </header>
        <main className="app-main">{children}</main>
      </div>
    )
  }

  const menu = menus[user.role] || []
  const basePath = user.role === 'ADMIN' ? '/admin' : `/${user.role.toLowerCase()}`
  const isActive = (path) => path === basePath ? location.pathname === path : location.pathname === path || location.pathname.startsWith(`${path}/`)

  return (
    <div className="dashboard-layout">
      {sidebarOpen && <button className="sidebar-backdrop" aria-label={t('common.close')} type="button" onClick={() => setSidebarOpen(false)} />}
      <aside className={`dashboard-sidebar ${sidebarOpen ? 'dashboard-sidebar--open' : ''}`}>
        <div className="sidebar-brand"><Brand /></div>
        <div className="sidebar-role"><span>QamqorMed</span><strong>{t(`roles.${user.role}`)}</strong></div>
        <nav className="sidebar-nav" aria-label={t('common.home')}>
          {menu.map(([key, path, icon]) => <button className={`sidebar-link ${isActive(path) ? 'sidebar-link--active' : ''}`} type="button" key={path} onClick={() => { navigate(path); setSidebarOpen(false) }}><span className="sidebar-link__icon" aria-hidden="true">{icon}</span><span>{t(`nav.${key}`)}</span></button>)}
        </nav>
        <div className="sidebar-footer"><button className="sidebar-link sidebar-link--logout" type="button" onClick={() => logout()}><span className="sidebar-link__icon" aria-hidden="true">↪</span><span>{t('common.logout')}</span></button></div>
      </aside>
      <div className="dashboard-main">
        <header className="dashboard-header">
          <button className="sidebar-toggle" type="button" aria-label={t('common.home')} onClick={() => setSidebarOpen(true)}>☰</button>
          <div className="header-context"><span>{t(`roles.${user.role}`)}</span><strong>{t('nav.dashboard')}</strong></div>
          <div className="header-actions">
            {user.role === 'PATIENT' && user.preferredCity && <button className="location-chip" type="button" onClick={() => navigate('/patient/location')} title="Изменить основной город">📍 {user.preferredCity.name}</button>}
            <LanguageSwitcher language={language} languages={languages} changeLanguage={changeLanguage} t={t} compact />
            <button className="profile-chip" type="button" onClick={() => navigate(`${basePath}/profile`)}><span className="profile-chip__avatar">{user.firstName?.[0]}{user.lastName?.[0]}</span><span className="profile-chip__name">{user.firstName} {user.lastName}</span></button>
            <button className="header-logout" type="button" onClick={() => logout()}>{t('common.logout')}</button>
          </div>
        </header>
        <main className="dashboard-content">{children}</main>
        {isDevelopment && <DevToolbar />}
      </div>
    </div>
  )
}

function Brand() {
  return <button className="brand-link" type="button" onClick={() => navigate('/')} aria-label="QamqorMed"><span className="brand-mark" aria-hidden="true">Q</span><span className="brand-name">QamqorMed</span></button>
}

function LanguageSwitcher({ language, languages, changeLanguage, t, compact = false }) {
  return <label className={`language-switcher ${compact ? 'language-switcher--compact' : ''}`}><span className="sr-only">{t('common.language')}</span><select value={language} onChange={(event) => changeLanguage(event.target.value)} aria-label={t('common.language')}>{languages.map((item) => <option value={item.code} key={item.code}>{compact ? item.shortLabel : item.label}</option>)}</select></label>
}

export default AppLayout
