import { createContext, useContext, useSyncExternalStore } from 'react'

const ThemeContext = createContext(null)
const store = window.QamqorMedTheme

export function ThemeProvider({ children }) {
  const snapshot = useSyncExternalStore(store.subscribe, store.getSnapshot)
  return <ThemeContext.Provider value={{ ...snapshot, setPreference: store.setPreference }}>{children}</ThemeContext.Provider>
}

export function useTheme() {
  const context = useContext(ThemeContext)
  if (!context) throw new Error('useTheme must be used inside ThemeProvider')
  return context
}
