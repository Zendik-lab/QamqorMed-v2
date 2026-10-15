import { useEffect, useState } from 'react'
import {
  adminAddTemplateVersion,
  adminCreateDocumentTemplate,
  adminGetClinics,
  adminGetDocumentTemplates,
  adminGetSpecialties,
  adminGetTemplateVersions,
  adminImportIcd10,
  adminSetTemplateActive,
} from '../api/apiClient.js'
import { translateError, useTranslation } from '../i18n/index.jsx'

const types = ['REPORT', 'CERTIFICATE', 'CONCLUSION', 'PRESCRIPTION', 'REFERRAL', 'LAB_REQUEST', 'OTHER']
const scopes = ['SYSTEM', 'CLINIC']
const languages = ['RU', 'KK', 'EN']
const emptyForm = { name: '', documentType: 'CONCLUSION', scope: 'SYSTEM', language: 'RU', clinicId: '', specialtyId: '', content: 'Документ № {{document.number}}\nПациент: {{patient.fullName}}\n\n{{visit.recommendations}}', requiredFields: '' }

export default function AdminDocumentTemplatesPage() {
  const { t } = useTranslation()
  const [templates, setTemplates] = useState([])
  const [clinics, setClinics] = useState([])
  const [specialties, setSpecialties] = useState([])
  const [form, setForm] = useState(emptyForm)
  const [versions, setVersions] = useState({})
  const [versionDrafts, setVersionDrafts] = useState({})
  const [loading, setLoading] = useState(true)
  const [saving, setSaving] = useState(false)
  const [error, setError] = useState('')
  const [notice, setNotice] = useState('')
  const [icdForm, setIcdForm] = useState({ file: null, source: 'WHO', sourceVersion: '2019' })
  const [importing, setImporting] = useState(false)

  const load = () => adminGetDocumentTemplates().then(setTemplates)
  useEffect(() => {
    Promise.all([load(), adminGetClinics(), adminGetSpecialties()])
      .then(([, clinicItems, specialtyItems]) => { setClinics(clinicItems); setSpecialties(specialtyItems) })
      .catch((requestError) => setError(translateError(requestError, t, 'admin.templateLoadError')))
      .finally(() => setLoading(false))
  }, [t])

  const update = (name) => (event) => setForm((current) => ({ ...current, [name]: event.target.value }))
  const create = async (event) => {
    event.preventDefault(); setSaving(true); setError(''); setNotice('')
    try {
      await adminCreateDocumentTemplate({ ...form, clinicId: form.scope === 'CLINIC' && form.clinicId ? Number(form.clinicId) : null, specialtyId: form.specialtyId ? Number(form.specialtyId) : null })
      setForm(emptyForm); await load(); setNotice(t('admin.templateSaved'))
    } catch (requestError) { setError(translateError(requestError, t, 'admin.templateSaveError')) } finally { setSaving(false) }
  }
  const showVersions = async (id) => {
    if (versions[id]) { setVersions((current) => { const next = { ...current }; delete next[id]; return next }) ; return }
    try { const items = await adminGetTemplateVersions(id); setVersions((current) => ({ ...current, [id]: items })) } catch (requestError) { setError(translateError(requestError, t, 'admin.templateLoadError')) }
  }
  const addVersion = async (template) => {
    const content = versionDrafts[template.id] || template.content || ''
    if (!content.trim()) return
    setSaving(true); setError(''); setNotice('')
    try { await adminAddTemplateVersion(template.id, { content, requiredFields: template.requiredFields, changeDescription: t('admin.templateVersionReason') }); await load(); setVersions((current) => ({ ...current, [template.id]: undefined })); setNotice(t('admin.templateVersionSaved')) } catch (requestError) { setError(translateError(requestError, t, 'admin.templateSaveError')) } finally { setSaving(false) }
  }
  const toggle = async (template) => {
    try { await adminSetTemplateActive(template.id, !template.active); await load() } catch (requestError) { setError(translateError(requestError, t, 'admin.templateSaveError')) }
  }
  const importIcd10 = async (event) => {
    event.preventDefault(); if (!icdForm.file) return
    setImporting(true); setError(''); setNotice('')
    try { const result = await adminImportIcd10(icdForm.file, icdForm.source, icdForm.sourceVersion); setNotice(t('admin.icd10Imported', result)) } catch (requestError) { setError(translateError(requestError, t, 'admin.icd10ImportError')) } finally { setImporting(false) }
  }

  if (loading) return <section className="page-shell"><p className="route-message">{t('common.loading')}</p></section>
  return <section className="page-shell admin-page">
    <div className="page-heading"><div className="eyebrow">{t('admin.onlyAdmin')}</div><h1>{t('nav.documentTemplates')}</h1><p className="form-description">{t('admin.templateDescription')}</p></div>
    {notice && <p className="success-message">{notice}</p>}{error && <p className="error-message" role="alert">{error}</p>}
    <section className="admin-section"><div className="section-heading"><div><h2>{t('admin.icd10Import')}</h2><p>{t('admin.icd10ImportDescription')}</p></div></div><form className="admin-form" onSubmit={importIcd10}><div className="form-grid"><Field label={t('admin.icd10Source')} value={icdForm.source} onChange={(event) => setIcdForm((current) => ({ ...current, source: event.target.value }))} required /><Field label={t('admin.icd10SourceVersion')} value={icdForm.sourceVersion} onChange={(event) => setIcdForm((current) => ({ ...current, sourceVersion: event.target.value }))} required /><label className="field-group"><span className="field-label">{t('admin.icd10File')}</span><input type="file" accept=".csv,text/csv" onChange={(event) => setIcdForm((current) => ({ ...current, file: event.target.files?.[0] || null }))} required /></label></div><div className="form-actions"><button className="button button--secondary" type="submit" disabled={importing || !icdForm.file}>{importing ? t('admin.saving') : t('admin.icd10ImportAction')}</button></div></form></section>
    <section className="admin-section"><div className="section-heading"><div><h2>{t('admin.templateCreate')}</h2><p>{t('admin.templateCreateDescription')}</p></div></div><form className="admin-form" onSubmit={create}><div className="form-grid"><Field label={t('admin.name')} value={form.name} onChange={update('name')} required /><Select label={t('admin.templateType')} value={form.documentType} onChange={update('documentType')} options={types} /><Select label={t('admin.templateScope')} value={form.scope} onChange={update('scope')} options={scopes} /><Select label={t('admin.templateLanguage')} value={form.language} onChange={update('language')} options={languages} />{form.scope === 'CLINIC' && <Select label={t('admin.clinic')} value={form.clinicId} onChange={update('clinicId')} options={clinics} optionLabel={(item) => `${item.name} (${item.cityName || ''})`} required />}<Select label={t('admin.specialty')} value={form.specialtyId} onChange={update('specialtyId')} options={specialties} /></div><label className="field-group"><span className="field-label">{t('admin.templateContent')}</span><textarea rows="8" value={form.content} onChange={update('content')} required /></label><label className="field-group"><span className="field-label">{t('admin.templateRequiredFields')}</span><input value={form.requiredFields} onChange={update('requiredFields')} placeholder="patient.fullName, diagnosis.code" /></label><div className="form-actions"><button className="button" type="submit" disabled={saving}>{saving ? t('admin.saving') : t('admin.addAction')}</button></div></form></section>
    <section className="admin-section"><div className="section-heading"><div><h2>{t('admin.templateLibrary')}</h2><p>{t('admin.templateLibraryDescription')}</p></div><span className="count-badge">{templates.length}</span></div><div className="admin-table">{templates.map((template) => <article className="admin-row admin-template-row" key={template.id}><div className="admin-template-row__main"><span><strong>{template.name}</strong><small>{template.documentType} · {template.scope} · {template.language} · v{template.currentVersion || '—'}</small></span><span className={`status-badge ${template.active ? 'status-badge--active' : ''}`}>{template.active ? t('common.active') : t('common.inactive')}</span></div><div className="admin-template-row__actions"><button className="text-button" type="button" onClick={() => showVersions(template.id)}>{t('admin.templateHistory')}</button><button className="text-button" type="button" onClick={() => toggle(template)}>{template.active ? t('admin.templateArchive') : t('admin.templateRestore')}</button></div>{versions[template.id] && <div className="template-version-panel"><div className="template-version-list">{versions[template.id].map((version) => <div className="template-version" key={version.id}><strong>v{version.versionNumber}</strong><span>{version.changeDescription || t('admin.templateVersion')}</span><small>{version.createdAt?.slice(0, 10)}</small><button className="text-button" type="button" onClick={() => setVersionDrafts((current) => ({ ...current, [template.id]: version.content }))}>{t('admin.templateRestore')}</button><pre className="template-version-content">{version.content}</pre></div>)}</div><textarea rows="5" value={versionDrafts[template.id] ?? template.content ?? ''} onChange={(event) => setVersionDrafts((current) => ({ ...current, [template.id]: event.target.value }))} /><button className="button button--secondary button--compact" type="button" disabled={saving} onClick={() => addVersion(template)}>{t('admin.templateNewVersion')}</button></div>}</article>)}</div></section>
  </section>
}

function Field({ label, value, onChange, ...props }) { return <label className="field-group"><span className="field-label">{label}</span><input value={value ?? ''} onChange={onChange} {...props} /></label> }
function Select({ label, value, onChange, options = [], optionLabel = (item) => item.name || item, ...props }) { const { t } = useTranslation(); return <label className="field-group"><span className="field-label">{label}</span><select value={value ?? ''} onChange={onChange} {...props}><option value="">{t('admin.choose')}</option>{options.map((item) => <option value={item.id || item} key={item.id || item}>{optionLabel(item)}</option>)}</select></label> }
