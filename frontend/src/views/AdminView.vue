<template>
  <div class="admin-page">
    <div class="admin-container">
      <div class="admin-back">
        <el-button :icon="ArrowLeft" @click="router.push('/chat')">返回问诊</el-button>
      </div>
      <h1 class="admin-title">管理后台</h1>

      <!-- Tab navigation -->
      <el-tabs v-model="activeTab" class="admin-tabs">
        <!-- User Management Tab -->
        <el-tab-pane label="用户管理" name="users">
          <div class="tab-toolbar">
            <el-input
              v-model="userSearch.keyword"
              placeholder="搜索用户名"
              clearable
              style="width: 200px"
              @keyup.enter="loadUsers"
              @clear="loadUsers"
            />
            <el-select v-model="userSearch.status" placeholder="状态筛选" clearable style="width: 130px" @change="loadUsers">
              <el-option label="正常" value="ACTIVE" />
              <el-option label="禁用" value="BANNED" />
            </el-select>
            <el-button type="primary" @click="loadUsers">查询</el-button>
          </div>

          <el-table :data="users" v-loading="usersLoading" stripe border style="width: 100%">
            <el-table-column prop="userId" label="ID" width="70" />
            <el-table-column prop="username" label="用户名" min-width="120" />
            <el-table-column prop="email" label="邮箱（脱敏）" min-width="160" />
            <el-table-column prop="role" label="角色" width="90">
              <template #default="{ row }">
                <el-tag :type="row.role === 'ADMIN' ? 'warning' : 'info'" size="small">{{ row.role }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="status" label="状态" width="100">
              <template #default="{ row }">
                <el-tag :type="row.status === 'ACTIVE' ? 'success' : 'danger'" size="small">{{ row.status === 'ACTIVE' ? '正常' : row.status === 'BANNED' ? '禁用' : row.status }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="createdAt" label="注册时间" min-width="150">
              <template #default="{ row }">{{ formatDate(row.createdAt) }}</template>
            </el-table-column>
            <el-table-column label="操作" width="360" fixed="right">
              <template #default="{ row }">
                <div class="operation-buttons">
                <el-button
                  v-if="row.status === 'ACTIVE'"
                  type="danger"
                  size="small"
                  plain
                  @click="handleBan(row)"
                >
                  禁用
                </el-button>
                <el-button
                  v-else
                  type="success"
                  size="small"
                  plain
                  @click="handleUnban(row)"
                >
                  启用
                </el-button>
                <el-button type="warning" size="small" plain @click="handleResetPassword(row)">
                  重置密码
                </el-button>
                <el-button type="primary" size="small" plain @click="handleViewConsultations(row)">
                  会话
                </el-button>
                <el-button
                  v-if="row.role !== 'ADMIN'"
                  type="info"
                  size="small"
                  plain
                  @click="handleAssignRole(row)"
                >
                  分配角色
                </el-button>
                </div>
              </template>
            </el-table-column>
          </el-table>

          <div class="pagination-bar">
            <el-pagination
              :current-page="userPage"
              :page-size="userPageSize"
              :total="userTotal"
              layout="total, prev, pager, next"
              @current-change="(page) => { userPage = page; loadUsers() }"
            />
          </div>
        </el-tab-pane>

        <!-- System Logs Tab -->
        <el-tab-pane label="系统日志" name="logs">
          <el-table :data="logs" v-loading="logsLoading" stripe border style="width: 100%">
            <el-table-column prop="operatorUsername" label="操作人" width="120" />
            <el-table-column prop="action" label="动作" width="140" />
            <el-table-column prop="targetUsername" label="目标用户" width="120" />
            <el-table-column prop="createdAt" label="时间" min-width="150">
              <template #default="{ row }">{{ formatDate(row.createdAt) }}</template>
            </el-table-column>
            <el-table-column prop="details" label="详情" min-width="200" show-overflow-tooltip />
          </el-table>

          <div class="pagination-bar">
            <el-pagination
              :current-page="logPage"
              :page-size="logPageSize"
              :total="logTotal"
              layout="total, prev, pager, next"
              @current-change="(page) => { logPage = page; loadLogs() }"
            />
          </div>
        </el-tab-pane>
      </el-tabs>
    </div>

    <!-- Consultation Metadata Dialog -->
    <el-dialog
      v-model="consultationDialogVisible"
      :title="`${selectedUser?.username} 的问诊记录`"
      width="700px"
    >
      <el-table :data="consultations" v-loading="consultationsLoading" stripe border>
        <el-table-column prop="consultationId" label="ID" width="70" />
        <el-table-column prop="chiefComplaint" label="主诉" min-width="140" show-overflow-tooltip />
        <el-table-column prop="status" label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="row.status === 'completed' ? 'success' : 'warning'" size="small">{{ row.status === 'completed' ? '已完成' : row.status === 'ongoing' ? '进行中' : row.status }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="riskLevel" label="风险等级" width="100">
          <template #default="{ row }">
            <RiskBadge :level="row.riskLevel" />
          </template>
        </el-table-column>
        <el-table-column prop="createdAt" label="创建时间" min-width="140">
          <template #default="{ row }">{{ formatDate(row.createdAt) }}</template>
        </el-table-column>
      </el-table>
    </el-dialog>

    <!-- Assign Role Dialog -->
    <el-dialog v-model="assignRoleDialogVisible" title="分配角色" width="360px" :close-on-click-modal="false">
      <p style="margin: 0 0 12px">用户：<strong>{{ selectedUser?.username }}</strong></p>
      <el-select v-model="assignRoleValue" style="width: 100%">
        <el-option label="普通用户 (USER)" value="USER" />
        <el-option label="知识工程师 (KNOWLEDGE_ENGINEER)" value="KNOWLEDGE_ENGINEER" />
      </el-select>
      <template #footer>
        <el-button @click="assignRoleDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="assignRoleLoading" @click="confirmAssignRole">确认</el-button>
      </template>
    </el-dialog>

    <!-- Reset Password Result Dialog -->
    <el-dialog v-model="resetPasswordDialogVisible" title="密码重置成功" width="360px" :close-on-click-modal="false">
      <p style="margin: 0 0 8px">用户 <strong>{{ selectedUser?.username }}</strong> 的临时密码：</p>
      <el-input :model-value="tempPassword" readonly>
        <template #append>
          <el-button @click="copyPassword">复制</el-button>
        </template>
      </el-input>
      <p class="reset-tip">请将此临时密码告知目标用户，登录后应立即修改。</p>
      <template #footer>
        <el-button type="primary" @click="resetPasswordDialogVisible = false">关闭</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ArrowLeft } from '@element-plus/icons-vue'
import {
  getAdminUsers,
  banUser,
  unbanUser,
  resetUserPassword,
  getUserConsultations,
  getAdminLogs,
  assignRole
} from '@/services/adminService'
import RiskBadge from '@/components/common/RiskBadge.vue'
import { ElMessage, ElMessageBox } from 'element-plus'

const router = useRouter()

const activeTab = ref('users')

// --- Users ---
const users = ref([])
const usersLoading = ref(false)
const userPage = ref(1)
const userPageSize = 10
const userTotal = ref(0)
const userSearch = ref({ keyword: '', status: '' })

async function loadUsers() {
  usersLoading.value = true
  try {
    const params = {
      page: userPage.value,
      size: userPageSize,
      ...(userSearch.value.keyword && { keyword: userSearch.value.keyword }),
      ...(userSearch.value.status && { status: userSearch.value.status })
    }
    const res = await getAdminUsers(params)
    users.value = res.data?.users || []
    userTotal.value = res.data?.total || 0
  } catch {
    ElMessage.error('获取用户列表失败')
  } finally {
    usersLoading.value = false
  }
}

async function handleBan(row) {
  await ElMessageBox.confirm(`确认禁用用户 "${row.username}" ?`, '操作确认', { type: 'warning' })
  try {
    await banUser(row.userId)
    ElMessage.success('已禁用')
    loadUsers()
  } catch (err) {
    ElMessage.error(err.response?.data?.message || '操作失败')
  }
}

async function handleUnban(row) {
  await ElMessageBox.confirm(`确认启用用户 "${row.username}" ?`, '操作确认', { type: 'warning' })
  try {
    await unbanUser(row.userId)
    ElMessage.success('已启用')
    loadUsers()
  } catch (err) {
    ElMessage.error(err.response?.data?.message || '操作失败')
  }
}

// --- Reset Password ---
const resetPasswordDialogVisible = ref(false)
const tempPassword = ref('')
const selectedUser = ref(null)

async function handleResetPassword(row) {
  await ElMessageBox.confirm(`确认重置用户 "${row.username}" 的密码？`, '操作确认', { type: 'warning' })
  try {
    const res = await resetUserPassword(row.userId)
    tempPassword.value = res.data?.newPassword || ''
    selectedUser.value = row
    resetPasswordDialogVisible.value = true
  } catch (err) {
    ElMessage.error(err.response?.data?.message || '重置失败')
  }
}

function copyPassword() {
  navigator.clipboard.writeText(tempPassword.value).then(() => {
    ElMessage.success('已复制到剪贴板')
  })
}

// --- Consultations ---
const consultationDialogVisible = ref(false)
const consultations = ref([])
const consultationsLoading = ref(false)

async function handleViewConsultations(row) {
  selectedUser.value = row
  consultationDialogVisible.value = true
  consultationsLoading.value = true
  try {
    const res = await getUserConsultations(row.userId)
    consultations.value = res.data || []
  } catch {
    ElMessage.error('获取会话记录失败')
  } finally {
    consultationsLoading.value = false
  }
}

// --- Assign Role ---
const assignRoleDialogVisible = ref(false)
const assignRoleValue = ref('USER')
const assignRoleLoading = ref(false)

function handleAssignRole(row) {
  selectedUser.value = row
  assignRoleValue.value = row.role
  assignRoleDialogVisible.value = true
}

async function confirmAssignRole() {
  assignRoleLoading.value = true
  try {
    await assignRole(selectedUser.value.userId, assignRoleValue.value)
    ElMessage.success('角色已更新')
    assignRoleDialogVisible.value = false
    loadUsers()
  } catch (err) {
    ElMessage.error(err.response?.data?.message || '操作失败')
  } finally {
    assignRoleLoading.value = false
  }
}

// --- Logs ---
const logs = ref([])
const logsLoading = ref(false)
const logPage = ref(1)
const logPageSize = 20
const logTotal = ref(0)

async function loadLogs() {
  logsLoading.value = true
  try {
    const res = await getAdminLogs({ page: logPage.value, size: logPageSize })
    logs.value = res.data?.logs || []
    logTotal.value = res.data?.total || 0
  } catch {
    ElMessage.error('获取日志失败')
  } finally {
    logsLoading.value = false
  }
}

function formatDate(dateStr) {
  if (!dateStr) return '-'
  return new Date(dateStr).toLocaleString('zh-CN', {
    year: 'numeric', month: '2-digit', day: '2-digit',
    hour: '2-digit', minute: '2-digit'
  })
}

onMounted(() => {
  loadUsers()
  loadLogs()
})
</script>

<style scoped>
.admin-page {
  min-height: 100vh;
  background: var(--color-surface, #f5f7fa);
  padding: 40px 24px;
}

.admin-container {
  max-width: 1100px;
  margin: 0 auto;
}

.admin-back {
  margin-bottom: 16px;
}

.admin-title {
  font-size: 22px;
  font-weight: 700;
  color: var(--color-text, #1a1a1a);
  margin: 0 0 24px;
}

.admin-tabs {
  background: #ffffff;
  border-radius: 12px;
  padding: 16px;
  border: 1px solid var(--color-border, #e4e7ed);
}

.tab-toolbar {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 16px;
}

.pagination-bar {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
}

.admin-page :deep(.operation-buttons) {
  display: flex;
  align-items: center;
  flex-wrap: nowrap;
  gap: 8px;
}

.admin-page :deep(.operation-buttons .el-button) {
  margin-left: 0;
}

.reset-tip {
  margin: 12px 0 0;
  font-size: 12px;
  color: var(--color-text-secondary, #909399);
}
</style>
