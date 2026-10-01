const apiUrl = (import.meta.env.VITE_API_URL || 'http://localhost:8080').replace(/\/$/, '')
const tokenKey = 'qamqormed.accessToken'

let unauthorizedHandler = null

export class ApiError extends Error {
  constructor(status, message, errors = {}) {
    super(message)
    this.name = 'ApiError'
    this.status = status
    this.errors = errors
  }
}

export function getStoredToken() {
  return window.localStorage.getItem(tokenKey)
}

export function storeToken(token) {
  window.localStorage.setItem(tokenKey, token)
}

export function clearStoredToken() {
  window.localStorage.removeItem(tokenKey)
}

export function setUnauthorizedHandler(handler) {
  unauthorizedHandler = handler
}

async function request(path, options = {}) {
  const { handleUnauthorized = true, ...fetchOptions } = options
  const token = getStoredToken()
  const headers = {
    Accept: 'application/json',
    ...(fetchOptions.body ? { 'Content-Type': 'application/json' } : {}),
    ...fetchOptions.headers,
  }

  if (token && fetchOptions.auth !== false) {
    headers.Authorization = `Bearer ${token}`
  }

  delete fetchOptions.auth

  const response = await fetch(`${apiUrl}${path}`, {
    ...fetchOptions,
    headers,
  })

  if (!response.ok) {
    const text = await response.text()
    let payload = {}
    try {
      payload = text ? JSON.parse(text) : {}
    } catch {
      payload = {}
    }

    if (response.status === 401 && handleUnauthorized) {
      unauthorizedHandler?.()
    }

    throw new ApiError(
      response.status,
      payload.message || `Request failed with status ${response.status}`,
      payload.errors || {},
    )
  }

  const text = await response.text()
  return text ? JSON.parse(text) : null
}

export function getHealth(signal) {
  return request('/api/health', { signal })
}

export function login(credentials) {
  return request('/api/auth/login', {
    method: 'POST',
    body: JSON.stringify(credentials),
    auth: false,
    handleUnauthorized: false,
  })
}

export function register(credentials) {
  return request('/api/auth/register', {
    method: 'POST',
    body: JSON.stringify(credentials),
    auth: false,
  })
}

export function getCurrentUser() {
  return request('/api/auth/me')
}

export function changePassword(credentials) {
  return request('/api/auth/change-password', {
    method: 'PUT',
    body: JSON.stringify(credentials),
  })
}
