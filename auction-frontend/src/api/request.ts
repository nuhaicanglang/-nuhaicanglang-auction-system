import axios, {
  AxiosError,
  type AxiosRequestConfig,
  type InternalAxiosRequestConfig,
} from 'axios'
import { ElLoading, ElMessage } from 'element-plus'
import type { ApiResponse, LoginResult } from '@/types/domain'
import {
  clearAuthSession,
  patchAuthSession,
  readAuthSession,
  writeAuthSession,
} from '@/utils/authSession'

export interface ApiRequestConfig<D = unknown> extends AxiosRequestConfig<D> {
  silent?: boolean
  skipAuth?: boolean
  idempotent?: boolean
  _retry?: boolean
}

const baseURL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080'

export const apiClient = axios.create({
  baseURL,
  timeout: 15_000,
})

let loadingCount = 0
let loadingInstance: ReturnType<typeof ElLoading.service> | undefined
let refreshingPromise: Promise<string> | null = null

function openLoading(config: ApiRequestConfig) {
  if (config.silent) {
    return
  }
  loadingCount += 1
  if (!loadingInstance) {
    loadingInstance = ElLoading.service({
      lock: false,
      text: '处理中...',
      background: 'rgba(255, 253, 248, 0.66)',
    })
  }
}

function closeLoading(config?: ApiRequestConfig) {
  if (config?.silent) {
    return
  }
  loadingCount = Math.max(0, loadingCount - 1)
  if (loadingCount === 0) {
    loadingInstance?.close()
    loadingInstance = undefined
  }
}

function makeIdempotentKey() {
  if (crypto.randomUUID) {
    return crypto.randomUUID()
  }
  return `idem-${Date.now()}-${Math.random().toString(16).slice(2)}`
}

function shouldAttachIdempotentKey(config: ApiRequestConfig) {
  const method = config.method?.toLowerCase()
  if (!['post', 'put', 'patch', 'delete'].includes(method ?? '')) {
    return false
  }
  if (config.idempotent) {
    return true
  }
  const url = config.url ?? ''
  return /\/bids$|\/buy-now$|\/pay$|\/wallet\/adjust$|\/credit\/users\/.+\/adjust$/.test(url)
}

async function refreshAccessToken() {
  if (refreshingPromise) {
    return refreshingPromise
  }

  const session = readAuthSession()
  if (!session.refreshToken) {
    throw new Error('No refresh token')
  }

  refreshingPromise = axios
    .post<ApiResponse<LoginResult>>(`${baseURL}/api/system/users/refresh`, {
      refreshToken: session.refreshToken,
    })
    .then((response) => {
      const payload = response.data
      if (payload.code !== 0) {
        throw new Error(payload.msg || '刷新登录状态失败')
      }
      writeAuthSession(payload.data)
      return payload.data.token
    })
    .finally(() => {
      refreshingPromise = null
    })

  return refreshingPromise
}

apiClient.interceptors.request.use((config: InternalAxiosRequestConfig) => {
  const apiConfig = config as ApiRequestConfig
  openLoading(apiConfig)

  if (!apiConfig.skipAuth) {
    const { token } = readAuthSession()
    if (token) {
      apiConfig.headers = apiConfig.headers ?? {}
      apiConfig.headers.Authorization = `Bearer ${token}`
    }
  }

  if (shouldAttachIdempotentKey(apiConfig)) {
    apiConfig.headers = apiConfig.headers ?? {}
    if (!apiConfig.headers['X-Idempotent-Key']) {
      apiConfig.headers['X-Idempotent-Key'] = makeIdempotentKey()
    }
  }

  return config
})

apiClient.interceptors.response.use(
  (response) => {
    closeLoading(response.config as ApiRequestConfig)
    if (response.config.responseType === 'blob') {
      return response.data
    }

    const payload = response.data as ApiResponse<unknown>
    if (payload.code === 0) {
      return payload.data
    }

    ElMessage.error(payload.msg || '请求处理失败')
    return Promise.reject(new Error(payload.msg || '请求处理失败'))
  },
  async (error: AxiosError<ApiResponse<unknown>>) => {
    const config = (error.config ?? {}) as ApiRequestConfig
    closeLoading(config)

    if (error.response?.status === 401 && !config._retry && readAuthSession().refreshToken) {
      config._retry = true
      try {
        const token = await refreshAccessToken()
        config.headers = config.headers ?? {}
        config.headers.Authorization = `Bearer ${token}`
        return apiClient(config)
      } catch {
        clearAuthSession()
        window.location.assign(`/login?redirect=${encodeURIComponent(window.location.pathname)}`)
        return Promise.reject(error)
      }
    }

    if (error.response?.status === 403) {
      ElMessage.error('无权限访问该资源')
    } else if (error.response?.data?.msg) {
      ElMessage.error(error.response.data.msg)
    } else {
      ElMessage.error('网络异常，请稍后重试')
    }

    return Promise.reject(error)
  },
)

export function request<T>(config: ApiRequestConfig): Promise<T> {
  return apiClient(config) as Promise<T>
}

export function updateStoredUser(result: LoginResult) {
  patchAuthSession({
    token: result.token,
    refreshToken: result.refreshToken,
    user: result.user,
    roles: result.roles,
  })
}

export async function downloadExcel(url: string, filename: string, params?: Record<string, unknown>) {
  const blob = await request<Blob>({
    url,
    method: 'GET',
    params,
    responseType: 'blob',
  })
  const link = document.createElement('a')
  const href = URL.createObjectURL(blob)
  link.href = href
  link.download = filename
  link.click()
  URL.revokeObjectURL(href)
}
