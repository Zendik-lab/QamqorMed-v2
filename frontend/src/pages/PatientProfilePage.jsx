import { useEffect, useRef, useState } from 'react'
import { getDependents, getPatientDiagnoses, getPatientLabResults, getPatientPrescriptions, getPatientProfile, updatePatientProfile } from '../api/apiClient.js'
import { useAuth } from '../auth/AuthContext.jsx'
import { localeFor, translateError, useTranslation } from '../i18n/index.jsx'
import { maskedIin } from '../utils/accountValidation.js'
import { AccountSettings, InitialsAvatar, ProfileFacts, ProfileHeader, ProfileLinks, ProfileLoading, ProfileSection, personName } from '../components/account/Profile.jsx'
import { Input, Select } from '../components/ui/Fields.jsx'
import { Alert, Button, EmptyState, ErrorState, LoadingSpinner } from '../components/ui/Feedback.jsx'

export default function PatientProfilePage() {
  const { t, language } = useTranslation()
  const { refreshUser } = useAuth()
  const [profile, setProfile] = useState(null)
  const [form, setForm] = useState({})
  const [editing, setEditing] = useState(false)
  const [error, setError] = useState(null)
  const [saved, setSaved] = useState(false)
  const [busy, setBusy] = useState(false)
  const pending = useRef(false)
  const editButton = useRef(null)
  const focusEdit = () => requestAnimationFrame(() => editButton.current?.focus())
  const load = () => { setError(null); getPatientProfile().then((value) => { setProfile(value); setForm(value) }).catch(setError) }
  useEffect(() => { load() }, [])
  const change = (name) => (event) => { setForm((current) => ({ ...current, [name]: name === 'iin' ? event.target.value.replace(/\D/g, '').slice(0, 12) : event.target.value })); setError(null); setSaved(false) }
  const cancel = () => { setForm(profile); setEditing(false); setError(null); focusEdit() }
  const save = async (event) => {
    event.preventDefault()
    if (pending.current) return
    pending.current = true; setBusy(true); setError(null); setSaved(false)
    try {
      const value = await updatePatientProfile({ firstName: form.firstName.trim(), lastName: form.lastName.trim(), middleName: form.middleName || '', dateOfBirth: form.dateOfBirth || null, gender: form.gender || null, iin: form.iin || '', email: form.email || '' })
      setProfile(value); setForm(value); setEditing(false); setSaved(true)
      focusEdit()
      // The profile update is already successful; a failed header refresh must
      // not misreport the save or prompt the patient to submit it again.
      refreshUser().catch(() => {})
    } catch (requestError) { setError(requestError) }
    finally { pending.current = false; setBusy(false) }
  }
  if (!profile) return <ProfileLoading error={error && translateError(error, t)} retry={load} />
  const dateOfBirth = profile.dateOfBirth ? new Intl.DateTimeFormat(localeFor(language), { dateStyle: 'long' }).format(new Date(`${profile.dateOfBirth}T12:00:00`)) : ''
  const gender = profile.gender === 'MALE' ? t('patient.male') : profile.gender === 'FEMALE' ? t('patient.female') : t('common.notSpecified')
  return <section className="page-shell account-page">
    <ProfileHeader person={profile} role="PATIENT" title={t('patient.profileTitle')} description={t('patient.profileDescription')} action={<Button ref={editButton} variant="secondary" disabled={busy} onClick={() => { if (editing) cancel(); else { setForm(profile); setEditing(true); setSaved(false) } }}>{t(editing ? 'common.cancel' : 'common.edit')}</Button>} />
    {error && <Alert tone="danger">{translateError(error, t)}</Alert>}{saved && <Alert tone="success">{t('patient.profileSaved')}</Alert>}
    <div className="account-grid"><div className="account-stack">
      <ProfileSection title={t('patient.personalData')}>
        {editing ? <form onSubmit={save} className="account-editor" aria-busy={busy}><p className="account-edit-note">{t('authFlow.requiredNote')}</p><fieldset className="account-fieldset" disabled={busy}><legend className="sr-only">{t('patient.personalData')}</legend><div className="form-grid">
          <Input label={t('patient.firstName')} value={form.firstName || ''} onChange={change('firstName')} autoComplete="given-name" pattern={'.*\\S.*'} title={t('authFlow.required')} autoFocus required />
          <Input label={t('patient.lastName')} value={form.lastName || ''} onChange={change('lastName')} autoComplete="family-name" pattern={'.*\\S.*'} title={t('authFlow.required')} required />
          <Input label={t('patient.middleName')} value={form.middleName || ''} onChange={change('middleName')} autoComplete="additional-name" />
          <Input label={t('patient.birthDate')} type="date" value={form.dateOfBirth || ''} onChange={change('dateOfBirth')} autoComplete="bday" />
          <Select label={t('patient.gender')} value={form.gender || ''} onChange={change('gender')} emptyLabel={t('common.notSpecified')} options={['MALE', 'FEMALE', 'UNKNOWN'].map((value) => [value, value === 'MALE' ? t('patient.male') : value === 'FEMALE' ? t('patient.female') : t('common.notSpecified')])} />
          <Input label={t('patient.iin')} value={form.iin || ''} onChange={change('iin')} inputMode="numeric" minLength={12} maxLength={12} pattern="[0-9]{12}" hint={t('patient.iinValidation')} />
          <Input label={t('patient.email')} type="email" value={form.email || ''} onChange={change('email')} autoComplete="email" />
        </div><p className="account-edit-note">{t('patient.phoneLogin')}</p><div className="form-actions"><Button type="submit" busy={busy}>{t(busy ? 'patient.saving' : 'patient.saveChanges')}</Button><Button variant="secondary" onClick={cancel}>{t('common.cancel')}</Button></div></fieldset></form> : <ProfileFacts items={[[t('staff.fullName'), personName(profile)], [t('patient.birthDate'), dateOfBirth], [t('patient.age'), profile.age == null ? '' : `${profile.age} ${t('staff.years')}`], [t('patient.gender'), gender], [t('patient.iin'), maskedIin(profile.iin, profile.maskedIin)], [t('patient.email'), profile.email]]} />}
      </ProfileSection>
      <ProfileSection title={t('patient.medicalCare')} icon="building"><ProfileFacts items={[[t('patient.region'), profile.preferredRegion?.name], [t('patient.city'), profile.preferredCity?.name], [t('patient.clinic'), profile.clinic?.name], [t('staff.address'), profile.address]]} /><ProfileLinks items={[["/patient/location", t('patient.changeCity'), 'pin']]} /></ProfileSection>
    </div><div className="account-stack">
      <DependentSummary />
      <MedicalSummary />
      <ProfileSection title={t('account.careLinks')} icon="heart"><p>{t('account.careDescription')}</p><ProfileLinks items={[["/patient/my-appointments", t('nav.myAppointments'), 'calendar'], ['/patient/medical-record', t('nav.medicalRecord'), 'heart'], ['/patient/documents', t('nav.documents'), 'file']]} /></ProfileSection>
    </div></div>
    <AccountSettings />
  </section>
}

function MedicalSummary() {
  const { t } = useTranslation()
  return <ProfileSection title={t('account.medicalSummary')} icon="medical"><p>{t('account.summaryDescription')}</p><div className="account-medical-summary">
    <MedicalSummaryItem loader={getPatientDiagnoses} href="/patient/diagnoses" label={t('nav.diagnoses')} />
    <MedicalSummaryItem loader={getPatientPrescriptions} href="/patient/prescriptions" label={t('nav.prescriptions')} />
    <MedicalSummaryItem loader={getPatientLabResults} href="/patient/lab-results" label={t('nav.labResults')} />
  </div></ProfileSection>
}

function MedicalSummaryItem({ loader, href, label }) {
  const { t } = useTranslation()
  const [count, setCount] = useState(null)
  const [error, setError] = useState(null)
  const load = () => { setError(null); loader().then((items) => { if (!Array.isArray(items)) throw new Error('Unexpected collection'); setCount(items.length) }).catch(setError) }
  useEffect(() => { load() }, [loader])
  return <div><ProfileLinks items={[[href, label, 'file']]} />{error ? <div><p role="alert">{translateError(error, t)}</p><Button variant="ghost" onClick={load}>{t('ui.retry')}</Button></div> : count == null ? <LoadingSpinner /> : <span className="account-medical-summary__count">{t('account.recordCount', { count })}</span>}</div>
}

function DependentSummary() {
  const { t } = useTranslation()
  const [people, setPeople] = useState(null)
  const [error, setError] = useState(null)
  const load = () => { setError(null); getDependents().then(setPeople).catch(setError) }
  useEffect(() => { load() }, [])
  return <ProfileSection title={t('patient.dependentsTitle')} icon="users">
    {error ? <ErrorState description={translateError(error, t)} onRetry={load} /> : !people ? <LoadingSpinner /> : !people.length ? <EmptyState compact title={t('patient.noDependents')} description={t('patient.noDependentsDescription')} /> : people.slice(0, 3).map((person) => <div className="account-person" key={person.id}><InitialsAvatar person={person} /><div><strong>{personName(person)}</strong><span>{person.relationshipLabel || t(`relationship.${person.relationship}`)}</span></div></div>)}
    <ProfileLinks items={[["/patient/dependents", t('account.manageDependents'), 'users']]} />
  </ProfileSection>
}
