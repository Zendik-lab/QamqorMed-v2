// CSS tokens are the source of timing values. Variants only animate compositor
// properties; no animation is allowed to control application state or requests.
export const variants = {
  page: { duration: '--duration-normal', enter: [{ opacity: 0, transform: 'translateY(7px)' }, { opacity: 1, transform: 'none' }] },
  fade: { duration: '--duration-normal', enter: [{ opacity: 0 }, { opacity: 1 }] },
  results: { duration: '--duration-fast', enter: [{ opacity: .65 }, { opacity: 1 }] },
  panel: { duration: '--duration-normal', enter: [{ opacity: 0, transform: 'translateY(6px) scale(.99)' }, { opacity: 1, transform: 'none' }], exit: [{ opacity: 1 }, { opacity: 0, transform: 'translateY(4px)' }] },
  drawer: { duration: '--duration-normal', enter: [{ opacity: 0, transform: 'translateX(-24px)' }, { opacity: 1, transform: 'none' }], exit: [{ opacity: 1 }, { opacity: 0, transform: 'translateX(-18px)' }] },
}

export function playMotion(element, variant = 'fade', { exiting = false } = {}, browser = globalThis.window) {
  if (!element?.animate || !browser?.matchMedia) return null
  const media = browser.matchMedia('(prefers-reduced-motion: reduce)')
  if (media.matches) return null
  const spec = variants[variant] || variants.fade
  const tokens = browser.getComputedStyle(browser.document.documentElement)
  const raw = tokens.getPropertyValue(exiting ? '--duration-fast' : spec.duration).trim()
  const duration = raw.endsWith('ms') ? parseFloat(raw) : parseFloat(raw) * 1000
  // If styles are unavailable, remain fully functional without motion.
  if (!Number.isFinite(duration) || duration <= 0) return null
  const animation = element.animate(exiting ? spec.exit || [...spec.enter].reverse() : spec.enter, {
    duration, easing: tokens.getPropertyValue(exiting ? '--ease-exit' : '--ease-standard').trim() || 'ease-out',
    fill: 'none',
  })
  let disposed = false
  const reduce = () => { if (media.matches) animation.cancel() }
  const cleanup = () => {
    if (disposed) return
    disposed = true
    media.removeEventListener('change', reduce)
  }
  media.addEventListener('change', reduce)
  // A preference change must also finish a pending exit immediately.
  animation.finished.then(cleanup, cleanup)
  return { finished: animation.finished.catch(() => {}), cancel: () => { cleanup(); animation.cancel() } }
}

export function isNearBottom(element, tolerance = 64) {
  return element.scrollHeight - element.scrollTop - element.clientHeight <= tolerance
}
