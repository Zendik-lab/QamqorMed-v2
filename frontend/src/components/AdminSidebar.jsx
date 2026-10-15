import { useAuth } from '../auth/AuthContext.jsx'
import { useTranslation } from '../i18n/index.jsx'
import { navigate, useLocation } from '../utils/navigation.js'

export const adminMenuItems = [
  ['dashboard', '/admin', '⌂'],
  ['regions', '/admin/regions', '⌖'],
  ['cities', '/admin/cities', '▦'],
  ['clinics', '/admin/clinics', '＋'],
  ['specialties', '/admin/specialties', '✦'],
  ['documentTemplates', '/admin/document-templates', '▤'],
  ['doctors', '/admin/doctors', '♧'],
  ['nurses', '/admin/nurses', '♧'],
  ['schedule', '/admin/schedule', '◷'],
  ['profile', '/admin/profile', '◉'],
]

function AdminSidebar({ open = false, onClose }) {
  const { logout } = useAuth()
  const { t } = useTranslation()
  const location = useLocation()

  const isActive = (path) => path === '/admin'
    ? location.pathname === path
    : location.pathname === path || location.pathname.startsWith(`${path}/`)

  const goTo = (path) => {
    navigate(path)
    onClose?.()
  }

  return (
    <aside id="app-sidebar" className={`dashboard-sidebar ${open ? 'dashboard-sidebar--open' : ''}`}>
      <div className="sidebar-brand">
        <button className="brand-link" type="button" onClick={() => goTo('/')} aria-label="QamqorMed">
          <span className="brand-mark" aria-hidden="true">Q</span>
          <span className="brand-name">QamqorMed</span>
        </button>
      </div>
      <div className="sidebar-role"><span>QamqorMed</span><strong>{t('roles.ADMIN')}</strong></div>
      <nav className="sidebar-nav" aria-label={t('roles.ADMIN')}>
        {adminMenuItems.map(([key, path, icon]) => {
          const active = isActive(path)
          return (
            <button
              className={`sidebar-link ${active ? 'sidebar-link--active' : ''}`}
              type="button"
              key={path}
              onClick={() => goTo(path)}
              aria-current={active ? 'page' : undefined}
            >
              <span className="sidebar-link__icon" aria-hidden="true">{icon}</span>
              <span>{t(`nav.${key}`)}</span>
            </button>
          )
        })}
      </nav>
      <div className="sidebar-footer">
        <button className="sidebar-link sidebar-link--logout" type="button" onClick={() => logout()}>
          <span className="sidebar-link__icon" aria-hidden="true">↪</span>
          <span>{t('common.logout')}</span>
        </button>
      </div>
    </aside>
  )
}

export default AdminSidebar
