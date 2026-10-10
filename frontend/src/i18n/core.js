import ru from './locales/ru.json'
import kk from './locales/kk.json'
import en from './locales/en.json'

export const languages = [
  { code: 'ru', label: 'Русский', shortLabel: 'RU' },
  { code: 'kk', label: 'Қазақша', shortLabel: 'KZ' },
  { code: 'en', label: 'English', shortLabel: 'EN' },
]

export const resources = { ru, kk, en }
export const languageKey = 'qamqormed.language'
export const defaultLanguage = 'ru'

export function normalizeLanguage(value) {
  const normalized = String(value || '').toLowerCase().split('-')[0]
  return resources[normalized] ? normalized : defaultLanguage
}

export function readPath(source, path) {
  return String(path).split('.').reduce((value, part) => value?.[part], source)
}

export function interpolate(value, variables = {}) {
  return String(value).replace(/{{(.*?)}}/g, (_, key) => String(variables[key.trim()] ?? ''))
}

export function createTranslator(language) {
  const currentLanguage = normalizeLanguage(language)
  return (key, variables = {}) => {
    const localized = readPath(resources[currentLanguage], key)
    const fallback = readPath(resources[defaultLanguage], key)
    const value = typeof localized === 'string' ? localized : fallback
    if (typeof value === 'string') return interpolate(value, variables)
    return readPath(resources[currentLanguage], 'common.notSpecified') || '—'
  }
}

export function localeFor(language) {
  return normalizeLanguage(language) === 'kk' ? 'kk-KZ' : normalizeLanguage(language) === 'en' ? 'en' : 'ru-KZ'
}

export function formatDate(value, language, options = {}) {
  if (!value) return '—'
  const date = value instanceof Date ? value : new Date(`${String(value).slice(0, 10)}T12:00:00`)
  return new Intl.DateTimeFormat(localeFor(language), options).format(date)
}

const knownBackendMessages = {
  'Неверный телефон или пароль': 'errors.invalidCredentials',
  'Validation failed': 'errors.validation',
  'Требуется авторизация': 'errors.unauthorized',
  'Пользователь с таким номером уже существует': 'errors.phoneAlreadyUsed',
  'Некорректный номер телефона': 'errors.invalidPhone',
  'Пароль должен содержать букву': 'errors.passwordLetter',
  'Пароль должен содержать цифру': 'errors.passwordDigit',
  'Пароли не совпадают': 'errors.passwordMismatch',
  'Врач не найден': 'errors.doctorNotFound',
  'Пациент не найден': 'errors.patientNotFound',
  'Запись не найдена': 'errors.appointmentNotFound',
  'Выбранное время уже занято': 'errors.slotUnavailable',
  'Выбранное время больше недоступно по расписанию': 'errors.slotUnavailable',
  'Выбранное время находится вне рабочего расписания или в перерыве': 'errors.slotUnavailable',
  'Выбранный временной слот недоступен': 'errors.slotUnavailable',
  'Дата приёма уже прошла': 'errors.pastAppointment',
  'На этом этапе запись уже нельзя отменить': 'errors.cannotCancel',
  'Сначала начните медицинский приём': 'errors.startAppointmentFirst',
  'Перед завершением укажите диагноз': 'errors.diagnosisRequired',
  'Приём уже завершён': 'errors.appointmentCompleted',
  'Отменённая запись недоступна': 'errors.appointmentCancelled',
}

export function translateError(error, t, fallbackKey = 'common.error') {
  const raw = error?.message || ''
  const key = error?.code && `errors.${error.code}`
  if (key && readPath(resources.ru, key)) return t(key)
  if (knownBackendMessages[raw]) return t(knownBackendMessages[raw])
  if (error?.status === 401) return t('errors.unauthorized')
  if (error?.status === 403) return t('errors.forbidden')
  if (error?.status === 404) return t('errors.notFound')
  if (error?.status === 409) return t('errors.conflict')
  if (error?.status >= 500) return t('errors.server')
  return t(fallbackKey)
}
