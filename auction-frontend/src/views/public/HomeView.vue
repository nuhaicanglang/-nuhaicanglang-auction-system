<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { BadgeCheck, Gavel, ShieldCheck, WalletCards } from '@lucide/vue'
import AuctionCard from '@/components/business/AuctionCard.vue'
import MetricCard from '@/components/business/MetricCard.vue'
import SectionHeader from '@/components/common/SectionHeader.vue'
import EmptyPanel from '@/components/common/EmptyPanel.vue'
import { listCategories, listItems } from '@/api/items'
import type { AuctionItem, Category } from '@/types/domain'
import { normalizeRecords } from '@/utils/format'

const router = useRouter()
const categories = ref<Category[]>([])
const featured = ref<AuctionItem[]>([])

onMounted(async () => {
  const [categoryTree, itemPage] = await Promise.all([
    listCategories().catch(() => []),
    listItems({ page: 1, size: 6, sort: 'endingSoon' }).catch(() => ({ records: [] })),
  ])
  categories.value = categoryTree
  featured.value = normalizeRecords(itemPage)
})
</script>

<template>
  <div class="home-view">
    <section class="hero page-shell">
      <div class="hero-copy">
        <p class="section-kicker">Real-time Enterprise Auction</p>
        <h1>面向企业场景的在线拍卖系统</h1>
        <p>
          从拍品发布、实时竞价、一口价成交，到订单、钱包、信用与运营审核，
          用一个规范前端覆盖完整拍卖业务闭环。
        </p>
        <div class="hero-actions">
          <ElButton type="primary" size="large" @click="router.push('/items')">进入拍品大厅</ElButton>
          <ElButton size="large" @click="router.push('/user/publish')">发布拍品</ElButton>
        </div>
      </div>
      <div class="hero-panel surface" aria-label="平台核心能力">
        <div class="panel-row">
          <Gavel aria-hidden="true" />
          <span>英式拍卖与一口价并行</span>
        </div>
        <div class="panel-row">
          <ShieldCheck aria-hidden="true" />
          <span>角色权限、审核与风控</span>
        </div>
        <div class="panel-row">
          <WalletCards aria-hidden="true" />
          <span>钱包资金流水可审计</span>
        </div>
        <div class="panel-row">
          <BadgeCheck aria-hidden="true" />
          <span>信用分与交易评价闭环</span>
        </div>
      </div>
    </section>

    <section class="page-shell page-stack">
      <div class="grid-4">
        <MetricCard label="实时竞拍" value="秒级更新" hint="出价与成交动态即时刷新" />
        <MetricCard label="资金安全" value="多重保障" hint="交易、支付与调账过程清晰可追踪" tone="success" />
        <MetricCard label="角色覆盖" value="全角色协同" hint="游客、用户、管理员统一体验" tone="info" />
        <MetricCard label="体验标准" value="RWD" hint="桌面优先并适配移动端" tone="warning" />
      </div>

      <SectionHeader
        kicker="Categories"
        title="拍品分类"
        description="按品类快速浏览拍品，分类结构清晰，便于筛选与发现。"
      />
      <div class="category-grid">
        <button
          v-for="category in categories"
          :key="category.id"
          class="category-tile surface"
          type="button"
          @click="router.push({ path: '/items', query: { categoryId: category.id } })"
        >
          <strong>{{ category.name }}</strong>
          <span>{{ category.description || `${category.children?.length ?? 0} 个子类` }}</span>
        </button>
      </div>

      <SectionHeader
        kicker="Featured"
        title="正在进行的精选拍品"
        description="集中展示当前热度较高的拍品，查看价格、剩余时间与竞拍活跃度。"
      >
        <template #actions>
          <ElButton @click="router.push('/items')">查看全部</ElButton>
        </template>
      </SectionHeader>
      <div v-if="featured.length" class="auction-grid">
        <AuctionCard v-for="item in featured" :key="item.id" :item="item" />
      </div>
      <EmptyPanel v-else title="暂无上架拍品" description="稍后再来看看，更多精选拍品正在准备中。" />
    </section>
  </div>
</template>

<style scoped>
.home-view {
  padding: 32px 0 56px;
}

.hero {
  display: grid;
  grid-template-columns: minmax(0, 1.2fr) minmax(320px, 0.8fr);
  gap: 26px;
  align-items: center;
  min-height: 520px;
}

.hero-copy h1 {
  max-width: 760px;
  margin: 0;
  font-family: var(--font-display);
  font-size: clamp(42px, 7vw, 78px);
  line-height: 1.05;
}

.hero-copy p:not(.section-kicker) {
  max-width: 680px;
  margin: 20px 0 0;
  color: var(--color-muted);
  font-size: 18px;
  line-height: 1.8;
}

.hero-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  margin-top: 28px;
}

.hero-panel {
  display: grid;
  gap: 12px;
  padding: 22px;
  background:
    linear-gradient(160deg, rgba(18, 54, 72, 0.96), rgba(34, 62, 74, 0.92)),
    var(--color-accent);
  color: #fffaf1;
}

.panel-row {
  display: flex;
  min-height: 64px;
  align-items: center;
  gap: 12px;
  padding: 12px;
  border: 1px solid rgba(255, 253, 248, 0.14);
  border-radius: var(--radius-md);
  background: rgba(255, 253, 248, 0.08);
}

.panel-row svg {
  width: 24px;
  height: 24px;
  color: #f1c77a;
}

.category-grid,
.auction-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 18px;
}

.category-tile {
  display: grid;
  min-height: 112px;
  gap: 8px;
  padding: 18px;
  text-align: left;
  cursor: pointer;
}

.category-tile strong {
  font-size: 20px;
}

.category-tile span {
  color: var(--color-muted);
}

@media (max-width: 980px) {
  .hero,
  .category-grid,
  .auction-grid {
    grid-template-columns: 1fr;
  }
}
</style>
