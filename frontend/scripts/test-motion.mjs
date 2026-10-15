import assert from 'node:assert/strict'
import { test } from 'node:test'
import { playMotion, variants, isNearBottom } from '../src/motion/motion.js'

function runtime({ reduced = false, duration = '220ms' } = {}) {
  const listeners = new Set()
  const media = { matches: reduced, addEventListener: (_, listener) => listeners.add(listener), removeEventListener: (_, listener) => listeners.delete(listener) }
  let resolve, reject
  const finished = new Promise((ok, fail) => { resolve = ok; reject = fail })
  const calls = []
  const animation = { finished, cancelCount: 0, cancel() { this.cancelCount++; reject(new Error('cancelled')) } }
  const element = { animate: (frames, options) => { calls.push({ frames, options }); return animation } }
  const browser = { document: { documentElement: {} }, matchMedia: () => media, getComputedStyle: () => ({ getPropertyValue: (key) => key.startsWith('--ease') ? 'ease-out' : key === '--duration-fast' ? '140ms' : duration }) }
  return { element, browser, media, animation, listeners, calls, resolve, reduce: () => { media.matches = true; for (const listener of listeners) listener() } }
}

test('reduced motion and missing animation support leave content immediately usable', () => {
  const r = runtime({ reduced: true })
  assert.equal(playMotion(r.element, 'page', {}, r.browser), null)
  assert.equal(r.calls.length, 0)
  assert.equal(playMotion({}, 'page', {}, r.browser), null)
})
test('page motion uses CSS timing and cannot animate layout or repeat indefinitely', () => {
  const r = runtime()
  const motion = playMotion(r.element, 'page', {}, r.browser)
  assert.equal(r.calls[0].options.duration, 220)
  assert.equal(r.calls[0].options.fill, 'none')
  assert.equal(r.calls[0].options.iterations, undefined)
  for (const spec of Object.values(variants)) for (const frame of [...spec.enter, ...(spec.exit || [])]) {
    assert.ok(Object.keys(frame).every((key) => ['opacity', 'transform'].includes(key)))
  }
  motion.cancel()
})
test('exit uses fast timing while preserving the same variant and element', () => {
  const r = runtime()
  const motion = playMotion(r.element, 'drawer', { exiting: true }, r.browser)
  assert.equal(r.calls[0].options.duration, 140)
  assert.deepEqual(r.calls[0].frames, variants.drawer.exit)
  motion.cancel()
})
test('preference changes cancel active motion and settle a pending exit', async () => {
  const r = runtime()
  const motion = playMotion(r.element, 'panel', { exiting: true }, r.browser)
  assert.equal(r.listeners.size, 1)
  r.reduce()
  await motion.finished
  assert.equal(r.animation.cancelCount, 1)
  assert.equal(r.listeners.size, 0)
})
test('completion and cancellation release listeners and cancellation is safe to await', async () => {
  for (const cancel of [true, false]) {
    const r = runtime()
    const motion = playMotion(r.element, 'fade', {}, r.browser)
    if (cancel) motion.cancel(); else r.resolve()
    await motion.finished
    assert.equal(r.listeners.size, 0)
  }
})
test('timings support seconds and invalid or unavailable CSS disables motion', () => {
  const seconds = runtime({ duration: '.22s' })
  playMotion(seconds.element, 'page', {}, seconds.browser).cancel()
  assert.equal(seconds.calls[0].options.duration, 220)
  for (const duration of ['', 'invalid', '0ms']) {
    const r = runtime({ duration })
    assert.equal(playMotion(r.element, 'page', {}, r.browser), null)
    assert.equal(r.listeners.size, 0)
  }
})
test('unknown variants fall back to a readable fade', () => {
  const r = runtime()
  const motion = playMotion(r.element, 'unknown', {}, r.browser)
  assert.deepEqual(r.calls[0].frames, variants.fade.enter)
  motion.cancel()
})
test('chat follow distinguishes bottom, scrollback, fractional and short histories', () => {
  assert.ok(isNearBottom({ scrollHeight: 1000, scrollTop: 600, clientHeight: 400 }))
  assert.ok(isNearBottom({ scrollHeight: 1000, scrollTop: 536.5, clientHeight: 400 }))
  assert.ok(!isNearBottom({ scrollHeight: 1000, scrollTop: 100, clientHeight: 400 }))
  assert.ok(isNearBottom({ scrollHeight: 200, scrollTop: 0, clientHeight: 400 }))
})
