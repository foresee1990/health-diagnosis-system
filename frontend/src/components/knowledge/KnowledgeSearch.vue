<template>
  <div class="panel">
    <div class="panel__header">
      <h1 class="panel__title">检索测试</h1>
      <p class="panel__desc">向量相似度检索测试，验证知识库召回效果，点击结果查看高亮命中内容</p>
    </div>

    <!-- 查询输入区 -->
    <div class="query-box">
      <el-input
        v-model="query"
        type="textarea"
        :rows="3"
        placeholder="输入查询语句，例如：高血压有哪些常见症状？"
        resize="none"
        class="query-textarea"
        @keydown.ctrl.enter="handleSearch"
      />

      <!-- 参数栏 -->
      <div class="params-bar">
        <div class="param-item">
          <span class="param-label mono">Top-K</span>
          <div class="topk-group">
            <button
              v-for="k in [3, 5, 10]"
              :key="k"
              class="topk-btn"
              :class="{ 'topk-btn--active': topK === k }"
              @click="topK = k"
            >{{ k }}</button>
          </div>
        </div>

        <div class="param-item param-item--slider">
          <span class="param-label mono">相似度阈值</span>
          <el-slider
            v-model="similarityThreshold"
            :min="0" :max="1" :step="0.05"
            :format-tooltip="(v) => (v * 100).toFixed(0) + '%'"
            style="flex: 1; margin: 0 10px"
          />
          <span class="threshold-val mono">{{ (similarityThreshold * 100).toFixed(0) }}%</span>
        </div>

        <button
          class="run-btn"
          :disabled="searching || !query.trim()"
          @click="handleSearch"
        >
          {{ searching ? '检索中...' : '▶ 检索' }}
        </button>
      </div>
    </div>

    <!-- 结果统计 -->
    <div v-if="searched" class="results-meta">
      <span class="mono text-muted">
        共 {{ results.length }} 条结果 · Top-K={{ topK }} · 阈值={{ (similarityThreshold * 100).toFixed(0) }}%
      </span>
    </div>

    <!-- 结果列表 -->
    <div v-loading="searching" class="results">
      <div
        v-for="(item, idx) in results"
        :key="idx"
        class="result-card"
        :class="{ 'result-card--open': expandedIndex === idx }"
        @click="toggleExpand(idx)"
      >
        <!-- 结果头部 -->
        <div class="result-row">
          <span class="result-idx mono">[{{ idx }}]</span>

          <!-- 相似度分数条 -->
          <div class="score-wrap">
            <div class="score-bar-bg">
              <div
                class="score-bar-fill"
                :style="{ width: (item.score ?? 0) * 100 + '%', background: scoreColor(item.score) }"
              />
            </div>
            <span class="score-val mono" :style="{ color: scoreColor(item.score) }">
              {{ item.score != null ? item.score.toFixed(4) : 'n/a' }}
            </span>
          </div>

          <!-- 来源信息 -->
          <div class="result-source">
            <span v-if="item.docName" class="source-file mono">{{ item.docName }}</span>
            <span v-if="item.chunkIndex !== null" class="source-chunk mono text-muted">
              · chunk_{{ String(item.chunkIndex).padStart(3, '0') }}
            </span>
            <template v-if="item.tags">
              <span v-for="t in item.tags.split(',')" :key="t" class="tag">{{ t.trim() }}</span>
            </template>
          </div>

          <span class="expand-arrow" :class="{ 'expand-arrow--open': expandedIndex === idx }">›</span>
        </div>

        <!-- 折叠预览 -->
        <div class="result-preview mono">
          {{ item.content.slice(0, 120) }}{{ item.content.length > 120 ? '…' : '' }}
        </div>

        <!-- 展开：高亮命中全文 -->
        <transition name="slide">
          <div v-if="expandedIndex === idx" class="result-detail" @click.stop>
            <div class="detail-header">
              <span class="detail-label mono text-muted">命中原文 · {{ item.content.length }} 字符</span>
            </div>
            <pre class="detail-content" v-html="highlight(item.content, query)" />
          </div>
        </transition>
      </div>

      <div v-if="searched && results.length === 0" class="no-results mono text-muted">
        // 未找到高于阈值的结果，尝试降低相似度阈值或换一种表述方式
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { ElMessage } from 'element-plus'
import { searchKnowledge } from '@/services/knowledgeService'

const query = ref('')
const topK = ref(5)
const similarityThreshold = ref(0.5)
const results = ref([])
const searching = ref(false)
const searched = ref(false)
const expandedIndex = ref(null)

async function handleSearch() {
  if (!query.value.trim()) return
  searching.value = true
  searched.value = false
  results.value = []
  expandedIndex.value = null
  try {
    const res = await searchKnowledge({
      query: query.value.trim(),
      topK: topK.value,
      similarityThreshold: similarityThreshold.value
    })
    results.value = res.data || []
    searched.value = true
  } catch (err) {
    ElMessage.error(err.response?.data?.message || '检索失败')
  } finally {
    searching.value = false
  }
}

function toggleExpand(idx) {
  expandedIndex.value = expandedIndex.value === idx ? null : idx
}

/**
 * 高亮命中词。
 * 策略：
 * 1. 去除标点符号，按空格拆分为词组；
 * 2. 对于纯中文且长度 > 4 的词，额外提取所有 2-gram 子串作为候选词；
 * 3. 按词长降序排列，避免短词的 <mark> 嵌套到长词已标注的范围内；
 * 4. 只保留长度 ≥ 2 的候选词。
 */
function highlight(text, queryStr) {
  if (!queryStr || !text) return escapeHtml(text)

  const terms = getHighlightTerms(queryStr)
  if (terms.length === 0) return escapeHtml(text)

  // 逐词替换，先转义 HTML，再插入 <mark>
  let escaped = escapeHtml(text)
  const pattern = terms.map(escapeRegex).join('|')
  escaped = escaped.replace(new RegExp(`(${pattern})`, 'g'), '<mark>$1</mark>')
  return escaped
}

function getHighlightTerms(queryStr) {
  // 去除中英文标点，保留汉字、字母、数字
  const cleaned = queryStr.replace(/[^\u4e00-\u9fffa-zA-Z0-9]/g, ' ')
  const segments = cleaned.split(/\s+/).filter(s => s.length >= 2)

  const candidates = new Set(segments)

  // 纯中文长词：额外提取 2-gram，增强召回匹配
  segments.forEach(seg => {
    if (/^[\u4e00-\u9fff]+$/.test(seg) && seg.length > 3) {
      for (let i = 0; i < seg.length - 1; i++) {
        candidates.add(seg.slice(i, i + 2))
      }
    }
  })

  // 按词长降序，避免短词嵌套匹配
  return [...candidates]
    .filter(t => t.length >= 2)
    .sort((a, b) => b.length - a.length)
}

function escapeHtml(s) {
  return s
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
}

function escapeRegex(s) {
  return s.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')
}

function scoreColor(score) {
  if (score == null) return '#9ca3af'
  if (score >= 0.8) return '#10b981'
  if (score >= 0.6) return '#f59e0b'
  return '#ef4444'
}
</script>

<style scoped>
.panel { padding: 28px 32px; max-width: 920px; }

.panel__header { margin-bottom: 20px; }
.panel__title { font-size: 16px; font-weight: 600; color: #111827; margin: 0 0 2px; }
.panel__desc { font-size: 12px; color: #6b7280; margin: 0; }

/* 查询框 */
.query-box {
  background: #fff;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  overflow: hidden;
  margin-bottom: 16px;
}

.query-box :deep(.el-textarea__inner) {
  border: none;
  border-radius: 0;
  box-shadow: none !important;
  font-size: 13px;
  padding: 12px 14px;
  background: #fff;
  resize: none;
}

.params-bar {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 8px 14px;
  background: #f9fafb;
  border-top: 1px solid #e5e7eb;
  flex-wrap: wrap;
}

.param-item {
  display: flex;
  align-items: center;
  gap: 8px;
}

.param-item--slider { flex: 1; min-width: 200px; }

.param-label { font-size: 11px; color: #9ca3af; white-space: nowrap; }

/* Top-K 按钮组 */
.topk-group { display: flex; }

.topk-btn {
  width: 30px;
  height: 24px;
  font-size: 12px;
  font-family: 'JetBrains Mono', monospace;
  font-weight: 500;
  border: 1px solid #e5e7eb;
  background: #fff;
  cursor: pointer;
  color: #6b7280;
  transition: all 0.1s;
}
.topk-btn:first-child { border-radius: 4px 0 0 4px; }
.topk-btn:last-child  { border-radius: 0 4px 4px 0; }
.topk-btn + .topk-btn { border-left: none; }
.topk-btn:hover { background: #f3f4f6; color: #111827; }
.topk-btn--active { background: #6366f1; border-color: #6366f1; color: #fff; }

.threshold-val { font-size: 12px; font-weight: 600; color: #6366f1; min-width: 32px; text-align: right; }

/* 检索按钮 */
.run-btn {
  padding: 5px 14px;
  font-size: 13px;
  font-weight: 600;
  background: #111827;
  color: #fff;
  border: none;
  border-radius: 5px;
  cursor: pointer;
  transition: background 0.1s;
  white-space: nowrap;
}
.run-btn:hover:not(:disabled) { background: #374151; }
.run-btn:disabled { opacity: 0.4; cursor: not-allowed; }

/* 结果统计 */
.results-meta { font-size: 12px; margin-bottom: 10px; }

/* 结果卡片 */
.result-card {
  background: #fff;
  border: 1px solid #e5e7eb;
  border-radius: 6px;
  margin-bottom: 6px;
  cursor: pointer;
  transition: border-color 0.12s;
  overflow: hidden;
}
.result-card:hover { border-color: #a5b4fc; }
.result-card--open { border-color: #6366f1; }

.result-row {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 10px 14px 4px;
}

.result-idx { font-size: 11px; color: #9ca3af; min-width: 24px; }

/* 分数条 */
.score-wrap { display: flex; align-items: center; gap: 8px; flex-shrink: 0; }

.score-bar-bg {
  width: 80px;
  height: 4px;
  background: #f3f4f6;
  border-radius: 2px;
  overflow: hidden;
}

.score-bar-fill { height: 100%; border-radius: 2px; transition: width 0.3s; }

.score-val { font-size: 12px; font-weight: 600; min-width: 52px; }

/* 来源信息 */
.result-source {
  display: flex;
  align-items: center;
  gap: 4px;
  flex: 1;
  overflow: hidden;
}

.source-file {
  font-size: 12px;
  color: #374151;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  max-width: 240px;
}

.source-chunk { font-size: 11px; white-space: nowrap; }

.tag {
  font-size: 10px;
  padding: 1px 6px;
  background: #f3f4f6;
  border: 1px solid #e5e7eb;
  border-radius: 3px;
  color: #6b7280;
  white-space: nowrap;
}

/* 展开箭头 */
.expand-arrow {
  font-size: 18px;
  color: #9ca3af;
  transition: transform 0.2s;
  display: inline-block;
  line-height: 1;
  flex-shrink: 0;
}
.expand-arrow--open { transform: rotate(90deg); color: #6366f1; }

/* 折叠预览 */
.result-preview {
  font-size: 11px;
  color: #9ca3af;
  padding: 2px 14px 10px;
  line-height: 1.5;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

/* 展开详情 */
.result-detail { border-top: 1px solid #f3f4f6; }

.detail-header {
  padding: 8px 14px 4px;
  background: #f9fafb;
  border-bottom: 1px solid #f3f4f6;
}

.detail-label { font-size: 11px; }

.detail-content {
  margin: 0;
  padding: 14px;
  font-size: 12px;
  line-height: 1.8;
  color: #374151;
  font-family: 'JetBrains Mono', 'Cascadia Code', monospace;
  white-space: pre-wrap;
  word-break: break-word;
  background: #fff;
}

.detail-content :deep(mark) {
  background: #fef9c3;
  color: #92400e;
  border-radius: 2px;
  padding: 0 1px;
  font-style: normal;
}

/* 无结果 */
.no-results { padding: 32px 0; font-size: 12px; }

/* 动画 */
.slide-enter-active, .slide-leave-active {
  transition: opacity 0.15s, transform 0.15s;
  transform-origin: top;
}
.slide-enter-from, .slide-leave-to { opacity: 0; transform: scaleY(0.97); }

.mono { font-family: 'JetBrains Mono', 'Cascadia Code', 'Fira Code', monospace; }
.text-muted { color: #9ca3af; }
</style>
