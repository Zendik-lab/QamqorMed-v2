import { useAuth } from '../auth/AuthContext.jsx'
import { useTranslation } from '../i18n/index.jsx'
import { navigate } from '../utils/navigation.js'
import { pathForRole } from '../components/RouteGuards.jsx'

function NotFoundPage() {
  const { user } = useAuth()
  const { t } = useTranslation()

  return (
    <section className="page-shell centered-page">
      <div className="error-code">404</div>
      <h1>{t('notFound.title')}</h1>
      <p className="form-description">{t('notFound.description')}</p>
      <button className="button button--compact" type="button" onClick={() => navigate(pathForRole(user?.role))}>
        {t('forbidden.backToCabinet')}
      </button>
    </section>
  )
}

export default NotFoundPage
