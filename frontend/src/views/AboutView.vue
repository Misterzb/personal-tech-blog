<script setup>
import { onMounted, ref } from 'vue'
import { fetchSite } from '../api'

const site = ref({})

onMounted(async () => {
  const res = await fetchSite()
  site.value = res.data || {}
  document.title = '关于 - 技术实践笔记'
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
  </section>
</template>
