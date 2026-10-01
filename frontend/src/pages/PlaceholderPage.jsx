import { useTranslation } from '../i18n/index.jsx'

function PlaceholderPage({ sectionKey, role }) {
  const { t } = useTranslation()
  const profile = sectionKey === 'profile'
  const admin = role === 'ADMIN'

  return (
    <section className="page-shell placeholder-page">
      <div className="placeholder-icon" aria-hidden="true">✦</div>
      <div className="eyebrow">{t('common.comingSoon')}</div>
      <h1>{t(`nav.${sectionKey}`)}</h1>
      <p className="form-description">
        {profile ? t('placeholder.profileDescription') : admin ? t('placeholder.adminDescription') : t('placeholder.description', { nextStage: t('common.nextStage') })}
      </p>
    </section>
  )
}

export default PlaceholderPage
