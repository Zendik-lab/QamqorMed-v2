import { navigate } from '../utils/navigation.js'

// Preserve native open-in-new-tab and copy-link behaviour while using the
// application's existing history router for ordinary navigation.
export default function AppLink({ href, children, onClick, ...props }) {
  return <a {...props} href={href} onClick={(event) => {
    onClick?.(event)
    if (event.defaultPrevented || event.button !== 0 || event.metaKey || event.ctrlKey || event.shiftKey || event.altKey || props.target || props.download) return
    event.preventDefault()
    navigate(href)
    window.scrollTo({ top: 0, left: 0, behavior: 'auto' })
  }}>{children}</a>
}
