import { normalizePhone } from './phone.js'

// Mirror RegisterRequest + AuthService. Do not apply new-password policy at login.
export function passwordRequirements(value = '') {
  return { length: value.length >= 8 && value.length <= 72, letter: /[A-Za-zА-Яа-я]/.test(value), digit: /[0-9]/.test(value) }
}

export function validateAccount(values, mode = 'register') {
  const errors = {}
  const required = (name) => { if (!String(values[name] ?? '').trim()) errors[name] = 'authFlow.required' }
  if (mode === 'register') for (const name of ['firstName', 'lastName']) {
    required(name)
    if ((values[name] || '').trim().length > 100) errors[name] = 'authFlow.nameLength'
  }
  if (mode !== 'password') {
    required('phone')
    if (!errors.phone) { try { normalizePhone(values.phone) } catch { errors.phone = 'errors.invalidPhone' } }
  }
  const passwordName = mode === 'password' ? 'newPassword' : 'password'
  required(passwordName)
  if (mode !== 'login' && !errors[passwordName]) {
    const rules = passwordRequirements(values[passwordName])
    if (!rules.length) errors[passwordName] = 'authFlow.passwordLength'
    else if (!rules.letter) errors[passwordName] = 'errors.passwordLetter'
    else if (!rules.digit) errors[passwordName] = 'errors.passwordDigit'
  }
  if (mode !== 'login') {
    const confirmation = mode === 'password' ? 'confirmNewPassword' : 'confirmPassword'
    required(confirmation)
    if (!errors[confirmation] && values[confirmation] !== values[passwordName]) errors[confirmation] = 'errors.passwordMismatch'
  }
  if (mode === 'password') required('currentPassword')
  return errors
}

export function maskedIin(value, masked) {
  const digits = String(value || masked || '').replace(/\D/g, '')
  return digits ? `••••••••${digits.slice(-4)}` : ''
}
