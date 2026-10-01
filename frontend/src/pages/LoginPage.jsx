import { useState } from 'react'
import { ApiError } from '../api/apiClient.js'
import { useAuth } from '../auth/AuthContext.jsx'
import { navigate, useLocation } from '../utils/navigation.js'
import { normalizePhone } from '../utils/phone.js'
import { pathForRole } from '../components/RouteGuards.jsx'
import { isDevelopment } from '../config/environment.js'
import { useTranslation } from '../i18n/index.jsx'

function LoginPage() {
  const { login } = useAuth()
  const { t } = useTranslation()
  const location = useLocation()
  const [form, setForm] = useState({ phone: '', password: '' })
  const [error, setError] = useState('')
  const [submitting, setSubmitting] = useState(false)
  const registered = new URLSearchParams(location.search).get('registered') === '1'

  const updateField = (event) => {
    setForm((current) => ({ ...current, [event.target.name]: event.target.value }))
    setError('')
  }

  const submit = async (event) => {
    event.preventDefault()
    setError('')
    setSubmitting(true)
    try {
      const user = await login({ ...form, phone: normalizePhone(form.phone) })
      navigate(pathForRole(user.role), { replace: true })
    } catch (requestError) {
      setError(requestError instanceof ApiError ? requestError.message : requestError.message || t('common.error'))
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <section className="form-card auth-card">
      <div className="eyebrow">QamqorMed</div>
      <h1>{t('auth.login')}</h1>
      <p className="form-description">{t('auth.loginDescription')}</p>
      {registered && <p className="success-message">{t('auth.registered')}</p>}
      {error && <p className="error-message" role="alert">{error}</p>}
      <form onSubmit={submit} noValidate>
        <label className="field-label" htmlFor="login-phone">{t('auth.phone')}</label>
        <input
          id="login-phone"
          name="phone"
          type="tel"
          autoComplete="tel"
          placeholder="+77001234567"
          value={form.phone}
          onChange={updateField}
          required
        />
        <label className="field-label" htmlFor="login-password">{t('auth.password')}</label>
        <input
          id="login-password"
          name="password"
          type="password"
          autoComplete="current-password"
          value={form.password}
          onChange={updateField}
          required
        />
        <button className="button" type="submit" disabled={submitting}>
          {submitting ? t('auth.signingIn') : t('auth.signIn')}
        </button>
      </form>

      {isDevelopment && (
        <div className="dev-login-entry">
          <p className="dev-login-entry__title">DEV Panel</p>
          <p className="dev-login-entry__description">Быстрый поиск и вход под DEMO-пользователями.</p>
          <p className="dev-login-entry__hint">Поиск по ФИО, ID, клинике и специальности.</p>
          <button className="button button--secondary" type="button" onClick={() => navigate('/dev')}>Открыть DEV Panel</button>
        </div>
      )}

      <p className="form-footer">{t('auth.noAccount')} <button className="text-button" type="button" onClick={() => navigate('/register')}>{t('auth.register')}</button></p>
    </section>
  )
}

export default LoginPage
