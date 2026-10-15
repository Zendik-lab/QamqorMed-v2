import { useEffect, useRef, useState } from 'react'
import { getDoctorCalendar, getDoctorProfile, getNurseCalendar, getNurseProfile, updateDoctorProfile, updateNurseProfile } from '../api/apiClient.js'
import { localizeSpecialty, translateError, useTranslation } from '../i18n/index.jsx'
import { Input, TextArea } from '../components/ui/Fields.jsx'
import { Alert, Button, EmptyState, ErrorState, LoadingSpinner } from '../components/ui/Feedback.jsx'
import { AccountSettings, InitialsAvatar, ProfileFacts, ProfileHeader, ProfileLinks, ProfileLoading, ProfileSection, personName } from '../components/account/Profile.jsx'

const services = {
  DOCTOR: { load: getDoctorProfile, save: updateDoctorProfile, calendar: getDoctorCalendar },
  NURSE: { load: getNurseProfile, save: updateNurseProfile, calendar: getNurseCalendar },
}

function draftFor(role, profile) {
  const common = { avatarUrl: profile.avatarUrl || '', cabinetNumber: profile.cabinetNumber || profile.officeNumber || '', education: profile.education || '', about: profile.about || profile.bio || '' }
  return role === 'DOCTOR' ? { ...common, qualificationCategory: profile.qualificationCategory || '', consultationDurationMinutes: profile.consultationDurationMinutes ?? '' } : { ...common, qualification: profile.qualification || '', experienceYears: profile.experienceYears ?? '' }
}

export function DoctorProfilePage() { return <StaffProfilePage role="DOCTOR" /> }
export function NurseProfilePage() { return <StaffProfilePage role="NURSE" /> }

function StaffProfilePage({ role }) {
  const { t } = useTranslation()
  const doctor = role === 'DOCTOR'
  const [profile, setProfile] = useState(null)
  const [form, setForm] = useState({})
  const [editing, setEditing] = useState(false)
  const [error, setError] = useState(null)
  const [saved, setSaved] = useState(false)
  const [busy, setBusy] = useState(false)
  const pending = useRef(false)
  const editButton = useRef(null)
  const focusEdit = () => requestAnimationFrame(() => editButton.current?.focus())
  const load = () => { setError(null); services[role].load().then((value) => { setProfile(value); setForm(draftFor(role, value)) }).catch(setError) }
  useEffect(() => { load() }, [role])
  const change = (name) => (event) => { setForm((current) => ({ ...current, [name]: event.target.value })); setError(null); setSaved(false) }
  const cancel = () => { setForm(draftFor(role, profile)); setEditing(false); setError(null); focusEdit() }
  const save = async (event) => {
    event.preventDefault()
    if (pending.current) return
    pending.current = true; setBusy(true); setError(null); setSaved(false)
    try {
      const value = await services[role].save({ ...form, ...(doctor ? { consultationDurationMinutes: Number(form.consultationDurationMinutes) } : { experienceYears: Number(form.experienceYears) }) })
      setProfile(value); setForm(draftFor(role, value)); setEditing(false); setSaved(true)
      focusEdit()
    } catch (requestError) { setError(requestError) }
    finally { pending.current = false; setBusy(false) }
  }
  if (!profile) return <ProfileLoading error={error && translateError(error, t, 'staff.loadError')} retry={load} />
  const linkedPerson = doctor ? profile.nurse : profile.doctor
  const specialty = doctor ? localizeSpecialty(profile.specialty, t) : t('staff.nursePosition')
  return <section className="page-shell account-page">
    <ProfileHeader person={profile} role={role} title={t(doctor ? 'staff.doctorProfileTitle' : 'staff.nurseProfileTitle')} description={t(doctor ? 'staff.doctorProfileDescription' : 'staff.nurseProfileDescription')} subtitle={[specialty, profile.clinic?.name].filter(Boolean).join(' · ')} action={<Button ref={editButton} variant="secondary" disabled={busy} onClick={() => { if (editing) cancel(); else { setForm(draftFor(role, profile)); setEditing(true); setSaved(false) } }}>{t(editing ? 'common.cancel' : 'common.edit')}</Button>} />
    {error && <Alert tone="danger">{translateError(error, t)}</Alert>}{saved && <Alert tone="success">{t('staff.profileSaved')}</Alert>}
    <div className="account-grid"><div className="account-stack">
      <ProfileSection title={t('staff.basicInformation')} icon="medical">
        {editing ? <form className="account-editor" onSubmit={save} aria-busy={busy}><fieldset className="account-fieldset" disabled={busy}><legend className="sr-only">{t('staff.basicInformation')}</legend><div className="form-grid">
          <Input label={t('staff.cabinet')} value={form.cabinetNumber} onChange={change('cabinetNumber')} maxLength={30} autoFocus />
          {doctor ? <Input label={t('staff.consultationDuration')} type="number" min={10} max={240} step={1} value={form.consultationDurationMinutes} onChange={change('consultationDurationMinutes')} required /> : <Input label={t('staff.experience')} type="number" min={0} max={70} step={1} value={form.experienceYears} onChange={change('experienceYears')} required />}
          <Input label={t('staff.qualification')} value={doctor ? form.qualificationCategory : form.qualification} onChange={change(doctor ? 'qualificationCategory' : 'qualification')} maxLength={doctor ? 120 : 300} />
          <Input label={t('staff.avatarUrl')} type="url" value={form.avatarUrl} onChange={change('avatarUrl')} maxLength={500} placeholder="https://…" />
        </div><TextArea label={t('staff.education')} value={form.education} onChange={change('education')} maxLength={2000} /><TextArea label={t('staff.about')} value={form.about} onChange={change('about')} maxLength={4000} /><p className="account-edit-note">{t('staff.protectedFields')}</p><div className="form-actions"><Button type="submit" busy={busy}>{t(busy ? 'patient.saving' : 'common.save')}</Button><Button variant="secondary" onClick={cancel}>{t('common.cancel')}</Button></div></fieldset></form> : <ProfileFacts items={[[t(doctor ? 'staff.specialty' : 'staff.position'), specialty], [t('staff.cabinet'), profile.cabinetNumber || profile.officeNumber], [t('staff.experience'), profile.experienceYears == null ? '' : `${profile.experienceYears} ${t('staff.years')}`], [t('staff.qualification'), doctor ? profile.qualificationCategory : profile.qualification], ...(doctor ? [[t('staff.consultationDuration'), profile.consultationDurationMinutes == null ? '' : `${profile.consultationDurationMinutes} ${t('staff.minutes')}`]] : [])]} />}
      </ProfileSection>
      {!editing && <><ProfileSection title={t('staff.education')} icon="file"><p className="profile-copy">{profile.education || t('staff.notProvided')}</p></ProfileSection><ProfileSection title={t('staff.about')}><p className="profile-copy">{profile.about || profile.bio || t('staff.notProvided')}</p></ProfileSection></>}
    </div><div className="account-stack">
      <ProfileSection title={t('staff.workplace')} icon="building"><ProfileFacts items={[[t('staff.clinic'), profile.clinic?.name], [t('staff.region'), profile.regionName || profile.clinic?.regionName], [t('staff.city'), profile.cityName || profile.clinic?.cityName], [t('staff.address'), profile.clinic?.address]]} /></ProfileSection>
      <ProfileSection title={t(doctor ? 'staff.myNurse' : 'staff.myDoctor')} icon="users">{linkedPerson ? <div className="account-person"><InitialsAvatar person={linkedPerson} /><div><strong>{personName(linkedPerson)}</strong><span>{doctor ? linkedPerson.qualification || t('staff.nursePosition') : localizeSpecialty(linkedPerson.specialty, t)}</span>{linkedPerson.cabinetNumber && <span>{t('staff.cabinet')}: {linkedPerson.cabinetNumber}</span>}</div></div> : <EmptyState compact title={t(doctor ? 'staff.noNurse' : 'staff.noDoctor')} description={t('account.assignmentNote')} icon="users" />}</ProfileSection>
      <ProfileSchedule role={role} />
    </div></div>
    <AccountSettings />
  </section>
}

function ProfileSchedule({ role }) {
  const { t } = useTranslation()
  const [calendar, setCalendar] = useState(null)
  const [error, setError] = useState(null)
  // The clinic runs in Kazakhstan's UTC+5, independent of the device timezone.
  const today = new Intl.DateTimeFormat('en-CA', { timeZone: 'Asia/Qyzylorda', year: 'numeric', month: '2-digit', day: '2-digit' }).format(new Date())
  const load = () => { setError(null); services[role].calendar({ from: today, to: today }).then(setCalendar).catch(setError) }
  useEffect(() => { load() }, [role, today])
  let intervals = []
  if (calendar) {
    const exception = calendar.exceptions?.find((item) => item.date === today)
    const day = ['SUNDAY', 'MONDAY', 'TUESDAY', 'WEDNESDAY', 'THURSDAY', 'FRIDAY', 'SATURDAY'][new Date(`${today}T12:00:00`).getDay()]
    intervals = exception ? exception.type === 'CUSTOM_WORKING_HOURS' ? [exception] : [] : (calendar.schedule?.intervals || []).filter((item) => item.dayOfWeek === day && !item.breakInterval)
  }
  return <ProfileSection title={t('account.todaySchedule')} icon="calendar">
    {error ? <ErrorState description={translateError(error, t, 'staff.loadError')} onRetry={load} /> : !calendar ? <LoadingSpinner /> : !calendar.schedule && !intervals.length ? <p>{t('account.scheduleMissing')}</p> : intervals.length ? <ProfileFacts items={[[t('staff.today'), intervals.map((item) => `${item.startTime?.slice(0, 5)}–${item.endTime?.slice(0, 5)}`).join(', ')]]} /> : <p>{t('staff.dayOff')}</p>}
    <ProfileLinks items={[[role === 'DOCTOR' ? '/doctor/schedule' : '/nurse/appointments', t('staff.viewSchedule'), 'calendar']]} />
  </ProfileSection>
}
