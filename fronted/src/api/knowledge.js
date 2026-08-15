import http from '../utils/http'

export function getKnowledgeResources(params) {
  return http.get('/knowledge/resources', { params })
}

export function getResourcesByCategory(category, params) {
  return http.get(`/knowledge/resources/category/${category}`, { params })
}

export function getResourceDetail(id) {
  return http.get(`/knowledge/resources/${id}`)
}

export function incrementResourceView(id) {
  return http.post(`/knowledge/resources/${id}/view`)
}

export function incrementResourceDownload(id) {
  return http.post(`/knowledge/resources/${id}/download`)
}

export function getKnowledgeStats() {
  return http.get('/knowledge/stats')
}

export function getPopularResources(limit = 10) {
  return http.get('/knowledge/popular', { params: { limit } })
}

export function getLatestResources(limit = 10) {
  return http.get('/knowledge/latest', { params: { limit } })
}