import { useEffect } from 'react'
import { useAuth } from '../auth/AuthContext.jsx'
import { navigate } from '../utils/navigation.js'
import { useTranslation } from '../i18n/index.jsx'

const rolePaths = {
  PATIENT: '/patient',
  DOCTOR: '/doctor',
  NURSE: '/nurse',
  ADMIN: '/admin',
}

export function Redirect({ to, message }) {
  useEffect(() => navigate(to), [to])
  return <p className="route-message">{message || '…'}</p>
}

export function ProtectedRoute({ children }) {
  const { loading, isAuthenticated } = useAuth()
  const { t } = useTranslation()

  if (loading) {
    return <p className="route-message">{t('auth.checking')}</p>
  }
  if (!isAuthenticated) {
    return <Redirect to="/login" message={t('auth.redirecting')} />
  }
  return children
}

export function RoleRoute({ role, children }) {
  const { user } = useAuth()
  if (!user || user.role !== role) {
    return <Redirect to="/403" message="" />
  }
  return children
}

export function pathForRole(role) {
  return rolePaths[role] || '/login'
}
