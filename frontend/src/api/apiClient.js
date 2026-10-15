import { confirmSessionAndNotify, withBearerToken } from './authSession.js'

function normalizeApiBase(value) {
  const normalized = String(value || '').trim().replace(/\/+$/, '')
  if (!normalized || normalized === '/api') return ''
  // Endpoint paths below already contain /api. Accept an env value with the
  // suffix without producing /api/api/... URLs.
  return normalized.endsWith('/api') ? normalized.slice(0, -4) : normalized
}

// Empty by default means same-origin /api. The browser never needs to know
// the private address of the Spring Boot process behind the Vite proxy.
const apiBase = normalizeApiBase(import.meta.env.VITE_API_URL)
const tokenKey = 'qamqormed.accessToken'

function buildApiUrl(path) {
  const normalizedPath = String(path).startsWith('/') ? path : `/${path}`
  return `${apiBase}${normalizedPath}`
}

let unauthorizedHandler = null
let sessionCheckPromise = null

export class ApiError extends Error {
  constructor(status, message, errors = {}, code = null) {
    super(message)
    this.name = 'ApiError'
    this.status = status
    this.errors = errors
    this.code = code
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

async function handleUnauthorized(path, token) {
  // An anonymous request has no authenticated browser session to terminate.
  if (!token) return

  // /api/auth/me is the canonical session check. Its own 401 is conclusive;
  // all other endpoints must be confirmed through it before global logout.
  if (path === '/api/auth/me') {
    unauthorizedHandler?.()
    return
  }

  if (!sessionCheckPromise) {
    const check = confirmSessionAndNotify({
      sessionUrl: buildApiUrl('/api/auth/me'),
      token,
      getCurrentToken: getStoredToken,
      onInvalid: () => unauthorizedHandler?.(),
    })
    const pending = check.finally(() => {
      if (sessionCheckPromise === pending) sessionCheckPromise = null
    })
    sessionCheckPromise = pending
  }
  await sessionCheckPromise
}

async function request(path, options = {}) {
  const { handleUnauthorized: shouldHandleUnauthorized = true, ...fetchOptions } = options
  const token = getStoredToken()
  const headers = {
    Accept: 'application/json',
    ...(fetchOptions.body && !(fetchOptions.body instanceof FormData) ? { 'Content-Type': 'application/json' } : {}),
    ...fetchOptions.headers,
  }

  if (token && fetchOptions.auth !== false) {
    headers.Authorization = `Bearer ${token}`
  }

  delete fetchOptions.auth

  let response
  try {
    response = await fetch(buildApiUrl(path), { ...fetchOptions, headers })
  } catch (error) {
    if (error?.name === 'AbortError') throw error
    // Keep browser/network internals out of the UI. The stable code is
    // translated to a localized, user-safe message.
    throw new ApiError(0, 'Network request failed', {}, 'network')
  }

  if (!response.ok) {
    const text = await response.text()
    let payload = {}
    try {
      payload = text ? JSON.parse(text) : {}
    } catch {
      payload = {}
    }

    if (response.status === 401 && shouldHandleUnauthorized) await handleUnauthorized(path, token)

    throw new ApiError(
      response.status,
      payload.message || `Request failed with status ${response.status}`,
      payload.errors || {},
    )
  }

  const text = await response.text()
  return text ? JSON.parse(text) : null
}

async function requestBlob(path, options = {}) {
  const { handleUnauthorized: shouldHandleUnauthorized = true, ...fetchOptions } = options
  const token = getStoredToken()
  const headers = { Accept: 'application/pdf', ...fetchOptions.headers }
  if (token && fetchOptions.auth !== false) headers.Authorization = `Bearer ${token}`
  delete fetchOptions.auth
  let response
  try {
    response = await fetch(buildApiUrl(path), { ...fetchOptions, headers })
  } catch (error) {
    if (error?.name === 'AbortError') throw error
    throw new ApiError(0, 'Network request failed', {}, 'network')
  }
  if (!response.ok) {
    const text = await response.text()
    let payload = {}
    try { payload = text ? JSON.parse(text) : {} } catch { payload = {} }
    if (response.status === 401 && shouldHandleUnauthorized) await handleUnauthorized(path, token)
    throw new ApiError(response.status, payload.message || `Request failed with status ${response.status}`, payload.errors || {})
  }
  return response.blob()
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
export const confirmPatientAppointment = (id) => request(`/api/appointments/${id}/confirm`, { method: 'PATCH' })
export const declinePatientAppointment = (id) => request(`/api/appointments/${id}/decline`, { method: 'PATCH' })
export const getAttendance = (patientId) => request(`/api/patients/${patientId}/attendance`)
export const getNotifications = () => request('/api/notifications')
export const getUnreadNotificationCount = () => request('/api/notifications/unread-count')
export const markNotificationRead = (id) => request(`/api/notifications/${id}/read`, { method: 'PATCH' })
export const markAllNotificationsRead = () => request('/api/notifications/read-all', { method: 'PATCH' })
export const getWaitlist = () => request('/api/waitlist')
export const createWaitlist = (body) => request('/api/waitlist', { method: 'POST', body: JSON.stringify(body) })
export const cancelWaitlist = (id) => request(`/api/waitlist/${id}`, { method: 'DELETE' })
export const bookWaitlist = (id, body) => request(`/api/waitlist/${id}/book`, { method: 'POST', body: JSON.stringify(body) })
export const getAdminAppointmentStatistics = (params = {}) => request(`/api/admin/appointments/statistics${queryString(params)}`)
export const getPatientMedicalRecords = () => request('/api/patient/medical-records')
export const getDependentMedicalRecords = (id) => request(`/api/patient/dependents/${id}/medical-records`)
export const getPatientDiagnoses = (subjectId) => request(`/api/patient/diagnoses${queryString({ subjectId })}`)
export const getPatientPrescriptions = (subjectId) => request(`/api/patient/prescriptions${queryString({ subjectId })}`)
export const getPatientMedications = (subjectId) => request(`/api/patient/medications${queryString({ subjectId })}`)
export const getPatientLabResults = (subjectId) => request(`/api/patient/lab-results${queryString({ subjectId })}`)
export const getPatientCertificates = (subjectId) => request(`/api/patient/certificates${queryString({ subjectId })}`)
export const getPatientReferrals = (subjectId) => request(`/api/patient/referrals${queryString({ subjectId })}`)
export const getPatientDocuments = (subjectId) => request(`/api/patient/documents${queryString({ subjectId })}`)
export const getPatientDocument = (id, subjectId) => request(`/api/patient/documents/${id}${queryString({ subjectId })}`)
export const getPatientVisitDocuments = (patientId, visitId) => request(`/api/patients/${patientId}/visits/${visitId}/documents`)
export const getPatientMedicalHistory = (subjectId) => request(`/api/patient/medical-history${queryString({ subjectId })}`)
export const getPatientSubjectVisits = (patientId) => request(`/api/patients/${patientId}/visits`)
export const getPatientSubjectDiagnoses = (patientId) => request(`/api/patients/${patientId}/diagnoses`)
export const getPatientSubjectPrescriptions = (patientId) => request(`/api/patients/${patientId}/prescriptions`)
export const getPatientSubjectMedications = (patientId) => request(`/api/patients/${patientId}/medications`)
export const getPatientSubjectLabResults = (patientId) => request(`/api/patients/${patientId}/lab-results`)
export const getPatientSubjectReferrals = (patientId) => request(`/api/patients/${patientId}/referrals`)
export const getPatientSubjectCertificates = (patientId) => request(`/api/patients/${patientId}/certificates`)
export const getPatientSubjectDocuments = (patientId) => request(`/api/patients/${patientId}/documents`)
export const getDoctorAppointments = (params = {}) => request(`/api/doctor/appointments${queryString(params)}`)
export const getNurseAppointments = (params = {}) => request(`/api/nurse/appointments${queryString(params)}`)
export const getDoctorProfile = () => request('/api/doctor/profile')
export const updateDoctorProfile = (body) => request('/api/doctor/profile', { method: 'PUT', body: JSON.stringify(body) })
export const getNurseProfile = () => request('/api/nurse/profile')
export const updateNurseProfile = (body) => request('/api/nurse/profile', { method: 'PUT', body: JSON.stringify(body) })
export const getDoctorCalendar = (params = {}) => request(`/api/doctor/calendar${queryString(params)}`)
export const getNurseCalendar = (params = {}) => request(`/api/nurse/calendar${queryString(params)}`)
export const getDoctorAppointmentDetails = (id) => request(`/api/doctor/appointments/${id}/details`)
export const getDoctorPatientDirectory = (params = {}) => request(`/api/doctor/patients${queryString(params)}`)
export const getDoctorPatientDetail = (id, type) => request(`/api/doctor/patients/${id}${queryString({ type })}`)
export const getNurseAppointmentDetails = (id) => request(`/api/nurse/appointments/${id}/details`)
export const startDoctorAppointment = (id) => request(`/api/doctor/appointments/${id}/start`, { method: 'POST' })
export const updateDoctorVisit = (id, body) => request(`/api/doctor/appointments/${id}/visit`, { method: 'PUT', body: JSON.stringify(body) })
export const getIcd10Codes = (query) => request(`/api/icd10${queryString({ query })}`)
export const getDoctorDocumentTemplates = (params = {}) => request(`/api/doctor/document-templates${queryString(params)}`)
export const createDoctorDocumentTemplate = (body) => request('/api/doctor/document-templates', { method: 'POST', body: JSON.stringify(body) })
export const addDoctorTemplateVersion = (id, body) => request(`/api/doctor/document-templates/${id}/versions`, { method: 'POST', body: JSON.stringify(body) })
export const getDoctorTemplateVersions = (id) => request(`/api/doctor/document-templates/${id}/versions`)
export const favoriteDoctorTemplate = (id) => request(`/api/doctor/document-templates/${id}/favorite`, { method: 'POST' })
export const unfavoriteDoctorTemplate = (id) => request(`/api/doctor/document-templates/${id}/favorite`, { method: 'DELETE' })
export const setDoctorDefaultTemplate = (type, templateId) => request(`/api/doctor/document-templates/defaults/${type}${queryString({ templateId })}`, { method: 'PUT' })
export const createDoctorDocumentDraft = (appointmentId, body) => request(`/api/doctor/appointments/${appointmentId}/document-drafts`, { method: 'POST', body: JSON.stringify(body) })
export const getDoctorDocument = (id) => request(`/api/doctor/documents/${id}`)
export const saveDoctorDocumentDraft = (id, body) => request(`/api/doctor/documents/${id}`, { method: 'PUT', body: JSON.stringify(body) })
export const approveDoctorDocument = (id) => request(`/api/doctor/documents/${id}/approve`, { method: 'POST' })
export const downloadDoctorDocumentPdf = (id) => requestBlob(`/api/doctor/documents/${id}/pdf`)
export const downloadPatientDocumentPdf = (id, subjectId) => requestBlob(`/api/patient/documents/${id}/pdf${queryString({ subjectId })}`)
export const addDoctorDiagnosis = (id, body) => request(`/api/doctor/appointments/${id}/diagnoses`, { method: 'POST', body: JSON.stringify(body) })
export const addDoctorLabResult = (id, body) => request(`/api/doctor/appointments/${id}/lab-results`, { method: 'POST', body: JSON.stringify(body) })
export const addDoctorCertificate = (id, body) => request(`/api/doctor/appointments/${id}/certificates`, { method: 'POST', body: JSON.stringify(body) })
export const addDoctorPrescription = (id, body) => request(`/api/doctor/appointments/${id}/prescriptions`, { method: 'POST', body: JSON.stringify(body) })
export const addDoctorMedication = (id, body) => request(`/api/doctor/appointments/${id}/medications`, { method: 'POST', body: JSON.stringify(body) })
export const addDoctorReferral = (id, body) => request(`/api/doctor/appointments/${id}/referrals`, { method: 'POST', body: JSON.stringify(body) })
export const getDoctorReferrals = (params = {}) => request(`/api/doctor/referrals${queryString(params)}`)
export const downloadDoctorReferralPdf = (id) => requestBlob(`/api/doctor/referrals/${id}/pdf`)
export const addDoctorDocument = (id, body) => request(`/api/doctor/appointments/${id}/documents`, { method: 'POST', body: JSON.stringify(body) })
export const completeDoctorAppointment = (id) => request(`/api/doctor/appointments/${id}/complete`, { method: 'POST' })
export const createDoctorMedicalRecord = (id, body) => request(`/api/doctor/appointments/${id}/medical-record`, { method: 'POST', body: JSON.stringify(body) })
export const updateDoctorAppointmentStatus = (id, body) => request(`/api/doctor/appointments/${id}/status`, { method: 'PATCH', body: JSON.stringify(body) })
export const createNurseIntake = (id, body) => request(`/api/nurse/appointments/${id}/intake`, { method: 'POST', body: JSON.stringify(body) })
export const getNurseAppointmentStart = (id) => request(`/api/nurse/appointments/${id}/start`, { method: 'POST' })
export const startNurseAssessment = (id) => request(`/api/nurse/appointments/${id}/start`, { method: 'POST' })
export const updateNurseAssessment = (id, body) => request(`/api/nurse/appointments/${id}/assessment`, { method: 'PUT', body: JSON.stringify(body) })
export const readyNurseAppointment = (id) => request(`/api/nurse/appointments/${id}/ready-for-doctor`, { method: 'POST' })
export const updateNurseAppointmentStatus = (id, body) => request(`/api/nurse/appointments/${id}/status`, { method: 'PATCH', body: JSON.stringify(body) })

export const getPatientSubjects = () => request('/api/patient-profiles')
export const getPatientSubjectAppointments = (patientId) => request(`/api/patients/${patientId}/appointments`)
export const getPatientSubjectHistory = (patientId) => request(`/api/patients/${patientId}/medical-history`)
export const getAppointmentResult = (appointmentId) => request(`/api/appointments/${appointmentId}/result`)

export const getAiConversations = () => request('/api/ai/conversations')
export const createAiConversation = () => request('/api/ai/conversations', { method: 'POST' })
export const getAiMessages = (conversationId) => request(`/api/ai/conversations/${conversationId}/messages`)
export const deleteAiConversation = (conversationId) => request(`/api/ai/conversations/${conversationId}`, { method: 'DELETE' })
export const getAiAvailableSlots = (doctorId, date, timeFrom, timeTo) => request(`/api/ai/doctors/${doctorId}/slots${queryString({ date, timeFrom, timeTo })}`)
export const prepareAiAppointment = (body) => request('/api/ai/appointments/proposals', { method: 'POST', body: JSON.stringify(body) })
export const confirmAiAppointment = (proposalId) => request(`/api/ai/appointments/proposals/${proposalId}/confirm`, { method: 'POST' })
export const cancelAiAppointment = (proposalId) => request(`/api/ai/appointments/proposals/${proposalId}/cancel`, { method: 'POST' })
export const confirmAiWaitlist = (proposalId) => request(`/api/ai/waitlist/proposals/${proposalId}/confirm`, { method: 'POST' })
export const cancelAiWaitlist = (proposalId) => request(`/api/ai/waitlist/proposals/${proposalId}/cancel`, { method: 'POST' })

export async function streamAiChat(body, { signal, onEvent, token: providedToken } = {}) {
  // Prefer the token from the already authenticated React session. The
  // storage fallback keeps this API usable from non-React callers and older
  // integrations while ensuring streaming uses the same bearer value as the
  // rest of the authenticated UI.
  const token = providedToken || getStoredToken()
  const headers = withBearerToken({
    Accept: 'text/event-stream',
    'Content-Type': 'application/json',
  }, token)

  let response
  try {
    response = await fetch(buildApiUrl('/api/ai/chat/stream'), {
      method: 'POST',
      headers,
      credentials: 'same-origin',
      body: JSON.stringify(body),
      signal,
    })
  } catch (error) {
    if (error?.name === 'AbortError') throw error
    throw new ApiError(0, 'Network request failed', {}, 'network')
  }

  if (!response.ok) {
    const text = await response.text()
    let payload = {}
    try { payload = text ? JSON.parse(text) : {} } catch { payload = {} }
    if (response.status === 401) await handleUnauthorized('/api/ai/chat/stream', token)
    throw new ApiError(response.status, payload.message || `Request failed with status ${response.status}`, payload.errors || {})
  }

  if (!response.body) throw new ApiError(0, 'Streaming is not supported', {}, 'network')
  const reader = response.body.getReader()
  const decoder = new TextDecoder()
  let buffer = ''

  const dispatch = (eventText) => {
    const data = eventText.split('\n')
      .filter((line) => line.startsWith('data:'))
      .map((line) => line.slice(5).trimStart())
      .join('\n')
    if (!data) return
    try {
      const payload = JSON.parse(data)
      onEvent?.(payload.type, payload)
    } catch {
      // Ignore malformed transport fragments; the backend emits complete JSON events.
    }
  }

  while (true) {
    const { value, done } = await reader.read()
    buffer += decoder.decode(value || new Uint8Array(), { stream: !done })
    const events = buffer.split('\n\n')
    buffer = events.pop() || ''
    events.forEach(dispatch)
    if (done) break
  }
  if (buffer.trim()) dispatch(buffer)
}

// These endpoints are the development-only account picker. They deliberately
// work before normal authentication and must not turn an expected anonymous
// request (or an expired token left in another tab) into a global logout.
const devBootstrapOptions = { auth: false, handleUnauthorized: false }

export const devSearchUsers = (params, signal) => request(`/api/dev/users/search${queryString(params)}`, { ...devBootstrapOptions, signal })
export const devGetRelation = (userId) => request(`/api/dev/users/${userId}/relation`)
export const devLogin = (userId) => request(`/api/dev/login/${userId}`, {
  method: 'POST',
  ...devBootstrapOptions,
})
export const devGetRegions = () => request('/api/dev/filters/regions', devBootstrapOptions)
export const devGetCities = (regionId) => request(`/api/dev/filters/regions/${regionId}/cities`, devBootstrapOptions)
export const devGetClinics = (cityId) => request(`/api/dev/filters/cities/${cityId}/clinics`, devBootstrapOptions)
export const devGetSpecialties = (clinicId) => request(`/api/dev/filters/clinics/${clinicId}/specialties`, devBootstrapOptions)
export const devGetAllSpecialties = () => request('/api/dev/filters/specialties', devBootstrapOptions)
export const devSearchAppointments = (params, signal) => request(`/api/dev/appointments${queryString(params)}`, { signal })
export const devSearchWorkflowAppointments = (params, signal) => request(`/api/dev/notification-workflow/appointments${queryString(params)}`, { signal })
export const devGetWorkflowAppointment = (appointmentId) => request(`/api/dev/notification-workflow/appointments/${appointmentId}`)
export const devRunWorkflowAction = (appointmentId, action) => request(`/api/dev/notification-workflow/appointments/${appointmentId}/${action}`, { method: 'POST' })
export const devGetIntegrityAudit = () => request('/api/dev/audit')

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
export const getAdminDoctorSchedule = (id) => request(`/api/admin/doctors/${id}/schedule`)
export const updateAdminDoctorSchedule = (id, body) => adminMutation(`/api/admin/doctors/${id}/schedule`, 'PUT', body)
export const adminGetDocumentTemplates = () => request('/api/admin/document-templates')
export const adminCreateDocumentTemplate = (body) => adminMutation('/api/admin/document-templates', 'POST', body)
export const adminGetTemplateVersions = (id) => request(`/api/admin/document-templates/${id}/versions`)
export const adminAddTemplateVersion = (id, body) => adminMutation(`/api/admin/document-templates/${id}/versions`, 'POST', body)
export const adminSetTemplateActive = (id, active) => request(`/api/admin/document-templates/${id}/active?value=${active}`, { method: 'PATCH' })
export const adminImportIcd10 = (file, source, sourceVersion) => {
  const body = new FormData(); body.append('file', file); body.append('source', source); body.append('sourceVersion', sourceVersion)
  return request('/api/admin/icd10/import', { method: 'POST', body })
}
