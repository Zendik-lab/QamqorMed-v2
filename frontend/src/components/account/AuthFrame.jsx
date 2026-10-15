import { useTranslation } from '../../i18n/index.jsx'
import Icon from '../ui/Icon.jsx'

export default function AuthFrame({ title, description, children, register = false }) {
  const { t } = useTranslation()
  return <section className={`auth-flow ${register ? 'auth-flow--register' : ''}`} aria-labelledby="auth-title">
    <aside className="auth-story">
      <span className="auth-story__mark" aria-hidden="true"><Icon name="medical" size={32} /></span>
      <div><span className="eyebrow">QamqorMed</span><h2>{t('authFlow.brandTitle')}</h2><p>{t('authFlow.brandDescription')}</p></div>
      <ul className="auth-story__features">{[['calendar', 'appointments'], ['file', 'documents'], ['users', 'family']].map(([icon, key]) => <li key={key}><span><Icon name={icon} size={22} /></span><div><strong>{t(`authFlow.${key}Title`)}</strong><p>{t(`authFlow.${key}Description`)}</p></div></li>)}</ul>
      <p className="auth-story__note"><Icon name="heart" size={18} />{t('authFlow.brandNote')}</p>
    </aside>
    <div className="auth-panel"><header><span className="eyebrow">{t(register ? 'auth.patientOnly' : 'authFlow.welcome')}</span><h1 id="auth-title">{title}</h1><p>{description}</p></header>{children}</div>
  </section>
}
