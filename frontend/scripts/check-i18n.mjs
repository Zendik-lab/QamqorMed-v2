import assert from 'node:assert/strict'
import fs from 'node:fs'
import path from 'node:path'
import { fileURLToPath } from 'node:url'

const root = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..')
const languages = ['ru', 'kk', 'en']
const resources = Object.fromEntries(languages.map((language) => [language, JSON.parse(fs.readFileSync(path.join(root, 'src', 'i18n', 'locales', `${language}.json`), 'utf8'))]))

function flatten(value, prefix = '', result = new Map()) {
  Object.entries(value).forEach(([key, child]) => {
    const fullKey = prefix ? `${prefix}.${key}` : key
    if (child && typeof child === 'object' && !Array.isArray(child)) flatten(child, fullKey, result)
    else result.set(fullKey, child)
  })
  return result
}

const flattened = Object.fromEntries(languages.map((language) => [language, flatten(resources[language])]))
const expectedKeys = [...flattened.ru.keys()].sort()
for (const language of languages) {
  assert.deepEqual([...flattened[language].keys()].sort(), expectedKeys, `${language} locale keys differ from ru`)
  for (const [key, value] of flattened[language]) assert.notEqual(value, '', `${language}.${key} is empty`)
}

function normalizeLanguage(value) {
  const normalized = String(value || '').toLowerCase().split('-')[0]
  return languages.includes(normalized) ? normalized : 'ru'
}

function translate(language, key, variables = {}) {
  const current = flattened[normalizeLanguage(language)].get(key)
  const fallback = flattened.ru.get(key)
  const value = typeof current === 'string' ? current : typeof fallback === 'string' ? fallback : resources[normalizeLanguage(language)].common.notSpecified
  assert.equal(typeof value, 'string', `Missing translation: ${key}`)
  return value.replace(/{{(.*?)}}/g, (_, name) => String(variables[name.trim()] ?? ''))
}

assert.equal(normalizeLanguage('kk-KZ'), 'kk')
assert.equal(normalizeLanguage('en-US'), 'en')
assert.equal(normalizeLanguage('unknown'), 'ru')
assert.notEqual(translate('ru', 'common.language'), translate('kk', 'common.language'))
assert.notEqual(translate('kk', 'common.language'), translate('en', 'common.language'))
assert.equal(translate('en', 'common.missing-key'), resources.en.common.notSpecified)
assert.match(new Intl.DateTimeFormat('kk-KZ', { month: 'long' }).format(new Date(2026, 0, 1)), /./)

console.log(`i18n check passed: ${expectedKeys.length} keys × ${languages.length} locales`)
