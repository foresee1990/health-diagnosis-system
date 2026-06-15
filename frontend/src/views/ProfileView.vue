<template>
  <div class="profile-page">
    <div class="profile-container">
      <!-- Back button -->
      <div class="profile-back">
        <el-button :icon="ArrowLeft" @click="router.push('/chat')">返回问诊</el-button>
      </div>

      <!-- Header -->
      <div class="profile-header">
        <el-avatar :size="64" :style="{ background: '#2F80ED', fontSize: '24px' }">
          {{ avatarLetter }}
        </el-avatar>
        <div class="profile-header__info">
          <h2 class="profile-header__name">{{ authStore.username }}</h2>
          <span class="profile-header__role" :class="'role--' + (authStore.role || 'USER').toLowerCase()">
            {{ authStore.role === 'ADMIN' ? '管理员' : '普通用户' }}
          </span>
        </div>
      </div>

      <!-- Basic Info Card -->
      <el-card class="profile-card" shadow="never">
        <template #header>
          <span class="card-title">基本信息</span>
        </template>
        <el-descriptions :column="1" border>
          <el-descriptions-item label="用户名">{{ authStore.username }}</el-descriptions-item>
          <el-descriptions-item label="邮箱">{{ currentEmail || '未设置' }}</el-descriptions-item>
          <el-descriptions-item label="角色">{{ authStore.role === 'ADMIN' ? '管理员' : '普通用户' }}</el-descriptions-item>
        </el-descriptions>
      </el-card>

      <!-- Update Email Card -->
      <el-card class="profile-card" shadow="never">
        <template #header>
          <span class="card-title">编辑资料</span>
        </template>
        <el-form
          ref="profileFormRef"
          :model="profileForm"
          :rules="profileRules"
          label-width="80px"
          @submit.prevent="submitProfile"
        >
          <el-form-item label="邮箱" prop="email">
            <el-input v-model="profileForm.email" placeholder="请输入新邮箱" />
          </el-form-item>
          <el-form-item>
            <el-button type="primary" :loading="profileLoading" @click="submitProfile">
              保存
            </el-button>
          </el-form-item>
        </el-form>
      </el-card>

      <!-- Health Profile Card -->
      <el-card class="profile-card" shadow="never">
        <template #header>
          <div class="card-header">
            <span class="card-title">健康档案</span>
            <el-tag v-if="healthProfile.filled" type="success" size="small">已填写</el-tag>
            <el-tag v-else type="info" size="small">未填写</el-tag>
          </div>
        </template>
        <el-form
          ref="healthFormRef"
          :model="healthForm"
          :rules="healthRules"
          label-width="100px"
          @submit.prevent="submitHealth"
        >
          <el-form-item label="年龄" prop="age">
            <el-input-number v-model="healthForm.age" :min="1" :max="150" placeholder="请输入年龄" style="width: 160px" />
          </el-form-item>
          <el-form-item label="性别" prop="gender">
            <el-radio-group v-model="healthForm.gender">
              <el-radio value="MALE">男</el-radio>
              <el-radio value="FEMALE">女</el-radio>
              <el-radio value="OTHER">其他</el-radio>
            </el-radio-group>
          </el-form-item>
          <el-form-item label="过敏史" prop="allergies">
            <el-input
              v-model="healthForm.allergies"
              type="textarea"
              :rows="2"
              placeholder="如：青霉素、花粉（无则留空）"
              maxlength="200"
              show-word-limit
            />
          </el-form-item>
          <el-form-item label="基础疾病" prop="chronicDiseases">
            <el-input
              v-model="healthForm.chronicDiseases"
              type="textarea"
              :rows="2"
              placeholder="如：高血压、2型糖尿病（无则留空）"
              maxlength="200"
              show-word-limit
            />
          </el-form-item>
          <el-form-item label="当前用药" prop="currentMedications">
            <el-input
              v-model="healthForm.currentMedications"
              type="textarea"
              :rows="2"
              placeholder="如：氨氯地平5mg（无则留空）"
              maxlength="200"
              show-word-limit
            />
          </el-form-item>
          <el-form-item>
            <el-button type="primary" :loading="healthLoading" @click="submitHealth">
              保存健康档案
            </el-button>
          </el-form-item>
        </el-form>
      </el-card>

      <!-- Change Password Card -->
      <el-card class="profile-card" shadow="never">
        <template #header>
          <span class="card-title">修改密码</span>
        </template>
        <el-form
          ref="passwordFormRef"
          :model="passwordForm"
          :rules="passwordRules"
          label-width="100px"
          @submit.prevent="submitPassword"
        >
          <el-form-item label="旧密码" prop="oldPassword">
            <el-input v-model="passwordForm.oldPassword" type="password" show-password placeholder="请输入当前密码" />
          </el-form-item>
          <el-form-item label="新密码" prop="newPassword">
            <el-input v-model="passwordForm.newPassword" type="password" show-password placeholder="6~20 位字符" />
          </el-form-item>
          <el-form-item label="确认新密码" prop="confirmPassword">
            <el-input v-model="passwordForm.confirmPassword" type="password" show-password placeholder="再次输入新密码" />
          </el-form-item>
          <el-form-item>
            <el-button type="primary" :loading="passwordLoading" @click="submitPassword">
              修改密码
            </el-button>
          </el-form-item>
        </el-form>
      </el-card>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { getUserInfo, updatePassword, updateProfile, getHealthProfile, updateHealthProfile } from '@/services/authService'
import { ElMessage } from 'element-plus'
import { ArrowLeft } from '@element-plus/icons-vue'

const router = useRouter()

const authStore = useAuthStore()
const avatarLetter = computed(() => (authStore.username || 'U')[0].toUpperCase())
const currentEmail = ref('')

onMounted(async () => {
  try {
    const res = await getUserInfo()
    currentEmail.value = res.data?.email || ''
  } catch {
    // ignore
  }
  try {
    const res = await getHealthProfile()
    const d = res.data
    healthProfile.value = d
    if (d.filled) {
      healthForm.value = {
        age: d.age ?? null,
        gender: d.gender ?? '',
        allergies: d.allergies ?? '',
        chronicDiseases: d.chronicDiseases ?? '',
        currentMedications: d.currentMedications ?? ''
      }
    }
  } catch {
    // ignore
  }
})

// --- Health profile ---
const healthProfile = ref({ filled: false })
const healthFormRef = ref(null)
const healthLoading = ref(false)
const healthForm = ref({
  age: null,
  gender: '',
  allergies: '',
  chronicDiseases: '',
  currentMedications: ''
})
const healthRules = {
  age: [{ type: 'number', min: 1, max: 150, message: '年龄需在 1~150 之间', trigger: 'blur' }]
}

async function submitHealth() {
  try {
    await healthFormRef.value.validate()
  } catch {
    return
  }
  healthLoading.value = true
  try {
    const payload = {}
    if (healthForm.value.age) payload.age = healthForm.value.age
    if (healthForm.value.gender) payload.gender = healthForm.value.gender
    if (healthForm.value.allergies) payload.allergies = healthForm.value.allergies
    if (healthForm.value.chronicDiseases) payload.chronicDiseases = healthForm.value.chronicDiseases
    if (healthForm.value.currentMedications) payload.currentMedications = healthForm.value.currentMedications
    const res = await updateHealthProfile(payload)
    healthProfile.value = res.data
    ElMessage.success('健康档案已保存')
  } catch (err) {
    ElMessage.error(err.response?.data?.message || '保存失败')
  } finally {
    healthLoading.value = false
  }
}

// --- Profile form ---
const profileFormRef = ref(null)
const profileLoading = ref(false)
const profileForm = ref({ email: '' })
const profileRules = {
  email: [
    { required: true, message: '请输入邮箱', trigger: 'blur' },
    { type: 'email', message: '邮箱格式不正确', trigger: 'blur' }
  ]
}

async function submitProfile() {
  try {
    await profileFormRef.value.validate()
  } catch {
    return
  }
  profileLoading.value = true
  try {
    const res = await updateProfile(profileForm.value.email)
    currentEmail.value = res.data?.email || profileForm.value.email
    profileForm.value.email = ''
    ElMessage.success('邮箱更新成功')
  } catch (err) {
    ElMessage.error(err.response?.data?.message || '更新失败')
  } finally {
    profileLoading.value = false
  }
}

// --- Password form ---
const passwordFormRef = ref(null)
const passwordLoading = ref(false)
const passwordForm = ref({ oldPassword: '', newPassword: '', confirmPassword: '' })

const passwordRules = {
  oldPassword: [{ required: true, message: '请输入旧密码', trigger: 'blur' }],
  newPassword: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    { min: 6, max: 20, message: '密码长度 6~20 位', trigger: 'blur' }
  ],
  confirmPassword: [
    { required: true, message: '请确认新密码', trigger: 'blur' },
    {
      validator: (rule, value, callback) => {
        if (value !== passwordForm.value.newPassword) {
          callback(new Error('两次输入的密码不一致'))
        } else {
          callback()
        }
      },
      trigger: 'blur'
    }
  ]
}

async function submitPassword() {
  try {
    await passwordFormRef.value.validate()
  } catch {
    return
  }
  passwordLoading.value = true
  try {
    await updatePassword(passwordForm.value.oldPassword, passwordForm.value.newPassword)
    passwordForm.value = { oldPassword: '', newPassword: '', confirmPassword: '' }
    ElMessage.success('密码修改成功')
  } catch (err) {
    ElMessage.error(err.response?.data?.message || '修改失败，请检查旧密码是否正确')
  } finally {
    passwordLoading.value = false
  }
}
</script>

<style scoped>
.profile-page {
  min-height: 100vh;
  background: var(--color-surface, #f5f7fa);
  padding: 40px 24px;
}

.profile-back {
  margin-bottom: 16px;
}

.profile-container {
  max-width: 600px;
  margin: 0 auto;
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.profile-header {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 24px;
  background: #ffffff;
  border-radius: 12px;
  border: 1px solid var(--color-border, #e4e7ed);
}

.profile-header__info {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.profile-header__name {
  font-size: 20px;
  font-weight: 600;
  color: var(--color-text, #1a1a1a);
  margin: 0;
}

.profile-header__role {
  display: inline-block;
  font-size: 12px;
  font-weight: 500;
  padding: 2px 10px;
  border-radius: 99px;
}

.role--admin {
  background: #FEF3C7;
  color: #D97706;
}

.role--user {
  background: #DBEAFE;
  color: #2563EB;
}

.profile-card {
  border-radius: 12px;
}

.card-title {
  font-size: 15px;
  font-weight: 600;
  color: var(--color-text, #1a1a1a);
}

.card-header {
  display: flex;
  align-items: center;
  gap: 8px;
}
</style>
