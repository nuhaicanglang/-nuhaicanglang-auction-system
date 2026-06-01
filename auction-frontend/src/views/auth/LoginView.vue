<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import SectionHeader from '@/components/common/SectionHeader.vue'
import { captchaApi } from '@/api/auth'
import { useAuthStore } from '@/stores/auth'

const router = useRouter()
const route = useRoute()
const auth = useAuthStore()
const formRef = ref<FormInstance>()
const loading = ref(false)
const captchaImage = ref('')

const form = reactive({
  username: '',
  password: '',
  captchaUuid: '',
  captchaCode: '',
})

const rules: FormRules = {
  username: [{ required: true, message: '请输入账号', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }],
}

async function refreshCaptcha() {
  const data = await captchaApi()
  form.captchaUuid = data.uuid || data.captchaUuid || ''
  captchaImage.value = data.image || data.img || data.base64 || ''
}

async function submit() {
  await formRef.value?.validate()
  loading.value = true
  try {
    await auth.login(form)
    ElMessage.success('登录成功')
    router.push(String(route.query.redirect || '/'))
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <div class="auth-view page-shell">
    <section class="auth-copy">
      <SectionHeader
        kicker="Sign in"
        title="登录云槌拍卖"
        description="登录后可发布拍品、参与竞价、查看订单、钱包流水和信用分。"
      />
    </section>
    <ElCard class="auth-card">
      <ElForm ref="formRef" :model="form" :rules="rules" label-position="top">
        <ElFormItem label="账号" prop="username">
          <ElInput v-model="form.username" autocomplete="username" />
        </ElFormItem>
        <ElFormItem label="密码" prop="password">
          <ElInput v-model="form.password" type="password" autocomplete="current-password" show-password />
        </ElFormItem>
        <ElFormItem label="图形验证码">
          <div class="captcha-row">
            <ElInput v-model="form.captchaCode" placeholder="需要时填写" />
            <ElButton @click="refreshCaptcha">获取验证码</ElButton>
          </div>
          <img v-if="captchaImage" class="captcha-image" :src="captchaImage" alt="图形验证码" />
        </ElFormItem>
        <ElButton type="primary" class="submit" :loading="loading" @click="submit">登录</ElButton>
        <p class="switch">还没有账号？<RouterLink to="/register">立即注册</RouterLink></p>
      </ElForm>
    </ElCard>
  </div>
</template>

<style scoped>
.auth-view {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 420px;
  gap: 28px;
  align-items: center;
  min-height: calc(100dvh - 82px);
  padding: 40px 0;
}

.auth-card {
  box-shadow: var(--shadow-md);
}

.captcha-row {
  display: grid;
  width: 100%;
  grid-template-columns: 1fr auto;
  gap: 10px;
}

.captcha-image {
  max-height: 80px;
  margin-top: 10px;
  border-radius: var(--radius-sm);
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
