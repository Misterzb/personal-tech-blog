<script setup>
import { useRoute, useRouter } from 'vue-router'

const route = useRoute()
const router = useRouter()
const nickname = localStorage.getItem('blog_nickname') || '管理员'

function logout() {
  localStorage.removeItem('blog_token')
  localStorage.removeItem('blog_nickname')
  router.push('/login')
}
</script>

<template>
  <el-container class="layout">
    <el-aside width="220px" class="aside">
      <div class="logo">技术实践笔记</div>
      <el-menu :default-active="route.path" router>
        <el-menu-item index="/dashboard">仪表盘</el-menu-item>
        <el-menu-item index="/articles">文章管理</el-menu-item>
        <el-menu-item index="/categories">专题管理</el-menu-item>
        <el-menu-item index="/tags">标签管理</el-menu-item>
        <el-menu-item index="/projects">开源项目</el-menu-item>
        <el-menu-item index="/comments">评论审核</el-menu-item>
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
  </el-container>
</template>

<style scoped>
.layout { min-height: 100vh; }
.aside { background: #fff; border-right: 1px solid #eef0f2; }
.logo {
  height: 60px;
  display: flex;
  align-items: center;
  padding: 0 20px;
  font-weight: 700;
  color: #0f6a56;
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
