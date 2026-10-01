import { useState } from 'react'
import { ApiError } from '../api/apiClient.js'
import { useAuth } from '../auth/AuthContext.jsx'
import { navigate } from '../utils/navigation.js'
import { normalizePhone } from '../utils/phone.js'

const initialForm = { firstName: '', lastName: '', phone: '', password: '', confirmPassword: '' }

function RegisterPage() {
  const { register } = useAuth()
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
      if (requestError instanceof ApiError) {
        setError(requestError.message)
        setFieldErrors(requestError.errors || {})
      } else {
        setError(requestError.message || 'Не удалось создать аккаунт')
        setFieldErrors({ phone: requestError.message || 'Некорректный номер телефона' })
      }
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
    <section className="form-card">
      <div className="eyebrow">Только для пациента</div>
      <h1>Создать аккаунт</h1>
      <p className="form-description">После регистрации аккаунт получает роль PATIENT.</p>
      {error && <p className="error-message" role="alert">{error}</p>}
      <form onSubmit={submit} noValidate>
        {field('firstName', 'Имя', 'text', 'given-name')}
        {field('lastName', 'Фамилия', 'text', 'family-name')}
        {field('phone', 'Номер телефона', 'tel', 'tel')}
        {field('password', 'Пароль', 'password', 'new-password')}
        {field('confirmPassword', 'Повтор пароля', 'password', 'new-password')}
        <p className="hint-text">Минимум 8 символов, буква и цифра.</p>
        <button className="button" type="submit" disabled={submitting}>
          {submitting ? 'Создание аккаунта...' : 'Зарегистрироваться'}
        </button>
      </form>
      <p className="form-footer">Уже есть аккаунт? <button className="text-button" type="button" onClick={() => navigate('/login')}>Войти</button></p>
    </section>
  )
}

export default RegisterPage
