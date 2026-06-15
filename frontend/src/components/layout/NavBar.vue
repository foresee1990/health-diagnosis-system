<template>
  <header class="navbar">
    <div class="navbar__brand">
      <svg class="navbar__logo" viewBox="0 0 32 32" fill="none" xmlns="http://www.w3.org/2000/svg">
        <rect width="32" height="32" rx="8" fill="#2F80ED"/>
        <path d="M16 8v16M8 16h16" stroke="white" stroke-width="3" stroke-linecap="round"/>
      </svg>
      <span class="navbar__title">健康 AI 问诊</span>
    </div>

    <div class="navbar__right">
      <el-dropdown trigger="click" @command="handleCommand">
        <div class="navbar__user">
          <el-avatar :size="36" :style="{ background: '#2F80ED', cursor: 'pointer' }">
            {{ avatarLetter }}
          </el-avatar>
          <span class="navbar__username">{{ username }}</span>
        </div>
        <template #dropdown>
          <el-dropdown-menu>
            <el-dropdown-item command="profile" :icon="User">
              个人中心
            </el-dropdown-item>
            <el-dropdown-item v-if="authStore.isAdmin" command="admin" :icon="Setting">
              管理后台
            </el-dropdown-item>
            <el-dropdown-item command="logout" :icon="SwitchButton" divided>
              退出登录
            </el-dropdown-item>
          </el-dropdown-menu>
        </template>
      </el-dropdown>
    </div>
  </header>
</template>

<script setup>
import { computed } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { User, SwitchButton, Setting } from '@element-plus/icons-vue'

const router = useRouter()
const authStore = useAuthStore()

const username = computed(() => authStore.username || 'User')
const avatarLetter = computed(() => (authStore.username || 'U')[0].toUpperCase())

function handleCommand(cmd) {
  if (cmd === 'logout') {
    authStore.logout()
    router.push('/login')
  } else if (cmd === 'profile') {
    router.push('/profile')
  } else if (cmd === 'admin') {
    router.push('/admin')
  }
}
</script>

<style scoped>
.navbar {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  height: var(--navbar-height);
  background: #ffffff;
  border-bottom: 1px solid var(--color-border);
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.06);
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 24px;
  z-index: 100;
}

.navbar__brand {
  display: flex;
  align-items: center;
  gap: 10px;
}

.navbar__logo {
  width: 32px;
  height: 32px;
  flex-shrink: 0;
}

.navbar__title {
  font-size: 17px;
  font-weight: 600;
  color: var(--color-text);
}

.navbar__right {
  display: flex;
  align-items: center;
}

.navbar__user {
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
}

.navbar__username {
  font-size: 14px;
  font-weight: 500;
  color: var(--color-text);
}
</style>
