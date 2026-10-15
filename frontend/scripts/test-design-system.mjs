import assert from 'node:assert/strict'
import { readFileSync, readdirSync } from 'node:fs'
import { test } from 'node:test'
import vm from 'node:vm'
import { workspaceMenus, mobilePrimary, workspaceSection, isWorkspacePathActive } from '../src/config/navigation.js'

const read = (name) => readFileSync(new URL(`../${name}`, import.meta.url), 'utf8')
const bootstrap = read('public/accessibility-init.js')
function browser({ saved = null, blocked = false } = {}) {
  const storage = new Map([['qamqormed.accessibility', saved]])
  const root = { dataset: {} }
  const events = new Map()
  const window = { localStorage: {
    getItem(key) { if (blocked) throw new Error('blocked'); return storage.get(key) },
    setItem(key, value) { if (blocked) throw new Error('blocked'); storage.set(key, value) },
  }, addEventListener: (name, listener) => events.set(name, listener) }
  vm.runInNewContext(bootstrap, { window, document: { documentElement: root } })
  return { store: window.QamqorMedAccessibility, root, storage, storageEvent: (key, newValue) => events.get('storage')({ key, newValue }) }
}

test('reading preferences apply before paint and survive a fresh document', () => {
  const b = browser()
  assert.equal(b.root.dataset.textSize, 'normal')
  b.store.setPreferences({ textSize: 'large', contrast: 'high' })
  const restored = browser({ saved: b.storage.get('qamqormed.accessibility') })
  assert.equal(restored.root.dataset.textSize, 'large')
  assert.equal(restored.root.dataset.contrast, 'high')
  b.store.setPreferences({ contrast: 'normal' })
  assert.equal(b.root.dataset.textSize, 'large')
})

test('invalid or blocked storage does not prevent reading preferences', () => {
  for (const saved of ['broken', 'null', '42', '{"textSize":"invalid","contrast":"invalid"}']) {
    const b = browser({ saved })
    assert.equal(b.root.dataset.textSize, 'normal')
    assert.equal(b.root.dataset.contrast, 'normal')
  }
  const blocked = browser({ blocked: true })
  blocked.store.setPreferences({ contrast: 'high' })
  assert.equal(blocked.root.dataset.contrast, 'high')
})

test('subscriptions update once, unsubscribe, and synchronize across tabs', () => {
  const b = browser()
  let count = 0
  const unsubscribe = b.store.subscribe(() => count++)
  b.store.setPreferences({ textSize: 'large' })
  b.store.setPreferences({ textSize: 'large' })
  assert.equal(count, 1)
  b.storageEvent('other', '{"contrast":"high"}')
  assert.equal(count, 1)
  b.storageEvent('qamqormed.accessibility', '{"contrast":"high"}')
  assert.equal(b.root.dataset.contrast, 'high')
  unsubscribe()
  b.storageEvent(null, null)
  assert.equal(b.root.dataset.contrast, 'normal')
  assert.equal(count, 2)
})

const resources = Object.fromEntries(['ru', 'kk', 'en'].map((language) => [language, JSON.parse(read(`src/i18n/locales/${language}.json`))]))
function hasKey(object, key) { return key.split('.').reduce((value, part) => value?.[part], object) }
test('each role has unique, localized links to existing routes and four mobile links', () => {
  const routes = read('src/App.jsx')
  for (const [role, menu] of Object.entries(workspaceMenus)) {
    assert.equal(new Set(menu.map((item) => item[1])).size, menu.length)
    assert.equal(mobilePrimary[role].length, 4)
    for (const [key, path, , group] of menu) {
      assert.ok(routes.includes(`'${path}'`), `Missing route ${path}`)
      for (const resource of Object.values(resources)) {
        assert.ok(hasKey(resource, `nav.${key}`), `Missing nav.${key}`)
        assert.ok(hasKey(resource, `workspace.groups.${group}`))
      }
      assert.ok(path.startsWith(`/${role.toLowerCase()}`))
    }
    for (const key of mobilePrimary[role]) assert.ok(menu.some((item) => item[0] === key))
  }
  assert.ok(!isWorkspacePathActive('/patient/appointments', '/patient', 'PATIENT'))
  assert.ok(isWorkspacePathActive('/doctor/patients/42', '/doctor/patients', 'DOCTOR'))
  assert.equal(workspaceSection('/patient/medical-history', 'PATIENT'), 'nav.medicalRecord')
})

test('literal translation keys in the new UI library, shell and footer exist in all locales', () => {
  const files = ['src/layouts/AppLayout.jsx', 'src/components/WorkspaceSidebar.jsx', 'src/components/AccessibilitySettings.jsx', 'src/components/Footer.jsx', ...readdirSync(new URL('../src/components/ui/', import.meta.url)).filter((file) => file.endsWith('.jsx')).map((file) => `src/components/ui/${file}`)]
  for (const file of files) for (const [, key] of read(file).matchAll(/\bt\('([^']+)'/g)) {
    for (const [language, resource] of Object.entries(resources)) assert.ok(hasKey(resource, key), `${language}: ${key} in ${file}`)
  }
})
