<script setup lang="ts">
// 管理端用户管理页面。
// 通过分页查询展示学生、教师、管理员和压测账号，支持新增用户、修改状态、
// 重置密码和调整角色，避免一次加载上万账号导致页面卡顿。
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import PageHeader from '@/components/PageHeader.vue'
import { useAuthStore } from '@/stores/auth'
import {
  adminUserRolesApi,
  adminUsersApi,
  createAdminUserApi,
  deleteAdminUserApi,
  resetAdminUserPasswordApi,
  updateAdminUserApi,
  updateAdminUserRolesApi,
  type AdminRole,
  type AdminUser,
  type UserStatus,
} from '@/api/adminUser'

const auth = useAuthStore()
const loading = ref(false)
const saving = ref(false)
const userDialogVisible = ref(false)
const roleDialogVisible = ref(false)
const passwordDialogVisible = ref(false)
const editingUser = ref<AdminUser | null>(null)
const keyword = ref('')
const users = ref<AdminUser[]>([])
const roles = ref<AdminRole[]>([])
const page = ref(1)
const pageSize = ref(50)
const total = ref(0)

const userForm = reactive({
  username: '',
  displayName: '',
  password: '',
  status: 'ACTIVE' as UserStatus,
  roleCodes: [] as string[],
})

const passwordForm = reactive({
  password: '',
})

const activeCount = computed(() => users.value.filter((user) => user.status === 'ACTIVE').length)
const adminCount = computed(() => users.value.filter((user) => user.roleCodes.includes('ADMIN')).length)

const statusText: Record<UserStatus, string> = {
  ACTIVE: '正常',
  DISABLED: '禁用',
  LOCKED: '锁定',
}

const statusType: Record<UserStatus, 'success' | 'info' | 'warning'> = {
  ACTIVE: 'success',
  DISABLED: 'info',
  LOCKED: 'warning',
}

onMounted(loadData)

// 功能：分页加载用户和角色数据。
// 说明：用户管理页同时需要账号列表和角色列表，账号列表走分页，避免压测账号较多时页面卡死。
async function loadData() {
  loading.value = true
  try {
    const [userResponse, roleResponse] = await Promise.all([
      adminUsersApi({
        keyword: keyword.value.trim() || undefined,
        page: page.value,
        size: pageSize.value,
      }),
      adminUserRolesApi(),
    ])
    users.value = userResponse.data.records
    total.value = userResponse.data.total
    roles.value = roleResponse.data
  } finally {
    loading.value = false
  }
}

function searchUsers() {
  page.value = 1
  loadData()
}

function handlePageChange(nextPage: number) {
  page.value = nextPage
  loadData()
}

function handleSizeChange(nextSize: number) {
  pageSize.value = nextSize
  page.value = 1
  loadData()
}

// 功能：打开新增用户弹窗。
// 说明：初始化空表单并默认选择学生角色，密码不预填，避免页面暴露默认测试密码。
function openCreateUser() {
  editingUser.value = null
  userForm.username = ''
  userForm.displayName = ''
  userForm.password = ''
  userForm.status = 'ACTIVE'
  userForm.roleCodes = ['STUDENT']
  userDialogVisible.value = true
}

function openEditUser(row: AdminUser) {
  editingUser.value = row
  userForm.username = row.username
  userForm.displayName = row.displayName
  userForm.password = ''
  userForm.status = row.status
  userForm.roleCodes = [...row.roleCodes]
  userDialogVisible.value = true
}

function openRoleDialog(row: AdminUser) {
  editingUser.value = row
  userForm.roleCodes = [...row.roleCodes]
  roleDialogVisible.value = true
}

function openPasswordDialog(row: AdminUser) {
  editingUser.value = row
  passwordForm.password = ''
  passwordDialogVisible.value = true
}

// 功能：保存用户基础信息或新增用户。
// 说明：新增时提交账号、显示名、密码和角色；编辑时只更新显示名和状态，
// 角色变更由单独的分配角色弹窗处理。
async function saveUser() {
  if (!editingUser.value && userForm.password.trim().length < 6) {
    ElMessage.error('请设置不少于 6 位的初始密码')
    return
  }
  saving.value = true
  try {
    if (editingUser.value) {
      await updateAdminUserApi(editingUser.value.userId, {
        displayName: userForm.displayName,
        status: userForm.status,
      })
      ElMessage.success('账号信息已更新')
    } else {
      await createAdminUserApi({
        username: userForm.username,
        displayName: userForm.displayName,
        password: userForm.password,
        roleCodes: userForm.roleCodes,
      })
      ElMessage.success('账号已新增')
    }
    userDialogVisible.value = false
    await loadData()
  } catch (error) {
    ElMessage.error(resolveErrorMessage(error, '保存账号失败'))
  } finally {
    saving.value = false
  }
}

// 功能：保存用户角色。
// 说明：管理员勾选角色后提交给后端，后端更新用户角色关系，影响该用户菜单和接口权限。
async function saveRoles() {
  if (!editingUser.value) return
  saving.value = true
  try {
    await updateAdminUserRolesApi(editingUser.value.userId, userForm.roleCodes)
    ElMessage.success('用户角色已更新')
    roleDialogVisible.value = false
    await loadData()
  } catch (error) {
    ElMessage.error(resolveErrorMessage(error, '保存用户角色失败'))
  } finally {
    saving.value = false
  }
}

// 功能：重置用户密码。
// 说明：管理员输入新密码后提交，前端不再使用固定默认密码，降低演示包明文泄露风险。
async function resetPassword() {
  if (!editingUser.value) return
  if (passwordForm.password.trim().length < 6) {
    ElMessage.error('请设置不少于 6 位的新密码')
    return
  }
  await ElMessageBox.confirm(`确认重置 ${editingUser.value.username} 的密码吗？`, '重置密码', {
    type: 'warning',
    confirmButtonText: '重置',
    cancelButtonText: '取消',
  })
  saving.value = true
  try {
    await resetAdminUserPasswordApi(editingUser.value.userId, passwordForm.password)
    ElMessage.success('密码已重置')
    passwordDialogVisible.value = false
  } catch (error) {
    ElMessage.error(resolveErrorMessage(error, '重置密码失败'))
  } finally {
    saving.value = false
  }
}

// 功能：删除用户账号。
// 说明：删除前弹窗确认，后端会阻止删除当前账号或已有学生档案、业务数据关联的账号。
async function removeUser(row: AdminUser) {
  await ElMessageBox.confirm(
    `确认物理删除账号 ${row.username} 吗？已有学生档案或业务数据的账号会被后端拒绝删除。`,
    '删除账号',
    {
      type: 'warning',
      confirmButtonText: '删除',
      cancelButtonText: '取消',
    },
  )
  try {
    await deleteAdminUserApi(row.userId)
    ElMessage.success('账号已删除')
    if (users.value.length === 1 && page.value > 1) {
      page.value -= 1
    }
    await loadData()
  } catch (error) {
    ElMessage.error(resolveErrorMessage(error, '删除账号失败'))
  }
}

function formatDateTime(value?: string) {
  return value ? new Date(value).toLocaleString('zh-CN') : '-'
}

function roleName(roleCode: string) {
  return roles.value.find((role) => role.code === roleCode)?.name ?? roleCode
}

function resolveErrorMessage(error: unknown, fallback: string) {
  if (
    typeof error === 'object' &&
    error !== null &&
    'response' in error &&
    typeof (error as { response?: { data?: { message?: string } } }).response?.data?.message === 'string'
  ) {
    return (error as { response: { data: { message: string } } }).response.data.message
  }
  return fallback
}
</script>

<template>
  <PageHeader title="用户与角色" description="维护系统账号、登录状态和角色归属，支持分页查看全部账号。" />

  <section class="admin-toolbar">
    <div class="admin-summary">
      <article><span>账号总数</span><strong>{{ total }}</strong></article>
      <article><span>本页正常</span><strong>{{ activeCount }}</strong></article>
      <article><span>本页管理员</span><strong>{{ adminCount }}</strong></article>
    </div>
    <div class="admin-actions">
      <el-input
        v-model="keyword"
        class="keyword-input"
        placeholder="账号或姓名"
        clearable
        @keyup.enter="searchUsers"
      />
      <el-button @click="searchUsers">查询</el-button>
      <el-button type="primary" @click="openCreateUser">新增账号</el-button>
    </div>
  </section>

  <section v-loading="loading" class="work-panel">
    <el-table :data="users" empty-text="暂无账号">
      <el-table-column prop="username" label="账号" width="130" />
      <el-table-column prop="displayName" label="姓名" width="130" />
      <el-table-column label="状态" width="100">
        <template #default="{ row }">
          <el-tag :type="statusType[row.status as UserStatus]">{{ statusText[row.status as UserStatus] }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="角色" min-width="180">
        <template #default="{ row }">
          <el-tag v-for="roleCode in row.roleCodes" :key="roleCode" class="role-tag" type="info">
            {{ roleName(roleCode) }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="最后登录" width="180">
        <template #default="{ row }">{{ formatDateTime(row.lastLoginAt) }}</template>
      </el-table-column>
      <el-table-column label="操作" width="310" fixed="right">
        <template #default="{ row }">
          <el-button type="primary" link @click="openEditUser(row)">编辑</el-button>
          <el-button type="primary" link @click="openRoleDialog(row)">角色</el-button>
          <el-button type="warning" link @click="openPasswordDialog(row)">重置密码</el-button>
          <el-button type="danger" link :disabled="row.username === auth.user?.username" @click="removeUser(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <div class="pagination-bar">
      <el-pagination
        v-model:current-page="page"
        v-model:page-size="pageSize"
        :page-sizes="[20, 50, 100, 200]"
        :total="total"
        layout="total, sizes, prev, pager, next, jumper"
        background
        @current-change="handlePageChange"
        @size-change="handleSizeChange"
      />
    </div>
  </section>

  <el-dialog v-model="userDialogVisible" :title="editingUser ? '编辑账号' : '新增账号'" width="520px">
    <el-form label-width="86px" :model="userForm">
      <el-form-item label="账号">
        <el-input v-model="userForm.username" :disabled="Boolean(editingUser)" placeholder="请输入账号" />
      </el-form-item>
      <el-form-item label="姓名">
        <el-input v-model="userForm.displayName" placeholder="请输入显示名称" />
      </el-form-item>
      <el-form-item v-if="!editingUser" label="初始密码">
        <el-input v-model="userForm.password" placeholder="不少于 6 位" type="password" show-password autocomplete="new-password" />
      </el-form-item>
      <el-form-item v-if="!editingUser" label="角色">
        <el-select v-model="userForm.roleCodes" class="full-field" multiple placeholder="请选择角色">
          <el-option v-for="role in roles" :key="role.code" :label="role.name" :value="role.code" />
        </el-select>
      </el-form-item>
      <el-form-item v-if="editingUser" label="状态">
        <el-select v-model="userForm.status" class="full-field">
          <el-option label="正常" value="ACTIVE" />
          <el-option label="禁用" value="DISABLED" />
          <el-option label="锁定" value="LOCKED" />
        </el-select>
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="userDialogVisible = false">取消</el-button>
      <el-button type="primary" :loading="saving" @click="saveUser">保存</el-button>
    </template>
  </el-dialog>

  <el-dialog v-model="roleDialogVisible" title="分配角色" width="480px">
    <el-checkbox-group v-model="userForm.roleCodes" class="role-checks">
      <el-checkbox v-for="role in roles" :key="role.code" :label="role.code">
        {{ role.name }}（{{ role.code }}）
      </el-checkbox>
    </el-checkbox-group>
    <template #footer>
      <el-button @click="roleDialogVisible = false">取消</el-button>
      <el-button type="primary" :loading="saving" @click="saveRoles">保存</el-button>
    </template>
  </el-dialog>

  <el-dialog v-model="passwordDialogVisible" title="重置密码" width="420px">
    <el-form label-width="86px" :model="passwordForm">
      <el-form-item label="新密码">
        <el-input v-model="passwordForm.password" placeholder="不少于 6 位" type="password" show-password autocomplete="new-password" />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="passwordDialogVisible = false">取消</el-button>
      <el-button type="primary" :loading="saving" @click="resetPassword">重置</el-button>
    </template>
  </el-dialog>
</template>

<style scoped>
.pagination-bar {
  display: flex;
  justify-content: flex-end;
  padding-top: 16px;
}
</style>
