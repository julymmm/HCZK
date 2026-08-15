import http from '../utils/http'

export function searchContent(params) {
  return http.get('/search', { params })
}

export function suggestContent(params) {
  return http.get('/search/suggest', { params })
}