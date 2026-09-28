<script setup>
import { onMounted, ref } from 'vue'
import { fetchProjects } from '../api'

const projects = ref([])

function githubOf(p) {
  if (p.githubUrl) return p.githubUrl
  if (p.repoUrl && String(p.repoUrl).includes('github.com')) return p.repoUrl
  return ''
}
function giteeOf(p) {
  if (p.giteeUrl) return p.giteeUrl
  if (p.repoUrl && String(p.repoUrl).includes('gitee.com')) return p.repoUrl
  return ''
}

onMounted(async () => {
  const res = await fetchProjects()
  projects.value = res.data || []
  document.title = '开源项目 - 技术实践笔记'
})
</script>

<template>
  <section class="section container">
    <div class="section-head">
      <div>
        <h2>开源项目</h2>
        <p class="muted">GitHub / Gitee 双仓库与演示入口集中展示。</p>
      </div>
    </div>
    <div class="grid-2">
      <div v-for="p in projects" :key="p.id" class="panel">
        <h3>{{ p.name }}</h3>
        <p class="muted">{{ p.summary }}</p>
        <div class="meta" style="margin-bottom: 0.8rem">
          <span class="chip" v-for="t in (p.techStack || '').split(',').filter(Boolean)" :key="t">{{ t.trim() }}</span>
        </div>
        <div style="display: flex; gap: 0.6rem; flex-wrap: wrap">
          <a v-if="githubOf(p)" class="btn" :href="githubOf(p)" target="_blank" rel="noopener">GitHub</a>
          <a v-if="giteeOf(p)" class="btn ghost" :href="giteeOf(p)" target="_blank" rel="noopener">Gitee</a>
          <a v-if="p.demoUrl" class="btn ghost" :href="p.demoUrl" target="_blank" rel="noopener">在线演示</a>
        </div>
      </div>
    </div>
  </section>
</template>
