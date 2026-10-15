import { useAuth } from '../auth/AuthContext.jsx'
import { useTranslation } from '../i18n/index.jsx'
import { platformContacts } from '../config/platform.js'

export default function HelpPage() {
  const { user } = useAuth()
  const { t } = useTranslation()
  return <section className="page-shell help-page">
    <div className="page-heading"><div className="eyebrow">QamqorMed</div><h1>{t('footer.help')}</h1><p className="form-description">{t('help.description')}</p></div>
    <div className="help-page__cards">
      <article className="patient-card"><h2>{t('help.workspaceTitle')}</h2><p>{t(`help.${user?.role || 'PATIENT'}`)}</p></article>
      <article className="patient-card"><h2>{t('help.preferencesTitle')}</h2><p>{t('help.preferences')}</p></article>
      <article className="patient-card"><h2>{t('footer.contacts')}</h2><p>{t('footer.region')}</p>{platformContacts.email && <p><a href={`mailto:${platformContacts.email}`}>{platformContacts.email}</a></p>}{platformContacts.phone && <p><a href={`tel:${platformContacts.phone.replace(/[^+\d]/g, '')}`}>{platformContacts.phone}</a></p>}{!platformContacts.email && !platformContacts.phone && <p>{t('footer.contactsPending')}</p>}<p>{t('footer.demo')}</p></article>
    </div>
  </section>
}
