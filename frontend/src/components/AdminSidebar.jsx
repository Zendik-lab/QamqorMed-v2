import { useAuth } from '../auth/AuthContext.jsx'
import { useLocation } from '../utils/navigation.js'
import { workspaceMenus } from '../config/navigation.js'
import WorkspaceSidebar from './WorkspaceSidebar.jsx'

export const adminMenuItems = workspaceMenus.ADMIN

// Compatibility wrapper; every role uses the same navigation component.
export default function AdminSidebar({ onClose, ...props }) {
  const { logout } = useAuth()
  const { pathname } = useLocation()
  return <WorkspaceSidebar {...props} role="ADMIN" pathname={pathname} onNavigate={onClose} onLogout={logout} />
}
