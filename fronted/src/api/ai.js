import http from '../utils/http'
import { config as appConfig } from '../utils/config'
import { useAuthStore } from '../stores'

export function suggestShareSummary(content) {
  return http.post('/ai/shares/summary/suggest', { content })
}

export function getShareSummary(id) {
  return http.post(`/ai/shares/${id}/summary`)
}

export function reindexShareRag(id) {
  return http.post(`/ai/shares/${id}/rag/reindex`)
}

export async function streamShareQa(id, { question, topK = 5, maxTokens = 1024, signal, onChunk }) {
  const auth = useAuthStore()
  auth.loadFromStorage()
  if (auth.accessToken && auth.isTokenExpired()) {
    await auth.refreshAccessToken()
  }

  const headers = { 'Content-Type': 'application/json' }
  if (auth.accessToken) headers.Authorization = `Bearer ${auth.accessToken}`

  const response = await fetch(`${appConfig.API_BASE_URL}/ai/shares/${id}/qa/stream`, {
    method: 'POST',
    headers,
    body: JSON.stringify({ question, topK, maxTokens }),
    signal
  })

  if (!response.ok) {
    throw new Error(response.status === 401 ? '请先登录后再提问' : `问答请求失败：${response.status}`)
  }

  const reader = response.body?.getReader()
  if (!reader) return ''
  const decoder = new TextDecoder('utf-8')
  let fullText = ''

  while (true) {
    const { done, value } = await reader.read()
    if (done) break
    const chunk = decoder.decode(value, { stream: true })
    fullText += chunk
    if (onChunk) onChunk(chunk, fullText)
  }

  const tail = decoder.decode()
  if (tail) {
    fullText += tail
    if (onChunk) onChunk(tail, fullText)
  }
  return fullText
}
