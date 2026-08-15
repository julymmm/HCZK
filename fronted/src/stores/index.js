import { defineStore } from 'pinia'

const TOKEN_STORAGE_KEY = 'hczk_auth_tokens'
const USER_STORAGE_KEY = 'hczk_current_user'
const LEGACY_STORAGE_KEY = 'authState'

function inferIdentifierType(identifier) {
  const value = String(identifier || '').trim()
  if (/^1[3-9]\d{9}$/.test(value)) return 'PHONE'
  return value.includes('@') ? 'EMAIL' : 'PHONE'
}
const emptyUser = () => ({
  id: null,
  username: '',
  nickname: '',
  email: '',
  phone: '',
  studentId: '',
  college: '',
  bio: '',
  avatarUrl: '',
  hic: 0,
  role: 'user'
})

export const useAppStore = defineStore('app', {
  state: () => ({
    isMobileMenuOpen: false,
    searchKeyword: '',
    notifications: []
  }),
  actions: {
    toggleMobileMenu() {
      this.isMobileMenuOpen = !this.isMobileMenuOpen
      document.body.style.overflow = this.isMobileMenuOpen ? 'hidden' : ''
    },
    setSearchKeyword(keyword) {
      this.searchKeyword = keyword
    },
    addNotification(notification) {
      const id = Date.now()
      this.notifications.push({ id, ...notification, timestamp: new Date() })
      if (notification.autoClose !== false) {
        const timeout = notification.timeout || 3000
        setTimeout(() => this.removeNotification(id), timeout)
      }
      return id
    },
    removeNotification(id) {
      const index = this.notifications.findIndex(n => n.id === id)
      if (index !== -1) this.notifications.splice(index, 1)
    },
    clearAllNotifications() {
      this.notifications = []
    }
  },
  getters: {
    hasNotifications: (state) => state.notifications.length > 0
  }
})

export const useAuthStore = defineStore('auth', {
  state: () => ({
    isLoggedIn: false,
    accessToken: '',
    refreshToken: '',
    accessTokenExpiresAt: '',
    refreshTokenExpiresAt: '',
    lastTokenCheck: 0,
    user: emptyUser()
  }),

  actions: {
    loadFromStorage() {
      try {
        const tokenRaw = localStorage.getItem(TOKEN_STORAGE_KEY)
        const userRaw = localStorage.getItem(USER_STORAGE_KEY)
        if (tokenRaw) {
          const tokenState = JSON.parse(tokenRaw)
          this.accessToken = tokenState.accessToken || ''
          this.refreshToken = tokenState.refreshToken || ''
          this.accessTokenExpiresAt = tokenState.accessTokenExpiresAt || ''
          this.refreshTokenExpiresAt = tokenState.refreshTokenExpiresAt || ''
          this.lastTokenCheck = tokenState.lastTokenCheck || 0
          this.isLoggedIn = !!this.accessToken
        }
        if (userRaw) {
          this.user = { ...emptyUser(), ...JSON.parse(userRaw) }
        }
        if (!tokenRaw) this.loadLegacyAuthState()
      } catch (_) {
        this.clearSession()
      }
    },

    loadLegacyAuthState() {
      const raw = localStorage.getItem(LEGACY_STORAGE_KEY)
      if (!raw) return
      try {
        const parsed = JSON.parse(raw)
        this.accessToken = parsed.accessToken || parsed.token || ''
        this.refreshToken = parsed.refreshToken || ''
        this.accessTokenExpiresAt = parsed.accessTokenExpiresAt || ''
        this.refreshTokenExpiresAt = parsed.refreshTokenExpiresAt || ''
        this.lastTokenCheck = parsed.lastTokenCheck || 0
        this.isLoggedIn = !!parsed.isLoggedIn && !!this.accessToken
        this.user = { ...emptyUser(), ...(parsed.user || {}) }
        this.persist()
        localStorage.removeItem(LEGACY_STORAGE_KEY)
      } catch (_) {
        localStorage.removeItem(LEGACY_STORAGE_KEY)
      }
    },

    persist() {
      localStorage.setItem(TOKEN_STORAGE_KEY, JSON.stringify({
        accessToken: this.accessToken,
        refreshToken: this.refreshToken,
        accessTokenExpiresAt: this.accessTokenExpiresAt,
        refreshTokenExpiresAt: this.refreshTokenExpiresAt,
        lastTokenCheck: this.lastTokenCheck
      }))
      localStorage.setItem(USER_STORAGE_KEY, JSON.stringify(this.user))
    },

    applyTokenData(data) {
      this.accessToken = data.accessToken || ''
      this.refreshToken = data.refreshToken || this.refreshToken || ''
      this.accessTokenExpiresAt = data.accessTokenExpiresAt || ''
      this.refreshTokenExpiresAt = data.refreshTokenExpiresAt || this.refreshTokenExpiresAt || ''
      this.isLoggedIn = !!this.accessToken
    },

    applyUserData(user) {
      if (!user) return
      this.user = {
        ...this.user,
        id: user.id ?? this.user.id,
        username: user.username || this.user.username,
        nickname: user.nickname || user.username || this.user.nickname,
        studentId: user.studentId || this.user.studentId || '',
        college: user.college || this.user.college || '',
        email: user.email || this.user.email || '',
        phone: user.phone || this.user.phone || '',
        bio: user.bio || this.user.bio || '',
        avatarUrl: user.avatarUrl || this.user.avatarUrl,
        hic: user.hic !== undefined ? user.hic : (this.user.hic || 0),
        role: user.role || this.user.role || 'user'
      }
    },

    async login(payload) {
      const { default: http } = await import('../utils/http')
      const resp = await http.post('/auth/login', {
        identifierType: payload.identifierType || inferIdentifierType(payload.identifier || payload.email || payload.phone),
        identifier: payload.identifier || payload.email || payload.phone,
        username: payload.username,
        phone: payload.phone,
        email: payload.email,
        password: payload.password,
        code: payload.code
      }, { skipAuthRefresh: true })
      const data = resp.data?.data || {}
      this.applyTokenData(data)
      this.applyUserData(data.user)
      this.persist()
      return { success: this.isLoggedIn }
    },

    async register(payload) {
      const { default: http } = await import('../utils/http')
      await http.post('/auth/register', {
        identifierType: payload.identifierType || inferIdentifierType(payload.identifier || payload.email || payload.phone),
        identifier: payload.identifier || payload.email || payload.phone,
        username: payload.username,
        phone: payload.phone,
        email: payload.email,
        password: payload.password,
        code: payload.code,
        nickname: payload.nickname || payload.username,
        studentId: payload.studentId,
        college: payload.college
      }, { skipAuthRefresh: true })
      return { success: true }
    },

    async sendAuthCode(payload) {
      const { default: http } = await import('../utils/http')
      const resp = await http.post('/auth/code', {
        scene: payload.scene,
        identifierType: payload.identifierType || inferIdentifierType(payload.identifier),
        identifier: payload.identifier
      }, { skipAuthRefresh: true })
      return resp.data?.data || {}
    },

    async sendResetPasswordCode(identifier) {
      return this.sendAuthCode({ scene: 'RESET_PASSWORD', identifierType: 'EMAIL', identifier })
    },

    async resetPassword(payload) {
      const { default: http } = await import('../utils/http')
      await http.post('/auth/password/reset', {
        identifierType: payload.identifierType || inferIdentifierType(payload.identifier),
        identifier: payload.identifier,
        code: payload.code,
        newPassword: payload.newPassword
      }, { skipAuthRefresh: true })
      return { success: true }
    },

    async refreshAccessToken() {
      if (!this.refreshToken || this.isRefreshTokenExpired()) {
        this.clearSession()
        return false
      }
      try {
        const { default: http } = await import('../utils/http')
        const resp = await http.post('/auth/refresh', {
          refreshToken: this.refreshToken
        }, { skipAuthRefresh: true })
        const data = resp.data?.data || {}
        this.applyTokenData(data)
        this.persist()
        return !!this.accessToken
      } catch (_) {
        this.clearSession()
        return false
      }
    },

    async logoutRemote(refreshToken = this.refreshToken) {
      if (!refreshToken) return
      try {
        const { default: http } = await import('../utils/http')
        await http.post('/auth/logout', { refreshToken }, { skipAuthRefresh: true })
      } catch (_) {}
    },

    clearSession() {
      this.isLoggedIn = false
      this.accessToken = ''
      this.refreshToken = ''
      this.accessTokenExpiresAt = ''
      this.refreshTokenExpiresAt = ''
      this.lastTokenCheck = 0
      this.user = emptyUser()
      localStorage.removeItem(TOKEN_STORAGE_KEY)
      localStorage.removeItem(USER_STORAGE_KEY)
      localStorage.removeItem(LEGACY_STORAGE_KEY)
    },

    async logout() {
      const refreshToken = this.refreshToken
      await this.logoutRemote(refreshToken)
      this.clearSession()
    },

    updateProfile(partial) {
      this.user = { ...this.user, ...partial }
      localStorage.setItem(USER_STORAGE_KEY, JSON.stringify(this.user))
    },

    async checkTokenValidity() {
      if (!this.accessToken) return false
      if (this.isTokenExpired()) return await this.refreshAccessToken()
      try {
        const { default: http } = await import('../utils/http')
        await http.get('/users/me')
        return true
      } catch (error) {
        if (error.response?.status === 401) return await this.refreshAccessToken()
        return true
      }
    },

    getTokenExpiration() {
      if (this.accessTokenExpiresAt) {
        const t = new Date(this.accessTokenExpiresAt)
        if (!Number.isNaN(t.getTime())) return t
      }
      if (!this.accessToken) return null
      try {
        const payload = JSON.parse(atob(this.accessToken.split('.')[1]))
        return new Date(payload.exp * 1000)
      } catch (_) {
        return null
      }
    },

    getRefreshTokenExpiration() {
      if (this.refreshTokenExpiresAt) {
        const t = new Date(this.refreshTokenExpiresAt)
        if (!Number.isNaN(t.getTime())) return t
      }
      if (!this.refreshToken) return null
      try {
        const payload = JSON.parse(atob(this.refreshToken.split('.')[1]))
        return new Date(payload.exp * 1000)
      } catch (_) {
        return null
      }
    },

    isTokenExpired() {
      const expiration = this.getTokenExpiration()
      if (!expiration) return true
      return expiration.getTime() <= Date.now() + 5000
    },

    isRefreshTokenExpired() {
      const expiration = this.getRefreshTokenExpiration()
      if (!expiration) return true
      return expiration.getTime() <= Date.now()
    }
  },

  getters: {
    isAuthenticated: (state) => state.isLoggedIn,
    tokens: (state) => ({
      accessToken: state.accessToken,
      refreshToken: state.refreshToken,
      accessTokenExpiresAt: state.accessTokenExpiresAt,
      refreshTokenExpiresAt: state.refreshTokenExpiresAt
    }),
    currentUser: (state) => state.user
  }
})

export const useKnowledgeStore = defineStore('knowledge', {
  state: () => ({ currentCategory: '纭欢', difficulty: '', resourceType: '', articles: [] }),
  actions: {
    setCategory(category) { this.currentCategory = category },
    setDifficulty(difficulty) { this.difficulty = difficulty },
    setResourceType(type) { this.resourceType = type },
    resetFilters() { this.difficulty = ''; this.resourceType = '' }
  },
  getters: {
    filteredArticles: (state) => {
      let result = state.articles
      if (state.currentCategory) result = result.filter(article => article.category === state.currentCategory)
      if (state.difficulty) result = result.filter(article => article.difficulty === state.difficulty)
      if (state.resourceType) result = result.filter(article => article.resourceType === state.resourceType)
      return result
    }
  }
})

export const useCompetitionStore = defineStore('competition', {
  state: () => ({ currentTab: 'info', teamFilter: '', competitionFilter: '' }),
  actions: {
    setTab(tab) { this.currentTab = tab },
    setTeamFilter(filter) { this.teamFilter = filter },
    setCompetitionFilter(filter) { this.competitionFilter = filter },
    resetFilters() { this.teamFilter = ''; this.competitionFilter = '' }
  }
})

export const stores = { useAppStore, useAuthStore, useKnowledgeStore, useCompetitionStore }
