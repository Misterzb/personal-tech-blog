<script setup>
import { onMounted, ref } from 'vue'
import { fetchFriendLinks, fetchSite } from '../api'

const site = ref({})
const friendLinks = ref([])

onMounted(async () => {
  const res = await fetchSite()
  site.value = res.data || {}
  document.title = '关于 - 技术实践笔记'
  try {
    const fl = await fetchFriendLinks()
    friendLinks.value = fl.data?.records || fl.data || []
  } catch (_) {}
})

function socialLinks() {
  try {
    return JSON.parse(site.value.socialLinks || '[]')
  } catch {
    return []
  }
}
</script>

<template>
  <section class="section container">
    <div class="section-head">
      <div>
        <h2>关于</h2>
        <p class="muted">个人背景、技能方向与联系方式。</p>
      </div>
    </div>
    <article class="article-body panel" v-html="site.aboutHtml"></article>
    <div class="meta" style="margin-top: 1.25rem">
      <a
        v-for="s in socialLinks()"
        :key="s.name"
        class="chip"
        :href="s.url"
        target="_blank"
        rel="noopener"
      >
        {{ s.name }}
      </a>
    </div>
    <div v-if="friendLinks.length" class="section" style="padding-bottom: 0">
      <div class="section-head">
        <div>
          <h2>友情链接</h2>
          <p class="muted">志同道合的站点。</p>
        </div>
      </div>
      <div class="friend-links about-friends">
        <a
          v-for="l in friendLinks"
          :key="l.id || l.url"
          class="chip"
          :href="l.url"
          target="_blank"
          rel="noopener noreferrer"
        >
          {{ l.name || l.title }}
        </a>
      </div>
    </div>
  </section>
</template>
