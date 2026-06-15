<template>
  <div class="kb-page">
    <NavBar />

    <div class="kb-layout">
      <!-- 侧边导航 -->
      <aside class="kb-sidebar">
        <div class="kb-sidebar__header">
          <span class="kb-sidebar__logo">⬡</span>
          <span class="kb-sidebar__title">知识库管理</span>
        </div>

        <nav class="kb-nav">
          <div
            v-for="item in navItems"
            :key="item.key"
            class="kb-nav__item"
            :class="{ 'kb-nav__item--active': activePanel === item.key }"
            @click="activePanel = item.key"
          >
            <el-icon class="kb-nav__icon"><component :is="item.icon" /></el-icon>
            <span class="kb-nav__label">{{ item.label }}</span>
          </div>
        </nav>

        <div class="kb-sidebar__footer">
          <div class="kb-nav__item kb-nav__item--back" @click="router.push('/chat')">
            <el-icon class="kb-nav__icon"><ArrowLeft /></el-icon>
            <span class="kb-nav__label">返回问诊</span>
          </div>
        </div>
      </aside>

      <!-- 主内容区 -->
      <main class="kb-main">
        <component :is="currentComponent" />
      </main>
    </div>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'
import { useRouter } from 'vue-router'
import { FolderOpened, Setting, Search, ArrowLeft } from '@element-plus/icons-vue'
import NavBar from '@/components/layout/NavBar.vue'
import KnowledgeDocuments from '@/components/knowledge/KnowledgeDocuments.vue'
import KnowledgeConfig from '@/components/knowledge/KnowledgeConfig.vue'
import KnowledgeSearch from '@/components/knowledge/KnowledgeSearch.vue'

const router = useRouter()
const activePanel = ref('documents')

const navItems = [
  { key: 'documents', label: '医疗资料管理', icon: FolderOpened },
  { key: 'config',    label: '分块配置',     icon: Setting },
  { key: 'search',    label: '检索测试',     icon: Search },
]

const componentMap = { documents: KnowledgeDocuments, config: KnowledgeConfig, search: KnowledgeSearch }
const currentComponent = computed(() => componentMap[activePanel.value])
</script>

<style scoped>
.kb-page {
  display: flex;
  flex-direction: column;
  height: 100vh;
  padding-top: var(--navbar-height);
  overflow: hidden;
  background: #f9fafb;
}

.kb-layout {
  display: flex;
  flex: 1;
  overflow: hidden;
}

.kb-sidebar {
  width: 200px;
  background: #ffffff;
  border-right: 1px solid #e5e7eb;
  display: flex;
  flex-direction: column;
  flex-shrink: 0;
}

.kb-sidebar__header {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 16px 16px 12px;
  border-bottom: 1px solid #e5e7eb;
}

.kb-sidebar__logo { font-size: 16px; color: #6366f1; line-height: 1; }

.kb-sidebar__title {
  font-size: 13px;
  font-weight: 600;
  color: #111827;
}

.kb-nav { flex: 1; padding: 8px 0; }

.kb-nav__item {
  display: flex;
  align-items: center;
  gap: 9px;
  padding: 8px 16px;
  cursor: pointer;
  border-left: 2px solid transparent;
  transition: background 0.12s, border-color 0.12s, color 0.12s;
  color: #6b7280;
  font-size: 13px;
  font-weight: 400;
  user-select: none;
}

.kb-nav__item:hover { background: #f3f4f6; color: #111827; }

.kb-nav__item--active {
  border-left-color: #6366f1;
  background: #f5f3ff;
  color: #4f46e5;
  font-weight: 500;
}

.kb-nav__icon { font-size: 14px; flex-shrink: 0; }

.kb-sidebar__footer { border-top: 1px solid #e5e7eb; padding: 8px 0; }

.kb-nav__item--back { color: #9ca3af; font-size: 12px; }

.kb-main { flex: 1; overflow-y: auto; background: #f9fafb; }
</style>
