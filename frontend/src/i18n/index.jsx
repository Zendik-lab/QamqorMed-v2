import { createContext, useContext, useEffect, useMemo, useState } from 'react'
import ru from './locales/ru.json'
import kk from './locales/kk.json'
import en from './locales/en.json'

export const languages = [
  { code: 'ru', label: 'Русский', shortLabel: 'RU' },
  { code: 'kk', label: 'Қазақша', shortLabel: 'KZ' },
  { code: 'en', label: 'English', shortLabel: 'EN' },
]

const resources = { ru, kk, en }
const languageKey = 'qamqormed.language'
const I18nContext = createContext(null)

function initialLanguage() {
  const saved = window.localStorage.getItem(languageKey)
  return resources[saved] ? saved : 'ru'
}

function readPath(source, path) {
  return path.split('.').reduce((value, part) => value?.[part], source)
}

function interpolate(value, variables) {
  return value.replace(/{{(.*?)}}/g, (_, key) => String(variables?.[key.trim()] ?? ''))
}

export function I18nProvider({ children }) {
  const [language, setLanguageState] = useState(initialLanguage)

  const changeLanguage = (nextLanguage) => {
    if (!resources[nextLanguage]) return
    window.localStorage.setItem(languageKey, nextLanguage)
    setLanguageState(nextLanguage)
  }

  useEffect(() => {
    document.documentElement.lang = language
  }, [language])

  const value = useMemo(() => ({
    language,
    languages,
    changeLanguage,
    t: (key, variables = {}) => {
      const value = readPath(resources[language], key) ?? readPath(resources.ru, key)
      return typeof value === 'string' ? interpolate(value, variables) : key
    },
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
