import { useState, useSyncExternalStore } from 'react'
import { useTranslation } from '../../i18n/index.jsx'
import { useTheme } from '../../theme/ThemeProvider.jsx'
import { changePassword } from '../../api/apiClient.js'
import { PasswordInput, PasswordRequirements, useAccountForm } from './AuthForm.jsx'
import { Select, Checkbox } from '../ui/Fields.jsx'
import { Alert, Badge, Button, ErrorState, PageSkeleton } from '../ui/Feedback.jsx'
import AppLink from '../AppLink.jsx'
import Icon from '../ui/Icon.jsx'

export const personName = (person) => [person?.lastName, person?.firstName, person?.middleName].filter(Boolean).join(' ')

export function InitialsAvatar({ person }) {
  return <div className="account-avatar" aria-hidden="true">{`${person?.firstName?.[0] || ''}${person?.lastName?.[0] || ''}`.toUpperCase() || '?'}</div>
}

export function ProfileHeader({ person, role, title, description, subtitle, action }) {
  const { t } = useTranslation()
  return <><header className="page-heading page-heading--row"><div><span className="eyebrow">{t(`roles.${role}`)}</span><h1>{title}</h1><p className="form-description">{description}</p></div>{action}</header>
    <section className="account-identity" aria-label={t('account.identity')}><InitialsAvatar person={person} /><div className="account-identity__copy"><Badge tone="info">{t(`roles.${role}`)}</Badge><h2>{personName(person) || t('common.notSpecified')}</h2>{subtitle && <p>{subtitle}</p>}<p>{person.phone || t('common.notSpecified')}</p></div>{typeof person.active === 'boolean' && <Badge tone={person.active ? 'success' : 'neutral'}>{t(person.active ? 'common.active' : 'common.inactive')}</Badge>}</section></>
}

export function ProfileSection({ title, icon = 'user', children, ...props }) {
  return <section className="account-section" {...props}><header className="account-section__heading"><Icon name={icon} size={21} /><h2>{title}</h2></header>{children}</section>
}

export function ProfileFacts({ items }) {
  const { t } = useTranslation()
  return <dl className="account-facts">{items.map(([label, value]) => <div key={label}><dt>{label}</dt><dd>{value === 0 ? 0 : value || t('common.notSpecified')}</dd></div>)}</dl>
}

export function ProfileLinks({ items }) {
  return <div className="account-shortcuts">{items.map(([href, label, icon = 'arrow']) => <AppLink href={href} key={href}><Icon name={icon} size={20} /><span>{label}</span><Icon name="chevron" size={16} /></AppLink>)}</div>
}

export function ProfileLoading({ error, retry }) {
  return <section className="page-shell account-page">{error ? <ErrorState description={error} onRetry={retry} /> : <PageSkeleton />}</section>
}

export function AccountSettings() {
  const { t, language, languages, changeLanguage } = useTranslation()
  const { preference, setPreference } = useTheme()
  const store = window.QamqorMedAccessibility
  const { textSize, contrast } = useSyncExternalStore(store.subscribe, store.getSnapshot)
  return <ProfileSection title={t('account.preferences')} icon="accessibility">
    <div className="account-preferences"><Select label={t('theme.choose')} value={preference} onChange={(event) => setPreference(event.target.value)} allowEmpty={false} options={['light', 'dark', 'system'].map((value) => [value, t(`theme.${value}`)])} /><Select label={t('common.language')} value={language} onChange={(event) => changeLanguage(event.target.value)} allowEmpty={false} options={languages.map((item) => [item.code, item.label])} /><Checkbox label={t('accessibility.largeText')} role="switch" checked={textSize === 'large'} onChange={(event) => store.setPreferences({ textSize: event.target.checked ? 'large' : 'normal' })} /><Checkbox label={t('accessibility.highContrast')} role="switch" checked={contrast === 'high'} onChange={(event) => store.setPreferences({ contrast: event.target.checked ? 'high' : 'normal' })} /></div>
    <PasswordSettings />
  </ProfileSection>
}

function PasswordSettings() {
  const { t } = useTranslation()
  const [expanded, setExpanded] = useState(false)
  const [saved, setSaved] = useState(false)
  const form = useAccountForm({ currentPassword: '', newPassword: '', confirmNewPassword: '' }, 'password')
  return <div className="account-security"><div className="account-security__header"><div><h3>{t('account.security')}</h3><p>{t('account.securityDescription')}</p></div><Button variant="secondary" disabled={form.busy} aria-expanded={expanded} aria-controls="account-password-form" onClick={() => { setExpanded((value) => !value); setSaved(false); form.reset() }}>{t(expanded ? 'common.cancel' : 'account.changePassword')}</Button></div>
    {saved && <Alert tone="success">{t('account.passwordSaved')}</Alert>}
    {expanded && <form id="account-password-form" ref={form.formRef} noValidate aria-busy={form.busy} onSubmit={form.submit(async (values) => { await changePassword(values); form.reset(); setExpanded(false); setSaved(true) })}>
      {form.error && <div ref={form.summaryRef} tabIndex={-1}><Alert tone="danger">{form.error}</Alert></div>}
      <fieldset disabled={form.busy} className="account-fieldset"><legend className="sr-only">{t('account.changePassword')}</legend>
        <PasswordInput {...form.field('currentPassword')} label={t('account.currentPassword')} autoComplete="current-password" required />
        <PasswordInput {...form.field('newPassword')} label={t('account.newPassword')} autoComplete="new-password" maxLength={72} required />
        <PasswordRequirements value={form.values.newPassword} />
        <PasswordInput {...form.field('confirmNewPassword')} label={t('account.confirmNewPassword')} autoComplete="new-password" maxLength={72} required />
        <Button type="submit" busy={form.busy}>{t(form.busy ? 'patient.saving' : 'account.changePassword')}</Button>
      </fieldset>
    </form>}
  </div>
}
