<template>
  <div class="message-list" ref="listEl">
    <div v-if="messages.length === 0" class="message-list__welcome">
      <div class="welcome-card">
        <div class="welcome-card__icon">🩺</div>
        <h3 class="welcome-card__title">您好！我是 AI 辅助问诊助手</h3>
        <p class="welcome-card__desc">我会根据医学知识库为您提供参考建议，请注意本系统仅供参考，不能替代专业医生诊断。</p>
        <div class="welcome-card__tips">
          <div class="welcome-card__tip-title">为了给您更准确的回复，请尽量描述：</div>
          <ul>
            <li>🔹 <strong>主要症状</strong>（如头痛、发烧、咳嗽等）</li>
            <li>🔹 <strong>持续时间</strong>（几小时 / 几天）</li>
            <li>🔹 <strong>严重程度</strong>（轻微 / 明显 / 剧烈）</li>
            <li>🔹 <strong>伴随症状</strong>（如有）</li>
          </ul>
        </div>
      </div>
    </div>

    <MessageItem
      v-for="(msg, i) in messages"
      :key="i"
      :message="msg"
      :is-loading="isLastAiMsg(i) && aiTyping && msg.content === '' && !msg.thinking"
    />
  </div>
</template>

<script setup>
import { ref, watch, nextTick } from 'vue'
import MessageItem from './MessageItem.vue'

const props = defineProps({
  messages: {
    type: Array,
    default: () => []
  },
  aiTyping: {
    type: Boolean,
    default: false
  }
})

const listEl = ref(null)

function isLastAiMsg(i) {
  return i === props.messages.length - 1 && props.messages[i]?.role === 'assistant'
}

function scrollToBottom() {
  nextTick(() => {
    if (listEl.value) {
      listEl.value.scrollTop = listEl.value.scrollHeight
    }
  })
}

watch(() => props.messages.length, scrollToBottom)
watch(() => {
  const last = props.messages[props.messages.length - 1]
  return `${last?.thinking || ''}\n${last?.content || ''}`
}, scrollToBottom)
</script>

<style scoped>
.message-list {
  flex: 1;
  overflow-y: auto;
  padding: 24px 32px;
  display: flex;
  flex-direction: column;
}

.message-list__welcome {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 24px;
}

.welcome-card {
  max-width: 480px;
  width: 100%;
  background: #ffffff;
  border: 1px solid var(--color-border);
  border-radius: 16px;
  padding: 32px 28px;
  text-align: center;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.06);
}

.welcome-card__icon {
  font-size: 40px;
  margin-bottom: 12px;
}

.welcome-card__title {
  font-size: 17px;
  font-weight: 600;
  color: var(--color-text);
  margin: 0 0 10px;
}

.welcome-card__desc {
  font-size: 13px;
  color: var(--color-text-secondary);
  line-height: 1.6;
  margin: 0 0 20px;
}

.welcome-card__tips {
  background: #f8faff;
  border-radius: 10px;
  padding: 16px 20px;
  text-align: left;
}

.welcome-card__tip-title {
  font-size: 12px;
  font-weight: 600;
  color: var(--color-text-secondary);
  margin-bottom: 10px;
}

.welcome-card__tips ul {
  margin: 0;
  padding: 0;
  list-style: none;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.welcome-card__tips li {
  font-size: 13px;
  color: var(--color-text);
  line-height: 1.5;
}
</style>
