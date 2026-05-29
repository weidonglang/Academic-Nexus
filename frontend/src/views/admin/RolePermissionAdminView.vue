<script setup lang="ts">
import { computed, nextTick, onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import type { TreeInstance } from 'element-plus'
import PageHeader from '@/components/PageHeader.vue'
import {
  rolePermissionMenuCodesApi,
  rolePermissionMenusApi,
  rolePermissionRolesApi,
  updateRolePermissionMenusApi,
  type RolePermissionMenu,
  type RolePermissionRole,
} from '@/api/rolePermission'

interface MenuTreeNode {
  code: string
  title: string
  path: string
  children: MenuTreeNode[]
}

const loading = ref(false)
const saving = ref(false)
const selectedRoleId = ref<number | null>(null)
const roles = ref<RolePermissionRole[]>([])
const menus = ref<RolePermissionMenu[]>([])
const checkedMenuCodes = ref<string[]>([])
const menuTreeRef = ref<TreeInstance>()

const selectedRole = computed(() => roles.value.find((role) => role.roleId === selectedRoleId.value) ?? null)
const checkedCount = computed(() => checkedMenuCodes.value.length)

const menuTree = computed(() => {
  const nodeMap = new Map<string, MenuTreeNode>()
  const roots: MenuTreeNode[] = []

  for (const menu of menus.value) {
    nodeMap.set(menu.code, {
      code: menu.code,
      title: menu.title,
      path: menu.path,
      children: [],
    })
  }

  for (const menu of menus.value) {
    const node = nodeMap.get(menu.code)
    if (!node) {
      continue
    }
    if (menu.parentCode) {
      const parent = nodeMap.get(menu.parentCode)
      if (parent) {
        parent.children.push(node)
        continue
      }
    }
    roots.push(node)
  }
  return roots
})

onMounted(loadData)

async function loadData() {
  loading.value = true
  try {
    const [roleResponse, menuResponse] = await Promise.all([
      rolePermissionRolesApi(),
      rolePermissionMenusApi(),
    ])
    roles.value = roleResponse.data
    menus.value = menuResponse.data
    selectedRoleId.value = roles.value[0]?.roleId ?? null
    if (selectedRoleId.value) {
      await loadRoleMenus(selectedRoleId.value)
    }
  } finally {
    loading.value = false
  }
}

async function selectRole(role: RolePermissionRole) {
  selectedRoleId.value = role.roleId
  await loadRoleMenus(role.roleId)
}

async function loadRoleMenus(roleId: number) {
  const response = await rolePermissionMenuCodesApi(roleId)
  checkedMenuCodes.value = response.data
  await nextTick()
  menuTreeRef.value?.setCheckedKeys(response.data, false)
}

async function saveRoleMenus() {
  if (!selectedRoleId.value) {
    return
  }
  const checkedKeys = menuTreeRef.value?.getCheckedKeys(false) ?? []
  const halfCheckedKeys = menuTreeRef.value?.getHalfCheckedKeys() ?? []
  const menuCodes = [...checkedKeys, ...halfCheckedKeys].map(String)

  saving.value = true
  try {
    const response = await updateRolePermissionMenusApi(selectedRoleId.value, menuCodes)
    checkedMenuCodes.value = response.data
    menuTreeRef.value?.setCheckedKeys(response.data, false)
    ElMessage.success('角色权限已保存')
  } catch (error) {
    ElMessage.error(resolveErrorMessage(error, '保存角色权限失败'))
  } finally {
    saving.value = false
  }
}

function checkAll() {
  const codes = menus.value.map((menu) => menu.code)
  checkedMenuCodes.value = codes
  menuTreeRef.value?.setCheckedKeys(codes, false)
}

function clearAll() {
  checkedMenuCodes.value = []
  menuTreeRef.value?.setCheckedKeys([], false)
}

function handleCheck() {
  checkedMenuCodes.value = (menuTreeRef.value?.getCheckedKeys(false) ?? []).map(String)
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
  <PageHeader title="角色权限管理" description="按角色配置可见菜单，为后续接口权限和用户管理预留基础。" />

  <section class="admin-toolbar permission-toolbar">
    <div class="admin-summary">
      <article>
        <span>角色数</span>
        <strong>{{ roles.length }}</strong>
      </article>
      <article>
        <span>菜单数</span>
        <strong>{{ menus.length }}</strong>
      </article>
      <article>
        <span>已勾选</span>
        <strong>{{ checkedCount }}</strong>
      </article>
    </div>
    <div class="admin-actions">
      <el-button @click="checkAll">全选</el-button>
      <el-button @click="clearAll">清空</el-button>
      <el-button type="primary" :loading="saving" :disabled="!selectedRoleId" @click="saveRoleMenus">
        保存权限
      </el-button>
    </div>
  </section>

  <section v-loading="loading" class="permission-grid">
    <article class="work-panel role-list permission-role-panel">
      <h2>角色</h2>
      <button
        v-for="role in roles"
        :key="role.roleId"
        class="role-item"
        :class="{ active: role.roleId === selectedRoleId }"
        type="button"
        @click="selectRole(role)"
      >
        <strong>{{ role.name }}</strong>
        <span>{{ role.code }}</span>
      </button>
    </article>

    <article class="work-panel permission-panel permission-menu-panel">
      <div class="panel-heading">
        <h2>{{ selectedRole ? `${selectedRole.name} 菜单权限` : '菜单权限' }}</h2>
        <el-tag type="info">保存后重新登录或刷新菜单生效</el-tag>
      </div>
      <el-tree
        ref="menuTreeRef"
        class="permission-tree"
        :data="menuTree"
        node-key="code"
        show-checkbox
        default-expand-all
        :props="{ label: 'title', children: 'children' }"
        @check="handleCheck"
      >
        <template #default="{ data }">
          <div class="permission-node">
            <span>{{ data.title }}</span>
            <small>{{ data.path }}</small>
          </div>
        </template>
      </el-tree>
    </article>
  </section>
</template>
