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
export const getClinicsPage = (params) => request(`/api/clinics/page${queryString(params)}`)
export const getSpecialties = () => request('/api/specialties')
export const getCitySpecialties = (cityId) => request(`/api/specialties/by-city/${cityId}`)
export const getDoctors = (params) => request(`/api/doctors${queryString(params)}`)
export const getDoctorsPage = (params) => request(`/api/doctors/page${queryString(params)}`)
export const searchDoctors = (params, signal) => request(`/api/doctors/search${queryString(params)}`, { signal })
export const getDoctorTimeSlots = (doctorId, params = {}, signal) => request(`/api/doctors/${doctorId}/time-slots${queryString(params)}`, { signal })
export const getDoctor = (id) => request(`/api/doctors/${id}`)
export const getDoctorNurse = (id) => request(`/api/doctors/${id}/nurse`)
export const getClinicSpecialties = (id) => request(`/api/clinics/${id}/specialties`)

export const getPatientProfile = () => request('/api/patient/profile')
export const updatePatientProfile = (body) => request('/api/patient/profile', { method: 'PUT', body: JSON.stringify(body) })
export const getPatientLocation = () => request('/api/patient/location')
export const updatePatientLocation = (body) => request('/api/patient/location', { method: 'PUT', body: JSON.stringify(body) })
export const getLocationRegions = () => request('/api/locations/regions')
export const getLocationCities = (regionId) => request(`/api/locations/regions/${regionId}/cities`)
export const searchClinics = (params) => request(`/api/clinics/search${queryString(params)}`)
export const getDependents = () => request('/api/patient/dependents')
export const getDependent = (id) => request(`/api/patient/dependents/${id}`)
export const createDependent = (body) => request('/api/patient/dependents', { method: 'POST', body: JSON.stringify(body) })
export const updateDependent = (id, body) => request(`/api/patient/dependents/${id}`, { method: 'PUT', body: JSON.stringify(body) })
export const deleteDependent = (id) => request(`/api/patient/dependents/${id}`, { method: 'DELETE' })
export const getPatientAppointments = () => request('/api/patient/appointments')
export const createPatientAppointment = (body) => request('/api/patient/appointments', { method: 'POST', body: JSON.stringify(body) })
export const cancelPatientAppointment = (id) => request(`/api/patient/appointments/${id}`, { method: 'DELETE' })
export const getPatientMedicalRecords = () => request('/api/patient/medical-records')
export const getDependentMedicalRecords = (id) => request(`/api/patient/dependents/${id}/medical-records`)
export const getDoctorAppointments = () => request('/api/doctor/appointments')
export const getNurseAppointments = () => request('/api/nurse/appointments')
export const createDoctorMedicalRecord = (id, body) => request(`/api/doctor/appointments/${id}/medical-record`, { method: 'POST', body: JSON.stringify(body) })
export const updateDoctorAppointmentStatus = (id, body) => request(`/api/doctor/appointments/${id}/status`, { method: 'PATCH', body: JSON.stringify(body) })
export const createNurseIntake = (id, body) => request(`/api/nurse/appointments/${id}/intake`, { method: 'POST', body: JSON.stringify(body) })

export const devSearchUsers = (params, signal) => request(`/api/dev/users/search${queryString(params)}`, { signal, handleUnauthorized: false })
export const devGetRelation = (userId) => request(`/api/dev/users/${userId}/relation`, { handleUnauthorized: false })
export const devLogin = (userId) => request(`/api/dev/login/${userId}`, {
  method: 'POST',
  handleUnauthorized: false,
})
export const devGetRegions = () => request('/api/dev/filters/regions', { handleUnauthorized: false })
export const devGetCities = (regionId) => request(`/api/dev/filters/regions/${regionId}/cities`, { handleUnauthorized: false })
export const devGetClinics = (cityId) => request(`/api/dev/filters/cities/${cityId}/clinics`, { handleUnauthorized: false })
export const devGetSpecialties = (clinicId) => request(`/api/dev/filters/clinics/${clinicId}/specialties`, { handleUnauthorized: false })
export const devGetAllSpecialties = () => request('/api/dev/filters/specialties', { handleUnauthorized: false })
export const devGetIntegrityAudit = () => request('/api/dev/audit', { handleUnauthorized: false })

export const adminGetRegions = () => request('/api/admin/regions')
export const adminGetCities = () => request('/api/admin/cities')
export const adminGetClinics = () => request('/api/admin/clinics')
export const adminGetClinicsPage = (params) => request(`/api/admin/clinics/page${queryString(params)}`)
export const adminGetSpecialties = () => request('/api/admin/specialties')
export const adminGetDoctors = () => request('/api/admin/doctors')
export const adminGetNurses = () => request('/api/admin/nurses')
export const adminGetDoctorsPage = (params) => request(`/api/admin/doctors/page${queryString(params)}`)
export const adminGetNursesPage = (params) => request(`/api/admin/nurses/page${queryString(params)}`)

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
