import AppLayout from './layouts/AppLayout.jsx'
import { AuthProvider, useAuth } from './auth/AuthContext.jsx'
import { pathForRole, ProtectedRoute, Redirect, RoleRoute } from './components/RouteGuards.jsx'
import DashboardPage from './pages/DashboardPage.jsx'
import AdminDashboardPage from './pages/AdminDashboardPage.jsx'
import DoctorsPage from './pages/DoctorsPage.jsx'
import DoctorDetailPage from './pages/DoctorDetailPage.jsx'
import LoginPage from './pages/LoginPage.jsx'
import RegisterPage from './pages/RegisterPage.jsx'
import PlaceholderPage from './pages/PlaceholderPage.jsx'
import ForbiddenPage from './pages/ForbiddenPage.jsx'
import NotFoundPage from './pages/NotFoundPage.jsx'
import DevPanelPage from './pages/DevPanelPage.jsx'
import {
  DependentDetailPage,
  PatientAppointmentsPage,
  PatientBookingPage,
  PatientLocationPage,
  PatientDependentsPage,
  PatientMedicalHistoryPage,
  PatientProfilePage,
} from './pages/PatientPages.jsx'
import { isDevelopment } from './config/environment.js'
import { useLocation } from './utils/navigation.js'
import { I18nProvider } from './i18n/index.jsx'
import { useTranslation } from './i18n/index.jsx'
import StaffWorkflowPage from './pages/StaffWorkflowPages.jsx'

function RoleDashboard({ role }) {
  return (
    <ProtectedRoute>
      <RoleRoute role={role}>
        <DashboardPage />
      </RoleRoute>
    </ProtectedRoute>
  )
}

function RolePage({ role, children }) {
  return (
    <ProtectedRoute>
      <RoleRoute role={role}>{children}</RoleRoute>
    </ProtectedRoute>
  )
}

const roleSectionRoutes = {
  PATIENT: {
    '/patient/appointments': 'bookAppointment',
    '/patient/my-appointments': 'myAppointments',
    '/patient/dependents': 'dependents',
    '/patient/medical-record': 'medicalRecord',
    '/patient/documents': 'documents',
    '/patient/profile': 'profile',
  },
  DOCTOR: {
    '/doctor/schedule': 'schedule',
    '/doctor/patients': 'patients',
    '/doctor/appointments': 'appointments',
    '/doctor/medical-records': 'medicalRecords',
    '/doctor/referrals': 'referrals',
    '/doctor/profile': 'profile',
  },
  NURSE: {
    '/nurse/schedule': 'schedule',
    '/nurse/today': 'todayPatients',
    '/nurse/vitals': 'vitals',
    '/nurse/appointments': 'appointments',
    '/nurse/profile': 'profile',
  },
}

function AppContent() {
  const location = useLocation()
  const { isAuthenticated, user, loading } = useAuth()
  const { t } = useTranslation()

  let page
  const needsPatientLocation = isAuthenticated && user?.role === 'PATIENT' && !user?.preferredCity && location.pathname !== '/patient/location'

  if (needsPatientLocation) {
    page = <RolePage role="PATIENT"><PatientLocationPage /></RolePage>
  } else if (location.pathname === '/') {
    page = isAuthenticated && user?.role ? <Redirect to={pathForRole(user.role)} /> : <Redirect to="/login" />
  } else if (location.pathname === '/login') {
    page = loading ? <p className="route-message">{t('auth.checking')}</p> : isAuthenticated && user?.role ? <Redirect to={pathForRole(user.role)} /> : <LoginPage />
  } else if (location.pathname === '/register') {
    page = <RegisterPage />
  } else if (isDevelopment && (location.pathname === '/dev' || location.pathname === '/dev/login')) {
    page = <DevPanelPage />
  } else if (location.pathname === '/patient') {
    page = <RoleDashboard role="PATIENT" />
  } else if (location.pathname === '/patient/location') {
    page = <RolePage role="PATIENT"><PatientLocationPage /></RolePage>
  } else if (location.pathname === '/patient/appointments') {
    page = <RolePage role="PATIENT"><PatientBookingPage /></RolePage>
  } else if (location.pathname === '/patient/my-appointments') {
    page = <RolePage role="PATIENT"><PatientAppointmentsPage /></RolePage>
  } else if (location.pathname === '/patient/dependents') {
    page = <RolePage role="PATIENT"><PatientDependentsPage /></RolePage>
  } else if (location.pathname.startsWith('/patient/dependents/')) {
    page = <RolePage role="PATIENT"><DependentDetailPage id={location.pathname.split('/')[3]} /></RolePage>
  } else if (location.pathname === '/patient/medical-record' || location.pathname === '/patient/medical-history') {
    page = <RolePage role="PATIENT"><PatientMedicalHistoryPage /></RolePage>
  } else if (location.pathname === '/patient/profile') {
    page = <RolePage role="PATIENT"><PatientProfilePage /></RolePage>
  } else if (location.pathname === '/doctor/appointments' || location.pathname === '/doctor/patients' || location.pathname === '/doctor/medical-records') {
    page = <RolePage role="DOCTOR"><StaffWorkflowPage role="DOCTOR" /></RolePage>
  } else if (location.pathname === '/nurse/appointments' || location.pathname === '/nurse/today' || location.pathname === '/nurse/vitals') {
    page = <RolePage role="NURSE"><StaffWorkflowPage role="NURSE" /></RolePage>
  } else if (location.pathname === '/doctor') {
    page = <RoleDashboard role="DOCTOR" />
  } else if (location.pathname === '/nurse') {
    page = <RoleDashboard role="NURSE" />
  } else if (location.pathname === '/admin') {
    page = <RolePage role="ADMIN"><AdminDashboardPage /></RolePage>
  } else if (location.pathname === '/admin/profile') {
    page = <RolePage role="ADMIN"><PlaceholderPage sectionKey="profile" role="ADMIN" /></RolePage>
  } else if (location.pathname.startsWith('/admin/')) {
    page = <RolePage role="ADMIN"><AdminDashboardPage /></RolePage>
  } else if (location.pathname === '/doctors') {
    page = <ProtectedRoute><DoctorsPage /></ProtectedRoute>
  } else if (location.pathname.startsWith('/doctors/')) {
    page = <ProtectedRoute><DoctorDetailPage id={location.pathname.split('/')[2]} /></ProtectedRoute>
  } else if (Object.entries(roleSectionRoutes).some(([, routes]) => routes[location.pathname])) {
    const [sectionRole, routes] = Object.entries(roleSectionRoutes).find(([, roleRoutes]) => roleRoutes[location.pathname])
    page = <RolePage role={sectionRole}><PlaceholderPage sectionKey={routes[location.pathname]} role={sectionRole} /></RolePage>
  } else if (location.pathname === '/403') {
    page = <ProtectedRoute><ForbiddenPage /></ProtectedRoute>
  } else if (isAuthenticated && !loading && user?.role) {
    page = <NotFoundPage />
  } else {
    page = isAuthenticated ? <NotFoundPage /> : <Redirect to="/login" />
  }

  return <AppLayout>{page}</AppLayout>
}

function App() {
  return <I18nProvider><AuthProvider><AppContent /></AuthProvider></I18nProvider>
}

export default App
