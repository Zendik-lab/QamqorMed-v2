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
  getPatientProfile,
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

  const update = (name) => (event) => setForm((current) => ({ ...current, [name]: event.target.value }))
  const submit = async (event) => {
    event.preventDefault(); setSaving(true); setError(''); setSuccess('')
    try { const data = await updatePatientProfile({ firstName: form.firstName, lastName: form.lastName, middleName: form.middleName, dateOfBirth: form.dateOfBirth || null, gender: form.gender, iin: form.iin, email: form.email }); setProfile(data); setForm(data); setSuccess('Профиль сохранён') } catch (e) { setError(messageFor(e)) } finally { setSaving(false) }
  }

  if (!profile) return <section className="page-shell"><p>Загрузка профиля…</p><PageMessage error={error} /></section>
  return <section className="page-shell">
    <div className="page-heading"><div className="eyebrow">Мой профиль</div><h1>Профиль: {fullName(profile)}</h1><p className="form-description">Эти данные относятся к вам и не смешиваются с профилями подопечных.</p></div>
    <PageMessage error={error} />{success && <p className="success-message">{success}</p>}
    <form className="form-card patient-form" onSubmit={submit}>
      <div className="form-grid">
        <Field label="Имя" value={form.firstName || ''} onChange={update('firstName')} required />
        <Field label="Фамилия" value={form.lastName || ''} onChange={update('lastName')} required />
        <Field label="Отчество" value={form.middleName || ''} onChange={update('middleName')} />
        <Field label="Дата рождения" type="date" value={form.dateOfBirth || ''} onChange={update('dateOfBirth')} />
        <Field label="Пол" value={form.gender || ''} onChange={update('gender')} placeholder="Например, женский" />
        <Field label="ИИН" value={form.iin || ''} onChange={update('iin')} inputMode="numeric" maxLength={12} />
        <Field label="Телефон" value={form.phone || ''} disabled />
        <Field label="Email" type="email" value={form.email || ''} onChange={update('email')} />
      </div>
      <div className="form-actions"><button className="button" disabled={saving}>{saving ? 'Сохраняем…' : 'Сохранить'}</button><span className="muted-text">Возраст: {profile.age ?? '—'}</span></div>
    </form>
  </section>
}

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
  return <section className="page-shell"><button className="back-link" onClick={() => navigate('/patient/dependents')}>← Мои подопечные</button><div className="profile-hero"><div className="profile-avatar profile-avatar--large">{dependent.firstName?.[0]}</div><div><div className="eyebrow">Профиль подопечного · {dependent.relationshipLabel}</div><h1>{dependent.fullName}</h1><p>{dependent.age ?? '—'} лет · дата рождения {dependent.dateOfBirth}</p></div><button className="button" onClick={() => navigate(`/patient/appointments?dependentId=${id}`)}>Записать к врачу</button></div><PageMessage error={error} /><div className="detail-grid"><article className="info-panel"><h2>Данные профиля</h2><p><span>Пол</span><strong>{dependent.gender || 'Не указан'}</strong></p><p><span>ИИН</span><strong>{dependent.iin || 'Не указан'}</strong></p><p><span>Телефон</span><strong>{dependent.phone || 'Не указан'}</strong></p></article><article className="info-panel"><h2>Ближайшие записи</h2>{appointments.length ? appointments.slice(0, 3).map((item) => <AppointmentRow item={item} key={item.id} />) : <p className="muted-text">Записей пока нет</p>}</article></div><section className="records-section"><div className="section-heading"><div><div className="eyebrow">Только этот профиль</div><h2>Медицинская история — {dependent.firstName}</h2></div></div>{records.length ? records.map((item) => <RecordCard record={item} key={item.id} />) : <div className="empty-state"><p>Завершённых приёмов пока нет.</p></div>}</section></section>
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
  const [timeSlotId, setTimeSlotId] = useState('')
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
    setSlotsLoading(true); setTimeSlotId('')
    getDoctorTimeSlots(doctorId, {}, controller.signal).then(setSlots).catch((requestError) => { if (requestError.name !== 'AbortError') setError(messageFor(requestError)) }).finally(() => setSlotsLoading(false))
    return () => controller.abort()
  }, [doctorId])

  const selectedSubject = form.dependentId ? dependents.find((item) => String(item.id) === String(form.dependentId)) : profile
  const filteredSpecialties = specialties.filter((item) => !specialtyQuery || item.name.toLowerCase().includes(specialtyQuery.toLowerCase()))
  const selectedSlot = slots.find((item) => String(item.id) === String(timeSlotId))
  const selectedDoctor = doctors.find((item) => String(item.id) === String(doctorId))
  const resetFromCity = () => { setSpecialtyId(''); setSpecialtyQuery(''); setClinicId(''); setDoctorId(''); setDoctorQuery(''); setTimeSlotId(''); setClinics([]); setDoctors([]); setSlots([]) }
  const changeRegion = (event) => { const value = event.target.value; setRegionId(value); setCityId(''); resetFromCity(); if (value) getLocationCities(value).then(setCities).catch((requestError) => setError(messageFor(requestError))); else setCities([]) }
  const changeCity = (event) => { setCityId(event.target.value); resetFromCity() }
  const chooseSpecialty = (item) => { setSpecialtyId(String(item.id)); setSpecialtyQuery(item.name); setClinicId(''); setDoctorId(''); setTimeSlotId('') }
  const changeClinic = (event) => { setClinicId(event.target.value); setDoctorId(''); setTimeSlotId('') }
  const selectDoctor = (id) => { setDoctorId(String(id)); setTimeSlotId('') }
  const updateNote = (event) => setForm((current) => ({ ...current, note: event.target.value }))
  const submit = async (event) => {
    event.preventDefault(); setSaving(true); setError(''); setSuccess(null)
    try {
      const result = await createPatientAppointment({ doctorId: Number(doctorId), dependentId: form.dependentId ? Number(form.dependentId) : null, appointmentAt: selectedSlot.startsAt, cityId: Number(cityId), specialtyId: Number(specialtyId), clinicId: clinicId ? Number(clinicId) : null, timeSlotId: Number(timeSlotId), note: form.note })
      setSuccess(result); setTimeSlotId(''); setDoctorId('')
    } catch (requestError) { setError(messageFor(requestError)) } finally { setSaving(false) }
  }

  if (loading) return <section className="page-shell"><p>Загрузка профиля и городов…</p><PageMessage error={error} /></section>
  return <section className="page-shell"><div className="page-heading"><div className="eyebrow">Новая запись</div><h1>Записаться к врачу</h1><p className="form-description">Выберите пациента, город и специальность — список врачей появится только после фильтрации.</p></div><PageMessage error={error} />{success && <div className="success-panel"><strong>Запись создана</strong><p>Пациент: {success.subject.fullName}</p><p>{success.appointmentAt?.replace('T', ' ')} · {success.doctor.fullName}</p><button className="button button--secondary" onClick={() => navigate('/patient/my-appointments')}>Открыть мои записи</button></div>}<form className="booking-form" onSubmit={submit}><section className="booking-step"><div className="step-number">1</div><div className="step-content"><h2>Кого записать?</h2><div className="subject-picker"><SubjectChoice selected={!form.dependentId} onClick={() => setForm((current) => ({ ...current, dependentId: '' }))} title="Я" subtitle={profile ? fullName(profile) : 'Ваш профиль'} />{dependents.map((item) => <SubjectChoice key={item.id} selected={String(form.dependentId) === String(item.id)} onClick={() => setForm((current) => ({ ...current, dependentId: String(item.id) }))} title={item.fullName} subtitle={`${item.relationshipLabel}, ${item.age ?? '—'} лет`} />)}</div><p className="selection-note">Вы записываете: <strong>{selectedSubject ? fullName(selectedSubject) : 'выберите профиль'}</strong></p></div></section><section className="booking-step"><div className="step-number">2</div><div className="step-content"><h2>Город</h2><div className="form-grid"><label className="field-group"><span className="field-label">Область</span><select value={regionId} onChange={changeRegion} required><option value="">Выберите область…</option>{regions.map((item) => <option value={item.id} key={item.id}>{item.name}</option>)}</select></label><label className="field-group"><span className="field-label">Город</span><select value={cityId} onChange={changeCity} disabled={!regionId} required><option value="">Выберите город…</option>{cities.map((item) => <option value={item.id} key={item.id}>{item.name}</option>)}</select></label></div><p className="muted-text">Основной город профиля можно временно изменить только для этой записи.</p></div></section><section className="booking-step"><div className="step-number">3</div><div className="step-content"><h2>Специальность</h2>{specialtiesLoading ? <p>Загрузка специальностей…</p> : !cityId ? <p className="muted-text">Сначала выберите город.</p> : specialties.length === 0 ? <p className="muted-text">В выбранном городе нет доступных специальностей.</p> : <><label className="field-group"><span className="field-label">Поиск специальности</span><input value={specialtyQuery} onChange={(event) => { setSpecialtyQuery(event.target.value); setSpecialtyId('') }} placeholder="Например, кардиолог" /></label><div className="search-option-list">{filteredSpecialties.slice(0, 8).map((item) => <button type="button" className={`search-option ${String(item.id) === String(specialtyId) ? 'is-selected' : ''}`} key={item.id} onClick={() => chooseSpecialty(item)}>{item.name}</button>)}</div>{specialtyId && <p className="selection-note">Выбрано: <strong>{specialties.find((item) => String(item.id) === String(specialtyId))?.name}</strong></p>}</>}</div></section>{specialtyId && <section className="booking-step"><div className="step-number">4</div><div className="step-content"><h2>Клиника <span className="muted-text">(необязательно)</span></h2>{clinicsLoading ? <p>Загрузка клиник…</p> : <label className="field-group"><span className="field-label">Поликлиника</span><select value={clinicId} onChange={changeClinic}><option value="">Все клиники</option>{clinics.map((item) => <option value={item.id} key={item.id}>{item.name}</option>)}</select></label>}<label className="field-group"><span className="field-label">Поиск врача по имени</span><input value={doctorQuery} onChange={(event) => setDoctorQuery(event.target.value)} placeholder="Введите фамилию или имя" /></label></div></section>}{specialtyId && <section className="booking-step"><div className="step-number">5</div><div className="step-content"><h2>Доступные врачи</h2>{doctorsLoading ? <p>Ищем подходящих врачей…</p> : doctors.length === 0 ? <div className="empty-state"><h3>В выбранном городе сейчас нет доступных врачей этой специальности.</h3><p>Измените специальность или выберите другой город.</p></div> : <div className="doctor-result-grid">{doctors.map((doctor) => <article className={`doctor-result-card ${String(doctor.id) === String(doctorId) ? 'is-selected' : ''}`} key={doctor.id}><div><h3>{doctor.lastName} {doctor.firstName}</h3><p>{doctor.specialty?.name}</p><p>{doctor.clinic?.name} · {doctor.cityName}</p>{doctor.officeNumber && <small>Кабинет {doctor.officeNumber}</small>}</div><button type="button" className="button button--compact" onClick={() => selectDoctor(doctor.id)}>{String(doctor.id) === String(doctorId) ? 'Выбран' : 'Выбрать'}</button></article>)}</div>}</div></section>}{doctorId && <section className="booking-step"><div className="step-number">6</div><div className="step-content"><h2>Свободное время</h2>{slotsLoading ? <p>Загрузка расписания…</p> : slots.length === 0 ? <p className="muted-text">Свободных слотов пока нет.</p> : <div className="slot-grid">{slots.map((slot) => <button type="button" key={slot.id} className={`slot-button ${String(slot.id) === String(timeSlotId) ? 'is-selected' : ''}`} onClick={() => setTimeSlotId(String(slot.id))}>{slot.startsAt.replace('T', ' ')}</button>)}</div>}<label className="field-group"><span className="field-label">Комментарий</span><input value={form.note} onChange={updateNote} placeholder="Необязательно" /></label><button className="button" disabled={saving || !selectedSubject || !timeSlotId}>{saving ? 'Создаём запись…' : 'Подтвердить запись'}</button></div></section>}</form></section>
}

export function PatientAppointmentsPage() {
  const [items, setItems] = useState([]); const [dependents, setDependents] = useState([]); const [filter, setFilter] = useState('ALL'); const [error, setError] = useState('')
  const load = () => Promise.all([getPatientAppointments(), getDependents()]).then(([appointments, family]) => { setItems(appointments); setDependents(family) }).catch((e) => setError(messageFor(e)))
  useEffect(() => { load() }, [])
  const filtered = items.filter((item) => filter === 'ALL' || (filter === 'SELF' ? item.subject?.type === 'SELF' : String(item.subject?.id) === String(filter)))
  const cancel = async (id) => { try { await cancelPatientAppointment(id); await load() } catch (e) { setError(messageFor(e)) } }
  return <section className="page-shell"><div className="page-heading page-heading--row"><div><div className="eyebrow">История обращений</div><h1>Мои записи</h1><p className="form-description">Каждая запись показывает реального пациента, а не только владельца аккаунта.</p></div><button className="button" onClick={() => navigate('/patient/appointments')}>＋ Новая запись</button></div><PageMessage error={error} /><div className="filter-pills"><button className={filter === 'ALL' ? 'is-active' : ''} onClick={() => setFilter('ALL')}>Все</button><button className={filter === 'SELF' ? 'is-active' : ''} onClick={() => setFilter('SELF')}>Мои</button>{dependents.map((item) => <button className={String(filter) === String(item.id) ? 'is-active' : ''} onClick={() => setFilter(String(item.id))} key={item.id}>{item.firstName}</button>)}</div><div className="appointments-list">{filtered.length ? filtered.map((item) => <article className="appointment-card" key={item.id}><AppointmentRow item={item} detailed /><div className="appointment-card__actions"><span className={`status status--${item.status.toLowerCase()}`}>{statusLabel(item.status)}</span>{!['CANCELLED', 'COMPLETED'].includes(item.status) && <button className="text-button text-button--danger" onClick={() => cancel(item.id)}>Отменить</button>}</div></article>) : <div className="empty-state"><h2>Записей нет</h2><p>Создайте запись для себя или одного из подопечных.</p></div>}</div></section>
}

export function PatientMedicalHistoryPage() {
  const [profile, setProfile] = useState(null); const [dependents, setDependents] = useState([]); const [selected, setSelected] = useState('SELF'); const [records, setRecords] = useState([]); const [error, setError] = useState('')
  useEffect(() => { Promise.all([getPatientProfile(), getDependents()]).then(([patient, family]) => { setProfile(patient); setDependents(family) }).catch((e) => setError(messageFor(e))) }, [])
  useEffect(() => { if (!profile) return; const task = selected === 'SELF' ? getPatientMedicalRecords() : getDependentMedicalRecords(selected); task.then(setRecords).catch((e) => setError(messageFor(e))) }, [selected, profile])
  const subjectName = selected === 'SELF' ? fullName(profile) : dependents.find((item) => String(item.id) === String(selected))?.fullName
  return <section className="page-shell"><div className="page-heading"><div className="eyebrow">Медицинские данные</div><h1>Медицинская история — {subjectName || '…'}</h1><p className="form-description">Выберите профиль, чтобы открыть только его приёмы, диагнозы и рекомендации.</p></div><PageMessage error={error} /><div className="subject-tabs"><button className={selected === 'SELF' ? 'is-active' : ''} onClick={() => setSelected('SELF')}>{fullName(profile) || 'Я'}</button>{dependents.map((item) => <button className={String(selected) === String(item.id) ? 'is-active' : ''} onClick={() => setSelected(String(item.id))} key={item.id}>{item.firstName} · {item.relationshipLabel}</button>)}</div><div className="records-section">{records.length ? records.map((record) => <RecordCard record={record} key={record.id} />) : <div className="empty-state"><h2>История пока пуста</h2><p>Завершённые приёмы выбранного профиля появятся здесь.</p></div>}</div></section>
}

function Field({ label, type = 'text', value, onChange, required = false, ...props }) { return <label className="field-group"><span className="field-label">{label}</span><input type={type} value={value ?? ''} onChange={onChange} required={required} {...props} /></label> }
function SubjectChoice({ selected, onClick, title, subtitle }) { return <button type="button" className={`subject-choice ${selected ? 'is-selected' : ''}`} onClick={onClick}><span className="subject-choice__radio">{selected ? '✓' : ''}</span><span><strong>{title}</strong><small>{subtitle}</small></span></button> }
function AppointmentRow({ item, detailed = false }) { return <div className="appointment-row"><div><span className="eyebrow">Пациент</span><strong>{item.subject?.fullName}</strong><small>{item.subject?.type === 'DEPENDENT' ? `${item.subject.relationship || 'Подопечный'} · записал ${item.bookedBy?.fullName}` : 'Вы'}</small></div><div><span className="eyebrow">Врач</span><strong>{item.doctor?.fullName}</strong><small>{item.doctor?.specialty} · {item.clinic?.name}</small></div><div><span className="eyebrow">Дата и время</span><strong>{item.appointmentAt?.replace('T', ' ')}</strong>{detailed && <small>{item.clinic?.cityName}</small>}</div></div> }
function RecordCard({ record }) { return <article className="record-card"><div className="record-card__header"><div><span className="eyebrow">{record.createdAt?.slice(0, 10)} · {record.doctor?.specialty}</span><h3>{record.subject?.fullName}</h3></div><span className="record-doctor">{record.doctor?.fullName}</span></div>{[['Диагноз', record.diagnosis], ['Рекомендации', record.recommendations], ['Назначения', record.prescriptions], ['Направления', record.referrals]].filter(([, value]) => value).map(([label, value]) => <p key={label}><strong>{label}</strong>{value}</p>)}</article> }
function statusLabel(status) { return { SCHEDULED: 'Запланирована', CONFIRMED: 'Подтверждена', IN_PROGRESS: 'В процессе', COMPLETED: 'Завершена', CANCELLED: 'Отменена', NO_SHOW: 'Не состоялась' }[status] || status }
function emptyDependent() { return { firstName: '', lastName: '', middleName: '', dateOfBirth: '', gender: '', iin: '', relationship: '', phone: '' } }
