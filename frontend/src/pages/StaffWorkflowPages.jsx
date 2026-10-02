import { useEffect, useState } from 'react'
import {
  ApiError,
  addDoctorDiagnosis,
  addDoctorMedication,
  addDoctorDocument,
  addDoctorPrescription,
  addDoctorReferral,
  completeDoctorAppointment,
  getDoctorAppointmentDetails,
  getDoctorAppointments,
  getIcd10Codes,
  getNurseAppointmentDetails,
  getNurseAppointments,
  readyNurseAppointment,
  startDoctorAppointment,
  startNurseAssessment,
  updateDoctorVisit,
  updateNurseAssessment,
} from '../api/apiClient.js'

const emptyAssessment = { temperature: '', bloodPressureSystolic: '', bloodPressureDiastolic: '', pulse: '', oxygenSaturation: '', height: '', weight: '', complaintSummary: '', nurseNotes: '' }
const emptyVisit = { chiefComplaint: '', anamnesis: '', diagnosis: '', diagnosisCode: '', diagnosisName: '', doctorNotes: '', recommendations: '' }
const emptyTreatment = { name: '', dosage: '', frequency: '', duration: '', instructions: '' }
const emptyMedication = { medicationName: '', dosage: '', frequency: '', duration: '', instructions: '', startDate: '', endDate: '' }
const emptyReferral = { referralType: 'SPECIALIST', targetSpecialtyId: '', targetClinicId: '', reason: '', notes: '' }
const emptyDocument = { type: 'CONCLUSION', title: '', description: '', fileUrl: '' }

function messageFor(error) { return error instanceof ApiError ? error.message : error?.message || 'Не удалось загрузить приёмы' }

export default function StaffWorkflowPage({ role }) {
  const [items, setItems] = useState([])
  const [filters, setFilters] = useState({ date: '', status: '', query: '' })
  const [selectedId, setSelectedId] = useState(null)
  const [details, setDetails] = useState(null)
  const [assessment, setAssessment] = useState(emptyAssessment)
  const [visit, setVisit] = useState(emptyVisit)
  const [treatment, setTreatment] = useState(emptyTreatment)
  const [medication, setMedication] = useState(emptyMedication)
  const [referral, setReferral] = useState(emptyReferral)
  const [document, setDocument] = useState(emptyDocument)
  const [icd10Codes, setIcd10Codes] = useState([])
  const [error, setError] = useState('')
  const [notice, setNotice] = useState('')
  const [busy, setBusy] = useState(false)

  useEffect(() => {
    const requestedId = new URLSearchParams(window.location.search).get('appointmentId')
    if (requestedId && items.length) {
      const item = items.find((value) => String(value.id) === String(requestedId))
      if (item) openAppointment(item)
    }
  }, [items])

  const load = () => {
    const fetcher = role === 'DOCTOR' ? getDoctorAppointments : getNurseAppointments
    fetcher(filters).then(setItems).catch((requestError) => setError(messageFor(requestError)))
  }

  useEffect(() => { load() }, [role, filters.date, filters.status, filters.query])

  const openAppointment = async (item) => {
    setSelectedId(item.id); setDetails(null); setError(''); setNotice('')
    try {
      const result = await (role === 'DOCTOR' ? getDoctorAppointmentDetails(item.id) : getNurseAppointmentDetails(item.id))
      setDetails(result)
      if (role === 'DOCTOR') getIcd10Codes('').then(setIcd10Codes).catch(() => {})
      if (role === 'DOCTOR') setVisit({ ...emptyVisit, ...(result.medicalVisit || {}) })
      else setAssessment({ ...emptyAssessment, ...(result.nurseAssessment || {}) })
    } catch (requestError) { setError(messageFor(requestError)) }
  }

  const reloadDetails = async () => {
    if (!selectedId) return
    const result = await (role === 'DOCTOR' ? getDoctorAppointmentDetails(selectedId) : getNurseAppointmentDetails(selectedId))
    setDetails(result)
    if (role === 'DOCTOR') setVisit({ ...emptyVisit, ...(result.medicalVisit || {}) })
    else setAssessment({ ...emptyAssessment, ...(result.nurseAssessment || {}) })
    load()
  }

  const run = async (action, successMessage) => {
    setBusy(true); setError(''); setNotice('')
    try { await action(); await reloadDetails(); setNotice(successMessage) }
    catch (requestError) { setError(messageFor(requestError)) }
    finally { setBusy(false) }
  }

  const updateFilter = (name) => (event) => setFilters((current) => ({ ...current, [name]: event.target.value }))
  const status = canonicalWorkflowStatus(details?.status)
  const readOnly = status === 'COMPLETED' || status === 'CANCELLED' || status === 'NO_SHOW'

  return <section className="page-shell workflow-page">
    <div className="page-heading"><div className="eyebrow">Медицинский workflow · {role === 'DOCTOR' ? 'врач' : 'медсестра'}</div><h1>{role === 'DOCTOR' ? 'Сегодняшние приёмы' : 'Пациенты у медсестры'}</h1><p className="form-description">Показываются только записи {role === 'DOCTOR' ? 'вашего профиля врача' : 'закреплённого врача'}. Данные обновляются после каждого действия.</p></div>
    <div className="workflow-filters"><Field label="Дата" type="date" value={filters.date} onChange={updateFilter('date')} /><label className="field-group"><span className="field-label">Статус</span><select value={filters.status} onChange={updateFilter('status')}><option value="">Все статусы</option>{statusOptions.map(([value, label]) => <option value={value} key={value}>{label}</option>)}</select></label><Field label="Поиск по ФИО" value={filters.query} onChange={updateFilter('query')} placeholder="Например, Иванов" /></div>
    {error && <p className="error-message" role="alert">{error}</p>}{notice && <p className="success-message">{notice}</p>}
    <div className="workflow-layout"><div className="staff-appointments">{items.length ? items.map((item) => <AppointmentCard key={item.id} item={item} selected={selectedId === item.id} onOpen={() => openAppointment(item)} />) : <div className="empty-state"><h2>Записей нет</h2><p>Новые записи появятся здесь после бронирования пациентом.</p></div>}</div>{details && <section className="workflow-detail"><DetailHeader details={details} role={role} /><PatientPanel details={details} />{role === 'NURSE' ? <NursePanel details={details} form={assessment} setForm={setAssessment} readOnly={readOnly} busy={busy} run={run} /> : <DoctorPanel details={details} form={visit} setForm={setVisit} icd10Codes={icd10Codes} treatment={treatment} setTreatment={setTreatment} medication={medication} setMedication={setMedication} referral={referral} setReferral={setReferral} document={document} setDocument={setDocument} readOnly={readOnly} busy={busy} run={run} />}</section>}</div>
  </section>
}

function AppointmentCard({ item, selected, onOpen }) {
  return <article className={`staff-appointment ${selected ? 'staff-appointment--selected' : ''}`}><div className="staff-appointment__subject"><div className="profile-avatar">{item.subject?.fullName?.[0] || '?'}</div><div><span className="eyebrow">{item.subject?.type === 'DEPENDENT' ? 'Ребёнок / подопечный' : 'Пациент'}</span><h2>{item.subject?.fullName}</h2><p>{item.subject?.age == null ? 'Возраст не указан' : `${item.subject.age} лет`} · {item.subject?.type === 'DEPENDENT' ? `записал ${item.bookedBy?.fullName}` : 'самостоятельная запись'}</p></div></div><div className="staff-appointment__meta"><div><span>Время</span><strong>{formatDateTime(item.appointmentAt)}</strong></div><div><span>Врач</span><strong>{item.doctor?.fullName}</strong></div><div><span>Клиника</span><strong>{item.clinic?.name}</strong></div><div><span>Состояние</span><StatusBadge value={item.status} /></div></div><button className="button button--secondary button--compact" type="button" onClick={onOpen}>{selected ? 'Обновить карточку' : 'Открыть приём'}</button></article>
}

function DetailHeader({ details, role }) {
  return <div className="workflow-detail__header"><div><div className="eyebrow">Запись #{details.id} · {role === 'DOCTOR' ? 'врачебный этап' : 'предварительный осмотр'}</div><h2>{details.subject?.fullName}</h2><p>{formatDateTime(details.appointmentAt)} · {details.doctor?.specialty} · {details.clinic?.name}</p></div><StatusBadge value={details.status} /></div>
}

function PatientPanel({ details }) {
  const subject = details.subject || {}
  return <div className="workflow-patient-card"><strong>Пациент</strong><span>{subject.fullName} · {subject.age == null ? 'возраст не указан' : `${subject.age} лет`}</span><small>Дата рождения: {subject.dateOfBirth || '—'} · Пол: {subject.gender || '—'}</small>{subject.type === 'DEPENDENT' && <small>Владелец записи: {details.bookedBy?.fullName}</small>}</div>
}

function NursePanel({ details, form, setForm, readOnly, busy, run }) {
  const update = (name) => (event) => setForm((current) => ({ ...current, [name]: event.target.value }))
  const status = canonicalWorkflowStatus(details.status)
  const canStart = ['BOOKED', 'CONFIRMED', 'ARRIVED'].includes(status)
  const canEdit = status === 'PREPARING'
  return <div className="workflow-stage"><div className="stage-heading"><div><span className="eyebrow">Данные медсестры</span><h3>Предварительный осмотр</h3></div>{canStart && <button className="button" type="button" disabled={busy} onClick={() => run(() => startNurseAssessment(details.id), 'Осмотр начат')}>Начать осмотр</button>}</div><div className="form-grid"><Field label="Температура" type="number" step="0.1" value={form.temperature} onChange={update('temperature')} disabled={!canEdit || readOnly} /><Field label="Систолическое давление" type="number" value={form.bloodPressureSystolic} onChange={update('bloodPressureSystolic')} disabled={!canEdit || readOnly} /><Field label="Диастолическое давление" type="number" value={form.bloodPressureDiastolic} onChange={update('bloodPressureDiastolic')} disabled={!canEdit || readOnly} /><Field label="Пульс" type="number" value={form.pulse} onChange={update('pulse')} disabled={!canEdit || readOnly} /><Field label="Сатурация" type="number" value={form.oxygenSaturation} onChange={update('oxygenSaturation')} disabled={!canEdit || readOnly} /><Field label="Рост, см" type="number" step="0.1" value={form.height} onChange={update('height')} disabled={!canEdit || readOnly} /><Field label="Вес, кг" type="number" step="0.1" value={form.weight} onChange={update('weight')} disabled={!canEdit || readOnly} /></div><TextArea label="Краткие жалобы" value={form.complaintSummary} onChange={update('complaintSummary')} disabled={!canEdit || readOnly} /><TextArea label="Комментарий медсестры" value={form.nurseNotes} onChange={update('nurseNotes')} disabled={!canEdit || readOnly} /><div className="form-actions">{canEdit && <><button className="button button--secondary" type="button" disabled={busy} onClick={() => run(() => updateNurseAssessment(details.id, numericAssessment(form)), 'Показатели сохранены')}>Сохранить</button><button className="button" type="button" disabled={busy} onClick={() => run(async () => { await updateNurseAssessment(details.id, numericAssessment(form)); await readyNurseAppointment(details.id) }, 'Пациент передан врачу')}>Передать врачу</button></>}</div></div>
}

function DoctorPanel({ details, form, setForm, icd10Codes, treatment, setTreatment, medication, setMedication, referral, setReferral, document, setDocument, readOnly, busy, run }) {
  const update = (name) => (event) => setForm((current) => ({ ...current, [name]: event.target.value }))
  const updateTreatment = (name) => (event) => setTreatment((current) => ({ ...current, [name]: event.target.value }))
  const updateMedication = (name) => (event) => setMedication((current) => ({ ...current, [name]: event.target.value }))
  const updateReferral = (name) => (event) => setReferral((current) => ({ ...current, [name]: event.target.value }))
  const updateDocument = (name) => (event) => setDocument((current) => ({ ...current, [name]: event.target.value }))
  const status = canonicalWorkflowStatus(details.status)
  const canStart = status === 'READY'
  const canEdit = status === 'IN_APPOINTMENT'
  const saved = details.medicalVisit
  const selectedCode = icd10Codes.find((item) => item.code === form.diagnosisCode)
   return <div className="workflow-stage"><div className="stage-heading"><div><span className="eyebrow">Медицинская карта</span><h3>Приём врача</h3></div>{canStart && <button className="button" type="button" disabled={busy} onClick={() => run(() => startDoctorAppointment(details.id), 'Приём начат')}>Начать приём</button>}</div>{details.nurseAssessment && <div className="assessment-summary"><strong>Данные медсестры</strong><span>Температура: {details.nurseAssessment.temperature || '—'} · Давление: {pressure(details.nurseAssessment)} · Пульс: {details.nurseAssessment.pulse || '—'} · SpO₂: {details.nurseAssessment.oxygenSaturation || '—'}</span><small>Жалобы: {details.nurseAssessment.complaintSummary || '—'} · {details.nurseAssessment.nurseNotes || ''}</small></div>}{saved && !canEdit && <ReadOnlyVisit visit={saved} />}{canEdit && <><div className="form-grid"><TextArea label="Жалобы пациента" value={form.chiefComplaint} onChange={update('chiefComplaint')} /><TextArea label="Анамнез" value={form.anamnesis} onChange={update('anamnesis')} /><Field label="Диагноз" value={form.diagnosis} onChange={update('diagnosis')} required /><label className="field-group"><span className="field-label">МКБ-10</span><select value={form.diagnosisCode || ''} onChange={(event) => { const code = icd10Codes.find((item) => item.code === event.target.value); setForm((current) => ({ ...current, diagnosisCode: event.target.value, diagnosisName: code?.name || current.diagnosisName })) }}><option value="">Выберите код</option>{icd10Codes.map((item) => <option value={item.code} key={item.id}>{item.code} — {item.name}</option>)}</select></label><Field label="Название диагноза" value={form.diagnosisName} onChange={update('diagnosisName')} /></div><TextArea label="Заметки врача" value={form.doctorNotes} onChange={update('doctorNotes')} /><TextArea label="Рекомендации" value={form.recommendations} onChange={update('recommendations')} /><button className="button button--secondary" type="button" disabled={busy} onClick={() => run(() => updateDoctorVisit(details.id, form), 'Данные приёма сохранены')}>Сохранить приём</button><AddTreatment form={treatment} update={updateTreatment} disabled={busy} onAdd={() => run(async () => { await updateDoctorVisit(details.id, form); await addDoctorPrescription(details.id, treatment); setTreatment({ ...emptyTreatment }) }, 'Назначение добавлено')} /><AddMedication form={medication} update={updateMedication} disabled={busy} onAdd={() => run(async () => { await addDoctorMedication(details.id, medication); setMedication({ ...emptyMedication }) }, 'Лекарство добавлено')} /><AddReferral form={referral} update={updateReferral} disabled={busy} onAdd={() => run(async () => { await addDoctorReferral(details.id, referral); setReferral({ ...emptyReferral }) }, 'Направление добавлено')} /><AddDocument form={document} update={updateDocument} disabled={busy} onAdd={() => run(async () => { await addDoctorDocument(details.id, document); setDocument({ ...emptyDocument }) }, 'Документ добавлен')} /><div className="form-actions"><button className="button" type="button" disabled={busy || !form.diagnosis} onClick={() => run(async () => { await updateDoctorVisit(details.id, form); await completeDoctorAppointment(details.id) }, 'Приём завершён')}>Завершить приём</button></div></>}</div>
}

function ReadOnlyVisit({ visit }) {
  return <div className="visit-result"><h4>Результат приёма</h4>{[['Диагноз', visit.diagnosis], ['Жалобы', visit.chiefComplaint], ['Анамнез', visit.anamnesis], ['Рекомендации', visit.recommendations], ['Заметки врача', visit.doctorNotes]].filter(([, value]) => value).map(([label, value]) => <p key={label}><strong>{label}</strong>{value}</p>)}<ListBlock title="Назначения" items={visit.treatments} render={(item) => `${item.name}${item.dosage ? ` · ${item.dosage}` : ''}${item.frequency ? ` · ${item.frequency}` : ''}`} /><ListBlock title="Направления" items={visit.referrals} render={(item) => `${item.referralType}${item.reason ? ` · ${item.reason}` : ''}`} /><ListBlock title="Документы" items={visit.documents} render={(item) => item.title} /></div>
}

function AddTreatment({ form, update, disabled, onAdd }) { return <div className="workflow-subform"><h4>Назначение лечения</h4><div className="form-grid"><Field label="Препарат / назначение" value={form.name} onChange={update('name')} /><Field label="Дозировка" value={form.dosage} onChange={update('dosage')} /><Field label="Частота" value={form.frequency} onChange={update('frequency')} /><Field label="Длительность" value={form.duration} onChange={update('duration')} /></div><Field label="Инструкции" value={form.instructions} onChange={update('instructions')} /><button className="button button--secondary" type="button" disabled={disabled || !form.name} onClick={onAdd}>＋ Добавить препарат</button></div> }
function AddMedication({ form, update, disabled, onAdd }) { return <div className="workflow-subform"><h4>Лекарство</h4><div className="form-grid"><Field label="Название" value={form.medicationName} onChange={update('medicationName')} /><Field label="Дозировка" value={form.dosage} onChange={update('dosage')} /><Field label="Частота" value={form.frequency} onChange={update('frequency')} /><Field label="Длительность" value={form.duration} onChange={update('duration')} /></div><div className="form-grid"><Field label="Начало" type="date" value={form.startDate} onChange={update('startDate')} /><Field label="Окончание" type="date" value={form.endDate} onChange={update('endDate')} /></div><Field label="Инструкции" value={form.instructions} onChange={update('instructions')} /><button className="button button--secondary" type="button" disabled={disabled || !form.medicationName} onClick={onAdd}>＋ Добавить лекарство</button></div> }
function AddReferral({ form, update, disabled, onAdd }) { return <div className="workflow-subform"><h4>Направление</h4><div className="form-grid"><label className="field-group"><span className="field-label">Тип</span><select value={form.referralType} onChange={update('referralType')}><option value="SPECIALIST">К специалисту</option><option value="LABORATORY">Лаборатория</option><option value="DIAGNOSTIC">Диагностика</option><option value="HOSPITAL">Стационар</option><option value="OTHER">Другое</option></select></label><Field label="Причина" value={form.reason} onChange={update('reason')} /></div><TextArea label="Заметки" value={form.notes} onChange={update('notes')} /><button className="button button--secondary" type="button" disabled={disabled} onClick={onAdd}>＋ Добавить направление</button></div> }
function AddDocument({ form, update, disabled, onAdd }) { return <div className="workflow-subform"><h4>Медицинский документ</h4><div className="form-grid"><label className="field-group"><span className="field-label">Тип</span><select value={form.type} onChange={update('type')}><option value="CONCLUSION">Заключение</option><option value="CERTIFICATE">Справка</option><option value="PRESCRIPTION">Рецепт</option><option value="REFERRAL">Направление</option><option value="LAB_REQUEST">Лабораторный запрос</option><option value="OTHER">Другое</option></select></label><Field label="Название" value={form.title} onChange={update('title')} /></div><TextArea label="Описание" value={form.description} onChange={update('description')} /><Field label="Ссылка на файл (опционально)" value={form.fileUrl} onChange={update('fileUrl')} /><button className="button button--secondary" type="button" disabled={disabled || !form.title} onClick={onAdd}>＋ Добавить документ</button></div> }
function ListBlock({ title, items = [], render }) { return items?.length ? <div className="visit-list"><strong>{title}</strong>{items.map((item) => <span key={item.id}>• {render(item)}</span>)}</div> : null }
function Field({ label, value, onChange, type = 'text', step, disabled = false, required = false, placeholder }) { return <label className="field-group"><span className="field-label">{label}</span><input type={type} step={step} value={value ?? ''} onChange={onChange} disabled={disabled} required={required} placeholder={placeholder} /></label> }
function TextArea({ label, value, onChange, disabled = false }) { return <label className="field-group"><span className="field-label">{label}</span><textarea value={value ?? ''} onChange={onChange} disabled={disabled} rows="3" /></label> }
function numericAssessment(form) { return Object.fromEntries(Object.entries(form).map(([key, value]) => ['temperature', 'bloodPressureSystolic', 'bloodPressureDiastolic', 'pulse', 'oxygenSaturation', 'height', 'weight'].includes(key) && value !== '' ? [key, Number(value)] : [key, value])) }
function pressure(value) { return value.bloodPressureSystolic || value.bloodPressureDiastolic ? `${value.bloodPressureSystolic || '—'}/${value.bloodPressureDiastolic || '—'}` : value.bloodPressure || '—' }
function formatDateTime(value) { return value ? value.replace('T', ' ') : '—' }
function StatusBadge({ value }) { return <span className={`status status--${String(value || '').toLowerCase()}`}>{statusLabel(value)}</span> }
function canonicalWorkflowStatus(value) { return { SCHEDULED: 'BOOKED', CHECKED_IN: 'ARRIVED', NURSE_IN_PROGRESS: 'PREPARING', READY_FOR_DOCTOR: 'READY', DOCTOR_IN_PROGRESS: 'IN_APPOINTMENT', IN_PROGRESS: 'IN_APPOINTMENT' }[value] || value }
function statusLabel(value) { return { BOOKED: 'Записан', CONFIRMED: 'Подтверждён', ARRIVED: 'Прибыл', PREPARING: 'Подготовка', READY: 'Готов к приёму', IN_APPOINTMENT: 'На приёме', COMPLETED: 'Завершён', CANCELLED: 'Отменён', NO_SHOW: 'Не явился', NURSE_IN_PROGRESS: 'Подготовка', READY_FOR_DOCTOR: 'Готов к приёму', DOCTOR_IN_PROGRESS: 'На приёме' }[value] || 'Статус записи' }
const statusOptions = [['BOOKED', 'Записан'], ['CONFIRMED', 'Подтверждён'], ['ARRIVED', 'Прибыл'], ['PREPARING', 'У медсестры'], ['READY', 'Ожидает врача'], ['IN_APPOINTMENT', 'Приём идёт'], ['COMPLETED', 'Завершён'], ['CANCELLED', 'Отменён'], ['NO_SHOW', 'Не явился']]
