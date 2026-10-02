import { useEffect, useState } from 'react'
import { ApiError, getDoctorPatientDetail, getDoctorPatientDirectory } from '../api/apiClient.js'
import { navigate } from '../utils/navigation.js'

const statusLabels = {
  BOOKED: 'Запланирован', CONFIRMED: 'Подтверждён', ARRIVED: 'Прибыл', PREPARING: 'Подготовка',
  READY: 'Ожидает врача', IN_APPOINTMENT: 'На приёме', COMPLETED: 'Завершён', CANCELLED: 'Отменён', NO_SHOW: 'Неявка',
  SCHEDULED: 'Запланирован', CHECKED_IN: 'Прибыл', NURSE_IN_PROGRESS: 'Подготовка', READY_FOR_DOCTOR: 'Ожидает врача',
  DOCTOR_IN_PROGRESS: 'На приёме', IN_PROGRESS: 'На приёме',
}

const tabs = [
  ['today', 'Сегодня'], ['upcoming', 'Предстоящие'], ['history', 'История'], ['patients', 'Все мои пациенты'],
]

function messageFor(error) { return error instanceof ApiError ? error.message : error?.message || 'Не удалось загрузить пациентов' }
function statusLabel(value) { return statusLabels[value] || 'Статус записи' }
function statusClass(value) { return String(value || '').toLowerCase().replaceAll('_', '-') }
function dateKey(value) { return String(value || '').slice(0, 10) }
function timeLabel(value) { return String(value || '').slice(11, 16) || '—' }
function dateLabel(value, options = { day: 'numeric', month: 'long', year: 'numeric' }) {
  const key = dateKey(value)
  if (!key) return '—'
  const [year, month, day] = key.split('-').map(Number)
  return new Intl.DateTimeFormat('ru-RU', options).format(new Date(year, month - 1, day, 12))
}
function initials(subject) { return `${subject?.fullName?.split(' ')?.[1]?.[0] || subject?.fullName?.[0] || ''}${subject?.fullName?.split(' ')?.[0]?.[0] || ''}`.toUpperCase() }
function subjectType(subject) { return subject?.type === 'DEPENDENT' ? 'Подопечный' : 'Пациент' }
function genderLabel(value) { return { MALE: 'Мужской', FEMALE: 'Женский', OTHER: 'Другой' }[value] || 'Не указан' }
function emptyText(tab) { return { today: 'На сегодня пациентов нет', upcoming: 'Предстоящих приёмов нет', history: 'Завершённых приёмов пока нет', patients: 'Пациенты не найдены' }[tab] }

export function DoctorPatientsPage() {
  const [data, setData] = useState(null)
  const [activeTab, setActiveTab] = useState('today')
  const [filters, setFilters] = useState({ query: '', status: '', date: '', period: '', type: '' })
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')

  useEffect(() => {
    setLoading(true); setError('')
    getDoctorPatientDirectory(filters)
      .then(setData)
      .catch((requestError) => setError(messageFor(requestError)))
      .finally(() => setLoading(false))
  }, [filters.query, filters.status, filters.date, filters.period, filters.type])

  const updateFilter = (name) => (event) => setFilters((current) => ({ ...current, [name]: event.target.value }))
  const items = data?.[activeTab] || []

  return <section className="page-shell doctor-patients-page">
    <div className="page-heading"><div className="eyebrow">Рабочий кабинет врача</div><h1>Пациенты</h1><p className="form-description">Ваши пациенты, предстоящие приёмы и медицинская история</p></div>
    <div className="doctor-patients-filters">
      <label className="field-group doctor-patients-search"><span className="field-label">Поиск пациента</span><input value={filters.query} onChange={updateFilter('query')} placeholder="ФИО или ИИН" /></label>
      <label className="field-group"><span className="field-label">Статус</span><select value={filters.status} onChange={updateFilter('status')}><option value="">Все статусы</option>{Object.entries(statusLabels).filter(([key]) => !['SCHEDULED', 'CHECKED_IN', 'NURSE_IN_PROGRESS', 'READY_FOR_DOCTOR', 'DOCTOR_IN_PROGRESS', 'IN_PROGRESS'].includes(key)).map(([value, label]) => <option value={value} key={value}>{label}</option>)}</select></label>
      <label className="field-group"><span className="field-label">Дата</span><input type="date" value={filters.date} onChange={updateFilter('date')} /></label>
      <label className="field-group"><span className="field-label">Период</span><select value={filters.period} onChange={updateFilter('period')}><option value="">Любой период</option><option value="TODAY">Сегодня</option><option value="WEEK">Ближайшие 7 дней</option><option value="MONTH">Ближайшие 30 дней</option></select></label>
      <label className="field-group"><span className="field-label">Тип пациента</span><select value={filters.type} onChange={updateFilter('type')}><option value="">Все</option><option value="SELF">Сам пациент</option><option value="DEPENDENT">Подопечные</option></select></label>
    </div>
    {error && <p className="error-message" role="alert">{error}</p>}
    <nav className="doctor-patient-tabs" aria-label="Разделы пациентов">{tabs.map(([key, label]) => <button className={activeTab === key ? 'is-active' : ''} type="button" onClick={() => setActiveTab(key)} key={key}>{label}{data && <span>{data[key]?.length || 0}</span>}</button>)}</nav>
    {loading ? <DoctorPatientsSkeleton /> : items.length ? <div className={activeTab === 'patients' ? 'doctor-patient-grid' : 'doctor-appointment-list'}>{items.map((item) => activeTab === 'patients' ? <PatientSummaryCard patient={item} key={`${item.subject?.type}-${item.subject?.id}`} /> : <DoctorAppointmentCard appointment={item} key={item.id} />)}</div> : <div className="empty-state doctor-patient-empty"><span className="doctor-patient-empty__icon">♧</span><h2>{emptyText(activeTab)}</h2><p>{activeTab === 'patients' ? 'Здесь появятся только пациенты, связанные с вашими записями и приёмами.' : 'Когда появится запись, она будет отображаться в этом разделе.'}</p></div>}
  </section>
}

function DoctorPatientsSkeleton() {
  return <div className="doctor-patient-grid">{[1, 2, 3].map((item) => <article className="doctor-patient-card doctor-patient-skeleton" key={item}><span className="skeleton skeleton--avatar" /><span className="skeleton skeleton--title" /><span className="skeleton skeleton--line" /><span className="skeleton skeleton--line" /></article>)}</div>
}

function DoctorAppointmentCard({ appointment }) {
  const subject = appointment.subject || {}
  const openPatient = () => navigate(`/doctor/patients/${subject.id}?type=${subject.type}`)
  const openAppointment = () => navigate(`/doctor/appointments?appointmentId=${appointment.id}`)
  const finished = ['COMPLETED', 'CANCELLED', 'NO_SHOW'].includes(appointment.status)
  const actionLabel = appointment.status === 'COMPLETED' ? 'Открыть результат' : appointment.status === 'IN_APPOINTMENT' ? 'Продолжить приём' : appointment.status === 'READY' ? 'Начать приём' : 'Открыть приём'
  return <article className="doctor-appointment-card">
    <div className="doctor-appointment-card__top"><div className="doctor-appointment-card__time"><strong>{timeLabel(appointment.appointmentAt)}</strong><span>{dateLabel(appointment.appointmentAt, { day: 'numeric', month: 'short', year: 'numeric' })}</span></div><div className="doctor-appointment-card__subject"><div className="profile-avatar">{initials(subject)}</div><div><span className="eyebrow">{subjectType(subject)}{subject.type === 'DEPENDENT' && appointment.bookedBy?.fullName ? ` · записал ${appointment.bookedBy.fullName}` : ''}</span><h2>{subject.fullName || 'Пациент'}</h2><p>{subject.age == null ? 'Возраст не указан' : `${subject.age} лет`} · {genderLabel(subject.gender)}</p></div></div><span className={`status status--${statusClass(appointment.status)}`}>{statusLabel(appointment.status)}</span></div>
    <div className="doctor-appointment-card__meta"><div><span>Причина обращения</span><strong>{appointment.note || 'Не указана'}</strong></div><div><span>Специальность</span><strong>{appointment.doctor?.specialty || '—'}</strong></div><div><span>Клиника</span><strong>{appointment.clinic?.name || '—'}</strong></div></div>
    {appointment.nurseAssessment ? <NurseAssessmentSummary assessment={appointment.nurseAssessment} /> : <p className="doctor-patient-muted">Подготовка медсестры ещё не заполнена</p>}
    <div className="doctor-appointment-card__actions"><button className="button button--secondary button--compact" type="button" onClick={openPatient}>Открыть карту</button>{!finished && <button className="button button--compact" type="button" onClick={openAppointment}>{actionLabel}</button>}{finished && <button className="button button--secondary button--compact" type="button" onClick={openAppointment}>{actionLabel}</button>}</div>
  </article>
}

function NurseAssessmentSummary({ assessment }) {
  const values = [['Давление', assessment.bloodPressureSystolic && assessment.bloodPressureDiastolic ? `${assessment.bloodPressureSystolic}/${assessment.bloodPressureDiastolic}` : assessment.bloodPressure], ['Температура', assessment.temperature ? `${assessment.temperature} °C` : null], ['Пульс', assessment.pulse ? `${assessment.pulse}` : null], ['Вес', assessment.weight ? `${assessment.weight} кг` : null], ['Рост', assessment.height ? `${assessment.height} см` : null]].filter(([, value]) => value)
  return <div className="doctor-nurse-summary"><div className="doctor-nurse-summary__heading"><strong>Подготовлен медсестрой</strong><span className="status status--active">Есть данные медсестры</span></div>{values.length > 0 && <div className="doctor-nurse-summary__values">{values.map(([label, value]) => <span key={label}><small>{label}</small><strong>{value}</strong></span>)}</div>}{(assessment.complaintSummary || assessment.nurseNotes) && <p><strong>Жалобы:</strong> {assessment.complaintSummary || '—'}{assessment.nurseNotes && <><br /><strong>Заметки:</strong> {assessment.nurseNotes}</>}</p>}</div>
}

function PatientSummaryCard({ patient }) {
  const subject = patient.subject || {}
  return <article className="doctor-patient-card"><div className="doctor-patient-card__top"><div className="profile-avatar">{initials(subject)}</div><div><span className="eyebrow">{subjectType(subject)}</span><h2>{subject.fullName || 'Пациент'}</h2><p>{subject.age == null ? 'Возраст не указан' : `${subject.age} лет`} · {genderLabel(subject.gender)}</p></div></div>{subject.type === 'DEPENDENT' && patient.ownerFullName && <p className="doctor-patient-owner">Подопечный аккаунта: <strong>{patient.ownerFullName}</strong></p>}<dl className="doctor-patient-stats"><div><dt>Последний приём</dt><dd>{dateLabel(patient.lastAppointmentDate)}</dd></div><div><dt>Следующий приём</dt><dd>{dateLabel(patient.nextAppointmentDate)}</dd></div><div><dt>Всего приёмов</dt><dd>{patient.appointmentCount || 0}</dd></div></dl>{patient.lastDiagnosis && <p className="doctor-patient-diagnosis"><span>Последний диагноз</span><strong>{patient.lastDiagnosis}</strong></p>}<button className="button button--secondary button--compact" type="button" onClick={() => navigate(`/doctor/patients/${subject.id}?type=${subject.type}`)}>Открыть карточку</button></article>
}

export function DoctorPatientDetailPage({ id }) {
  const [data, setData] = useState(null)
  const [tab, setTab] = useState('overview')
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const type = new URLSearchParams(window.location.search).get('type') || undefined

  useEffect(() => {
    setLoading(true); setError('')
    getDoctorPatientDetail(id, type).then(setData).catch((requestError) => setError(messageFor(requestError))).finally(() => setLoading(false))
  }, [id, type])

  if (loading) return <section className="page-shell doctor-patient-detail-page"><DoctorPatientsSkeleton /></section>
  if (error || !data) return <section className="page-shell doctor-patient-detail-page"><button className="back-link" type="button" onClick={() => navigate('/doctor/patients')}>← К пациентам</button><p className="error-message" role="alert">{error || 'Пациент не найден'}</p></section>
  const subject = data.patient?.subject || {}
  const current = data.currentAppointment
  const visits = data.visits || []
  const tabItems = medicalTabItems(tab, visits, data.labResults, data.certificates)
  return <section className="page-shell doctor-patient-detail-page">
    <button className="back-link" type="button" onClick={() => navigate('/doctor/patients')}>← К пациентам</button>
    <div className="doctor-patient-hero"><div className="profile-avatar profile-avatar--large">{initials(subject)}</div><div><span className="eyebrow">{subjectType(subject)}</span><h1>{subject.fullName}</h1><p>{subject.age == null ? 'Возраст не указан' : `${subject.age} лет`} · {genderLabel(subject.gender)} · {subject.dateOfBirth ? `родился ${dateLabel(subject.dateOfBirth)}` : 'дата рождения не указана'}</p>{subject.type === 'DEPENDENT' && data.patient.ownerFullName && <strong>Подопечный аккаунта: {data.patient.ownerFullName}</strong>}</div><span className="status status--active">Связан с вами</span></div>
    <div className="doctor-patient-contact"><div><span>ИИН</span><strong>{subject.maskedIin || 'Не указан'}</strong></div><div><span>Телефон</span><strong>{data.patient.phone || 'Не указан'}</strong></div><div><span>Email</span><strong>{data.patient.email || 'Не указан'}</strong></div></div>
    {current && <CurrentAppointment appointment={current} />}
    <nav className="medical-tabs doctor-medical-tabs" aria-label="Разделы медицинской карты">{[['overview', 'Обзор'], ['visits', 'Приёмы'], ['diagnoses', 'Диагнозы'], ['prescriptions', 'Назначения'], ['labs', 'Анализы'], ['referrals', 'Направления'], ['documents', 'Документы']].map(([key, label]) => <button className={tab === key ? 'is-active' : ''} type="button" onClick={() => setTab(key)} key={key}>{label}</button>)}</nav>
    {tab === 'overview' ? <PatientOverview visits={visits} current={current} labResults={data.labResults} /> : tabItems.length ? <div className="doctor-detail-list">{tab === 'visits' ? tabItems.map((visit) => <DoctorVisitCard visit={visit} key={visit.id} />) : tabItems.map((item, index) => <ClinicalItem item={item} tab={tab} key={`${item.id || item.title || item.name || index}-${index}`} />)}</div> : <div className="empty-state doctor-patient-empty"><h2>{tabEmptyText(tab)}</h2><p>Данные появятся после завершения соответствующего этапа медицинского workflow.</p></div>}
  </section>
}

function CurrentAppointment({ appointment }) {
  const open = () => navigate(`/doctor/appointments?appointmentId=${appointment.id}`)
  const label = appointment.status === 'IN_APPOINTMENT' ? 'Продолжить приём' : appointment.status === 'COMPLETED' ? 'Открыть результат' : 'Начать приём'
  return <section className="doctor-current-appointment"><div><span className="eyebrow">Текущий приём</span><h2>{dateLabel(appointment.appointmentAt)} · {timeLabel(appointment.appointmentAt)}</h2><p>{appointment.note || 'Причина обращения не указана'} · <span className={`status status--${statusClass(appointment.status)}`}>{statusLabel(appointment.status)}</span></p></div>{appointment.nurseAssessment && <NurseAssessmentSummary assessment={appointment.nurseAssessment} />}<button className="button" type="button" onClick={open}>{label}</button></section>
}

function PatientOverview({ visits, current, labResults = [] }) {
  const latest = visits[0]
  const diagnosis = latest?.diagnosisName || latest?.diagnosis
  const treatment = latest?.treatments?.[0]?.name || latest?.prescriptions?.[0]?.title || latest?.medications?.[0]?.medicationName
  return <div className="doctor-detail-overview"><div className="medical-overview-grid"><OverviewCard title="Последний приём" value={latest ? dateLabel(latest.appointmentAt) : 'Нет данных'} meta={latest?.doctor?.fullName || ''} /><OverviewCard title="Последний диагноз" value={diagnosis || 'Диагнозов пока нет'} meta={latest?.diagnosisCode || ''} /><OverviewCard title="Активные назначения" value={treatment || 'Назначений пока нет'} meta={latest?.treatments?.length > 1 ? `Ещё ${latest.treatments.length - 1}` : ''} /><OverviewCard title="Последние анализы" value={labResults[0]?.testName || 'Результатов анализов пока нет'} meta={labResults[0]?.result || ''} /></div>{current && <div className="doctor-overview-note"><strong>Сегодняшняя запись открыта выше.</strong><span>После завершения приёма диагнозы, назначения и документы появятся в соответствующих вкладках.</span></div>}{latest ? <DoctorVisitCard visit={latest} compact /> : <div className="empty-state doctor-patient-empty"><h2>Медицинская история пока пуста</h2><p>Завершённые приёмы этого пациента у вас появятся здесь.</p></div>}</div>
}

function OverviewCard({ title, value, meta }) { return <article className="medical-overview-card"><div><span>{title}</span><strong>{value}</strong>{meta && <small>{meta}</small>}</div></article> }

function DoctorVisitCard({ visit, compact = false }) {
  return <article className={`record-card doctor-visit-card ${compact ? 'doctor-visit-card--compact' : ''}`}><div className="record-card__header"><div><span className="eyebrow">{dateLabel(visit.appointmentAt)} · {visit.doctor?.specialty || 'Приём'}</span><h3>{visit.diagnosisName || visit.diagnosis || 'Приём без диагноза'}</h3><small className="muted-text">{visit.doctor?.fullName || 'Врач'} · {visit.clinic?.name || 'Клиника'} · <span className={`status status--${statusClass(visit.appointmentStatus)}`}>{statusLabel(visit.appointmentStatus)}</span></small></div><button className="button button--secondary button--compact" type="button" onClick={() => navigate(`/doctor/appointments?appointmentId=${visit.appointmentId}`)}>Подробнее</button></div>{!compact && <div className="doctor-visit-fields">{[['Жалобы', visit.chiefComplaint], ['Диагноз', visit.diagnosis], ['Назначения', visit.treatments?.map((item) => [item.name, item.dosage, item.frequency, item.duration].filter(Boolean).join(' · ')).join('; ')], ['Рекомендации', visit.recommendations]].filter(([, value]) => value).map(([label, value]) => <p key={label}><strong>{label}</strong>{value}</p>)}</div>}</article>
}

function medicalTabItems(tab, visits, labResults = [], certificates = []) {
  const visitById = new Map(visits.map((visit) => [visit.id, visit]))
  if (tab === 'visits') return visits
  if (tab === 'diagnoses') return visits.flatMap((visit) => visit.diagnoses?.length ? visit.diagnoses.map((item) => ({ ...item, visit })) : visit.diagnosis ? [{ title: visit.diagnosisName || visit.diagnosis, description: visit.diagnosisCode, visit }] : [])
  if (tab === 'prescriptions') return visits.flatMap((visit) => [...(visit.treatments || []), ...(visit.prescriptions || []), ...(visit.medications || [])].map((item) => ({ ...item, visit })))
  if (tab === 'referrals') return visits.flatMap((visit) => (visit.referrals || []).map((item) => ({ ...item, visit })))
  if (tab === 'labs') return labResults.map((item) => ({ ...item, visit: visitById.get(item.visitId) }))
  if (tab === 'documents') return [...visits.flatMap((visit) => (visit.documents || []).map((item) => ({ ...item, visit }))), ...certificates.map((item) => ({ ...item, visit: visitById.get(item.visitId) }))]
  return []
}

function ClinicalItem({ item, tab }) {
  const title = item.title || item.name || item.medicationName || item.icd10Code?.name || item.referralType || item.type || 'Медицинская запись'
  const detail = tab === 'prescriptions' ? [item.dosage, item.frequency, item.duration, item.instructions, item.description].filter(Boolean).join(' · ') : tab === 'labs' ? [item.status, item.result, item.referenceRange, item.comment].filter(Boolean).join(' · ') : item.notes || item.description || item.reason || item.icd10Code?.code
  return <article className="record-card doctor-clinical-item"><div className="record-card__header"><div><span className="eyebrow">{tabLabel(tab)}{item.visit?.appointmentAt ? ` · ${dateLabel(item.visit.appointmentAt)}` : ''}</span><h3>{title}</h3></div></div>{detail && <p>{detail}</p>}</article>
}

function tabLabel(tab) { return { diagnoses: 'Диагноз', prescriptions: 'Назначение', labs: 'Анализ', referrals: 'Направление', documents: 'Документ' }[tab] || 'Медицинская запись' }
function tabEmptyText(tab) { return { diagnoses: 'Диагнозов пока нет', prescriptions: 'Назначений пока нет', labs: 'Результатов анализов пока нет', referrals: 'Направлений пока нет', documents: 'Документов пока нет', visits: 'Завершённых приёмов пока нет' }[tab] || 'Данных пока нет' }

