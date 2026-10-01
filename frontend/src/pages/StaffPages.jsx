import { useEffect, useMemo, useState } from 'react'
import { ApiError, getDoctorCalendar, getDoctorProfile, getNurseCalendar, getNurseProfile, updateDoctorProfile, updateNurseProfile } from '../api/apiClient.js'
import { localizeSpecialty, useTranslation } from '../i18n/index.jsx'
import { navigate } from '../utils/navigation.js'

const emptyDoctorForm = { avatarUrl: '', about: '', education: '', qualificationCategory: '', cabinetNumber: '', consultationDurationMinutes: 30 }
const emptyNurseForm = { avatarUrl: '', cabinetNumber: '', education: '', qualification: '', experienceYears: 0, about: '' }

export function DoctorProfilePage() {
  const { t } = useTranslation()
  const [profile, setProfile] = useState(null)
  const [form, setForm] = useState(emptyDoctorForm)
  const [editing, setEditing] = useState(false)
  const [error, setError] = useState('')
  const [notice, setNotice] = useState('')
  const [busy, setBusy] = useState(false)

  const load = () => getDoctorProfile().then((value) => { setProfile(value); setForm({ ...emptyDoctorForm, avatarUrl: value.avatarUrl || '', about: value.about || value.bio || '', education: value.education || '', qualificationCategory: value.qualificationCategory || '', cabinetNumber: value.cabinetNumber || value.officeNumber || '', consultationDurationMinutes: value.consultationDurationMinutes || 30 }) }).catch((requestError) => setError(messageFor(requestError, t)))
  useEffect(() => { load() }, [])
  const save = async (event) => { event.preventDefault(); setBusy(true); setError(''); setNotice(''); try { const value = await updateDoctorProfile({ ...form, consultationDurationMinutes: Number(form.consultationDurationMinutes) }); setProfile(value); setEditing(false); setNotice(t('staff.profileSaved')) } catch (requestError) { setError(messageFor(requestError, t)) } finally { setBusy(false) } }
  if (!profile) return <StaffLoading error={error} retry={load} t={t} />
  const fullName = [profile.lastName, profile.firstName, profile.middleName].filter(Boolean).join(' ')
  return <section className="page-shell staff-profile-page">
    <div className="page-heading page-heading--row"><div><div className="eyebrow">{t('staff.doctorRole')}</div><h1>{t('staff.doctorProfileTitle')}</h1><p className="form-description">{t('staff.doctorProfileDescription')}</p></div><button className="button button--secondary button--compact" type="button" onClick={() => setEditing((value) => !value)}>{editing ? t('common.cancel') : t('common.edit')}</button></div>
    {error && <p className="error-message" role="alert">{error}</p>}{notice && <p className="success-message">{notice}</p>}
    <ProfileHero profile={profile} role="doctor" fullName={fullName} t={t} />
    {editing ? <form className="staff-profile-edit" onSubmit={save}><div className="staff-profile-section"><SectionTitle title={t('staff.basicInformation')} /><div className="form-grid"><Field label={t('staff.avatarUrl')} value={form.avatarUrl} onChange={setFormValue(setForm, 'avatarUrl')} placeholder="https://…" /><Field label={t('staff.cabinet')} value={form.cabinetNumber} onChange={setFormValue(setForm, 'cabinetNumber')} /><Field label={t('staff.consultationDuration')} type="number" min="10" max="240" value={form.consultationDurationMinutes} onChange={setFormValue(setForm, 'consultationDurationMinutes')} /><Field label={t('staff.qualification')} value={form.qualificationCategory} onChange={setFormValue(setForm, 'qualificationCategory')} /></div><Field label={t('staff.education')} value={form.education} onChange={setFormValue(setForm, 'education')} /><TextArea label={t('staff.about')} value={form.about} onChange={setFormValue(setForm, 'about')} /><p className="form-hint">{t('staff.protectedFields')}</p><button className="button" type="submit" disabled={busy}>{busy ? t('common.loading') : t('common.save')}</button></div></form> : <DoctorProfileDetails profile={profile} fullName={fullName} t={t} />}
  </section>
}

export function NurseProfilePage() {
  const { t } = useTranslation()
  const [profile, setProfile] = useState(null)
  const [schedule, setSchedule] = useState(null)
  const [form, setForm] = useState(emptyNurseForm)
  const [editing, setEditing] = useState(false)
  const [error, setError] = useState('')
  const [notice, setNotice] = useState('')
  const [busy, setBusy] = useState(false)
  const today = todayKey()
  const load = () => Promise.all([getNurseProfile(), getNurseCalendar({ from: today, to: today })]).then(([value, calendar]) => { setProfile(value); setSchedule(calendar); setForm({ ...emptyNurseForm, avatarUrl: value.avatarUrl || '', cabinetNumber: value.cabinetNumber || '', education: value.education || '', qualification: value.qualification || '', experienceYears: value.experienceYears || 0, about: value.about || '' }) }).catch((requestError) => setError(messageFor(requestError, t)))
  useEffect(() => { load() }, [])
  const save = async (event) => { event.preventDefault(); setBusy(true); setError(''); setNotice(''); try { const value = await updateNurseProfile({ ...form, experienceYears: Number(form.experienceYears) }); setProfile(value); setEditing(false); setNotice(t('staff.profileSaved')) } catch (requestError) { setError(messageFor(requestError, t)) } finally { setBusy(false) } }
  if (!profile) return <StaffLoading error={error} retry={load} t={t} />
  const fullName = [profile.lastName, profile.firstName, profile.middleName].filter(Boolean).join(' ')
  const doctorName = profile.doctor ? [profile.doctor.lastName, profile.doctor.firstName, profile.doctor.middleName].filter(Boolean).join(' ') : ''
  const shift = shiftForDay(schedule, today)
  return <section className="page-shell staff-profile-page">
    <div className="page-heading page-heading--row"><div><div className="eyebrow">{t('staff.nurseRole')}</div><h1>{t('staff.nurseProfileTitle')}</h1><p className="form-description">{t('staff.nurseProfileDescription')}</p></div><button className="button button--secondary button--compact" type="button" onClick={() => setEditing((value) => !value)}>{editing ? t('common.cancel') : t('common.edit')}</button></div>
    {error && <p className="error-message" role="alert">{error}</p>}{notice && <p className="success-message">{notice}</p>}
    <ProfileHero profile={profile} role="nurse" fullName={fullName} t={t} />
    {editing ? <form className="staff-profile-edit" onSubmit={save}><div className="staff-profile-section"><SectionTitle title={t('staff.basicInformation')} /><div className="form-grid"><Field label={t('staff.avatarUrl')} value={form.avatarUrl} onChange={setFormValue(setForm, 'avatarUrl')} placeholder="https://…" /><Field label={t('staff.cabinet')} value={form.cabinetNumber} onChange={setFormValue(setForm, 'cabinetNumber')} /><Field label={t('staff.qualification')} value={form.qualification} onChange={setFormValue(setForm, 'qualification')} /><Field label={t('staff.experience')} type="number" min="0" max="70" value={form.experienceYears} onChange={setFormValue(setForm, 'experienceYears')} /></div><Field label={t('staff.education')} value={form.education} onChange={setFormValue(setForm, 'education')} /><TextArea label={t('staff.about')} value={form.about} onChange={setFormValue(setForm, 'about')} /><button className="button" type="submit" disabled={busy}>{busy ? t('common.loading') : t('common.save')}</button></div></form> : <NurseProfileDetails profile={profile} doctorName={doctorName} shift={shift} t={t} onSchedule={() => navigate('/nurse/schedule')} />}
  </section>
}

function DoctorProfileDetails({ profile, fullName, t }) {
  return <div className="staff-profile-grid"><div className="staff-profile-section"><SectionTitle title={t('staff.basicInformation')} /><DefinitionList items={[[t('staff.fullName'), fullName], [t('staff.specialty'), localizeSpecialty(profile.specialty, t)], [t('staff.cabinet'), profile.cabinetNumber || profile.officeNumber || '—'], [t('staff.consultationDuration'), profile.consultationDurationMinutes ? `${profile.consultationDurationMinutes} ${t('staff.minutes')}` : '—'], [t('staff.experience'), `${profile.experienceYears || 0} ${t('staff.years')}`], [t('staff.qualification'), profile.qualificationCategory || '—']]} /></div><div className="staff-profile-section"><SectionTitle title={t('staff.workplace')} /><DefinitionList items={[[t('staff.region'), profile.regionName || profile.clinic?.regionName], [t('staff.city'), profile.cityName || profile.clinic?.cityName], [t('staff.clinic'), profile.clinic?.name], [t('staff.address'), profile.clinic?.address || '—']]} /></div><div className="staff-profile-section"><SectionTitle title={t('staff.education')} /><p className="profile-copy">{profile.education || t('staff.notProvided')}</p></div><div className="staff-profile-section"><SectionTitle title={t('staff.about')} /><p className="profile-copy">{profile.about || profile.bio || t('staff.notProvided')}</p></div><NurseCard nurse={profile.nurse} t={t} /></div>
}

function NurseProfileDetails({ profile, doctorName, shift, t, onSchedule }) {
  return <div className="staff-profile-grid"><div className="staff-profile-section"><SectionTitle title={t('staff.basicInformation')} /><DefinitionList items={[[t('staff.fullName'), [profile.lastName, profile.firstName, profile.middleName].filter(Boolean).join(' ')], [t('staff.position'), t('staff.nursePosition')], [t('staff.cabinet'), profile.cabinetNumber || '—'], [t('staff.experience'), `${profile.experienceYears || 0} ${t('staff.years')}`], [t('staff.qualification'), profile.qualification || '—']]} /></div><div className="staff-profile-section"><SectionTitle title={t('staff.workplace')} /><DefinitionList items={[[t('staff.clinic'), profile.clinic?.name], [t('staff.city'), profile.clinic?.cityName], [t('staff.region'), profile.clinic?.regionName], [t('staff.address'), profile.clinic?.address || '—']]} /></div><div className="staff-profile-section"><SectionTitle title={t('staff.education')} /><p className="profile-copy">{profile.education || t('staff.notProvided')}</p></div><div className="staff-profile-section"><SectionTitle title={t('staff.about')} /><p className="profile-copy">{profile.about || t('staff.notProvided')}</p></div><section className="staff-profile-section staff-profile-section--featured"><SectionTitle title={t('staff.myDoctor')} />{profile.doctor ? <div className="linked-staff"><ProfileAvatar profile={profile.doctor} /> <div><strong>{doctorName}</strong><span>{localizeSpecialty(profile.doctor.specialty, t)}</span><span>{t('staff.cabinet')}: {profile.doctor.cabinetNumber || '—'}</span><span>{profile.doctor.clinicName || profile.clinic?.name}</span><span>{t('staff.today')}: {shift || t('staff.dayOff')}</span></div><button className="button button--secondary button--compact" type="button" onClick={onSchedule}>{t('staff.viewSchedule')}</button></div> : <div className="empty-state"><p>{t('staff.noDoctor')}</p></div>}</section></div>
}

function NurseCard({ nurse, t }) { return <section className="staff-profile-section staff-profile-section--featured"><SectionTitle title={t('staff.myNurse')} />{nurse ? <div className="linked-staff"><ProfileAvatar profile={nurse} /><div><strong>{[nurse.lastName, nurse.firstName, nurse.middleName].filter(Boolean).join(' ')}</strong><span>{nurse.qualification || t('staff.nursePosition')}</span><span>{t('staff.nurseAtClinic')}</span></div></div> : <div className="empty-state"><p>{t('staff.noNurse')}</p></div>}</section> }
function ProfileHero({ profile, role, fullName, t }) { return <article className="staff-profile-hero"><ProfileAvatar profile={profile} large /><div className="staff-profile-hero__copy"><div className="eyebrow">{role === 'doctor' ? localizeSpecialty(profile.specialty, t) : t('staff.nursePosition')}</div><h2>{fullName}</h2><p>{role === 'doctor' ? profile.clinic?.name : profile.clinic?.name}</p><div className="staff-profile-hero__meta"><span>{profile.clinic?.cityName}</span><span>{t('staff.experience')}: {profile.experienceYears || 0} {t('staff.years')}</span><span className={`status-badge ${profile.active ? 'status-badge--active' : ''}`}>{profile.active ? t('common.active') : t('common.inactive')}</span></div></div></article> }
function ProfileAvatar({ profile, large = false }) { const name = [profile?.firstName, profile?.lastName].filter(Boolean).join(' '); return profile?.avatarUrl ? <img className={`profile-avatar ${large ? 'profile-avatar--large' : ''}`} src={profile.avatarUrl} alt={name} /> : <div className={`profile-avatar ${large ? 'profile-avatar--large' : ''}`} aria-label={name}>{`${profile?.firstName?.[0] || ''}${profile?.lastName?.[0] || ''}`.toUpperCase()}</div> }
function SectionTitle({ title }) { return <div className="staff-profile-section__title"><h3>{title}</h3></div> }
function DefinitionList({ items }) { return <dl className="staff-definition-list">{items.map(([label, value]) => <div key={label}><dt>{label}</dt><dd>{value || '—'}</dd></div>)}</dl> }
function Field({ label, value, onChange, type = 'text', min, max, placeholder }) { return <label className="field-group"><span className="field-label">{label}</span><input type={type} min={min} max={max} value={value ?? ''} onChange={onChange} placeholder={placeholder} /></label> }
function TextArea({ label, value, onChange }) { return <label className="field-group"><span className="field-label">{label}</span><textarea rows="4" value={value ?? ''} onChange={onChange} /></label> }
function setFormValue(setForm, name) { return (event) => setForm((current) => ({ ...current, [name]: event.target.value })) }
function StaffLoading({ error, retry, t }) { return <section className="page-shell"><div className="staff-loading"><span className="loading-dot" /><p>{error || t('common.loading')}</p>{error && <button className="button button--secondary button--compact" type="button" onClick={retry}>{t('staff.retry')}</button>}</div></section> }
function messageFor(error, t) { return error instanceof ApiError ? error.message : error?.message || t('staff.loadError') }
function todayKey() { const date = new Date(); return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}-${String(date.getDate()).padStart(2, '0')}` }
function shiftForDay(calendar, date) { if (!calendar?.schedule) return ''; const day = dayOfWeek(date); const exception = calendar.exceptions?.find((item) => item.date === date); if (exception && exception.type !== 'CUSTOM_WORKING_HOURS') return ''; const intervals = exception?.type === 'CUSTOM_WORKING_HOURS' ? [exception] : calendar.schedule.intervals.filter((item) => item.dayOfWeek === day && !item.breakInterval); return intervals.map((item) => `${item.startTime}–${item.endTime}`).join(', ') }
function dayOfWeek(value) { return ['SUNDAY', 'MONDAY', 'TUESDAY', 'WEDNESDAY', 'THURSDAY', 'FRIDAY', 'SATURDAY'][new Date(`${value}T12:00:00`).getDay()] }

