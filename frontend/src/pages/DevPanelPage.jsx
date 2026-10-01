import { useEffect, useMemo, useState } from 'react'
import {
  ApiError,
  devGetAllSpecialties,
  devGetCities,
  devGetClinics,
  devGetRegions,
  devGetSpecialties,
  devSearchUsers,
} from '../api/apiClient.js'
import { useAuth } from '../auth/AuthContext.jsx'
import { pathForRole } from '../components/RouteGuards.jsx'
import { isDevelopment } from '../config/environment.js'
import { navigate } from '../utils/navigation.js'

const PAGE_SIZE = 20

const roles = [
  { value: 'DOCTOR', label: 'Врачи', icon: '✦' },
  { value: 'NURSE', label: 'Медсёстры', icon: '♧' },
  { value: 'PATIENT', label: 'Пациенты', icon: '○' },
  { value: 'ADMIN', label: 'Администраторы', icon: '⚙' },
]

const issueFilters = {
  DOCTOR: [
    ['', 'Все записи'],
    ['without-nurse', 'Только без медсестры'],
    ['without-clinic', 'Только без клиники'],
    ['without-specialty', 'Только без специальности'],
  ],
  NURSE: [
    ['', 'Все записи'],
    ['without-doctor', 'Только без врача'],
    ['without-clinic', 'Только без клиники'],
  ],
}

function DevPanelPage() {
  const { user, isAuthenticated, loginAsDevUser } = useAuth()
  const [role, setRole] = useState('DOCTOR')
  const [regions, setRegions] = useState([])
  const [cities, setCities] = useState([])
  const [clinics, setClinics] = useState([])
  const [specialties, setSpecialties] = useState([])
  const [allSpecialties, setAllSpecialties] = useState([])
  const [searchDraft, setSearchDraft] = useState('')
  const [filters, setFilters] = useState({
    query: '', regionId: '', cityId: '', clinicId: '', specialtyId: '', issue: '', page: 0,
  })
  const [results, setResults] = useState([])
  const [pageInfo, setPageInfo] = useState({ page: 0, totalPages: 0, totalElements: 0 })
  const [loading, setLoading] = useState(true)
  const [catalogLoading, setCatalogLoading] = useState(true)
  const [error, setError] = useState('')
  const [actionUserId, setActionUserId] = useState(null)
  const [selectedRelation, setSelectedRelation] = useState(null)

  useEffect(() => {
    if (!isDevelopment) return undefined
    Promise.all([devGetRegions(), devGetAllSpecialties()])
      .then(([regionData, specialtyData]) => {
        setRegions(regionData)
        setAllSpecialties(specialtyData)
        setSpecialties(specialtyData)
      })
      .catch((requestError) => setError(messageFor(requestError)))
      .finally(() => setCatalogLoading(false))
    return undefined
  }, [])

  useEffect(() => {
    const timer = window.setTimeout(() => {
      setFilters((current) => current.query === searchDraft ? current : { ...current, query: searchDraft, page: 0 })
    }, 400)
    return () => window.clearTimeout(timer)
  }, [searchDraft])

  useEffect(() => {
    if (!filters.regionId) {
      setCities([])
      return undefined
    }
    devGetCities(filters.regionId).then(setCities).catch((requestError) => setError(messageFor(requestError)))
    return undefined
  }, [filters.regionId])

  useEffect(() => {
    if (!filters.cityId) {
      setClinics([])
      return undefined
    }
    devGetClinics(filters.cityId).then(setClinics).catch((requestError) => setError(messageFor(requestError)))
    return undefined
  }, [filters.cityId])

  useEffect(() => {
    if (!filters.clinicId) {
      setSpecialties(allSpecialties)
      return undefined
    }
    devGetSpecialties(filters.clinicId)
      .then((items) => setSpecialties(items.length ? items : allSpecialties))
      .catch(() => setSpecialties(allSpecialties))
    return undefined
  }, [filters.clinicId, allSpecialties])

  useEffect(() => {
    if (!isDevelopment) return undefined
    const controller = new AbortController()
    setLoading(true)
    setError('')
    devSearchUsers({
      role,
      query: filters.query,
      regionId: filters.regionId,
      cityId: filters.cityId,
      clinicId: filters.clinicId,
      specialtyId: filters.specialtyId,
      issue: filters.issue,
      page: filters.page,
      size: PAGE_SIZE,
    }, controller.signal)
      .then((result) => {
        setResults(result.content || [])
        setPageInfo({ page: result.page, totalPages: result.totalPages, totalElements: result.totalElements })
      })
      .catch((requestError) => {
        if (requestError.name !== 'AbortError') setError(messageFor(requestError))
      })
      .finally(() => setLoading(false))
    return () => controller.abort()
  }, [role, filters])

  const selectedRole = useMemo(() => roles.find((item) => item.value === role), [role])
  const currentIssues = issueFilters[role] || [['', 'Все записи']]
  const setFilter = (name, value, reset = true) => setFilters((current) => ({ ...current, [name]: value, ...(reset ? { page: 0 } : {}) }))

  const changeRole = (nextRole) => {
    setRole(nextRole)
    setSearchDraft('')
    setFilters({ query: '', regionId: '', cityId: '', clinicId: '', specialtyId: '', issue: '', page: 0 })
    setSelectedRelation(null)
  }

  const updateRegion = (event) => setFilters((current) => ({ ...current, regionId: event.target.value, cityId: '', clinicId: '', specialtyId: '', page: 0 }))
  const updateCity = (event) => setFilters((current) => ({ ...current, cityId: event.target.value, clinicId: '', specialtyId: '', page: 0 }))
  const updateClinic = (event) => setFilters((current) => ({ ...current, clinicId: event.target.value, specialtyId: '', page: 0 }))
  const changePage = (page) => setFilter('page', Math.max(0, Math.min(page, Math.max(pageInfo.totalPages - 1, 0))), false)

  const loginAs = async (item) => {
    setActionUserId(item.userId)
    setError('')
    try {
      const currentUser = await loginAsDevUser(item.userId)
      navigate(pathForRole(currentUser.role), { replace: true })
    } catch (requestError) {
      setError(messageFor(requestError))
    } finally {
      setActionUserId(null)
    }
  }

  if (!isDevelopment) return null

  return (
    <section className="dev-panel-page">
      <header className="dev-panel-hero">
        <div>
          <div className="dev-kicker"><span className="dev-pulse" /> Development tools</div>
          <h1>QamqorMed DEV PANEL</h1>
          <p>Быстрый поиск DEMO-пользователей, проверка связей и вход одним кликом.</p>
        </div>
        <div className="dev-hero-actions">
          {isAuthenticated && user?.role && <button className="dev-ghost-button" type="button" onClick={() => navigate(pathForRole(user.role))}>В кабинет</button>}
          <button className="dev-ghost-button" type="button" onClick={() => navigate('/login')}>Обычный вход</button>
        </div>
      </header>

      <div className="dev-panel-body">
        <div className="dev-tabs" role="tablist" aria-label="Роли">
          {roles.map((item) => <button className={`dev-tab ${role === item.value ? 'dev-tab--active' : ''}`} type="button" role="tab" aria-selected={role === item.value} key={item.value} onClick={() => changeRole(item.value)}><span>{item.icon}</span>{item.label}</button>)}
        </div>

        <div className="dev-search-card">
          <label className="dev-search-field"><span>Поиск по ФИО или ID</span><input value={searchDraft} onChange={(event) => setSearchDraft(event.target.value)} placeholder="Например: Аян, 10025 или 523" /></label>
          <div className="dev-filter-grid">
            {(role === 'DOCTOR' || role === 'NURSE') && <>
              <Filter label="Регион" value={filters.regionId} onChange={updateRegion} disabled={catalogLoading} options={regions} allLabel="Все регионы" />
              <Filter label="Город" value={filters.cityId} onChange={updateCity} disabled={!filters.regionId} options={cities} allLabel="Все города" />
              <Filter label="Клиника" value={filters.clinicId} onChange={updateClinic} disabled={!filters.cityId} options={clinics} allLabel="Все клиники" />
              {role === 'DOCTOR' && <Filter label="Специальность" value={filters.specialtyId} onChange={(event) => setFilter('specialtyId', event.target.value)} disabled={false} options={specialties} allLabel="Все специальности" />}
              {role === 'NURSE' && <Filter label="Специальность" value={filters.specialtyId} onChange={(event) => setFilter('specialtyId', event.target.value)} disabled={false} options={specialties} allLabel="Все специальности" />}
              <Filter label="Быстрый фильтр" value={filters.issue} onChange={(event) => setFilter('issue', event.target.value)} options={currentIssues.slice(1).map(([value, label]) => ({ id: value, name: label }))} allLabel="Все записи" />
            </>}
          </div>
        </div>

        {error && <p className="error-message" role="alert">{error}</p>}

        <div className="dev-results-header"><div><span className="dev-section-kicker">{selectedRole?.label}</span><h2>Найденные пользователи</h2></div><div className="dev-result-count">{loading ? 'Загрузка…' : `Всего найдено: ${pageInfo.totalElements}`}</div></div>
        {loading && <div className="dev-loading"><span className="dev-spinner" /> Ищем по серверу…</div>}
        {!loading && results.length === 0 && <div className="dev-empty"><span>⌕</span><h3>Ничего не найдено</h3><p>Попробуйте изменить запрос или убрать один из фильтров.</p></div>}
        <div className="dev-results-grid">{results.map((item) => <DevUserCard key={item.userId} item={item} onLogin={loginAs} onRelation={setSelectedRelation} busy={actionUserId === item.userId} />)}</div>

        {!loading && pageInfo.totalPages > 0 && <div className="dev-pagination"><button className="dev-secondary-button" type="button" onClick={() => changePage(pageInfo.page - 1)} disabled={pageInfo.page === 0}>← Назад</button><span>Страница <strong>{pageInfo.page + 1}</strong> из <strong>{pageInfo.totalPages}</strong></span><button className="dev-secondary-button" type="button" onClick={() => changePage(pageInfo.page + 1)} disabled={pageInfo.page + 1 >= pageInfo.totalPages}>Вперёд →</button></div>}
      </div>

      {selectedRelation && <RelationModal relation={selectedRelation} onClose={() => setSelectedRelation(null)} />}
    </section>
  )
}

function Filter({ label, value, onChange, disabled = false, options = [], allLabel = 'Все' }) {
  return <label className="dev-filter"><span>{label}</span><select value={value} onChange={onChange} disabled={disabled}><option value="">{allLabel}</option>{options.map((item) => <option value={item.id} key={item.id}>{item.name}</option>)}</select></label>
}

function DevUserCard({ item, onLogin, onRelation, busy }) {
  const relation = item.relation
  const credentials = item.credentials || fallbackCredentials(item)
  const hasRelation = item.role === 'DOCTOR' || item.role === 'NURSE'
  const hasWarning = item.warnings?.length > 0 || (hasRelation && relation && !relation.connected)
  const location = [item.clinic?.name, item.city?.name, item.region?.name].filter(Boolean).join(' · ')
  return <article className={`dev-user-card ${hasWarning ? 'dev-user-card--warning' : ''}`}>
    <div className="dev-card-top"><div className="dev-avatar">{initials(item.fullName)}</div><div className="dev-card-title"><div className="dev-badges"><span className="dev-badge dev-badge--role">{item.role}</span>{item.demo && <span className="dev-badge dev-badge--demo">DEMO</span>}{!hasWarning && item.role !== 'PATIENT' && item.role !== 'ADMIN' && <span className="dev-badge dev-badge--connected">CONNECTED</span>}{hasWarning && <span className="dev-badge dev-badge--broken">BROKEN LINK</span>}</div><h3>{item.fullName}</h3><p className="dev-muted">User ID: {item.userId}</p></div></div>
    <div className="dev-card-details">
      {item.specialty && <div><span>Специальность</span><strong>{item.specialty.name}</strong></div>}
      {location && <div><span>Место работы</span><strong>{location}</strong></div>}
      {item.role === 'DOCTOR' && <div><span>Закреплённая медсестра</span><strong>{relation?.nurseName || '—'}</strong></div>}
      {item.role === 'NURSE' && <div><span>Закреплённый врач</span><strong>{relation?.doctorName || '—'}{relation?.specialtyName ? ` · ${relation.specialtyName}` : ''}</strong></div>}
    </div>
    {credentials && <div className="dev-credentials"><span className="dev-credentials__title">Данные для входа</span><div><span>Телефон</span><code>{credentials.phone}</code></div><div><span>Пароль</span><code>{credentials.password}</code></div></div>}
    {(item.warnings || []).map((warning) => <p className="dev-warning" key={warning}>⚠ {warning}</p>)}
    <div className="dev-id-row"><span>{item.doctorProfileId ? `DoctorProfile: ${item.doctorProfileId}` : item.nurseProfileId ? `NurseProfile: ${item.nurseProfileId}` : `Role: ${item.role}`}</span><span className={item.active ? 'dev-active' : 'dev-inactive'}>{item.active ? 'ACTIVE' : 'INACTIVE'}</span></div>
    <div className="dev-card-actions"><button className="dev-primary-button" type="button" onClick={() => onLogin(item)} disabled={busy}>{busy ? 'Входим…' : `Войти как ${roleLabel(item.role)}`}</button>{(item.role === 'DOCTOR' || item.role === 'NURSE') && <button className="dev-secondary-button" type="button" onClick={() => onRelation(relation)}>Показать связь</button>}</div>
  </article>
}

function fallbackCredentials(item) {
  if (!item?.demo || !item.phone) return null
  const legacyPasswords = {
    '+77000000001': 'Patient123!',
    '+77000000002': 'Doctor123!',
    '+77000000003': 'Nurse123!',
    '+77000000004': 'Admin123!',
  }
  const password = legacyPasswords[item.phone] || (['DOCTOR', 'NURSE'].includes(item.role) ? 'DemoStaff123!' : '')
  return password ? { phone: item.phone, password } : null
}

function RelationModal({ relation, onClose }) {
  return <div className="dev-modal-backdrop" role="presentation" onMouseDown={(event) => event.target === event.currentTarget && onClose()}><section className="dev-modal" role="dialog" aria-modal="true" aria-labelledby="relation-title"><div className="dev-modal-header"><div><span className="dev-section-kicker">Relationship inspector</span><h2 id="relation-title">Связь Doctor ↔ Nurse</h2></div><button className="dev-close-button" type="button" aria-label="Закрыть" onClick={onClose}>×</button></div>{relation?.connected ? <><div className="dev-relation-flow"><RelationPerson role="Doctor" name={relation.doctorName} subtitle={relation.specialtyName} /><span className="dev-relation-arrow">↓</span><div className="dev-relation-clinic"><span>работает в</span><strong>{relation.clinicName || 'Клиника не указана'}</strong><small>{[relation.cityName, relation.regionName].filter(Boolean).join(' · ')}</small></div><span className="dev-relation-arrow">↓</span><RelationPerson role="Nurse" name={relation.nurseName} /></div><div className="dev-technical-grid"><Tech label="doctorId" value={relation.doctorId} /><Tech label="nurseId" value={relation.nurseId} /><Tech label="doctorUserId" value={relation.doctorUserId} /><Tech label="nurseUserId" value={relation.nurseUserId} /><Tech label="clinicId" value={relation.clinicId} /><Tech label="specialtyId" value={relation.specialtyId} /><Tech label="assignmentId" value={relation.assignmentId} /></div></> : <div className="dev-relation-broken"><span>⚠</span><h3>Связь не найдена</h3><p>{relation?.warnings?.join(', ') || 'Для этого пользователя нет активного assignment.'}</p><div className="dev-technical-grid"><Tech label="doctorId" value={relation?.doctorId} /><Tech label="nurseId" value={relation?.nurseId} /><Tech label="assignmentId" value={relation?.assignmentId} /></div></div>}<button className="dev-primary-button dev-modal-button" type="button" onClick={onClose}>Закрыть</button></section></div>
}

function RelationPerson({ role, name, subtitle }) {
  return <div className="dev-relation-person"><span>{role}</span><strong>{name || '—'}</strong>{subtitle && <small>{subtitle}</small>}</div>
}

function Tech({ label, value }) {
  return <div><span>{label}</span><strong>{value ?? '—'}</strong></div>
}

function initials(value) {
  return (value || '?').split(/\s+/).slice(0, 2).map((part) => part[0]).join('').toUpperCase()
}

function roleLabel(role) {
  return { DOCTOR: 'врач', NURSE: 'медсестра', PATIENT: 'пациент', ADMIN: 'администратор' }[role] || role
}

function messageFor(error) {
  return error instanceof ApiError ? error.message : 'Не удалось загрузить данные Dev Panel'
}

export default DevPanelPage
