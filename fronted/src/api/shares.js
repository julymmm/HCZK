import http from '../utils/http'

// Share list and detail
export function getShares(params) {
  return http.get('/senior-shares', { params })
}

export function getShareDetail(id) {
  return http.get(`/senior-shares/${id}`)
}

// Progressive publish flow
export function createShareDraft() {
  return http.post('/senior-shares/drafts')
}

export function presignShareContent({ postId, contentType = 'text/markdown', ext = '.md' }) {
  return http.post('/storage/presign', {
    scene: 'share_content',
    postId,
    contentType,
    ext,
    filename: `content${ext}`
  })
}

export function confirmShareContent(id, data) {
  return http.post(`/senior-shares/${id}/content/confirm`, data)
}

export function patchShareMetadata(id, data) {
  return http.patch(`/senior-shares/${id}`, data)
}

export function publishShare(id) {
  return http.post(`/senior-shares/${id}/publish`)
}

// Legacy compatibility
export function createShare(data) {
  return http.post('/senior-shares', data)
}

export function updateShare(id, data) {
  return http.patch(`/senior-shares/${id}`, data)
}

export function deleteShare(id) {
  return http.delete(`/senior-shares/${id}`)
}
