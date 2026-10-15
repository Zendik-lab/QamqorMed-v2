// Runs synchronously in <head>, before styles or React can paint. This store
// owns theme resolution for both the initial page and the React provider.
;(function () {
  const storageKey = 'qamqormed.theme'
  const modes = ['light', 'dark', 'system']
  const normalize = (value) => modes.includes(value) ? value : 'system'
  const media = window.matchMedia('(prefers-color-scheme: dark)')
  const listeners = new Set()
  let preference = 'system'
  let snapshot

  try { preference = normalize(window.localStorage.getItem(storageKey)) } catch { /* Storage may be disabled. */ }

  function apply() {
    const resolved = preference === 'system' ? (media.matches ? 'dark' : 'light') : preference
    if (snapshot?.preference === preference && snapshot.resolved === resolved) return
    snapshot = Object.freeze({ preference, resolved })
    document.documentElement.dataset.theme = resolved
    document.documentElement.dataset.themePreference = preference
    document.documentElement.style.colorScheme = resolved
    document.querySelector('meta[name="theme-color"]')?.setAttribute('content', resolved === 'dark' ? '#141a20' : '#f4f8fb')
    listeners.forEach((listener) => listener())
  }

  window.QamqorMedTheme = Object.freeze({
    getSnapshot: () => snapshot,
    subscribe(listener) {
      listeners.add(listener)
      return () => listeners.delete(listener)
    },
    setPreference(value) {
      preference = normalize(value)
      try { window.localStorage.setItem(storageKey, preference) } catch { /* Keep working in memory. */ }
      apply()
    },
  })

  if (media.addEventListener) media.addEventListener('change', apply)
  else media.addListener(apply)
  window.addEventListener('storage', (event) => {
    if (event.key !== storageKey && event.key !== null) return
    preference = normalize(event.newValue)
    apply()
  })
  apply()
})()
