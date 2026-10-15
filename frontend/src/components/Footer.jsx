import { useAuth } from '../auth/AuthContext.jsx'
import { useTranslation } from '../i18n/index.jsx'
import { officialResources, platformContacts } from '../config/platform.js'
import AppLink from './AppLink.jsx'
import Brand from './Brand.jsx'
import Icon from './ui/Icon.jsx'

const workspaceLinks = {
  PATIENT: [['nav.bookAppointment', '/patient/appointments'], ['nav.myAppointments', '/patient/my-appointments'], ['nav.documents', '/patient/documents'], ['footer.visitHistory', '/patient/medical-history']],
  DOCTOR: [['nav.appointments', '/doctor/appointments'], ['nav.schedule', '/doctor/schedule'], ['nav.medicalRecords', '/doctor/medical-records'], ['nav.referrals', '/doctor/referrals']],
  NURSE: [['nav.todayPatients', '/nurse/today'], ['nav.appointments', '/nurse/appointments'], ['nav.vitals', '/nurse/vitals'], ['nav.profile', '/nurse/profile']],
  ADMIN: [['nav.clinics', '/admin/clinics'], ['nav.doctors', '/admin/doctors'], ['nav.nurses', '/admin/nurses'], ['nav.schedule', '/admin/schedule']],
}

export default function Footer() {
  const { user } = useAuth()
  const { t, language } = useTranslation()
  const links = workspaceLinks[user?.role] || workspaceLinks.PATIENT

  return <footer className="site-footer">
    <div className="site-footer__grid">
      <div className="site-footer__about">
        <Brand />
        <p>{t('footer.description')}</p>
        <p className="site-footer__availability"><span aria-hidden="true">◈</span>{t('footer.availability')}</p>
      </div>
      <section aria-labelledby="footer-contacts">
        <h2 id="footer-contacts">{t('footer.contacts')}</h2>
        <ul>
          <li>{t('footer.region')}</li>
          {platformContacts.email && <li><a href={`mailto:${platformContacts.email}`}>{platformContacts.email}</a></li>}
          {platformContacts.phone && <li><a href={`tel:${platformContacts.phone.replace(/[^+\d]/g, '')}`}>{platformContacts.phone}</a></li>}
        </ul>
        {!platformContacts.email && !platformContacts.phone && <p>{t('footer.contactsPending')}</p>}
        <div className="site-footer__developer"><strong>{t('footer.developer')}</strong><span>Zendlless · @Zendlless</span><a className="site-footer__telegram" href="https://t.me/Zendlless" target="_blank" rel="noopener noreferrer"><Icon name="telegram" size={17} />{t('footer.writeTelegram')}<span className="sr-only"> ({t('footer.newTab')})</span></a></div>
      </section>
      <nav aria-labelledby="footer-useful">
        <h2 id="footer-useful">{t('footer.usefulLinks')}</h2>
        <ul>{links.map(([key, href]) => <li key={href}><AppLink href={href}>{t(key)}</AppLink></li>)}<li><AppLink href="/help">{t('footer.help')}</AppLink></li></ul>
      </nav>
      <nav aria-labelledby="footer-resources">
        <h2 id="footer-resources">{t('footer.resources')}</h2>
        <ul>{officialResources(language).map(({ key, href }) => <li key={key}><a href={href} target="_blank" rel="noopener noreferrer">{t(`footer.${key}`)}<span className="site-footer__external" aria-hidden="true">↗</span><span className="sr-only"> ({t('footer.newTab')})</span></a></li>)}</ul>
      </nav>
    </div>
    <div className="site-footer__bottom">
      <p>© {new Date().getFullYear()} QamqorMed. {t('footer.rights')}</p>
      <p className="site-footer__demo"><span aria-hidden="true">ⓘ</span>{t('footer.demo')}</p>
    </div>
  </footer>
}
