import { useAuth } from '../auth/AuthContext.jsx'
import { useTranslation } from '../i18n/index.jsx'
import { AccountSettings, ProfileFacts, ProfileHeader, ProfileLinks, ProfileSection, personName } from '../components/account/Profile.jsx'

export default function AdminProfilePage() {
  const { user } = useAuth()
  const { t } = useTranslation()
  return <section className="page-shell account-page">
    <ProfileHeader person={user} role="ADMIN" title={t('account.adminTitle')} description={t('account.adminDescription')} />
    <div className="account-grid"><ProfileSection title={t('account.identity')}><ProfileFacts items={[[t('staff.fullName'), personName(user)], [t('auth.phone'), user.phone], [t('account.role'), t('roles.ADMIN')]]} /><p>{t('account.adminIdentityNote')}</p></ProfileSection>
      <ProfileSection title={t('account.workspace')} icon="building"><ProfileLinks items={[["/admin", t('nav.dashboard'), 'home'], ['/admin/doctors', t('nav.doctors'), 'medical'], ['/admin/nurses', t('nav.nurses'), 'users'], ['/admin/schedule', t('nav.schedule'), 'calendar'], ['/admin/document-templates', t('nav.documentTemplates'), 'file']]} /></ProfileSection></div>
    <AccountSettings />
  </section>
}
