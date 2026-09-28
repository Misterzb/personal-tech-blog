<script setup>
import { onMounted, ref } from 'vue'
import { fetchSite } from '../api'

const site = ref({})

onMounted(async () => {
  try {
    const res = await fetchSite()
    site.value = res.data || {}
  } catch (_) {}
})
</script>

<template>
  <footer class="site-footer">
    <div class="container">
      <div>{{ site.siteName || '技术实践笔记' }} · {{ site.siteSubtitle || 'Java · Agent · 全栈落地' }}</div>
      <div style="margin-top: 0.4rem">
        <a href="/rss.xml" target="_blank" rel="noopener">RSS</a>
        <span v-if="site.icp"> · {{ site.icp }}</span>
      </div>
    </div>
  </footer>
</template>
