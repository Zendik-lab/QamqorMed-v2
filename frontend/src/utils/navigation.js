import { useEffect, useState } from 'react'

export function navigate(path, options = {}) {
  const current = `${window.location.pathname}${window.location.search}`
  if (current === path) {
    return
  }
  const method = options.replace ? 'replaceState' : 'pushState'
  window.history[method]({}, '', path)

  // Redirect can run during the first render, before useLocation has mounted
  // its listeners. Notify on the next microtask so the initial / -> /login
  // transition cannot be lost and leave the shell looking blank.
  const notify = () => {
    window.dispatchEvent(new PopStateEvent('popstate'))
    window.dispatchEvent(new Event('qamqormed:navigate'))
  }
  if (typeof queueMicrotask === 'function') {
    queueMicrotask(notify)
  } else {
    Promise.resolve().then(notify)
  }
}

export function useLocation() {
  const [location, setLocation] = useState(() => ({
    pathname: window.location.pathname,
    search: window.location.search,
  }))

  useEffect(() => {
    const update = () => setLocation({ pathname: window.location.pathname, search: window.location.search })
    window.addEventListener('popstate', update)
    window.addEventListener('qamqormed:navigate', update)
    return () => {
      window.removeEventListener('popstate', update)
      window.removeEventListener('qamqormed:navigate', update)
    }
  }, [])

  return location
}
