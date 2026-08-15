import axios from 'axios'
import { useAuthStore } from '../stores'
import { config as appConfig } from './config'

const http = axios.create({
  baseURL: appConfig.API_BASE_URL,
  timeout: 15000,
  headers: {
    'Cache-Control': 'no-cache, no-store, must-revalidate',
    'Pragma': 'no-cache',
    'Expires': '0'
  }
})

let refreshPromise = null

async function ensureFreshToken(auth) {
  if (!auth?.accessToken || !auth.isTokenExpired()) {
    return true
  }
  if (!refreshPromise) {
    refreshPromise = auth.refreshAccessToken().finally(() => {
      refreshPromise = null
    })
  }
  return await refreshPromise
}

http.interceptors.request.use(async (requestConfig) => {
  try {
    const auth = useAuthStore()
    if (!requestConfig.skipAuthRefresh) {
      await ensureFreshToken(auth)
    }
    if (auth?.accessToken) {
      requestConfig.headers.Authorization = `Bearer ${auth.accessToken}`
    }
  } catch (_) {}
  return requestConfig
})

http.interceptors.response.use(
  (resp) => resp,
  async (error) => {
    const original = error.config || {}
    if (error.response?.status === 401 && !original._retry && !original.skipAuthRefresh) {
      original._retry = true
      try {
        const auth = useAuthStore()
        if (!refreshPromise) {
          refreshPromise = auth.refreshAccessToken().finally(() => {
            refreshPromise = null
          })
        }
        const refreshed = await refreshPromise
        if (refreshed && auth.accessToken) {
          original.headers = original.headers || {}
          original.headers.Authorization = `Bearer ${auth.accessToken}`
          return http(original)
        }
      } catch (_) {}
    }

    if (error.response?.status === 401) {
      try {
        const auth = useAuthStore()
        auth.logout()
      } catch (_) {}
    }
    return Promise.reject(error)
  }
)

export default http
