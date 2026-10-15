import { useAuth } from '../auth/AuthContext.jsx'
import { navigate, useLocation } from '../utils/navigation.js'
import { normalizePhone } from '../utils/phone.js'
import { pathForRole } from '../components/RouteGuards.jsx'
import { isDevelopment } from '../config/environment.js'
import { useTranslation } from '../i18n/index.jsx'
import AuthFrame from '../components/account/AuthFrame.jsx'
import { PasswordInput, useAccountForm } from '../components/account/AuthForm.jsx'
import { Input } from '../components/ui/Fields.jsx'
import { Alert, Button } from '../components/ui/Feedback.jsx'
import AppLink from '../components/AppLink.jsx'

export default function LoginPage() {
  const { login } = useAuth()
  const { t } = useTranslation()
  const location = useLocation()
  const form = useAccountForm({ phone: '', password: '' }, 'login')
  const registered = new URLSearchParams(location.search).get('registered') === '1'
  return <AuthFrame title={t('auth.login')} description={t('auth.loginDescription')}>
    {registered && <Alert tone="success">{t('auth.registered')}</Alert>}
    {form.error && <div ref={form.summaryRef} tabIndex={-1}><Alert tone="danger">{form.error}</Alert></div>}
    <form ref={form.formRef} noValidate onSubmit={form.submit(async (values) => {
      const user = await login({ ...values, phone: normalizePhone(values.phone) })
      navigate(pathForRole(user.role), { replace: true })
    })} aria-busy={form.busy}>
      <fieldset disabled={form.busy} className="account-fieldset">
        <legend className="sr-only">{t('auth.login')}</legend>
        <Input id="login-phone" {...form.field('phone')} label={t('auth.phone')} type="tel" inputMode="tel" autoComplete="tel" placeholder="+7 700 123 45 67" hint={t('authFlow.phoneHint')} required />
        <PasswordInput id="login-password" {...form.field('password')} label={t('auth.password')} autoComplete="current-password" required />
        <Button type="submit" busy={form.busy} className="auth-submit">{t(form.busy ? 'auth.signingIn' : 'auth.signIn')}</Button>
      </fieldset>
    </form>
    <p className="auth-panel__switch">{t('auth.noAccount')} <AppLink href="/register">{t('auth.register')}</AppLink></p>
    {isDevelopment && <aside className="auth-dev-entry"><span className="eyebrow">{t('dev.kicker')}</span><AppLink href="/dev">{t('dev.title')}</AppLink></aside>}
  </AuthFrame>
}
