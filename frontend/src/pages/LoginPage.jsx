import { useState } from 'react'
import { ApiError } from '../api/apiClient.js'
import { useAuth } from '../auth/AuthContext.jsx'
import { navigate, useLocation } from '../utils/navigation.js'
import { normalizePhone } from '../utils/phone.js'
import { pathForRole } from '../components/RouteGuards.jsx'

const demoUsers = [
  { label: 'Пациент', phone: '+77000000001', password: 'Patient123!' },
  { label: 'Врач', phone: '+77000000002', password: 'Doctor123!' },
  { label: 'Медсестра', phone: '+77000000003', password: 'Nurse123!' },
  { label: 'Администратор', phone: '+77000000004', password: 'Admin123!' },
]

function LoginPage() {
  const { login } = useAuth()
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
      navigate(pathForRole(user.role))
    } catch (requestError) {
      setError(requestError instanceof ApiError ? requestError.message : requestError.message || 'Не удалось выполнить вход')
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <section className="form-card">
      <h1>Вход</h1>
      <p className="form-description">Войдите в свой аккаунт</p>
      {registered && <p className="success-message">Аккаунт создан. Теперь выполните вход.</p>}
      {error && <p className="error-message" role="alert">{error}</p>}
      <form onSubmit={submit} noValidate>
        <label className="field-label" htmlFor="login-phone">Номер телефона</label>
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
        <label className="field-label" htmlFor="login-password">Пароль</label>
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
          {submitting ? 'Вход...' : 'Войти'}
        </button>
      </form>

      {import.meta.env.DEV && (
        <div className="demo-panel">
          <p className="demo-title">Тестовый вход</p>
          <p className="demo-description">Только development, локальные вымышленные аккаунты.</p>
          <div className="demo-buttons">
            {demoUsers.map((demo) => (
              <button
                className="button button--demo"
                type="button"
                key={demo.phone}
                onClick={() => setForm({ phone: demo.phone, password: demo.password })}
              >
                {demo.label}
              </button>
            ))}
          </div>
        </div>
      )}

      <p className="form-footer">Нет аккаунта? <button className="text-button" type="button" onClick={() => navigate('/register')}>Зарегистрироваться</button></p>
    </section>
  )
}

export default LoginPage
