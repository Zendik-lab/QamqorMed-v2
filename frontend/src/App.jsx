import AppLayout from './layouts/AppLayout.jsx'
import { AuthProvider, useAuth } from './auth/AuthContext.jsx'
import { pathForRole, ProtectedRoute, Redirect, RoleRoute } from './components/RouteGuards.jsx'
import DashboardPage from './pages/DashboardPage.jsx'
import LoginPage from './pages/LoginPage.jsx'
import RegisterPage from './pages/RegisterPage.jsx'
import { useLocation } from './utils/navigation.js'

function RoleDashboard({ role }) {
  return (
    <ProtectedRoute>
      <RoleRoute role={role}>
        <DashboardPage />
      </RoleRoute>
    </ProtectedRoute>
  )
}

function AppContent() {
  const location = useLocation()
  const { isAuthenticated, user, loading } = useAuth()

  let page
  if (location.pathname === '/') {
    page = <Redirect to="/login" />
  } else if (location.pathname === '/login') {
    page = <LoginPage />
  } else if (location.pathname === '/register') {
    page = <RegisterPage />
  } else if (location.pathname === '/patient') {
    page = <RoleDashboard role="PATIENT" />
  } else if (location.pathname === '/doctor') {
    page = <RoleDashboard role="DOCTOR" />
  } else if (location.pathname === '/nurse') {
    page = <RoleDashboard role="NURSE" />
  } else if (location.pathname === '/admin') {
    page = <RoleDashboard role="ADMIN" />
  } else if (isAuthenticated && !loading && user?.role) {
    page = <Redirect to={pathForRole(user.role)} />
  } else {
    page = <Redirect to="/login" />
  }

  return <AppLayout>{page}</AppLayout>
}

function App() {
  return <AuthProvider><AppContent /></AuthProvider>
}

export default App
