<script setup>
import { computed, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAuthStore } from '../stores/auth'
import CaptchaField from '../components/CaptchaField.vue'

const props = defineProps({
  mode: { type: String, default: 'login' },
})

const auth = useAuthStore()
const router = useRouter()
const route = useRoute()
const tab = ref(props.mode === 'register' ? 'register' : 'login')
const loading = ref(false)
const error = ref('')
const captchaRef = ref(null)

const form = reactive({
  phone: '',
  password: '',
  nickname: '',
  email: '',
  captchaId: '',
  captchaCode: '',
})

const title = computed(() => (tab.value === 'login' ? '登录' : '注册'))

async function submit() {
  error.value = ''
  loading.value = true
  try {
    const captcha = { captchaId: form.captchaId, captchaCode: form.captchaCode }
    if (tab.value === 'login') {
      await auth.login({ phone: form.phone, password: form.password, ...captcha })
    } else {
      await auth.register({
        phone: form.phone,
        password: form.password,
        nickname: form.nickname,
        email: form.email || undefined,
        ...captcha,
      })
    }
    const redirect = typeof route.query.redirect === 'string' ? route.query.redirect : '/'
    router.replace(redirect || '/')
  } catch (e) {
    error.value = e.message || '操作失败'
    captchaRef.value?.refresh?.()
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <section class="section container auth-page">
    <div class="auth-card panel">
      <div class="auth-tabs">
        <button type="button" :class="{ active: tab === 'login' }" @click="tab = 'login'">登录</button>
        <button type="button" :class="{ active: tab === 'register' }" @click="tab = 'register'">注册</button>
      </div>
      <h1>{{ title }}</h1>
      <p class="muted">使用手机号即可，评论需登录。默认生成可爱科技风头像。</p>
      <form class="auth-form" @submit.prevent="submit">
        <label>
          手机号
          <input v-model="form.phone" required pattern="1[0-9]{10}" maxlength="11" placeholder="11 位手机号" />
        </label>
        <label v-if="tab === 'register'">
          昵称
          <input v-model="form.nickname" required maxlength="32" placeholder="评论时展示的名字" />
        </label>
        <label>
          密码
          <input v-model="form.password" type="password" required minlength="6" placeholder="至少 6 位" />
        </label>
        <label v-if="tab === 'register'">
          邮箱（选填）
          <input v-model="form.email" type="email" maxlength="128" placeholder="可选" />
        </label>
        <label>
          验证码
          <CaptchaField ref="captchaRef" v-model="form" />
        </label>
        <button class="btn" type="submit" :disabled="loading">{{ loading ? '提交中…' : title }}</button>
        <p v-if="error" class="auth-error">{{ error }}</p>
      </form>
    </div>
  </section>
</template>

<style scoped>
.auth-page {
  max-width: 480px;
  margin: 0 auto;
}
.auth-card {
  padding: 1.5rem 1.4rem 1.6rem;
}
.auth-tabs {
  display: flex;
  gap: 0.5rem;
  margin-bottom: 1rem;
}
.auth-tabs button {
  border: 1px solid var(--line);
  background: transparent;
  color: var(--muted);
  border-radius: 999px;
  padding: 0.35rem 0.9rem;
  cursor: pointer;
  font-family: inherit;
}
.auth-tabs button.active {
  background: var(--accent-soft);
  color: var(--accent);
  border-color: var(--accent);
}
.auth-form {
  display: grid;
  gap: 0.85rem;
}
.auth-form label {
  display: grid;
  gap: 0.35rem;
  font-size: 0.9rem;
}
.auth-form input {
  border: 1px solid var(--line);
  border-radius: 10px;
  padding: 0.65rem 0.75rem;
  font: inherit;
  background: var(--card);
  color: var(--ink);
}
.auth-error {
  color: #b42318;
  margin: 0;
}
</style>
