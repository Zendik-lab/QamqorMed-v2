import { useTheme } from '../theme/ThemeProvider.jsx'
import { useTranslation } from '../i18n/index.jsx'
import Popover from './ui/Popover.jsx'

function ThemeIcon({ mode }) {
  return <svg viewBox="0 0 24 24" width="21" height="21" fill="none" stroke="currentColor" strokeWidth="1.7" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">
    {mode === 'light' ? <><circle cx="12" cy="12" r="4" /><path d="M12 2v2m0 16v2M2 12h2m16 0h2M5 5l1.4 1.4m11.2 11.2L19 19M5 19l1.4-1.4M17.6 6.4L19 5" /></>
      : mode === 'dark' ? <path d="M20.8 13A9 9 0 0 1 11 3.2 9 9 0 1 0 20.8 13Z" />
        : <><rect x="3" y="4" width="18" height="13" rx="2" /><path d="M8 21h8m-4-4v4" /></>}
  </svg>
}

export default function ThemeSwitcher() {
  const { preference, setPreference } = useTheme()
  const { t } = useTranslation()
  return <Popover label={`${t('theme.choose')}: ${t(`theme.${preference}`)}`} className="theme-menu preference-options" trigger={<span key={preference} className="theme-menu__icon"><ThemeIcon mode={preference} /></span>}>
    <div className="preference-options__list" role="group" aria-label={t('theme.choose')}>{['light', 'dark', 'system'].map((mode) => <button type="button" key={mode} data-close-popover aria-pressed={preference === mode} onClick={() => setPreference(mode)}><ThemeIcon mode={mode} />{t(`theme.${mode}`)}</button>)}</div>
  </Popover>
}
