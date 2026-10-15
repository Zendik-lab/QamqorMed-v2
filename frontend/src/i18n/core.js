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

// Keep the staff workspaces localised while the larger locale files remain
// compatible with deployments that still ship the previous catalog version.
const inlineTranslations = {
  'ai.open': { ru: 'Открыть QamqorMed AI', kk: 'QamqorMed AI ашу', en: 'Open QamqorMed AI' },
  'ai.shortTitle': { ru: 'AI-помощник', kk: 'AI-көмекші', en: 'AI assistant' },
  'ai.subtitle': { ru: 'Локальный помощник платформы', kk: 'Платформаның жергілікті көмекшісі', en: 'Local platform assistant' },
  'ai.newChat': { ru: 'Новый чат', kk: 'Жаңа чат', en: 'New chat' },
  'ai.history': { ru: 'История', kk: 'Тарих', en: 'History' },
  'ai.noHistory': { ru: 'Сохранённых диалогов пока нет', kk: 'Сақталған диалогтар әзірге жоқ', en: 'No saved conversations yet' },
  'ai.deleteChat': { ru: 'Удалить диалог', kk: 'Диалогты өшіру', en: 'Delete conversation' },
  'ai.greeting': { ru: 'Здравствуйте, {{name}}', kk: 'Сәлеметсіз бе, {{name}}', en: 'Hello, {{name}}' },
  'ai.description': { ru: 'Я помогу разобраться в QamqorMed и отвечу на общие вопросы о здоровье. Не заменяю врача и не вижу ваши медицинские данные.', kk: 'QamqorMed жүйесін түсінуге және денсаулық туралы жалпы сұрақтарға жауап беруге көмектесемін. Дәрігерді алмастырмаймын және медициналық деректеріңізді көрмеймін.', en: "I can help you use QamqorMed and answer general health questions. I do not replace a doctor or see your medical data." },
  'ai.placeholder': { ru: 'Напишите вопрос…', kk: 'Сұрағыңызды жазыңыз…', en: 'Ask a question…' },
  'ai.send': { ru: 'Отправить', kk: 'Жіберу', en: 'Send' },
  'ai.stop': { ru: 'Остановить генерацию', kk: 'Генерацияны тоқтату', en: 'Stop generation' },
  'ai.disclaimer': { ru: 'Не вводите персональные медицинские данные', kk: 'Жеке медициналық деректерді енгізбеңіз', en: 'Do not enter personal medical data' },
  'ai.unavailable': { ru: 'AI временно недоступен. Убедитесь, что Ollama запущена локально.', kk: 'AI уақытша қолжетімсіз. Ollama жергілікті іске қосылғанын тексеріңіз.', en: 'AI is temporarily unavailable. Make sure Ollama is running locally.' },
  'ai.historyError': { ru: 'Не удалось загрузить историю диалогов.', kk: 'Диалогтар тарихын жүктеу мүмкін болмады.', en: 'Conversation history could not be loaded.' },
  'ai.suggestions.booking': { ru: 'Как записаться к врачу?', kk: 'Дәрігерге қалай жазыламын?', en: 'How do I book a doctor appointment?' },
  'ai.suggestions.documents': { ru: 'Где посмотреть мои медицинские документы?', kk: 'Медициналық құжаттарымды қайдан көремін?', en: 'Where can I find my medical documents?' },
  'ai.suggestions.health': { ru: 'Что важно знать о профилактике простуды?', kk: 'Суық тиюдің алдын алу туралы не білу керек?', en: 'What should I know about preventing a cold?' },
  'ai.suggestions.doctorNavigation': { ru: 'Как работать с кабинетом врача?', kk: 'Дәрігер кабинетімен қалай жұмыс істеймін?', en: 'How do I use the doctor workspace?' },
  'ai.suggestions.icd10': { ru: 'Что такое МКБ-10 в QamqorMed?', kk: 'QamqorMed жүйесіндегі АХЖ-10 деген не?', en: 'What is ICD-10 in QamqorMed?' },
  'ai.suggestions.schedule': { ru: 'Где посмотреть расписание приёмов?', kk: 'Қабылдау кестесін қайдан көремін?', en: 'Where can I view the appointment schedule?' },
  'ai.suggestions.vitals': { ru: 'Как внести показатели пациента?', kk: 'Пациент көрсеткіштерін қалай енгіземін?', en: 'How do I enter patient measurements?' },
  'ai.suggestions.preparation': { ru: 'Как подготовить пациента к приёму?', kk: 'Пациентті қабылдауға қалай дайындаймын?', en: 'How do I prepare a patient for a visit?' },
  'ai.suggestions.clinics': { ru: 'Как управлять клиниками?', kk: 'Клиникаларды қалай басқарамын?', en: 'How do I manage clinics?' },
  'ai.suggestions.staff': { ru: 'Как управлять персоналом?', kk: 'Қызметкерлерді қалай басқарамын?', en: 'How do I manage staff?' },
  'ai.suggestions.schedules': { ru: 'Как настроить расписание врачей?', kk: 'Дәрігерлер кестесін қалай баптаймын?', en: 'How do I configure doctor schedules?' },
  'sections.doctorRecordsDescription': { ru: 'Самостоятельная история здоровья пациентов, связанных с вашими приёмами. Откройте карту, чтобы посмотреть хронологию и документы.', kk: 'Сіздің қабылдауларыңызбен байланысты пациенттердің денсаулық тарихы. Картаны ашып, хронология мен құжаттарды көріңіз.', en: 'A separate health history for patients connected to your visits. Open a record to review its timeline and documents.' },
  'sections.accessiblePatients': { ru: 'Доступные карты', kk: 'Қолжетімді карталар', en: 'Accessible records' },
  'sections.referralsDescription': { ru: 'Выданные направления связаны с конкретным приёмом и сразу появляются в карте пациента и его кабинете.', kk: 'Берілген жолдамалар нақты қабылдаумен байланысты және пациент картасы мен кабинетінде бірден көрінеді.', en: "Issued referrals stay linked to the visit and appear in the patient's record and cabinet." },
  'sections.createReferral': { ru: 'Создать направление', kk: 'Жолдама жасау', en: 'Create referral' },
  'sections.relatedAppointment': { ru: 'Связанный приём', kk: 'Байланысты қабылдау', en: 'Related visit' },
  'sections.chooseActiveAppointment': { ru: 'Выберите текущий приём', kk: 'Ағымдағы қабылдауды таңдаңыз', en: 'Choose an active visit' },
  'sections.referralAppointmentRequired': { ru: 'Выберите приём, в рамках которого оформляется направление.', kk: 'Жолдама жасалатын қабылдауды таңдаңыз.', en: 'Choose the visit where this referral is being issued.' },
  'sections.referralCreated': { ru: 'Направление создано и добавлено в медицинскую карту.', kk: 'Жолдама жасалып, медициналық картаға қосылды.', en: 'Referral created and added to the medical record.' },
  'sections.noReferrals': { ru: 'Направлений пока нет', kk: 'Жолдамалар әзірге жоқ', en: 'No referrals yet' },
  'sections.noReferralsDescription': { ru: 'Оформленные направления появятся здесь после проведения приёма.', kk: 'Қабылдау аяқталғаннан кейін жолдамалар осында көрсетіледі.', en: 'Issued referrals will appear here after a visit is conducted.' },
  'sections.toDate': { ru: 'По дату', kk: 'Соңғы күн', en: 'To date' },
  'sections.validUntil': { ru: 'Действует до', kk: 'Мерзімі', en: 'Valid until' },
  'sections.documentPreview': { ru: 'Предпросмотр документа', kk: 'Құжатты алдын ала көру', en: 'Document preview' },
  'sections.downloadPdf': { ru: 'Скачать PDF', kk: 'PDF жүктеу', en: 'Download PDF' },
  'sections.print': { ru: 'Печать', kk: 'Басып шығару', en: 'Print' },
  'sections.nurseTodayDescription': { ru: 'Очередь пациентов закреплённого врача на сегодня: отмечайте прибытие и готовность к приёму.', kk: 'Бекітілген дәрігердің бүгінгі кезегі: келуді және қабылдауға дайындығын белгілеңіз.', en: "Today's queue for the assigned doctor: mark arrival and readiness for the visit." },
  'sections.openVitals': { ru: 'Открыть показатели', kk: 'Көрсеткіштерді ашу', en: 'Open vitals' },
  'sections.measureVitals': { ru: 'Измерить показатели', kk: 'Көрсеткіштерді өлшеу', en: 'Measure vitals' },
  'sections.nurseVitalsDescription': { ru: 'Выберите запись, внесите показатели и передайте подготовленного пациента врачу.', kk: 'Жазбаны таңдап, көрсеткіштерді енгізіп, дайын пациентті дәрігерге жіберіңіз.', en: 'Select a visit, enter measurements and hand the prepared patient to the doctor.' },
  'sections.previousVitals': { ru: 'Последние сохранённые показатели', kk: 'Соңғы сақталған көрсеткіштер', en: 'Last saved measurements' },
  'sections.measuredAt': { ru: 'Измерено', kk: 'Өлшенген уақыт', en: 'Measured at' },
  'sections.measuredBy': { ru: 'Сотрудник', kk: 'Қызметкер', en: 'Staff member' },
  'sections.invalidVitals': { ru: 'Проверьте диапазоны измерений: значения должны быть медицински корректными.', kk: 'Өлшем мәндерін тексеріңіз: мәндер медициналық дұрыс ауқымда болуы керек.', en: 'Check the measurement ranges: values must be medically plausible.' },
  'sections.nurseAppointmentsDescription': { ru: 'Интерактивное расписание закреплённого врача с переходом к подготовке пациента.', kk: 'Пациентті дайындауға өтумен бекітілген дәрігердің интерактивті кестесі.', en: 'Interactive schedule for the assigned doctor with a route to patient preparation.' },
  'sections.doctorAppointmentsDescription': { ru: 'Единый рабочий процесс приёма: сегодняшние и предстоящие записи, результаты медсестры и завершённая история.', kk: 'Қабылдаудың бірыңғай үдерісі: бүгінгі және алдағы жазбалар, мейірбике деректері және аяқталған тарих.', en: 'One clinical visit workflow: today and upcoming appointments, nurse data and completed history.' },
}

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
    const localized = readPath(resources[currentLanguage], key) ?? inlineTranslations[key]?.[currentLanguage]
    const fallback = readPath(resources[defaultLanguage], key) ?? inlineTranslations[key]?.[defaultLanguage]
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
  'Неверный номер телефона или пароль': 'errors.invalidCredentials',
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
  'Выбранное время уже занято. Выберите другое.': 'errors.slotUnavailable',
  'Это время уже занято. Выберите другое.': 'errors.slotUnavailable',
  'Время не совпадает со свободным слотом': 'errors.slotUnavailable',
  'Время приёма не найдено': 'errors.slotUnavailable',
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
  if (error?.status === 0 || error?.name === 'TypeError') return t('errors.network')
  if (knownBackendMessages[raw]) return t(knownBackendMessages[raw])
  if (error?.status === 401) return t('errors.unauthorized')
  if (error?.status === 403) return t('errors.forbidden')
  if (error?.status === 404) return t('errors.notFound')
  if (error?.status === 409) return t('errors.conflict')
  if (error?.status === 400) return t('errors.validation')
  if (error?.status >= 500) return t('errors.server')
  return t(fallbackKey)
}
