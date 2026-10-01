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

function queryString(params) {
  const query = new URLSearchParams()
  Object.entries(params || {}).forEach(([key, value]) => {
    if (value !== undefined && value !== null && value !== '') query.set(key, value)
  })
  const value = query.toString()
  return value ? `?${value}` : ''
}

export const getRegions = () => request('/api/regions')
export const getRegionCities = (regionId) => request(`/api/regions/${regionId}/cities`)
export const getCityClinics = (cityId) => request(`/api/cities/${cityId}/clinics`)
export const getClinics = () => request('/api/clinics')
export const getSpecialties = () => request('/api/specialties')
export const getDoctors = (params) => request(`/api/doctors${queryString(params)}`)
export const getDoctor = (id) => request(`/api/doctors/${id}`)

export const adminGetRegions = () => request('/api/admin/regions')
export const adminGetCities = () => request('/api/admin/cities')
export const adminGetClinics = () => request('/api/admin/clinics')
export const adminGetSpecialties = () => request('/api/admin/specialties')
export const adminGetDoctors = () => request('/api/admin/doctors')
export const adminGetNurses = () => request('/api/admin/nurses')

function adminMutation(path, method, body) {
  return request(path, { method, body: JSON.stringify(body) })
}

export const createAdminRegion = (body) => adminMutation('/api/admin/regions', 'POST', body)
export const updateAdminRegion = (id, body) => adminMutation(`/api/admin/regions/${id}`, 'PUT', body)
export const createAdminCity = (body) => adminMutation('/api/admin/cities', 'POST', body)
export const updateAdminCity = (id, body) => adminMutation(`/api/admin/cities/${id}`, 'PUT', body)
export const createAdminClinic = (body) => adminMutation('/api/admin/clinics', 'POST', body)
export const updateAdminClinic = (id, body) => adminMutation(`/api/admin/clinics/${id}`, 'PUT', body)
export const createAdminSpecialty = (body) => adminMutation('/api/admin/specialties', 'POST', body)
export const updateAdminSpecialty = (id, body) => adminMutation(`/api/admin/specialties/${id}`, 'PUT', body)
export const createAdminDoctor = (body) => adminMutation('/api/admin/doctors', 'POST', body)
export const updateAdminDoctor = (id, body) => adminMutation(`/api/admin/doctors/${id}`, 'PUT', body)
export const createAdminNurse = (body) => adminMutation('/api/admin/nurses', 'POST', body)
export const updateAdminNurse = (id, body) => adminMutation(`/api/admin/nurses/${id}`, 'PUT', body)
