import { useEffect, useState } from 'react'
import { useAuth } from '../auth/AuthContext.jsx'
import { useTranslation } from '../i18n/index.jsx'
import { navigate, useLocation } from '../utils/navigation.js'
import { isDevelopment } from '../config/environment.js'
import DevToolbar from '../components/DevToolbar.jsx'
import AdminSidebar, { adminMenuItems } from '../components/AdminSidebar.jsx'
import NotificationCenter from '../components/NotificationCenter.jsx'

const menus = {
  PATIENT: [
    ['dashboard', '/patient', '⌂'], ['bookAppointment', '/patient/appointments', '＋'], ['myAppointments', '/patient/my-appointments', '◷'],
    ['documents', '/patient/documents', '▤'], ['waitlist', '/patient/waitlist', '⌛'], ['dependents', '/patient/dependents', '♧'], ['profile', '/patient/profile', '◉'],
  ],
  DOCTOR: [
    ['dashboard', '/doctor', '⌂'], ['appointments', '/doctor/appointments', '＋'], ['schedule', '/doctor/schedule', '▦'], ['patients', '/doctor/patients', '♧'], ['medicalRecords', '/doctor/medical-records', '▤'], ['referrals', '/doctor/referrals', '↗'], ['profile', '/doctor/profile', '◉'],
  ],
  NURSE: [
    ['dashboard', '/nurse', '⌂'], ['todayPatients', '/nurse/today', '♧'], ['appointments', '/nurse/appointments', '▦'], ['vitals', '/nurse/vitals', '♡'], ['profile', '/nurse/profile', '◉'],
  ],
}

const mobileMenus = {
  PATIENT: {
    primary: [['dashboard', '/patient', '⌂'], ['myAppointments', '/patient/my-appointments', '◷'], ['documents', '/patient/documents', '▤'], ['profile', '/patient/profile', '◉']],
    more: [['bookAppointment', '/patient/appointments', '＋'], ['waitlist', '/patient/waitlist', '⌛'], ['dependents', '/patient/dependents', '♧']],
  },
  DOCTOR: {
    primary: [['dashboard', '/doctor', '⌂'], ['appointments', '/doctor/appointments', '＋'], ['medicalRecords', '/doctor/medical-records', '▤'], ['referrals', '/doctor/referrals', '↗']],
    more: [['patients', '/doctor/patients', '♧'], ['schedule', '/doctor/schedule', '▦'], ['profile', '/doctor/profile', '◉']],
  },
  NURSE: {
    primary: [['dashboard', '/nurse', '⌂'], ['todayPatients', '/nurse/today', '♧'], ['appointments', '/nurse/appointments', '▦'], ['profile', '/nurse/profile', '◉']],
    more: [['vitals', '/nurse/vitals', '♡']],
  },
}

function AppLayout({ children }) {
  const { user, isAuthenticated, logout } = useAuth()
  const { t, language, languages, changeLanguage } = useTranslation()
  const location = useLocation()
  const [sidebarOpen, setSidebarOpen] = useState(false)
  const [moreOpen, setMoreOpen] = useState(false)

  useEffect(() => {
    setSidebarOpen(false)
    setMoreOpen(false)
  }, [location.pathname])

  useEffect(() => {
    if (!sidebarOpen && !moreOpen) return undefined
    const closeOnEscape = (event) => {
      if (event.key !== 'Escape') return
      setSidebarOpen(false)
      setMoreOpen(false)
    }
    window.addEventListener('keydown', closeOnEscape)
    return () => window.removeEventListener('keydown', closeOnEscape)
  }, [sidebarOpen, moreOpen])
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

  const menu = user.role === 'ADMIN' ? adminMenuItems : menus[user.role] || []
  const basePath = user.role === 'ADMIN' ? '/admin' : `/${user.role.toLowerCase()}`
  const isActive = (path) => path === basePath ? location.pathname === path : location.pathname === path || location.pathname.startsWith(`${path}/`)
  const activeMenu = menu.find(([, path]) => isActive(path))
    || (user.role === 'PATIENT' && ['/patient/medical-record', '/patient/diagnoses', '/patient/prescriptions', '/patient/medications', '/patient/lab-results', '/patient/referrals', '/patient/certificates', '/patient/documents', '/patient/medical-history'].some((path) => location.pathname === path) ? menu.find(([key]) => key === 'documents') : null)

  return (
    <div className="dashboard-layout">
      {sidebarOpen && <button className="sidebar-backdrop" aria-label={t('common.close')} type="button" onClick={() => setSidebarOpen(false)} />}
      {user.role === 'ADMIN' ? <AdminSidebar open={sidebarOpen} onClose={() => setSidebarOpen(false)} /> : (
        <aside id="app-sidebar" className={`dashboard-sidebar ${sidebarOpen ? 'dashboard-sidebar--open' : ''}`}>
          <div className="sidebar-brand"><Brand /></div>
          <div className="sidebar-role"><span>QamqorMed</span><strong>{t(`roles.${user.role}`)}</strong></div>
          <nav className="sidebar-nav" aria-label={t('common.home')}>
            {menu.map(([key, path, icon]) => {
              const active = isActive(path)
              return <button className={`sidebar-link ${active ? 'sidebar-link--active' : ''}`} aria-current={active ? 'page' : undefined} type="button" key={path} onClick={() => { navigate(path); setSidebarOpen(false) }}><span className="sidebar-link__icon" aria-hidden="true">{icon}</span><span>{t(`nav.${key}`)}</span></button>
            })}
          </nav>
          <div className="sidebar-footer"><button className="sidebar-link sidebar-link--logout" type="button" onClick={() => logout()}><span className="sidebar-link__icon" aria-hidden="true">↪</span><span>{t('common.logout')}</span></button></div>
        </aside>
      )}
      <div className="dashboard-main">
        <header className="dashboard-header">
          <button className="sidebar-toggle" type="button" aria-label={t('common.menu')} aria-expanded={sidebarOpen} aria-controls="app-sidebar" onClick={() => setSidebarOpen(true)}>☰</button>
          <div className="header-context"><span>{t(`roles.${user.role}`)}</span><strong>{t(`nav.${activeMenu?.[0] || 'dashboard'}`)}</strong></div>
          <div className="header-actions">
            {user.role === 'PATIENT' && user.preferredCity && <button className="location-chip" type="button" onClick={() => navigate('/patient/location')} title={t('patient.changeCity')}>📍 {user.preferredCity.name}</button>}
            <LanguageSwitcher language={language} languages={languages} changeLanguage={changeLanguage} t={t} compact />
            <NotificationCenter />
            <button className="profile-chip" type="button" onClick={() => navigate(`${basePath}/profile`)}><span className="profile-chip__avatar">{user.firstName?.[0]}{user.lastName?.[0]}</span><span className="profile-chip__name">{user.firstName} {user.lastName}</span></button>
            <button className="header-logout" type="button" onClick={() => logout()}>{t('common.logout')}</button>
          </div>
        </header>
        <main id="main-content" className="dashboard-content" tabIndex="-1">{children}</main>
        {mobileMenus[user.role] && <MobileBottomNav role={user.role} pathname={location.pathname} menu={mobileMenus[user.role]} onMore={() => setMoreOpen((value) => !value)} moreOpen={moreOpen} t={t} />}
        {mobileMenus[user.role] && moreOpen && <MobileMoreSheet role={user.role} menu={mobileMenus[user.role].more} pathname={location.pathname} onClose={() => setMoreOpen(false)} logout={logout} t={t} />}
        {isDevelopment && <DevToolbar />}
      </div>
    </div>
  )
}

function MobileBottomNav({ role, pathname, menu, onMore, moreOpen, t }) {
  const active = (path) => path === `/${role.toLowerCase()}` ? pathname === path : pathname === path || pathname.startsWith(`${path}/`)
  return (
    <nav className="mobile-bottom-nav" aria-label={t('common.home')}>
      {menu.primary.map(([key, path, icon]) => {
        const isCurrent = active(path)
        return <button className={`mobile-bottom-nav__item ${isCurrent ? 'is-active' : ''}`} type="button" key={path} aria-current={isCurrent ? 'page' : undefined} onClick={() => navigate(path)}><span className="mobile-bottom-nav__icon" aria-hidden="true">{icon}</span><span>{t(`nav.${key}`)}</span></button>
      })}
      <button className={`mobile-bottom-nav__item ${moreOpen ? 'is-active' : ''}`} type="button" aria-expanded={moreOpen} onClick={onMore}><span className="mobile-bottom-nav__icon" aria-hidden="true">⋯</span><span>{t('nav.more')}</span></button>
    </nav>
  )
}

function MobileMoreSheet({ role, menu, pathname, onClose, logout, t }) {
  const active = (path) => pathname === path || pathname.startsWith(`${path}/`)
  return (
    <div className="mobile-more-backdrop" role="presentation" onMouseDown={(event) => { if (event.target === event.currentTarget) onClose() }}>
      <section className="mobile-more-sheet" role="dialog" aria-modal="true" aria-labelledby="mobile-more-title">
        <div className="mobile-more-sheet__handle" aria-hidden="true" />
        <div className="mobile-more-sheet__header"><h2 id="mobile-more-title">{t('nav.more')}</h2><button className="drawer-close" type="button" aria-label={t('common.close')} onClick={onClose}>×</button></div>
        <nav className="mobile-more-sheet__nav" aria-label={t(`roles.${role}`)}>
          {menu.map(([key, path, icon]) => { const isCurrent = active(path); return <button className={`mobile-more-sheet__link ${isCurrent ? 'is-active' : ''}`} type="button" key={path} aria-current={isCurrent ? 'page' : undefined} onClick={() => navigate(path)}><span className="mobile-bottom-nav__icon" aria-hidden="true">{icon}</span><span>{t(`nav.${key}`)}</span></button> })}
          <button className="mobile-more-sheet__link mobile-more-sheet__link--logout" type="button" onClick={logout}><span className="mobile-bottom-nav__icon" aria-hidden="true">↪</span><span>{t('common.logout')}</span></button>
        </nav>
      </section>
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
