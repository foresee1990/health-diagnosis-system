<template>
  <div class="panel">
    <div class="panel__header">
      <h1 class="panel__title">分块配置</h1>
      <p class="panel__desc">全局文本分块参数，应用于新上传文档及重新处理操作</p>
    </div>

    <div v-loading="configLoading" class="config-body">
      <!-- 参数配置 -->
      <section class="section">
        <h2 class="section__title">参数设置</h2>

        <div class="param-grid">
          <!-- chunk_size -->
          <div class="param-row">
            <div class="param-info">
              <span class="param-key mono">chunk_size（分段长度）</span>
              <span class="param-type mono text-muted">整数 · 字符数</span>
              <p class="param-desc">每个文本块的最大字符数。较小的值提高精确度，较大的值保留更多上下文。建议 300–800。</p>
            </div>
            <div class="param-input">
              <el-input-number
                v-model="form.chunkSize"
                :min="100"
                :max="2000"
                :step="50"
                size="default"
                style="width: 140px"
              />
              <span class="param-range mono text-muted">100 – 2000</span>
            </div>
          </div>

          <!-- overlap -->
          <div class="param-row">
            <div class="param-info">
              <span class="param-key mono">overlap（重叠长度）</span>
              <span class="param-type mono text-muted">整数 · 字符数</span>
              <p class="param-desc">相邻块之间的重叠字符数，防止语义在块边界截断。建议为 chunk_size 的 10%~20%。</p>
            </div>
            <div class="param-input">
              <el-input-number
                v-model="form.overlap"
                :min="0"
                :max="500"
                :step="10"
                size="default"
                style="width: 140px"
              />
              <span class="param-range mono text-muted">0 – 500</span>
            </div>
          </div>
        </div>

        <!-- 计算值 -->
        <div class="computed-block">
          <div class="computed-row">
            <span class="computed-key mono text-muted"># 有效步长</span>
            <span class="computed-val mono">= chunk_size - overlap = <strong>{{ effectiveStep }}</strong> 字符</span>
          </div>
          <div class="computed-row">
            <span class="computed-key mono text-muted"># 每千字符约产生</span>
            <span class="computed-val mono"><strong>{{ approxChunks }}</strong> 个 chunk</span>
          </div>
        </div>

        <div class="action-row">
          <button class="btn btn--primary" :disabled="saving" @click="saveConfig">
            {{ saving ? '保存中...' : '保存配置' }}
          </button>
          <span v-if="lastUpdated" class="saved-at mono text-muted">
            上次保存：{{ formatDate(lastUpdated) }}
          </span>
        </div>
      </section>

      <!-- 重新处理 -->
      <section class="section section--danger-zone">
        <h2 class="section__title">重新处理</h2>
        <div class="reprocess-row">
          <div class="reprocess-text">
            <p class="reprocess-desc">
              删除所有来源为 <code class="inline-code">pdf_upload</code> 的向量，
              并以当前配置重新对所有已索引文档进行分块和向量化。
            </p>
            <p class="reprocess-warn">⚠ 操作不可撤销，执行期间请勿关闭页面。</p>
          </div>
          <button class="btn btn--danger" :disabled="reprocessing" @click="handleReprocess">
            {{ reprocessing ? '处理中...' : '重新处理全部' }}
          </button>
        </div>
      </section>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getKnowledgeConfig, updateKnowledgeConfig, reprocessAll } from '@/services/knowledgeService'

const configLoading = ref(false)
const saving = ref(false)
const reprocessing = ref(false)
const lastUpdated = ref(null)
const form = ref({ chunkSize: 500, overlap: 50 })

const effectiveStep = computed(() => {
  const s = form.value.chunkSize - form.value.overlap
  return s > 0 ? s : form.value.chunkSize
})

const approxChunks = computed(() => {
  const step = form.value.chunkSize - form.value.overlap
  return step > 0 ? (1000 / step).toFixed(1) : '∞'
})

async function loadConfig() {
  configLoading.value = true
  try {
    const res = await getKnowledgeConfig()
    form.value.chunkSize = res.data.chunkSize
    form.value.overlap = res.data.overlap
    lastUpdated.value = res.data.updatedAt
  } catch { ElMessage.error('获取配置失败') }
  finally { configLoading.value = false }
}

async function saveConfig() {
  saving.value = true
  try {
    const res = await updateKnowledgeConfig(form.value)
    lastUpdated.value = res.data.updatedAt
    ElMessage.success('配置已保存')
  } catch (err) { ElMessage.error(err.response?.data?.message || '保存失败') }
  finally { saving.value = false }
}

async function handleReprocess() {
  await ElMessageBox.confirm(
    '将重新处理所有文档，原有向量数据会被替换，确认继续？',
    '重新处理',
    { type: 'warning', confirmButtonText: '确认执行', cancelButtonText: '取消' }
  )
  reprocessing.value = true
  try {
    await reprocessAll()
    ElMessage.success('全部文档重新处理完成')
  } catch (err) { ElMessage.error(err.response?.data?.message || '处理失败') }
  finally { reprocessing.value = false }
}

function formatDate(str) {
  if (!str) return ''
  const d = new Date(str)
  return `${d.getFullYear()}-${String(d.getMonth()+1).padStart(2,'0')}-${String(d.getDate()).padStart(2,'0')} ${String(d.getHours()).padStart(2,'0')}:${String(d.getMinutes()).padStart(2,'0')}`
}

onMounted(loadConfig)
</script>

<style scoped>
.panel { padding: 28px 32px; max-width: 800px; }

.panel__header { margin-bottom: 24px; }
.panel__title { font-size: 16px; font-weight: 600; color: #111827; margin: 0 0 2px; }
.panel__desc { font-size: 12px; color: #6b7280; margin: 0; }

.section {
  background: #fff;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  padding: 20px 24px;
  margin-bottom: 16px;
}

.section--danger-zone { border-color: #fecaca; }

.section__title {
  font-size: 11px;
  font-weight: 600;
  text-transform: uppercase;
  letter-spacing: 0.07em;
  color: #6b7280;
  margin: 0 0 16px;
}

.param-grid { display: flex; flex-direction: column; }

.param-row {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 24px;
  padding: 14px 0;
  border-bottom: 1px solid #f3f4f6;
}
.param-row:last-child { border-bottom: none; }

.param-info { flex: 1; }
.param-key { font-size: 13px; font-weight: 600; color: #111827; display: block; margin-bottom: 2px; }
.param-type { font-size: 11px; display: block; margin-bottom: 6px; }
.param-desc { font-size: 12px; color: #6b7280; margin: 0; line-height: 1.5; }

.param-input { display: flex; align-items: center; gap: 10px; padding-top: 2px; }
.param-range { font-size: 11px; white-space: nowrap; }

.computed-block {
  background: #f9fafb;
  border: 1px solid #e5e7eb;
  border-radius: 6px;
  padding: 10px 14px;
  margin-top: 14px;
  display: flex;
  flex-direction: column;
  gap: 4px;
}
.computed-row { display: flex; gap: 12px; align-items: center; }
.computed-key { font-size: 11px; min-width: 180px; }
.computed-val { font-size: 12px; color: #374151; }

.action-row {
  display: flex;
  align-items: center;
  gap: 14px;
  margin-top: 20px;
}
.saved-at { font-size: 11px; }

.reprocess-row {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 24px;
}
.reprocess-desc { font-size: 13px; color: #374151; margin: 0 0 6px; line-height: 1.5; }
.reprocess-warn { font-size: 12px; color: #b45309; margin: 0; }

.inline-code {
  font-family: 'JetBrains Mono', monospace;
  font-size: 12px;
  background: #f3f4f6;
  padding: 1px 5px;
  border-radius: 3px;
  color: #6366f1;
}

.btn {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 7px 16px;
  font-size: 13px;
  font-weight: 500;
  border-radius: 6px;
  border: 1px solid transparent;
  cursor: pointer;
  white-space: nowrap;
  transition: background 0.12s, opacity 0.12s;
  line-height: 1.4;
}
.btn:disabled { opacity: 0.5; cursor: not-allowed; }
.btn--primary { background: #6366f1; color: #fff; border-color: #6366f1; }
.btn--primary:hover:not(:disabled) { background: #4f46e5; }
.btn--danger { background: #fff; color: #ef4444; border-color: #fca5a5; }
.btn--danger:hover:not(:disabled) { background: #fef2f2; border-color: #ef4444; }

.mono { font-family: 'JetBrains Mono', 'Cascadia Code', 'Fira Code', monospace; }
.text-muted { color: #9ca3af; }
</style>
