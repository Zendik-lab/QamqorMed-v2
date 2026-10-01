import { useEffect, useState } from 'react'
import { ApiError, getClinicSpecialties, getCityClinics, getDoctorsPage, getRegionCities, getRegions, getSpecialties } from '../api/apiClient.js'
import { navigate } from '../utils/navigation.js'
import { localizeDirectory, localizeSpecialty, useTranslation } from '../i18n/index.jsx'

const PAGE_SIZE = 20

function DoctorsPage() {
  const { t, language } = useTranslation()
  const [regions, setRegions] = useState([])
  const [cities, setCities] = useState([])
  const [clinics, setClinics] = useState([])
  const [allSpecialties, setAllSpecialties] = useState([])
  const [specialties, setSpecialties] = useState([])
  const [doctors, setDoctors] = useState([])
  const [pageInfo, setPageInfo] = useState({ page: 0, totalPages: 0, totalElements: 0 })
  const [filters, setFilters] = useState({ regionId: '', cityId: '', clinicId: '', specialtyId: '', search: '' })
  const [loading, setLoading] = useState(true)
  const [doctorsLoading, setDoctorsLoading] = useState(false)
  const [error, setError] = useState('')

  useEffect(() => {
    Promise.all([getRegions(), getSpecialties()])
      .then(([regionData, specialtyData]) => { setRegions(regionData); setAllSpecialties(specialtyData); setSpecialties(specialtyData) })
      .catch((requestError) => setError(requestError instanceof ApiError ? requestError.message : t('common.error')))
      .finally(() => setLoading(false))
  }, [t])

  useEffect(() => {
    if (!filters.regionId) { setCities([]); return }
    getRegionCities(filters.regionId).then(setCities).catch(() => setError(t('common.error')))
  }, [filters.regionId, t])

  useEffect(() => {
    if (!filters.cityId) { setClinics([]); return }
    getCityClinics(filters.cityId).then(setClinics).catch(() => setError(t('common.error')))
  }, [filters.cityId, t])

  useEffect(() => {
    if (!filters.clinicId) { setSpecialties(allSpecialties); return }
    getClinicSpecialties(filters.clinicId).then((items) => setSpecialties(items.length ? items : allSpecialties)).catch(() => setSpecialties(allSpecialties))
  }, [filters.clinicId, allSpecialties])

  useEffect(() => {
    setDoctorsLoading(true)
    getDoctorsPage({ ...filters, page: pageInfo.page, size: PAGE_SIZE })
      .then((result) => { setDoctors(result.content || []); setPageInfo({ page: result.page, totalPages: result.totalPages, totalElements: result.totalElements }) })
      .catch((requestError) => setError(requestError instanceof ApiError ? requestError.message : t('common.error')))
      .finally(() => setDoctorsLoading(false))
  }, [filters.regionId, filters.cityId, filters.clinicId, filters.specialtyId, filters.search, pageInfo.page, t])

  const resetPage = () => setPageInfo((current) => ({ ...current, page: 0 }))
  const updateRegion = (event) => { resetPage(); setFilters((current) => ({ ...current, regionId: event.target.value, cityId: '', clinicId: '', specialtyId: '' })) }
  const updateCity = (event) => { resetPage(); setFilters((current) => ({ ...current, cityId: event.target.value, clinicId: '', specialtyId: '' })) }
  const update = (name) => (event) => { resetPage(); setFilters((current) => ({ ...current, [name]: event.target.value })) }
  const changePage = (nextPage) => setPageInfo((current) => ({ ...current, page: Math.max(0, Math.min(nextPage, Math.max(current.totalPages - 1, 0))) }))

  return (
    <section className="page-shell">
      <div className="page-heading"><div className="eyebrow">{t('searchDoctors.eyebrow')}</div><h1>{t('searchDoctors.title')}</h1><p className="form-description">{t('searchDoctors.description')}</p></div>
      {error && <p className="error-message" role="alert">{error}</p>}
      <div className="filter-grid">
        <label className="field-group"><span className="field-label">{t('searchDoctors.region')}</span><select value={filters.regionId} onChange={updateRegion} disabled={loading}><option value="">{t('searchDoctors.allRegions')}</option>{regions.map((item) => <option value={item.id} key={item.id}>{localizeDirectory(item, language)}</option>)}</select></label>
        <label className="field-group"><span className="field-label">{t('searchDoctors.city')}</span><select value={filters.cityId} onChange={updateCity} disabled={!filters.regionId}><option value="">{t('searchDoctors.allCities')}</option>{cities.map((item) => <option value={item.id} key={item.id}>{localizeDirectory(item, language)}</option>)}</select></label>
        <label className="field-group"><span className="field-label">{t('searchDoctors.clinic')}</span><select value={filters.clinicId} onChange={update('clinicId')} disabled={!filters.cityId}><option value="">{t('searchDoctors.allClinics')}</option>{clinics.map((item) => <option value={item.id} key={item.id}>{item.name}</option>)}</select></label>
        <label className="field-group"><span className="field-label">{t('searchDoctors.specialty')}</span><select value={filters.specialtyId} onChange={update('specialtyId')}><option value="">{t('searchDoctors.allSpecialties')}</option>{specialties.map((item) => <option value={item.id} key={item.id}>{localizeDirectory(item, language) || item.name}</option>)}</select></label>
        <label className="field-group field-group--wide"><span className="field-label">{t('searchDoctors.searchByName')}</span><input value={filters.search} onChange={update('search')} placeholder={t('searchDoctors.searchPlaceholder')} /></label>
      </div>
      <div className="results-heading"><h2>{t('searchDoctors.results')}</h2>{doctorsLoading && <span className="hint-text">{t('common.loading')}</span>} {!doctorsLoading && <span className="hint-text">{pageInfo.totalElements} · {t('searchDoctors.demoNotice')}</span>}</div>
      {!doctorsLoading && doctors.length === 0 && <p className="empty-state">{t('searchDoctors.notFound')}</p>}
      <div className="doctor-grid">{doctors.map((doctor) => <article className="doctor-card" key={doctor.id}>
        <div className="doctor-avatar">{doctor.firstName?.[0]}{doctor.lastName?.[0]}</div><div className="doctor-card__body"><div className="card-meta">{doctor.demo && <span className="demo-badge">{t('searchDoctors.demo')}</span>}</div><h3>{doctor.firstName} {doctor.lastName}</h3><p className="doctor-specialty">{localizeSpecialty(doctor.specialty, t)}</p><p>{doctor.clinic.name}</p><p className="muted">{doctor.cityName} · {t('searchDoctors.experience')}: {doctor.experienceYears} {t('searchDoctors.years')} · {t('searchDoctors.office')}: {doctor.officeNumber || '—'}</p><button className="button button--secondary" type="button" onClick={() => navigate(`/doctors/${doctor.id}`)}>{t('searchDoctors.openProfile')}</button></div>
      </article>)}</div>
      {pageInfo.totalPages > 1 && <div className="pagination"><button className="button button--secondary button--compact" type="button" onClick={() => changePage(pageInfo.page - 1)} disabled={pageInfo.page === 0}>{t('searchDoctors.previous')}</button><span>{pageInfo.page + 1} / {pageInfo.totalPages}</span><button className="button button--secondary button--compact" type="button" onClick={() => changePage(pageInfo.page + 1)} disabled={pageInfo.page + 1 >= pageInfo.totalPages}>{t('searchDoctors.next')}</button></div>}
    </section>
  )
}
export default DoctorsPage
