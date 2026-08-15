export const config = {
  API_BASE_URL: '/api',
  UPLOAD_BASE_URL: import.meta.env.VITE_OSS_PUBLIC_BASE_URL || '',

  getApiUrl(path) {
    return `${this.API_BASE_URL}${path.startsWith('/') ? '' : '/'}${path}`
  },

  getUploadUrl(path) {
    if (!path) return ''
    if (path.startsWith('http') || path.startsWith('data:') || path.startsWith('blob:')) return path

    const normalized = path.replace(/^\/+/, '')
    if (!this.UPLOAD_BASE_URL) return `/${normalized}`

    const base = this.UPLOAD_BASE_URL.replace(/\/+$/, '')
    return `${base}/${normalized}`
  },

  getAvatarUrl(avatar) {
    if (!avatar) return new URL('../assets/about-image.jpg', import.meta.url).href
    return this.getUploadUrl(avatar)
  }
}

export default config