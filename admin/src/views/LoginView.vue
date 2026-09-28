<script setup>
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { login } from '../api'

const router = useRouter()
const loading = ref(false)
const form = reactive({ username: 'admin', password: 'admin123' })

async function submit() {
  loading.value = true
  try {
    const res = await login(form)
    localStorage.setItem('blog_token', res.data.token)
    localStorage.setItem('blog_nickname', res.data.nickname || res.data.username)
    ElMessage.success('登录成功')
    router.push('/dashboard')
  } finally {
    loading.value = false
  }
}
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
</style>
