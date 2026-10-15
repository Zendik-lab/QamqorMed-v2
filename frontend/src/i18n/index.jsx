import { createContext, useContext, useEffect, useMemo, useState } from 'react'
import { createTranslator, defaultLanguage, languageKey, languages, localeFor, normalizeLanguage, resources, translateError } from './core.js'

export { createTranslator, languageKey, languages, localeFor, normalizeLanguage, resources, translateError }
const I18nContext = createContext(null)

function initialLanguage() {
  try { return normalizeLanguage(window.localStorage.getItem(languageKey) || defaultLanguage) }
  catch { return defaultLanguage }
}

export function I18nProvider({ children }) {
  const [language, setLanguageState] = useState(initialLanguage)

  const changeLanguage = (nextLanguage) => {
    const normalized = normalizeLanguage(nextLanguage)
    try { window.localStorage.setItem(languageKey, normalized) } catch { /* Continue in memory. */ }
    setLanguageState(normalized)
  }

  useEffect(() => {
    document.documentElement.lang = language
  }, [language])

  const value = useMemo(() => ({
    language,
    languages,
    changeLanguage,
    t: createTranslator(language),
  }), [language])

  return <I18nContext.Provider value={value}>{children}</I18nContext.Provider>
}

export function useTranslation() {
  const context = useContext(I18nContext)
  if (!context) throw new Error('useTranslation must be used inside I18nProvider')
  return context
}

export function getLanguageStorageKey() {
  return languageKey
}

export function localizeSpecialty(specialty, t) {
  const name = typeof specialty === 'object' ? (specialty.nameRu || specialty.name || '') : specialty
  const value = String(name || '').trim().toLowerCase()
  const key = value.includes('карди') || value.includes('cardio') ? 'cardiology'
    : value.includes('терап') || value.includes('therap') ? 'therapy'
      : value.includes('педиатр') || value.includes('pediatr') ? 'pediatrics' : null
  return key ? t(`medical.specialties.${key}`) : name
}

export function localizeDirectory(item, language) {
  if (!item) return ''
  if (language === 'kk') return item.nameKk || item.nameRu || item.name
  if (language === 'en') return item.nameEn || item.nameRu || item.name
  return item.nameRu || item.name
}
