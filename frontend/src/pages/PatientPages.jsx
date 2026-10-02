import { useEffect, useMemo, useState } from 'react'
import {
  ApiError,
  cancelPatientAppointment,
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
  getPatientMedicalRecords,
  getPatientDiagnoses,
  getPatientPrescriptions,
  getPatientMedications,
  getPatientLabResults,
  getPatientCertificates,
  getPatientDocuments,
  getPatientMedicalHistory,
  getPatientProfile,
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

const relationshipOptions = [
  ['CHILD', 'Ребёнок'],
  ['SPOUSE', 'Супруг / супруга'],
  ['PARENT', 'Родитель'],
  ['SIBLING', 'Брат / сестра'],
  ['GUARDIAN', 'Подопечный'],
  ['OTHER', 'Другой подопечный'],
]

function messageFor(error) {
  return error instanceof ApiError ? error.message : error?.message || 'Не удалось загрузить данные'
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

function formatDateKey(dateKey, options = {}) {
  if (!dateKey) return ''
  return new Intl.DateTimeFormat('ru-RU', { timeZone: 'Asia/Qyzylorda', ...options }).format(dateFromKey(dateKey))
}

function formatDateLong(dateKey) {
  return formatDateKey(dateKey, { weekday: 'long', day: 'numeric', month: 'long' })
}

function formatDateWithYear(dateKey) {
  return formatDateKey(dateKey, { day: 'numeric', month: 'long', year: 'numeric' })
}

function formatDateShort(dateKey) {
  return formatDateKey(dateKey, { day: 'numeric', month: 'short' }).replace(/\.$/, '')
}

function formatWeekday(dateKey) {
  return formatDateKey(dateKey, { weekday: 'short' }).replace(/\.$/, '')
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

function timeGroupLabel(time) {
  const hour = Number(String(time).slice(0, 2))
  if (hour < 12) return 'Утро'
  if (hour < 18) return 'День'
  return 'Вечер'
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

function genderLabel(value) {
  return { MALE: 'Мужской', FEMALE: 'Женский', UNKNOWN: 'Не указан' }[value] || 'Не указан'
}

function formatPersonDate(value) {
  if (!value) return 'Не указана'
  const [year, month, day] = String(value).slice(0, 10).split('-').map(Number)
  return Number.isFinite(year) ? new Intl.DateTimeFormat('ru-RU', { day: 'numeric', month: 'long', year: 'numeric' }).format(new Date(year, month - 1, day, 12)) : 'Не указана'
}

function formatPhone(value) {
  const digits = String(value || '').replace(/\D/g, '')
  if (digits.length === 11 && digits.startsWith('7')) return `+7 (${digits.slice(1, 4)}) ${digits.slice(4, 7)}-${digits.slice(7, 9)}-${digits.slice(9)}`
  return value || 'Не указан'
}

function appointmentDateTime(value) {
  const dateKey = String(value || '').slice(0, 10)
  return dateKey ? `${formatDateWithYear(dateKey)} · ${formatSlotTime(value)}` : 'Дата не указана'
}

export function PatientLocationPage() {
  const { refreshUser } = useAuth()
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
      .catch((requestError) => setError(messageFor(requestError)))
      .finally(() => setLoading(false))
  }, [])

  useEffect(() => {
    if (!regionId) { setCities([]); setCityId(''); return undefined }
    getLocationCities(regionId)
      .then((items) => setCities(items))
      .catch((requestError) => setError(messageFor(requestError)))
    return undefined
  }, [regionId])

  const submit = async (event) => {
    event.preventDefault(); setSaving(true); setError('')
    try { await updatePatientLocation({ regionId: Number(regionId), cityId: Number(cityId) }); await refreshUser(); navigate('/patient', { replace: true }) }
    catch (requestError) { setError(messageFor(requestError)) }
    finally { setSaving(false) }
  }

  return <section className="location-page"><div className="location-card"><div className="eyebrow">QamqorMed · профиль пациента</div><h1>Выберите ваш регион</h1><p>Город станет отправной точкой поиска. При записи его можно временно изменить.</p><PageMessage error={error} />{loading ? <p>Загрузка областей…</p> : <form onSubmit={submit}><label className="field-group"><span className="field-label">Область</span><select value={regionId} onChange={(event) => { setRegionId(event.target.value); setCityId('') }} required><option value="">Выберите область…</option>{regions.map((item) => <option value={item.id} key={item.id}>{item.name}</option>)}</select></label><label className="field-group"><span className="field-label">Город</span><select value={cityId} onChange={(event) => setCityId(event.target.value)} required disabled={!regionId}><option value="">Выберите город…</option>{cities.map((item) => <option value={item.id} key={item.id}>{item.name}</option>)}</select></label><button className="button" disabled={saving || !cityId}>{saving ? 'Сохраняем…' : 'Сохранить город'}</button></form>}</div></section>
}

export function PatientProfilePage() {
  const [profile, setProfile] = useState(null)
  const [form, setForm] = useState({})
  const [error, setError] = useState('')
  const [success, setSuccess] = useState('')
  const [saving, setSaving] = useState(false)

  useEffect(() => {
    getPatientProfile().then((data) => { setProfile(data); setForm(data) }).catch((e) => setError(messageFor(e)))
  }, [])

  useEffect(() => {
    if (!success) return undefined
    const timer = window.setTimeout(() => setSuccess(''), 3500)
    return () => window.clearTimeout(timer)
  }, [success])

  const update = (name) => (event) => setForm((current) => ({ ...current, [name]: name === 'iin' ? event.target.value.replace(/\D/g, '').slice(0, 12) : event.target.value }))
  const submit = async (event) => {
    event.preventDefault(); setSaving(true); setError(''); setSuccess('')
    if (form.iin && !/^\d{12}$/.test(form.iin)) { setError('ИИН должен содержать ровно 12 цифр.'); setSaving(false); return }
    try { const data = await updatePatientProfile({ firstName: form.firstName, lastName: form.lastName, middleName: form.middleName, dateOfBirth: form.dateOfBirth || null, gender: form.gender, iin: form.iin, email: form.email }); setProfile(data); setForm(data); setSuccess('Профиль сохранён') } catch (e) { setError(messageFor(e)) } finally { setSaving(false) }
  }

  if (!profile) return <section className="page-shell"><p>Загрузка профиля…</p><PageMessage error={error} /></section>
  const initials = `${profile.firstName?.[0] || ''}${profile.lastName?.[0] || ''}`.toUpperCase()
  const fullNameValue = fullName(profile)
  return <section className="page-shell">
    <div className="page-heading"><div className="eyebrow">Личные настройки</div><h1>Мой профиль</h1><p className="form-description">Персональная и контактная информация. Медицинские данные находятся в отдельной карте.</p></div>
    <PageMessage error={error} />
    <div className="patient-profile-summary"><div className="profile-avatar profile-avatar--large">{initials || '?'}</div><div className="patient-profile-summary__copy"><span className="eyebrow">Профиль пациента</span><h2>{fullNameValue || 'Ваш профиль'}</h2><p>Пациент <span className="profile-active-dot">● Профиль активен</span></p></div><dl className="patient-profile-summary__facts"><div><dt>Возраст</dt><dd>{profile.age ?? '—'}{profile.age != null && ' лет'}</dd></div><div><dt>Пол</dt><dd>{genderLabel(profile.gender)}</dd></div><div><dt>ИИН</dt><dd>{profile.maskedIin || 'Не указан'}</dd></div><div><dt>Телефон</dt><dd>{formatPhone(profile.phone)}</dd></div></dl></div>
    <form className="patient-profile-form" onSubmit={submit}>
      <section className="patient-profile-section"><div className="patient-profile-section__heading"><span className="patient-icon patient-icon--soft">⌁</span><div><h2>Личные данные</h2><p>Как к вам обращаться в медицинской системе.</p></div></div><div className="form-grid"><Field label="Имя" value={form.firstName || ''} onChange={update('firstName')} required /><Field label="Фамилия" value={form.lastName || ''} onChange={update('lastName')} required /><Field label="Отчество" value={form.middleName || ''} onChange={update('middleName')} /><Field label="Дата рождения" type="date" value={form.dateOfBirth || ''} onChange={update('dateOfBirth')} /><label className="field-group"><span className="field-label">Пол</span><select value={form.gender || ''} onChange={update('gender')}><option value="">Не указан</option><option value="MALE">Мужской</option><option value="FEMALE">Женский</option><option value="UNKNOWN">Не указан</option></select></label><Field label="ИИН" value={form.iin || ''} onChange={update('iin')} inputMode="numeric" maxLength={12} pattern="\\d{12}" title="Введите 12 цифр" placeholder="12 цифр" /></div></section>
      <section className="patient-profile-section"><div className="patient-profile-section__heading"><span className="patient-icon patient-icon--soft">◌</span><div><h2>Контактные данные</h2><p>Телефон используется для входа в кабинет.</p></div></div><div className="form-grid"><Field label="Телефон" value={formatPhone(form.phone)} disabled /><Field label="Email" type="email" value={form.email || ''} onChange={update('email')} placeholder="you@example.com" /></div></section>
      <section className="patient-profile-section"><div className="patient-profile-section__heading"><span className="patient-icon patient-icon--soft">⌖</span><div><h2>Медицинское обслуживание</h2><p>Основное направление для поиска врачей и записи.</p></div></div><div className="profile-location-grid"><ProfileFact label="Регион" value={profile.preferredRegion?.name} /><ProfileFact label="Город" value={profile.preferredCity?.name} /><ProfileFact label="Поликлиника" value={profile.clinic?.name} /></div><button className="text-button" type="button" onClick={() => navigate('/patient/location')}>Изменить город →</button></section>
      <div className="form-actions"><button className="button button--compact" disabled={saving}>{saving ? 'Сохраняем…' : 'Сохранить изменения'}</button><button className="button button--secondary button--compact" type="button" onClick={() => { setForm(profile); setError('') }}>Отменить</button></div>
    </form>
    {success && <div className="toast" role="status"><span>✓</span>{success}</div>}
  </section>
}

function ProfileFact({ label, value }) { return <div className="profile-fact"><span>{label}</span><strong>{value || 'Не указано'}</strong></div> }

export function PatientDependentsPage() {
  const [dependents, setDependents] = useState([])
  const [form, setForm] = useState(emptyDependent())
  const [editingId, setEditingId] = useState(null)
  const [openForm, setOpenForm] = useState(false)
  const [error, setError] = useState('')
  const [saving, setSaving] = useState(false)

  const load = () => getDependents().then(setDependents).catch((e) => setError(messageFor(e)))
  useEffect(() => { load() }, [])
  const update = (name) => (event) => setForm((current) => ({ ...current, [name]: event.target.value }))
  const startEdit = (item) => { setEditingId(item.id); setForm({ firstName: item.firstName, lastName: item.lastName, middleName: item.middleName || '', dateOfBirth: item.dateOfBirth, gender: item.gender || '', iin: item.iin || '', relationship: item.relationship, phone: item.phone || '' }); setOpenForm(true) }
  const startCreate = () => { setEditingId(null); setForm(emptyDependent()); setOpenForm(true) }
  const submit = async (event) => {
    event.preventDefault(); setSaving(true); setError('')
    try { if (editingId) await updateDependent(editingId, form); else await createDependent(form); await load(); setOpenForm(false) } catch (e) { setError(messageFor(e)) } finally { setSaving(false) }
  }
  const remove = async (id) => { if (!window.confirm('Скрыть этот профиль из активных подопечных? Медицинская история сохранится.')) return; try { await deleteDependent(id); await load() } catch (e) { setError(messageFor(e)) } }

  return <section className="page-shell">
    <div className="page-heading page-heading--row"><div><div className="eyebrow">Семейные профили</div><h1>Мои подопечные</h1><p className="form-description">Добавляйте профили детей и других людей, которыми вы управляете.</p></div><button className="button" onClick={startCreate}>＋ Добавить подопечного</button></div>
    <PageMessage error={error} />
    {openForm && <form className="form-card dependent-form" onSubmit={submit}><h2>{editingId ? 'Редактировать профиль' : 'Добавить подопечного'}</h2><div className="form-grid"><Field label="Имя" value={form.firstName} onChange={update('firstName')} required /><Field label="Фамилия" value={form.lastName} onChange={update('lastName')} required /><Field label="Отчество" value={form.middleName} onChange={update('middleName')} /><Field label="Дата рождения" type="date" value={form.dateOfBirth} onChange={update('dateOfBirth')} required /><Field label="Пол" value={form.gender} onChange={update('gender')} /><Field label="ИИН" value={form.iin} onChange={update('iin')} inputMode="numeric" maxLength={12} /><label className="field-group"><span className="field-label">Родство</span><select value={form.relationship} onChange={update('relationship')} required><option value="">Выберите…</option>{relationshipOptions.map(([value, label]) => <option value={value} key={value}>{label}</option>)}</select></label><Field label="Телефон" value={form.phone} onChange={update('phone')} /></div><div className="form-actions"><button className="button" disabled={saving}>{saving ? 'Сохраняем…' : 'Сохранить'}</button><button className="button button--secondary" type="button" onClick={() => setOpenForm(false)}>Отмена</button></div></form>}
    {dependents.length === 0 && !openForm && <div className="empty-state"><h2>Подопечных пока нет</h2><p>Создайте профиль ребёнка или другого близкого человека, чтобы записывать его к врачу отдельно от себя.</p><button className="button button--secondary" onClick={startCreate}>Добавить первый профиль</button></div>}
    <div className="profile-card-grid">{dependents.map((item) => <article className="profile-card" key={item.id}><div className="profile-card__top"><div className="profile-avatar">{(item.firstName?.[0] || '?').toUpperCase()}</div><div><span className="eyebrow">{item.relationshipLabel}</span><h2>{item.fullName}</h2><p>{item.age == null ? 'Возраст не указан' : `${item.age} лет`} · {item.dateOfBirth}</p></div></div><div className="profile-card__actions"><button className="button button--compact" onClick={() => navigate(`/patient/dependents/${item.id}`)}>Открыть профиль</button><button className="button button--secondary button--compact" onClick={() => navigate(`/patient/appointments?dependentId=${item.id}`)}>Записать к врачу</button><button className="text-button" onClick={() => startEdit(item)}>Редактировать</button><button className="text-button text-button--danger" onClick={() => remove(item.id)}>Скрыть</button></div></article>)}</div>
  </section>
}

export function DependentDetailPage({ id }) {
  const [dependent, setDependent] = useState(null)
  const [records, setRecords] = useState([])
  const [appointments, setAppointments] = useState([])
  const [error, setError] = useState('')
  useEffect(() => { Promise.all([getDependent(id), getDependentMedicalRecords(id), getPatientAppointments()]).then(([profile, history, allAppointments]) => { setDependent(profile); setRecords(history); setAppointments(allAppointments.filter((item) => item.subject?.type === 'DEPENDENT' && String(item.subject.id) === String(id))) }).catch((e) => setError(messageFor(e))) }, [id])
  if (!dependent) return <section className="page-shell"><PageMessage error={error} /><p>Загрузка профиля…</p></section>
  return <section className="page-shell"><button className="back-link" onClick={() => navigate('/patient/dependents')}>← Мои подопечные</button><div className="profile-hero"><div className="profile-avatar profile-avatar--large">{dependent.firstName?.[0]}</div><div><div className="eyebrow">Профиль подопечного · {dependent.relationshipLabel}</div><h1>{dependent.fullName}</h1><p>{dependent.age ?? '—'} лет · дата рождения {formatPersonDate(dependent.dateOfBirth)}</p></div><button className="button" onClick={() => navigate(`/patient/appointments?dependentId=${id}`)}>Записать к врачу</button></div><div className="dependent-viewing-banner"><span>◉</span><p>Вы просматриваете данные: <strong>{dependent.fullName}</strong></p></div><PageMessage error={error} /><div className="detail-grid"><article className="info-panel"><h2>Данные профиля</h2><p><span>Пол</span><strong>{genderLabel(dependent.gender)}</strong></p><p><span>ИИН</span><strong>{dependent.iin || 'Не указан'}</strong></p><p><span>Телефон</span><strong>{formatPhone(dependent.phone)}</strong></p></article><article className="info-panel"><h2>Ближайшие записи</h2>{appointments.length ? appointments.slice(0, 3).map((item) => <AppointmentRow item={item} key={item.id} />) : <p className="muted-text">Записей пока нет</p>}</article></div><section className="records-section"><div className="section-heading"><div><div className="eyebrow">Только этот профиль</div><h2>Медицинская история — {dependent.firstName}</h2></div></div>{records.length ? records.map((item) => <RecordCard record={item} key={item.id} />) : <div className="empty-state"><h3>История пока пуста</h3><p>Завершённых приёмов пока нет.</p></div>}</section></section>
}

export function PatientBookingPage() {
  const location = useLocation()
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
      .catch((requestError) => setError(messageFor(requestError)))
      .finally(() => setLoading(false))
  }, [])

  useEffect(() => { setForm((current) => ({ ...current, dependentId: selectedDependentId })) }, [selectedDependentId])

  useEffect(() => {
    if (!cityId) { setSpecialties([]); return undefined }
    setSpecialtiesLoading(true); setError('')
    getCitySpecialties(cityId).then(setSpecialties).catch((requestError) => setError(messageFor(requestError))).finally(() => setSpecialtiesLoading(false))
    return undefined
  }, [cityId])

  useEffect(() => {
    if (!cityId || !specialtyId) { setClinics([]); setDoctors([]); return undefined }
    setClinicsLoading(true); setDoctorsLoading(true); setError('')
    const controller = new AbortController()
    Promise.all([searchClinics({ cityId, specialtyId }), searchDoctors({ cityId, specialtyId, clinicId: clinicId || null, query: doctorQuery, page: 0, size: 20 }, controller.signal)])
      .then(([clinicData, doctorPage]) => { setClinics(clinicData); setDoctors(doctorPage.content || []) })
      .catch((requestError) => { if (requestError.name !== 'AbortError') setError(messageFor(requestError)) })
      .finally(() => { setClinicsLoading(false); setDoctorsLoading(false) })
    return () => controller.abort()
  }, [cityId, specialtyId, clinicId, doctorQuery])

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
      .catch((requestError) => { if (requestError.name !== 'AbortError') setScheduleError(requestError instanceof ApiError ? messageFor(requestError) : 'Не удалось загрузить расписание врача.') })
      .finally(() => setSlotsLoading(false))
    return () => controller.abort()
  }, [doctorId, slotsRequestKey])

  const selectedSubject = form.dependentId ? dependents.find((item) => String(item.id) === String(form.dependentId)) : profile
  const filteredSpecialties = specialties.filter((item) => !specialtyQuery || item.name.toLowerCase().includes(specialtyQuery.toLowerCase()))
  const selectedDoctor = doctors.find((item) => String(item.id) === String(doctorId))
  const slotsByDate = useMemo(() => groupSlotsByDate(slots), [slots])
  const availableDates = useMemo(() => Object.keys(slotsByDate).sort(), [slotsByDate])
  const visibleDates = showAllDates ? availableDates : availableDates.slice(0, 6)
  const selectedDateSlots = slotsByDate[selectedDate] || []
  const groupedTimes = selectedDateSlots.reduce((groups, slot) => {
    const label = timeGroupLabel(formatSlotTime(slot.startsAt))
    groups[label] = [...(groups[label] || []), slot]
    return groups
  }, {})
  const calendarDateKeys = useMemo(() => new Set(availableDates), [availableDates])
  const calendarDateCells = useMemo(() => calendarCells(calendarMonth), [calendarMonth])
  const resetScheduleSelection = () => { setSelectedDate(''); setSelectedTime(''); setSelectedSlot(null); setCalendarOpen(false); setShowAllDates(false) }
  const resetFromCity = () => { setSpecialtyId(''); setSpecialtyQuery(''); setClinicId(''); setDoctorId(''); setDoctorQuery(''); resetScheduleSelection(); setClinics([]); setDoctors([]); setSlots([]) }
  const changeRegion = (event) => { const value = event.target.value; setRegionId(value); setCityId(''); resetFromCity(); if (value) getLocationCities(value).then(setCities).catch((requestError) => setError(messageFor(requestError))); else setCities([]) }
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
      const requestMessage = messageFor(requestError)
      setError(requestMessage)
      if (requestMessage.toLowerCase().includes('занят')) {
        resetScheduleSelection()
        setSlotsRequestKey((value) => value + 1)
      }
    } finally { setSaving(false) }
  }

  if (loading) return <section className="page-shell"><p>Загрузка профиля и городов…</p><PageMessage error={error} /></section>
  return <section className="page-shell"><div className="page-heading"><div className="eyebrow">Новая запись</div><h1>Записаться к врачу</h1><p className="form-description">Выберите пациента, город и специальность — список врачей появится только после фильтрации.</p></div><PageMessage error={error} />{success && <div className="success-panel success-panel--booking"><strong>✓ Вы успешно записаны</strong><p>{formatDateWithYear(slotDateKey({ startsAt: success.appointmentAt }))} · {formatSlotTime(success.appointmentAt)}</p><p>Пациент: {success.subject?.fullName}</p><p>Врач: {success.doctor?.fullName}</p><p>Клиника: {success.clinic?.name}</p><div className="form-actions"><button className="button" type="button" onClick={() => navigate('/patient/my-appointments')}>Перейти к моим записям</button><button className="button button--secondary" type="button" onClick={() => setSuccess(null)}>Закрыть</button></div></div>}<form className="booking-form" onSubmit={submit}><section className="booking-step"><div className="step-number">1</div><div className="step-content"><h2>Кого записать?</h2><div className="subject-picker"><SubjectChoice selected={!form.dependentId} onClick={() => setForm((current) => ({ ...current, dependentId: '' }))} title="Я" subtitle={profile ? fullName(profile) : 'Ваш профиль'} />{dependents.map((item) => <SubjectChoice key={item.id} selected={String(form.dependentId) === String(item.id)} onClick={() => setForm((current) => ({ ...current, dependentId: String(item.id) }))} title={item.fullName} subtitle={`${item.relationshipLabel}, ${item.age ?? '—'} лет`} />)}</div><p className="selection-note">Вы записываете: <strong>{selectedSubject ? fullName(selectedSubject) : 'выберите профиль'}</strong></p></div></section><section className="booking-step"><div className="step-number">2</div><div className="step-content"><h2>Город</h2><div className="form-grid"><label className="field-group"><span className="field-label">Область</span><select value={regionId} onChange={changeRegion} required><option value="">Выберите область…</option>{regions.map((item) => <option value={item.id} key={item.id}>{item.name}</option>)}</select></label><label className="field-group"><span className="field-label">Город</span><select value={cityId} onChange={changeCity} disabled={!regionId} required><option value="">Выберите город…</option>{cities.map((item) => <option value={item.id} key={item.id}>{item.name}</option>)}</select></label></div><p className="muted-text">Основной город профиля можно временно изменить только для этой записи.</p></div></section><section className="booking-step"><div className="step-number">3</div><div className="step-content"><h2>Специальность</h2>{specialtiesLoading ? <p>Загрузка специальностей…</p> : !cityId ? <p className="muted-text">Сначала выберите город.</p> : specialties.length === 0 ? <p className="muted-text">В выбранном городе нет доступных специальностей.</p> : <><label className="field-group"><span className="field-label">Поиск специальности</span><input value={specialtyQuery} onChange={(event) => { setSpecialtyQuery(event.target.value); setSpecialtyId(''); setClinicId(''); setDoctorId(''); resetScheduleSelection() }} placeholder="Например, кардиолог" /></label><div className="search-option-list">{filteredSpecialties.slice(0, 8).map((item) => <button type="button" className={`search-option ${String(item.id) === String(specialtyId) ? 'is-selected' : ''}`} key={item.id} onClick={() => chooseSpecialty(item)}>{item.name}</button>)}</div>{specialtyId && <p className="selection-note">Выбрано: <strong>{specialties.find((item) => String(item.id) === String(specialtyId))?.name}</strong></p>}</>}</div></section>{specialtyId && <section className="booking-step"><div className="step-number">4</div><div className="step-content"><h2>Клиника <span className="muted-text">(необязательно)</span></h2>{clinicsLoading ? <p>Загрузка клиник…</p> : <label className="field-group"><span className="field-label">Поликлиника</span><select value={clinicId} onChange={changeClinic}><option value="">Все клиники</option>{clinics.map((item) => <option value={item.id} key={item.id}>{item.name}</option>)}</select></label>}<label className="field-group"><span className="field-label">Поиск врача по имени</span><input value={doctorQuery} onChange={(event) => setDoctorQuery(event.target.value)} placeholder="Введите фамилию или имя" /></label></div></section>}{specialtyId && <section className="booking-step"><div className="step-number">5</div><div className="step-content"><h2>Доступные врачи</h2>{doctorsLoading ? <p>Ищем подходящих врачей…</p> : doctors.length === 0 ? <div className="empty-state"><h3>В выбранном городе сейчас нет доступных врачей этой специальности.</h3><p>Измените специальность или выберите другой город.</p></div> : <div className="doctor-result-grid">{doctors.map((doctor) => <article className={`doctor-result-card ${String(doctor.id) === String(doctorId) ? 'is-selected' : ''}`} key={doctor.id}><div><h3>{doctor.lastName} {doctor.firstName}</h3><p>{doctor.specialty?.name}</p><p>{doctor.clinic?.name} · {doctor.cityName}</p>{doctor.officeNumber && <small>Кабинет {doctor.officeNumber}</small>}</div><button type="button" className="button button--compact" onClick={() => selectDoctor(doctor.id)}>{String(doctor.id) === String(doctorId) ? 'Выбран' : 'Выбрать'}</button></article>)}</div>}</div></section>}{doctorId && <section className="booking-step"><div className="step-number">6</div><div className="step-content"><h2>Свободное время</h2><p className="form-description">Сначала выберите дату, затем доступное время только для этого дня.</p>{slotsLoading ? <div className="schedule-state"><span className="loading-dot" />Загрузка свободного времени…</div> : scheduleError ? <div className="schedule-state schedule-state--error"><p>{scheduleError || 'Не удалось загрузить расписание врача.'}</p><button className="button button--secondary button--compact" type="button" onClick={retrySlots}>Повторить</button></div> : availableDates.length === 0 ? <div className="schedule-state"><p>Свободных дат пока нет.</p><p className="muted-text">Попробуйте выбрать другого врача.</p></div> : <><div className="date-picker-heading"><span className="field-label">Выберите дату</span><button className="date-calendar-toggle" type="button" onClick={() => setCalendarOpen((value) => !value)}>📅 Другие даты</button></div><div className="date-card-grid">{visibleDates.map((dateKey) => <button type="button" className={`date-card ${selectedDate === dateKey ? 'is-selected' : ''}`} key={dateKey} onClick={() => selectDate(dateKey)}><span className="date-card__badge">{dateKey === currentClinicDateKey() ? 'Сегодня' : dateKey === dateKeyFromDate(new Date(dateFromKey(currentClinicDateKey()).getTime() + 86400000)) ? 'Завтра' : capitalized(formatWeekday(dateKey))}</span><strong>{formatDateShort(dateKey)}</strong><small>{slotsByDate[dateKey].length} {slotsByDate[dateKey].length === 1 ? 'место' : slotsByDate[dateKey].length < 5 ? 'места' : 'мест'}</small></button>)}{availableDates.length > 6 && <button className="date-card date-card--more" type="button" onClick={() => { setShowAllDates((value) => !value); setCalendarOpen(false) }}><span>＋</span><strong>{showAllDates ? 'Скрыть даты' : 'Показать ещё'}</strong><small>{showAllDates ? 'Ближайшие даты' : `${availableDates.length - 6} доступно`}</small></button>}</div>{calendarOpen && <div className="date-calendar" aria-label="Календарь доступных дат"><div className="date-calendar__header"><button type="button" className="icon-button" aria-label="Предыдущий месяц" onClick={() => setCalendarMonth((value) => new Date(value.getFullYear(), value.getMonth() - 1, 1, 12))}>‹</button><strong>{capitalized(new Intl.DateTimeFormat('ru-RU', { month: 'long', year: 'numeric' }).format(calendarMonth))}</strong><button type="button" className="icon-button" aria-label="Следующий месяц" onClick={() => setCalendarMonth((value) => new Date(value.getFullYear(), value.getMonth() + 1, 1, 12))}>›</button></div><div className="calendar-weekdays">{['Пн', 'Вт', 'Ср', 'Чт', 'Пт', 'Сб', 'Вс'].map((day) => <span key={day}>{day}</span>)}</div><div className="calendar-grid">{calendarDateCells.map((date, index) => { const key = date ? dateKeyFromDate(date) : `empty-${index}`; const isAvailable = date && calendarDateKeys.has(key); const isPast = date && key < currentClinicDateKey(); return date ? <button type="button" key={key} className={`calendar-day ${selectedDate === key ? 'is-selected' : ''}`} disabled={!isAvailable || isPast} onClick={() => selectDate(key)}>{date.getDate()}</button> : <span className="calendar-day calendar-day--empty" key={key} /> })}</div></div>} {selectedDate && <div className="selected-date-panel"><div><span className="eyebrow">Дата приёма</span><strong>{capitalized(formatDateLong(selectedDate))}</strong></div><button type="button" className="text-button" onClick={() => { setSelectedDate(''); setSelectedTime(''); setSelectedSlot(null) }}>Изменить дату</button></div>}{selectedDate && <div className="time-selection"><h3>Свободное время</h3>{Object.entries(groupedTimes).map(([group, groupSlots]) => <div className="time-group" key={group}><h4>{group}</h4><div className="slot-grid">{groupSlots.map((slot) => <button type="button" key={slot.id} className={`slot-button ${selectedSlot?.id === slot.id ? 'is-selected' : ''}`} onClick={() => selectTime(slot)}>{selectedSlot?.id === slot.id ? `✓ ${formatSlotTime(slot.startsAt)}` : formatSlotTime(slot.startsAt)}</button>)}</div></div>)}{selectedDateSlots.length === 0 && <div className="schedule-state"><p>На эту дату свободного времени нет.</p><button className="button button--secondary button--compact" type="button" onClick={() => setSelectedDate('')}>Выбрать другую дату</button></div>}</div>}{selectedSlot && <BookingSummary subject={selectedSubject} doctor={selectedDoctor} dateKey={selectedDate} time={selectedTime} specialty={specialties.find((item) => String(item.id) === String(specialtyId))?.name} clinic={selectedDoctor?.clinic?.name} />}</>}<label className="field-group"><span className="field-label">Комментарий</span><input value={form.note} onChange={updateNote} placeholder="Необязательно" /></label><button className="button" disabled={saving || !selectedSubject || !doctorId || !selectedDate || !selectedTime || !selectedSlot}>{saving ? 'Создаём запись…' : 'Подтвердить запись'}</button></div></section>}</form></section>
}

export function PatientAppointmentsPage() {
  const [items, setItems] = useState([]); const [dependents, setDependents] = useState([]); const [filter, setFilter] = useState('ALL'); const [error, setError] = useState(''); const [loading, setLoading] = useState(true)
  const load = () => Promise.all([getPatientAppointments(), getDependents()]).then(([appointments, family]) => { setItems(appointments); setDependents(family) }).catch((e) => setError(messageFor(e)))
  useEffect(() => { load().finally(() => setLoading(false)) }, [])
  const filtered = items.filter((item) => filter === 'ALL' || (filter === 'SELF' ? item.subject?.type === 'SELF' : String(item.subject?.id) === String(filter)))
  const cancel = async (id) => { try { await cancelPatientAppointment(id); await load() } catch (e) { setError(messageFor(e)) } }
  const upcoming = filtered.filter((item) => !['COMPLETED', 'CANCELLED', 'NO_SHOW'].includes(item.status))
  const finished = filtered.filter((item) => ['COMPLETED', 'CANCELLED', 'NO_SHOW'].includes(item.status))
  if (loading) return <section className="page-shell"><div className="page-heading"><div className="eyebrow">История обращений</div><h1>Мои записи</h1></div><div className="patient-card patient-skeleton-card"><span className="skeleton skeleton--line" /><span className="skeleton skeleton--title" /><span className="skeleton skeleton--line" /><span className="skeleton skeleton--line" /></div></section>
  return <section className="page-shell"><div className="page-heading page-heading--row"><div><div className="eyebrow">История обращений</div><h1>Мои записи</h1><p className="form-description">Предстоящие записи и история приёмов собраны в одном месте.</p></div><button className="button button--compact" onClick={() => navigate('/patient/appointments')}>＋ Новая запись</button></div><PageMessage error={error} />{!items.length && <EmptyAppointments onAction={() => navigate('/patient/appointments')} />}{items.length > 0 && <><div className="filter-pills"><button className={filter === 'ALL' ? 'is-active' : ''} onClick={() => setFilter('ALL')}>Все</button><button className={filter === 'SELF' ? 'is-active' : ''} onClick={() => setFilter('SELF')}>Мои</button>{dependents.map((item) => <button className={String(filter) === String(item.id) ? 'is-active' : ''} onClick={() => setFilter(String(item.id))} key={item.id}>{item.firstName}</button>)}</div><AppointmentGroup title="Предстоящие" items={upcoming} cancel={cancel} emptyTitle="Предстоящих записей нет" /><AppointmentGroup title="История" items={finished} cancel={cancel} emptyTitle="История записей пока пуста" /></>}</section>
}

function AppointmentGroup({ title, items, cancel, emptyTitle }) { return <section className="records-section"><div className="section-heading"><h2>{title}</h2></div><div className="appointments-list">{items.length ? items.map((item) => <article className="appointment-card" key={item.id}><AppointmentRow item={item} detailed /><div className="appointment-card__actions"><span className={`status status--${String(item.status).toLowerCase()}`}><span>{statusLabel(item.status)}</span></span>{!['CANCELLED', 'COMPLETED', 'NO_SHOW'].includes(item.status) && <button className="text-button text-button--danger" onClick={() => { if (window.confirm('Отменить эту запись?')) cancel(item.id) }}>Отменить запись</button>}</div></article>) : <div className="empty-state empty-state--inline"><p>{emptyTitle || 'Нет записей в этом разделе.'}</p></div>}</div></section> }
function EmptyAppointments({ onAction }) { return <div className="empty-state patient-empty-state"><span className="patient-empty-state__icon">◷</span><h2>У вас пока нет записей</h2><p>Выберите врача и удобное время для консультации.</p><button className="button button--secondary button--compact" type="button" onClick={onAction}>Записаться к врачу</button></div> }

function LegacyPatientMedicalHistoryPage() {
  const location = useLocation()
  const historyOnly = location.pathname === '/patient/medical-history'
  const [profile, setProfile] = useState(null); const [dependents, setDependents] = useState([]); const [selected, setSelected] = useState('SELF'); const [records, setRecords] = useState([]); const [summary, setSummary] = useState({ diagnoses: [], prescriptions: [], labs: [] }); const [error, setError] = useState(''); const [loading, setLoading] = useState(true)
  useEffect(() => { Promise.all([getPatientProfile(), getDependents()]).then(([patient, family]) => { setProfile(patient); setDependents(family) }).catch((e) => setError(messageFor(e))).finally(() => setLoading(false)) }, [])
  useEffect(() => { if (!profile) return; const patientId = selected === 'SELF' ? profile.userId : selected; setLoading(true); Promise.all([getPatientSubjectHistory(patientId), getPatientDiagnoses(patientId), getPatientPrescriptions(patientId), getPatientLabResults(patientId)]).then(([history, diagnoses, prescriptions, labs]) => { setRecords(history); setSummary({ diagnoses, prescriptions, labs }) }).catch((e) => setError(messageFor(e))).finally(() => setLoading(false)) }, [selected, profile])
  const subjectName = selected === 'SELF' ? fullName(profile) : dependents.find((item) => String(item.id) === String(selected))?.fullName
  if (!profile) return <section className="page-shell"><p>Загрузка медицинской карты…</p><PageMessage error={error} /></section>
  return <section className="page-shell medical-record-page"><div className="page-heading"><div className="eyebrow">Медицинская карта</div><h1>{historyOnly ? 'История приёмов' : 'Обзор здоровья'}</h1><p className="form-description">Данные разделены по профилям и доступны только вам и вашим подопечным.</p></div><MedicalTabs active={historyOnly ? 'history' : 'overview'} /><div className="subject-tabs"><button className={selected === 'SELF' ? 'is-active' : ''} onClick={() => setSelected('SELF')}>{fullName(profile) || 'Я'}</button>{dependents.map((item) => <button className={String(selected) === String(item.id) ? 'is-active' : ''} onClick={() => setSelected(String(item.id))} key={item.id}>{item.firstName} · {item.relationshipLabel}</button>)}</div><SubjectContext name={subjectName} />{loading ? <MedicalSkeleton /> : historyOnly ? <div className="records-section">{records.length ? records.map((record) => <VisitCard visit={record} key={record.id} />) : <EmptyMedicalState title="История приёмов пока пуста" description="Завершённые приёмы выбранного профиля появятся здесь." />}</div> : <><div className="medical-overview-grid"><OverviewCard icon="◷" title="Последний приём" value={records[0] ? formatDateWithYear(String(records[0].appointmentAt).slice(0, 10)) : 'Нет данных'} meta={records[0] ? `${records[0].doctor?.fullName || 'Врач'} · ${records[0].clinic?.name || 'Клиника'}` : ''} /><OverviewCard icon="◇" title="Последний диагноз" value={summary.diagnoses[0]?.icd10Code?.name || records[0]?.diagnosisName || 'Диагнозов пока нет'} meta={summary.diagnoses[0]?.icd10Code?.code || ''} /><OverviewCard icon="⌁" title="Активные назначения" value={summary.prescriptions[0]?.title || records[0]?.prescriptions?.[0]?.title || 'Назначений пока нет'} meta={summary.prescriptions.length > 1 ? `Ещё ${summary.prescriptions.length - 1}` : ''} /><OverviewCard icon="▥" title="Последний анализ" value={summary.labs[0]?.testName || 'Результатов анализов пока нет'} meta={summary.labs[0]?.result || summary.labs[0]?.status || ''} /></div><div className="records-section"><div className="section-heading"><div><div className="eyebrow">Подробно</div><h2>Последние приёмы</h2></div><button className="text-button" type="button" onClick={() => navigate('/patient/medical-history')}>Вся история →</button></div>{records.length ? records.slice(0, 3).map((record) => <VisitCard visit={record} key={record.id} />) : <EmptyMedicalState title="Медицинских событий пока нет" description="После завершения приёма здесь появится подробная информация." action="Записаться к врачу" onAction={() => navigate('/patient/appointments')} />}</div></>}</section>
}

function MedicalTabs({ active }) {
  const tabs = [['overview', 'Обзор', '/patient/medical-record'], ['history', 'История приёмов', '/patient/medical-history'], ['diagnoses', 'Диагнозы', '/patient/diagnoses'], ['prescriptions', 'Назначения', '/patient/prescriptions'], ['labs', 'Анализы', '/patient/lab-results'], ['referrals', 'Направления', '/patient/referrals'], ['certificates', 'Справки', '/patient/certificates'], ['documents', 'Документы', '/patient/documents']]
  return <nav className="medical-tabs" aria-label="Разделы медицинской карты">{tabs.map(([key, label, path]) => <button type="button" className={active === key ? 'is-active' : ''} onClick={() => navigate(path)} key={key}>{label}</button>)}</nav>
}

function SubjectContext({ name }) { return name ? <div className="dependent-viewing-banner"><span>◉</span><p>Данные профиля: <strong>{name}</strong></p></div> : null }
function MedicalSkeleton() { return <div className="medical-skeleton"><span className="skeleton skeleton--line" /><span className="skeleton skeleton--title" /><span className="skeleton skeleton--line" /><span className="skeleton skeleton--line" /></div> }
function EmptyMedicalState({ title, description, action, onAction }) { return <div className="empty-state patient-empty-state"><span className="patient-empty-state__icon">▤</span><h3>{title}</h3><p>{description}</p>{action && <button className="button button--secondary button--compact" type="button" onClick={onAction}>{action}</button>}</div> }
function OverviewCard({ icon, title, value, meta }) { return <article className="medical-overview-card"><span className="patient-icon patient-icon--soft">{icon}</span><div><span>{title}</span><strong>{value}</strong>{meta && <small>{meta}</small>}</div></article> }

/*
export function PatientClinicalDataPage({ kind = 'diagnoses' }) {
  const [profile, setProfile] = useState(null)
  const [dependents, setDependents] = useState([])
  const [selected, setSelected] = useState('SELF')
  const [items, setItems] = useState([])
  const [error, setError] = useState('')
  const loaders = { diagnoses: getPatientDiagnoses, prescriptions: getPatientPrescriptions, medications: getPatientMedications, labs: getPatientLabResults, referrals: getPatientReferrals, certificates: getPatientCertificates, documents: getPatientDocuments }
  const titles = { diagnoses: 'Диагнозы', prescriptions: 'Назначения', medications: 'Лекарства', labs: 'Анализы', certificates: 'Справки' }
  const load = loaders[kind] || loaders.diagnoses

  useEffect(() => { Promise.all([getPatientProfile(), getDependents()]).then(([patient, family]) => { setProfile(patient); setDependents(family) }).catch((e) => setError(messageFor(e))) }, [])
  useEffect(() => { if (!profile) return; load(selected === 'SELF' ? profile.userId : selected).then(setItems).catch((e) => setError(messageFor(e))) }, [profile, selected, kind])

  if (!profile) return <section className="page-shell"><p>Загрузка медицинских данных…</p><PageMessage error={error} /></section>
  return <section className="page-shell"><div className="page-heading"><div className="eyebrow">Медицинский кабинет</div><h1>{titles[kind] || titles.diagnoses}</h1><p className="form-description">Данные отображаются отдельно для каждого медицинского получателя.</p></div><PageMessage error={error} /><div className="subject-tabs"><button className={selected === 'SELF' ? 'is-active' : ''} onClick={() => setSelected('SELF')}>{fullName(profile) || 'Я'}</button>{dependents.map((item) => <button className={String(selected) === String(item.id) ? 'is-active' : ''} onClick={() => setSelected(String(item.id))} key={item.id}>{item.firstName} · {item.relationshipLabel}</button>)}</div>{items.length ? <div className="records-section">{items.map((item) => <article className="record-card" key={item.id}>{kind === 'diagnoses' && <><span className="eyebrow">МКБ-10 · {item.icd10Code?.code}</span><h3>{item.icd10Code?.name}</h3>{item.notes && <p>{item.notes}</p>}</>}{kind === 'prescriptions' && <><span className="eyebrow">Назначение</span><h3>{item.title}</h3>{item.description && <p>{item.description}</p>}{item.instructions && <p><strong>Как принимать:</strong> {item.instructions}</p></>}{kind === 'medications' && <><span className="eyebrow">Лекарство</span><h3>{item.medicationName}</h3><p>{[item.dosage, item.frequency, item.duration].filter(Boolean).join(' · ')}</p>{item.instructions && <p>{item.instructions}</p>}</>}</article>)}</div> : <div className="empty-state"><h2>Пока нет данных</h2><p>После завершения приёма информация появится здесь.</p></div>}</section>
}
*/

export function PatientClinicalDataPage({ kind = 'diagnoses' }) {
  const [profile, setProfile] = useState(null)
  const [dependents, setDependents] = useState([])
  const [selected, setSelected] = useState('SELF')
  const [items, setItems] = useState([])
  const [error, setError] = useState(''); const [loading, setLoading] = useState(true)
  const loaders = { diagnoses: getPatientDiagnoses, prescriptions: getPatientPrescriptions, medications: getPatientMedications, labs: getPatientLabResults, certificates: getPatientCertificates }
  const titles = { diagnoses: 'Диагнозы', prescriptions: 'Назначения', medications: 'Лекарства', labs: 'Анализы', referrals: 'Направления', certificates: 'Справки', documents: 'Документы' }

  useEffect(() => {
    Promise.all([getPatientProfile(), getDependents()]).then(([patient, family]) => { setProfile(patient); setDependents(family) }).catch((e) => setError(messageFor(e))).finally(() => setLoading(false))
  }, [])
  useEffect(() => {
    if (!profile) return
    const subjectId = selected === 'SELF' ? null : selected
    setLoading(true)
    const load = (loaders[kind] || loaders.diagnoses)(subjectId)
    load.then(setItems).catch((e) => setError(messageFor(e))).finally(() => setLoading(false))
  }, [profile, selected, kind])

  if (!profile) return <section className="page-shell"><p>Загрузка медицинских данных…</p><PageMessage error={error} /></section>
  const subjectName = selected === 'SELF' ? fullName(profile) : dependents.find((item) => String(item.id) === String(selected))?.fullName
  const emptyCopy = { diagnoses: ['Диагнозов пока нет', 'Диагнозы появятся после завершения приёма.'], prescriptions: ['Назначений пока нет', 'Назначения врача появятся после консультации.'], medications: ['Лекарств пока нет', 'Лекарственные назначения появятся после консультации.'], labs: ['Результатов анализов пока нет', 'Когда врач добавит результаты, они будут доступны здесь.'], referrals: ['Направлений пока нет', 'Направления врача появятся после завершения приёма.'], certificates: ['Справок пока нет', 'Выданные медицинские справки будут отображаться здесь.'], documents: ['Документов пока нет', 'Документы из медицинской карты появятся здесь.'] }[kind] || ['Данных пока нет', 'После завершения приёма информация появится здесь.']
  return <section className="page-shell medical-record-page"><div className="page-heading"><div className="eyebrow">Медицинская карта</div><h1>{titles[kind] || titles.diagnoses}</h1><p className="form-description">Информация отображается отдельно для каждого медицинского получателя.</p></div><MedicalTabs active={kind} /><PageMessage error={error} /><div className="subject-tabs"><button className={selected === 'SELF' ? 'is-active' : ''} onClick={() => setSelected('SELF')}>{fullName(profile) || 'Я'}</button>{dependents.map((item) => <button className={String(selected) === String(item.id) ? 'is-active' : ''} onClick={() => setSelected(String(item.id))} key={item.id}>{item.firstName} · {item.relationshipLabel}</button>)}</div><SubjectContext name={subjectName} />{loading ? <MedicalSkeleton /> : items.length ? <div className="records-section">{items.map((item, index) => <ClinicalDataCard key={`${item.id || 'item'}-${index}`} item={item} kind={kind} />)}</div> : <EmptyMedicalState title={emptyCopy[0]} description={emptyCopy[1]} />}</section>
}

function ClinicalDataCard({ item, kind }) {
  if (kind === 'diagnoses') return <article className="record-card"><span className="eyebrow">МКБ-10 · {item.icd10Code?.code}</span><h3>{item.icd10Code?.name}</h3>{item.notes && <p>{item.notes}</p>}</article>
  if (kind === 'prescriptions') return <article className="record-card"><span className="eyebrow">Назначение</span><h3>{item.title}</h3>{item.description && <p>{item.description}</p>}{item.instructions && <p><strong>Как выполнять:</strong> {item.instructions}</p>}</article>
  if (kind === 'labs') return <article className="record-card"><span className="eyebrow">Анализ · {item.testDate || 'дата не указана'}</span><h3>{item.testName}</h3>{item.status && <p><strong>Статус:</strong> {labStatusLabel(item.status)}</p>}{item.result && <p><strong>Результат:</strong> {item.result}</p>}{item.referenceRange && <p><strong>Норма:</strong> {item.referenceRange}</p>}{item.comment && <p>{item.comment}</p>}{item.doctorName && <small className="muted-text">Назначил: {item.doctorName}</small>}</article>
  if (kind === 'referrals') return <article className="record-card"><span className="eyebrow">Направление</span><h3>{item.referralType || item.type || 'Медицинское направление'}</h3>{item.reason && <p><strong>Причина</strong>{item.reason}</p>}{item.destination && <p><strong>Куда направлено</strong>{item.destination}</p>}{item.visit?.appointmentAt && <small className="muted-text">По итогам приёма {appointmentDateTime(item.visit.appointmentAt)}</small>}</article>
  if (kind === 'certificates') return <article className="record-card"><span className="eyebrow">Справка · {item.issuedAt?.slice(0, 10)}</span><h3>{item.type}</h3>{item.description && <p>{item.description}</p>}{item.validUntil && <p><strong>Действует до:</strong> {item.validUntil}</p>}{item.doctorName && <small className="muted-text">Выдал: {item.doctorName}</small>}</article>
  if (kind === 'documents') return <article className="record-card"><span className="eyebrow">Документ</span><h3>{item.title || item.name || item.type || 'Медицинский документ'}</h3>{item.description && <p>{item.description}</p>}{item.documentType && <p><strong>Тип</strong>{item.documentType}</p>}{item.visit?.appointmentAt && <small className="muted-text">По итогам приёма {appointmentDateTime(item.visit.appointmentAt)}</small>}</article>
  return <article className="record-card"><span className="eyebrow">Лекарство</span><h3>{item.medicationName}</h3><p>{[item.dosage, item.frequency, item.duration].filter(Boolean).join(' · ')}</p>{item.instructions && <p>{item.instructions}</p>}</article>
}

function labStatusLabel(value) { return { READY: 'Готов', COMPLETED: 'Готов', PENDING: 'В обработке', IN_PROGRESS: 'В обработке', CANCELLED: 'Отменён' }[value] || 'Статус результата' }

function Field({ label, type = 'text', value, onChange, required = false, ...props }) { return <label className="field-group"><span className="field-label">{label}</span><input type={type} value={value ?? ''} onChange={onChange} required={required} {...props} /></label> }
function SubjectChoice({ selected, onClick, title, subtitle }) { return <button type="button" className={`subject-choice ${selected ? 'is-selected' : ''}`} onClick={onClick}><span className="subject-choice__radio">{selected ? '✓' : ''}</span><span><strong>{title}</strong><small>{subtitle}</small></span></button> }
function BookingSummary({ subject, doctor, specialty, clinic, dateKey, time }) { return <div className="booking-summary"><div className="booking-summary__heading"><span className="eyebrow">Ваша запись</span><strong>{formatDateWithYear(dateKey)} · {time}</strong></div><dl><div><dt>Пациент</dt><dd>{subject ? fullName(subject) : '—'}{subject?.relationshipLabel && <small>Ребёнок / подопечный · {subject.relationshipLabel}</small>}</dd></div><div><dt>Клиника</dt><dd>{clinic || '—'}</dd></div><div><dt>Специальность</dt><dd>{specialty || '—'}</dd></div><div><dt>Врач</dt><dd>{doctor ? `${doctor.lastName} ${doctor.firstName}` : '—'}</dd></div></dl></div> }
function AppointmentRow({ item, detailed = false }) { return <div className="appointment-row"><div><span className="eyebrow">Пациент</span><strong>{item.subject?.fullName}</strong><small>{item.subject?.type === 'DEPENDENT' ? `${item.subject.relationship || 'Подопечный'} · записал ${item.bookedBy?.fullName}` : 'Вы'}</small></div><div><span className="eyebrow">Врач</span><strong>{item.doctor?.fullName}</strong><small>{item.doctor?.specialty} · {item.clinic?.name}</small></div><div><span className="eyebrow">Дата и время</span><strong>{appointmentDateTime(item.appointmentAt)}</strong>{detailed && <small>{item.clinic?.cityName}</small>}</div></div> }
function RecordCard({ record }) { return <article className="record-card"><div className="record-card__header"><div><span className="eyebrow">{record.createdAt?.slice(0, 10)} · {record.doctor?.specialty}</span><h3>{record.subject?.fullName}</h3></div><span className="record-doctor">{record.doctor?.fullName}</span></div>{[['Диагноз', record.diagnosis], ['Рекомендации', record.recommendations], ['Назначения', record.prescriptions], ['Направления', record.referrals]].filter(([, value]) => value).map(([label, value]) => <p key={label}><strong>{label}</strong>{value}</p>)}</article> }
function VisitCard({ visit }) { return <article className="record-card"><div className="record-card__header"><div><span className="eyebrow">{visit.appointmentAt?.replace('T', ' ')} · {visit.doctor?.specialty}</span><h3>{visit.diagnosisName || visit.diagnosis}</h3><small className="muted-text">{visit.clinic?.name} · завершён {visit.completedAt?.replace('T', ' ')}</small></div><span className="record-doctor">{visit.doctor?.fullName}</span></div><p className="muted-text">Приём #{visit.appointmentId}</p>{[['Диагноз', visit.diagnosis], ['Жалобы', visit.chiefComplaint], ['Анамнез', visit.anamnesis], ['Рекомендации', visit.recommendations], ['Заметки врача', visit.doctorNotes]].filter(([, value]) => value).map(([label, value]) => <p key={label}><strong>{label}</strong>{value}</p>)}<ListForVisit title="Назначения" items={visit.treatments} render={(item) => `${item.name}${item.dosage ? ` · ${item.dosage}` : ''}${item.frequency ? ` · ${item.frequency}` : ''}${item.duration ? ` · ${item.duration}` : ''}`} /><ListForVisit title="Направления" items={visit.referrals} render={(item) => `${item.referralType}${item.reason ? ` · ${item.reason}` : ''}`} /><ListForVisit title="Документы" items={visit.documents} render={(item) => item.title} /></article> }
function ListForVisit({ title, items = [], render }) { return items?.length ? <div className="visit-list"><strong>{title}</strong>{items.map((item) => <span key={item.id}>• {render(item)}</span>)}</div> : null }
function statusLabel(status) { return { BOOKED: 'Записана', CONFIRMED: 'Подтверждена', ARRIVED: 'Пациент прибыл', PREPARING: 'Подготовка', READY: 'Ожидает врача', IN_APPOINTMENT: 'Приём идёт', COMPLETED: 'Завершена', CANCELLED: 'Отменена', NO_SHOW: 'Неявка', SCHEDULED: 'Запланирована', CHECKED_IN: 'Пациент прибыл', NURSE_IN_PROGRESS: 'Подготовка', READY_FOR_DOCTOR: 'Ожидает врача', DOCTOR_IN_PROGRESS: 'Приём идёт', IN_PROGRESS: 'В процессе' }[status] || 'Статус записи' }
function emptyDependent() { return { firstName: '', lastName: '', middleName: '', dateOfBirth: '', gender: '', iin: '', relationship: '', phone: '' } }

export function PatientMedicalHistoryPage() {
  const location = useLocation()
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

  useEffect(() => {
    setLoading(true)
    Promise.all([getPatientProfile(), getDependents()])
      .then(([patient, family]) => { setProfile(patient); setDependents(family) })
      .catch((e) => setError(messageFor(e)))
      .finally(() => setLoading(false))
  }, [reloadKey])

  useEffect(() => {
    if (!profile) return
    const subjectId = selected === 'SELF' ? null : selected
    setLoading(true)
    setError('')
    setData(null)
    getPatientMedicalHistory(subjectId)
      .then(setData)
      .catch((e) => setError(messageFor(e)))
      .finally(() => setLoading(false))
  }, [profile, selected, reloadKey])

  if (!profile) return <section className="page-shell"><p>{loading ? 'Загрузка медицинской карты…' : 'Не удалось загрузить профиль пациента.'}</p><PageMessage error={error} />{error && <button className="button button--secondary" type="button" onClick={() => setReloadKey((value) => value + 1)}>Повторить</button>}</section>
  if (loading && !data) return <section className="page-shell"><p>Загрузка медицинской карты…</p><PageMessage error={error} /></section>
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
  return <section className="page-shell medical-record-page">
    <div className="page-heading"><div className="eyebrow">Медицинская карта</div><h1>{historyOnly ? 'История приёмов' : 'Обзор здоровья'}</h1><p className="form-description">Данные разделены по профилям и доступны только вам и вашим подопечным.</p></div>
    <MedicalTabs active={historyOnly ? 'history' : 'overview'} />
     <PageMessage error={error} />{error && <button className="button button--secondary button--compact" type="button" onClick={() => setReloadKey((value) => value + 1)}>Повторить загрузку</button>}
    <div className="subject-tabs"><button className={selected === 'SELF' ? 'is-active' : ''} onClick={() => setSelected('SELF')}>{fullName(profile) || 'Я'}</button>{dependents.map((item) => <button className={String(selected) === String(item.id) ? 'is-active' : ''} onClick={() => setSelected(String(item.id))} key={item.id}>{item.firstName} · {item.relationshipLabel}</button>)}</div>
    <SubjectContext name={subjectName} />
    {loading ? <MedicalSkeleton /> : historyOnly ? <div className="records-section">{records.length ? records.map((record) => <VisitCard visit={record} key={record.id} />) : <EmptyMedicalState title="История приёмов пока пуста" description="Завершённые приёмы выбранного профиля появятся здесь." />}</div> : <><div className="medical-overview-grid">
      <OverviewCard icon="◷" title="Посещения" value={summary.visits ?? 0} meta={records[0] ? formatDateWithYear(String(records[0].appointmentAt).slice(0, 10)) : 'Завершённых приёмов пока нет'} />
      <OverviewCard icon="◇" title="Диагнозы" value={summary.diagnoses ?? 0} meta={records[0]?.diagnosisName || 'Диагнозов пока нет'} />
      <OverviewCard icon="⌁" title="Активные назначения" value={summary.activePrescriptions ?? 0} meta={'Лекарств: ' + (summary.activeMedications ?? 0)} />
      <OverviewCard icon="▥" title="Ожидаются анализы" value={summary.pendingLabResults ?? 0} meta={'Документов: ' + (summary.documents ?? 0)} />
     </div><div className="records-section"><div className="section-heading"><div><div className="eyebrow">Хронология</div><h2>Последние события</h2></div><button className="text-button" type="button" onClick={() => navigate('/patient/medical-history')}>Вся история →</button></div>
       <div className="medical-filter-bar"><label className="field-group"><span className="field-label">Тип события</span><select value={timelineType} onChange={(event) => setTimelineType(event.target.value)}><option value="ALL">Все события</option><option value="VISIT">Приёмы</option><option value="DIAGNOSIS">Диагнозы</option><option value="PRESCRIPTION">Назначения</option><option value="MEDICATION">Лекарства</option><option value="LAB_RESULT">Анализы</option><option value="REFERRAL">Направления</option><option value="CERTIFICATE">Справки</option><option value="DOCUMENT">Документы</option></select></label><label className="field-group"><span className="field-label">С даты</span><input type="date" value={timelineFrom} onChange={(event) => setTimelineFrom(event.target.value)} /></label><label className="field-group"><span className="field-label">По дату</span><input type="date" value={timelineTo} onChange={(event) => setTimelineTo(event.target.value)} /></label></div>
       {visibleTimeline.length ? <div className="medical-timeline">{visibleTimeline.slice(0, 12).map((item) => <article className="medical-timeline__item" key={item.type + '-' + item.relatedEntityId + '-' + item.date}><span className="medical-timeline__dot" /><div><span className="eyebrow">{timelineLabel(item.type)} · {formatTimelineDate(item.date)}</span><h3>{item.title}</h3>{item.description && <p>{item.description}</p>}</div></article>)}</div> : <EmptyMedicalState title={timeline.length ? 'События не найдены' : 'Медицинских событий пока нет'} description={timeline.length ? 'Измените фильтры периода или типа события.' : 'После завершения приёма здесь появится подробная информация.'} action={timeline.length ? null : 'Записаться к врачу'} onAction={timeline.length ? undefined : () => navigate('/patient/appointments')} />}
    </div></>}</section>
}

function timelineLabel(type) {
  return { VISIT: 'Приём', DIAGNOSIS: 'Диагноз', PRESCRIPTION: 'Назначение', MEDICATION: 'Лекарство', LAB_RESULT: 'Анализ', REFERRAL: 'Направление', CERTIFICATE: 'Справка', DOCUMENT: 'Документ' }[type] || 'Событие'
}

function formatTimelineDate(value) {
  return value ? formatDateWithYear(String(value).slice(0, 10)) : 'Дата не указана'
}
