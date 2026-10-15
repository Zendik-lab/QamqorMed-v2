import { useEffect, useMemo, useState } from 'react'
import {
  addDoctorTemplateVersion,
  approveDoctorDocument,
  createDoctorDocumentDraft,
  createDoctorDocumentTemplate,
  downloadDoctorDocumentPdf,
  favoriteDoctorTemplate,
  getDoctorDocumentTemplates,
  saveDoctorDocumentDraft,
  setDoctorDefaultTemplate,
  unfavoriteDoctorTemplate,
} from '../../api/apiClient.js'
import { translateError } from '../../i18n/index.jsx'

const emptyNewTemplate = { name: '', documentType: 'CONCLUSION', language: 'RU', content: '' }

export default function DocumentComposer({ appointmentId, language, readOnly, t, onSaved }) {
  const [open, setOpen] = useState(false)
  const [templates, setTemplates] = useState([])
  const [selectedId, setSelectedId] = useState('')
  const [typeFilter, setTypeFilter] = useState('')
  const [document, setDocument] = useState(null)
  const [content, setContent] = useState('')
  const [newTemplate, setNewTemplate] = useState(emptyNewTemplate)
  const [showNewTemplate, setShowNewTemplate] = useState(false)
  const [loading, setLoading] = useState(false)
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState('')
  const [notice, setNotice] = useState('')

  const apiLanguage = useMemo(() => language === 'kk' ? 'KK' : language === 'en' ? 'EN' : 'RU', [language])
  const selected = templates.find((item) => String(item.id) === String(selectedId))

  useEffect(() => {
    if (!open) return
    setLoading(true); setError('')
    getDoctorDocumentTemplates({ language: apiLanguage, type: typeFilter || undefined })
      .then((items) => { setTemplates(items || []); const preferred = items?.find((item) => item.defaultTemplate) || items?.[0]; if (!selectedId && preferred) setSelectedId(String(preferred.id)) })
      .catch((requestError) => setError(translateError(requestError, t, 'documentEditor.loadError')))
      .finally(() => setLoading(false))
  }, [open, apiLanguage, typeFilter])

  const useTemplate = async () => {
    if (!selected) return
    setBusy(true); setError(''); setNotice('')
    try {
      const value = await createDoctorDocumentDraft(appointmentId, { templateId: selected.id })
      setDocument(value); setContent(value.draftContent || ''); setNotice(t('documentEditor.saved')); onSaved?.(value)
    } catch (requestError) { setError(translateError(requestError, t, 'common.error')) }
    finally { setBusy(false) }
  }

  const save = async () => {
    if (!document) return
    setBusy(true); setError(''); setNotice('')
    try { const value = await saveDoctorDocumentDraft(document.id, { content }); setDocument(value); setContent(value.draftContent || content); setNotice(t('documentEditor.saved')); onSaved?.(value) }
    catch (requestError) { setError(translateError(requestError, t, 'common.error')) }
    finally { setBusy(false) }
  }

  const approve = async () => {
    if (!document) return
    setBusy(true); setError(''); setNotice('')
    try { const value = await approveDoctorDocument(document.id); setDocument(value); setContent(value.finalizedContent || content); setNotice(t('documentEditor.approvedNotice')); onSaved?.(value) }
    catch (requestError) { setError(translateError(requestError, t, 'documentEditor.missingFields')) }
    finally { setBusy(false) }
  }

  const toggleFavorite = async (item) => {
    try {
      if (item.favorite) await unfavoriteDoctorTemplate(item.id)
      else await favoriteDoctorTemplate(item.id)
      setTemplates((current) => current.map((value) => value.id === item.id ? { ...value, favorite: !item.favorite } : value))
    } catch (requestError) { setError(translateError(requestError, t, 'common.error')) }
  }

  const makeDefault = async (item) => {
    try { await setDoctorDefaultTemplate(item.documentType, item.id); setTemplates((current) => current.map((value) => ({ ...value, defaultTemplate: value.id === item.id }))); setNotice(t('documentEditor.saved')) }
    catch (requestError) { setError(translateError(requestError, t, 'common.error')) }
  }

  const createTemplate = async (event) => {
    event.preventDefault(); setBusy(true); setError('')
    try {
      const value = await createDoctorDocumentTemplate({ ...newTemplate, specialtyId: null })
      setTemplates((current) => [...current, value]); setSelectedId(String(value.id)); setNewTemplate(emptyNewTemplate); setShowNewTemplate(false); setNotice(t('documentEditor.saved'))
    } catch (requestError) { setError(translateError(requestError, t, 'common.error')) }
    finally { setBusy(false) }
  }

  return <div className="document-composer">
    {!open && <button className="button" type="button" disabled={readOnly} onClick={() => setOpen(true)}>＋ {t('documentEditor.create')}</button>}
    {open && <section className="document-composer__panel" aria-label={t('documentEditor.create')}>
      <div className="stage-heading"><div><span className="eyebrow">{t('documentEditor.templates')}</span><h3>{document ? document.title : t('documentEditor.chooseTemplate')}</h3></div><button className="button button--secondary button--compact" type="button" onClick={() => setOpen(false)}>×</button></div>
      {error && <p className="error-message" role="alert">{error}</p>}{notice && <p className="success-message">{notice}</p>}
      {!document && <>
        <label className="field-group"><span className="field-label">{t('documentEditor.type')}</span><select value={typeFilter} onChange={(event) => { setTypeFilter(event.target.value); setSelectedId('') }}><option value="">{t('documentEditor.allTypes')}</option><option value="CONCLUSION">{t('workflow.conclusion')}</option><option value="CERTIFICATE">{t('workflow.certificate')}</option><option value="REFERRAL">{t('workflow.referral')}</option><option value="LAB_REQUEST">{t('workflow.labRequest')}</option><option value="PRESCRIPTION">{t('workflow.prescription')}</option><option value="REPORT">{t('workflow.result')}</option><option value="OTHER">{t('workflow.other')}</option></select></label>
        {loading ? <p>{t('common.loading')}</p> : templates.length ? <>
          <div className="template-grid">{templates.map((item) => <article className={'template-card ' + (String(item.id) === String(selectedId) ? 'template-card--selected' : '')} key={item.id} onClick={() => setSelectedId(String(item.id))}><div><strong>{item.name}</strong><small>{item.documentType} · {item.scope} · v{item.currentVersion || '—'}</small></div><div className="template-card__actions"><button type="button" className="button button--text" onClick={(event) => { event.stopPropagation(); toggleFavorite(item) }} aria-label={item.favorite ? t('documentEditor.unfavorite') : t('documentEditor.favorite')}>{item.favorite ? '★' : '☆'}</button><button type="button" className="button button--text" onClick={(event) => { event.stopPropagation(); makeDefault(item) }} aria-label={t('documentEditor.defaultTemplate')}>✓</button></div></article>)}</div>
          <div className="form-actions"><button className="button" type="button" disabled={busy || !selected} onClick={useTemplate}>{t('documentEditor.useTemplate')}</button><button className="button button--secondary" type="button" onClick={() => setShowNewTemplate((value) => !value)}>＋ {t('documentEditor.create')}</button></div>
        </> : <p className="empty-state">{t('documentEditor.empty')}</p>}
        {showNewTemplate && <form className="workflow-subform" onSubmit={createTemplate}><Field label={t('workflow.name')} value={newTemplate.name} onChange={(event) => setNewTemplate((current) => ({ ...current, name: event.target.value }))} required /><label className="field-group"><span className="field-label">{t('workflow.medicalDocument')}</span><select value={newTemplate.documentType} onChange={(event) => setNewTemplate((current) => ({ ...current, documentType: event.target.value }))}><option value="CONCLUSION">{t('workflow.conclusion')}</option><option value="CERTIFICATE">{t('workflow.certificate')}</option><option value="REFERRAL">{t('workflow.referral')}</option><option value="LAB_REQUEST">{t('workflow.labRequest')}</option><option value="PRESCRIPTION">{t('workflow.prescription')}</option></select></label><textarea value={newTemplate.content} onChange={(event) => setNewTemplate((current) => ({ ...current, content: event.target.value }))} rows="5" placeholder="{{patient.fullName}}" /><button className="button" type="submit" disabled={busy || !newTemplate.name || !newTemplate.content}>{t('common.save')}</button></form>}
      </>}
      {document && <>
        <p className="template-meta">{t('documentEditor.templateVersion')}: {document.templateVersion || '—'} · {document.status === 'APPROVED' ? t('documentEditor.approved') : t('documentEditor.draft')}</p>
        <label className="field-group"><span className="field-label">{t('documentEditor.content')}</span><textarea className="document-editor__textarea" value={content} onChange={(event) => setContent(event.target.value)} disabled={document.status !== 'DRAFT' || busy || readOnly} rows="16" /></label>
        <div className="form-actions">{document.status === 'DRAFT' && !readOnly && <><button className="button button--secondary" type="button" disabled={busy} onClick={save}>{t('documentEditor.saveDraft')}</button><button className="button" type="button" disabled={busy} onClick={approve}>{t('documentEditor.approve')}</button></>}<button className="button button--secondary" type="button" disabled={busy} onClick={async () => { const blob = await downloadDoctorDocumentPdf(document.id); const url = URL.createObjectURL(blob); window.open(url, '_blank', 'noopener,noreferrer'); window.setTimeout(() => URL.revokeObjectURL(url), 60000) }}>{t('documentEditor.downloadPdf')}</button></div>
      </>}
    </section>}
  </div>
}

function Field({ label, value, onChange, required = false }) { return <label className="field-group"><span className="field-label">{label}</span><input value={value} onChange={onChange} required={required} /></label> }
