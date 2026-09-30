<script setup>
import { onMounted, onUnmounted, ref } from 'vue'
import { fetchSite, fetchUnreadCount } from '../api'
import { useAuthStore } from '../stores/auth'
import { useThemeStore } from '../stores/theme'

const siteName = ref('技术实践笔记')
const auth = useAuthStore()
const theme = useThemeStore()
const unread = ref(0)
let timer = null

async function refreshUnread() {
  if (!auth.isLoggedIn) {
    unread.value = 0
    return
  }
  try {
    const res = await fetchUnreadCount()
    unread.value = res.data?.count ?? res.data ?? 0
  } catch {
    unread.value = 0
  }
}

onMounted(async () => {
  try {
    const res = await fetchSite()
    if (res.data?.siteName) siteName.value = res.data.siteName
  } catch (_) {}
  if (auth.token) {
    auth.refreshMe().catch(() => {})
    refreshUnread()
    timer = setInterval(refreshUnread, 60000)
  }
})

onUnmounted(() => {
  if (timer) clearInterval(timer)
})

const fontLabel = { sm: '小', md: '中', lg: '大' }
</script>

<template>
  <header class="site-header">
    <div class="container nav">
      <RouterLink class="brand" to="/">
        <img class="brand-mark" src="/favicon.svg" width="28" height="28" alt="" />
        <span>{{ siteName }}</span>
      </RouterLink>
      <nav class="nav-links">
        <RouterLink to="/">首页</RouterLink>
        <RouterLink to="/articles">文章</RouterLink>
        <RouterLink to="/categories">专题</RouterLink>
        <RouterLink to="/projects">开源</RouterLink>
        <RouterLink to="/about">关于</RouterLink>
        <RouterLink to="/search">搜索</RouterLink>
        <div class="nav-prefs" aria-label="显示偏好">
          <button
            type="button"
            class="nav-pref-btn"
            :title="theme.isDark ? '切换浅色' : '切换深色'"
            @click="theme.toggleTheme()"
          >
            {{ theme.isDark ? '浅色' : '深色' }}
          </button>
          <button
            type="button"
            class="nav-pref-btn"
            title="调整字号"
            @click="theme.cycleFontSize()"
          >
            字号{{ fontLabel[theme.fontSize] }}
          </button>
        </div>
        <template v-if="auth.isLoggedIn">
          <RouterLink class="nav-user" to="/me">
            <img v-if="auth.member?.avatar" class="nav-avatar" :src="auth.member.avatar" alt="" />
            <span>{{ auth.member?.nickname }}</span>
            <span v-if="unread" class="nav-badge">{{ unread > 99 ? '99+' : unread }}</span>
          </RouterLink>
          <button type="button" class="nav-logout" @click="auth.logout()">退出</button>
        </template>
        <RouterLink v-else to="/login">登录</RouterLink>
      </nav>
    </div>
  </header>
</template>
