<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { ElMessage, type TreeInstance } from 'element-plus'
import { assignRolePermissions, getRolePermissions, listPermissions, listRoles } from '@/api/admin'
import { useAuthStore } from '@/stores/auth'
import type { ID, Permission, Role } from '@/types/domain'

const auth = useAuthStore()
const roles = ref<Role[]>([])
const permissions = ref<Permission[]>([])
const selectedRole = ref<Role | null>(null)
const checkedKeys = ref<ID[]>([])
const treeRef = ref<TreeInstance>()

async function load() {
  const [roleData, permissionData] = await Promise.all([listRoles().catch(() => []), listPermissions().catch(() => [])])
  roles.value = roleData
  permissions.value = permissionData
}

async function selectRole(role: Role) {
  selectedRole.value = role
  checkedKeys.value = await getRolePermissions(role.id).catch(() => [])
  treeRef.value?.setCheckedKeys(checkedKeys.value as string[])
}

async function save() {
  if (!selectedRole.value) {
    ElMessage.warning('请先选择角色')
    return
  }
  if (!auth.roles.includes('SUPER_ADMIN')) {
    ElMessage.warning('当前账号可查看权限树，但仅超级管理员可以保存角色权限')
    return
  }
  const keys = treeRef.value?.getCheckedKeys(false) ?? []
  await assignRolePermissions(selectedRole.value.id, keys as ID[])
  ElMessage.success('角色权限已保存')
}

onMounted(load)
</script>

<template>
  <div class="role-layout">
    <ElCard>
      <template #header>角色列表</template>
      <div class="role-list">
        <button
          v-for="role in roles"
          :key="role.id"
          type="button"
          :class="{ active: selectedRole?.id === role.id }"
          @click="selectRole(role)"
        >
          <strong>{{ role.name }}</strong>
          <span>{{ role.code }}</span>
        </button>
      </div>
    </ElCard>
    <ElCard>
      <template #header>
        <div class="card-header">
          <span>权限树</span>
          <ElButton type="primary" :disabled="!auth.roles.includes('SUPER_ADMIN')" @click="save">保存权限</ElButton>
        </div>
      </template>
      <ElTree
        ref="treeRef"
        :data="permissions"
        node-key="id"
        show-checkbox
        default-expand-all
        :check-strictly="!auth.roles.includes('SUPER_ADMIN')"
        :props="{ label: 'name', children: 'children' }"
      />
    </ElCard>
  </div>
</template>

<style scoped>
.role-layout {
  display: grid;
  grid-template-columns: 320px minmax(0, 1fr);
  gap: 18px;
}

.role-list {
  display: grid;
  gap: 8px;
}

.role-list button {
  display: grid;
  gap: 4px;
  padding: 12px;
  border: 1px solid var(--color-line);
  border-radius: var(--radius-md);
  background: #fffaf1;
  text-align: left;
  cursor: pointer;
}

.role-list button.active {
  border-color: var(--color-primary);
  background: #f6ead7;
}

.role-list span {
  color: var(--color-muted);
  font-size: 13px;
}

.card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

@media (max-width: 900px) {
  .role-layout {
    grid-template-columns: 1fr;
  }
}
</style>
