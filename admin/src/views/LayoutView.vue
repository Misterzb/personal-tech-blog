<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { changePassword } from '../api'

const route = useRoute()
const router = useRouter()
const nickname = localStorage.getItem('blog_nickname') || '管理员'
const mustChange = computed(() => localStorage.getItem('blog_must_change_password') === '1')
const pwdDialog = ref(false)
const pwdLoading = ref(false)
const pwd = reactive({ oldPassword: '', newPassword: '', confirm: '' })

onMounted(() => {
  if (mustChange.value) pwdDialog.value = true
})

async function submitPwd() {
  if (!pwd.oldPassword || !pwd.newPassword) {
    ElMessage.warning('请填写原密码与新密码')
    return
  }
  if (pwd.newPassword !== pwd.confirm) {
    ElMessage.warning('两次新密码不一致')
    return
  }
  pwdLoading.value = true
  try {
    await changePassword({ oldPassword: pwd.oldPassword, newPassword: pwd.newPassword })
    localStorage.removeItem('blog_must_change_password')
    pwdDialog.value = false
    pwd.oldPassword = ''
    pwd.newPassword = ''
    pwd.confirm = ''
    ElMessage.success('密码已修改，可以继续使用后台')
  } finally {
    pwdLoading.value = false
  }
}

function logout() {
  localStorage.removeItem('blog_token')
  localStorage.removeItem('blog_nickname')
  localStorage.removeItem('blog_must_change_password')
  router.push('/login')
}
</script>

<template>
  <el-container class="layout">
    <el-aside width="220px" class="aside">
      <div class="logo">
        <img src="/favicon.svg" width="28" height="28" alt="" />
        <span>技术实践笔记</span>
      </div>
      <el-menu :default-active="route.path" router>
        <el-menu-item index="/dashboard">仪表盘</el-menu-item>
        <el-menu-item index="/articles">文章管理</el-menu-item>
        <el-menu-item index="/categories">专题管理</el-menu-item>
        <el-menu-item index="/tags">标签管理</el-menu-item>
        <el-menu-item index="/projects">开源项目</el-menu-item>
        <el-menu-item index="/comments">评论审核</el-menu-item>
        <el-menu-item index="/members">会员管理</el-menu-item>
        <el-menu-item index="/announcements">公告管理</el-menu-item>
        <el-menu-item index="/friend-links">友情链接</el-menu-item>
        <el-menu-item index="/site">站点配置</el-menu-item>
      </el-menu>
    </el-aside>
    <el-container>
      <el-header class="header">
        <span>{{ nickname }}</span>
        <el-button link type="danger" @click="logout">退出</el-button>
      </el-header>
      <el-main>
        <RouterView />
      </el-main>
    </el-container>

    <el-dialog
      v-model="pwdDialog"
      title="请先修改密码"
      width="420px"
      :close-on-click-modal="false"
      :close-on-press-escape="false"
      :show-close="false"
    >
      <p style="margin-top:0;color:#888;font-size:13px">检测到必须修改密码后才能继续使用管理后台。</p>
      <el-form label-width="90px">
        <el-form-item label="原密码">
          <el-input v-model="pwd.oldPassword" type="password" show-password />
        </el-form-item>
        <el-form-item label="新密码">
          <el-input v-model="pwd.newPassword" type="password" show-password />
        </el-form-item>
        <el-form-item label="确认密码">
          <el-input v-model="pwd.confirm" type="password" show-password />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button type="primary" :loading="pwdLoading" @click="submitPwd">确认修改</el-button>
      </template>
    </el-dialog>
  </el-container>
</template>

<style scoped>
.layout { min-height: 100vh; }
.aside { background: #fff; border-right: 1px solid #eef0f2; }
.logo {
  height: 60px;
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 0 20px;
  font-weight: 700;
  color: #0f6a56;
}
.logo img {
  border-radius: 7px;
  flex-shrink: 0;
}
.header {
  display: flex;
  justify-content: flex-end;
  align-items: center;
  gap: 12px;
  background: #fff;
  border-bottom: 1px solid #eef0f2;
}
</style>
