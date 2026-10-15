import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'
import { test } from 'node:test'
import vm from 'node:vm'

const bootstrap = readFileSync(new URL('../public/theme-init.js', import.meta.url), 'utf8')

function browser({ saved = null, dark = false, blockedStorage = false, legacyMedia = false } = {}) {
  const storage = new Map(saved === null ? [] : [['qamqormed.theme', saved]])
  const windowEvents = new Map()
  let mediaListener
  let metaColor
  const media = { matches: dark }
  media[legacyMedia ? 'addListener' : 'addEventListener'] = (...args) => { mediaListener = args.at(-1) }
  const root = { dataset: {}, style: {} }
  const window = {
    matchMedia: () => media,
    localStorage: {
      getItem(key) { if (blockedStorage) throw new Error('disabled'); return storage.get(key) ?? null },
      setItem(key, value) { if (blockedStorage) throw new Error('disabled'); storage.set(key, value) },
    },
    addEventListener: (name, listener) => windowEvents.set(name, listener),
  }
  vm.runInNewContext(bootstrap, {
    window,
    document: { documentElement: root, querySelector: () => ({ setAttribute: (_, value) => { metaColor = value } }) },
  })
  return {
    store: window.QamqorMedTheme, root, storage,
    system(next) { media.matches = next; mediaListener() },
    storageEvent(key, newValue) { windowEvents.get('storage')({ key, newValue }) },
    get metaColor() { return metaColor },
  }
}

test('bootstrap restores each saved mode before React runs', () => {
  for (const saved of ['light', 'dark', 'system']) {
    const b = browser({ saved, dark: true })
    assert.equal(b.root.dataset.theme, saved === 'light' ? 'light' : 'dark')
    assert.equal(b.root.dataset.themePreference, saved)
    assert.equal(b.root.style.colorScheme, b.root.dataset.theme)
  }
})

test('first visit and invalid preferences follow device settings', () => {
  for (const saved of [null, '', 'invalid']) for (const dark of [false, true]) {
    const b = browser({ saved, dark })
    assert.equal(b.root.dataset.themePreference, 'system')
    assert.equal(b.root.dataset.theme, dark ? 'dark' : 'light')
  }
})

test('all three choices persist across a new page load', () => {
  const b = browser()
  for (const preference of ['dark', 'light', 'system']) {
    b.store.setPreference(preference)
    assert.equal(b.storage.get('qamqormed.theme'), preference)
    const reloaded = browser({ saved: b.storage.get('qamqormed.theme') })
    assert.equal(reloaded.store.getSnapshot().preference, preference)
  }
})

test('system changes update mounted consumers and explicit modes stay fixed', () => {
  const b = browser()
  let updates = 0
  const stop = b.store.subscribe(() => updates++)
  const initial = b.store.getSnapshot()
  assert.equal(initial, b.store.getSnapshot(), 'React snapshots must be stable')
  b.system(true)
  assert.equal(b.root.dataset.theme, 'dark')
  assert.equal(b.metaColor, '#141a20')
  assert.equal(updates, 1)
  b.store.setPreference('light')
  b.system(false)
  b.system(true)
  assert.equal(b.root.dataset.theme, 'light')
  assert.equal(updates, 2)
  b.store.setPreference('system')
  assert.equal(b.root.dataset.theme, 'dark')
  stop()
  b.system(false)
  assert.equal(updates, 3)
})

test('cross-tab changes and clearing storage update the theme', () => {
  const b = browser({ saved: 'dark' })
  b.storageEvent('qamqormed.language', 'en')
  assert.equal(b.root.dataset.theme, 'dark')
  b.storageEvent('qamqormed.theme', 'light')
  assert.equal(b.root.dataset.theme, 'light')
  b.storageEvent(null, null)
  assert.equal(b.root.dataset.themePreference, 'system')
})

test('unavailable storage and older MediaQueryList APIs remain usable', () => {
  const b = browser({ blockedStorage: true, legacyMedia: true })
  b.store.setPreference('dark')
  assert.equal(b.root.dataset.theme, 'dark')
  b.store.setPreference('system')
  b.system(true)
  assert.equal(b.root.dataset.theme, 'dark')
})

const tokens = readFileSync(new URL('../src/styles/tokens.css', import.meta.url), 'utf8')
const palettes = [...tokens.matchAll(/:root(?:\[data-theme="dark"\])?\s*\{([^}]+)\}/g)].map((match) =>
  Object.fromEntries([...match[1].matchAll(/(--[\w-]+):\s*(#[a-f\d]{6});/g)].map((item) => [item[1], item[2]])))

function luminance(hex) {
  const channels = [1, 3, 5].map((i) => parseInt(hex.slice(i, i + 2), 16) / 255)
    .map((value) => value <= .04045 ? value / 12.92 : ((value + .055) / 1.055) ** 2.4)
  return channels[0] * .2126 + channels[1] * .7152 + channels[2] * .0722
}

test('body, footer, fields, actions and status text meet WCAG AA contrast', () => {
  const pairs = [
    ['text-primary', 'page-bg'], ['text-primary', 'surface'], ['text-primary', 'input-bg'],
    ['text-secondary', 'surface'], ['text-muted', 'surface'], ['text-muted', 'surface-subtle'],
    ['link', 'surface'], ['link', 'accent-soft'], ['footer-text', 'footer-bg'], ['footer-heading', 'footer-bg'],
    ['text-on-accent', 'accent-solid'], ['text-on-accent-muted', 'accent-solid'], ['text-on-accent', 'danger-solid'],
    ...['accent-soft', 'surface-hover', 'success-soft', 'warning-soft', 'danger-soft', 'info-soft', 'violet-soft'].map((name) => ['text-muted', name]),
    ...['success', 'warning', 'danger', 'info', 'violet'].map((name) => [`${name}-text`, `${name}-soft`]),
  ]
  for (const [index, own] of palettes.entries()) {
    const palette = { ...palettes[0], ...own }
    for (const [fg, bg] of pairs) {
      const values = [luminance(palette[`--${fg}`]), luminance(palette[`--${bg}`])].sort((a, b) => b - a)
      const contrast = (values[0] + .05) / (values[1] + .05)
      assert.ok(contrast >= 4.5, `${index ? 'dark' : 'light'} ${fg}/${bg}: ${contrast.toFixed(2)}`)
    }
  }
})

test('high-contrast reading modes and visible field boundaries meet contrast thresholds', () => {
  const extract = (selector) => Object.fromEntries([...tokens.slice(tokens.indexOf(selector)).split('}')[0].matchAll(/(--[\w-]+):\s*(#[a-f\d]{6});/g)].map((match) => [match[1], match[2]]))
  const light = palettes[0], dark = { ...light, ...palettes[1] }
  const high = extract(':root[data-contrast="high"]')
  const highDark = extract(':root[data-theme="dark"][data-contrast="high"]')
  const variants = [light, dark, { ...light, ...high }, { ...dark, ...high, ...highDark }]
  const ratio = (a, b) => { const values = [luminance(a), luminance(b)].sort((x, y) => y - x); return (values[0] + .05) / (values[1] + .05) }
  for (const palette of variants) {
    for (const text of ['text-primary', 'text-secondary', 'text-muted', 'link']) {
      for (const background of ['surface', 'page-bg', 'input-bg']) assert.ok(ratio(palette[`--${text}`], palette[`--${background}`]) >= 4.5, `${text}/${background}`)
    }
    assert.ok(ratio(palette['--secondary-text'], palette['--secondary-soft']) >= 4.5)
    assert.ok(ratio(palette['--border-strong'], palette['--input-bg']) >= 3, 'Field outline must remain distinguishable')
    assert.ok(ratio(palette['--focus'], palette['--surface']) >= 3, 'Focus must remain distinguishable')
  }
})
