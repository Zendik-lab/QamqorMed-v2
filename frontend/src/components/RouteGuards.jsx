import { useEffect } from 'react'
import { useAuth } from '../auth/AuthContext.jsx'
import { navigate } from '../utils/navigation.js'

const rolePaths = {
  PATIENT: '/patient',
  DOCTOR: '/doctor',
  NURSE: '/nurse',
  ADMIN: '/admin',
}

export function Redirect({ to, message }) {
  useEffect(() => navigate(to), [to])
  return message ? <p className="route-message">{message}</p> : null
}

export function ProtectedRoute({ children }) {
  const { loading, isAuthenticated } = useAuth()

  if (loading) {
    return <p className="route-message">Проверяем авторизацию...</p>
  }
  if (!isAuthenticated) {
    return <Redirect to="/login" message="Перенаправляем на страницу входа..." />
  }
  return children
}

export function RoleRoute({ role, children }) {
  const { user } = useAuth()
  if (!user || user.role !== role) {
    return <Redirect to={rolePaths[user?.role] || '/login'} message="Открываем ваш кабинет..." />
  }
  return children
}

export function pathForRole(role) {
  return rolePaths[role] || '/login'
}
