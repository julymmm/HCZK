<template>
  <MainLayout>
    <section class="search-page">
      <div class="search-shell">
        <header class="page-head">
          <div>
            <p class="eyebrow">站内检索</p>
            <h1>搜索华创智库内容</h1>
          </div>
          <p>统一检索知识资源、项目分享、华创推文和师兄师姐经验。</p>
        </header>

        <div class="search-layout">
          <aside class="search-panel">
            <el-autocomplete
              v-model="keyword"
              :fetch-suggestions="querySuggestions"
              placeholder="输入关键词"
              clearable
              class="keyword-input"
              @select="handleSuggestSelect"
              @keyup.enter="startSearch"
            />

            <div class="type-tabs">
              <button
                v-for="option in typeOptions"
                :key="option.value || 'all'"
                :class="['type-tab', { active: filters.type === option.value }]"
                type="button"
                @click="selectType(option.value)"
              >
                {{ option.label }}
              </button>
            </div>

            <el-input v-model="filters.category" placeholder="分类" clearable @keyup.enter="startSearch" />
            <el-input v-model="filters.tags" placeholder="标签，逗号分隔" clearable @keyup.enter="startSearch" />

            <div class="panel-actions">
              <el-button type="primary" :loading="loading" @click="startSearch">搜索</el-button>
              <el-button @click="resetSearch">重置</el-button>
            </div>
          </aside>

          <main class="result-area">
            <div class="result-toolbar">
              <div>
                <h2>{{ searched ? '搜索结果' : '内容发现' }}</h2>
                <span v-if="searched">共找到 {{ total }} 条内容</span>
                <span v-else>输入关键词后，可以在全站内容中快速定位。</span>
              </div>
              <span v-if="fallback" class="fallback">数据库兜底检索</span>
            </div>

            <div v-if="loading" class="loading-list">
              <el-skeleton :rows="5" animated />
            </div>

            <div v-else-if="items.length" class="result-list">
              <article
                v-for="item in items"
                :key="`${item.contentType}-${item.contentId}`"
                class="result-card"
                @click="goResult(item)"
              >
                <div class="result-topline">
                  <el-tag size="small" :type="tagType(item.contentType)">{{ typeLabel(item.contentType) }}</el-tag>
                  <span v-if="item.category" class="category">{{ item.category }}</span>
                  <span class="heat">{{ item.viewCount || 0 }} 浏览 · {{ item.likeCount || 0 }} 赞</span>
                </div>
                <h3 v-html="highlight(item, 'title') || item.title || '未命名内容'"></h3>
                <p v-html="highlight(item, 'description') || highlight(item, 'body') || item.description || '暂无摘要'"></p>
                <div class="result-meta">
                  <span>{{ item.authorName || '匿名作者' }}</span>
                  <span v-if="item.publishTime">{{ formatDate(item.publishTime) }}</span>
                  <span v-if="item.hicProtected" class="protected">HIC 认证内容</span>
                </div>
                <div v-if="item.tags?.length" class="tags">
                  <el-tag v-for="tag in item.tags" :key="tag" size="small" effect="plain">{{ tag }}</el-tag>
                </div>
              </article>
            </div>

            <el-empty v-else-if="searched" description="没有找到相关内容" />

            <div v-else class="empty-guide">
              <h3>可以搜索这些内容</h3>
              <div class="guide-grid">
                <button v-for="word in hotWords" :key="word" type="button" @click="quickSearch(word)">{{ word }}</button>
              </div>
            </div>

            <div class="load-more" v-if="nextAfter">
              <el-button :loading="loadingMore" @click="loadMore">加载更多</el-button>
            </div>
          </main>
        </div>
      </div>
    </section>
  </MainLayout>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import MainLayout from '../layouts/MainLayout.vue'
import { searchContent, suggestContent } from '../api/search'

const route = useRoute()
const router = useRouter()
const keyword = ref(route.query.q || '')
const filters = reactive({
  type: route.query.type || '',
  category: route.query.category || '',
  tags: route.query.tags || ''
})
const items = ref([])
const total = ref(0)
const nextAfter = ref('')
const fallback = ref(false)
const loading = ref(false)
const loadingMore = ref(false)
const searched = ref(false)

const typeOptions = [
  { label: '全部', value: '' },
  { label: '经验', value: 'share' },
  { label: '资源', value: 'resource' },
  { label: '项目', value: 'project' },
  { label: '推文', value: 'article' }
]
const hotWords = ['就业', '人工智能', '嵌入式', '华为竞赛', '项目经验', '学习路线']

const unwrap = (resp) => resp.data?.data || resp.data || {}

const buildParams = (after = '') => ({
  q: keyword.value,
  type: filters.type || undefined,
  category: filters.category || undefined,
  tags: filters.tags || undefined,
  size: 10,
  after: after || undefined
})

const runSearch = async (append = false) => {
  if (!keyword.value?.trim() && !filters.type && !filters.category && !filters.tags) {
    items.value = []
    total.value = 0
    nextAfter.value = ''
    searched.value = false
    return
  }

  if (!append) loading.value = true
  else loadingMore.value = true
  try {
    const data = unwrap(await searchContent(buildParams(append ? nextAfter.value : '')))
    const nextItems = data.items || data.records || []
    items.value = append ? [...items.value, ...nextItems] : nextItems
    total.value = data.total || items.value.length
    nextAfter.value = data.nextAfter || ''
    fallback.value = !!data.fallback
    searched.value = true
    router.replace({
      query: {
        q: keyword.value || undefined,
        type: filters.type || undefined,
        category: filters.category || undefined,
        tags: filters.tags || undefined
      }
    })
  } finally {
    loading.value = false
    loadingMore.value = false
  }
}

const startSearch = () => runSearch(false)
const loadMore = () => runSearch(true)

const querySuggestions = async (prefix, callback) => {
  if (!prefix?.trim()) return callback([])
  try {
    const data = unwrap(await suggestContent({ prefix, size: 8 }))
    callback((data.suggestions || []).map(value => ({ value })))
  } catch (_) {
    callback([])
  }
}

const handleSuggestSelect = (item) => {
  keyword.value = item.value
  startSearch()
}

const selectType = (type) => {
  filters.type = type
  startSearch()
}

const quickSearch = (word) => {
  keyword.value = word
  startSearch()
}

const resetSearch = () => {
  keyword.value = ''
  filters.type = ''
  filters.category = ''
  filters.tags = ''
  items.value = []
  total.value = 0
  nextAfter.value = ''
  fallback.value = false
  searched.value = false
  router.replace({ query: {} })
}

const highlight = (item, field) => {
  const value = item.highlights?.[field]
  return Array.isArray(value) ? value.join('...') : value || ''
}

const typeLabel = (type) => ({ share: '经验', resource: '资源', project: '项目', article: '推文' }[type] || '内容')
const tagType = (type) => ({ share: 'success', resource: 'primary', project: 'warning', article: 'info' }[type] || 'info')
const formatDate = (value) => new Date(value).toLocaleDateString('zh-CN')

const goResult = (item) => {
  if (item.url) router.push(item.url)
}

onMounted(() => {
  if (keyword.value || filters.type || filters.category || filters.tags) startSearch()
})
</script>

<style scoped lang="scss">
.search-page {
  min-height: 100vh;
  background: #f6f8fb;
  padding: 72px 20px 56px;
}

.search-shell {
  max-width: 1180px;
  margin: 0 auto;
}

.page-head {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 32px;
  margin-bottom: 24px;
}

.eyebrow {
  margin: 0 0 8px;
  color: #2563eb;
  font-size: 14px;
  font-weight: 700;
}

.page-head h1 {
  margin: 0;
  font-size: 34px;
  color: #172033;
}

.page-head > p {
  max-width: 460px;
  margin: 0;
  color: #64748b;
  line-height: 1.7;
}

.search-layout {
  display: grid;
  grid-template-columns: 300px minmax(0, 1fr);
  gap: 20px;
  align-items: start;
}

.search-panel,
.result-area {
  background: #fff;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
}

.search-panel {
  position: sticky;
  top: 92px;
  display: grid;
  gap: 14px;
  padding: 18px;
}

.keyword-input {
  width: 100%;
}

.type-tabs {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 8px;
}

.type-tab {
  height: 34px;
  border: 1px solid #dbe3ef;
  border-radius: 6px;
  background: #fff;
  color: #334155;
  cursor: pointer;
}

.type-tab.active {
  color: #e11d2e;
  border-color: #e11d2e;
  background: #fff5f6;
  font-weight: 700;
}

.panel-actions {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 10px;
}

.result-area {
  min-height: 520px;
  padding: 20px;
}

.result-toolbar {
  display: flex;
  justify-content: space-between;
  gap: 16px;
  align-items: center;
  padding-bottom: 16px;
  border-bottom: 1px solid #edf0f5;
  margin-bottom: 16px;
}

.result-toolbar h2 {
  margin: 0 0 4px;
  font-size: 22px;
  color: #172033;
}

.result-toolbar span {
  color: #64748b;
}

.fallback {
  color: #b45309;
  background: #fff7ed;
  border: 1px solid #fed7aa;
  padding: 4px 8px;
  border-radius: 4px;
  white-space: nowrap;
}

.loading-list {
  padding: 12px 0;
}

.result-list {
  display: grid;
  gap: 12px;
}

.result-card {
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  padding: 16px;
  cursor: pointer;
  transition: 0.18s ease;
}

.result-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 10px 22px rgba(15, 23, 42, 0.08);
  border-color: #bfdbfe;
}

.result-topline,
.result-meta {
  display: flex;
  gap: 10px;
  align-items: center;
  flex-wrap: wrap;
  color: #64748b;
  font-size: 13px;
}

.result-card h3 {
  margin: 12px 0 8px;
  font-size: 19px;
  color: #111827;
}

.result-card p {
  margin: 0 0 12px;
  color: #475569;
  line-height: 1.7;
}

.heat {
  margin-left: auto;
}

.tags {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
  margin-top: 12px;
}

.protected {
  color: #b45309;
}

.empty-guide {
  padding: 58px 12px;
  text-align: center;
}

.empty-guide h3 {
  margin: 0 0 18px;
  color: #334155;
}

.guide-grid {
  display: flex;
  justify-content: center;
  flex-wrap: wrap;
  gap: 10px;
}

.guide-grid button {
  border: 1px solid #dbe3ef;
  border-radius: 6px;
  background: #f8fafc;
  color: #334155;
  height: 34px;
  padding: 0 14px;
  cursor: pointer;
}

.guide-grid button:hover {
  color: #2563eb;
  border-color: #93c5fd;
}

:deep(em) {
  color: #dc2626;
  font-style: normal;
  background: #fef2f2;
  padding: 0 2px;
  border-radius: 2px;
}

.load-more {
  text-align: center;
  margin-top: 24px;
}

@media (max-width: 900px) {
  .page-head,
  .result-toolbar {
    align-items: flex-start;
    flex-direction: column;
  }

  .search-layout {
    grid-template-columns: 1fr;
  }

  .search-panel {
    position: static;
  }

  .heat {
    margin-left: 0;
  }
}
</style>