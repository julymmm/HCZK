<template>
  <MainLayout>
    <div class="knowledge-page">
      <!-- 页面标题区 -->
      <section class="page-header">
        <div class="container">
          <h1 class="page-title">知识库</h1>
          <p class="page-description">
            汇聚华为技术精华，涵盖硬件、软件、嵌入式开发、人工智能等前沿领域的学习资源与实践案例
          </p>
        </div>
      </section>

      <!-- 分类导航 -->
      <section class="category-nav">
        <div class="container">
          <el-tabs 
            v-model="activeCategory" 
            class="category-tabs"
            @tab-click="handleCategoryChange"
          >
            <el-tab-pane label="全部" name="全部"></el-tab-pane>
            <el-tab-pane label="硬件" name="hardware"></el-tab-pane>
            <el-tab-pane label="软件" name="software"></el-tab-pane>
            <el-tab-pane label="嵌入式开发" name="embed"></el-tab-pane>
            <el-tab-pane label="人工智能" name="ai"></el-tab-pane>
            <el-tab-pane label="其他" name="other"></el-tab-pane>
          </el-tabs>
        </div>
      </section>

      <!-- 筛选条件 -->
      <section class="filter-section">
        <div class="container">
          <div class="filters">
            <div class="filter-left">
              <div class="filter-group">
                <label class="filter-label">资源类型</label>
                <el-radio-group v-model="resourceType" size="medium" @change="applyFilters">
                  <el-radio-button label="">全部</el-radio-button>
                  <el-radio-button label="document">文档</el-radio-button>
                  <el-radio-button label="video">视频</el-radio-button>
                  <el-radio-button label="project">项目</el-radio-button>

                  <el-radio-button label="other">其他</el-radio-button>
                </el-radio-group>
              </div>
            </div>

            <div class="filter-right">
              <div class="filter-group">
                <label class="filter-label">搜索资源</label>
                <el-input 
                  v-model="searchQuery" 
                  placeholder="搜索资源标题或描述..." 
                  prefix-icon="Search"
                  clearable
                  @input="handleSearch"
                />
              </div>
            </div>
          </div>
          

        </div>
      </section>

      <!-- 内容区 -->
      <section class="content-section">
        <div class="container">
          
          <!-- 正在加载 -->
          <div v-if="loading" class="loading-container">
            <el-skeleton :rows="5" animated />
          </div>

          <!-- 无搜索结果 -->
          <div v-else-if="resources.length === 0" class="empty-container">
            <div class="empty-icon">🔍</div>
            <h2 class="empty-title">未找到匹配的资源</h2>
            <p class="empty-description">
              尝试更改搜索关键词或筛选条件
            </p>
          </div>

          <!-- 资源列表 -->
          <div v-else class="resources-container">
            <h2 class="section-title">
              {{ getCategoryTitle(activeCategory) }}
              <span class="resource-count">({{ totalResources }})</span>
            </h2>
            
            <div class="resources-list grid grid-cols-3 grid-gap-lg">
              <div 
                v-for="(resource, index) in resources" 
                :key="resource.id || index"
                class="resource-card"
              >
                <div class="resource-image" :style="{backgroundColor: getResourceColor(resource.id)}">
                  <font-awesome-icon :icon="getResourceTypeIcon(resource.type)" />
                </div>
                <div class="resource-content">
                  <div class="resource-tags">
                    <div class="resource-category-tag" :class="`category-${resource.category}`">
                      {{ getCategoryName(resource.category) }}
                    </div>
                    <div class="resource-type-tag" :class="`type-${resource.type}`">
                      {{ getResourceTypeName(resource.type) }}
                    </div>
                  </div>
                  <h3 class="resource-title">{{ resource.title }}</h3>
                <p class="resource-description">{{ resource.description }}</p>

                  <div class="resource-meta">
                    <div class="meta-item">
                      <font-awesome-icon icon="eye" />
                      <span>{{ resource.eyeCount || 0 }} 次浏览</span>
                    </div>
                    <div class="meta-item">
                      <font-awesome-icon icon="clock" />
                      <span>{{ formatDate(resource.createdAt) }}</span>
                    </div>
                  </div>

                  <div class="resource-actions">
                    <button 
                      @click="handleResourceView(resource)" 
                      class="action-btn"
                      :class="resource.hic === 1 ? 'hic-protected' : 'view'"
                    >
                      <font-awesome-icon :icon="resource.hic === 1 ? 'shield-alt' : 'eye'" />
                      {{ resource.hic === 1 ? 'HIC专属' : '访问资源' }}
                      <font-awesome-icon icon="external-link-alt" />
                    </button>
                  </div>
                </div>
              </div>
            </div>

            <!-- 分页 -->
            <div class="pagination-container" v-if="totalResources > 0">
              <el-pagination
                :current-page="currentPage"
                :page-size="pageSize"
                :total="totalResources"
                layout="prev, pager, next"
                @current-change="handlePageChange"
              />
            </div>
          </div>
        </div>
      </section>
    </div>
  </MainLayout>
</template>

<script setup>
import { ref, computed, onMounted, watch } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import MainLayout from '../layouts/MainLayout.vue'
import { getKnowledgeResources, incrementResourceView } from '../api/knowledge'
import { useAuthStore } from '../stores'
import http from '../utils/http'

const router = useRouter()
const route = useRoute()
const authStore = useAuthStore()

// 状态变量
const loading = ref(true)
const activeCategory = ref('全部')
const resourceType = ref('')
const searchQuery = ref('')
const currentPage = ref(1)
const pageSize = ref(9)
const resources = ref([])
const totalResources = ref(0)
const totalPages = ref(0)

// 计算属性（暂无需要）

// 监听分类变化
watch(activeCategory, () => {
  currentPage.value = 1
  resourceType.value = '' // 切换category时重置type筛选
  loadResources()
})

// 监听筛选条件变化
watch(resourceType, () => {
  currentPage.value = 1
  loadResources()
})

// 监听搜索关键词变化
watch(searchQuery, () => {
  currentPage.value = 1
  loadResources()
})

// 监听路由变化
watch(() => route.params.category, (newCategory) => {
  const targetCategory = newCategory || '全部'
  if (targetCategory !== activeCategory.value) {
    activeCategory.value = targetCategory
    // 重置筛选条件，但保留搜索
    resourceType.value = ''
    currentPage.value = 1
  }
})

// 生命周期钩子
onMounted(() => {
  // 根据URL参数设置初始分类
  activeCategory.value = route.params.category || '全部'
  
  // 加载数据
  loadResources()
})

// 加载资源数据
const loadResources = async () => {
  try {
    loading.value = true
    
    const params = {
      page: currentPage.value,
      size: pageSize.value,
      category: activeCategory.value !== '全部' ? activeCategory.value : '',
      type: resourceType.value || '',
      search: searchQuery.value || ''
    }
    
    const response = await getKnowledgeResources(params)
    
    const body = response.data || {}
    if (body.code === 0) {
      const data = body.data || {}
      resources.value = data.data || []
      totalResources.value = data.total || 0
      totalPages.value = data.totalPages || Math.ceil((data.total || 0) / pageSize.value)
    } else {
      throw new Error(body.message || '获取资源失败')
    }
  } catch (error) {
    ElMessage.error('加载资源失败，请稍后重试')
    resources.value = []
    totalResources.value = 0
    totalPages.value = 0
  } finally {
    loading.value = false
  }
}

// 方法
const handleCategoryChange = (tab) => {
  activeCategory.value = tab.props.name
  // 切换分类时重置type筛选，但保留搜索
  resourceType.value = ''
  currentPage.value = 1
  
  // 更新路由
  if (tab.props.name === '全部') {
    router.push('/knowledge')
  } else {
    router.push(`/knowledge/${tab.props.name}`)
  }
}



const applyFilters = () => {
  currentPage.value = 1
  loadResources()
}



const handlePageChange = (page) => {
  currentPage.value = page
  loadResources()
  // 滚动到页面顶部
  window.scrollTo({
    top: 0,
    behavior: 'smooth'
  })
}

// 处理资源查看
const handleResourceView = async (resource) => {
  try {
    // 判断是否为resources文件夹下的文件，如果是则直接下载
    if (resource.resourceUrl && resource.resourceUrl.includes('/resources/')) {
      // 检查是否需要HIC认证
      if (resource.hic === 1) {
        // 检查用户是否登录
        if (!authStore.isLoggedIn) {
          ElMessage.warning('需要HIC认证才能下载此资源，请先登录')
          router.push('/auth/login')
          return
        }
        
        // 检查用户是否有HIC认证
        if (authStore.user.hic !== 1) {
          ElMessage.warning('需要HIC认证才能下载此资源')
          return
        }
      }
      
      // 增加浏览次数
      await incrementResourceView(resource.id)
      // 更新本地浏览次数
      resource.eyeCount = (resource.eyeCount || 0) + 1
      
      // 使用axios下载文件，这样会自动携带认证头
      const downloadUrl = `/knowledge/resources/${resource.id}/download`
      
      try {
        const response = await http.get(downloadUrl, {
          responseType: 'blob'
        })
        
        // 从响应头获取文件名，支持UTF-8编码
        const contentDisposition = response.headers['content-disposition']
        let filename = 'download'
        if (contentDisposition) {
          // 优先尝试获取UTF-8编码的文件名
          const utf8Match = contentDisposition.match(/filename\*=UTF-8''([^;]+)/)
          if (utf8Match && utf8Match[1]) {
            filename = decodeURIComponent(utf8Match[1])
          } else {
            // 回退到普通文件名，改进正则表达式
            const filenameMatch = contentDisposition.match(/filename="([^"]+)"/)
            if (filenameMatch && filenameMatch[1]) {
              filename = filenameMatch[1]
            } else {
              // 再次回退，处理没有引号的情况
              const simpleMatch = contentDisposition.match(/filename=([^;]+)/)
              if (simpleMatch && simpleMatch[1]) {
                filename = simpleMatch[1].trim()
              }
            }
          }
        }
        
        // 如果未能从响应头获得文件名，尝试从资源链接提取
        if (!filename || filename === 'download') {
          const rawUrl = resource.resourceUrl || ''
          const lastSegment = rawUrl.split('?')[0].split('/').pop()
          if (lastSegment) {
            filename = decodeURIComponent(lastSegment)
          } else if (resource.title) {
            let titleName = resource.title
            if (titleName && !titleName.includes('.')) {
              const extMatch = rawUrl.match(/\.([a-zA-Z0-9]+)(?:$|\?)/)
              if (extMatch) {
                titleName = titleName + '.' + extMatch[1]
              }
            }
            filename = titleName
          }
        }
        // 清理非法字符，避免保存名被浏览器丢弃
        filename = (filename || '').replace(/[\\/:*?"<>|]/g, '_') || 'file'

        // 创建下载链接，使用响应的Content-Type
        const contentType = response.headers['content-type'] || 'application/octet-stream'
        const blob = new Blob([response.data], { type: contentType })
        const url = window.URL.createObjectURL(blob)
        const link = document.createElement('a')
        link.href = url
        link.download = filename
        link.setAttribute('download', filename)
        link.style.display = 'none'
        document.body.appendChild(link)
        link.click()
        document.body.removeChild(link)
        window.URL.revokeObjectURL(url)
        
        ElMessage.success('文件下载已开始')
      } catch (downloadError) {
        if (downloadError.response?.status === 401) {
          ElMessage.error('需要HIC认证才能下载此资源')
        } else if (downloadError.response?.status === 403) {
          ElMessage.error('没有权限下载此资源')
        } else {
          ElMessage.error('下载失败，请稍后重试')
        }
      }
    } else if (resource.resourceUrl) {
      // 检查是否需要HIC认证
      if (resource.hic === 1) {
        // 检查用户是否登录
        if (!authStore.isLoggedIn) {
          ElMessage.warning('需要HIC认证才能访问此资源，请先登录')
          router.push('/auth/login')
          return
        }
        
        // 检查用户是否有HIC认证
        if (authStore.user.hic !== 1) {
          ElMessage.warning('需要HIC认证才能访问此资源')
          return
        }
      }
      
      // 增加浏览次数
      await incrementResourceView(resource.id)
      // 更新本地浏览次数
      resource.eyeCount = (resource.eyeCount || 0) + 1
      
      // 如果有外部链接，则打开链接
      window.open(resource.resourceUrl, '_blank')
    } else {
      ElMessage.warning('该资源暂无可访问链接')
    }
  } catch (error) {
    // 即使增加浏览次数失败，也要尝试访问资源
    if (resource.resourceUrl && resource.resourceUrl.includes('/resources/')) {
      // 检查HIC认证后再下载
      if (resource.hic === 1 && (!authStore.isLoggedIn || authStore.user.hic !== 1)) {
        ElMessage.warning('需要HIC认证才能下载此资源')
        return
      }
      
      // 使用axios下载文件
      const downloadUrl = `/knowledge/resources/${resource.id}/download`
      try {
        const response = await http.get(downloadUrl, {
          responseType: 'blob'
        })
        
        // 从响应头获取文件名，支持UTF-8编码
        const contentDisposition = response.headers['content-disposition']
        let filename = 'download'
        if (contentDisposition) {
          // 优先尝试获取UTF-8编码的文件名
          const utf8Match = contentDisposition.match(/filename\*=UTF-8''([^;]+)/)
          if (utf8Match && utf8Match[1]) {
            filename = decodeURIComponent(utf8Match[1])
          } else {
            // 回退到普通文件名，改进正则表达式
            const filenameMatch = contentDisposition.match(/filename="([^"]+)"/)
            if (filenameMatch && filenameMatch[1]) {
              filename = filenameMatch[1]
            } else {
              // 再次回退，处理没有引号的情况
              const simpleMatch = contentDisposition.match(/filename=([^;]+)/)
              if (simpleMatch && simpleMatch[1]) {
                filename = simpleMatch[1].trim()
              }
            }
          }
        }
        
        // 如果未能从响应头获得文件名，尝试从资源链接提取
        if (!filename || filename === 'download') {
          const rawUrl = resource.resourceUrl || ''
          const lastSegment = rawUrl.split('?')[0].split('/').pop()
          if (lastSegment) {
            filename = decodeURIComponent(lastSegment)
          } else if (resource.title) {
            let titleName = resource.title
            if (titleName && !titleName.includes('.')) {
              const extMatch = rawUrl.match(/\.([a-zA-Z0-9]+)(?:$|\?)/)
              if (extMatch) {
                titleName = titleName + '.' + extMatch[1]
              }
            }
            filename = titleName
          }
        }
        // 清理非法字符，避免保存名被浏览器丢弃
        filename = (filename || '').replace(/[\\/:*?"<>|]/g, '_') || 'file'

        // 创建下载链接，使用响应的Content-Type
        const contentType = response.headers['content-type'] || 'application/octet-stream'
        const blob = new Blob([response.data], { type: contentType })
        const url = window.URL.createObjectURL(blob)
        const link = document.createElement('a')
        link.href = url
        link.download = filename
        link.setAttribute('download', filename)
        link.style.display = 'none'
        document.body.appendChild(link)
        link.click()
        document.body.removeChild(link)
        window.URL.revokeObjectURL(url)
      } catch (downloadError) {
        ElMessage.error('下载失败，请稍后重试')
      }
    } else if (resource.resourceUrl) {
      // 检查HIC认证后再访问
      if (resource.hic === 1 && (!authStore.isLoggedIn || authStore.user.hic !== 1)) {
        ElMessage.warning('需要HIC认证才能访问此资源')
        return
      }
      
      window.open(resource.resourceUrl, '_blank')
    }
  }
}

const formatDate = (dateStr) => {
  if (!dateStr) return '未知时间'
  const date = new Date(dateStr)
  if (isNaN(date.getTime())) return '未知时间'
  return date.toLocaleDateString('zh-CN', { year: 'numeric', month: 'long', day: 'numeric' })
}

// 为每个资源生成固定颜色，避免搜索时颜色变化
const getResourceColor = (resourceId) => {
  const colors = [
    '#f56c6c', '#e6a23c', '#67c23a', '#409eff', '#909399',
    '#9254de', '#36cfc9', '#40a9ff', '#ff7875', '#ffc53d'
  ]
  // 使用资源ID生成固定的颜色索引
  const index = resourceId ? resourceId % colors.length : 0
  return colors[index]
}

const getResourceTypeIcon = (type) => {
  const iconMap = {
    document: 'file-lines',
    video: 'video',
    project: 'folder',

    other: 'file'
  }
  return iconMap[type] || 'file'
}

const getResourceTypeName = (type) => {
  const types = {
    document: '文档',
    video: '视频',
    project: '项目',

    other: '其他'
  }
  return types[type] || '其他'
}

const getCategoryName = (category) => {
  const categories = {
    software: '软件',
    hardware: '硬件',
    embed: '嵌入式',
    ai: '人工智能',
    other: '其他'
  }
  return categories[category] || '其他'
}

const getCategoryTitle = (category) => {
  const titles = {
    '全部': '全部资源',
    'hardware': '硬件资源',
    'software': '软件资源',
    'embed': '嵌入式开发资源',
         'ai': '人工智能资源',
    'other': '其他资源'
  }
  return titles[category] || '资源'
}

// 处理搜索输入
const handleSearch = () => {
  currentPage.value = 1
  loadResources()
}


</script>

<style lang="scss" scoped>
// 导入变量文件
@import '../styles/variables';

.knowledge-page {
  padding-bottom: $spacing-3xl;
}

/* 页面标题区 */
.page-header {
  background-color: #f0f2f5;
  padding: 60px 0;
  margin-bottom: 40px;
  text-align: center;
}

.page-title {
  font-size: 30px;
  color: #2d3748;
  margin-bottom: 1rem;
  font-weight: 700;
}

.page-description {
  max-width: 800px;
  color: #4a5568;
  line-height: 1.6;
  text-align: center;
  margin: 0 auto;
  font-size: 1.1rem;
}

/* 分类导航 */
.category-nav {
  margin-bottom: $spacing-xl;
}

/* 筛选部分 */
.filter-section {
  margin-bottom: $spacing-xl;
  background-color: white;
  padding: $spacing-lg 0;
  border-bottom: 1px solid #f0f0f0;
}

.filters {
  display: flex;
  justify-content: space-between;
  align-items: center;
  
  @media (max-width: $breakpoint-md) {
    flex-direction: column;
    gap: $spacing-lg;
    align-items: stretch;
  }
}

.filter-left {
  display: flex;
  gap: $spacing-lg;
  align-items: center;
  
  @media (max-width: $breakpoint-md) {
    flex-direction: column;
    align-items: stretch;
  }
}

.filter-right {
  display: flex;
  gap: $spacing-lg;
  align-items: center;
  
  @media (max-width: $breakpoint-md) {
    flex-direction: column;
    align-items: stretch;
  }
}

.filter-group {
  display: flex;
  flex-direction: column;
  gap: $spacing-sm;
}

.filter-label {
  font-weight: 500;
  color: $dark-color;
  font-size: $font-size-sm;
}

/* 搜索输入组合样式 */
.search-input-group {
  display: flex;
  gap: $spacing-sm;
  
  .search-input {
    flex: 1;
  }
  
  .search-button {
    white-space: nowrap;
    
    .svg-inline--fa {
      margin-right: 4px;
    }
  }
  
  @media (max-width: $breakpoint-sm) {
    flex-direction: column;
    
    .search-button {
      width: 100%;
    }
  }
}

.filter-actions {
  margin-top: $spacing-md;
  display: flex;
  gap: $spacing-sm;
  justify-content: center;
}

/* 内容区 */
.content-section {
  min-height: 500px;
}

.section-title {
  font-size: $font-size-2xl;
  margin-bottom: $spacing-lg;
  
  .resource-count {
    font-size: $font-size-base;
    color: $gray-dark-color;
    font-weight: normal;
  }
}

/* 资源列表 */
.resources-list {
  @media (max-width: $breakpoint-lg) {
    grid-template-columns: repeat(2, 1fr);
  }
  
  @media (max-width: $breakpoint-sm) {
    grid-template-columns: 1fr;
  }
}

.resource-card {
  background-color: white;
  border-radius: $border-radius-lg;
  overflow: hidden;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.05);
  transition: transform 0.3s ease, box-shadow 0.3s ease;
  
  &:hover {
    transform: translateY(-5px);
    box-shadow: 0 8px 20px rgba(0, 0, 0, 0.1);
  }
}

.resource-image {
  height: 160px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
  font-size: 48px;
}

.resource-content {
  padding: $spacing-lg;
  position: relative;
}

.resource-tags {
  position: absolute;
  top: 10px;
  right: $spacing-lg;
  display: flex;
  gap: $spacing-xs;
  z-index: 10;
}

.resource-category-tag {
  padding: 4px 8px;
  border-radius: $border-radius-full;
  font-size: $font-size-xs;
  font-weight: 500;
  color: white;
  
  &.category-software {
    background-color: #409eff;
  }
  
  &.category-hardware {
    background-color: #f56c6c;
  }
  
  &.category-embed {
    background-color: #e6a23c;
  }
  
  &.category-ai {
    background-color: #9254de;
  }
  
  &.category-other {
    background-color: #909399;
  }
}

.resource-type-tag {
  padding: 4px 8px;
  border-radius: $border-radius-full;
  font-size: $font-size-xs;
  font-weight: 500;
  color: white;
  
  &.type-document {
    background-color: #67c23a;
  }
  
  &.type-video {
    background-color: #ff7875;
  }
  
  &.type-project {
    background-color: #36cfc9;
  }
  

  
  &.type-other {
    background-color: #b7eb8f;
  }
}

.resource-title {
  font-size: $font-size-lg;
  margin-bottom: $spacing-sm;
  line-height: 1.4;
  height: 50px; // 固定高度，显示两行
  overflow: hidden;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
}

.resource-description {
  color: $gray-dark-color;
  margin-bottom: $spacing-lg;
  line-height: 1.5;
  height: 66px; // 固定高度，显示三行
  overflow: hidden;
  display: -webkit-box;
  -webkit-line-clamp: 3;
  -webkit-box-orient: vertical;
}

.resource-meta {
  display: flex;
  justify-content: space-between;
  margin-bottom: $spacing-lg;
  
  .meta-item {
    display: flex;
    align-items: center;
    font-size: $font-size-sm;
    color: $gray-dark-color;
    
    .svg-inline--fa {
      margin-right: 4px;
    }
    
  }
}

.resource-actions {
  display: flex;
  gap: $spacing-sm;
}

.action-btn {
  display: inline-flex;
  align-items: center;
  gap: 0.5rem;
  padding: 0.5rem 1rem;
  border-radius: 0.375rem;
  border: none;
  cursor: pointer;
  text-decoration: none;
  font-size: 0.875rem;
  font-weight: 500;
  transition: all 0.2s ease;

  &.view {
    background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
    color: white;

    &:hover {
      transform: translateY(-1px);
      box-shadow: 0 4px 12px rgba(102, 126, 234, 0.4);
    }
  }

  &.hic-protected {
    background: linear-gradient(135deg, #ff6b6b 0%, #ee5a24 100%);
    color: white;
    position: relative;

    &:hover {
      transform: translateY(-1px);
      box-shadow: 0 4px 12px rgba(255, 107, 107, 0.4);
    }

    &::before {
      content: '';
      position: absolute;
      top: -2px;
      left: -2px;
      right: -2px;
      bottom: -2px;
      background: linear-gradient(45deg, #ff6b6b, #ee5a24, #ff6b6b);
      border-radius: 0.5rem;
      z-index: -1;
      animation: hic-glow 2s ease-in-out infinite alternate;
    }
  }

  &.download {
    background: linear-gradient(135deg, #f093fb 0%, #f5576c 100%);
    color: white;

    &:hover {
      transform: translateY(-1px);
      box-shadow: 0 4px 12px rgba(240, 147, 251, 0.4);
    }
  }
}

/* 空状态 */
.empty-container {
  text-align: center;
  padding: $spacing-3xl 0;
}

.empty-icon {
  font-size: 64px;
  color: $gray-color;
  margin-bottom: $spacing-lg;
}

.empty-title {
  font-size: $font-size-xl;
  margin-bottom: $spacing-md;
  color: $dark-color;
}

.empty-description {
  color: $gray-dark-color;
  max-width: 400px;
  margin: 0 auto;
}

.reset-link {
  color: $primary-color;
  background: none;
  border: none;
  cursor: pointer;
  font-weight: 500;
  
  &:hover {
    text-decoration: underline;
  }
}

/* HIC发光动画 */
@keyframes hic-glow {
  0% {
    opacity: 0.7;
    transform: scale(1);
  }
  100% {
    opacity: 1;
    transform: scale(1.02);
  }
}

/* 分页 */
.pagination-container {
  margin-top: $spacing-2xl;
  display: flex;
  justify-content: center;
}

/* 加载状态 */
.loading-container {
  padding: $spacing-xl 0;
}


</style>