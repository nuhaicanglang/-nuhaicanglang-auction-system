<script setup lang="ts">
import { computed, ref } from 'vue'
import { useRouter } from 'vue-router'
import { Gavel, LayoutDashboard, LogOut, Menu, Search, UserRound } from '@lucide/vue'
import { useAuthStore } from '@/stores/auth'

const router = useRouter()
const auth = useAuthStore()
const keyword = ref('')
const menuOpen = ref(false)

const userName = computed(() => auth.user?.nickname || auth.user?.username || '用户中心')

function submitSearch() {
  router.push({ path: '/search', query: { keyword: keyword.value } })
}

async function logout() {
  await auth.logout()
  router.push('/')
}
</script>

<template>
  <div class="public-layout">
    <header class="topbar">
      <RouterLink class="brand" to="/" aria-label="返回首页">
        <span class="brand-mark"><Gavel aria-hidden="true" /></span>
        <span>
          <strong>云槌</strong>
          <small>Enterprise Auction</small>
        </span>
      </RouterLink>

      <nav class="nav" :class="{ open: menuOpen }" aria-label="主导航">
        <RouterLink to="/items">拍品大厅</RouterLink>
        <RouterLink to="/search">高级搜索</RouterLink>
        <RouterLink v-if="auth.isAuthenticated" to="/user">用户中心</RouterLink>
        <RouterLink v-if="auth.isAdmin" to="/admin">运营后台</RouterLink>
      </nav>

      <form class="search-box" role="search" @submit.prevent="submitSearch">
        <Search aria-hidden="true" />
        <label class="sr-only" for="global-search">搜索拍品</label>
        <input id="global-search" v-model="keyword" placeholder="搜索国画、腕表、珠宝..." />
      </form>

      <div class="actions">
        <ElButton class="mobile-menu" circle aria-label="展开导航" @click="menuOpen = !menuOpen">
          <Menu aria-hidden="true" />
        </ElButton>
        <template v-if="auth.isAuthenticated">
          <ElDropdown>
            <ElButton>
              <UserRound aria-hidden="true" />
              {{ userName }}
            </ElButton>
            <template #dropdown>
              <ElDropdownMenu>
                <ElDropdownItem @click="router.push('/user')">用户中心</ElDropdownItem>
                <ElDropdownItem v-if="auth.isAdmin" @click="router.push('/admin')">
                  <LayoutDashboard aria-hidden="true" />
                  运营后台
                </ElDropdownItem>
                <ElDropdownItem divided @click="logout">
                  <LogOut aria-hidden="true" />
                  退出登录
                </ElDropdownItem>
              </ElDropdownMenu>
            </template>
          </ElDropdown>
        </template>
        <template v-else>
          <ElButton @click="router.push('/login')">登录</ElButton>
          <ElButton type="primary" @click="router.push('/register')">注册</ElButton>
        </template>
      </div>
    </header>

    <main>
      <RouterView v-slot="{ Component }">
        <Transition name="fade-slide" mode="out-in">
          <component :is="Component" />
        </Transition>
      </RouterView>
    </main>
  </div>
</template>

<style scoped>
.public-layout {
  min-height: 100dvh;
}

.topbar {
  position: sticky;
  top: 0;
  z-index: 40;
  display: grid;
  grid-template-columns: auto 1fr minmax(240px, 360px) auto;
  align-items: center;
  gap: 18px;
  padding: 14px 28px;
  border-bottom: 1px solid rgba(230, 220, 203, 0.82);
  background: rgba(255, 253, 248, 0.9);
  backdrop-filter: blur(16px);
}

.brand {
  display: inline-flex;
  align-items: center;
  gap: 10px;
  min-height: 48px;
}

.brand-mark {
  display: grid;
  width: 42px;
  height: 42px;
  place-items: center;
  border-radius: 12px;
  color: #fffaf1;
  background: linear-gradient(135deg, var(--color-accent), var(--color-primary));
  box-shadow: 0 10px 20px rgba(18, 54, 72, 0.22);
}

.brand-mark svg {
  width: 22px;
  height: 22px;
}

.brand strong,
.brand small {
  display: block;
}

.brand strong {
  font-family: var(--font-display);
  font-size: 20px;
}

.brand small {
  color: var(--color-muted);
  font-size: 12px;
}

.nav {
  display: flex;
  align-items: center;
  gap: 8px;
}

.nav a {
  min-height: 40px;
  padding: 9px 12px;
  border-radius: 7px;
  color: var(--color-muted);
  font-weight: 700;
}

.nav a.router-link-active,
.nav a:hover {
  color: var(--color-accent);
  background: rgba(18, 54, 72, 0.08);
}

.search-box {
  display: flex;
  min-height: 44px;
  align-items: center;
  gap: 8px;
  padding: 0 12px;
  border: 1px solid var(--color-line);
  border-radius: 999px;
  background: #fffaf1;
}

.search-box svg {
  width: 18px;
  color: var(--color-primary);
}

.search-box input {
  width: 100%;
  border: 0;
  outline: 0;
  background: transparent;
  color: var(--color-ink);
  font-size: 15px;
}

.actions {
  display: flex;
  align-items: center;
  gap: 10px;
}

.actions svg {
  width: 16px;
  height: 16px;
}

.mobile-menu {
  display: none;
}

@media (max-width: 980px) {
  .topbar {
    grid-template-columns: auto 1fr auto;
  }

  .search-box {
    grid-column: 1 / -1;
  }

  .nav {
    position: absolute;
    top: 76px;
    right: 16px;
    display: none;
    min-width: 180px;
    flex-direction: column;
    align-items: stretch;
    padding: 10px;
    border: 1px solid var(--color-line);
    border-radius: var(--radius-md);
    background: var(--color-surface-strong);
    box-shadow: var(--shadow-md);
  }

  .nav.open {
    display: flex;
  }

  .mobile-menu {
    display: inline-flex;
  }
}

@media (max-width: 640px) {
  .topbar {
    padding: 12px;
  }

  .brand small {
    display: none;
  }
}
</style>
