import { useAuth } from '../auth/AuthContext.jsx'
import { navigate } from '../utils/navigation.js'
import { normalizePhone } from '../utils/phone.js'
import { useTranslation } from '../i18n/index.jsx'
import AuthFrame from '../components/account/AuthFrame.jsx'
import { PasswordInput, PasswordRequirements, useAccountForm } from '../components/account/AuthForm.jsx'
import { Input } from '../components/ui/Fields.jsx'
import { Alert, Button } from '../components/ui/Feedback.jsx'
import AppLink from '../components/AppLink.jsx'

export default function RegisterPage() {
  const { register } = useAuth()
  const { t } = useTranslation()
  const form = useAccountForm({ firstName: '', lastName: '', phone: '', password: '', confirmPassword: '' }, 'register')
  return <AuthFrame register title={t('auth.createAccount')} description={t('authFlow.registrationDescription')}>
    <p className="auth-required-note">{t('authFlow.requiredNote')}</p>
    {form.error && <div ref={form.summaryRef} tabIndex={-1}><Alert tone="danger">{form.error}</Alert></div>}
    <form ref={form.formRef} noValidate aria-busy={form.busy} onSubmit={form.submit(async (values) => {
      await register({ ...values, firstName: values.firstName.trim(), lastName: values.lastName.trim(), phone: normalizePhone(values.phone) })
      navigate('/login?registered=1', { replace: true })
    })}>
      <fieldset disabled={form.busy} className="account-fieldset"><legend>{t('authFlow.personalGroup')}</legend>
        <div className="auth-name-grid"><Input {...form.field('firstName')} label={t('auth.firstName')} autoComplete="given-name" maxLength={100} required /><Input {...form.field('lastName')} label={t('auth.lastName')} autoComplete="family-name" maxLength={100} required /></div>
        <Input {...form.field('phone')} label={t('auth.phone')} type="tel" inputMode="tel" autoComplete="tel" placeholder="+7 700 123 45 67" hint={t('authFlow.phoneHint')} required />
      </fieldset>
      <fieldset disabled={form.busy} className="account-fieldset"><legend>{t('authFlow.securityGroup')}</legend>
        <PasswordInput {...form.field('password')} label={t('auth.password')} autoComplete="new-password" maxLength={72} required />
        <PasswordRequirements value={form.values.password} />
        <PasswordInput {...form.field('confirmPassword')} label={t('auth.confirmPassword')} autoComplete="new-password" maxLength={72} required />
      </fieldset>
      <Button type="submit" busy={form.busy} className="auth-submit">{t(form.busy ? 'auth.creatingAccount' : 'auth.createAccount')}</Button>
    </form>
    <p className="auth-panel__switch">{t('auth.alreadyAccount')} <AppLink href="/login">{t('auth.signIn')}</AppLink></p>
  </AuthFrame>
}
