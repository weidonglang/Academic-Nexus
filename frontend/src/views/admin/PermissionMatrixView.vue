<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { Check, X } from 'lucide-vue-next'
import { NexusMetricCard, NexusPageHeader, NexusRiskBadge } from '@/components/nexus'
import { permissionMatrixApi, type PermissionMatrixResponse } from '@/api/rolePermission'

const loading = ref(false)
const matrix = ref<PermissionMatrixResponse | null>(null)
const moduleFilter = ref('')

const menus = computed(() => {
  const all = matrix.value?.menus ?? []
  return moduleFilter.value ? all.filter((menu) => menu.path.startsWith(moduleFilter.value)) : all
})
const capabilities = computed(() => matrix.value?.capabilities ?? [])
const highRiskCapabilities = computed(() => capabilities.value.filter((item) => /WRITE|AUDIT|SQL|STATUS|NOTICE/.test(item.code)).length)

onMounted(loadMatrix)

async function loadMatrix() {
  loading.value = true
  try {
    matrix.value = (await permissionMatrixApi()).data
  } finally {
    loading.value = false
  }
}

function hasMenu(roleCodes: string[], code: string) {
  return roleCodes.includes(code)
}

function hasCapability(roleCodes: string[], code: string) {
  return roleCodes.includes(code)
}

function riskLevel(code: string) {
  return /ROLE_PERMISSION|USER|SQL|AUDIT/.test(code) ? 'high' : /WRITE|STATUS|NOTICE/.test(code) ? 'medium' : 'low'
}
</script>

<template>
  <NexusPageHeader
    title="权限矩阵"
    eyebrow="Permission Matrix"
    description="用角色 × 菜单/能力矩阵展示学生、教师、管理员的访问边界，前端隐藏菜单之外，后端仍按角色和权限拦截。"
  />

  <section class="matrix-metrics">
    <NexusMetricCard label="角色" :value="matrix?.roles.length ?? 0" suffix="个" tone="info" />
    <NexusMetricCard label="菜单权限" :value="matrix?.menus.length ?? 0" suffix="项" tone="success" />
    <NexusMetricCard label="能力权限" :value="capabilities.length" suffix="项" tone="warning" />
    <NexusMetricCard label="高敏权限" :value="highRiskCapabilities" suffix="项" tone="danger" />
  </section>

  <section class="admin-toolbar">
    <div class="admin-actions">
      <el-select v-model="moduleFilter" placeholder="按路径模块筛选" clearable>
        <el-option label="管理端" value="/admin" />
        <el-option label="教师端" value="/teacher" />
        <el-option label="学生端" value="/student" />
        <el-option label="AI" value="/ai" />
      </el-select>
      <el-button :loading="loading" @click="loadMatrix">刷新</el-button>
    </div>
  </section>

  <section v-loading="loading" class="work-panel">
    <div class="panel-heading">
      <h2>菜单权限矩阵</h2>
      <span>{{ menus.length }} 项</span>
    </div>
    <el-table :data="menus" border>
      <el-table-column prop="title" label="菜单" min-width="150" />
      <el-table-column prop="path" label="路径" min-width="180" show-overflow-tooltip />
      <el-table-column
        v-for="role in matrix?.roleRows ?? []"
        :key="role.roleCode"
        :label="role.roleName"
        width="110"
        align="center"
      >
        <template #default="{ row }">
          <Check v-if="hasMenu(role.menuCodes, row.code)" class="matrix-yes" :size="18" />
          <X v-else class="matrix-no" :size="18" />
        </template>
      </el-table-column>
    </el-table>
  </section>

  <section class="work-panel">
    <div class="panel-heading">
      <h2>接口与能力权限</h2>
      <span>{{ capabilities.length }} 项</span>
    </div>
    <el-table :data="capabilities" border>
      <el-table-column prop="name" label="能力" min-width="160" />
      <el-table-column prop="code" label="权限码" min-width="180" />
      <el-table-column label="风险" width="100">
        <template #default="{ row }">
          <NexusRiskBadge :level="riskLevel(row.code)" />
        </template>
      </el-table-column>
      <el-table-column prop="description" label="说明" min-width="220" />
      <el-table-column
        v-for="role in matrix?.roleRows ?? []"
        :key="role.roleCode"
        :label="role.roleName"
        width="110"
        align="center"
      >
        <template #default="{ row }">
          <Check v-if="hasCapability(role.capabilityCodes, row.code)" class="matrix-yes" :size="18" />
          <X v-else class="matrix-no" :size="18" />
        </template>
      </el-table-column>
    </el-table>
  </section>
</template>

<style scoped>
.matrix-metrics {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 14px;
  margin-bottom: 18px;
}

.matrix-yes {
  color: var(--nexus-success);
}

.matrix-no {
  color: #a8b2c1;
}

@media (max-width: 1024px) {
  .matrix-metrics {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 720px) {
  .matrix-metrics {
    grid-template-columns: 1fr;
  }
}
</style>
