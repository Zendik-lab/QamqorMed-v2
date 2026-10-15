import { useId, useRef, useState } from 'react'
import { useTranslation, translateError } from '../../i18n/index.jsx'
import { Input } from '../ui/Fields.jsx'
import Icon from '../ui/Icon.jsx'
import { passwordRequirements, validateAccount } from '../../utils/accountValidation.js'

export function useAccountForm(initialValues, mode) {
  const { t } = useTranslation()
  const [values, setValues] = useState(initialValues)
  const [touched, setTouched] = useState({})
  const [serverError, setServerError] = useState(null)
  const [serverFields, setServerFields] = useState({})
  const [busy, setBusy] = useState(false)
  const pending = useRef(false)
  const formRef = useRef(null)
  const summaryRef = useRef(null)
  const validation = validateAccount(values, mode)
  const focusInvalid = () => requestAnimationFrame(() => formRef.current?.querySelector('[aria-invalid="true"]')?.focus())
  const field = (name) => ({
    name, value: values[name],
    onChange: (event) => {
      setValues((current) => ({ ...current, [name]: event.target.value }))
      setServerError(null)
      setServerFields((current) => ({ ...current, [name]: null }))
    },
    onBlur: () => setTouched((current) => ({ ...current, [name]: true })),
    error: touched[name] && validation[name] ? t(validation[name]) : serverFields[name] ? translateError({ message: String(serverFields[name]), status: 400 }, t, 'errors.validation') : '',
  })
  const submit = (action) => async (event) => {
    event.preventDefault()
    if (pending.current) return
    setTouched(Object.fromEntries(Object.keys(values).map((key) => [key, true])))
    setServerError(null); setServerFields({})
    if (Object.keys(validation).length) { focusInvalid(); return }
    pending.current = true; setBusy(true)
    try { await action(values) }
    catch (error) {
      const mapped = { ...error.errors }
      if (mode === 'password' && mapped.password) { mapped.newPassword = mapped.password; delete mapped.password }
      setServerFields(mapped); setServerError(error)
      requestAnimationFrame(() => summaryRef.current?.focus())
    } finally { pending.current = false; setBusy(false) }
  }
  const reset = () => { setValues(initialValues); setTouched({}); setServerError(null); setServerFields({}) }
  return { values, field, busy, submit, formRef, summaryRef, error: serverError ? translateError(serverError, t) : '', reset }
}

export function PasswordInput({ id, label, error, hint, ...props }) {
  const { t } = useTranslation()
  const generated = useId()
  const fieldId = id || generated
  const [visible, setVisible] = useState(false)
  return <div className="password-field">
    <Input {...props} id={fieldId} type={visible ? 'text' : 'password'} label={label} error={error} hint={hint} className="password-field__input" endAdornment={<button className="password-field__toggle" type="button" aria-label={`${t(visible ? 'authFlow.hidePassword' : 'authFlow.showPassword')}: ${label}`} aria-controls={fieldId} aria-pressed={visible} onClick={() => setVisible((value) => !value)}><Icon name={visible ? 'eyeOff' : 'eye'} size={20} /></button>} />
  </div>
}

export function PasswordRequirements({ value }) {
  const { t } = useTranslation()
  const requirements = passwordRequirements(value)
  return <div className="password-requirements"><span>{t('authFlow.passwordRequirements')}</span><ul>{Object.entries(requirements).map(([key, met]) => <li className={met ? 'is-met' : ''} key={key}><Icon name={met ? 'check' : 'info'} size={15} /><span>{t(`authFlow.rule${key[0].toUpperCase()}${key.slice(1)}`)}</span><span className="sr-only">{met ? t('authFlow.ruleMet') : t('authFlow.ruleUnmet')}</span></li>)}</ul></div>
}
