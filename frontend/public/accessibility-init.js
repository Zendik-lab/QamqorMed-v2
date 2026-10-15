// Apply reading preferences before the first paint, including when storage
// is disabled. This store is independent from auth and theme resolution.
;(function () {
  const key = 'qamqormed.accessibility'
  const listeners = new Set()
  let snapshot

  function parse(value) {
    try { return JSON.parse(value) || {} } catch { return {} }
  }
  function apply(value = {}) {
    const textSize = value.textSize === 'large' ? 'large' : 'normal'
    const contrast = value.contrast === 'high' ? 'high' : 'normal'
    if (snapshot?.textSize === textSize && snapshot.contrast === contrast) return
    snapshot = Object.freeze({ textSize, contrast })
    document.documentElement.dataset.textSize = textSize
    document.documentElement.dataset.contrast = contrast
    listeners.forEach((listener) => listener())
  }
  let saved
  try { saved = window.localStorage.getItem(key) } catch { /* Use defaults. */ }
  apply(parse(saved))
  window.QamqorMedAccessibility = Object.freeze({
    getSnapshot: () => snapshot,
    subscribe(listener) { listeners.add(listener); return () => listeners.delete(listener) },
    setPreferences(value) {
      apply({ ...snapshot, ...value })
      try { window.localStorage.setItem(key, JSON.stringify(snapshot)) } catch { /* Keep working in memory. */ }
    },
  })
  window.addEventListener('storage', (event) => {
    if (event.key === key || event.key === null) apply(parse(event.newValue))
  })
})()
