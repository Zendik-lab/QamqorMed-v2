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
  PatientClinicalDataPage,
  PatientDocumentsPage,
  PatientProfilePage,
} from './pages/PatientPages.jsx'
import { isDevelopment } from './config/environment.js'
import { useLocation } from './utils/navigation.js'
import { I18nProvider } from './i18n/index.jsx'
import { useTranslation } from './i18n/index.jsx'
import StaffWorkflowPage from './pages/StaffWorkflowPages.jsx'
import { DoctorProfilePage, NurseProfilePage } from './pages/StaffPages.jsx'
import StaffCalendar from './components/calendar/StaffCalendar.jsx'
import AdminSchedulePage from './pages/AdminSchedulePage.jsx'
import AdminDocumentTemplatesPage from './pages/AdminDocumentTemplatesPage.jsx'
import { DoctorPatientDetailPage, DoctorPatientsPage } from './pages/DoctorPatientsPages.jsx'
import PatientWaitlistPage from './pages/PatientWaitlistPage.jsx'
import { DoctorMedicalRecordsPage, DoctorReferralsPage, NurseTodayPage, NurseVitalsPage } from './pages/StaffSectionPages.jsx'

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
    '/doctor/today': 'today',
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

const adminSectionRoutes = {
  '/admin/regions': 'regions',
  '/admin/cities': 'cities',
  '/admin/clinics': 'clinics',
  '/admin/specialties': 'specialties',
  '/admin/document-templates': 'documentTemplates',
  '/admin/doctors': 'doctors',
  '/admin/nurses': 'nurses',
}

function AppContent() {
  const location = useLocation()
  const { isAuthenticated, user, loading } = useAuth()
  const { t } = useTranslation()

  let page
  const needsPatientLocation = isAuthenticated && user?.role === 'PATIENT' && !user?.preferredCity && location.pathname !== '/patient/location'
  const isDevPanel = isDevelopment && (location.pathname === '/dev' || location.pathname === '/dev/login')

  if (isDevPanel) {
    // The development account picker is the pre-authentication entry point.
    // Only the workflow section inside the panel requires an ADMIN session.
    page = <DevPanelPage />
  } else if (needsPatientLocation) {
    page = <RolePage role="PATIENT"><PatientLocationPage /></RolePage>
  } else if (location.pathname === '/') {
    page = isAuthenticated && user?.role ? <Redirect to={pathForRole(user.role)} /> : <Redirect to="/login" />
  } else if (location.pathname === '/login') {
    page = loading ? <p className="route-message">{t('auth.checking')}</p> : isAuthenticated && user?.role ? <Redirect to={pathForRole(user.role)} /> : <LoginPage />
  } else if (location.pathname === '/register') {
    page = <RegisterPage />
  } else if (location.pathname === '/patient') {
    page = <RoleDashboard role="PATIENT" />
  } else if (location.pathname === '/patient/location') {
    page = <RolePage role="PATIENT"><PatientLocationPage /></RolePage>
  } else if (location.pathname === '/patient/appointments') {
    page = <RolePage role="PATIENT"><PatientBookingPage /></RolePage>
  } else if (location.pathname === '/patient/my-appointments') {
    page = <RolePage role="PATIENT"><PatientAppointmentsPage /></RolePage>
  } else if (location.pathname === '/patient/waitlist') {
    page = <RolePage role="PATIENT"><PatientWaitlistPage /></RolePage>
  } else if (location.pathname === '/patient/dependents') {
    page = <RolePage role="PATIENT"><PatientDependentsPage /></RolePage>
  } else if (location.pathname.startsWith('/patient/dependents/')) {
    page = <RolePage role="PATIENT"><DependentDetailPage id={location.pathname.split('/')[3]} /></RolePage>
  } else if (location.pathname === '/patient/medical-record' || location.pathname === '/patient/medical-history') {
    page = <RolePage role="PATIENT"><PatientMedicalHistoryPage /></RolePage>
  } else if (location.pathname === '/patient/diagnoses') {
    page = <RolePage role="PATIENT"><PatientClinicalDataPage kind="diagnoses" /></RolePage>
  } else if (location.pathname === '/patient/medications') {
    page = <RolePage role="PATIENT"><PatientClinicalDataPage kind="medications" /></RolePage>
  } else if (['/patient/prescriptions', '/patient/lab-results', '/patient/referrals', '/patient/certificates'].includes(location.pathname)) {
    const legacyCategory = location.pathname === '/patient/lab-results' ? 'labs' : location.pathname === '/patient/referrals' ? 'referrals' : location.pathname === '/patient/certificates' ? 'certificates' : 'prescriptions'
    page = <RolePage role="PATIENT"><Redirect to={`/patient/documents?category=${legacyCategory}`} /></RolePage>
  } else if (location.pathname === '/patient/documents') {
    page = <RolePage role="PATIENT"><PatientDocumentsPage /></RolePage>
  } else if (location.pathname === '/patient/profile') {
    page = <RolePage role="PATIENT"><PatientProfilePage /></RolePage>
  } else if (location.pathname === '/doctor/profile') {
    page = <RolePage role="DOCTOR"><DoctorProfilePage /></RolePage>
  } else if (location.pathname === '/doctor/schedule') {
    page = <RolePage role="DOCTOR"><section className="page-shell"><div className="page-heading"><div className="eyebrow">{t('roles.DOCTOR')}</div><h1>{t('nav.schedule')}</h1><p className="form-description">{t('staff.doctorProfileDescription')}</p></div><StaffCalendar role="DOCTOR" /></section></RolePage>
  } else if (location.pathname.startsWith('/doctor/patients/')) {
    page = <RolePage role="DOCTOR"><DoctorPatientDetailPage id={location.pathname.split('/')[3]} /></RolePage>
  } else if (location.pathname === '/doctor/patients') {
    page = <RolePage role="DOCTOR"><DoctorPatientsPage /></RolePage>
  } else if (location.pathname === '/doctor/appointments') {
    page = <RolePage role="DOCTOR"><StaffWorkflowPage role="DOCTOR" /></RolePage>
  } else if (location.pathname === '/doctor/medical-records') {
    page = <RolePage role="DOCTOR"><DoctorMedicalRecordsPage /></RolePage>
  } else if (location.pathname === '/doctor/referrals') {
    page = <RolePage role="DOCTOR"><DoctorReferralsPage /></RolePage>
  } else if (location.pathname === '/doctor/today') {
    page = <RolePage role="DOCTOR"><Redirect to="/doctor/appointments" /></RolePage>
  } else if (location.pathname === '/nurse/profile') {
    page = <RolePage role="NURSE"><NurseProfilePage /></RolePage>
  } else if (location.pathname === '/nurse/schedule') {
    page = <RolePage role="NURSE"><Redirect to="/nurse/appointments" /></RolePage>
  } else if (location.pathname === '/nurse/appointments') {
    page = <RolePage role="NURSE"><section className="page-shell"><div className="page-heading"><div className="eyebrow">{t('roles.NURSE')}</div><h1>{t('nav.appointments')}</h1><p className="form-description">{t('sections.nurseAppointmentsDescription')}</p></div><StaffCalendar role="NURSE" /></section></RolePage>
  } else if (location.pathname === '/nurse/today') {
    page = <RolePage role="NURSE"><NurseTodayPage /></RolePage>
  } else if (location.pathname === '/nurse/vitals') {
    page = <RolePage role="NURSE"><NurseVitalsPage /></RolePage>
  } else if (location.pathname === '/doctor') {
    page = <RoleDashboard role="DOCTOR" />
  } else if (location.pathname === '/nurse') {
    page = <RoleDashboard role="NURSE" />
  } else if (location.pathname === '/admin') {
    page = <RolePage role="ADMIN"><AdminDashboardPage /></RolePage>
  } else if (location.pathname === '/admin/profile') {
    page = <RolePage role="ADMIN"><PlaceholderPage sectionKey="profile" role="ADMIN" /></RolePage>
  } else if (location.pathname === '/admin/schedule') {
    page = <RolePage role="ADMIN"><AdminSchedulePage /></RolePage>
  } else if (location.pathname === '/admin/document-templates') {
    page = <RolePage role="ADMIN"><AdminDocumentTemplatesPage /></RolePage>
  } else if (adminSectionRoutes[location.pathname]) {
    page = <RolePage role="ADMIN"><AdminDashboardPage section={adminSectionRoutes[location.pathname]} /></RolePage>
  } else if (location.pathname.startsWith('/admin/')) {
    page = <RolePage role="ADMIN"><NotFoundPage /></RolePage>
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
