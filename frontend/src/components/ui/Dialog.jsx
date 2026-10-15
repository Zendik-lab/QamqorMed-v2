import { useEffect, useId, useRef, useState } from 'react'
import { createPortal } from 'react-dom'
import { translateError, useTranslation } from '../../i18n/index.jsx'
import Icon from './Icon.jsx'
import { usePresence } from '../../motion/Motion.jsx'

let scrollLocks = 0
let previousOverflow = ''

// Native modal dialogs contain focus and make the background inert. Portals
// keep overlays out of clipped/sticky containers in existing pages.
export function Modal({ open, onClose, title, children, className = '', id, drawer = false, footer }) {
  const { t } = useTranslation()
  const titleId = useId()
  const ref = useRef(null)
  const present = usePresence(open, ref, drawer ? 'drawer' : 'panel')
  const closeRef = useRef(onClose)
  closeRef.current = onClose
  useEffect(() => {
    const dialog = ref.current
    if (!present || !dialog) return undefined
    const returnFocus = document.activeElement
    if (scrollLocks++ === 0) { previousOverflow = document.body.style.overflow; document.body.style.overflow = 'hidden' }
    dialog.showModal()
    return () => {
      dialog.close()
      if (--scrollLocks === 0) document.body.style.overflow = previousOverflow
      if (returnFocus?.isConnected) returnFocus.focus({ preventScroll: true })
    }
  }, [present])
  return createPortal(<dialog ref={ref} id={id} data-closing={present && !open} inert={!open} className={`ui-dialog ${drawer ? 'ui-dialog--drawer' : ''} ${className}`} aria-labelledby={titleId}
    onCancel={(event) => { event.preventDefault(); closeRef.current?.() }}
    onClick={(event) => {
      if (event.target !== event.currentTarget) return
      const rect = event.currentTarget.getBoundingClientRect()
      if (event.clientX < rect.left || event.clientX > rect.right || event.clientY < rect.top || event.clientY > rect.bottom) closeRef.current?.()
    }}>
    <div className="ui-dialog__header"><h2 id={titleId}>{title}</h2><button className="ui-icon-button" type="button" aria-label={t('common.close')} onClick={onClose}><Icon name="close" /></button></div>
    <div className="ui-dialog__body">{children}</div>
    {footer && <div className="ui-dialog__footer">{footer}</div>}
  </dialog>, document.body)
}

export function Drawer(props) { return <Modal {...props} drawer /> }

export function ConfirmDialog({ open, onClose, onConfirm, title, description, confirmLabel, danger = true }) {
  const { t } = useTranslation()
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState('')
  useEffect(() => { if (open) setError('') }, [open])
  const confirm = async () => {
    setBusy(true); setError('')
    try { await onConfirm(); onClose() }
    catch (requestError) { setError(translateError(requestError, t)) }
    finally { setBusy(false) }
  }
  return <Modal open={open} onClose={() => { if (!busy) onClose() }} title={title} footer={<>
    <button className="ui-button ui-button--secondary" type="button" disabled={busy} onClick={onClose}>{t('common.cancel')}</button>
    <button className={`ui-button ${danger ? 'ui-button--danger' : ''}`} type="button" disabled={busy} onClick={confirm}>{busy ? t('common.loading') : confirmLabel || t('ui.confirm')}</button>
  </>}><p>{description}</p>{error && <p role="alert" className="error-message">{error}</p>}</Modal>
}
