<template>
  <div class="message" :class="isUser ? 'message--user' : 'message--ai'">
    <div v-if="!isUser" class="message__avatar">
      <svg viewBox="0 0 28 28" fill="none" xmlns="http://www.w3.org/2000/svg">
        <rect width="28" height="28" rx="7" fill="#2F80ED"/>
        <path d="M14 7v14M7 14h14" stroke="white" stroke-width="2.5" stroke-linecap="round"/>
      </svg>
    </div>
    <div class="message__bubble">
      <LoadingDots v-if="isLoading" />
      <template v-else-if="!isUser">
        <details v-if="showThinking" class="message__thinking">
          <summary class="message__thinking-summary">
            思考过程
            <span class="message__thinking-hint">{{ thinkingSummary }}</span>
          </summary>
          <div class="message__thinking-content" v-html="renderedThinking" />
        </details>
        <div v-if="hasAnswer" class="message__markdown" v-html="renderedAnswer" />
        <div v-else-if="showThinking" class="message__thinking-waiting">正在整理最终回答...</div>
      </template>
      <span v-else class="message__text">{{ message.content }}</span>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { marked } from 'marked'
import LoadingDots from '@/components/common/LoadingDots.vue'

marked.use({ gfm: true, breaks: true })

const THINK_TAG_REGEX = /<think>([\s\S]*?)<\/think>/gi
const THINKING_PREVIEW_LIMIT = 80

const props = defineProps({
  message: {
    type: Object,
    required: true
  },
  isLoading: {
    type: Boolean,
    default: false
  }
})

const isUser = computed(() => props.message.role === 'user')

function extractSegments(message) {
  const rawContent = typeof message?.content === 'string' ? message.content : ''
  const explicitThinking = typeof message?.thinking === 'string' ? message.thinking : ''
  const thinkingParts = []

  const answer = rawContent.replace(THINK_TAG_REGEX, (_, thinkContent) => {
    if (thinkContent?.trim()) {
      thinkingParts.push(thinkContent.trim())
    }
    return ''
  }).trim()

  if (explicitThinking.trim()) {
    thinkingParts.unshift(explicitThinking.trim())
  }

  const mergedThinking = thinkingParts.join('\n\n').trim()

  return {
    thinking: mergedThinking,
    answer
  }
}

const segments = computed(() => extractSegments(props.message))
const showThinking = computed(() => Boolean(segments.value.thinking))
const hasAnswer = computed(() => Boolean(segments.value.answer))

const renderedThinking = computed(() => {
  if (!segments.value.thinking) return ''
  return marked.parse(segments.value.thinking)
})

const renderedAnswer = computed(() => {
  if (!segments.value.answer) return ''
  return marked.parse(segments.value.answer)
})

const thinkingSummary = computed(() => {
  const text = segments.value.thinking.replace(/\s+/g, ' ').trim()
  if (!text) return ''
  if (text.length <= THINKING_PREVIEW_LIMIT) {
    return text
  }
  return `${text.slice(0, THINKING_PREVIEW_LIMIT)}...`
})
</script>

<style scoped>
.message {
  display: flex;
  align-items: flex-end;
  gap: 8px;
  margin-bottom: 16px;
}

.message--user {
  flex-direction: row-reverse;
}

.message__avatar {
  width: 28px;
  height: 28px;
  flex-shrink: 0;
}

.message__avatar svg {
  width: 100%;
  height: 100%;
}

.message__bubble {
  max-width: 65%;
  padding: 12px 16px;
  border-radius: var(--radius-bubble);
  font-size: 14px;
  line-height: 1.7;
  word-break: break-word;
}

.message--user .message__bubble {
  background: var(--color-user-bubble);
  color: var(--color-text);
  border-bottom-right-radius: 4px;
}

.message--ai .message__bubble {
  background: #ffffff;
  border: 1px solid var(--color-border);
  color: var(--color-text);
  border-bottom-left-radius: 4px;
}

.message__text {
  white-space: pre-wrap;
}

.message__thinking {
  margin-bottom: 12px;
  border: 1px solid #dbe4f0;
  background: #f7fafc;
  border-radius: 12px;
  overflow: hidden;
}

.message__thinking-summary {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 10px 12px;
  cursor: pointer;
  font-weight: 600;
  color: #355070;
  list-style: none;
}

.message__thinking-summary::-webkit-details-marker {
  display: none;
}

.message__thinking-hint {
  flex: 1;
  color: #6b7a90;
  font-weight: 400;
  font-size: 12px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.message__thinking-content {
  padding: 0 12px 12px;
  color: #516072;
  font-size: 13px;
}

.message__thinking-waiting {
  color: #6b7a90;
  font-size: 13px;
}

.message__markdown :deep(p),
.message__thinking-content :deep(p) {
  margin: 0 0 8px;
}

.message__markdown :deep(p:last-child),
.message__thinking-content :deep(p:last-child) {
  margin-bottom: 0;
}

.message__markdown :deep(strong),
.message__thinking-content :deep(strong) {
  font-weight: 600;
  color: #1a1a1a;
}

.message__markdown :deep(ul),
.message__markdown :deep(ol),
.message__thinking-content :deep(ul),
.message__thinking-content :deep(ol) {
  margin: 6px 0 8px;
  padding-left: 20px;
}

.message__markdown :deep(li),
.message__thinking-content :deep(li) {
  margin-bottom: 4px;
}

.message__markdown :deep(h3),
.message__markdown :deep(h4),
.message__thinking-content :deep(h3),
.message__thinking-content :deep(h4) {
  font-size: 14px;
  font-weight: 600;
  margin: 10px 0 4px;
}

.message__markdown :deep(code),
.message__thinking-content :deep(code) {
  background: #f3f4f6;
  padding: 1px 5px;
  border-radius: 4px;
  font-size: 13px;
}
</style>
