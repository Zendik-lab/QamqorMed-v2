import { useState } from 'react'
import { useAuth } from '../auth/AuthContext.jsx'
import { navigate } from '../utils/navigation.js'
import { normalizePhone } from '../utils/phone.js'
import { translateError, useTranslation } from '../i18n/index.jsx'

const initialForm = { firstName: '', lastName: '', phone: '', password: '', confirmPassword: '' }

function RegisterPage() {
  const { register } = useAuth()
  const { t } = useTranslation()
  const [form, setForm] = useState(initialForm)
  const [error, setError] = useState('')
  const [fieldErrors, setFieldErrors] = useState({})
  const [submitting, setSubmitting] = useState(false)

  const updateField = (event) => {
    setForm((current) => ({ ...current, [event.target.name]: event.target.value }))
    setError('')
    setFieldErrors((current) => ({ ...current, [event.target.name]: '' }))
  }

  const submit = async (event) => {
    event.preventDefault()
    setError('')
    setFieldErrors({})
    setSubmitting(true)
    try {
      await register({ ...form, phone: normalizePhone(form.phone) })
      navigate('/login?registered=1')
    } catch (requestError) {
      const message = translateError(requestError, t)
      setError(message)
      const fieldErrors = Object.fromEntries(Object.entries(requestError?.errors || {}).map(([field, value]) => [field, translateError({ message: String(value), status: requestError?.status }, t, 'errors.validation')]))
      setFieldErrors(Object.keys(fieldErrors).length ? fieldErrors : { phone: message })
    } finally {
      setSubmitting(false)
    }
  }

  const field = (name, label, type = 'text', autoComplete = undefined) => (
    <div className="field-group" key={name}>
      <label className="field-label" htmlFor={`register-${name}`}>{label}</label>
      <input
        id={`register-${name}`}
        name={name}
        type={type}
        autoComplete={autoComplete}
        value={form[name]}
        onChange={updateField}
        required
      />
      {fieldErrors[name] && <span className="field-error">{fieldErrors[name]}</span>}
    </div>
  )

  return (
    <section className="form-card auth-card">
      <div className="eyebrow">{t('auth.patientOnly')}</div>
      <h1>{t('auth.createAccount')}</h1>
      <p className="form-description">{t('auth.registrationRole')}</p>
      {error && <p className="error-message" role="alert">{error}</p>}
      <form onSubmit={submit} noValidate>
        {field('firstName', t('auth.firstName'), 'text', 'given-name')}
        {field('lastName', t('auth.lastName'), 'text', 'family-name')}
        {field('phone', t('auth.phone'), 'tel', 'tel')}
        {field('password', t('auth.password'), 'password', 'new-password')}
        {field('confirmPassword', t('auth.confirmPassword'), 'password', 'new-password')}
        <p className="hint-text">{t('auth.passwordHint')}</p>
        <button className="button" type="submit" disabled={submitting}>
          {submitting ? t('auth.creatingAccount') : t('auth.register')}
        </button>
      </form>
      <p className="form-footer">{t('auth.alreadyAccount')} <button className="text-button" type="button" onClick={() => navigate('/login')}>{t('auth.signIn')}</button></p>
    </section>
  )
}

export default RegisterPage
