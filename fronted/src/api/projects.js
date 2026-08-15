import http from '../utils/http'

export function getProjects(params) {
  return http.get('/projects', { params })
}

export function getProjectDetail(id, increaseView = false) {
  return http.get(`/projects/${id}`, {
    params: { increaseView }
  })
}

export function getProjectById(id, increaseView = false) {
  return getProjectDetail(id, increaseView)
}

export function createProject(data) {
  return http.post('/projects', data)
}

export function updateProject(id, data) {
  const hasFile = data.documentFile instanceof File

  if (hasFile) {
    const formData = new FormData()
    formData.append('title', data.title)
    formData.append('description', data.description || '')
    formData.append('category', data.category)
    if (data.githubUrl) {
      formData.append('githubUrl', data.githubUrl)
    }
    formData.append('documentFile', data.documentFile)

    return http.put(`/projects/${id}`, formData, {
      headers: {
        'Content-Type': 'multipart/form-data'
      }
    })
  }

  return http.put(`/projects/${id}/info`, {
    title: data.title,
    description: data.description || '',
    category: data.category,
    githubUrl: data.githubUrl || '',
    detailedDescription: data.detailedDescription || null
  })
}

export function deleteProject(id) {
  return http.delete(`/projects/${id}`)
}

export function incrementProjectView(id) {
  return http.post(`/projects/${id}/view`)
}

export function incrementProjectLike(id) {
  return http.post(`/projects/${id}/like`)
}

export function decrementProjectLike(id) {
  return http.delete(`/projects/${id}/like`)
}

export function incrementProjectFavorite(id) {
  return http.post(`/projects/${id}/favorite`)
}

export function decrementProjectFavorite(id) {
  return http.delete(`/projects/${id}/favorite`)
}

export function getProjectComments(projectId, params) {
  return http.get(`/projects/${projectId}/comments`, { params })
}

export function addProjectComment(projectId, data) {
  return http.post(`/projects/${projectId}/comments`, data)
}

export function deleteProjectComment(projectId, commentId) {
  return http.delete(`/projects/${projectId}/comments/${commentId}`)
}

export function getProjectCategories() {
  return http.get('/projects/categories')
}

export function getProjectTags() {
  return http.get('/projects/tags')
}

export function getFeaturedProjects(limit = 10) {
  return http.get('/projects/featured', { params: { limit } })
}

export function getPopularProjects(limit = 10) {
  return http.get('/projects/popular', { params: { limit } })
}

export function getMyProjects(params) {
  return http.get('/projects/my', { params })
}