import assert from 'node:assert/strict'
import test from 'node:test'
import { confirmSessionAndNotify, withBearerToken } from '../src/api/authSession.js'

const response = (status) => ({ status })

test('streaming requests retain the JWT Authorization header', () => {
  assert.deepEqual(withBearerToken({ Accept: 'text/event-stream' }, 'jwt-value'), {
    Accept: 'text/event-stream',
    Authorization: 'Bearer jwt-value',
  })
})

test('streaming can use the authenticated session token directly', () => {
  assert.equal(withBearerToken({}, 'session-jwt').Authorization, 'Bearer session-jwt')
})

test('a feature 401 does not logout when the canonical session is still valid', async () => {
  let logoutCount = 0
  let requestedUrl = ''
  const invalid = await confirmSessionAndNotify({
    fetchImpl: async (url) => {
      requestedUrl = url
      return response(200)
    },
    sessionUrl: '/api/auth/me',
    token: 'valid-jwt',
    getCurrentToken: () => 'valid-jwt',
    onInvalid: () => { logoutCount += 1 },
  })

  assert.equal(requestedUrl, '/api/auth/me')
  assert.equal(invalid, false)
  assert.equal(logoutCount, 0)
})

test('Ollama/server errors and timeouts never trigger session invalidation', async () => {
  let logoutCount = 0
  for (const status of [500, 502, 503]) {
    const invalid = await confirmSessionAndNotify({
      fetchImpl: async () => response(status),
      sessionUrl: '/api/auth/me',
      token: 'valid-jwt',
      onInvalid: () => { logoutCount += 1 },
    })
    assert.equal(invalid, false)
  }

  const timeout = await confirmSessionAndNotify({
    fetchImpl: async () => { throw new Error('timeout') },
    sessionUrl: '/api/auth/me',
    token: 'valid-jwt',
    onInvalid: () => { logoutCount += 1 },
  })

  assert.equal(timeout, false)
  assert.equal(logoutCount, 0)
})

test('a feature 401 logs out only after /api/auth/me also returns 401', async () => {
  let logoutCount = 0
  const invalid = await confirmSessionAndNotify({
    fetchImpl: async () => response(401),
    sessionUrl: '/api/auth/me',
    token: 'expired-jwt',
    getCurrentToken: () => 'expired-jwt',
    onInvalid: () => { logoutCount += 1 },
  })

  assert.equal(invalid, true)
  assert.equal(logoutCount, 1)
})

test('a request without a token cannot clear an existing session', async () => {
  let fetchCount = 0
  let logoutCount = 0
  const invalid = await confirmSessionAndNotify({
    fetchImpl: async () => {
      fetchCount += 1
      return response(401)
    },
    sessionUrl: '/api/auth/me',
    token: null,
    onInvalid: () => { logoutCount += 1 },
  })

  assert.equal(invalid, false)
  assert.equal(fetchCount, 0)
  assert.equal(logoutCount, 0)
})
