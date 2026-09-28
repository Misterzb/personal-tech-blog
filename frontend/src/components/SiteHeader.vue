<script setup>
import { onMounted, ref } from 'vue'
import { fetchSite } from '../api'

const siteName = ref('技术实践笔记')

onMounted(async () => {
  try {
    const res = await fetchSite()
    if (res.data?.siteName) siteName.value = res.data.siteName
  } catch (_) {}
})
</script>

<template>
  <header class="site-header">
    <div class="container nav">
      <RouterLink class="brand" to="/">{{ siteName }}</RouterLink>
      <nav class="nav-links">
        <RouterLink to="/">首页</RouterLink>
        <RouterLink to="/articles">文章</RouterLink>
        <RouterLink to="/categories">专题</RouterLink>
        <RouterLink to="/projects">开源</RouterLink>
        <RouterLink to="/about">关于</RouterLink>
        <RouterLink to="/search">搜索</RouterLink>
      </nav>
    </div>
  </header>
</template>
