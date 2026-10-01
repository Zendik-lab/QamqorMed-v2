import { createContext, useCallback, useContext, useEffect, useMemo, useState } from 'react'
import {
  clearStoredToken,
  getCurrentUser,
  getStoredToken,
  login as loginRequest,
  register as registerRequest,
  setUnauthorizedHandler,
  storeToken,
} from '../api/apiClient.js'
import { navigate } from '../utils/navigation.js'

const AuthContext = createContext(null)

export function AuthProvider({ children }) {
  const [state, setState] = useState({
    user: null,
    token: getStoredToken(),
    isAuthenticated: false,
    loading: true,
  })

  const logout = useCallback((shouldNavigate = true) => {
    clearStoredToken()
    setState({ user: null, token: null, isAuthenticated: false, loading: false })
    if (shouldNavigate && window.location.pathname !== '/login') {
      navigate('/login')
    }
  }, [])

  useEffect(() => {
    setUnauthorizedHandler(() => logout(true))
    const token = getStoredToken()

    if (!token) {
      setState({ user: null, token: null, isAuthenticated: false, loading: false })
      return () => setUnauthorizedHandler(null)
    }

    getCurrentUser()
      .then((user) => {
        setState({ user, token, isAuthenticated: true, loading: false })
      })
      .catch(() => {
        clearStoredToken()
        setState({ user: null, token: null, isAuthenticated: false, loading: false })
      })

    return () => setUnauthorizedHandler(null)
  }, [logout])

  const login = useCallback(async (credentials) => {
    const response = await loginRequest(credentials)
    storeToken(response.accessToken)
    setState({
      user: response.user,
      token: response.accessToken,
      isAuthenticated: true,
      loading: false,
    })
    return response.user
  }, [])

  const register = useCallback((credentials) => registerRequest(credentials), [])

  const value = useMemo(() => ({ ...state, login, register, logout }), [state, login, register, logout])

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}

export function useAuth() {
  const context = useContext(AuthContext)
  if (!context) {
    throw new Error('useAuth must be used inside AuthProvider')
  }
  return context
}
