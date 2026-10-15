import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import App from './App.jsx'
import { ThemeProvider } from './theme/ThemeProvider.jsx'
import './styles/tokens.css'
import './styles/global.css'
import './styles/shell.css'
import './styles/workspace.css'
import './styles/ui.css'
import './styles/account.css'
import './styles/ai-assistant.css'
import './styles/motion.css'

createRoot(document.getElementById('root')).render(
  <StrictMode>
    <ThemeProvider><App /></ThemeProvider>
  </StrictMode>,
)
