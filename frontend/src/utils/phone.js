const normalizedPhonePattern = /^\+7\d{10}$/

export function normalizePhone(value) {
  let digits = String(value || '').replace(/\D/g, '')
  if (digits.startsWith('8') && digits.length === 11) {
    digits = `7${digits.slice(1)}`
  } else if (digits.length === 10) {
    digits = `7${digits}`
  }

  const normalized = `+${digits}`
  if (!normalizedPhonePattern.test(normalized)) {
    throw new Error('Некорректный номер телефона')
  }
  return normalized
}
