<template>
  <div class="auth-page">
    <div class="auth-card">
      <div class="auth-card__logo">
        <svg viewBox="0 0 40 40" fill="none" xmlns="http://www.w3.org/2000/svg">
          <rect width="40" height="40" rx="10" fill="#2F80ED"/>
          <path d="M20 10v20M10 20h20" stroke="white" stroke-width="3.5" stroke-linecap="round"/>
        </svg>
      </div>
      <h1 class="auth-card__title">健康 AI 问诊</h1>
      <p class="auth-card__subtitle">登录您的账户</p>

      <el-form
        ref="formRef"
        :model="form"
        :rules="rules"
        label-position="top"
        class="auth-form"
        @submit.prevent="handleLogin"
      >
        <el-form-item label="用户名" prop="username">
          <el-input
            v-model="form.username"
            placeholder="请输入用户名"
            size="large"
            :prefix-icon="User"
          />
        </el-form-item>

        <el-form-item label="密码" prop="password">
          <el-input
            v-model="form.password"
            type="password"
            placeholder="请输入密码"
            size="large"
            :prefix-icon="Lock"
            show-password
            @keyup.enter="handleLogin"
          />
        </el-form-item>

        <el-button
          type="primary"
          size="large"
          class="auth-form__submit"
          :loading="loading"
          @click="handleLogin"
        >
          登录
        </el-button>
      </el-form>

      <p class="auth-card__footer">
        还没有账户？
        <RouterLink to="/register">立即注册</RouterLink>
      </p>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { ElMessage } from 'element-plus'
import { User, Lock } from '@element-plus/icons-vue'

const router = useRouter()
const authStore = useAuthStore()

const formRef = ref(null)
const loading = ref(false)
const form = ref({ username: '', password: '' })

const rules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }]
}

async function handleLogin() {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return

  loading.value = true
  try {
    await authStore.login(form.value.username, form.value.password)
    router.push('/chat')
  } catch (e) {
    const msg = e.response?.data?.message || '登录失败，请检查您的账号和密码。'
    ElMessage.error(msg)
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.auth-page {
  min-height: 100vh;
  background: var(--color-bg);
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 24px;
}

.auth-card {
  background: #ffffff;
  border-radius: var(--radius-card);
  box-shadow: 0 4px 24px rgba(0, 0, 0, 0.08);
  padding: 48px 40px;
  width: 100%;
  max-width: 400px;
  text-align: center;
}

.auth-card__logo {
  display: inline-block;
  margin-bottom: 16px;
}

.auth-card__logo svg {
  width: 48px;
  height: 48px;
}

.auth-card__title {
  font-size: 20px;
  font-weight: 600;
  color: var(--color-text);
  margin-bottom: 6px;
}

.auth-card__subtitle {
  font-size: 14px;
  color: var(--color-text-secondary);
  margin-bottom: 32px;
}

.auth-form {
  text-align: left;
}

.auth-form__submit {
  width: 100%;
  margin-top: 8px;
  border-radius: var(--radius-btn);
}

.auth-card__footer {
  margin-top: 24px;
  font-size: 14px;
  color: var(--color-text-secondary);
}
</style>
