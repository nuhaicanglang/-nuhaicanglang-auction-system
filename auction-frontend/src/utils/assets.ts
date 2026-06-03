const apiBaseUrl = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080'

function normalizeBaseUrl(url: string) {
  return url.endsWith('/') ? url.slice(0, -1) : url
}

export function resolveAssetUrl(url?: string | null) {
  if (!url) {
    return ''
  }
  if (/^(https?:)?\/\//.test(url) || url.startsWith('data:') || url.startsWith('blob:')) {
    return url
  }
  if (url.startsWith('/uploads/')) {
    const base = normalizeBaseUrl(apiBaseUrl)
    return `${base}${url}`
  }
  if (url.startsWith('uploads/')) {
    const base = normalizeBaseUrl(apiBaseUrl)
    return `${base}/${url}`
  }
  return url
}
