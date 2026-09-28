<script setup>
import { onMounted, ref } from 'vue'
import { fetchHome } from '../api'
import ArticleCard from '../components/ArticleCard.vue'

const loading = ref(true)
const data = ref({ site: {}, articles: [], categories: [], projects: [] })

onMounted(async () => {
  try {
    const res = await fetchHome()
    data.value = res.data || data.value
    document.title = `${data.value.site?.siteName || '技术实践笔记'} - ${data.value.site?.siteSubtitle || ''}`
  } finally {
    loading.value = false
  }
})
</script>

<template>
  <div>
    <section class="hero container">
      <h1 class="hero-brand">{{ data.site.siteName || '技术实践笔记' }}</h1>
      <p>{{ data.site.siteSubtitle || '记录 Java、Agent 落地与全栈实践，把能复用的经验写清楚。' }}</p>
      <div style="display: flex; gap: 0.75rem; flex-wrap: wrap">
        <RouterLink class="btn" to="/articles">阅读文章</RouterLink>
        <RouterLink class="btn ghost" to="/projects">查看开源</RouterLink>
      </div>
    </section>

    <section class="section container" v-if="!loading">
      <div class="section-head">
        <div>
          <h2>最新文章</h2>
          <p class="muted">从工程实践出发，少空话，多可复用步骤。</p>
        </div>
        <RouterLink to="/articles">全部文章 →</RouterLink>
      </div>
      <div class="article-list">
        <ArticleCard v-for="a in data.articles" :key="a.id" :article="a" />
      </div>
    </section>

    <section class="section container">
      <div class="section-head">
        <div>
          <h2>专项专题</h2>
          <p class="muted">按方向聚合，方便系统阅读。</p>
        </div>
        <RouterLink to="/categories">全部专题 →</RouterLink>
      </div>
      <div class="grid-3">
        <RouterLink
          v-for="c in data.categories"
          :key="c.id"
          class="panel"
          :to="`/categories/${c.slug}`"
        >
          <h3>{{ c.name }}</h3>
          <p class="muted" style="margin: 0">{{ c.description }}</p>
        </RouterLink>
      </div>
    </section>

    <section class="section container">
      <div class="section-head">
        <div>
          <h2>开源项目</h2>
          <p class="muted">仓库地址与技术栈一目了然。</p>
        </div>
        <RouterLink to="/projects">全部项目 →</RouterLink>
      </div>
      <div class="grid-2">
        <div v-for="p in data.projects" :key="p.id" class="panel">
          <h3>{{ p.name }}</h3>
          <p class="muted">{{ p.summary }}</p>
          <div class="meta" style="margin-bottom: 0.8rem">
            <span class="chip" v-for="t in (p.techStack || '').split(',').filter(Boolean)" :key="t">{{ t.trim() }}</span>
          </div>
          <div style="display: flex; gap: 0.6rem; flex-wrap: wrap">
            <a
              v-if="p.githubUrl || (p.repoUrl && String(p.repoUrl).includes('github.com'))"
              class="btn"
              :href="p.githubUrl || p.repoUrl"
              target="_blank"
              rel="noopener"
            >GitHub</a>
            <a
              v-if="p.giteeUrl || (p.repoUrl && String(p.repoUrl).includes('gitee.com'))"
              class="btn ghost"
              :href="p.giteeUrl || p.repoUrl"
              target="_blank"
              rel="noopener"
            >Gitee</a>
          </div>
        </div>
      </div>
    </section>
  </div>
</template>
