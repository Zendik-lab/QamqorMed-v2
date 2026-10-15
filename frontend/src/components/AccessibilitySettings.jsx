import { useSyncExternalStore } from 'react'
import { useTranslation } from '../i18n/index.jsx'
import Icon from './ui/Icon.jsx'
import Popover from './ui/Popover.jsx'
import { Checkbox } from './ui/Fields.jsx'

export default function AccessibilitySettings() {
  const { t } = useTranslation()
  const store = window.QamqorMedAccessibility
  const { textSize, contrast } = useSyncExternalStore(store.subscribe, store.getSnapshot)
  return <Popover label={t('accessibility.title')} trigger={<Icon name="accessibility" />} className="accessibility-settings">
    <strong>{t('accessibility.title')}</strong>
    <p className="ui-muted">{t('accessibility.description')}</p>
    <Checkbox label={t('accessibility.largeText')} role="switch" checked={textSize === 'large'} onChange={(event) => store.setPreferences({ textSize: event.target.checked ? 'large' : 'normal' })} />
    <Checkbox label={t('accessibility.highContrast')} role="switch" checked={contrast === 'high'} onChange={(event) => store.setPreferences({ contrast: event.target.checked ? 'high' : 'normal' })} />
  </Popover>
}
