import { useState } from 'react'
import { ApiError } from '../api/apiClient.js'
import { useAuth } from '../auth/AuthContext.jsx'
import { navigate, useLocation } from '../utils/navigation.js'
import { normalizePhone } from '../utils/phone.js'
import { pathForRole } from '../components/RouteGuards.jsx'
import { useTranslation } from '../i18n/index.jsx'

const demoUsers = [
  { labelKey: 'auth.demoPatient', phone: '+77000000001', password: 'Patient123!' },
  { labelKey: 'auth.demoDoctor', phone: '+77000000002', password: 'Doctor123!' },
  { labelKey: 'auth.demoNurse', phone: '+77000000003', password: 'Nurse123!' },
  { labelKey: 'auth.demoAdmin', phone: '+77000000004', password: 'Admin123!' },
]

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

      {import.meta.env.DEV && (
        <div className="demo-panel">
          <p className="demo-title">{t('auth.demoLogin')}</p>
          <p className="demo-description">{t('auth.demoDescription')}</p>
          <div className="demo-buttons">
            {demoUsers.map((demo) => (
              <button
                className="button button--demo"
                type="button"
                key={demo.phone}
                onClick={() => setForm({ phone: demo.phone, password: demo.password })}
              >
                {t(demo.labelKey)}
              </button>
            ))}
          </div>
        </div>
      )}

      <p className="form-footer">{t('auth.noAccount')} <button className="text-button" type="button" onClick={() => navigate('/register')}>{t('auth.register')}</button></p>
    </section>
  )
}

export default LoginPage
