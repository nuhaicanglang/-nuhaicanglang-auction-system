<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import SectionHeader from '@/components/common/SectionHeader.vue'
import { useAuthStore } from '@/stores/auth'

const router = useRouter()
const auth = useAuthStore()
const formRef = ref<FormInstance>()
const loading = ref(false)

const form = reactive({
  username: '',
  password: '',
  nickname: '',
  email: '',
  phone: '',
})

const rules: FormRules = {
  username: [{ required: true, min: 3, message: '账号至少 3 个字符', trigger: 'blur' }],
  password: [{ required: true, min: 6, message: '密码至少 6 个字符', trigger: 'blur' }],
  email: [{ type: 'email', message: '请输入有效邮箱', trigger: 'blur' }],
}

async function submit() {
  await formRef.value?.validate()
  loading.value = true
  try {
    await auth.register(form)
    ElMessage.success('注册成功，请登录')
    router.push('/login')
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <div class="auth-view page-shell">
    <section class="auth-copy">
      <SectionHeader
        kicker="Create Account"
        title="创建竞买人账号"
        description="同一账户可作为买家参与竞拍，也可作为卖家发布拍品，符合平台角色态设计。"
      />
    </section>
    <ElCard class="auth-card">
      <ElForm ref="formRef" :model="form" :rules="rules" label-position="top">
        <ElFormItem label="账号" prop="username"><ElInput v-model="form.username" autocomplete="username" /></ElFormItem>
        <ElFormItem label="昵称"><ElInput v-model="form.nickname" /></ElFormItem>
        <ElFormItem label="邮箱" prop="email"><ElInput v-model="form.email" autocomplete="email" /></ElFormItem>
        <ElFormItem label="手机号"><ElInput v-model="form.phone" autocomplete="tel" /></ElFormItem>
        <ElFormItem label="密码" prop="password">
          <ElInput v-model="form.password" type="password" autocomplete="new-password" show-password />
        </ElFormItem>
        <ElButton type="primary" class="submit" :loading="loading" @click="submit">注册</ElButton>
        <p class="switch">已有账号？<RouterLink to="/login">去登录</RouterLink></p>
      </ElForm>
    </ElCard>
  </div>
</template>

<style scoped>
.auth-view {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 460px;
  gap: 28px;
  align-items: center;
  min-height: calc(100dvh - 82px);
  padding: 40px 0;
}

.auth-card {
  box-shadow: var(--shadow-md);
}

.submit {
  width: 100%;
}

.switch {
  margin: 16px 0 0;
  text-align: center;
  color: var(--color-muted);
}

.switch a {
  color: var(--color-primary-dark);
  font-weight: 700;
}

@media (max-width: 900px) {
  .auth-view {
    grid-template-columns: 1fr;
  }
}
</style>
