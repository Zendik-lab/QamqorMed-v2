import { useEffect, useRef, useState } from 'react'
import { usePresence } from '../../motion/Motion.jsx'

// A disclosure, not an ARIA menu: ordinary links, fields and Tab navigation
// remain native and don't require a second keyboard interaction model.
export default function Popover({ label, trigger, children, className = '' }) {
  const ref = useRef(null)
  const panelRef = useRef(null)
  const [open, setOpen] = useState(false)
  const present = usePresence(open, panelRef)
  useEffect(() => {
    if (!open) return undefined
    const dismiss = (event) => {
      if (!ref.current?.contains(event.target)) setOpen(false)
    }
    const escape = (event) => {
      if (event.key !== 'Escape') return
      event.stopPropagation()
      setOpen(false)
      ref.current?.querySelector('summary')?.focus()
    }
    document.addEventListener('pointerdown', dismiss)
    ref.current?.addEventListener('keydown', escape)
    const element = ref.current
    return () => { document.removeEventListener('pointerdown', dismiss); element?.removeEventListener('keydown', escape) }
  }, [open])
  return <details ref={ref} open={present} className={`ui-popover ${className}`}>
    <summary className="ui-popover__trigger" aria-label={label} aria-expanded={open} onClick={(event) => { event.preventDefault(); setOpen((current) => !current) }}>{trigger}</summary>
    <div ref={panelRef} className="ui-popover__panel" inert={!open} aria-label={label} onClick={(event) => {
      if (event.target.closest('a, [data-close-popover]')) { setOpen(false); ref.current?.querySelector('summary')?.focus({ preventScroll: true }) }
    }}>{children}</div>
  </details>
}
