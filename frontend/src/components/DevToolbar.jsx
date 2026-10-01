import { useEffect, useState } from 'react'
import { devGetRelation } from '../api/apiClient.js'
import { useAuth } from '../auth/AuthContext.jsx'
import { isDevelopment } from '../config/environment.js'
import { navigate } from '../utils/navigation.js'

function DevToolbar() {
  const { user, loginAsDevUser, logout } = useAuth()
  const [relation, setRelation] = useState(null)
  const [switching, setSwitching] = useState(false)

  useEffect(() => {
    if (!isDevelopment || !user || !['DOCTOR', 'NURSE'].includes(user.role)) {
      setRelation(null)
      return undefined
    }
    devGetRelation(user.id).then(setRelation).catch(() => setRelation(null))
    return undefined
  }, [user])

  if (!isDevelopment || !user) return null
  const targetUserId = user.role === 'DOCTOR' ? relation?.nurseUserId : relation?.doctorUserId
  const targetLabel = user.role === 'DOCTOR' ? 'Switch to Nurse' : 'Switch to Doctor'

  const switchRole = async () => {
    if (!targetUserId) return
    setSwitching(true)
    try {
      const nextUser = await loginAsDevUser(targetUserId)
      navigate(nextUser.role === 'DOCTOR' ? '/doctor' : '/nurse', { replace: true })
    } finally {
      setSwitching(false)
    }
  }

  return <aside className="dev-toolbar" aria-label="Development toolbar"><div className="dev-toolbar-status"><span className="dev-pulse" /> DEV MODE</div><div className="dev-toolbar-user"><strong>{user.role}</strong><span>{user.firstName} {user.lastName}</span></div><button type="button" onClick={() => navigate('/dev')}>Dev Panel</button>{['DOCTOR', 'NURSE'].includes(user.role) && <button type="button" onClick={switchRole} disabled={!targetUserId || switching}>{switching ? 'Switching…' : targetLabel}</button>}<button className="dev-toolbar-logout" type="button" onClick={() => logout()}>Logout</button></aside>
}

export default DevToolbar
