<template>
  <div class="page-shell system-view">
    <h1>403</h1>
    <p>当前账号没有访问该页面的权限。</p>
    <ElButton type="primary" @click="$router.push('/')">返回首页</ElButton>
  </div>
</template>

<style scoped>
.system-view {
  display: grid;
  min-height: calc(100dvh - 82px);
  place-items: center;
  align-content: center;
  gap: 12px;
  text-align: center;
}

h1 {
  margin: 0;
  font-family: var(--font-display);
  font-size: 88px;
}

p {
  margin: 0;
  color: var(--color-muted);
}
</style>
