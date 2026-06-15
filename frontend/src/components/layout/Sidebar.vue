<template>
  <aside class="sidebar">
    <div class="sidebar__top">
      <el-button
        type="primary"
        class="sidebar__new-btn"
        :icon="Plus"
        :loading="creating"
        @click="handleNewConsultation"
      >
        新建问诊
      </el-button>
    </div>

    <div class="sidebar__section-title">最近</div>

    <div class="sidebar__list" v-if="consultationStore.list.length">
      <div
        v-for="item in consultationStore.list"
        :key="item.id"
        class="sidebar__item"
        :class="{ 'sidebar__item--active': item.id === consultationStore.currentId }"
        @click="openConsultation(item.id)"
      >
        <div class="sidebar__item-main">
          <div class="sidebar__item-title">{{ item.chiefComplaint || '问诊 #' + item.id }}</div>
          <div class="sidebar__item-meta">
            <span class="sidebar__item-status" :class="'status--' + item.status">{{ item.status === 'ongoing' ? '进行中' : item.status === 'completed' ? '已完成' : item.status }}</span>
            <span class="sidebar__item-date">{{ formatDate(item.createdAt) }}</span>
          </div>
        </div>

        <el-dropdown
          trigger="click"
          @command="(cmd) => handleMenuCommand(cmd, item)"
          @click.stop
        >
          <span class="sidebar__item-more" @click.stop>
            <el-icon><MoreFilled /></el-icon>
          </span>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item command="rename">重命名</el-dropdown-item>
              <el-dropdown-item command="delete" style="color: #f56c6c">删除</el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
      </div>
    </div>

    <div class="sidebar__empty" v-else>
      <p>暂无问诊记录。</p>
    </div>

    <!-- Knowledge entry — visible to KNOWLEDGE_ENGINEER and ADMIN -->
    <div v-if="authStore.canAccessKnowledge" class="sidebar__admin-entry" @click="router.push('/knowledge')">
      <el-icon><Files /></el-icon>
      <span>知识库管理</span>
    </div>

    <!-- Admin entry — only visible to ADMIN -->
    <div v-if="authStore.isAdmin" class="sidebar__admin-entry" @click="router.push('/admin')">
      <el-icon><Setting /></el-icon>
      <span>管理后台</span>
    </div>

    <!-- Rename Dialog -->
    <el-dialog v-model="renameDialog.visible" title="重命名" width="360px" :close-on-click-modal="false">
      <el-input v-model="renameDialog.value" placeholder="输入新名称" maxlength="50" show-word-limit @keyup.enter="confirmRename" />
      <template #footer>
        <el-button @click="renameDialog.visible = false">取消</el-button>
        <el-button type="primary" :disabled="!renameDialog.value.trim()" @click="confirmRename">确认</el-button>
      </template>
    </el-dialog>
  </aside>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { useConsultationStore } from '@/stores/consultation'
import { useAuthStore } from '@/stores/auth'
import { Plus, Setting, Files, MoreFilled } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getHealthProfile } from '@/services/authService'

const router = useRouter()
const consultationStore = useConsultationStore()
const authStore = useAuthStore()

const creating = ref(false)

const renameDialog = reactive({ visible: false, id: null, value: '' })

async function handleNewConsultation() {
  try {
    const res = await getHealthProfile()
    if (!res.data?.filled) {
      await ElMessageBox.confirm(
        '填写健康档案后，AI 可以提供更个性化的问诊建议。是否现在前往填写？',
        '完善健康档案',
        { confirmButtonText: '去填写', cancelButtonText: '跳过', type: 'info', distinguishCancelAndClose: true }
      )
      router.push('/profile')
      return
    }
  } catch (action) {
    if (action === 'close') return
  }

  creating.value = true
  try {
    const id = await consultationStore.createConsultation('')
    router.push(`/chat/${id}`)
  } catch {
    ElMessage.error('创建失败，请重试。')
  } finally {
    creating.value = false
  }
}

function openConsultation(id) {
  router.push(`/chat/${id}`)
}

async function handleMenuCommand(cmd, item) {
  if (cmd === 'rename') {
    renameDialog.id = item.id
    renameDialog.value = item.chiefComplaint || ''
    renameDialog.visible = true
  } else if (cmd === 'delete') {
    try {
      await ElMessageBox.confirm('确定删除该问诊记录吗？此操作不可恢复。', '删除确认', {
        confirmButtonText: '删除',
        cancelButtonText: '取消',
        type: 'warning',
        confirmButtonClass: 'el-button--danger'
      })
      await consultationStore.deleteConsultation(item.id)
      if (consultationStore.currentId === null) router.push('/chat')
      ElMessage.success('已删除')
    } catch {
      // 取消，无需处理
    }
  }
}

async function confirmRename() {
  const title = renameDialog.value.trim()
  if (!title) return
  try {
    await consultationStore.renameConsultation(renameDialog.id, title)
    renameDialog.visible = false
    ElMessage.success('已重命名')
  } catch {
    ElMessage.error('重命名失败')
  }
}

function formatDate(dateStr) {
  if (!dateStr) return ''
  const d = new Date(dateStr)
  return d.toLocaleDateString('zh-CN', { month: 'short', day: 'numeric' })
}
</script>

<style scoped>
.sidebar {
  width: var(--sidebar-width);
  height: 100%;
  background: #ffffff;
  border-right: 1px solid var(--color-border);
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.sidebar__top {
  padding: 16px 12px 8px;
}

.sidebar__new-btn {
  width: 100%;
  border-radius: var(--radius-btn);
}

.sidebar__section-title {
  padding: 12px 16px 4px;
  font-size: 11px;
  font-weight: 600;
  text-transform: uppercase;
  letter-spacing: 0.08em;
  color: var(--color-text-secondary);
}

.sidebar__list {
  flex: 1;
  overflow-y: auto;
  padding: 4px 8px;
}

.sidebar__item {
  display: flex;
  align-items: center;
  gap: 4px;
  padding: 10px 8px 10px 12px;
  border-radius: 8px;
  cursor: pointer;
  transition: background 0.15s;
  margin-bottom: 2px;
}

.sidebar__item:hover {
  background: var(--color-surface);
}

.sidebar__item--active {
  background: #EBF4FF;
}

.sidebar__item--active .sidebar__item-title {
  color: var(--color-primary);
}

.sidebar__item-main {
  flex: 1;
  min-width: 0;
}

.sidebar__item-title {
  font-size: 13px;
  font-weight: 500;
  color: var(--color-text);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  margin-bottom: 4px;
}

.sidebar__item-meta {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.sidebar__item-status {
  font-size: 11px;
  font-weight: 500;
  text-transform: capitalize;
  border-radius: 4px;
  padding: 1px 6px;
}

.status--ongoing {
  background: #FEF3C7;
  color: #D97706;
}

.status--completed {
  background: #D1FAE5;
  color: #059669;
}

.sidebar__item-date {
  font-size: 11px;
  color: var(--color-text-secondary);
}

.sidebar__item-more {
  flex-shrink: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  width: 24px;
  height: 24px;
  border-radius: 4px;
  color: var(--color-text-secondary);
  opacity: 0;
  transition: opacity 0.15s, background 0.15s;
}

.sidebar__item:hover .sidebar__item-more {
  opacity: 1;
}

.sidebar__item-more:hover {
  background: rgba(0, 0, 0, 0.06);
  color: var(--color-text);
}

.sidebar__empty {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--color-text-secondary);
  font-size: 13px;
}

.sidebar__admin-entry {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 12px 16px;
  margin: 4px 8px;
  border-radius: 8px;
  cursor: pointer;
  font-size: 13px;
  font-weight: 500;
  color: var(--color-text-secondary);
  border-top: 1px solid var(--color-border);
  transition: background 0.15s, color 0.15s;
}

.sidebar__admin-entry:hover {
  background: var(--color-surface);
  color: var(--color-primary);
}
</style>
