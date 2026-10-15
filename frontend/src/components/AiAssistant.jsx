import { useEffect, useMemo, useRef, useState } from 'react'
import {
  deleteAiConversation,
  getAiConversations,
  getAiMessages,
  streamAiChat,
} from '../api/apiClient.js'
import { useAuth } from '../auth/AuthContext.jsx'
import { useTranslation } from '../i18n/index.jsx'
import { translateError } from '../i18n/core.js'

const roleSuggestions = {
  PATIENT: ['booking', 'documents', 'health'],
  DOCTOR: ['doctorNavigation', 'icd10', 'documents'],
  NURSE: ['schedule', 'vitals', 'preparation'],
  ADMIN: ['clinics', 'staff', 'schedules'],
}

function makeId(prefix) {
  return `${prefix}-${Date.now()}-${Math.random().toString(36).slice(2)}`
}

function AiAssistant() {
  const { user, isAuthenticated } = useAuth()
  const { t, language } = useTranslation()
  const [open, setOpen] = useState(false)
  const [historyOpen, setHistoryOpen] = useState(false)
  const [conversations, setConversations] = useState([])
  const [conversationId, setConversationId] = useState(null)
  const [messages, setMessages] = useState([])
  const [draft, setDraft] = useState('')
  const [loading, setLoading] = useState(false)
  const [historyLoading, setHistoryLoading] = useState(false)
  const [error, setError] = useState('')
  const abortRef = useRef(null)
  const scrollRef = useRef(null)

  const suggestions = useMemo(() => roleSuggestions[user?.role] || roleSuggestions.PATIENT, [user?.role])

  useEffect(() => {
    if (!open || !isAuthenticated) return undefined
    let cancelled = false
    setHistoryLoading(true)
    getAiConversations()
      .then((items) => {
        if (!cancelled) setConversations(items || [])
      })
      .catch((requestError) => {
        if (!cancelled) setError(translateError(requestError, t, 'ai.unavailable'))
      })
      .finally(() => {
        if (!cancelled) setHistoryLoading(false)
      })
    return () => { cancelled = true }
  }, [open, isAuthenticated, t])

  useEffect(() => {
    if (!scrollRef.current) return
    scrollRef.current.scrollTop = scrollRef.current.scrollHeight
  }, [messages, loading])

  useEffect(() => () => abortRef.current?.abort(), [])

  if (!isAuthenticated || !user) return null

  const selectConversation = async (id) => {
    if (loading) return
    setHistoryOpen(false)
    setError('')
    setConversationId(id)
    try {
      const loaded = await getAiMessages(id)
      setMessages((loaded || []).map((message) => ({
        id: message.id,
        role: message.role === 'USER' ? 'user' : 'assistant',
        content: message.content,
      })))
    } catch (requestError) {
      setError(translateError(requestError, t, 'ai.historyError'))
    }
  }

  const startNewChat = () => {
    if (loading) return
    setConversationId(null)
    setMessages([])
    setDraft('')
    setError('')
    setHistoryOpen(false)
  }

  const removeConversation = async (event, id) => {
    event.stopPropagation()
    if (loading) return
    try {
      await deleteAiConversation(id)
      setConversations((items) => items.filter((item) => item.id !== id))
      if (conversationId === id) startNewChat()
    } catch (requestError) {
      setError(translateError(requestError, t, 'ai.historyError'))
    }
  }

  const sendMessage = async (event) => {
    event?.preventDefault()
    const message = draft.trim()
    if (!message || loading) return

    const userMessage = { id: makeId('user'), role: 'user', content: message }
    const assistantId = makeId('assistant')
    setMessages((items) => [...items, userMessage, { id: assistantId, role: 'assistant', content: '' }])
    setDraft('')
    setError('')
    setLoading(true)
    const controller = new AbortController()
    abortRef.current = controller

    try {
      await streamAiChat({ conversationId, message, language }, {
        signal: controller.signal,
        onEvent: (type, payload) => {
          if (type === 'meta') setConversationId(payload.conversationId)
          if (type === 'delta') {
            setMessages((items) => items.map((item) => item.id === assistantId
              ? { ...item, content: item.content + payload.text }
              : item))
          }
          if (type === 'error') {
            setError(payload.code === 'ai_unavailable' ? t('ai.unavailable') : (payload.message || t('ai.unavailable')))
            setMessages((items) => items.filter((item) => item.id !== assistantId))
          }
        },
      })
      const refreshed = await getAiConversations()
      setConversations(refreshed || [])
    } catch (requestError) {
      if (requestError?.name !== 'AbortError') {
        setError(requestError?.status === 503 ? t('ai.unavailable') : translateError(requestError, t, 'ai.unavailable'))
        setMessages((items) => items.filter((item) => item.id !== assistantId))
      }
    } finally {
      abortRef.current = null
      setLoading(false)
    }
  }

  const stopGeneration = () => {
    abortRef.current?.abort()
    setLoading(false)
  }

  return (
    <>
      {!open && <button className="ai-launcher" type="button" onClick={() => setOpen(true)} aria-label={t('ai.open')}><span aria-hidden="true">✦</span><span>{t('ai.shortTitle')}</span></button>}
      {open && <div className="ai-assistant" role="dialog" aria-modal="false" aria-labelledby="ai-title">
        <header className="ai-assistant__header">
          <div className="ai-assistant__identity"><span className="ai-assistant__mark" aria-hidden="true">✦</span><div><strong id="ai-title">QamqorMed AI</strong><span>{t('ai.subtitle')}</span></div></div>
          <div className="ai-assistant__actions"><button type="button" className="ai-icon-button" onClick={startNewChat} title={t('ai.newChat')} aria-label={t('ai.newChat')}>＋</button><button type="button" className="ai-icon-button" onClick={() => setOpen(false)} title={t('common.close')} aria-label={t('common.close')}>×</button></div>
        </header>
        <div className="ai-assistant__toolbar"><button type="button" className={`ai-history-button ${historyOpen ? 'is-active' : ''}`} onClick={() => setHistoryOpen((value) => !value)}>◷ {t('ai.history')} {conversations.length > 0 && <span>{conversations.length}</span>}</button><span className="ai-role-badge">{t(`roles.${user.role}`)}</span></div>
        {historyOpen && <div className="ai-history" aria-label={t('ai.history')}>
          {historyLoading && <p className="ai-history__empty">{t('common.loading')}</p>}
          {!historyLoading && conversations.length === 0 && <p className="ai-history__empty">{t('ai.noHistory')}</p>}
          {!historyLoading && conversations.map((conversation) => <button className={`ai-history__item ${conversation.id === conversationId ? 'is-active' : ''}`} key={conversation.id} type="button" onClick={() => selectConversation(conversation.id)}><span>{conversation.title || t('ai.newChat')}</span><b onClick={(event) => removeConversation(event, conversation.id)} aria-label={t('ai.deleteChat')}>×</b></button>)}
        </div>}
        <div className="ai-assistant__messages" ref={scrollRef}>
          {messages.length === 0 && <div className="ai-welcome"><div className="ai-welcome__halo">✦</div><h2>{t('ai.greeting', { name: user.firstName })}</h2><p>{t('ai.description')}</p><div className="ai-suggestions">{suggestions.map((key) => <button type="button" key={key} onClick={() => setDraft(t(`ai.suggestions.${key}`))}>{t(`ai.suggestions.${key}`)}</button>)}</div></div>}
          {messages.map((message) => <div className={`ai-message ai-message--${message.role}`} key={message.id}><span className="ai-message__avatar" aria-hidden="true">{message.role === 'assistant' ? '✦' : (user.firstName?.[0] || 'U')}</span><div className="ai-message__bubble">{message.content || (loading && <span className="ai-typing"><i /><i /><i /></span>)}</div></div>)}
          {error && <div className="ai-error" role="alert">{error}</div>}
        </div>
        <form className="ai-composer" onSubmit={sendMessage}><textarea value={draft} onChange={(event) => setDraft(event.target.value)} placeholder={t('ai.placeholder')} maxLength={4000} rows={1} disabled={loading} onKeyDown={(event) => { if (event.key === 'Enter' && !event.shiftKey) { event.preventDefault(); sendMessage(event) } }} /><button type={loading ? 'button' : 'submit'} onClick={loading ? stopGeneration : undefined} className={loading ? 'is-stop' : ''} aria-label={loading ? t('ai.stop') : t('ai.send')}>{loading ? '■' : '➤'}</button><small>{t('ai.disclaimer')}</small></form>
      </div>}
    </>
  )
}

export default AiAssistant
