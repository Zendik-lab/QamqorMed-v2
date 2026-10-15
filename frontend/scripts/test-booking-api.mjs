import assert from 'node:assert/strict'
import { readFile } from 'node:fs/promises'
import test from 'node:test'

const source = await readFile(new URL('../src/api/apiClient.js', import.meta.url), 'utf8')
const authModule = new URL('../src/api/authSession.js', import.meta.url).href

// Supply the Vite build-time setting while exercising the real request functions in Node.
async function client(apiUrl) {
  const moduleSource = source
    .replace("'./authSession.js'", JSON.stringify(authModule))
    .replaceAll('import.meta.env.VITE_API_URL', JSON.stringify(apiUrl))
  return import(`data:text/javascript;base64,${Buffer.from(moduleSource).toString('base64')}`)
}

function mockRequests(t, payload) {
  const requests = []
  t.mock.method(globalThis, 'fetch', async (url, options) => {
    requests.push({ url, options })
    return new Response(JSON.stringify(payload), { status: 200 })
  })
  const previousWindow = Object.getOwnPropertyDescriptor(globalThis, 'window')
  Object.defineProperty(globalThis, 'window', { configurable: true, value: { localStorage: { getItem: () => 'patient-jwt' } } })
  t.after(() => {
    if (previousWindow) Object.defineProperty(globalThis, 'window', previousWindow)
    else delete globalThis.window
  })
  return requests
}

test('booking catalog uses the Render API base and preserves successful empty lists', async (t) => {
  const requests = mockRequests(t, [])
  const api = await client('https://booking-api.example/api/')
  const controller = new AbortController()
  const responses = await Promise.all([
    api.getLocationRegions(),
    api.getLocationCities(21, controller.signal),
    api.getCitySpecialties(91, controller.signal),
    api.searchClinics({ cityId: 91, specialtyId: 56 }, controller.signal),
    api.getClinicSpecialties(32),
    api.getDoctorTimeSlots(1643, { from: '2030-01-01', to: '2030-01-14' }, controller.signal),
  ])
  assert.deepEqual(responses, [[], [], [], [], [], []])
  assert.deepEqual(requests.map(({ url }) => url), [
    'https://booking-api.example/api/locations/regions',
    'https://booking-api.example/api/locations/regions/21/cities',
    'https://booking-api.example/api/specialties/by-city/91',
    'https://booking-api.example/api/clinics/search?cityId=91&specialtyId=56',
    'https://booking-api.example/api/clinics/32/specialties',
    'https://booking-api.example/api/doctors/1643/time-slots?from=2030-01-01&to=2030-01-14',
  ])
  for (const { options } of requests) assert.equal(options.headers.Authorization, 'Bearer patient-jwt')
  for (const index of [1, 2, 3, 5]) assert.equal(requests[index].options.signal, controller.signal)
})

test('doctor search sends city/specialty filters and accepts an empty page', async (t) => {
  const requests = mockRequests(t, { content: [], totalElements: 0 })
  const api = await client('')
  const result = await api.searchDoctors({ cityId: 91, specialtyId: 56, clinicId: null, query: '', page: 0, size: 20 })
  assert.deepEqual(result.content, [])
  assert.equal(requests[0].url, '/api/doctors/search?cityId=91&specialtyId=56&page=0&size=20')
})

test('booking sends the selected slot and catalog filters to the patient endpoint', async (t) => {
  const requests = mockRequests(t, { id: 1 })
  const api = await client('')
  const booking = { doctorId: 1643, cityId: 91, specialtyId: 56, clinicId: 32, timeSlotId: 7, appointmentAt: '2030-01-01T09:00:00' }
  await api.createPatientAppointment(booking)
  assert.equal(requests[0].url, '/api/patient/appointments')
  assert.equal(requests[0].options.method, 'POST')
  assert.deepEqual(JSON.parse(requests[0].options.body), booking)
})

test('catalog errors and cancelled requests stay distinguishable from empty lists', async (t) => {
  mockRequests(t, [])
  const api = await client('')
  t.mock.method(globalThis, 'fetch', async () => new Response('{"message":"Unavailable"}', { status: 503 }))
  await assert.rejects(api.getCitySpecialties(91), (error) => error.status === 503)
  const abort = new DOMException('Cancelled', 'AbortError')
  t.mock.method(globalThis, 'fetch', async () => { throw abort })
  await assert.rejects(api.getCitySpecialties(91), (error) => error === abort)
})
