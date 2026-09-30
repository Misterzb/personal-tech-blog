<script setup>
import { onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { fetchCaptcha, login } from '../api'

const router = useRouter()
const loading = ref(false)
const captchaImage = ref('')
const captchaLoading = ref(false)
const form = reactive({
  username: 'admin',
  password: 'admin123',
  captchaId: '',
  captchaCode: '',
})

async function refreshCaptcha() {
  captchaLoading.value = true
  try {
    const res = await fetchCaptcha()
    const data = res.data || {}
    form.captchaId = data.captchaId || ''
    form.captchaCode = ''
    captchaImage.value = data.imageBase64 || ''
  } catch {
    captchaImage.value = ''
  } finally {
    captchaLoading.value = false
  }
}

async function submit() {
  loading.value = true
  try {
    const res = await login(form)
    localStorage.setItem('blog_token', res.data.token)
    localStorage.setItem('blog_nickname', res.data.nickname || res.data.username)
    if (res.data.mustChangePassword) {
      localStorage.setItem('blog_must_change_password', '1')
    } else {
      localStorage.removeItem('blog_must_change_password')
    }
    ElMessage.success('登录成功')
    router.push('/dashboard')
  } catch (e) {
    ElMessage.error(e.message || '登录失败')
    await refreshCaptcha()
  } finally {
    loading.value = false
  }
}

onMounted(refreshCaptcha)
</script>

<template>
  <div class="login-wrap">
    <el-card class="login-card">
      <h2>博客管理后台</h2>
      <p class="tip">默认账号 admin / admin123，上线后请立即修改密码。</p>
      <el-form :model="form" @submit.prevent="submit" label-position="top">
        <el-form-item label="用户名">
          <el-input v-model="form.username" />
        </el-form-item>
        <el-form-item label="密码">
          <el-input v-model="form.password" type="password" show-password />
        </el-form-item>
        <el-form-item label="验证码">
          <div class="captcha-row">
            <el-input v-model="form.captchaCode" maxlength="8" placeholder="验证码" />
            <button type="button" class="captcha-btn" :disabled="captchaLoading" @click="refreshCaptcha">
              <img v-if="captchaImage" :src="captchaImage" alt="验证码" />
              <span v-else>{{ captchaLoading ? '加载中' : '刷新' }}</span>
            </button>
          </div>
        </el-form-item>
        <el-button type="primary" style="width: 100%" :loading="loading" @click="submit">登录</el-button>
      </el-form>
    </el-card>
  </div>
</template>

<style scoped>
.login-wrap {
  min-height: 100vh;
  display: grid;
  place-items: center;
  background: linear-gradient(145deg, #eef5f1, #f7f1e6);
}
.login-card {
  width: min(420px, calc(100% - 2rem));
}
.tip {
  color: #667;
  font-size: 13px;
  margin-top: -0.5rem;
}
.captcha-row {
  display: flex;
  gap: 0.6rem;
  width: 100%;
}
.captcha-btn {
  width: 120px;
  height: 32px;
  padding: 0;
  border: 1px solid var(--el-border-color);
  border-radius: 4px;
  background: #fff;
  cursor: pointer;
  overflow: hidden;
  flex-shrink: 0;
}
.captcha-btn img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
}
</style>
