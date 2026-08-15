import { createRouter, createWebHistory } from 'vue-router'

const Landing = () => import('../pages/Landing.vue')
const Portal = () => import('../pages/Portal.vue')
const About = () => import('../pages/About.vue')
const Knowledge = () => import('../pages/Knowledge.vue')
const Projects = () => import('../pages/Projects.vue')
const ProjectUpload = () => import('../pages/ProjectUpload.vue')
const Competitions = () => import('../pages/Competitions.vue')
const Tools = () => import('../pages/Tools.vue')
const Articles = () => import('../pages/Articles.vue')
const Share = () => import('../pages/Share.vue')
const ShareDetail = () => import('../pages/ShareDetail.vue')
const ShareUpload = () => import('../pages/ShareUpload.vue')
const Search = () => import('../pages/Search.vue')
const Announcements = () => import('../pages/Announcements.vue')
const Events = () => import('../pages/Events.vue')
const AnnouncementDetail = () => import('../pages/AnnouncementDetail.vue')
const EventDetail = () => import('../pages/EventDetail.vue')
const Login = () => import('../pages/Login.vue')
const Register = () => import('../pages/Register.vue')
const ForgotPassword = () => import('../pages/ForgotPassword.vue')
const Profile = () => import('../pages/Profile.vue')
const ProjectRepository = () => import('../pages/ProjectRepository.vue')
const ProjectEdit = () => import('../pages/ProjectEdit.vue')
const ProjectDetail = () => import('../pages/ProjectDetail.vue')
const Admin = () => import('../pages/Admin.vue')

const NotFound = () => import('../pages/Portal.vue')
const ContentDetail = { template: '<div>内容详情 - 开发中</div>' }

const routes = [
  { path: '/', name: 'Landing', component: Landing, meta: { title: '华创智库 - 西电华为俱乐部' } },
  { path: '/auth/login', name: 'Login', component: Login, meta: { title: '登录 - 华创智库', guestOnly: true } },
  { path: '/auth/register', name: 'Register', component: Register, meta: { title: '注册 - 华创智库', guestOnly: true } },
  { path: '/auth/forgot-password', name: 'ForgotPassword', component: ForgotPassword, meta: { title: '找回密码 - 华创智库', guestOnly: true } },
  { path: '/auth/profile', name: 'Profile', component: Profile, meta: { title: '个人主页 - 华创智库', requiresAuth: true } },
  { path: '/repository', name: 'ProjectRepository', component: ProjectRepository, meta: { title: '项目仓库 - 华创智库', requiresAuth: true } },
  { path: '/admin', name: 'Admin', component: Admin, meta: { title: '管理员控制台 - 华创智库', requiresAuth: true, requiresAdmin: true } },
  { path: '/project/edit/:id', name: 'ProjectEdit', component: ProjectEdit, meta: { title: '编辑项目 - 华创智库', requiresAuth: true } },
  { path: '/portal', name: 'Portal', component: Portal, meta: { title: '门户 - 华创智库' } },
  { path: '/search', name: 'Search', component: Search, meta: { title: '全站搜索 - 华创智库' } },
  { path: '/about', name: 'About', component: About, meta: { title: '关于我们 - 华创智库' } },
  { path: '/knowledge', name: 'Knowledge', component: Knowledge, meta: { title: '知识库 - 华创智库' } },
  { path: '/knowledge/:category', name: 'KnowledgeCategory', component: Knowledge, meta: { title: '知识库 - 华创智库' } },
  { path: '/projects', name: 'Projects', component: Projects, meta: { title: '项目展示 - 华创智库' } },
  { path: '/projects/upload', name: 'ProjectUpload', component: ProjectUpload, meta: { title: '分享我的项目 - 华创智库', requiresAuth: true } },
  { path: '/projects/:id', name: 'ProjectDetail', component: ProjectDetail, meta: { title: '项目详情 - 华创智库' } },
  { path: '/competitions', name: 'Competitions', component: Competitions, meta: { title: '竞赛专区 - 华创智库' } },
  { path: '/tools', name: 'Tools', component: Tools, meta: { title: '实用工具 - 华创智库' } },
  { path: '/articles', name: 'Articles', component: Articles, meta: { title: '华创推文 - 华创智库' } },
  { path: '/content/:id', name: 'ContentDetail', component: ContentDetail, meta: { title: '内容详情 - 华创智库' } },
  { path: '/share', name: 'Share', component: Share, meta: { title: '师兄师姐说 - 华创智库' } },
  { path: '/share/upload', name: 'ShareUpload', component: ShareUpload, meta: { title: '分享你的经验 - 华创智库', requiresAuth: true } },
  { path: '/share/:id', name: 'ShareDetail', component: ShareDetail, meta: { title: '经验详情 - 华创智库' } },
  { path: '/announcements', name: 'Announcements', component: Announcements, meta: { title: '最新公告 - 华创智库' } },
  { path: '/announcements/:id', name: 'AnnouncementDetail', component: AnnouncementDetail, meta: { title: '公告详情 - 华创智库' } },
  { path: '/events', name: 'Events', component: Events, meta: { title: '近期活动 - 华创智库' } },
  { path: '/events/:id', name: 'EventDetail', component: EventDetail, meta: { title: '活动详情 - 华创智库' } },
  { path: '/:pathMatch(.*)*', name: 'NotFound', component: NotFound, meta: { title: '页面未找到 - 华创智库' } }
]

const router = createRouter({
  history: createWebHistory(),
  routes,
  scrollBehavior(to, from, savedPosition) {
    return savedPosition || { top: 0 }
  }
})

function parseJwtExpiresAt(token) {
  try {
    const payload = JSON.parse(atob(token.split('.')[1]))
    return payload.exp ? new Date(payload.exp * 1000) : null
  } catch (_) {
    return null
  }
}

function readUserState() {
  try {
    const raw = localStorage.getItem('hczk_current_user')
    return raw ? JSON.parse(raw) : {}
  } catch (_) {
    return {}
  }
}

function readTokenState() {
  try {
    const raw = localStorage.getItem('hczk_auth_tokens') || localStorage.getItem('authState')
    return raw ? JSON.parse(raw) : {}
  } catch (_) {
    return {}
  }
}

router.beforeEach(async (to, from, next) => {
  if (to.meta.title) document.title = to.meta.title

  const tokenState = readTokenState()
  const accessToken = tokenState.accessToken || tokenState.token || ''
  const refreshToken = tokenState.refreshToken || ''
  let isLoggedIn = !!accessToken

  if (isLoggedIn) {
    const now = new Date()
    const accessExpiresAt = parseJwtExpiresAt(accessToken)
    if (!accessExpiresAt || accessExpiresAt <= now) {
      const refreshExpiresAt = parseJwtExpiresAt(refreshToken)
      if (!refreshExpiresAt || refreshExpiresAt <= now) {
        localStorage.removeItem('hczk_auth_tokens')
        localStorage.removeItem('hczk_current_user')
        localStorage.removeItem('authState')
        isLoggedIn = false
      }
    }
  }

  if (to.path === '/' && isLoggedIn) return next({ path: '/portal' })
  if (to.meta.requiresAuth && !isLoggedIn) return next({ path: '/auth/login', query: { redirect: to.fullPath } })
  if (to.meta.requiresAdmin) {
    const userState = readUserState()
    if ((userState.role || 'user') !== 'admin') return next({ path: '/portal' })
  }
  if (to.meta.guestOnly && isLoggedIn) return next({ path: '/auth/profile' })
  next()
})

export default router