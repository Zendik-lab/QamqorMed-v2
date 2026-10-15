import { useId, useRef, useState } from 'react'

export default function Tabs({ items, label, value, onChange }) {
  const id = useId()
  const ref = useRef(null)
  const [internal, setInternal] = useState(items[0]?.id)
  const selected = value ?? internal
  const active = items.some((item) => item.id === selected) ? selected : items[0]?.id
  const select = (next) => { setInternal(next); onChange?.(next) }
  return <div className="ui-tabs"><div ref={ref} role="tablist" aria-label={label} className="ui-tabs__list" onKeyDown={(event) => {
    const index = items.findIndex((item) => item.id === active)
    const next = event.key === 'ArrowRight' ? (index + 1) % items.length : event.key === 'ArrowLeft' ? (index - 1 + items.length) % items.length : event.key === 'Home' ? 0 : event.key === 'End' ? items.length - 1 : null
    if (next === null || !items.length) return
    event.preventDefault(); select(items[next].id); ref.current.querySelectorAll('[role="tab"]')[next]?.focus()
  }}>{items.map((item, index) => <button type="button" key={item.id} id={`${id}-tab-${index}`} role="tab" aria-selected={item.id === active} aria-controls={`${id}-panel-${index}`} tabIndex={item.id === active ? 0 : -1} onClick={() => select(item.id)}>{item.label}</button>)}</div>
    {items.map((item, index) => <div role="tabpanel" key={item.id} id={`${id}-panel-${index}`} aria-labelledby={`${id}-tab-${index}`} hidden={item.id !== active} tabIndex={0}>{item.content}</div>)}
  </div>
}
