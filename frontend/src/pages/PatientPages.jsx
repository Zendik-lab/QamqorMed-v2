import { EmptyState as EmptyMedicalState } from '../components/ui/Feedback.jsx'
import { Input as Field } from '../components/ui/Fields.jsx'
import { useEffect, useMemo, useRef, useState } from 'react'
import { useMotion } from '../motion/Motion.jsx'
import {
  cancelPatientAppointment,
  confirmPatientAppointment,
  declinePatientAppointment,
  createDependent,
  createPatientAppointment,
  deleteDependent,
  getDependent,
  getDependentMedicalRecords,
  getDependents,
  getCitySpecialties,
  getDoctorTimeSlots,
  getLocationCities,
  getLocationRegions,
  getPatientLocation,
  getPatientAppointments,
  getPatientProfile,
  getAttendance,
  getPatientMedicalRecords,
  getPatientDiagnoses,
  getPatientPrescriptions,
  getPatientMedications,
  getPatientLabResults,
  getPatientCertificates,
  getPatientDocument,
  getPatientDocuments,
  downloadPatientDocumentPdf,
  getPatientMedicalHistory,
  getPatientReferrals,
  getPatientSubjectHistory,
  searchClinics,
  searchDoctors,
  updateDependent,
  updatePatientLocation,
  updatePatientProfile,
} from '../api/apiClient.js'
import { navigate, useLocation } from '../utils/navigation.js'
import { useAuth } from '../auth/AuthContext.jsx'
import { localeFor, translateError, useTranslation } from '../i18n/index.jsx'

const relationshipOptions = ['CHILD', 'SPOUSE', 'PARENT', 'SIBLING', 'GUARDIAN', 'OTHER']

function messageFor(error, t, fallbackKey = 'common.error') {
  return translateError(error, t, fallbackKey)
}

function fullName(person) {
  return [person?.lastName, person?.firstName, person?.middleName].filter(Boolean).join(' ')
}

function PageMessage({ error }) {
  return error ? <p className="error-message" role="alert">{error}</p> : null
}

function dateKeyFromDate(date = new Date()) {
  return [date.getFullYear(), String(date.getMonth() + 1).padStart(2, '0'), String(date.getDate()).padStart(2, '0')].join('-')
}

function currentClinicDateTimeKey() {
  const parts = new Intl.DateTimeFormat('en-CA', { timeZone: 'Asia/Qyzylorda', year: 'numeric', month: '2-digit', day: '2-digit', hour: '2-digit', minute: '2-digit', second: '2-digit', hourCycle: 'h23' })
    .formatToParts(new Date())
    .reduce((result, item) => ({ ...result, [item.type]: item.value }), {})
  return `${parts.year}-${parts.month}-${parts.day}T${parts.hour}:${parts.minute}:${parts.second}`
}

function currentClinicDateKey() {
  return currentClinicDateTimeKey().slice(0, 10)
}

function dateFromKey(dateKey) {
  const [year, month, day] = dateKey.split('-').map(Number)
  return new Date(year, month - 1, day, 12)
}

function monthStart(dateKey) {
  const date = dateKey ? dateFromKey(dateKey) : new Date()
  return new Date(date.getFullYear(), date.getMonth(), 1, 12)
}

function formatDateKey(dateKey, language = 'ru', options = {}) {
  if (!dateKey) return ''
  return new Intl.DateTimeFormat(localeFor(language), { timeZone: 'Asia/Qyzylorda', ...options }).format(dateFromKey(dateKey))
}

function formatDateLong(dateKey, language) {
  return formatDateKey(dateKey, language, { weekday: 'long', day: 'numeric', month: 'long' })
}

function formatDateWithYear(dateKey, language) {
  return formatDateKey(dateKey, language, { day: 'numeric', month: 'long', year: 'numeric' })
}

function formatDateShort(dateKey, language) {
  return formatDateKey(dateKey, language, { day: 'numeric', month: 'short' }).replace(/\.$/, '')
}

function formatWeekday(dateKey, language) {
  return formatDateKey(dateKey, language, { weekday: 'short' }).replace(/\.$/, '')
}

function formatSlotTime(value) {
  return String(value || '').slice(11, 16)
}

function slotDateKey(slot) {
  return String(slot?.startsAt || '').slice(0, 10)
}

function slotIsInFuture(slot, nowKey = currentClinicDateTimeKey()) {
  const dateKey = slotDateKey(slot)
  if (!dateKey) return false
  const startsAt = String(slot.startsAt).length >= 19 ? String(slot.startsAt).slice(0, 19) : `${String(slot.startsAt).slice(0, 16)}:00`
  return startsAt >= nowKey
}

function groupSlotsByDate(items) {
  const nowKey = currentClinicDateTimeKey()
  return items.filter((slot) => slotIsInFuture(slot, nowKey)).reduce((groups, slot) => {
    const key = slotDateKey(slot)
    if (!key) return groups
    groups[key] = [...(groups[key] || []), slot]
    return groups
  }, {})
}

function timeGroupLabel(time, t) {
  const hour = Number(String(time).slice(0, 2))
  if (hour < 12) return t('common.morning')
  if (hour < 18) return t('common.afternoon')
  return t('common.evening')
}

function calendarCells(month) {
  const firstDay = new Date(month.getFullYear(), month.getMonth(), 1, 12)
  const offset = (firstDay.getDay() + 6) % 7
  const daysInMonth = new Date(month.getFullYear(), month.getMonth() + 1, 0, 12).getDate()
  return [...Array(offset).fill(null), ...Array.from({ length: daysInMonth }, (_, index) => new Date(month.getFullYear(), month.getMonth(), index + 1, 12))]
}

function capitalized(value) {
  return value ? `${value[0].toUpperCase()}${value.slice(1)}` : value
}

function genderLabel(value, t) {
  return { MALE: t('common.genderMale'), FEMALE: t('common.genderFemale'), UNKNOWN: t('common.notSpecified') }[value] || t('common.notSpecified')
}

function formatPersonDate(value, language, t) {
  if (!value) return t('patient.dateNotSpecified')
  const [year, month, day] = String(value).slice(0, 10).split('-').map(Number)
  return Number.isFinite(year) ? new Intl.DateTimeFormat(localeFor(language), { day: 'numeric', month: 'long', year: 'numeric' }).format(new Date(year, month - 1, day, 12)) : t('patient.dateNotSpecified')
}

function formatPhone(value, t) {
  const digits = String(value || '').replace(/\D/g, '')
  if (digits.length === 11 && digits.startsWith('7')) return `+7 (${digits.slice(1, 4)}) ${digits.slice(4, 7)}-${digits.slice(7, 9)}-${digits.slice(9)}`
  return value || t('patient.notProvided')
}

function appointmentDateTime(value, language, t) {
  const dateKey = String(value || '').slice(0, 10)
  return dateKey ? `${formatDateWithYear(dateKey, language)} · ${formatSlotTime(value)}` : t('patient.dateNotSpecified')
}

export function PatientLocationPage() {
  const { refreshUser } = useAuth()
  const { t } = useTranslation()
  const [regions, setRegions] = useState([])
  const [cities, setCities] = useState([])
  const [regionId, setRegionId] = useState('')
  const [cityId, setCityId] = useState('')
  const [error, setError] = useState('')
  const [loading, setLoading] = useState(true)
  const [saving, setSaving] = useState(false)

  useEffect(() => {
    Promise.all([getLocationRegions(), getPatientLocation()])
      .then(([regionData, location]) => {
        setRegions(regionData)
        setRegionId(location.regionId ? String(location.regionId) : '')
        setCityId(location.cityId ? String(location.cityId) : '')
      })
      .catch((requestError) => setError(messageFor(requestError, t)))
      .finally(() => setLoading(false))
  }, [])

  useEffect(() => {
    if (!regionId) { setCities([]); setCityId(''); return undefined }
    getLocationCities(regionId)
      .then((items) => setCities(items))
      .catch((requestError) => setError(messageFor(requestError, t)))
    return undefined
  }, [regionId])

  const submit = async (event) => {
    event.preventDefault(); setSaving(true); setError('')
    try { await updatePatientLocation({ regionId: Number(regionId), cityId: Number(cityId) }); await refreshUser(); navigate('/patient', { replace: true }) }
    catch (requestError) { setError(messageFor(requestError, t)) }
    finally { setSaving(false) }
  }

  return <section className="location-page"><div className="location-card"><div className="eyebrow">{t('patient.locationEyebrow')}</div><h1>{t('patient.chooseRegion')}</h1><p>{t('patient.locationDescription')}</p><PageMessage error={error} />{loading ? <p>{t('patient.loadingRegions')}</p> : <form onSubmit={submit}><label className="field-group"><span className="field-label">{t('patient.region')}</span><select value={regionId} onChange={(event) => { setRegionId(event.target.value); setCityId('') }} required><option value="">{t('patient.chooseRegionOption')}</option>{regions.map((item) => <option value={item.id} key={item.id}>{item.name}</option>)}</select></label><label className="field-group"><span className="field-label">{t('patient.city')}</span><select value={cityId} onChange={(event) => setCityId(event.target.value)} required disabled={!regionId}><option value="">{t('patient.chooseCityOption')}</option>{cities.map((item) => <option value={item.id} key={item.id}>{item.name}</option>)}</select></label><button className="button" disabled={saving || !cityId}>{saving ? t('patient.saving') : t('patient.saveCity')}</button></form>}</div></section>
}

export function PatientDependentsPage() {
  const { t } = useTranslation()
  const [dependents, setDependents] = useState([])
  const [form, setForm] = useState(emptyDependent())
  const [editingId, setEditingId] = useState(null)
  const [openForm, setOpenForm] = useState(false)
  const [error, setError] = useState('')
  const [saving, setSaving] = useState(false)

  const load = () => getDependents().then(setDependents).catch((e) => setError(messageFor(e, t)))
  useEffect(() => { load() }, [])
  const update = (name) => (event) => setForm((current) => ({ ...current, [name]: event.target.value }))
  const startEdit = (item) => { setEditingId(item.id); setForm({ firstName: item.firstName, lastName: item.lastName, middleName: item.middleName || '', dateOfBirth: item.dateOfBirth, gender: item.gender || '', iin: item.iin || '', relationship: item.relationship, phone: item.phone || '' }); setOpenForm(true) }
  const startCreate = () => { setEditingId(null); setForm(emptyDependent()); setOpenForm(true) }
  const submit = async (event) => {
    event.preventDefault(); setSaving(true); setError('')
    try { if (editingId) await updateDependent(editingId, form); else await createDependent(form); await load(); setOpenForm(false) } catch (e) { setError(messageFor(e, t)) } finally { setSaving(false) }
  }
  const remove = async (id) => { if (!window.confirm(t('patient.hideConfirm'))) return; try { await deleteDependent(id); await load() } catch (e) { setError(messageFor(e, t)) } }

  return <section className="page-shell">
    <div className="page-heading page-heading--row"><div><div className="eyebrow">{t('patient.familyProfiles')}</div><h1>{t('patient.dependentsTitle')}</h1><p className="form-description">{t('patient.dependentsDescription')}</p></div><button className="button" onClick={startCreate}>{t('patient.addDependent')}</button></div>
    <PageMessage error={error} />
    {openForm && <form className="form-card dependent-form" onSubmit={submit}><h2>{editingId ? t('patient.editProfile') : t('patient.addDependentTitle')}</h2><div className="form-grid"><Field label={t('patient.firstName')} value={form.firstName} onChange={update('firstName')} required /><Field label={t('patient.lastName')} value={form.lastName} onChange={update('lastName')} required /><Field label={t('patient.middleName')} value={form.middleName} onChange={update('middleName')} /><Field label={t('patient.birthDate')} type="date" value={form.dateOfBirth} onChange={update('dateOfBirth')} required /><Field label={t('patient.gender')} value={form.gender} onChange={update('gender')} /><Field label={t('patient.iin')} value={form.iin} onChange={update('iin')} inputMode="numeric" maxLength={12} /><label className="field-group"><span className="field-label">{t('patient.relationship')}</span><select value={form.relationship} onChange={update('relationship')} required><option value="">{t('patient.selectRelationship')}</option>{relationshipOptions.map((value) => <option value={value} key={value}>{t(`relationship.${value}`)}</option>)}</select></label><Field label={t('patient.phone')} value={form.phone} onChange={update('phone')} /></div><div className="form-actions"><button className="button" disabled={saving}>{saving ? t('patient.saving') : t('common.save')}</button><button className="button button--secondary" type="button" onClick={() => setOpenForm(false)}>{t('common.cancel')}</button></div></form>}
    {dependents.length === 0 && !openForm && <div className="empty-state"><h2>{t('patient.noDependents')}</h2><p>{t('patient.noDependentsDescription')}</p><button className="button button--secondary" onClick={startCreate}>{t('patient.addFirstProfile')}</button></div>}
    <div className="profile-card-grid">{dependents.map((item) => <article className="profile-card" key={item.id}><div className="profile-card__top"><div className="profile-avatar">{(item.firstName?.[0] || '?').toUpperCase()}</div><div><span className="eyebrow">{item.relationshipLabel}</span><h2>{item.fullName}</h2><p>{item.age == null ? t('patient.ageMissing') : `${item.age} ${t('staff.years')}`} · {item.dateOfBirth}</p></div></div><div className="profile-card__actions"><button className="button button--compact" onClick={() => navigate(`/patient/dependents/${item.id}`)}>{t('patient.openProfile')}</button><button className="button button--secondary button--compact" onClick={() => navigate(`/patient/appointments?dependentId=${item.id}`)}>{t('patient.bookForDoctor')}</button><button className="text-button" onClick={() => startEdit(item)}>{t('common.edit')}</button><button className="text-button text-button--danger" onClick={() => remove(item.id)}>{t('patient.hide')}</button></div></article>)}</div>
  </section>
}

export function DependentDetailPage({ id }) {
  const { t, language } = useTranslation()
  const [dependent, setDependent] = useState(null)
  const [records, setRecords] = useState([])
  const [appointments, setAppointments] = useState([])
  const [error, setError] = useState('')
  useEffect(() => { Promise.all([getDependent(id), getDependentMedicalRecords(id), getPatientAppointments()]).then(([profile, history, allAppointments]) => { setDependent(profile); setRecords(history); setAppointments(allAppointments.filter((item) => item.subject?.type === 'DEPENDENT' && String(item.subject.id) === String(id))) }).catch((e) => setError(messageFor(e, t))) }, [id, t])
  if (!dependent) return <section className="page-shell"><PageMessage error={error} /><p>{t('patient.loadingProfile')}</p></section>
  return <section className="page-shell"><button className="back-link" onClick={() => navigate('/patient/dependents')}>{t('patient.backToDependents')}</button><div className="profile-hero"><div className="profile-avatar profile-avatar--large">{dependent.firstName?.[0]}</div><div><div className="eyebrow">{t('patient.dependentProfile')} · {dependent.relationshipLabel}</div><h1>{dependent.fullName}</h1><p>{dependent.age ?? '—'} {t('staff.years')} · {t('patient.birthDate').toLowerCase()} {formatPersonDate(dependent.dateOfBirth, language, t)}</p></div><button className="button" onClick={() => navigate(`/patient/appointments?dependentId=${id}`)}>{t('patient.bookForDoctor')}</button></div><div className="dependent-viewing-banner"><span>◉</span><p>{t('patient.viewingData')} <strong>{dependent.fullName}</strong></p></div><PageMessage error={error} /><div className="detail-grid"><article className="info-panel"><h2>{t('patient.profileData')}</h2><p><span>{t('patient.gender')}</span><strong>{genderLabel(dependent.gender, t)}</strong></p><p><span>{t('patient.iin')}</span><strong>{dependent.iin || t('patient.notProvided')}</strong></p><p><span>{t('patient.phone')}</span><strong>{formatPhone(dependent.phone, t)}</strong></p></article><article className="info-panel"><h2>{t('patient.upcomingAppointments')}</h2>{appointments.length ? appointments.slice(0, 3).map((item) => <AppointmentRow item={item} key={item.id} t={t} language={language} />) : <p className="muted-text">{t('patient.noAppointments')}</p>}</article></div><section className="records-section"><div className="section-heading"><div><div className="eyebrow">{t('patient.onlyThisProfile')}</div><h2>{t('patient.medicalHistory')} — {dependent.firstName}</h2></div></div>{records.length ? records.map((item) => <RecordCard record={item} key={item.id} t={t} language={language} />) : <div className="empty-state"><h3>{t('patient.historyEmpty')}</h3><p>{t('patient.completedVisitsEmpty')}</p></div>}</section></section>
}

export function PatientBookingPage() {
  const location = useLocation()
  const { t, language } = useTranslation()
  const selectedDependentId = new URLSearchParams(location.search).get('dependentId') || ''
  const [dependents, setDependents] = useState([])
  const [profile, setProfile] = useState(null)
  const [regions, setRegions] = useState([])
  const [cities, setCities] = useState([])
  const [specialties, setSpecialties] = useState([])
  const [clinics, setClinics] = useState([])
  const [doctors, setDoctors] = useState([])
  const [slots, setSlots] = useState([])
  const [regionId, setRegionId] = useState('')
  const [cityId, setCityId] = useState('')
  const [specialtyId, setSpecialtyId] = useState('')
  const [specialtyQuery, setSpecialtyQuery] = useState('')
  const [clinicId, setClinicId] = useState('')
  const [doctorId, setDoctorId] = useState('')
  const [doctorQuery, setDoctorQuery] = useState('')
  const [selectedDate, setSelectedDate] = useState('')
  const [selectedTime, setSelectedTime] = useState('')
  const [selectedSlot, setSelectedSlot] = useState(null)
  const [calendarOpen, setCalendarOpen] = useState(false)
  const [calendarMonth, setCalendarMonth] = useState(monthStart())
  const calendarMotionRef = useRef(null)
  const timeMotionRef = useRef(null)
  useMotion(calendarMotionRef, calendarOpen + '|' + calendarMonth.getTime())
  useMotion(timeMotionRef, selectedDate, 'results')
  const [showAllDates, setShowAllDates] = useState(false)
  const [scheduleError, setScheduleError] = useState('')
  const [slotsRequestKey, setSlotsRequestKey] = useState(0)
  const [form, setForm] = useState({ dependentId: selectedDependentId, note: '' })
  const [error, setError] = useState('')
  const [loading, setLoading] = useState(true)
  const [specialtiesLoading, setSpecialtiesLoading] = useState(false)
  const [clinicsLoading, setClinicsLoading] = useState(false)
  const [doctorsLoading, setDoctorsLoading] = useState(false)
  const [slotsLoading, setSlotsLoading] = useState(false)
  const [saving, setSaving] = useState(false)
  const [success, setSuccess] = useState(null)

  useEffect(() => {
    Promise.all([getPatientProfile(), getDependents(), getLocationRegions()])
      .then(([patient, family, regionData]) => {
        setProfile(patient); setDependents(family); setRegions(regionData)
        if (patient.preferredRegion?.id) {
          const nextRegionId = String(patient.preferredRegion.id)
          setRegionId(nextRegionId)
          getLocationCities(nextRegionId).then((items) => setCities(items))
        }
        if (patient.preferredCity?.id) setCityId(String(patient.preferredCity.id))
      })
      .catch((requestError) => setError(messageFor(requestError, t)))
      .finally(() => setLoading(false))
  }, [t])

  useEffect(() => { setForm((current) => ({ ...current, dependentId: selectedDependentId })) }, [selectedDependentId])

  useEffect(() => {
    if (!cityId) { setSpecialties([]); return undefined }
    setSpecialtiesLoading(true); setError('')
    getCitySpecialties(cityId).then(setSpecialties).catch((requestError) => setError(messageFor(requestError, t))).finally(() => setSpecialtiesLoading(false))
    return undefined
  }, [cityId, t])

  useEffect(() => {
    if (!cityId || !specialtyId) { setClinics([]); setDoctors([]); return undefined }
    setClinicsLoading(true); setDoctorsLoading(true); setError('')
    const controller = new AbortController()
    Promise.all([searchClinics({ cityId, specialtyId }), searchDoctors({ cityId, specialtyId, clinicId: clinicId || null, query: doctorQuery, page: 0, size: 20 }, controller.signal)])
      .then(([clinicData, doctorPage]) => { setClinics(clinicData); setDoctors(doctorPage.content || []) })
      .catch((requestError) => { if (requestError.name !== 'AbortError') setError(messageFor(requestError, t)) })
      .finally(() => { setClinicsLoading(false); setDoctorsLoading(false) })
    return () => controller.abort()
  }, [cityId, specialtyId, clinicId, doctorQuery, t])

  useEffect(() => {
    if (!doctorId) { setSlots([]); return undefined }
    const controller = new AbortController()
    setSlotsLoading(true); setScheduleError(''); setSelectedDate(''); setSelectedTime(''); setSelectedSlot(null); setCalendarOpen(false); setShowAllDates(false)
    getDoctorTimeSlots(doctorId, {}, controller.signal)
      .then((items) => {
        setSlots(items)
        const firstDate = Object.keys(groupSlotsByDate(items)).sort()[0]
        if (firstDate) setCalendarMonth(monthStart(firstDate))
      })
      .catch((requestError) => { if (requestError.name !== 'AbortError') setScheduleError(messageFor(requestError, t, 'patient.scheduleUnavailable')) })
      .finally(() => setSlotsLoading(false))
    return () => controller.abort()
  }, [doctorId, slotsRequestKey, t])

  const selectedSubject = form.dependentId ? dependents.find((item) => String(item.id) === String(form.dependentId)) : profile
  const filteredSpecialties = specialties.filter((item) => !specialtyQuery || item.name.toLowerCase().includes(specialtyQuery.toLowerCase()))
  const selectedDoctor = doctors.find((item) => String(item.id) === String(doctorId))
  const slotsByDate = useMemo(() => groupSlotsByDate(slots), [slots])
  const availableDates = useMemo(() => Object.keys(slotsByDate).sort(), [slotsByDate])
  const visibleDates = showAllDates ? availableDates : availableDates.slice(0, 6)
  const selectedDateSlots = slotsByDate[selectedDate] || []
  const groupedTimes = selectedDateSlots.reduce((groups, slot) => {
    const label = timeGroupLabel(formatSlotTime(slot.startsAt), t)
    groups[label] = [...(groups[label] || []), slot]
    return groups
  }, {})
  const calendarDateKeys = useMemo(() => new Set(availableDates), [availableDates])
  const calendarDateCells = useMemo(() => calendarCells(calendarMonth), [calendarMonth])
  const resetScheduleSelection = () => { setSelectedDate(''); setSelectedTime(''); setSelectedSlot(null); setCalendarOpen(false); setShowAllDates(false) }
  const resetFromCity = () => { setSpecialtyId(''); setSpecialtyQuery(''); setClinicId(''); setDoctorId(''); setDoctorQuery(''); resetScheduleSelection(); setClinics([]); setDoctors([]); setSlots([]) }
  const changeRegion = (event) => { const value = event.target.value; setRegionId(value); setCityId(''); resetFromCity(); if (value) getLocationCities(value).then(setCities).catch((requestError) => setError(messageFor(requestError, t))); else setCities([]) }
  const changeCity = (event) => { setCityId(event.target.value); resetFromCity() }
  const chooseSpecialty = (item) => { setSpecialtyId(String(item.id)); setSpecialtyQuery(item.name); setClinicId(''); setDoctorId(''); resetScheduleSelection() }
  const changeClinic = (event) => { setClinicId(event.target.value); setDoctorId(''); resetScheduleSelection() }
  const selectDoctor = (id) => { setDoctorId(String(id)); resetScheduleSelection() }
  const selectDate = (dateKey) => {
    if (!slotsByDate[dateKey]?.length) return
    setSelectedDate(dateKey); setSelectedTime(''); setSelectedSlot(null); setCalendarOpen(false)
  }
  const selectTime = (slot) => { setSelectedTime(formatSlotTime(slot.startsAt)); setSelectedSlot(slot) }
  const retrySlots = () => { setScheduleError(''); setSlotsRequestKey((value) => value + 1) }
  const updateNote = (event) => setForm((current) => ({ ...current, note: event.target.value }))
  const submit = async (event) => {
    event.preventDefault(); setSaving(true); setError(''); setSuccess(null)
    try {
      const result = await createPatientAppointment({ doctorId: Number(doctorId), dependentId: form.dependentId ? Number(form.dependentId) : null, appointmentAt: selectedSlot.startsAt, cityId: Number(cityId), specialtyId: Number(specialtyId), clinicId: clinicId ? Number(clinicId) : null, timeSlotId: Number(selectedSlot.id), note: form.note })
      setSuccess(result); resetScheduleSelection(); setDoctorId('')
    } catch (requestError) {
      const requestMessage = messageFor(requestError, t)
      setError(requestMessage)
      if ([400, 409].includes(requestError?.status) || requestError?.code === 'slotUnavailable') {
        resetScheduleSelection()
        setSlotsRequestKey((value) => value + 1)
      }
    } finally { setSaving(false) }
  }

  const bookingView = <section className="page-shell"><div className="page-heading"><div className="eyebrow">{t('patient.bookingTitle')}</div><h1>{t('patient.bookingTitle')}</h1><p className="form-description">{t('patient.bookingDescription')}</p></div><PageMessage error={error} />{success && <div className="success-panel success-panel--booking"><strong>✓ {t('patient.bookingSuccess')}</strong><p>{formatDateWithYear(slotDateKey({ startsAt: success.appointmentAt }), language)} · {formatSlotTime(success.appointmentAt)}</p><p>{t('workflow.patient')}: {success.subject?.fullName}</p><p>{t('workflow.doctor')}: {success.doctor?.fullName}</p><p>{t('patient.clinic')}: {success.clinic?.name}</p><div className="form-actions"><button className="button" type="button" onClick={() => navigate('/patient/my-appointments')}>{t('patient.viewMyAppointments')}</button><button className="button button--secondary" type="button" onClick={() => setSuccess(null)}>{t('common.close')}</button></div></div>}<form className="booking-form" onSubmit={submit}><section className="booking-step"><div className="step-number">1</div><div className="step-content"><h2>{t('patient.selectSubject')}</h2><div className="subject-picker"><SubjectChoice selected={!form.dependentId} onClick={() => setForm((current) => ({ ...current, dependentId: '' }))} title={t('patient.myself')} subtitle={profile ? fullName(profile) : t('patient.yourProfile')} />{dependents.map((item) => <SubjectChoice key={item.id} selected={String(form.dependentId) === String(item.id)} onClick={() => setForm((current) => ({ ...current, dependentId: String(item.id) }))} title={item.fullName} subtitle={`${item.relationshipLabel}, ${item.age ?? '—'} ${t('staff.years')}`} />)}</div><p className="selection-note">{t('patient.selectSubject')}: <strong>{selectedSubject ? fullName(selectedSubject) : t('common.select')}</strong></p></div></section><section className="booking-step"><div className="step-number">2</div><div className="step-content"><h2>{t('patient.city')}</h2><div className="form-grid"><label className="field-group"><span className="field-label">{t('patient.region')}</span><select value={regionId} onChange={changeRegion} required><option value="">{t('patient.chooseRegionOption')}</option>{regions.map((item) => <option value={item.id} key={item.id}>{item.name}</option>)}</select></label><label className="field-group"><span className="field-label">{t('patient.city')}</span><select value={cityId} onChange={changeCity} disabled={!regionId} required><option value="">{t('patient.chooseCityOption')}</option>{cities.map((item) => <option value={item.id} key={item.id}>{item.name}</option>)}</select></label></div><p className="muted-text">{t('patient.locationDescription')}</p></div></section><section className="booking-step"><div className="step-number">3</div><div className="step-content"><h2>{t('staff.specialty')}</h2>{specialtiesLoading ? <p>{t('patient.loadingMedical')}</p> : !cityId ? <p className="muted-text">{t('patient.chooseCityOption')}</p> : specialties.length === 0 ? <p className="muted-text">{t('patient.noClinics')}</p> : <><label className="field-group"><span className="field-label">{t('patient.searchSpecialty')}</span><input value={specialtyQuery} onChange={(event) => { setSpecialtyQuery(event.target.value); setSpecialtyId(''); setClinicId(''); setDoctorId(''); resetScheduleSelection() }} placeholder={t('patient.searchSpecialty')} /></label><div className="search-option-list">{filteredSpecialties.slice(0, 8).map((item) => <button type="button" className={`search-option ${String(item.id) === String(specialtyId) ? 'is-selected' : ''}`} key={item.id} onClick={() => chooseSpecialty(item)}>{item.name}</button>)}</div>{specialtyId && <p className="selection-note">{t('common.select')}: <strong>{specialties.find((item) => String(item.id) === String(specialtyId))?.name}</strong></p>}</>}</div></section>{specialtyId && <section className="booking-step"><div className="step-number">4</div><div className="step-content"><h2>{t('patient.clinic')}</h2>{clinicsLoading ? <p>{t('patient.loadingMedical')}</p> : <label className="field-group"><span className="field-label">{t('patient.clinic')}</span><select value={clinicId} onChange={changeClinic}><option value="">{t('patient.allClinics')}</option>{clinics.map((item) => <option value={item.id} key={item.id}>{item.name}</option>)}</select></label>}<label className="field-group"><span className="field-label">{t('patient.searchDoctor')}</span><input value={doctorQuery} onChange={(event) => setDoctorQuery(event.target.value)} placeholder={t('patient.searchDoctor')} /></label></div></section>}{specialtyId && <section className="booking-step"><div className="step-number">5</div><div className="step-content"><h2>{t('patient.allDoctors')}</h2>{doctorsLoading ? <p>{t('patient.loadingMedical')}</p> : doctors.length === 0 ? <div className="empty-state"><h3>{t('patient.noDoctors')}</h3><p>{t('patient.changeFilters')}</p></div> : <div className="doctor-result-grid">{doctors.map((doctor) => <article className={`doctor-result-card ${String(doctor.id) === String(doctorId) ? 'is-selected' : ''}`} key={doctor.id}><div><h3>{doctor.lastName} {doctor.firstName}</h3><p>{doctor.specialty?.name}</p><p>{doctor.clinic?.name} · {doctor.cityName}</p>{doctor.officeNumber && <small>{t('staff.cabinet')} {doctor.officeNumber}</small>}</div><button type="button" className="button button--compact" onClick={() => selectDoctor(doctor.id)}>{String(doctor.id) === String(doctorId) ? t('common.active') : t('common.select')}</button></article>)}</div>}</div></section>}{doctorId && <section className="booking-step"><div className="step-number">6</div><div className="step-content"><h2>{t('patient.availableDates')}</h2><p className="form-description">{t('patient.selectDateTime')}</p>{slotsLoading ? <div className="schedule-state"><span className="loading-dot" />{t('patient.loadingMedical')}</div> : scheduleError ? <div className="schedule-state schedule-state--error"><p>{scheduleError}</p><button className="button button--secondary button--compact" type="button" onClick={retrySlots}>{t('staff.retry')}</button></div> : availableDates.length === 0 ? <div className="schedule-state"><p>{t('patient.noSlots')}</p><p className="muted-text">{t('patient.selectDoctor')}</p></div> : <><div className="date-picker-heading"><span className="field-label">{t('patient.availableDates')}</span><button className="date-calendar-toggle" type="button" onClick={() => setCalendarOpen((value) => !value)}>📅 {t('patient.showMoreDates')}</button></div><div className="date-card-grid">{visibleDates.map((dateKey) => <button type="button" className={`date-card ${selectedDate === dateKey ? 'is-selected' : ''}`} key={dateKey} onClick={() => selectDate(dateKey)}><span className="date-card__badge">{capitalized(formatWeekday(dateKey, language))}</span><strong>{formatDateShort(dateKey, language)}</strong><small>{slotsByDate[dateKey].length}</small></button>)}{availableDates.length > 6 && <button className="date-card date-card--more" type="button" onClick={() => { setShowAllDates((value) => !value); setCalendarOpen(false) }}><span>＋</span><strong>{showAllDates ? t('common.close') : t('patient.showMoreDates')}</strong><small>{availableDates.length - 6}</small></button>}</div>{calendarOpen && <div className="date-calendar" aria-label={t('patient.availableDates')}><div className="date-calendar__header"><button type="button" className="icon-button" aria-label={t('staff.previousPeriod')} onClick={() => setCalendarMonth((value) => new Date(value.getFullYear(), value.getMonth() - 1, 1, 12))}>‹</button><strong>{capitalized(new Intl.DateTimeFormat(localeFor(language), { month: 'long', year: 'numeric' }).format(calendarMonth))}</strong><button type="button" className="icon-button" aria-label={t('staff.nextPeriod')} onClick={() => setCalendarMonth((value) => new Date(value.getFullYear(), value.getMonth() + 1, 1, 12))}>›</button></div><div className="calendar-weekdays">{Array.from({ length: 7 }, (_, index) => <span key={index}>{formatWeekday(dateKeyFromDate(new Date(2024, 0, 1 + index)), language)}</span>)}</div><div ref={calendarMotionRef} className="calendar-grid">{calendarDateCells.map((date, index) => { const key = date ? dateKeyFromDate(date) : `empty-${index}`; const isAvailable = date && calendarDateKeys.has(key); const isPast = date && key < currentClinicDateKey(); return date ? <button type="button" key={key} className={`calendar-day ${selectedDate === key ? 'is-selected' : ''}`} disabled={!isAvailable || isPast} onClick={() => selectDate(key)}>{date.getDate()}</button> : <span className="calendar-day calendar-day--empty" key={key} /> })}</div></div>} {selectedDate && <div className="selected-date-panel"><div><span className="eyebrow">{t('patient.appointment')}</span><strong>{capitalized(formatDateLong(selectedDate, language))}</strong></div><button type="button" className="text-button" onClick={() => { setSelectedDate(''); setSelectedTime(''); setSelectedSlot(null) }}>{t('common.edit')}</button></div>}{selectedDate && <div ref={timeMotionRef} className="time-selection"><h3>{t('patient.chooseTime')}</h3>{Object.entries(groupedTimes).map(([group, groupSlots]) => <div className="time-group" key={group}><h4>{group}</h4><div className="slot-grid">{groupSlots.map((slot) => <button type="button" key={slot.id} className={`slot-button ${selectedSlot?.id === slot.id ? 'is-selected' : ''}`} onClick={() => selectTime(slot)}>{selectedSlot?.id === slot.id ? `✓ ${formatSlotTime(slot.startsAt)}` : formatSlotTime(slot.startsAt)}</button>)}</div></div>)}{selectedDateSlots.length === 0 && <div className="schedule-state"><p>{t('patient.noSlots')}</p><button className="button button--secondary button--compact" type="button" onClick={() => setSelectedDate('')}>{t('common.edit')}</button></div>}</div>}{selectedSlot && <BookingSummary subject={selectedSubject} doctor={selectedDoctor} dateKey={selectedDate} time={selectedTime} specialty={specialties.find((item) => String(item.id) === String(specialtyId))?.name} clinic={selectedDoctor?.clinic?.name} t={t} language={language} />}</>}<label className="field-group"><span className="field-label">{t('patient.note')}</span><input value={form.note} onChange={updateNote} placeholder={t('patient.notePlaceholder')} /></label><button className="button" disabled={saving || !selectedSubject || !doctorId || !selectedDate || !selectedTime || !selectedSlot}>{saving ? t('patient.confirmingBooking') : t('patient.confirmBooking')}</button></div></section>}</form></section>
  if (loading) return <section className="page-shell"><p>{t('patient.loadingProfile')}</p><PageMessage error={error} /></section>
  return bookingView
}

export function PatientAppointmentsPage() {
  const { t, language } = useTranslation()
  const [items, setItems] = useState([]); const [dependents, setDependents] = useState([]); const [filter, setFilter] = useState('ALL'); const [error, setError] = useState(''); const [loading, setLoading] = useState(true); const [declineId, setDeclineId] = useState(null); const [attendance, setAttendance] = useState(null); const [busyId, setBusyId] = useState(null)
  const load = () => Promise.all([getPatientAppointments(), getDependents(), getPatientProfile()]).then(([appointments, family, profile]) => { setItems(appointments); setDependents(family); return getAttendance(profile.id).then(setAttendance) }).catch((e) => setError(messageFor(e, t)))
  useEffect(() => { load().finally(() => setLoading(false)) }, [])
  const filtered = items.filter((item) => filter === 'ALL' || (filter === 'SELF' ? item.subject?.type === 'SELF' : String(item.subject?.id) === String(filter)))
  const cancel = async (id) => { try { await cancelPatientAppointment(id); await load() } catch (e) { setError(messageFor(e, t)) } }
  const confirm = async (id) => { setBusyId(id); try { await confirmPatientAppointment(id); await load() } catch (e) { setError(messageFor(e, t)) } finally { setBusyId(null) } }
  const decline = async () => { if (!declineId) return; setBusyId(declineId); try { await declinePatientAppointment(declineId); setDeclineId(null); await load() } catch (e) { setError(messageFor(e, t)) } finally { setBusyId(null) } }
  const upcoming = filtered.filter((item) => !['COMPLETED', 'CANCELLED', 'NO_SHOW'].includes(item.status))
  const finished = filtered.filter((item) => ['COMPLETED', 'CANCELLED', 'NO_SHOW'].includes(item.status))
  if (loading) return <section className="page-shell"><div className="page-heading"><div className="eyebrow">{t('patient.historyTab')}</div><h1>{t('patient.appointmentsTitle')}</h1></div><div className="patient-card patient-skeleton-card"><span className="skeleton skeleton--line" /><span className="skeleton skeleton--title" /><span className="skeleton skeleton--line" /><span className="skeleton skeleton--line" /></div></section>
  return <><section className="page-shell"><div className="page-heading page-heading--row"><div><div className="eyebrow">{t('patient.historyTab')}</div><h1>{t('patient.appointmentsTitle')}</h1><p className="form-description">{t('appointmentUi.description')}</p></div><button className="button button--compact" onClick={() => navigate('/patient/appointments')}>{t('appointmentUi.newAppointment')}</button></div><PageMessage error={error} />{attendance && <div className="attendance-card"><div><span className="eyebrow">{t('attendance.title')}</span><strong>{attendance.attendanceRate}%</strong></div><p>{t('attendance.completed')}: <b>{attendance.completedAppointments}</b> · {t('attendance.noShow')}: <b>{attendance.noShowAppointments}</b> · {t('attendance.cancelled')}: <b>{attendance.cancelledAppointments}</b></p></div>}{!items.length && <EmptyAppointments onAction={() => navigate('/patient/appointments')} t={t} />}{items.length > 0 && <><div className="filter-pills"><button className={filter === 'ALL' ? 'is-active' : ''} onClick={() => setFilter('ALL')}>{t('common.all')}</button><button className={filter === 'SELF' ? 'is-active' : ''} onClick={() => setFilter('SELF')}>{t('appointmentUi.myFilter')}</button>{dependents.map((item) => <button className={String(filter) === String(item.id) ? 'is-active' : ''} onClick={() => setFilter(String(item.id))} key={item.id}>{item.firstName}</button>)}</div><AppointmentGroup title={t('patient.upcomingTab')} items={upcoming} cancel={cancel} confirm={confirm} requestDecline={setDeclineId} busyId={busyId} emptyTitle={t('appointmentUi.noUpcoming')} t={t} language={language} /><AppointmentGroup title={t('patient.historyTab')} items={finished} cancel={cancel} emptyTitle={t('appointmentUi.historyEmpty')} t={t} language={language} /></>}</section>
    {declineId && <div className="modal-backdrop" role="presentation"><section className="confirm-dialog" role="dialog" aria-modal="true" aria-labelledby="decline-title"><h2 id="decline-title">{t('confirmation.declineTitle')}</h2><p>{t('confirmation.declineDescription')}</p><div className="form-actions"><button className="button button--secondary" type="button" onClick={() => setDeclineId(null)}>{t('common.cancel')}</button><button className="button button--danger" type="button" disabled={busyId === declineId} onClick={decline}>{t('confirmation.decline')}</button></div></section></div>}
  </>;
}

function AppointmentGroup({ title, items, cancel, confirm, requestDecline, busyId, emptyTitle, t, language }) { return <section className="records-section"><div className="section-heading"><h2>{title}</h2></div><div className="appointments-list">{items.length ? items.map((item) => <article className="appointment-card" key={item.id}><AppointmentRow item={item} detailed t={t} language={language} /><div className="appointment-card__actions"><span className={`status status--${String(item.status).toLowerCase()}`}><span>{statusLabel(item.status, t)}</span></span>{confirm && item.confirmationStatus === 'PENDING' && !['CANCELLED', 'COMPLETED', 'NO_SHOW'].includes(item.status) && <div className="appointment-confirmation-card"><strong>{t('confirmation.question')}</strong><div className="form-actions"><button className="button button--compact" type="button" disabled={busyId === item.id} onClick={() => confirm(item.id)}>{t('confirmation.confirm')}</button><button className="button button--secondary button--compact" type="button" disabled={busyId === item.id} onClick={() => requestDecline(item.id)}>{t('confirmation.decline')}</button></div></div>}{!['CANCELLED', 'COMPLETED', 'NO_SHOW'].includes(item.status) && <button className="text-button text-button--danger" onClick={() => { if (window.confirm(t('patient.cancelConfirm'))) cancel(item.id) }}>{t('patient.cancelAppointment')}</button>}</div></article>) : <div className="empty-state empty-state--inline"><p>{emptyTitle || t('patient.noAppointmentsSection')}</p></div>}</div></section> }
function EmptyAppointments({ onAction, t }) { return <div className="empty-state patient-empty-state"><span className="patient-empty-state__icon">◷</span><h2>{t('patient.noAppointmentsTitle')}</h2><p>{t('patient.noAppointmentsDescription')}</p><button className="button button--secondary button--compact" type="button" onClick={onAction}>{t('patient.bookForDoctor')}</button></div> }

function MedicalTabs({ active, t }) {
  const tabs = [['overview', 'patient.overview', '/patient/medical-record'], ['history', 'patient.historyTab', '/patient/medical-history'], ['documents', 'patient.documents', '/patient/documents']]
  return <nav className="medical-tabs" aria-label={t('patient.medicalCard')}>{tabs.map(([key, label, path]) => <button type="button" className={active === key ? 'is-active' : ''} onClick={() => navigate(path)} key={key}>{t(label)}</button>)}</nav>
}

function SubjectContext({ name, t }) { return name ? <div className="dependent-viewing-banner"><span>◉</span><p>{t('patient.profileDataLabel')} <strong>{name}</strong></p></div> : null }
function MedicalSkeleton() { return <div className="medical-skeleton"><span className="skeleton skeleton--line" /><span className="skeleton skeleton--title" /><span className="skeleton skeleton--line" /><span className="skeleton skeleton--line" /></div> }
function OverviewCard({ icon, title, value, meta }) { return <article className="medical-overview-card"><span className="patient-icon patient-icon--soft">{icon}</span><div><span>{title}</span><strong>{value}</strong>{meta && <small>{meta}</small>}</div></article> }

export function PatientClinicalDataPage({ kind = 'diagnoses' }) {
  const { t, language } = useTranslation()
  const [profile, setProfile] = useState(null)
  const [dependents, setDependents] = useState([])
  const [selected, setSelected] = useState('SELF')
  const [items, setItems] = useState([])
  const [documentType, setDocumentType] = useState('ALL')
  const [documentFrom, setDocumentFrom] = useState('')
  const [documentTo, setDocumentTo] = useState('')
  const [selectedDocument, setSelectedDocument] = useState(null)
  const [error, setError] = useState(''); const [loading, setLoading] = useState(true)
  const resultsRef = useRef(null)
  useMotion(resultsRef, [kind, selected, documentType, documentFrom, documentTo, loading].join('|'), 'results')
  const loaders = { diagnoses: getPatientDiagnoses, prescriptions: getPatientPrescriptions, medications: getPatientMedications, labs: getPatientLabResults, referrals: getPatientReferrals, certificates: getPatientCertificates, documents: getPatientDocuments }
  const titles = { diagnoses: 'patient.diagnoses', prescriptions: 'patient.prescriptions', medications: 'patient.medications', labs: 'patient.labs', referrals: 'patient.referrals', certificates: 'patient.certificates', documents: 'patient.documents' }

  useEffect(() => {
    Promise.all([getPatientProfile(), getDependents()]).then(([patient, family]) => { setProfile(patient); setDependents(family) }).catch((e) => setError(messageFor(e, t))).finally(() => setLoading(false))
  }, [t])
  useEffect(() => {
    if (!profile) return
    const subjectId = selected === 'SELF' ? null : selected
    setLoading(true); setError(''); setItems([]); setSelectedDocument(null)
    const load = (loaders[kind] || loaders.diagnoses)(subjectId)
    load.then(setItems).catch((e) => setError(messageFor(e, t))).finally(() => setLoading(false))
  }, [profile, selected, kind, t])

  if (!profile) return <section className="page-shell"><p>{t('patient.loadingMedical')}</p><PageMessage error={error} /></section>
  const subjectName = selected === 'SELF' ? fullName(profile) : dependents.find((item) => String(item.id) === String(selected))?.fullName
  const emptyCopy = { diagnoses: ['medicalUi.noDiagnoses', 'medicalUi.noDiagnosesDescription'], prescriptions: ['medicalUi.noPrescriptions', 'medicalUi.noPrescriptionsDescription'], medications: ['medicalUi.noMedications', 'medicalUi.noMedicationsDescription'], labs: ['medicalUi.noLabs', 'medicalUi.noLabsDescription'], referrals: ['medicalUi.noReferrals', 'medicalUi.noReferralsDescription'], certificates: ['medicalUi.noCertificates', 'medicalUi.noCertificatesDescription'], documents: ['medicalUi.noDocuments', 'medicalUi.noDocumentsDescription'] }[kind] || ['medicalUi.noData', 'medicalUi.noDataDescription']
  const visibleItems = kind === 'documents' ? items.filter((item) => {
    const date = String(item.approvedAt || item.createdAt || '').slice(0, 10)
    return (documentType === 'ALL' || item.type === documentType)
      && (!documentFrom || date >= documentFrom)
      && (!documentTo || date <= documentTo)
  }) : items
  const subjectId = selected === 'SELF' ? null : selected
  const openDocument = async (item) => {
    try { setError(''); setSelectedDocument(await getPatientDocument(item.id, subjectId)) }
    catch (requestError) { setError(messageFor(requestError, t)) }
  }
  return <section className="page-shell medical-record-page"><div className="page-heading"><div className="eyebrow">{t('patient.medicalCard')}</div><h1>{t(titles[kind] || titles.diagnoses)}</h1><p className="form-description">{t('patient.medicalCardDescription')}</p></div><MedicalTabs active={kind} t={t} /><PageMessage error={error} /><div className="subject-tabs"><button className={selected === 'SELF' ? 'is-active' : ''} onClick={() => setSelected('SELF')}>{fullName(profile) || t('patient.myself')}</button>{dependents.map((item) => <button className={String(selected) === String(item.id) ? 'is-active' : ''} onClick={() => setSelected(String(item.id))} key={item.id}>{item.firstName} · {item.relationshipLabel}</button>)}</div><SubjectContext name={subjectName} t={t} />{kind === 'documents' && <div className="medical-filter-bar document-filter-bar"><label className="field-group"><span className="field-label">{t('patientDocuments.documentTypeFilter')}</span><select value={documentType} onChange={(event) => setDocumentType(event.target.value)}><option value="ALL">{t('patientDocuments.allDocumentTypes')}</option>{patientDocumentTypes.map((type) => <option value={type} key={type}>{documentTypeLabel(type, t)}</option>)}</select></label><label className="field-group"><span className="field-label">{t('patient.fromDate')}</span><input type="date" value={documentFrom} onChange={(event) => setDocumentFrom(event.target.value)} /></label><label className="field-group"><span className="field-label">{t('patient.toDate')}</span><input type="date" value={documentTo} onChange={(event) => setDocumentTo(event.target.value)} /></label></div>}{loading ? <MedicalSkeleton /> : error ? null : visibleItems.length ? <div ref={resultsRef} className="records-section">{visibleItems.map((item, index) => <ClinicalDataCard key={`${item.id || 'item'}-${index}`} item={item} kind={kind} t={t} language={language} subjectId={subjectId} onViewDocument={openDocument} />)}</div> : <EmptyMedicalState title={t(emptyCopy[0])} description={t(emptyCopy[1])} t={t} />}{selectedDocument && <PatientDocumentViewer document={selectedDocument} subjectId={subjectId} t={t} language={language} onClose={() => setSelectedDocument(null)} />}</section>
}

const patientDocumentCategories = ['ALL', 'PRESCRIPTIONS', 'LABS', 'REFERRALS', 'CERTIFICATES', 'CONCLUSIONS']

function documentCategoryForType(type) {
  if (type === 'PRESCRIPTION') return 'PRESCRIPTIONS'
  if (['LAB_REQUEST', 'LAB_RESULT'].includes(type)) return 'LABS'
  if (type === 'REFERRAL') return 'REFERRALS'
  if (type === 'CERTIFICATE') return 'CERTIFICATES'
  if (type === 'CONCLUSION') return 'CONCLUSIONS'
  return 'ALL'
}

function recordDate(value) {
  return value?.date || value?.approvedAt || value?.issuedAt || value?.testDate || value?.completedAt || value?.createdAt || value?.appointmentAt || ''
}

function recordDateKey(value) { return String(recordDate(value) || '').slice(0, 10) }
function normalizeRecordText(value) { return String(value || '').trim().toLocaleLowerCase().replace(/\s+/g, ' ') }
function visitForRecord(value, visits) { return visits.get(value?.medicalVisitId ?? value?.visitId) }

function buildUnifiedPatientDocuments({ documents = [], prescriptions = [], medications = [], labs = [], referrals = [], certificates = [], history = {} }) {
  const visits = new Map((history.visits || []).map((visit) => [visit.id, visit]))
  const documentItems = documents.map((value) => {
    const visit = visits.get(value.visitId)
    return {
      id: `document-${value.id}`, sourceId: value.id, category: documentCategoryForType(value.type), kind: 'DOCUMENT',
      title: value.title || value.type, date: recordDate(value), visitId: value.visitId, visit,
      doctorName: value.doctorName || visit?.doctor?.fullName, organization: value.clinicName || visit?.clinic?.name,
      status: value.status, document: value, details: [value.description].filter(Boolean),
    }
  })
  const structuredItems = [
    ...prescriptions.map((value) => ({
      id: `prescription-${value.id}`, sourceId: value.id, category: 'PRESCRIPTIONS', kind: 'PRESCRIPTION', title: value.title,
      date: recordDate(value) || visitForRecord(value, visits)?.completedAt || visitForRecord(value, visits)?.appointmentAt,
      visitId: value.medicalVisitId, visit: visitForRecord(value, visits), status: value.status,
      details: [value.description, value.instructions, visitForRecord(value, visits)?.recommendations, value.startDate && `С ${value.startDate}`, value.endDate && `До ${value.endDate}`].filter(Boolean),
    })),
    ...medications.map((value) => ({
      id: `medication-${value.id}`, sourceId: value.id, category: 'PRESCRIPTIONS', kind: 'MEDICATION', title: value.medicationName,
      date: recordDate(value) || visitForRecord(value, visits)?.completedAt || visitForRecord(value, visits)?.appointmentAt,
      visitId: value.medicalVisitId, visit: visitForRecord(value, visits), status: value.status,
      details: [value.dosage && `${value.dosage}${value.dosageUnit ? ` ${value.dosageUnit}` : ''}`, value.frequency, value.route, value.duration || (value.durationDays && `${value.durationDays} дн.`), value.instructions, visitForRecord(value, visits)?.recommendations].filter(Boolean),
    })),
    ...labs.map((value) => ({
      id: `lab-${value.id}`, sourceId: value.id, category: 'LABS', kind: 'LAB', title: value.testName,
      date: recordDate(value) || visitForRecord(value, visits)?.completedAt || visitForRecord(value, visits)?.appointmentAt,
      visitId: value.visitId, visit: visitForRecord(value, visits), status: value.status, doctorName: value.doctorName,
      details: [value.result, value.referenceRange, value.comment, value.testCode].filter(Boolean),
    })),
    ...referrals.map((value) => ({
      id: `referral-${value.id}`, sourceId: value.id, category: 'REFERRALS', kind: 'REFERRAL', title: value.referralType,
      date: recordDate(value) || visitForRecord(value, visits)?.completedAt || visitForRecord(value, visits)?.appointmentAt,
      visitId: value.medicalVisitId, visit: visitForRecord(value, visits), status: value.status,
      organization: value.targetClinicName, doctorName: value.targetDoctorName,
      details: [value.targetSpecialtyName, value.reason, value.notes, value.validUntil && `До ${value.validUntil}`].filter(Boolean),
    })),
    ...certificates.map((value) => ({
      id: `certificate-${value.id}`, sourceId: value.id, category: 'CERTIFICATES', kind: 'CERTIFICATE', title: value.title || value.type,
      date: recordDate(value), visitId: value.visitId, visit: visitForRecord(value, visits), status: value.status, doctorName: value.doctorName,
      details: [value.documentNumber, value.description, value.validFrom && `С ${value.validFrom}`, value.validUntil && `До ${value.validUntil}`].filter(Boolean),
    })),
  ]

  const consumedDocuments = new Set()
  const linkedStructuredItems = structuredItems.map((item) => {
    const candidates = documentItems.filter((document) => document.category === item.category && document.visitId && document.visitId === item.visitId && !consumedDocuments.has(document.sourceId))
    const exact = candidates.find((document) => normalizeRecordText(document.title) === normalizeRecordText(item.title))
    const linked = exact || (candidates.length === 1 ? candidates[0] : null)
    if (linked) consumedDocuments.add(linked.sourceId)
    return { ...item, document: linked?.document, doctorName: item.doctorName || linked?.doctorName || item.visit?.doctor?.fullName, organization: item.organization || linked?.organization || item.visit?.clinic?.name }
  })

  return [...documentItems.filter((item) => !consumedDocuments.has(item.sourceId)), ...linkedStructuredItems]
}

export function PatientDocumentsPage() {
  const { t, language } = useTranslation()
  const location = useLocation()
  const queryCategory = (new URLSearchParams(location.search).get('category') || '').toUpperCase()
  const [profile, setProfile] = useState(null)
  const [dependents, setDependents] = useState([])
  const [selected, setSelected] = useState('SELF')
  const [category, setCategory] = useState(patientDocumentCategories.includes(queryCategory) ? queryCategory : 'ALL')
  const [search, setSearch] = useState('')
  const [fromDate, setFromDate] = useState('')
  const [toDate, setToDate] = useState('')
  const [status, setStatus] = useState('ALL')
  const [items, setItems] = useState([])
  const [selectedDocument, setSelectedDocument] = useState(null)
  const [error, setError] = useState('')
  const [loading, setLoading] = useState(true)
  const resultsRef = useRef(null)
  useMotion(resultsRef, [selected, category, search, fromDate, toDate, status, loading].join('|'), 'results')

  useEffect(() => {
    if (patientDocumentCategories.includes(queryCategory)) setCategory(queryCategory)
  }, [queryCategory])

  useEffect(() => {
    Promise.all([getPatientProfile(), getDependents()])
      .then(([patient, family]) => { setProfile(patient); setDependents(family) })
      .catch((requestError) => setError(messageFor(requestError, t)))
      .finally(() => setLoading(false))
  }, [t])

  useEffect(() => {
    if (!profile) return
    const subjectId = selected === 'SELF' ? null : selected
    setLoading(true); setError(''); setSelectedDocument(null)
    Promise.all([
      getPatientDocuments(subjectId), getPatientPrescriptions(subjectId), getPatientMedications(subjectId),
      getPatientLabResults(subjectId), getPatientReferrals(subjectId), getPatientCertificates(subjectId), getPatientMedicalHistory(subjectId),
    ])
      .then(([documents, prescriptions, medications, labs, referrals, certificates, history]) => setItems(buildUnifiedPatientDocuments({ documents, prescriptions, medications, labs, referrals, certificates, history })))
      .catch((requestError) => { setItems([]); setError(messageFor(requestError, t)) })
      .finally(() => setLoading(false))
  }, [profile, selected, t])

  const chooseCategory = (value) => {
    setCategory(value)
    const params = new URLSearchParams(location.search)
    if (value === 'ALL') params.delete('category'); else params.set('category', value.toLowerCase())
    const next = params.toString() ? `/patient/documents?${params}` : '/patient/documents'
    navigate(next, { replace: true })
  }

  const statusOptions = [...new Set(items.map((item) => item.status).filter(Boolean))].sort()
  const visibleItems = items.filter((item) => {
    const haystack = [item.title, item.doctorName, item.organization, ...(item.details || [])].join(' ').toLocaleLowerCase()
    const date = recordDateKey(item)
    return (category === 'ALL' || item.category === category)
      && (!search.trim() || haystack.includes(search.trim().toLocaleLowerCase()))
      && (!fromDate || date >= fromDate) && (!toDate || date <= toDate)
      && (status === 'ALL' || item.status === status)
  }).sort((left, right) => String(recordDate(right) || '').localeCompare(String(recordDate(left) || '')))

  if (!profile) return <section className="page-shell"><p>{t('patient.loadingMedical')}</p><PageMessage error={error} /></section>
  const subjectId = selected === 'SELF' ? null : selected
  const subjectName = selected === 'SELF' ? fullName(profile) : dependents.find((item) => String(item.id) === String(selected))?.fullName
  return <section className="page-shell medical-record-page patient-documents-page">
    <div className="page-heading"><div className="eyebrow">{t('patient.medicalCard')}</div><h1>{t('patient.documents')}</h1><p className="form-description">{t('patientDocuments.description')}</p></div>
    <MedicalTabs active="documents" t={t} />
    <PageMessage error={error} />
    <div className="subject-tabs"><button className={selected === 'SELF' ? 'is-active' : ''} onClick={() => setSelected('SELF')}>{fullName(profile) || t('patient.myself')}</button>{dependents.map((item) => <button className={String(selected) === String(item.id) ? 'is-active' : ''} onClick={() => setSelected(String(item.id))} key={item.id}>{item.firstName} · {item.relationshipLabel}</button>)}</div>
    <SubjectContext name={subjectName} t={t} />
    <div className="document-category-tabs" role="tablist" aria-label={t('patient.documents')}>
      {patientDocumentCategories.map((value) => <button type="button" role="tab" aria-selected={category === value} className={category === value ? 'is-active' : ''} onClick={() => chooseCategory(value)} key={value}><span aria-hidden="true">{documentCategoryIcon(value)}</span>{t(`patientDocuments.categories.${value}`)}</button>)}
    </div>
    <div className="medical-filter-bar patient-documents-filter"><label className="field-group"><span className="field-label">{t('patientDocuments.search')}</span><input value={search} onChange={(event) => setSearch(event.target.value)} placeholder={t('patientDocuments.searchPlaceholder')} /></label><label className="field-group"><span className="field-label">{t('patientDocuments.statusFilter')}</span><select value={status} onChange={(event) => setStatus(event.target.value)}><option value="ALL">{t('patientDocuments.allStatuses')}</option>{statusOptions.map((value) => <option value={value} key={value}>{value}</option>)}</select></label><label className="field-group"><span className="field-label">{t('patient.fromDate')}</span><input type="date" value={fromDate} onChange={(event) => setFromDate(event.target.value)} /></label><label className="field-group"><span className="field-label">{t('patient.toDate')}</span><input type="date" value={toDate} onChange={(event) => setToDate(event.target.value)} /></label></div>
    {loading ? <MedicalSkeleton /> : error ? null : visibleItems.length ? <div ref={resultsRef} className="records-section unified-document-list">{visibleItems.map((item) => <UnifiedPatientDocumentCard item={item} key={item.id} t={t} language={language} subjectId={subjectId} onViewDocument={setSelectedDocument} />)}</div> : <EmptyMedicalState title={t('patientDocuments.emptyTitle')} description={t(category === 'ALL' ? 'patientDocuments.emptyDescription' : 'patientDocuments.emptyCategoryDescription')} />}
    {selectedDocument && <PatientDocumentViewer document={selectedDocument} subjectId={subjectId} t={t} language={language} onClose={() => setSelectedDocument(null)} />}
  </section>
}

function documentCategoryIcon(category) { return { ALL: '▤', PRESCRIPTIONS: '✚', LABS: '⌁', REFERRALS: '↗', CERTIFICATES: '▣', CONCLUSIONS: '✓' }[category] || '▤' }
function documentStatusClass(status) { return String(status || '').toLowerCase().replaceAll('_', '-') }

function UnifiedPatientDocumentCard({ item, t, language, subjectId, onViewDocument }) {
  const document = item.document
  const visit = item.visit
  const title = item.title || t('patientDocuments.untitled')
  const date = recordDateKey(item)
  const displayCategory = item.category === 'ALL' && document?.type ? t(`patientDocuments.types.${document.type}`) : t(`patientDocuments.categories.${item.category}`)
  const details = [...(item.details || [])]
  if (item.category === 'CONCLUSIONS' && visit?.diagnosis) details.push(`${t('patient.diagnosis')}: ${visit.diagnosisName || visit.diagnosis}`)
  if (item.category === 'CONCLUSIONS' && visit?.recommendations) details.push(`${t('patient.recommendations')}: ${visit.recommendations}`)
  return <article className="record-card unified-document-card"><div className="record-card__header"><div><span className="eyebrow"><span className="document-category-icon" aria-hidden="true">{documentCategoryIcon(item.category)}</span> {displayCategory} · {date ? formatDateWithYear(date, language) : t('patient.dateNotSpecified')}</span><h3>{title}</h3></div>{item.status && <span className={`status status--${documentStatusClass(item.status)}`}>{item.status}</span>}</div><div className="unified-document-card__meta">{item.doctorName && <span>{t('patient.issuedBy')}: {item.doctorName}</span>}{item.organization && <span>{t('patientDocuments.organization')}: {item.organization}</span>}{item.visitId && <span>{t('patientDocuments.visitNumber')}: #{item.visitId}</span>}</div>{details.length ? <div className="unified-document-card__details">{details.map((detail, index) => <p key={`${item.id}-detail-${index}`}>{detail}</p>)}</div> : null}<div className="record-card__actions">{document && <><button className="button button--secondary button--compact" type="button" onClick={() => onViewDocument(document)}>{t('documentEditor.view')}</button><PatientDocumentPdfButton id={document.id} subjectId={subjectId} t={t} /></>}{item.visitId && <button className="button button--text button--compact" type="button" onClick={() => navigate(`/patient/medical-history?visitId=${item.visitId}${subjectId ? `&subjectId=${subjectId}` : ''}`)}>{t('patientDocuments.openVisit')}</button>}</div></article>
}

const patientDocumentTypes = ['CERTIFICATE', 'REFERRAL', 'PRESCRIPTION', 'CONCLUSION', 'REPORT', 'LAB_REQUEST', 'LAB_RESULT', 'SCAN', 'XRAY', 'MRI', 'CT', 'ULTRASOUND', 'OTHER']
  function documentTypeLabel(type, t) { return t(`patientDocuments.types.${type}`) || type }

function ClinicalDataCard({ item, kind, t, language, subjectId, onViewDocument }) {
  if (kind === 'diagnoses') return <article className="record-card"><span className="eyebrow">{t('workflow.icd10')} · {item.icd10Code?.code}</span><h3>{item.icd10Code?.name}</h3>{item.notes && <p>{item.notes}</p>}</article>
  if (kind === 'prescriptions') return <article className="record-card"><span className="eyebrow">{t('patient.prescription')}</span><h3>{item.title}</h3>{item.description && <p>{item.description}</p>}{item.instructions && <p><strong>{t('patient.howToPerform')}</strong> {item.instructions}</p>}</article>
  if (kind === 'labs') return <article className="record-card"><span className="eyebrow">{t('patient.lab')} · {item.testDate || t('patient.dateNotSpecified')}</span><h3>{item.testName}</h3>{item.status && <p><strong>{t('patient.status')}:</strong> {labStatusLabel(item.status, t)}</p>}{item.result && <p><strong>{t('patient.result')}:</strong> {item.result}</p>}{item.referenceRange && <p><strong>{t('patient.referenceRange')}:</strong> {item.referenceRange}</p>}{item.comment && <p>{item.comment}</p>}{item.doctorName && <small className="muted-text">{t('patient.assignedBy')}: {item.doctorName}</small>}</article>
  if (kind === 'referrals') return <article className="record-card"><span className="eyebrow">{t('patient.referrals')}</span><h3>{item.referralType || item.type || t('patient.medicalDocument')}</h3>{item.reason && <p><strong>{t('patient.reason')}</strong>{item.reason}</p>}{item.destination && <p><strong>{t('patient.destination')}</strong>{item.destination}</p>}{item.visit?.appointmentAt && <small className="muted-text">{t('patient.afterAppointment')} {appointmentDateTime(item.visit.appointmentAt, language, t)}</small>}</article>
  if (kind === 'certificates') return <article className="record-card"><span className="eyebrow">{t('patient.certificate')} · {item.issuedAt?.slice(0, 10)}</span><h3>{item.type}</h3>{item.description && <p>{item.description}</p>}{item.validUntil && <p><strong>{t('patient.validUntil')}:</strong> {item.validUntil}</p>}{item.doctorName && <small className="muted-text">{t('patient.issuedBy')}: {item.doctorName}</small>}</article>
  if (kind === 'documents') return <article className="record-card"><span className="eyebrow">{item.documentNumber || t('patient.document')} · {item.status === 'APPROVED' ? t('documentEditor.approved') : item.status || t('documentEditor.approved')}</span><h3>{item.title || item.name || item.type || t('patient.medicalDocument')}</h3>{item.description && <p>{item.description}</p>}<small className="muted-text">{item.approvedAt?.slice(0, 10) || item.createdAt?.slice(0, 10) || t('patient.dateNotSpecified')}{item.doctorName ? ` · ${item.doctorName}` : ''}{item.clinicName ? ` · ${item.clinicName}` : ''}</small>{item.type && <p><strong>{t('patient.type')}</strong> {documentTypeLabel(item.type, t)}</p>}{item.appointmentAt && <small className="muted-text">{t('patient.afterAppointment')} {appointmentDateTime(item.appointmentAt, language, t)}</small>}{item.id && <div className="record-card__actions"><button className="button button--secondary button--compact" type="button" onClick={() => onViewDocument?.(item)}>{t('documentEditor.view')}</button><PatientDocumentPdfButton id={item.id} subjectId={subjectId} t={t} /></div>}</article>
  return <article className="record-card"><span className="eyebrow">{t('patient.prescriptionMedicine')}</span><h3>{item.medicationName}</h3><p>{[item.dosage, item.frequency, item.duration].filter(Boolean).join(' · ')}</p>{item.instructions && <p>{item.instructions}</p>}</article>
}

function PatientDocumentPdfButton({ id, subjectId, t }) {
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState('')
  const openPdf = async (print = false) => {
    setBusy(true); setError('')
    try {
      const blob = await downloadPatientDocumentPdf(id, subjectId)
      const url = URL.createObjectURL(blob)
      if (print) window.open(url, '_blank', 'noopener,noreferrer')
      else { const link = document.createElement('a'); link.href = url; link.download = `QamqorMed-${id}.pdf`; link.click() }
      window.setTimeout(() => URL.revokeObjectURL(url), 60000)
    } catch (requestError) { setError(requestError.message || t('common.error')) } finally { setBusy(false) }
  }
  return <><button className="button button--secondary button--compact" type="button" disabled={busy} onClick={() => openPdf(false)}>{busy ? t('common.loading') : t('documentEditor.downloadPdf')}</button><button className="button button--secondary button--compact" type="button" disabled={busy} onClick={() => openPdf(true)}>{t('documentEditor.print')}</button>{error && <small className="error-message">{error}</small>}</>
}

function PatientDocumentViewer({ document: value, subjectId, t, language, onClose }) {
  return <div className="dev-modal-backdrop" role="presentation" onMouseDown={(event) => event.target === event.currentTarget && onClose()}><section className="dev-modal document-viewer" role="dialog" aria-modal="true" aria-labelledby="patient-document-title"><div className="dev-modal-header"><div><span className="dev-section-kicker">{t('patient.medicalDocument')}</span><h2 id="patient-document-title">{value.title || t('patient.document')}</h2></div><button className="dev-close-button" type="button" aria-label={t('common.close')} onClick={onClose}>×</button></div><div className="document-viewer__meta"><span>{value.documentNumber || '—'}</span><span>{value.approvedAt?.slice(0, 10) || value.createdAt?.slice(0, 10) || t('patient.dateNotSpecified')}</span><span>{value.doctorName || '—'}</span><span>{value.clinicName || '—'}</span></div><pre className="document-viewer__content">{value.finalizedContent || value.description || t('patientDocuments.documentContentUnavailable')}</pre><div className="document-viewer__actions"><PatientDocumentPdfButton id={value.id} subjectId={subjectId} t={t} /><button className="button button--secondary button--compact" type="button" onClick={onClose}>{t('common.close')}</button></div></section></div>
}

function labStatusLabel(value, t) { return { READY: t('patient.ready'), COMPLETED: t('patient.ready'), PENDING: t('patient.pending'), IN_PROGRESS: t('patient.inProgress'), CANCELLED: t('patient.cancelled') }[value] || t('patient.status') }

function SubjectChoice({ selected, onClick, title, subtitle }) { return <button type="button" className={`subject-choice ${selected ? 'is-selected' : ''}`} onClick={onClick}><span className="subject-choice__radio">{selected ? '✓' : ''}</span><span><strong>{title}</strong><small>{subtitle}</small></span></button> }
function BookingSummary({ subject, doctor, specialty, clinic, dateKey, time, t, language }) { return <div className="booking-summary"><div className="booking-summary__heading"><span className="eyebrow">{t('patient.bookingTitle')}</span><strong>{formatDateWithYear(dateKey, language)} · {time}</strong></div><dl><div><dt>{t('workflow.patient')}</dt><dd>{subject ? fullName(subject) : '—'}{subject?.relationshipLabel && <small>{t('patient.childDependent')} · {subject.relationshipLabel}</small>}</dd></div><div><dt>{t('patient.clinic')}</dt><dd>{clinic || '—'}</dd></div><div><dt>{t('staff.specialty')}</dt><dd>{specialty || '—'}</dd></div><div><dt>{t('workflow.doctor')}</dt><dd>{doctor ? `${doctor.lastName} ${doctor.firstName}` : '—'}</dd></div></dl></div> }
function AppointmentRow({ item, detailed = false, t, language }) { return <div className="appointment-row"><div><span className="eyebrow">{t('patient.patient')}</span><strong>{item.subject?.fullName}</strong><small>{item.subject?.type === 'DEPENDENT' ? `${item.subject.relationship || t('patient.dependentProfile')} · ${t('workflow.bookedBy', { name: item.bookedBy?.fullName })}` : t('patient.selfBooking')}</small></div><div><span className="eyebrow">{t('workflow.doctor')}</span><strong>{item.doctor?.fullName}</strong><small>{item.doctor?.specialty} · {item.clinic?.name}</small></div><div><span className="eyebrow">{t('workflow.time')}</span><strong>{appointmentDateTime(item.appointmentAt, language, t)}</strong>{detailed && <small>{item.clinic?.cityName}</small>}</div></div> }
function RecordCard({ record, t, language }) { return <article className="record-card"><div className="record-card__header"><div><span className="eyebrow">{record.createdAt?.slice(0, 10)} · {record.doctor?.specialty}</span><h3>{record.subject?.fullName}</h3></div><span className="record-doctor">{record.doctor?.fullName}</span></div>{[[t('patient.diagnosis'), record.diagnosis], [t('patient.recommendations'), record.recommendations], [t('patient.treatments'), record.prescriptions], [t('patient.referrals'), record.referrals]].filter(([, value]) => value).map(([label, value]) => <p key={label}><strong>{label}</strong>{value}</p>)}</article> }
function VisitCard({ visit, t, subjectId, onViewDocument }) { return <article className="record-card" id={`visit-${visit.id}`}><div className="record-card__header"><div><span className="eyebrow">{visit.appointmentAt?.replace('T', ' ')} · {visit.doctor?.specialty}</span><h3>{visit.diagnosisName || visit.diagnosis || t('patient.appointment')}</h3><small className="muted-text">{visit.clinic?.name} · {t('patient.completedVisitsEmpty')}</small></div><span className="record-doctor">{visit.doctor?.fullName}</span></div><p className="muted-text">{t('patient.appointment')} #{visit.appointmentId}</p>{[[t('patient.diagnosis'), visit.diagnosis], [t('patient.complaints'), visit.chiefComplaint], [t('patient.anamnesis'), visit.anamnesis], [t('patient.recommendations'), visit.recommendations], [t('patient.doctorNotes'), visit.doctorNotes]].filter(([, value]) => value).map(([label, value]) => <p key={label}><strong>{label}</strong>{value}</p>)}<ListForVisit title={t('patient.treatments')} items={visit.treatments} render={(item) => `${item.name}${item.dosage ? ` · ${item.dosage}` : ''}${item.frequency ? ` · ${item.frequency}` : ''}${item.duration ? ` · ${item.duration}` : ''}`} /><ListForVisit title={t('patient.referrals')} items={visit.referrals} render={(item) => `${item.referralType}${item.reason ? ` · ${item.reason}` : ''}`} />{visit.documents?.length ? <div className="visit-list"><strong>{t('patient.documents')}</strong>{visit.documents.map((item) => <div className="visit-document-row" key={item.id}><span>{item.title || documentTypeLabel(item.type, t)}<small>{item.documentNumber || item.status || ''}</small></span><div><button className="button button--secondary button--compact" type="button" onClick={() => onViewDocument?.(item)}>{t('documentEditor.view')}</button><PatientDocumentPdfButton id={item.id} subjectId={subjectId} t={t} /></div></div>)}</div> : null}</article> }
function ListForVisit({ title, items = [], render }) { return items?.length ? <div className="visit-list"><strong>{title}</strong>{items.map((item) => <span key={item.id}>• {render(item)}</span>)}</div> : null }
function statusLabel(status, t) { const key = { BOOKED: 'booked', CONFIRMED: 'confirmed', ARRIVED: 'arrived', PREPARING: 'preparing', READY: 'ready', IN_APPOINTMENT: 'inAppointment', COMPLETED: 'completed', CANCELLED: 'cancelled', NO_SHOW: 'noShow', SCHEDULED: 'booked', CHECKED_IN: 'arrived', NURSE_IN_PROGRESS: 'preparing', READY_FOR_DOCTOR: 'ready', DOCTOR_IN_PROGRESS: 'inAppointment', IN_PROGRESS: 'preparing' }[status]; return key ? t(`appointment.status.${key}`) : t('staff.unknownStatus') }
function emptyDependent() { return { firstName: '', lastName: '', middleName: '', dateOfBirth: '', gender: '', iin: '', relationship: '', phone: '' } }

export function PatientMedicalHistoryPage() {
  const location = useLocation()
  const { t, language } = useTranslation()
  const historyOnly = location.pathname === '/patient/medical-history'
  const [profile, setProfile] = useState(null)
  const [dependents, setDependents] = useState([])
  const [selected, setSelected] = useState('SELF')
  const [data, setData] = useState(null)
  const [error, setError] = useState('')
  const [loading, setLoading] = useState(true)
  const [reloadKey, setReloadKey] = useState(0)
  const [timelineType, setTimelineType] = useState('ALL')
  const [timelineFrom, setTimelineFrom] = useState('')
  const [timelineTo, setTimelineTo] = useState('')
  const [selectedDocument, setSelectedDocument] = useState(null)
  const timelineRef = useRef(null)
  useMotion(timelineRef, [selected, timelineType, timelineFrom, timelineTo, loading].join('|'), 'results')
  const targetVisitId = new URLSearchParams(location.search).get('visitId')
  const targetSubjectId = new URLSearchParams(location.search).get('subjectId')

  useEffect(() => {
    if (targetSubjectId) setSelected(targetSubjectId)
  }, [targetSubjectId])

  useEffect(() => {
    setLoading(true)
    Promise.all([getPatientProfile(), getDependents()])
      .then(([patient, family]) => { setProfile(patient); setDependents(family) })
      .catch((e) => setError(messageFor(e, t)))
      .finally(() => setLoading(false))
  }, [reloadKey, t])

  useEffect(() => {
    if (!profile) return
    const subjectId = selected === 'SELF' ? null : selected
    setLoading(true)
    setError('')
    setData(null)
    getPatientMedicalHistory(subjectId)
      .then(setData)
      .catch((e) => setError(messageFor(e, t)))
      .finally(() => setLoading(false))
  }, [profile, selected, reloadKey, t])

  useEffect(() => {
    if (loading || !targetVisitId) return
    const target = document.getElementById(`visit-${targetVisitId}`)
    target?.scrollIntoView({ behavior: 'smooth', block: 'center' })
  }, [loading, targetVisitId, data])

  if (!profile) return <section className="page-shell"><p>{loading ? t('patient.loadingRecord') : t('patient.notLoadedProfile')}</p><PageMessage error={error} />{error && <button className="button button--secondary" type="button" onClick={() => setReloadKey((value) => value + 1)}>{t('staff.retry')}</button>}</section>
  if (loading && !data) return <section className="page-shell"><p>{t('patient.loadingRecord')}</p><PageMessage error={error} /></section>
  const subjectName = selected === 'SELF' ? fullName(profile) : dependents.find((item) => String(item.id) === String(selected))?.fullName
  const summary = data?.summary || {}
  const records = data?.visits || []
  const timeline = data?.timeline || []
  const visibleTimeline = timeline.filter((item) => {
    const date = String(item.date || '').slice(0, 10)
    return (timelineType === 'ALL' || item.type === timelineType)
      && (!timelineFrom || date >= timelineFrom)
      && (!timelineTo || date <= timelineTo)
  })
  const openDocument = async (item) => {
    try { setError(''); setSelectedDocument(await getPatientDocument(item.id, selected === 'SELF' ? null : selected)) }
    catch (requestError) { setError(messageFor(requestError, t)) }
  }
  return <section className="page-shell medical-record-page">
    <div className="page-heading"><div className="eyebrow">{t('patient.medicalCard')}</div><h1>{historyOnly ? t('patient.historyTitle') : t('patient.overview')}</h1><p className="form-description">{t('patient.medicalCardDescription')}</p></div>
    <MedicalTabs active={historyOnly ? 'history' : 'overview'} t={t} />
     <PageMessage error={error} />{error && <button className="button button--secondary button--compact" type="button" onClick={() => setReloadKey((value) => value + 1)}>{t('staff.retry')}</button>}
    <div className="subject-tabs"><button className={selected === 'SELF' ? 'is-active' : ''} onClick={() => setSelected('SELF')}>{fullName(profile) || t('patient.myself')}</button>{dependents.map((item) => <button className={String(selected) === String(item.id) ? 'is-active' : ''} onClick={() => setSelected(String(item.id))} key={item.id}>{item.firstName} · {item.relationshipLabel}</button>)}</div>
    <SubjectContext name={subjectName} t={t} />
    {loading ? <MedicalSkeleton /> : historyOnly ? <div className="records-section">{records.length ? records.map((record) => <VisitCard visit={record} key={record.id} t={t} language={language} subjectId={selected === 'SELF' ? null : selected} onViewDocument={openDocument} />) : <EmptyMedicalState title={t('patient.historyEmpty')} description={t('patient.completedVisitsEmpty')} />}</div> : <><div className="medical-overview-grid">
      <OverviewCard icon="◷" title={t('patient.visits')} value={summary.visits ?? 0} meta={records[0] ? formatDateWithYear(String(records[0].appointmentAt).slice(0, 10), language) : t('medicalUi.completedVisitsEmpty')} />
      <OverviewCard icon="◇" title={t('patient.diagnoses')} value={summary.diagnoses ?? 0} meta={records[0]?.diagnosisName || t('medicalUi.noDiagnoses')} />
      <OverviewCard icon="⌁" title={t('patient.prescriptions')} value={summary.activePrescriptions ?? 0} meta={`${t('patient.medications')}: ${summary.activeMedications ?? 0}`} />
      <OverviewCard icon="▥" title={t('patient.labs')} value={summary.pendingLabResults ?? 0} meta={`${t('patient.documents')}: ${summary.documents ?? 0}`} />
     </div><div className="records-section"><div className="section-heading"><div><div className="eyebrow">{t('patient.timeline')}</div><h2>{t('patient.latestEvents')}</h2></div><button className="text-button" type="button" onClick={() => navigate('/patient/medical-history')}>{t('patient.allEvents')} →</button></div>
       <div className="medical-filter-bar"><label className="field-group"><span className="field-label">{t('patient.event')}</span><select value={timelineType} onChange={(event) => setTimelineType(event.target.value)}><option value="ALL">{t('patient.allEvents')}</option><option value="VISIT">{t('patient.visits')}</option><option value="DIAGNOSIS">{t('patient.diagnoses')}</option><option value="PRESCRIPTION">{t('patient.prescriptions')}</option><option value="MEDICATION">{t('patient.medications')}</option><option value="LAB_RESULT">{t('patient.labs')}</option><option value="REFERRAL">{t('patient.referrals')}</option><option value="CERTIFICATE">{t('patient.certificates')}</option><option value="DOCUMENT">{t('patient.documents')}</option></select></label><label className="field-group"><span className="field-label">{t('patient.fromDate')}</span><input type="date" value={timelineFrom} onChange={(event) => setTimelineFrom(event.target.value)} /></label><label className="field-group"><span className="field-label">{t('patient.toDate')}</span><input type="date" value={timelineTo} onChange={(event) => setTimelineTo(event.target.value)} /></label></div>
       {visibleTimeline.length ? <div ref={timelineRef} className="medical-timeline">{visibleTimeline.slice(0, 12).map((item) => <article className="medical-timeline__item" key={item.type + '-' + item.relatedEntityId + '-' + item.date}><span className="medical-timeline__dot" /><div><span className="eyebrow">{timelineLabel(item.type, t)} · {formatTimelineDate(item.date, language, t)}</span><h3>{item.title}</h3>{item.description && <p>{item.description}</p>}</div></article>)}</div> : <EmptyMedicalState title={timeline.length ? t('patient.eventsNotFound') : t('patient.noEvents')} description={timeline.length ? t('patient.changeFilters') : t('patient.afterAppointment')} action={timeline.length ? null : t('patient.bookForDoctor')} onAction={timeline.length ? undefined : () => navigate('/patient/appointments')} />}
    </div></>}{selectedDocument && <PatientDocumentViewer document={selectedDocument} subjectId={selected === 'SELF' ? null : selected} t={t} language={language} onClose={() => setSelectedDocument(null)} />}</section>
}

function timelineLabel(type, t) {
  return { VISIT: t('patient.appointment'), DIAGNOSIS: t('patient.diagnosis'), PRESCRIPTION: t('patient.prescription'), MEDICATION: t('patient.prescriptionMedicine'), LAB_RESULT: t('patient.lab'), REFERRAL: t('patient.referral'), CERTIFICATE: t('patient.certificate'), DOCUMENT: t('patient.document') }[type] || t('patient.event')
}

function formatTimelineDate(value, language, t) {
  return value ? formatDateWithYear(String(value).slice(0, 10), language) : t('patient.dateNotSpecified')
}
