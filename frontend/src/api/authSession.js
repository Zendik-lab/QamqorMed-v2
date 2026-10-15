export function withBearerToken(headers, token) {
  if (!token) return { ...headers }
  return { ...headers, Authorization: `Bearer ${token}` }
}

/**
 * A 401 from a feature endpoint is not enough to invalidate the whole UI
 * session. Confirm the session through the canonical auth endpoint first.
 * Network errors and every response other than 401 deliberately preserve the
 * token and leave the original feature error visible to the caller.
 */
export async function confirmSessionAndNotify({
  fetchImpl = globalThis.fetch,
  sessionUrl,
  token,
  getCurrentToken = () => token,
  onInvalid,
}) {
  if (!token || typeof fetchImpl !== 'function') return false

  let response
  try {
    response = await fetchImpl(sessionUrl, {
      method: 'GET',
      headers: withBearerToken({ Accept: 'application/json' }, token),
    })
  } catch {
    return false
  }

  if (response.status !== 401 || getCurrentToken() !== token) return false
  onInvalid?.()
  return true
}
