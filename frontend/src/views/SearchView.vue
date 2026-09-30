<script setup>
import { ref } from 'vue'
import { searchArticles } from '../api'
import ArticleCard from '../components/ArticleCard.vue'

const q = ref('')
const articles = ref([])
const searched = ref(false)

async function search() {
  if (!q.value.trim()) return
  const res = await searchArticles({ kw: q.value.trim(), page: 1, size: 20 })
  articles.value = res.data.records || []
  searched.value = true
  document.title = `搜索：${q.value} - 技术实践笔记`
}
</script>

<template>
  <section class="section container">
    <div class="section-head">
      <div>
        <h2>搜索</h2>
        <p class="muted">按标题、摘要、正文与标签关键词检索。</p>
      </div>
    </div>
    <form class="search-box" @submit.prevent="search">
      <input v-model="q" placeholder="例如：Agent、Spring Boot、小程序" />
      <button class="btn" type="submit">搜索</button>
    </form>
    <div class="article-list">
      <ArticleCard v-for="a in articles" :key="a.id" :article="a" />
      <p v-if="searched && !articles.length" class="muted">没有找到相关文章。</p>
    </div>
  </section>
</template>
