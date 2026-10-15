import { lazy, Suspense } from 'react'
import AppLayout from './layouts/AppLayout.jsx'
import { AuthProvider, useAuth } from './auth/AuthContext.jsx'
import { pathForRole, ProtectedRoute, Redirect, RoleRoute } from './components/RouteGuards.jsx'
import { isDevelopment } from './config/environment.js'
import { useLocation } from './utils/navigation.js'
import { I18nProvider, useTranslation } from './i18n/index.jsx'
import RouteErrorBoundary from './components/RouteErrorBoundary.jsx'
import { PageSkeleton } from './components/ui/Feedback.jsx'

const DashboardPage = lazy(() => import('./pages/DashboardPage.jsx'))
const AdminDashboardPage = lazy(() => import('./pages/AdminDashboardPage.jsx'))
const DoctorsPage = lazy(() => import('./pages/DoctorsPage.jsx'))
const DoctorDetailPage = lazy(() => import('./pages/DoctorDetailPage.jsx'))
const LoginPage = lazy(() => import('./pages/LoginPage.jsx'))
const RegisterPage = lazy(() => import('./pages/RegisterPage.jsx'))
const AdminProfilePage = lazy(() => import('./pages/AdminProfilePage.jsx'))
const PlaceholderPage = lazy(() => import('./pages/PlaceholderPage.jsx'))
const ForbiddenPage = lazy(() => import('./pages/ForbiddenPage.jsx'))
const NotFoundPage = lazy(() => import('./pages/NotFoundPage.jsx'))
const DevPanelPage = isDevelopment ? lazy(() => import('./pages/DevPanelPage.jsx')) : null
const StaffWorkflowPage = lazy(() => import('./pages/StaffWorkflowPages.jsx'))
const StaffCalendar = lazy(() => import('./components/calendar/StaffCalendar.jsx'))
const AdminSchedulePage = lazy(() => import('./pages/AdminSchedulePage.jsx'))
const AdminDocumentTemplatesPage = lazy(() => import('./pages/AdminDocumentTemplatesPage.jsx'))
const PatientWaitlistPage = lazy(() => import('./pages/PatientWaitlistPage.jsx'))
const HelpPage = lazy(() => import('./pages/HelpPage.jsx'))
const DependentDetailPage = lazy(() => import('./pages/PatientPages.jsx').then(module => ({ default: module.DependentDetailPage })))
const PatientAppointmentsPage = lazy(() => import('./pages/PatientPages.jsx').then(module => ({ default: module.PatientAppointmentsPage })))
const PatientBookingPage = lazy(() => import('./pages/PatientPages.jsx').then(module => ({ default: module.PatientBookingPage })))
const PatientLocationPage = lazy(() => import('./pages/PatientPages.jsx').then(module => ({ default: module.PatientLocationPage })))
const PatientDependentsPage = lazy(() => import('./pages/PatientPages.jsx').then(module => ({ default: module.PatientDependentsPage })))
const PatientMedicalHistoryPage = lazy(() => import('./pages/PatientPages.jsx').then(module => ({ default: module.PatientMedicalHistoryPage })))
const PatientClinicalDataPage = lazy(() => import('./pages/PatientPages.jsx').then(module => ({ default: module.PatientClinicalDataPage })))
const PatientDocumentsPage = lazy(() => import('./pages/PatientPages.jsx').then(module => ({ default: module.PatientDocumentsPage })))
const PatientProfilePage = lazy(() => import('./pages/PatientProfilePage.jsx'))
const DoctorProfilePage = lazy(() => import('./pages/StaffPages.jsx').then(module => ({ default: module.DoctorProfilePage })))
const NurseProfilePage = lazy(() => import('./pages/StaffPages.jsx').then(module => ({ default: module.NurseProfilePage })))
const DoctorPatientDetailPage = lazy(() => import('./pages/DoctorPatientsPages.jsx').then(module => ({ default: module.DoctorPatientDetailPage })))
const DoctorPatientsPage = lazy(() => import('./pages/DoctorPatientsPages.jsx').then(module => ({ default: module.DoctorPatientsPage })))
const DoctorMedicalRecordsPage = lazy(() => import('./pages/StaffSectionPages.jsx').then(module => ({ default: module.DoctorMedicalRecordsPage })))
const DoctorReferralsPage = lazy(() => import('./pages/StaffSectionPages.jsx').then(module => ({ default: module.DoctorReferralsPage })))
const NurseTodayPage = lazy(() => import('./pages/StaffSectionPages.jsx').then(module => ({ default: module.NurseTodayPage })))
const NurseVitalsPage = lazy(() => import('./pages/StaffSectionPages.jsx').then(module => ({ default: module.NurseVitalsPage })))

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
  } else if (location.pathname === '/help') {
    page = <HelpPage />
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
    page = <RolePage role="ADMIN"><AdminProfilePage /></RolePage>
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

  return <AppLayout><RouteErrorBoundary key={location.pathname}><Suspense fallback={<PageSkeleton />}>{page}</Suspense></RouteErrorBoundary></AppLayout>
}

function App() {
  return <I18nProvider><AuthProvider><AppContent /></AuthProvider></I18nProvider>
}

export default App
