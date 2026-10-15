import { useEffect, useState } from 'react'
import { getNotifications, getUnreadNotificationCount, markAllNotificationsRead, markNotificationRead } from '../api/apiClient.js'
import { localeFor, translateError, useTranslation } from '../i18n/index.jsx'
import { navigate } from '../utils/navigation.js'

export default function NotificationCenter() {
  const { t, language } = useTranslation()
  const [items, setItems] = useState([])
  const [count, setCount] = useState(0)
  const [open, setOpen] = useState(false)
  const [error, setError] = useState('')

  const refreshCount = () => getUnreadNotificationCount().then((value) => setCount(Number(value?.count || 0))).catch(() => {})
  const refresh = () => getNotifications().then((value) => { setItems(value || []); setCount((value || []).filter((item) => !item.read).length); setError('') }).catch((e) => setError(translateError(e, t)))

  useEffect(() => {
    refreshCount()
    const timer = window.setInterval(refreshCount, 45000)
    return () => window.clearInterval(timer)
  }, [])

  useEffect(() => { if (open) refresh() }, [open])

  const read = async (item) => {
    if (item.read) return
    try { await markNotificationRead(item.id); setItems((current) => current.map((value) => value.id === item.id ? { ...value, read: true, readAt: new Date().toISOString() } : value)); setCount((value) => Math.max(0, value - 1)) } catch (e) { setError(translateError(e, t)) }
  }
  const readAll = async () => { try { await markAllNotificationsRead(); setItems((current) => current.map((item) => ({ ...item, read: true }))); setCount(0) } catch (e) { setError(translateError(e, t)) } }
  const openAction = async (item) => { await read(item); if (item.actionUrl) { setOpen(false); navigate(item.actionUrl) } }

  return <div className="notification-center">
    <button className="notification-trigger" type="button" aria-label={t('notifications.title')} aria-expanded={open} onClick={() => setOpen((value) => !value)}>
      <span aria-hidden="true">🔔</span>{count > 0 && <span className="notification-badge">{count > 99 ? '99+' : count}</span>}
    </button>
    {open && <div className="notification-panel" role="dialog" aria-label={t('notifications.title')}>
      <div className="notification-panel__header"><div><span className="eyebrow">QamqorMed</span><h2>{t('notifications.title')}</h2></div><button className="drawer-close" type="button" aria-label={t('common.close')} onClick={() => setOpen(false)}>×</button></div>
      {count > 0 && <button className="text-button notification-read-all" type="button" onClick={readAll}>{t('notifications.readAll')}</button>}
      {error && <p className="error-message">{error}</p>}
      <div className="notification-list">{items.length ? items.map((item) => <article className={`notification-item ${item.read ? '' : 'notification-item--unread'}`} key={item.id}>
        <button className="notification-item__body" type="button" onClick={() => openAction(item)}><strong>{item.title}</strong><span>{item.message}</span><time dateTime={item.createdAt}>{new Intl.DateTimeFormat(localeFor(language), { day: 'numeric', month: 'short', hour: '2-digit', minute: '2-digit' }).format(new Date(item.createdAt))}</time></button>
        <div className="notification-item__actions">{!item.read && <button className="text-button" type="button" onClick={() => read(item)}>{t('notifications.markRead')}</button>}{item.actionUrl && <button className="text-button" type="button" onClick={() => openAction(item)}>{t('notifications.open')}</button>}</div>
      </article>) : <p className="notification-empty">{t('notifications.empty')}</p>}</div>
    </div>}
  </div>
}
