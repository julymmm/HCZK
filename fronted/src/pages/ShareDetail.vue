<template>
  <MainLayout>
    <div class="share-detail-page">
      <section class="detail-hero">
        <button class="back-btn" @click="router.push('/share')">返回列表</button>
        <div class="hero-inner">
          <el-tag v-if="detail.category" type="success" effect="plain">{{ categoryLabel(detail.category) }}</el-tag>
          <h1>{{ detail.title || '经验分享详情' }}</h1>
          <div class="meta-row">
            <span>{{ detail.authorName || '匿名作者' }}</span>
            <span v-if="detail.publishTime || detail.createdAt">{{ formatDate(detail.publishTime || detail.createdAt) }}</span>
            <span>{{ detail.viewCount || 0 }} 次浏览</span>
          </div>
        </div>
      </section>

      <main v-if="!loading" class="detail-layout">
        <article class="content-panel">
          <section class="ai-summary-panel">
            <div class="panel-title">
              <span>AI 摘要</span>
              <el-button size="small" type="primary" plain :loading="summaryLoading" @click="loadSummary(true)">生成摘要</el-button>
            </div>
            <p v-if="aiSummary">{{ aiSummary }}</p>
            <p v-else class="muted">暂时没有摘要，可点击生成。</p>
          </section>

          <div v-if="htmlContent" class="markdown-content" v-html="htmlContent"></div>
          <el-empty v-else description="暂无分享正文" />
        </article>

        <aside class="rag-panel">
          <div class="panel-title">
            <span>围绕本文提问</span>
            <el-tag size="small" type="info" effect="plain">RAG</el-tag>
          </div>
          <p class="muted">回答只依据当前经验分享正文生成。</p>
          <el-input v-model="question" type="textarea" :rows="4" maxlength="300" show-word-limit placeholder="例如：这篇分享里建议如何准备面试？" />
          <div class="qa-actions">
            <el-button type="primary" :loading="qaLoading" :disabled="!question.trim()" @click="askQuestion">提问</el-button>
            <el-button v-if="qaLoading" @click="stopAnswer">停止</el-button>
          </div>
          <div class="answer-box" v-if="answer || qaLoading">
            <div class="answer-title">回答</div>
            <p>{{ answer || '正在生成...' }}</p>
          </div>
          <div v-if="qaError" class="qa-error">{{ qaError }}</div>
        </aside>
      </main>

      <div v-else class="loading-state">
        <el-icon class="is-loading"><Loading /></el-icon>
        <span>正在加载分享内容...</span>
      </div>
    </div>
  </MainLayout>
</template>

<script setup>
import { onBeforeUnmount, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Loading } from '@element-plus/icons-vue'
import { marked } from 'marked'
import DOMPurify from 'dompurify'
import MainLayout from '../layouts/MainLayout.vue'
import { getShareDetail } from '../api/shares'
import { getShareSummary, streamShareQa } from '../api/ai'
import { config } from '../utils/config'

const route = useRoute()
const router = useRouter()
const loading = ref(true)
const detail = ref({})
const htmlContent = ref('')
const rawMarkdown = ref('')
const aiSummary = ref('')
const summaryLoading = ref(false)
const question = ref('')
const answer = ref('')
const qaLoading = ref(false)
const qaError = ref('')
let controller = null

const unwrap = (resp) => resp.data?.data || resp.data || {}

const absoluteUrl = (url) => {
  if (!url) return ''
  if (/^https?:\/\//i.test(url)) return url
  return config.getUploadUrl(url)
}

const renderMarkdown = (markdown) => {
  marked.setOptions({ breaks: true, gfm: true })
  htmlContent.value = DOMPurify.sanitize(marked.parse(markdown || ''))
}

const loadMarkdown = async (share) => {
  if (share.textUrl) {
    const resp = await fetch(`${absoluteUrl(share.textUrl)}?_t=${Date.now()}`, { cache: 'no-store' })
    if (!resp.ok) throw new Error('Markdown 文件加载失败')
    rawMarkdown.value = await resp.text()
  } else {
    rawMarkdown.value = share.content || ''
  }
  renderMarkdown(rawMarkdown.value)
}

const fetchDetail = async () => {
  loading.value = true
  try {
    const data = unwrap(await getShareDetail(route.params.id))
    detail.value = data
    aiSummary.value = data.aiSummary || ''
    await loadMarkdown(data)
    if (!aiSummary.value) loadSummary(false)
  } catch (error) {
    ElMessage.error(error.message || '分享加载失败')
  } finally {
    loading.value = false
  }
}

const loadSummary = async (showError) => {
  summaryLoading.value = true
  try {
    const data = unwrap(await getShareSummary(route.params.id))
    aiSummary.value = data.summary || ''
  } catch (error) {
    if (showError) ElMessage.warning(error.response?.data?.message || '暂时无法生成摘要')
  } finally {
    summaryLoading.value = false
  }
}

const askQuestion = async () => {
  qaLoading.value = true
  qaError.value = ''
  answer.value = ''
  controller = new AbortController()
  try {
    await streamShareQa(route.params.id, {
      question: question.value,
      signal: controller.signal,
      onChunk: (_, fullText) => { answer.value = fullText }
    })
  } catch (error) {
    if (error.name !== 'AbortError') qaError.value = error.message || '问答生成失败'
  } finally {
    qaLoading.value = false
    controller = null
  }
}

const stopAnswer = () => controller?.abort()
const formatDate = (value) => new Date(value).toLocaleDateString('zh-CN')
const categoryLabel = (value) => ({ learn: '学习经验', job: '求职经验', plan: '规划建议', others: '其他' }[value] || '其他')

onMounted(fetchDetail)
onBeforeUnmount(() => stopAnswer())
</script>

<style scoped lang="scss">
.share-detail-page { min-height: 100vh; background: #f6f8fb; padding-bottom: 48px; }
.detail-hero { background: linear-gradient(135deg, #eef7ff, #f7fbf5); padding: 96px 16px 48px; position: relative; border-bottom: 1px solid #e5e7eb; }
.hero-inner { max-width: 920px; margin: 0 auto; text-align: center; }
.hero-inner h1 { margin: 16px 0; font-size: clamp(28px, 4vw, 44px); color: #172033; line-height: 1.25; }
.meta-row { display: flex; justify-content: center; gap: 16px; flex-wrap: wrap; color: #64748b; }
.back-btn { position: absolute; left: 24px; top: 82px; border: 1px solid #dbe3ef; background: white; color: #475569; border-radius: 6px; padding: 8px 12px; cursor: pointer; }
.detail-layout { max-width: 1180px; margin: 28px auto 0; padding: 0 16px; display: grid; grid-template-columns: minmax(0, 1fr) 340px; gap: 22px; align-items: start; }
.content-panel, .rag-panel, .ai-summary-panel { background: white; border: 1px solid #e5e7eb; border-radius: 8px; box-shadow: 0 8px 22px rgba(15, 23, 42, .05); }
.content-panel { padding: 22px; }
.ai-summary-panel { padding: 18px; margin-bottom: 18px; background: #fbfdff; }
.rag-panel { padding: 18px; position: sticky; top: 82px; }
.panel-title { display: flex; justify-content: space-between; align-items: center; gap: 12px; margin-bottom: 12px; font-weight: 700; color: #1f2937; }
.muted { color: #64748b; line-height: 1.7; margin: 0 0 12px; }
.qa-actions { display: flex; gap: 10px; margin: 12px 0; }
.answer-box { background: #f8fafc; border: 1px solid #e2e8f0; border-radius: 6px; padding: 12px; white-space: pre-wrap; line-height: 1.7; color: #334155; }
.answer-title { font-weight: 700; color: #1f2937; margin-bottom: 6px; }
.qa-error { color: #dc2626; background: #fef2f2; border: 1px solid #fecaca; border-radius: 6px; padding: 10px; margin-top: 12px; }
.loading-state { min-height: 360px; display: flex; align-items: center; justify-content: center; gap: 10px; color: #64748b; padding-top: 120px; }
:deep(.markdown-content) { color: #273142; line-height: 1.78; font-size: 16px; }
:deep(.markdown-content h1), :deep(.markdown-content h2), :deep(.markdown-content h3) { color: #172033; margin-top: 1.8em; }
:deep(.markdown-content img) { max-width: 100%; border-radius: 6px; }
:deep(.markdown-content pre) { overflow-x: auto; background: #f6f8fa; padding: 14px; border-radius: 6px; }
@media (max-width: 960px) { .detail-layout { grid-template-columns: 1fr; } .rag-panel { position: static; } .back-btn { position: static; margin: 0 0 20px; } }
</style>
