import { computed, ref } from 'vue'
import { defineStore } from 'pinia'
import { currentUserApi, loginApi, logoutApi, registerApi } from '@/api/auth'
import type { LoginPayload, RegisterPayload, User } from '@/types/domain'
import { clearAuthSession, readAuthSession, writeAuthSession } from '@/utils/authSession'

const adminRoles = new Set(['ADMIN', 'SUPER_ADMIN', 'ROLE_ADMIN', 'ROLE_SUPER_ADMIN'])

export const useAuthStore = defineStore('auth', () => {
  const session = ref(readAuthSession())
  const bootstrapped = ref(false)

  const token = computed(() => session.value.token)
  const user = computed(() => session.value.user)
  const roles = computed(() => session.value.roles)
  const isAuthenticated = computed(() => Boolean(session.value.token))
  const isAdmin = computed(() => session.value.roles.some((role) => adminRoles.has(role)))

  function syncFromStorage() {
    session.value = readAuthSession()
  }

  async function login(payload: LoginPayload) {
    const result = await loginApi(payload)
    session.value = writeAuthSession(result)
    bootstrapped.value = true
    return result
  }

  async function register(payload: RegisterPayload) {
    return registerApi(payload)
  }

  async function loadProfile(force = false) {
    if (!force && bootstrapped.value && session.value.user) {
      return session.value.user
    }
    const profile = await currentUserApi()
    const nextUser: User = {
      ...profile,
      roles: profile.roles?.length ? profile.roles : session.value.roles,
    }
    session.value = {
      ...session.value,
      user: nextUser,
      roles: nextUser.roles ?? [],
    }
    window.localStorage.setItem('auction.auth.session', JSON.stringify(session.value))
    bootstrapped.value = true
    return nextUser
  }

  async function logout() {
    try {
      await logoutApi()
    } finally {
      clearAuthSession()
      syncFromStorage()
      bootstrapped.value = false
    }
  }

  window.addEventListener('auction-auth-session-changed', syncFromStorage)

  return {
    token,
    user,
    roles,
    isAuthenticated,
    isAdmin,
    syncFromStorage,
    login,
    register,
    loadProfile,
    logout,
  }
})
