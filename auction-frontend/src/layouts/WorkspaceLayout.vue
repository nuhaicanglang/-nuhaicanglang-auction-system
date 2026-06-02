<script setup lang="ts">
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import {
  BadgeCheck,
  BookOpenCheck,
  Gavel,
  History,
  LayoutDashboard,
  LogOut,
  Menu,
  PackageCheck,
  Scale,
  Search,
  ShieldCheck,
  Upload,
  UserRound,
  WalletCards,
} from '@lucide/vue'
import { useAuthStore } from '@/stores/auth'
import { useUiStore } from '@/stores/ui'

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()
const ui = useUiStore()

const userMenus = [
  { path: '/user/overview', label: '用户概览', icon: LayoutDashboard },
  { path: '/user/publish', label: '发布拍品', icon: Upload },
  { path: '/user/items', label: '我的拍品', icon: Gavel },
  { path: '/user/bids', label: '我的竞拍', icon: BadgeCheck },
  { path: '/user/orders/buyer', label: '我买到的', icon: PackageCheck },
  { path: '/user/orders/seller', label: '我卖出的', icon: BookOpenCheck },
  { path: '/user/wallet', label: '我的钱包', icon: WalletCards },
  { path: '/user/credit', label: '信用分', icon: ShieldCheck },
  { path: '/user/reviews', label: '评价管理', icon: Scale },
  { path: '/user/search-history', label: '搜索历史', icon: History },
]

const adminMenus = [
  { path: '/admin/stats', label: '运营看板', icon: LayoutDashboard },
  { path: '/admin/items/audits', label: '商品审核', icon: Gavel },
  { path: '/admin/categories', label: '分类管理', icon: Search },
  { path: '/admin/users', label: '用户管理', icon: UserRound },
  { path: '/admin/roles', label: '角色权限', icon: ShieldCheck },
  { path: '/admin/wallet', label: '资金审计', icon: WalletCards },
  { path: '/admin/credit', label: '信用管理', icon: BadgeCheck },
  { path: '/admin/exports', label: '数据导出', icon: BookOpenCheck },
]

const isAdminSection = computed(() => route.path.startsWith('/admin'))
const menus = computed(() => (isAdminSection.value ? adminMenus : userMenus))
const title = computed(() => route.meta.title || (isAdminSection.value ? '运营后台' : '用户中心'))

async function logout() {
  await auth.logout()
  router.push('/login')
}
</script>

<template>
  <div class="workspace" :class="{ collapsed: ui.sidebarCollapsed }">
    <aside class="sidebar">
      <RouterLink class="workspace-brand" to="/">
        <span><Gavel aria-hidden="true" /></span>
        <strong v-if="!ui.sidebarCollapsed">云槌拍卖</strong>
      </RouterLink>

      <nav aria-label="工作台导航">
        <RouterLink
          v-for="menu in menus"
          :key="menu.path"
          :to="menu.path"
          class="menu-link"
          :title="menu.label"
        >
          <component :is="menu.icon" aria-hidden="true" />
          <span v-if="!ui.sidebarCollapsed">{{ menu.label }}</span>
        </RouterLink>
      </nav>
    </aside>

    <section class="workspace-main">
      <header class="workspace-header">
        <div>
          <p class="section-kicker">{{ isAdminSection ? 'Admin Console' : 'Member Workspace' }}</p>
          <h1>{{ title }}</h1>
        </div>
        <div class="header-actions">
          <ElButton circle aria-label="折叠侧边栏" @click="ui.toggleSidebar">
            <Menu aria-hidden="true" />
          </ElButton>
          <ElButton v-if="isAdminSection" @click="router.push('/user')">用户中心</ElButton>
          <ElButton v-else-if="auth.isAdmin" @click="router.push('/admin')">运营后台</ElButton>
          <ElButton @click="logout">
            <LogOut aria-hidden="true" />
            退出
          </ElButton>
        </div>
      </header>

      <main class="workspace-content">
        <RouterView v-slot="{ Component }">
          <Transition name="fade-slide" mode="out-in">
            <component :is="Component" />
          </Transition>
        </RouterView>
      </main>
    </section>
  </div>
</template>

<style scoped>
.workspace {
  display: grid;
  min-height: 100dvh;
  grid-template-columns: 248px minmax(0, 1fr);
}

.workspace.collapsed {
  grid-template-columns: 78px minmax(0, 1fr);
}

.sidebar {
  position: sticky;
  top: 0;
  height: 100dvh;
  padding: 16px;
  border-right: 1px solid var(--color-line);
  background: #102f3f;
  color: #fffaf1;
}

.workspace-brand {
  display: flex;
  min-height: 52px;
  align-items: center;
  gap: 10px;
  margin-bottom: 18px;
}

.workspace-brand span {
  display: grid;
  width: 42px;
  height: 42px;
  place-items: center;
  border-radius: 10px;
  background: rgba(255, 253, 248, 0.12);
}

.workspace-brand svg,
.menu-link svg,
.header-actions svg {
  width: 18px;
  height: 18px;
}

.workspace-brand strong {
  font-family: var(--font-display);
  font-size: 20px;
}

nav {
  display: grid;
  gap: 6px;
}

.menu-link {
  display: flex;
  min-height: 44px;
  align-items: center;
  gap: 10px;
  padding: 10px 12px;
  border-radius: 8px;
  color: rgba(255, 250, 241, 0.78);
  font-weight: 700;
}

.menu-link:hover,
.menu-link.router-link-active {
  color: #fffaf1;
  background: rgba(255, 253, 248, 0.13);
}

.workspace-main {
  min-width: 0;
}

.workspace-header {
  position: sticky;
  top: 0;
  z-index: 20;
  display: flex;
  min-height: 82px;
  align-items: center;
  justify-content: space-between;
  gap: 18px;
  padding: 14px 28px;
  border-bottom: 1px solid rgba(230, 220, 203, 0.82);
  background: rgba(255, 253, 248, 0.9);
  backdrop-filter: blur(16px);
}

.workspace-header h1 {
  margin: 0;
  font-family: var(--font-display);
  font-size: 28px;
}

.header-actions {
  display: flex;
  align-items: center;
  gap: 10px;
}

.workspace-content {
  padding: 28px;
}

@media (max-width: 900px) {
  .workspace,
  .workspace.collapsed {
    grid-template-columns: 1fr;
  }

  .sidebar {
    position: static;
    height: auto;
  }

  nav {
    grid-template-columns: repeat(auto-fit, minmax(128px, 1fr));
  }

  .workspace-content,
  .workspace-header {
    padding: 18px;
  }
}

@media (max-width: 640px) {
  .workspace-header {
    align-items: flex-start;
    flex-direction: column;
  }

  .header-actions {
    flex-wrap: wrap;
  }
}
</style>
