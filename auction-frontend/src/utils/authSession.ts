import type { LoginResult, User } from '@/types/domain'

export interface AuthSession {
  token: string
  refreshToken: string
  user: User | null
  roles: string[]
}

const STORAGE_KEY = 'auction.auth.session'

const emptySession: AuthSession = {
  token: '',
  refreshToken: '',
  user: null,
  roles: [],
}

export function readAuthSession(): AuthSession {
  const raw = window.localStorage.getItem(STORAGE_KEY)
  if (!raw) {
    return { ...emptySession }
  }

  try {
    return { ...emptySession, ...(JSON.parse(raw) as Partial<AuthSession>) }
  } catch {
    window.localStorage.removeItem(STORAGE_KEY)
    return { ...emptySession }
  }
}

export function writeAuthSession(result: LoginResult): AuthSession {
  const session: AuthSession = {
    token: result.token,
    refreshToken: result.refreshToken,
    user: result.user,
    roles: result.roles ?? result.user?.roles ?? [],
  }
  window.localStorage.setItem(STORAGE_KEY, JSON.stringify(session))
  window.dispatchEvent(new CustomEvent('auction-auth-session-changed'))
  return session
}

export function patchAuthSession(patch: Partial<AuthSession>): AuthSession {
  const session = { ...readAuthSession(), ...patch }
  window.localStorage.setItem(STORAGE_KEY, JSON.stringify(session))
  window.dispatchEvent(new CustomEvent('auction-auth-session-changed'))
  return session
}

export function clearAuthSession() {
  window.localStorage.removeItem(STORAGE_KEY)
  window.dispatchEvent(new CustomEvent('auction-auth-session-changed'))
}
