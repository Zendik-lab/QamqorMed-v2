import { useEffect, useState } from 'react'

export function navigate(path) {
  const current = `${window.location.pathname}${window.location.search}`
  if (current === path) {
    return
  }
  window.history.pushState({}, '', path)
  window.dispatchEvent(new PopStateEvent('popstate'))
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
