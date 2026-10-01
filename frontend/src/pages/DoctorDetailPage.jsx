import { useEffect, useState } from 'react'
import { ApiError, getDoctor } from '../api/apiClient.js'
import { navigate } from '../utils/navigation.js'
import { localizeSpecialty, useTranslation } from '../i18n/index.jsx'

function DoctorDetailPage({ id }) {
  const { t } = useTranslation()
  const [doctor, setDoctor] = useState(null)
  const [error, setError] = useState('')
  useEffect(() => { getDoctor(id).then(setDoctor).catch((requestError) => setError(requestError instanceof ApiError ? requestError.message : t('common.error'))) }, [id, t])
  if (error) return <section className="page-shell"><p className="error-message">{error}</p><button className="button button--secondary" type="button" onClick={() => navigate('/doctors')}>{t('common.back')}</button></section>
  if (!doctor) return <p className="route-message">{t('searchDoctors.loadingProfile')}</p>
  return <section className="page-shell"><button className="text-button back-link" type="button" onClick={() => navigate('/doctors')}>{t('searchDoctors.allDoctors')}</button><article className="profile-card"><div className="doctor-avatar doctor-avatar--large">{doctor.firstName?.[0]}{doctor.lastName?.[0]}</div><div><div className="eyebrow">{t('searchDoctors.doctorProfile')}</div><h1>{doctor.firstName} {doctor.lastName}</h1><p className="doctor-specialty">{localizeSpecialty(doctor.specialty.name, t)}</p></div><dl className="detail-list"><div><dt>{t('searchDoctors.clinic')}</dt><dd>{doctor.clinic.name}</dd></div><div><dt>{t('searchDoctors.cityLabel')}</dt><dd>{doctor.cityName}</dd></div><div><dt>{t('searchDoctors.regionLabel')}</dt><dd>{doctor.regionName}</dd></div><div><dt>{t('searchDoctors.experience')}</dt><dd>{doctor.experienceYears} {t('searchDoctors.years')}</dd></div><div><dt>{t('searchDoctors.office')}</dt><dd>{doctor.officeNumber || '—'}</dd></div></dl>{doctor.bio && <div className="bio-block"><h2>{t('searchDoctors.aboutDoctor')}</h2><p>{doctor.bio}</p></div>}<p className="hint-text">{t('common.nextStage')}</p></article></section>
}
export default DoctorDetailPage
