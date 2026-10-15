import assert from 'node:assert/strict'
import { readFileSync, readdirSync } from 'node:fs'
import { test } from 'node:test'
import { maskedIin, passwordRequirements, validateAccount } from '../src/utils/accountValidation.js'

const valid = { firstName: 'Аян', lastName: 'Тест', phone: '8 (700) 123-45-67', password: 'Valid123', confirmPassword: 'Valid123' }
test('registration accepts supported phone formats and exact password length boundaries', () => {
  for (const phone of ['+77001234567', '87001234567', '7001234567']) assert.deepEqual(validateAccount({ ...valid, phone }), {})
  for (const size of [8, 72]) {
    const password = `Ab1${'x'.repeat(size - 3)}`
    assert.deepEqual(validateAccount({ ...valid, password, confirmPassword: password }), {})
  }
  for (const size of [7, 73]) assert.equal(validateAccount({ ...valid, password: `Ab1${'x'.repeat(size - 3)}` }).password, 'authFlow.passwordLength')
})
test('registration identifies missing names, invalid phone and mismatched confirmation separately', () => {
  const errors = validateAccount({ ...valid, firstName: ' ', lastName: 'a'.repeat(101), phone: '+123', confirmPassword: 'different' })
  assert.equal(errors.firstName, 'authFlow.required')
  assert.equal(errors.lastName, 'authFlow.nameLength')
  assert.equal(errors.phone, 'errors.invalidPhone')
  assert.equal(errors.confirmPassword, 'errors.passwordMismatch')
})
test('password guidance matches the existing server letter and digit requirements', () => {
  assert.deepEqual(passwordRequirements('Аян12345'), { length: true, letter: true, digit: true })
  assert.equal(validateAccount({ ...valid, password: '12345678' }).password, 'errors.passwordLetter')
  assert.equal(validateAccount({ ...valid, password: 'abcdefgh' }).password, 'errors.passwordDigit')
})
test('login allows legacy passwords and does not apply registration length or complexity rules', () => {
  assert.deepEqual(validateAccount({ phone: valid.phone, password: 'old' }, 'login'), {})
  assert.deepEqual(validateAccount({ phone: valid.phone, password: '' }, 'login'), { password: 'authFlow.required' })
})
test('password changes validate the actual current/new/confirmation API field names', () => {
  const values = { currentPassword: 'old', newPassword: valid.password, confirmNewPassword: valid.password }
  assert.deepEqual(validateAccount(values, 'password'), {})
  assert.equal(validateAccount({ ...values, currentPassword: '' }, 'password').currentPassword, 'authFlow.required')
  assert.equal(validateAccount({ ...values, confirmNewPassword: 'different' }, 'password').confirmNewPassword, 'errors.passwordMismatch')
})
test('IIN summaries reveal only the last four digits even if maskedIin contains a raw identifier', () => {
  for (const args of [['123456789012'], [null, '123456789012'], [null, '********9012']]) {
    const summary = maskedIin(...args)
    assert.equal(summary, '••••••••9012')
    assert.ok(!summary.includes('12345678'))
  }
  assert.equal(maskedIin(null, null), '')
})
test('new account screens have translations in all three locales, including dynamic copy', () => {
  const read = (file) => readFileSync(new URL(`../${file}`, import.meta.url), 'utf8')
  const files = ['src/pages/LoginPage.jsx', 'src/pages/RegisterPage.jsx', 'src/pages/PatientProfilePage.jsx', 'src/pages/StaffPages.jsx', 'src/pages/AdminProfilePage.jsx', ...readdirSync(new URL('../src/components/account/', import.meta.url)).map((file) => `src/components/account/${file}`)]
  const keys = files.flatMap((file) => [...read(file).matchAll(/\bt\('([^']+)'/g)].map((match) => match[1]))
  keys.push(...['length', 'letter', 'digit'].map((key) => `authFlow.rule${key[0].toUpperCase()}${key.slice(1)}`))
  for (const language of ['ru', 'kk', 'en']) {
    const resource = JSON.parse(read(`src/i18n/locales/${language}.json`))
    for (const key of keys) assert.equal(typeof key.split('.').reduce((value, part) => value?.[part], resource), 'string', `${language}: ${key}`)
  }
})
