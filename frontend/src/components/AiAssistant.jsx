import { useEffect, useLayoutEffect, useMemo, useRef, useState } from 'react'
import {
  cancelAiAppointment,
  cancelAiWaitlist,
  confirmAiAppointment,
  confirmAiWaitlist,
  deleteAiConversation,
  getAiAvailableSlots,
  getAiConversations,
  getAiMessages,
  getPatientSubjects,
  prepareAiAppointment,
  streamAiChat,
} from '../api/apiClient.js'
import { useAuth } from '../auth/AuthContext.jsx'
import { useTranslation } from '../i18n/index.jsx'
import { translateError } from '../i18n/core.js'
import { usePresence } from '../motion/Motion.jsx'
import { isNearBottom } from '../motion/motion.js'
import Icon from './ui/Icon.jsx'

const roleSuggestions = {
  PATIENT: ['booking', 'documents', 'health'],
  DOCTOR: ['doctorNavigation', 'icd10', 'documents'],
  NURSE: ['schedule', 'vitals', 'preparation'],
  ADMIN: ['clinics', 'staff', 'schedules'],
}

function makeId(prefix) {
  return `${prefix}-${Date.now()}-${Math.random().toString(36).slice(2)}`
}

function todayIso() {
  return new Date().toISOString().slice(0, 10)
}

function ToolResult({ item, t, subjects, canBook, onShowSlots, onPrepareSlot, onConfirmAppointment, onCancelAppointment, onConfirmWaitlist, onCancelWaitlist, busy }) {
  const { tool, data } = item
  if (!item.success) return <div className="ai-tool-error">{item.message || t('ai.toolError')}</div>
  if (tool === 'search_doctors') return <div className="ai-tool-results"><strong>{t('ai.doctorsFound')}</strong>{data?.doctors?.length ? data.doctors.map((doctor) => <DoctorCard key={doctor.id} doctor={doctor} requestedDate={data.requestedDate} t={t} onShowSlots={onShowSlots} />) : <p>{t('ai.noDoctors')}</p>}</div>
  if (tool === 'get_available_slots') return <div className="ai-tool-results"><strong>{t('ai.availableSlots')}</strong>{data?.slots?.length ? data.slots.map((slot) => <SlotCard key={slot.id} slot={slot} doctor={data.doctor} date={data.date} subjects={subjects} canBook={canBook} t={t} onPrepareSlot={onPrepareSlot} busy={busy} />) : <p>{t('ai.noSlots')}</p>}</div>
  if (tool === 'list_patient_subjects') return <div className="ai-tool-results"><strong>{t('ai.subjects')}</strong>{data?.map((subject) => <div className="ai-subject-card" key={`${subject.type}-${subject.id}`}><span>{subject.fullName}</span><small>{subject.type === 'DEPENDENT' ? subject.relationship || t('ai.dependent') : t('ai.self')}</small></div>)}</div>
  if (tool === 'prepare_appointment' || tool === 'appointment_confirmation') return <AppointmentCard proposal={data} t={t} onConfirm={() => onConfirmAppointment(item.id, data)} onCancel={() => onCancelAppointment(item.id, data)} busy={busy} />
  if (tool === 'prepare_waitlist' || tool === 'waitlist_confirmation') return <WaitlistCard proposal={data} t={t} onConfirm={() => onConfirmWaitlist(item.id, data)} onCancel={() => onCancelWaitlist(item.id, data)} busy={busy} />
  return null
}

function DoctorCard({ doctor, requestedDate, t, onShowSlots }) {
  return <article className="ai-doctor-card"><div><strong>{doctor.fullName}</strong><span>{doctor.specialty?.name}</span></div><p>{doctor.clinic?.name} · {doctor.clinic?.cityName}</p>{doctor.nearestSlots?.length > 0 && <small>{t('ai.nextSlot')}: {doctor.nearestSlots[0].startsAt?.replace('T', ' ')}</small>}<button type="button" onClick={() => onShowSlots(doctor.id, requestedDate || todayIso())}>{t('ai.viewSlots')}</button></article>
}

function SlotCard({ slot, doctor, date, subjects, canBook, t, onPrepareSlot, busy }) {
  return <article className="ai-slot-card"><div><strong>{slot.startsAt?.slice(11, 16)}</strong><span>{doctor?.fullName}</span></div><small>{date} · {doctor?.clinic?.name}</small>{canBook && <select aria-label={t('ai.subject')} defaultValue="" disabled={busy} onChange={(event) => onPrepareSlot(slot, event.target.value ? Number(event.target.value) : null)}><option value="">{t('ai.bookForSelf')}</option>{subjects.filter((subject) => subject.type === 'DEPENDENT').map((subject) => <option key={subject.id} value={subject.id}>{subject.fullName}</option>)}</select>}</article>
}

function AppointmentCard({ proposal, t, onConfirm, onCancel, busy }) {
  if (!proposal) return null
  const confirmed = proposal.status === 'CONFIRMED' && proposal.appointment
  return <article className={`ai-proposal-card ${confirmed ? 'is-confirmed' : ''}`}><strong>{confirmed ? t('ai.appointmentConfirmed') : t('ai.confirmAppointment')}</strong><p>{proposal.subject?.fullName} · {proposal.doctor?.fullName}</p><p>{proposal.clinic?.name} · {proposal.appointmentAt?.replace('T', ' ')}</p>{!confirmed && proposal.status === 'PREPARED' && <div className="ai-proposal-card__actions"><button type="button" disabled={busy} onClick={() => onConfirm(proposal)}>{t('ai.confirm')}</button><button type="button" className="is-secondary" disabled={busy} onClick={() => onCancel(proposal)}>{t('ai.cancel')}</button></div>}</article>
}

function WaitlistCard({ proposal, t, onConfirm, onCancel, busy }) {
  if (!proposal) return null
  const confirmed = proposal.status === 'CONFIRMED' && proposal.waitlist
  return <article className={`ai-proposal-card ${confirmed ? 'is-confirmed' : ''}`}><strong>{confirmed ? t('ai.waitlistConfirmed') : t('ai.confirmWaitlist')}</strong><p>{proposal.subject?.fullName} · {proposal.specialtyName}</p><p>{proposal.clinic?.name} · {proposal.desiredDateFrom} — {proposal.desiredDateTo}</p>{!confirmed && proposal.status === 'PREPARED' && <div className="ai-proposal-card__actions"><button type="button" disabled={busy} onClick={() => onConfirm(proposal)}>{t('ai.confirm')}</button><button type="button" className="is-secondary" disabled={busy} onClick={() => onCancel(proposal)}>{t('ai.cancel')}</button></div>}</article>
}

function AiAssistant() {
  const { user, token, isAuthenticated } = useAuth()
  const { t, language } = useTranslation()
  const [open, setOpen] = useState(false)
  const [historyOpen, setHistoryOpen] = useState(false)
  const [conversations, setConversations] = useState([])
  const [subjects, setSubjects] = useState([])
  const [conversationId, setConversationId] = useState(null)
  const [messages, setMessages] = useState([])
  const [draft, setDraft] = useState('')
  const [loading, setLoading] = useState(false)
  const [historyLoading, setHistoryLoading] = useState(false)
  const [actionKey, setActionKey] = useState('')
  const [error, setError] = useState('')
  const abortRef = useRef(null)
  const scrollRef = useRef(null)
  const panelRef = useRef(null)
  const launcherRef = useRef(null)
  const followRef = useRef(true)
  const scrollTopRef = useRef(0)
  const openedRef = useRef(false)
  const present = usePresence(open, panelRef)

  const suggestions = useMemo(() => roleSuggestions[user?.role] || roleSuggestions.PATIENT, [user?.role])

  useEffect(() => {
    if (!open || !isAuthenticated) return undefined
    let cancelled = false
    setHistoryLoading(true)
    const subjectRequest = user?.role === 'PATIENT' ? getPatientSubjects().catch(() => []) : Promise.resolve([])
    Promise.all([getAiConversations(), subjectRequest])
      .then(([items, ownSubjects]) => {
        if (!cancelled) { setConversations(items || []); setSubjects(ownSubjects || []) }
      })
      .catch((requestError) => { if (!cancelled) setError(translateError(requestError, t, 'ai.unavailable')) })
      .finally(() => { if (!cancelled) setHistoryLoading(false) })
    return () => { cancelled = true }
  }, [open, isAuthenticated, user?.role, t])

  useLayoutEffect(() => {
    const element = scrollRef.current
    if (!open || !element) return
    // Streaming follows only a reader already at the bottom. Scroll instantly
    // per rendered update; repeated smooth-scroll commands fight one another.
    element.scrollTop = followRef.current ? element.scrollHeight : scrollTopRef.current
  }, [open, messages, loading])
  useEffect(() => {
    if (open) { openedRef.current = true; panelRef.current?.querySelector('textarea')?.focus({ preventScroll: true }) }
    else if (openedRef.current) launcherRef.current?.focus({ preventScroll: true })
  }, [open])
  useEffect(() => () => abortRef.current?.abort(), [])

  if (!isAuthenticated || !user) return null

  const addToolMessage = (tool, data, success = true, message = '') => setMessages((items) => [...items, { id: makeId('tool-message'), role: 'assistant', content: '', tools: [{ id: makeId('tool'), tool, data, success, message }] }])
  const updateTool = (toolId, data) => setMessages((items) => items.map((message) => ({ ...message, tools: message.tools?.map((tool) => tool.id === toolId ? { ...tool, data } : tool) })))

  const selectConversation = async (id) => {
    if (loading) return
    followRef.current = true
    setHistoryOpen(false); setError(''); setConversationId(id)
    try {
      const loaded = await getAiMessages(id)
      setMessages((loaded || []).map((message) => ({ id: message.id, role: message.role === 'USER' ? 'user' : 'assistant', content: message.content })))
    } catch (requestError) { setError(translateError(requestError, t, 'ai.historyError')) }
  }

  const startNewChat = () => {
    if (loading) return
    setConversationId(null); setMessages([]); setDraft(''); setError(''); setHistoryOpen(false)
  }

  const removeConversation = async (event, id) => {
    event.stopPropagation()
    if (loading) return
    try {
      await deleteAiConversation(id)
      setConversations((items) => items.filter((item) => item.id !== id))
      if (conversationId === id) startNewChat()
    } catch (requestError) { setError(translateError(requestError, t, 'ai.historyError')) }
  }

  const showSlots = async (doctorId, date) => {
    setActionKey(`slots-${doctorId}-${date}`)
    try { addToolMessage('get_available_slots', await getAiAvailableSlots(doctorId, date)) } catch (requestError) { setError(translateError(requestError, t, 'ai.unavailable')) } finally { setActionKey('') }
  }

  const prepareSlot = async (slot, dependentId) => {
    setActionKey(`prepare-${slot.id}`)
    try { addToolMessage('prepare_appointment', await prepareAiAppointment({ doctorId: slot.doctorId, timeSlotId: slot.id, dependentId })) } catch (requestError) { setError(translateError(requestError, t, 'ai.unavailable')) } finally { setActionKey('') }
  }

  const confirmAppointment = async (toolId, proposal) => {
    setActionKey(`confirm-appointment-${proposal.proposalId}`)
    try { updateTool(toolId, await confirmAiAppointment(proposal.proposalId)) } catch (requestError) { setError(translateError(requestError, t, 'ai.unavailable')) } finally { setActionKey('') }
  }

  const cancelAppointment = async (toolId, proposal) => {
    setActionKey(`cancel-appointment-${proposal.proposalId}`)
    try { await cancelAiAppointment(proposal.proposalId); updateTool(toolId, { ...proposal, status: 'CANCELLED' }) } catch (requestError) { setError(translateError(requestError, t, 'ai.unavailable')) } finally { setActionKey('') }
  }

  const confirmWaitlist = async (toolId, proposal) => {
    setActionKey(`confirm-waitlist-${proposal.proposalId}`)
    try { updateTool(toolId, await confirmAiWaitlist(proposal.proposalId)) } catch (requestError) { setError(translateError(requestError, t, 'ai.unavailable')) } finally { setActionKey('') }
  }

  const cancelWaitlist = async (toolId, proposal) => {
    setActionKey(`cancel-waitlist-${proposal.proposalId}`)
    try { await cancelAiWaitlist(proposal.proposalId); updateTool(toolId, { ...proposal, status: 'CANCELLED' }) } catch (requestError) { setError(translateError(requestError, t, 'ai.unavailable')) } finally { setActionKey('') }
  }

  const sendMessage = async (event) => {
    event?.preventDefault()
    const message = draft.trim()
    if (!message || loading) return
    followRef.current = true
    const assistantId = makeId('assistant')
    setMessages((items) => [...items, { id: makeId('user'), role: 'user', content: message }, { id: assistantId, role: 'assistant', content: '', tools: [] }])
    setDraft(''); setError(''); setLoading(true)
    const controller = new AbortController(); abortRef.current = controller
    try {
      await streamAiChat({ conversationId, message, language }, {
        token,
        signal: controller.signal,
        onEvent: (type, payload) => {
          if (type === 'meta') setConversationId(payload.conversationId)
          if (type === 'delta') setMessages((items) => items.map((item) => item.id === assistantId ? { ...item, content: item.content + payload.text } : item))
          if (type === 'tool_result') setMessages((items) => items.map((item) => item.id === assistantId ? { ...item, tools: [...(item.tools || []), { id: makeId('tool'), tool: payload.tool, data: payload.data, success: payload.success, message: payload.message }] } : item))
          if (type === 'error') { setError(payload.code === 'ai_unavailable' ? t('ai.unavailable') : (payload.message || t('ai.unavailable'))); setMessages((items) => items.filter((item) => item.id !== assistantId)) }
        },
      })
      setConversations((await getAiConversations()) || [])
    } catch (requestError) {
      if (requestError?.name !== 'AbortError') { setError(requestError?.status === 503 ? t('ai.unavailable') : translateError(requestError, t, 'ai.unavailable')); setMessages((items) => items.filter((item) => item.id !== assistantId)) }
    } finally { abortRef.current = null; setLoading(false) }
  }

  const stopGeneration = () => { abortRef.current?.abort(); setLoading(false) }

  return <>
    {!open && <button ref={launcherRef} className="ai-launcher" type="button" onClick={() => setOpen(true)} aria-label={t('ai.open')} title={t('ai.open')} aria-haspopup="dialog" aria-expanded={open} aria-controls="qamqormed-ai-panel"><span className="ai-launcher__icon" aria-hidden="true"><Icon name="sparkles" size={24} /></span><span className="ai-launcher__label">{t('ai.shortTitle')}</span></button>}
    {present && <div ref={panelRef} id="qamqormed-ai-panel" className="ai-assistant" role="dialog" inert={!open} aria-modal="false" aria-labelledby="ai-title" onKeyDown={(event) => { if (event.key === 'Escape') { event.stopPropagation(); setOpen(false) } }}>
      <header className="ai-assistant__header"><div className="ai-assistant__identity"><span className="ai-assistant__mark" aria-hidden="true"><Icon name="sparkles" size={24} /></span><div><strong id="ai-title">QamqorMed AI</strong><span>{t('ai.subtitle')}</span></div></div><div className="ai-assistant__actions"><button type="button" className="ai-icon-button" onClick={startNewChat} title={t('ai.newChat')} aria-label={t('ai.newChat')}><Icon name="plus" /></button><button type="button" className="ai-icon-button" onClick={() => setOpen(false)} title={t('ai.close')} aria-label={t('ai.close')}><Icon name="close" /></button></div></header>
      <div className="ai-assistant__toolbar"><button type="button" className={`ai-history-button ${historyOpen ? 'is-active' : ''}`} onClick={() => setHistoryOpen((value) => !value)}>◷ {t('ai.history')} {conversations.length > 0 && <span>{conversations.length}</span>}</button><span className="ai-role-badge">{t(`roles.${user.role}`)}</span></div>
      {historyOpen && <div className="ai-history" aria-label={t('ai.history')}>{historyLoading && <p className="ai-history__empty">{t('common.loading')}</p>}{!historyLoading && conversations.length === 0 && <p className="ai-history__empty">{t('ai.noHistory')}</p>}{!historyLoading && conversations.map((conversation) => <button className={`ai-history__item ${conversation.id === conversationId ? 'is-active' : ''}`} key={conversation.id} type="button" onClick={() => selectConversation(conversation.id)}><span>{conversation.title || t('ai.newChat')}</span><b onClick={(event) => removeConversation(event, conversation.id)} aria-label={t('ai.deleteChat')}>×</b></button>)}</div>}
      <div className="ai-assistant__messages" ref={scrollRef} role="log" aria-label={t('ai.history')} tabIndex={0} aria-live="off" aria-busy={loading} onScroll={(event) => { followRef.current = isNearBottom(event.currentTarget); scrollTopRef.current = event.currentTarget.scrollTop }}>
        {messages.length === 0 && <div className="ai-welcome"><div className="ai-welcome__halo">✦</div><h2>{t('ai.greeting', { name: user.firstName })}</h2><p>{t('ai.description')}</p><div className="ai-suggestions">{suggestions.map((key) => <button type="button" key={key} onClick={() => setDraft(t(`ai.suggestions.${key}`))}>{t(`ai.suggestions.${key}`)}</button>)}</div></div>}
        {messages.map((message) => <div className={`ai-message ai-message--${message.role}`} key={message.id}><span className="ai-message__avatar" aria-hidden="true">{message.role === 'assistant' ? '✦' : (user.firstName?.[0] || 'U')}</span><div className="ai-message__body">{(message.content || (loading && message.role === 'assistant')) && <div className="ai-message__bubble">{message.content || (loading && <span role="status"><span className="ai-typing" aria-hidden="true"><i /><i /><i /></span><span className="ai-typing__label">{t('common.loading')}</span></span>)}</div>}{message.tools?.map((tool) => <ToolResult key={tool.id} item={tool} t={t} subjects={subjects} canBook={user.role === 'PATIENT'} onShowSlots={showSlots} onPrepareSlot={prepareSlot} onConfirmAppointment={confirmAppointment} onCancelAppointment={cancelAppointment} onConfirmWaitlist={confirmWaitlist} onCancelWaitlist={cancelWaitlist} busy={Boolean(actionKey)} />)}</div></div>)}
        {error && <div className="ai-error" role="alert">{error}</div>}
      </div>
      <form className="ai-composer" onSubmit={sendMessage}><textarea value={draft} onChange={(event) => setDraft(event.target.value)} placeholder={t('ai.placeholder')} maxLength={4000} rows={1} disabled={loading} onKeyDown={(event) => { if (event.key === 'Enter' && !event.shiftKey) { event.preventDefault(); sendMessage(event) } }} /><button type={loading ? 'button' : 'submit'} onClick={loading ? stopGeneration : undefined} className={loading ? 'is-stop' : ''} aria-label={loading ? t('ai.stop') : t('ai.send')}>{loading ? '■' : '➤'}</button><small>{t('ai.disclaimer')}</small></form>
    </div>}
  </>
}

export default AiAssistant
