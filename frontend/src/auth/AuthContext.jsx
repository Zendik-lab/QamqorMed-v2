import { createContext, useCallback, useContext, useEffect, useMemo, useState } from 'react'
import {
  clearStoredToken,
  devLogin as devLoginRequest,
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
      navigate('/login', { replace: true })
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

  const hydrateAuthResponse = useCallback(async (response) => {
    storeToken(response.accessToken)
    // Always hydrate the session from the backend so role and staff details
    // are never taken from client-controlled state.
    let currentUser
    try {
      currentUser = await getCurrentUser()
    } catch (error) {
      clearStoredToken()
      throw error
    }
    setState({
      user: currentUser,
      token: response.accessToken,
      isAuthenticated: true,
      loading: false,
    })
    return currentUser
  }, [])

  const refreshUser = useCallback(async () => {
    const currentUser = await getCurrentUser()
    setState((current) => ({ ...current, user: currentUser }))
    return currentUser
  }, [])

  const login = useCallback((credentials) => loginRequest(credentials).then(hydrateAuthResponse), [hydrateAuthResponse])

  const loginAsDevUser = useCallback((userId) => devLoginRequest(userId).then(hydrateAuthResponse), [hydrateAuthResponse])

  const register = useCallback((credentials) => registerRequest(credentials), [])

  const value = useMemo(() => ({ ...state, login, loginAsDevUser, register, logout, refreshUser }), [state, login, loginAsDevUser, register, logout, refreshUser])

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}

export function useAuth() {
  const context = useContext(AuthContext)
  if (!context) {
    throw new Error('useAuth must be used inside AuthProvider')
  }
  return context
}
