import { cloneElement, useEffect, useId, useRef, useState } from 'react'
import { useTranslation } from '../../i18n/index.jsx'
import Icon from './Icon.jsx'
import { usePresence } from '../../motion/Motion.jsx'

export function Button({ children, variant = 'primary', busy = false, disabled, className = '', ...props }) {
  return <button type="button" {...props} disabled={disabled || busy} aria-busy={busy || undefined} className={`ui-button ui-button--${variant} ${className}`}>{busy && <span className="ui-spinner" aria-hidden="true" />}{children}</button>
}
export function Card({ children, className = '', ...props }) { return <section className={`ui-card ${className}`} {...props}>{children}</section> }
export function Badge({ children, tone = 'neutral', className = '', ...props }) { return <span className={`ui-badge ui-badge--${tone} ${className}`} {...props}>{children}</span> }
export function Alert({ children, tone = 'info', title, className = '', ...props }) {
  return <div role={tone === 'danger' ? 'alert' : 'status'} className={`ui-alert ui-alert--${tone} ${className}`} {...props}><Icon name={tone === 'success' ? 'check' : 'info'} /><div>{title && <strong>{title}</strong>}{children}</div></div>
}
export function LoadingSpinner({ label, className = '' }) {
  const { t } = useTranslation()
  return <div role="status" className={`ui-loading ${className}`}><span className="ui-spinner" aria-hidden="true" /><span>{label || t('ui.loading')}</span></div>
}
export function Skeleton({ className = '', ...props }) { return <span className={`ui-skeleton ${className}`} aria-hidden="true" {...props} /> }
export function PageSkeleton() {
  const { t } = useTranslation()
  return <div className="ui-page-skeleton" role="status" aria-label={t('ui.loading')}><Skeleton className="ui-skeleton--heading" /><Skeleton /><div className="ui-page-skeleton__cards">{[0, 1, 2].map((item) => <div className="ui-card" key={item}><Skeleton className="ui-skeleton--heading" /><Skeleton /><Skeleton /></div>)}</div><span className="sr-only">{t('ui.loading')}</span></div>
}
export function EmptyState({ title, description, action, onAction, compact = false, className = '', icon = 'file' }) {
  const { t } = useTranslation()
  return <div className={`ui-empty ${compact ? 'ui-empty--compact' : ''} ${className}`}><span className="ui-empty__icon"><Icon name={icon} size={24} /></span><h3>{title || t('ui.emptyTitle')}</h3><p>{description || t('ui.emptyDescription')}</p>{action && onAction && <Button variant="secondary" onClick={onAction}>{action}</Button>}</div>
}
export function ErrorState({ title, description, onRetry }) {
  const { t } = useTranslation()
  return <div className="ui-error-state" role="alert"><Icon name="info" size={28} /><h3>{title || t('ui.errorTitle')}</h3>{description && <p>{description}</p>}{onRetry && <Button variant="secondary" onClick={onRetry}>{t('ui.retry')}</Button>}</div>
}
export function Tooltip({ label, children }) {
  const id = useId()
  const [dismissed, setDismissed] = useState(false)
  return <span className={`ui-tooltip ${dismissed ? 'is-dismissed' : ''}`} onKeyDown={(event) => { if (event.key === 'Escape') { setDismissed(true); event.stopPropagation() } }} onMouseLeave={() => setDismissed(false)} onBlur={() => setDismissed(false)}>
    {cloneElement(children, { 'aria-describedby': [children.props['aria-describedby'], id].filter(Boolean).join(' ') })}<span className="ui-tooltip__label" id={id} role="tooltip">{label}</span>
  </span>
}
export function Toast({ message, tone = 'success', onDismiss, duration = tone === 'danger' || tone === 'warning' ? 0 : 6000 }) {
  const { t } = useTranslation()
  const ref = useRef(null)
  const last = useRef({ message, tone })
  if (message) last.current = { message, tone }
  const present = usePresence(Boolean(message), ref)
  const [paused, setPaused] = useState(false)
  useEffect(() => {
    if (!message || !onDismiss || !duration || paused) return undefined
    const timer = window.setTimeout(onDismiss, duration)
    return () => window.clearTimeout(timer)
  }, [message, duration, onDismiss, paused])
  if (!present) return null
  return <div ref={ref} className="ui-toast" inert={!message} onPointerEnter={() => setPaused(true)} onPointerLeave={() => setPaused(false)} onFocus={() => setPaused(true)} onBlur={(event) => { if (!event.currentTarget.contains(event.relatedTarget)) setPaused(false) }}><Alert tone={last.current.tone}>{last.current.message}</Alert>{onDismiss && <button type="button" className="ui-icon-button" onClick={onDismiss} aria-label={t('ui.closeNotification')}><Icon name="close" size={18} /></button>}</div>
}
