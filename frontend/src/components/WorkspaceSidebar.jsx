import { useLayoutEffect, useRef } from 'react'
import { useTranslation } from '../i18n/index.jsx'
import { workspaceMenus, isWorkspacePathActive } from '../config/navigation.js'
import AppLink from './AppLink.jsx'
import Brand from './Brand.jsx'
import Icon from './ui/Icon.jsx'

export default function WorkspaceSidebar({ role, pathname, collapsed = false, onCollapse, onNavigate, onLogout, mobile = false }) {
  const { t } = useTranslation()
  const navRef = useRef(null)
  const indicatorRef = useRef(null)
  useLayoutEffect(() => {
    const nav = navRef.current
    const update = () => {
      const active = nav?.querySelector('[aria-current="page"]')
      const indicator = indicatorRef.current
      if (!indicator) return
      indicator.style.opacity = active ? '1' : '0'
      if (active) {
        const bounds = active.getBoundingClientRect()
        const top = bounds.top - nav.getBoundingClientRect().top + nav.scrollTop
        indicator.style.transform = `translateY(${top + 8}px) scaleY(${Math.max(1, bounds.height - 16)})`
      }
    }
    update()
    const observer = new ResizeObserver(update)
    if (nav) observer.observe(nav)
    return () => observer.disconnect()
  }, [pathname, collapsed, t])
  const groups = [...new Set((workspaceMenus[role] || []).map((item) => item[3]))]
  return <aside className={`workspace-sidebar ${collapsed ? 'is-collapsed' : ''} ${mobile ? 'is-mobile' : ''}`} aria-label={t(`roles.${role}`)}>
    <div className="workspace-sidebar__brand"><Brand onClick={onNavigate} />{!mobile && <button className="ui-icon-button sidebar-collapse" type="button" aria-label={t(collapsed ? 'workspace.expandSidebar' : 'workspace.collapseSidebar')} title={t(collapsed ? 'workspace.expandSidebar' : 'workspace.collapseSidebar')} onClick={onCollapse}><Icon name={collapsed ? 'expand' : 'collapse'} size={18} /></button>}</div>
    <div className="workspace-sidebar__role"><span className="workspace-sidebar__role-dot" aria-hidden="true" /><span>{t(`roles.${role}`)}</span></div>
    <nav ref={navRef} className="workspace-sidebar__nav" aria-label={t('workspace.navigation')}>
      <span ref={indicatorRef} className="workspace-active-indicator" aria-hidden="true" />
      {groups.map((group) => <div className="workspace-sidebar__group" key={group}><div className="workspace-sidebar__group-label">{t(`workspace.groups.${group}`)}</div>
        {workspaceMenus[role].filter((item) => item[3] === group).map(([key, path, icon]) => {
          const active = isWorkspacePathActive(pathname, path, role)
          return <AppLink className={`workspace-nav-link ${active ? 'is-active' : ''}`} href={path} key={path} onClick={onNavigate} aria-current={active ? 'page' : undefined} aria-label={collapsed ? t(`nav.${key}`) : undefined} title={collapsed ? t(`nav.${key}`) : undefined}>
            <Icon name={icon} /><span className="workspace-nav-link__label">{t(`nav.${key}`)}</span>{collapsed && <span className="workspace-nav-link__tooltip" aria-hidden="true">{t(`nav.${key}`)}</span>}
          </AppLink>
        })}
      </div>)}
    </nav>
    <div className="workspace-sidebar__footer">
      <AppLink href="/help" className="workspace-nav-link" onClick={onNavigate} aria-label={collapsed ? t('footer.help') : undefined} title={collapsed ? t('footer.help') : undefined}><Icon name="help" /><span className="workspace-nav-link__label">{t('footer.help')}</span></AppLink>
      <button className="workspace-nav-link workspace-nav-link--logout" type="button" onClick={onLogout} aria-label={collapsed ? t('common.logout') : undefined} title={collapsed ? t('common.logout') : undefined}><Icon name="logout" /><span className="workspace-nav-link__label">{t('common.logout')}</span></button>
    </div>
  </aside>
}
