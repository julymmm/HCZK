<template>
  <MainLayout>
    <section class="upload-page">
      <div class="upload-shell">
        <div class="page-head">
          <button class="back-btn" @click="router.push('/share')">返回</button>
          <div>
            <h1>发布经验分享</h1>
            <p>按草稿、直传、确认、补全和发布的流程提交 Markdown 正文。</p>
          </div>
        </div>

        <el-steps :active="activeStep" finish-status="success" class="publish-steps">
          <el-step title="创建草稿" />
          <el-step title="直传 OSS" />
          <el-step title="确认内容" />
          <el-step title="正式发布" />
        </el-steps>

        <el-form label-position="top" class="upload-form" @submit.prevent>
          <el-form-item label="分享标题" required>
            <el-input v-model="form.title" maxlength="100" show-word-limit placeholder="例如：秋招算法岗准备路线复盘" />
          </el-form-item>

          <el-form-item label="分享分类" required>
            <el-select v-model="form.category" placeholder="请选择分类" class="full-width">
              <el-option label="学习经验" value="learn" />
              <el-option label="求职经验" value="job" />
              <el-option label="规划建议" value="plan" />
              <el-option label="其他" value="others" />
            </el-select>
          </el-form-item>

          <el-form-item label="正文兜底内容">
            <el-input v-model="form.content" type="textarea" :rows="5" maxlength="1200" show-word-limit placeholder="可填写文章简介；当 Markdown 无法读取时作为搜索摘要和 RAG 兜底内容。" />
          </el-form-item>

          <el-form-item label="标签">
            <el-input v-model="form.tags" maxlength="255" placeholder="多个标签用逗号分隔，例如：Java,面试,规划" />
          </el-form-item>

          <el-form-item label="Markdown 正文" required>
            <el-upload
              drag
              action="#"
              :auto-upload="false"
              :limit="1"
              accept=".md,.markdown,text/markdown,text/plain"
              :on-change="handleFileChange"
              :on-remove="handleFileRemove"
              :on-exceed="handleExceed"
            >
              <div class="upload-drop">
                <div class="upload-icon">MD</div>
                <p>拖拽或点击选择 Markdown 文件</p>
                <span>发布时文件将通过预签名 URL 直传到 OSS</span>
              </div>
            </el-upload>
            <el-progress v-if="uploadProgress > 0" :percentage="uploadProgress" class="upload-progress" />
          </el-form-item>

          <el-form-item label="AI 摘要">
            <div class="summary-row">
              <el-input v-model="form.aiSummary" type="textarea" :rows="3" maxlength="80" show-word-limit placeholder="可手动填写，也可根据 Markdown 正文生成。" />
              <el-button type="primary" plain :loading="summaryLoading" @click="generateSummary">生成</el-button>
            </div>
          </el-form-item>

          <div class="form-actions">
            <el-button @click="resetForm">重置</el-button>
            <el-button type="primary" :loading="submitting" @click="submitShare">发布分享</el-button>
          </div>
        </el-form>
      </div>
    </section>
  </MainLayout>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import MainLayout from '../layouts/MainLayout.vue'
import { suggestShareSummary } from '../api/ai'
import {
  confirmShareContent,
  createShareDraft,
  patchShareMetadata,
  presignShareContent,
  publishShare
} from '../api/shares'

const router = useRouter()
const form = reactive({ title: '', category: '', content: '', tags: '', aiSummary: '' })
const markdownFile = ref(null)
const markdownText = ref('')
const submitting = ref(false)
const summaryLoading = ref(false)
const uploadProgress = ref(0)
const activeStep = ref(0)

const unwrap = (resp) => resp.data?.data || resp.data || {}

const readFileText = (file) => new Promise((resolve, reject) => {
  const reader = new FileReader()
  reader.onload = () => resolve(String(reader.result || ''))
  reader.onerror = () => reject(reader.error)
  reader.readAsText(file, 'utf-8')
})

const sha256Hex = async (file) => {
  const buffer = await file.arrayBuffer()
  const digest = await crypto.subtle.digest('SHA-256', buffer)
  return Array.from(new Uint8Array(digest)).map((b) => b.toString(16).padStart(2, '0')).join('')
}

const handleFileChange = async (uploadFile) => {
  const file = uploadFile.raw
  if (!file) return
  const name = file.name.toLowerCase()
  if (!name.endsWith('.md') && !name.endsWith('.markdown')) {
    ElMessage.error('只能上传 Markdown 文件')
    return false
  }
  markdownFile.value = file
  markdownText.value = await readFileText(file)
}

const handleFileRemove = () => {
  markdownFile.value = null
  markdownText.value = ''
  uploadProgress.value = 0
}

const handleExceed = () => ElMessage.warning('只能上传一个 Markdown 文件')

const generateSummary = async () => {
  const source = markdownText.value || form.content
  if (!source.trim()) {
    ElMessage.warning('请先填写正文或选择 Markdown 文件')
    return
  }
  summaryLoading.value = true
  try {
    const data = unwrap(await suggestShareSummary(source.slice(0, 6000)))
    form.aiSummary = data.summary || ''
  } catch (error) {
    ElMessage.warning(error.response?.data?.message || '摘要生成失败，可手动填写')
  } finally {
    summaryLoading.value = false
  }
}

const validate = () => {
  if (!form.title.trim()) return '请填写分享标题'
  if (!form.category) return '请选择分享分类'
  if (!markdownFile.value && !form.content.trim()) return '请上传 Markdown 文件或填写正文兜底内容'
  return ''
}

const putToOss = async (putUrl, headers, file) => {
  const response = await fetch(putUrl, {
    method: 'PUT',
    headers: headers || { 'Content-Type': file.type || 'text/markdown' },
    body: file
  })
  if (!response.ok) throw new Error(`OSS 上传失败：${response.status}`)
  uploadProgress.value = 100
  return response.headers.get('ETag') || response.headers.get('etag') || ''
}

const submitShare = async () => {
  const message = validate()
  if (message) {
    ElMessage.warning(message)
    return
  }

  submitting.value = true
  activeStep.value = 0
  uploadProgress.value = 0
  try {
    const draft = unwrap(await createShareDraft())
    const shareId = draft.id
    activeStep.value = 1

    let contentMeta = null
    if (markdownFile.value) {
      const ext = markdownFile.value.name.toLowerCase().endsWith('.markdown') ? '.markdown' : '.md'
      const presign = unwrap(await presignShareContent({ postId: shareId, contentType: 'text/markdown', ext }))
      const etag = await putToOss(presign.putUrl, presign.headers, markdownFile.value)
      const sha256 = await sha256Hex(markdownFile.value)
      contentMeta = {
        objectKey: presign.objectKey,
        etag: etag || sha256,
        size: markdownFile.value.size,
        sha256
      }
      activeStep.value = 2
      await confirmShareContent(shareId, contentMeta)
    }

    await patchShareMetadata(shareId, {
      title: form.title.trim(),
      category: form.category,
      content: form.content.trim() || markdownText.value.slice(0, 1200) || form.title.trim(),
      tags: form.tags.trim(),
      aiSummary: form.aiSummary.trim()
    })

    activeStep.value = 3
    const published = unwrap(await publishShare(shareId))
    ElMessage.success('经验分享发布成功')
    const result = await ElMessageBox.confirm('是否立即查看这篇分享？', '发布成功', {
      confirmButtonText: '查看分享',
      cancelButtonText: '返回列表',
      type: 'success'
    }).catch(() => 'cancel')
    router.push(result === 'confirm' && published.id ? `/share/${published.id}` : '/share')
  } catch (error) {
    ElMessage.error(error.message || error.response?.data?.message || '发布失败，请检查 OSS 配置后重试')
  } finally {
    submitting.value = false
  }
}

const resetForm = () => {
  form.title = ''
  form.category = ''
  form.content = ''
  form.tags = ''
  form.aiSummary = ''
  markdownFile.value = null
  markdownText.value = ''
  uploadProgress.value = 0
  activeStep.value = 0
}
</script>

<style scoped lang="scss">
.upload-page { min-height: 100vh; background: #f6f8fb; padding: 96px 16px 48px; }
.upload-shell { max-width: 900px; margin: 0 auto; }
.page-head { display: flex; align-items: flex-start; gap: 16px; margin-bottom: 22px; }
.page-head h1 { margin: 0 0 8px; font-size: 30px; color: #172033; }
.page-head p { margin: 0; color: #64748b; }
.back-btn { border: 1px solid #dbe3ef; background: white; color: #475569; border-radius: 6px; padding: 8px 12px; cursor: pointer; }
.publish-steps { background: white; border: 1px solid #e5e7eb; border-radius: 8px; padding: 18px 20px; margin-bottom: 18px; }
.upload-form { background: white; border: 1px solid #e5e7eb; border-radius: 8px; padding: 24px; box-shadow: 0 8px 22px rgba(15, 23, 42, .05); }
.full-width { width: 100%; }
.upload-drop { padding: 14px 0; text-align: center; color: #475569; }
.upload-drop p { margin: 8px 0 4px; font-weight: 600; }
.upload-drop span { color: #94a3b8; font-size: 13px; }
.upload-icon { width: 44px; height: 44px; display: inline-flex; align-items: center; justify-content: center; border-radius: 8px; background: #eff6ff; color: #2563eb; font-weight: 800; }
.upload-progress { margin-top: 12px; width: 100%; }
.summary-row { display: grid; grid-template-columns: minmax(0, 1fr) auto; gap: 12px; width: 100%; align-items: start; }
.form-actions { display: flex; justify-content: flex-end; gap: 12px; padding-top: 8px; }
@media (max-width: 720px) { .summary-row { grid-template-columns: 1fr; } .page-head { flex-direction: column; } }
</style>
