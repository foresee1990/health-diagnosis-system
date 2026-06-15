<template>
  <div class="chat-layout">
    <NavBar />

    <div class="chat-layout__body">
      <Sidebar />

      <main class="chat-main">
        <template v-if="consultationStore.currentId">
          <ChatHeader
            :title="currentTitle"
            :status="consultationStore.currentStatus"
            :risk-level="consultationStore.currentRiskLevel"
            :consultation-id="consultationStore.currentId"
            @complete="handleComplete"
          />
          <MessageList
            :messages="consultationStore.messages"
            :ai-typing="consultationStore.aiTyping"
          />
          <ChatInput
            :disabled="consultationStore.aiTyping || consultationStore.currentStatus === 'completed'"
            @send="handleSend"
          />
        </template>

        <div v-else class="chat-main__welcome">
          <div class="chat-main__welcome-icon">
            <svg viewBox="0 0 64 64" fill="none" xmlns="http://www.w3.org/2000/svg">
              <rect width="64" height="64" rx="16" fill="#EBF4FF"/>
              <path d="M32 16v32M16 32h32" stroke="#2F80ED" stroke-width="5" stroke-linecap="round"/>
            </svg>
          </div>
          <h2>欢迎使用健康 AI 问诊</h2>
          <p>请从左侧边栏选择历史问诊，或新建一次问诊。</p>
        </div>
      </main>
    </div>
  </div>
</template>

<script setup>
import { computed, watch, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import NavBar from '@/components/layout/NavBar.vue'
import Sidebar from '@/components/layout/Sidebar.vue'
import ChatHeader from '@/components/chat/ChatHeader.vue'
import MessageList from '@/components/chat/MessageList.vue'
import ChatInput from '@/components/chat/ChatInput.vue'
import { useConsultationStore } from '@/stores/consultation'

const route = useRoute()
const consultationStore = useConsultationStore()

const currentTitle = computed(() => {
  const item = consultationStore.list.find(c => c.id === consultationStore.currentId)
  return item?.chiefComplaint || 'AI 健康问诊'
})

onMounted(async () => {
  await consultationStore.fetchList()
  const id = Number(route.params.consultationId)
  if (id) {
    await consultationStore.openConsultation(id)
  }
})

watch(() => route.params.consultationId, async (id) => {
  if (id) {
    await consultationStore.openConsultation(Number(id))
  } else {
    consultationStore.currentId = null
    consultationStore.messages = []
    consultationStore.currentStatus = null
    consultationStore.currentRiskLevel = null
  }
})

async function handleSend(content) {
  try {
    await consultationStore.sendMessage(content)
  } catch {
    ElMessage.error('发送消息失败。')
  }
}

async function handleComplete() {
  try {
    await consultationStore.completeConsultation()
    ElMessage.success('问诊已结束。')
  } catch {
    ElMessage.error('结束问诊失败。')
  }
}
</script>

<style scoped>
.chat-layout {
  display: flex;
  flex-direction: column;
  height: 100vh;
  min-width: 1280px;
}

.chat-layout__body {
  display: flex;
  flex: 1;
  overflow: hidden;
  margin-top: var(--navbar-height);
}

.chat-main {
  flex: 1;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  background: var(--color-bg);
}

.chat-main__welcome {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  text-align: center;
  gap: 16px;
  color: var(--color-text-secondary);
}

.chat-main__welcome-icon svg {
  width: 80px;
  height: 80px;
}

.chat-main__welcome h2 {
  font-size: 20px;
  font-weight: 600;
  color: var(--color-text);
}

.chat-main__welcome p {
  font-size: 14px;
  max-width: 360px;
}
</style>
