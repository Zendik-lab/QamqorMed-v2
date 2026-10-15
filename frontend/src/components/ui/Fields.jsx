import { useId } from 'react'
import { useTranslation } from '../../i18n/index.jsx'

function useField({ id, hint, error, 'aria-describedby': describedBy }) {
  const generated = useId()
  const fieldId = id || generated
  return { fieldId, description: [describedBy, hint && `${fieldId}-hint`, error && `${fieldId}-error`].filter(Boolean).join(' ') || undefined }
}

function FieldFrame({ id, label, hint, error, required, children, className = '' }) {
  return <div className={`field-group ui-field ${error ? 'ui-field--invalid' : ''} ${className}`}>
    {label && <label className="field-label" htmlFor={id}>{label}{required && <span className="ui-field__required" aria-hidden="true"> *</span>}</label>}
    {children}{hint && <small className="ui-field__hint" id={`${id}-hint`}>{hint}</small>}
    {error && <small className="ui-field__error" id={`${id}-error`} role="alert">{error}</small>}
  </div>
}

export function Input({ label, hint, error, className, endAdornment, ...props }) {
  const { fieldId, description } = useField({ ...props, hint, error })
  const input = <input type="text" {...props} id={fieldId} value={Object.hasOwn(props, 'value') ? props.value ?? '' : undefined} aria-invalid={error ? true : undefined} aria-describedby={description} />
  return <FieldFrame id={fieldId} label={label} hint={hint} error={error} required={props.required} className={className}>
    {endAdornment ? <div className="ui-input-control">{input}{endAdornment}</div> : input}
  </FieldFrame>
}

export function TextArea({ label, hint, error, className, ...props }) {
  const { fieldId, description } = useField({ ...props, hint, error })
  return <FieldFrame id={fieldId} label={label} hint={hint} error={error} required={props.required} className={className}>
    <textarea rows={3} {...props} id={fieldId} value={Object.hasOwn(props, 'value') ? props.value ?? '' : undefined} aria-invalid={error ? true : undefined} aria-describedby={description} />
  </FieldFrame>
}

export function Select({ label, hint, error, className, options = [], optionLabel, emptyLabel, allowEmpty = true, children, ...props }) {
  const { t } = useTranslation()
  const { fieldId, description } = useField({ ...props, hint, error })
  return <FieldFrame id={fieldId} label={label} hint={hint} error={error} required={props.required} className={className}>
    <select {...props} id={fieldId} value={Object.hasOwn(props, 'value') ? props.value ?? '' : undefined} aria-invalid={error ? true : undefined} aria-describedby={description}>
      {allowEmpty && <option value="">{emptyLabel ?? t('common.select')}</option>}
      {children || options.map((item) => {
        const value = Array.isArray(item) ? item[0] : typeof item === 'object' ? item.id : item
        const text = optionLabel ? optionLabel(item) : Array.isArray(item) ? item[1] : typeof item === 'object' ? item.name : item
        return <option key={value} value={value}>{text}</option>
      })}
    </select>
  </FieldFrame>
}

export function Checkbox({ label, hint, error, className = '', ...props }) {
  const { fieldId, description } = useField({ ...props, hint, error })
  return <div className={`ui-choice ${className}`}><label className="checkbox-field" htmlFor={fieldId}><input type="checkbox" {...props} id={fieldId} aria-describedby={description} aria-invalid={error ? true : undefined} /><span>{label}</span></label>
    {hint && <small className="ui-field__hint" id={`${fieldId}-hint`}>{hint}</small>}{error && <small className="ui-field__error" role="alert" id={`${fieldId}-error`}>{error}</small>}
  </div>
}

export function Radio(props) { return <Checkbox {...props} type="radio" /> }
export function DatePicker(props) { return <Input {...props} type="date" /> }
