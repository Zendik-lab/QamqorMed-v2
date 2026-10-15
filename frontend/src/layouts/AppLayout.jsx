import { useEffect, useRef, useState } from 'react'
import { useAuth } from '../auth/AuthContext.jsx'
import { useTranslation } from '../i18n/index.jsx'
import { useLocation } from '../utils/navigation.js'
import { isDevelopment } from '../config/environment.js'
import { isWorkspacePathActive, mobilePrimary, workspaceMenus, workspaceSection } from '../config/navigation.js'
import DevToolbar from '../components/DevToolbar.jsx'
import NotificationCenter from '../components/NotificationCenter.jsx'
import AiAssistant from '../components/AiAssistant.jsx'
import Brand from '../components/Brand.jsx'
import Footer from '../components/Footer.jsx'
import ThemeSwitcher from '../components/ThemeSwitcher.jsx'
import AccessibilitySettings from '../components/AccessibilitySettings.jsx'
import WorkspaceSidebar from '../components/WorkspaceSidebar.jsx'
import AppLink from '../components/AppLink.jsx'
import Icon from '../components/ui/Icon.jsx'
import Popover from '../components/ui/Popover.jsx'
import { Drawer } from '../components/ui/Dialog.jsx'
import { MotionRegion } from '../motion/Motion.jsx'

function readCollapsed() {
  try { return window.localStorage.getItem('qamqormed.sidebar.collapsed') === 'true' } catch { return false }
}

export default function AppLayout({ children }) {
  const { user, isAuthenticated, logout } = useAuth()
  const { t, language, languages, changeLanguage } = useTranslation()
  const { pathname } = useLocation()
  const [sidebarOpen, setSidebarOpen] = useState(false)
  const [collapsed, setCollapsed] = useState(readCollapsed)
  const mainRef = useRef(null)
  const previousPath = useRef(pathname)

  useEffect(() => {
    setSidebarOpen(false)
    if (previousPath.current !== pathname) {
      mainRef.current?.focus({ preventScroll: true })
      window.scrollTo({ top: 0, behavior: 'auto' })
      previousPath.current = pathname
    }
  }, [pathname])

  useEffect(() => {
    const media = window.matchMedia('(min-width: 901px)')
    const close = () => { if (media.matches) setSidebarOpen(false) }
    media.addEventListener('change', close)
    return () => media.removeEventListener('change', close)
  }, [])

  const toggleCollapsed = () => setCollapsed((current) => {
    const next = !current
    try { window.localStorage.setItem('qamqormed.sidebar.collapsed', String(next)) } catch { /* Continue in memory. */ }
    return next
  })
  const dashboard = isAuthenticated && user && !['/login', '/register'].includes(pathname) && !(isDevelopment && ['/dev', '/dev/login'].includes(pathname))
  const skipLink = <a className="skip-link" href="#main-content" onClick={(event) => { event.preventDefault(); mainRef.current?.focus(); mainRef.current?.scrollIntoView({ block: 'start' }) }}>{t('accessibility.skipToContent')}</a>
  const preferences = <><AccessibilitySettings /><ThemeSwitcher /><LanguageSwitcher language={language} languages={languages} changeLanguage={changeLanguage} t={t} /></>

  if (!dashboard) return <div className={`app-shell app-shell--public ${['/login', '/register'].includes(pathname) ? 'app-shell--auth' : ''}`}>
    {skipLink}<header className="app-header public-header"><Brand /><div className="header-actions">{preferences}</div></header>
    <main id="main-content" ref={mainRef} tabIndex={-1} className="app-main"><MotionRegion replayKey={pathname} variant="page" className="motion-route">{children}</MotionRegion></main><Footer />
  </div>

  const basePath = `/${user.role.toLowerCase()}`
  const currentTitle = t(workspaceSection(pathname, user.role))
  const sidebarProps = { role: user.role, pathname, onNavigate: () => setSidebarOpen(false), onLogout: logout }
  return <div className={`dashboard-layout workspace-layout ${collapsed ? 'workspace-layout--collapsed' : ''}`}>
    {skipLink}
    <div className="workspace-sidebar-slot"><WorkspaceSidebar {...sidebarProps} collapsed={collapsed} onCollapse={toggleCollapsed} /></div>
    <Drawer id="mobile-sidebar" open={sidebarOpen} onClose={() => setSidebarOpen(false)} title={t('common.menu')} className="workspace-mobile-drawer"><WorkspaceSidebar {...sidebarProps} mobile /></Drawer>
    <div className="dashboard-main">
      <header className="dashboard-header workspace-header">
        <button className="sidebar-toggle ui-icon-button" type="button" aria-label={t('common.menu')} aria-expanded={sidebarOpen} aria-controls="mobile-sidebar" onClick={() => setSidebarOpen(true)}><Icon name="menu" /></button>
        <div className="workspace-header__context">
          <nav className="workspace-breadcrumbs" aria-label={t('workspace.breadcrumbs')}><ol><li><AppLink href={basePath}>{t(`roles.${user.role}`)}</AppLink></li><li aria-hidden="true"><Icon name="chevron" size={12} /></li><li aria-current="page">{currentTitle}</li></ol></nav>
          <strong>{currentTitle}</strong>
        </div>
        <div className="header-actions workspace-header__actions">
          {user.role === 'PATIENT' && user.preferredCity && <AppLink className="workspace-location" href="/patient/location" title={t('patient.changeCity')}><Icon name="pin" size={16} /><span>{user.preferredCity.name}</span></AppLink>}
          {preferences}<NotificationCenter />
          <Popover label={t('workspace.userMenu')} className="workspace-user-menu" trigger={<><span className="profile-chip__avatar" aria-hidden="true">{user.firstName?.[0]}{user.lastName?.[0]}</span><span className="workspace-user-menu__name">{user.firstName} {user.lastName}</span><Icon name="down" size={14} /></>}>
            <div className="workspace-user-menu__identity"><strong>{user.firstName} {user.lastName}</strong><span>{t(`roles.${user.role}`)}</span></div>
            <AppLink href={`${basePath}/profile`}><Icon name="user" size={18} />{t('nav.profile')}</AppLink><AppLink href="/help"><Icon name="help" size={18} />{t('footer.help')}</AppLink>
            <button type="button" data-close-popover onClick={logout}><Icon name="logout" size={18} />{t('common.logout')}</button>
          </Popover>
        </div>
      </header>
      <main id="main-content" ref={mainRef} className="dashboard-content" tabIndex={-1}><MotionRegion replayKey={pathname} variant="page" className="motion-route">{children}</MotionRegion></main>
      <Footer />
      <nav className="workspace-bottom-nav" aria-label={t('workspace.navigation')}>
        {(workspaceMenus[user.role] || []).filter(([key]) => mobilePrimary[user.role]?.includes(key)).map(([key, path, icon]) => <AppLink key={path} href={path} className={isWorkspacePathActive(pathname, path, user.role) ? 'is-active' : ''} aria-current={isWorkspacePathActive(pathname, path, user.role) ? 'page' : undefined}><Icon name={icon} /><span>{t(`nav.${key}`)}</span></AppLink>)}
        <button type="button" aria-expanded={sidebarOpen} aria-controls="mobile-sidebar" onClick={() => setSidebarOpen(true)}><Icon name="more" /><span>{t('nav.more')}</span></button>
      </nav>
      {isDevelopment && <DevToolbar />}<AiAssistant />
    </div>
  </div>
}

function LanguageSwitcher({ language, languages, changeLanguage, t }) {
  return <Popover label={t('common.language')} className="language-menu preference-options" trigger={<>{languages.find((item) => item.code === language)?.shortLabel}<Icon name="down" size={12} /></>}>
    <div className="preference-options__list" role="group" aria-label={t('common.language')}>{languages.map((item) => <button type="button" data-close-popover key={item.code} aria-pressed={language === item.code} onClick={() => changeLanguage(item.code)}>{item.label}</button>)}</div>
  </Popover>
}
