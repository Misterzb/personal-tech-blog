<script setup>
import { onMounted, ref } from 'vue'
import { fetchFriendLinks, fetchSite } from '../api'

const site = ref({})
const links = ref([])

onMounted(async () => {
  try {
    const res = await fetchSite()
    site.value = res.data || {}
  } catch (_) {}
  try {
    const res = await fetchFriendLinks()
    links.value = res.data?.records || res.data || []
  } catch (_) {}
})
</script>

<template>
  <footer class="site-footer">
    <div class="container">
      <div>{{ site.siteName || '技术实践笔记' }} · {{ site.siteSubtitle || 'Java · Agent · 全栈落地' }}</div>
      <div v-if="links.length" class="friend-links">
        <span class="friend-label">友情链接</span>
        <a
          v-for="l in links"
          :key="l.id || l.url"
          :href="l.url"
          target="_blank"
          rel="noopener noreferrer"
        >
          {{ l.name || l.title }}
        </a>
      </div>
      <div style="margin-top: 0.4rem">
        <a href="/rss.xml" target="_blank" rel="noopener">RSS</a>
        <span v-if="site.icp"> · {{ site.icp }}</span>
      </div>
    </div>
  </footer>
</template>
