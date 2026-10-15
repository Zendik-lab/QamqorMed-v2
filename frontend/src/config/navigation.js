export const workspaceMenus = {
  PATIENT: [
    ['dashboard', '/patient', 'home', 'workspace'],
    ['bookAppointment', '/patient/appointments', 'plus', 'care'],
    ['myAppointments', '/patient/my-appointments', 'calendar', 'care'],
    ['documents', '/patient/documents', 'file', 'care'],
    ['medicalRecord', '/patient/medical-record', 'heart', 'care'],
    ['waitlist', '/patient/waitlist', 'clock', 'care'],
    ['dependents', '/patient/dependents', 'users', 'account'],
    ['profile', '/patient/profile', 'user', 'account'],
  ],
  DOCTOR: [
    ['dashboard', '/doctor', 'home', 'workspace'],
    ['appointments', '/doctor/appointments', 'medical', 'care'],
    ['schedule', '/doctor/schedule', 'calendar', 'care'],
    ['patients', '/doctor/patients', 'users', 'care'],
    ['medicalRecords', '/doctor/medical-records', 'file', 'care'],
    ['referrals', '/doctor/referrals', 'arrow', 'care'],
    ['profile', '/doctor/profile', 'user', 'account'],
  ],
  NURSE: [
    ['dashboard', '/nurse', 'home', 'workspace'],
    ['todayPatients', '/nurse/today', 'users', 'care'],
    ['appointments', '/nurse/appointments', 'calendar', 'care'],
    ['vitals', '/nurse/vitals', 'heart', 'care'],
    ['profile', '/nurse/profile', 'user', 'account'],
  ],
  ADMIN: [
    ['dashboard', '/admin', 'home', 'workspace'],
    ['regions', '/admin/regions', 'pin', 'directory'],
    ['cities', '/admin/cities', 'building', 'directory'],
    ['clinics', '/admin/clinics', 'medical', 'directory'],
    ['specialties', '/admin/specialties', 'heart', 'directory'],
    ['documentTemplates', '/admin/document-templates', 'file', 'directory'],
    ['doctors', '/admin/doctors', 'users', 'team'],
    ['nurses', '/admin/nurses', 'users', 'team'],
    ['schedule', '/admin/schedule', 'calendar', 'team'],
    ['profile', '/admin/profile', 'user', 'account'],
  ],
}

export function isWorkspacePathActive(pathname, path, role) {
  return path === `/${role.toLowerCase()}` ? pathname === path : pathname === path || pathname.startsWith(`${path}/`)
}

export function workspaceSection(pathname, role) {
  if (pathname === '/help') return 'footer.help'
  if (pathname === '/doctors' || pathname.startsWith('/doctors/')) return 'nav.doctors'
  const item = workspaceMenus[role]?.find(([, path]) => isWorkspacePathActive(pathname, path, role))
  if (item) return `nav.${item[0]}`
  const aliases = {
    '/patient/medical-history': 'nav.medicalRecord', '/patient/diagnoses': 'nav.medicalRecord',
    '/patient/medications': 'nav.medicalRecord', '/patient/location': 'patient.changeCity',
    '/doctor/today': 'nav.appointments', '/nurse/schedule': 'nav.appointments',
  }
  return aliases[pathname] || 'nav.dashboard'
}

export const mobilePrimary = {
  PATIENT: ['dashboard', 'myAppointments', 'documents', 'profile'],
  DOCTOR: ['dashboard', 'appointments', 'patients', 'schedule'],
  NURSE: ['dashboard', 'todayPatients', 'vitals', 'appointments'],
  ADMIN: ['dashboard', 'clinics', 'doctors', 'schedule'],
}
