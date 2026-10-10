import { useEffect, useState } from 'react'
import { getDoctor } from '../api/apiClient.js'
import { navigate } from '../utils/navigation.js'
import { localizeSpecialty, translateError, useTranslation } from '../i18n/index.jsx'
import { useAuth } from '../auth/AuthContext.jsx'

function DoctorDetailPage({ id }) {
  const { t } = useTranslation()
  const { user } = useAuth()
  const [doctor, setDoctor] = useState(null)
  const [error, setError] = useState('')
  useEffect(() => { getDoctor(id).then(setDoctor).catch((requestError) => setError(translateError(requestError, t))) }, [id, t])
  if (error) return <section className="page-shell"><p className="error-message">{error}</p><button className="button button--secondary" type="button" onClick={() => navigate('/doctors')}>{t('common.back')}</button></section>
  if (!doctor) return <p className="route-message">{t('searchDoctors.loadingProfile')}</p>
  return <section className="page-shell"><button className="text-button back-link" type="button" onClick={() => navigate('/doctors')}>{t('searchDoctors.allDoctors')}</button><article className="profile-card"><div className="doctor-avatar doctor-avatar--large">{doctor.firstName?.[0]}{doctor.lastName?.[0]}</div><div><div className="eyebrow">{t('searchDoctors.doctorProfile')}</div><h1>{doctor.firstName} {doctor.lastName}</h1><p className="doctor-specialty">{localizeSpecialty(doctor.specialty, t)}</p>{doctor.demo && <span className="demo-badge">{t('searchDoctors.demo')}</span>}</div><dl className="detail-list"><div><dt>{t('searchDoctors.clinic')}</dt><dd>{doctor.clinic.name}</dd></div><div><dt>{t('searchDoctors.cityLabel')}</dt><dd>{doctor.cityName}</dd></div><div><dt>{t('searchDoctors.regionLabel')}</dt><dd>{doctor.regionName}</dd></div><div><dt>{t('searchDoctors.experience')}</dt><dd>{doctor.experienceYears} {t('searchDoctors.years')}</dd></div><div><dt>{t('searchDoctors.office')}</dt><dd>{doctor.officeNumber || '—'}</dd></div><div><dt>{t('searchDoctors.assignedNurse')}</dt><dd>{doctor.nurse ? `${doctor.nurse.firstName} ${doctor.nurse.lastName}` : '—'}</dd></div></dl>{doctor.bio && <div className="bio-block"><h2>{t('searchDoctors.aboutDoctor')}</h2><p>{doctor.bio}</p></div>}{doctor.demo && <p className="demo-notice">{t('searchDoctors.demoDescription')}</p>}{user?.role === 'PATIENT' && <button className="button" type="button" onClick={() => navigate(`/patient/appointments?doctorId=${doctor.id}`)}>{t('patient.bookForDoctor')}</button>}</article></section>
}
export default DoctorDetailPage
