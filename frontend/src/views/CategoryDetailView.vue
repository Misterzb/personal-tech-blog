<script setup>
import { onMounted, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import { fetchArticles, fetchCategory } from '../api'
import ArticleCard from '../components/ArticleCard.vue'

const route = useRoute()
const category = ref(null)
const articles = ref([])

async function load() {
  const c = await fetchCategory(route.params.slug)
  category.value = c.data
  if (!category.value) return
  document.title = `${category.value.name} - 技术实践笔记`
  const a = await fetchArticles({ page: 1, size: 50, categoryId: category.value.id })
  articles.value = a.data.records || []
}

onMounted(load)
watch(() => route.params.slug, load)
</script>

<template>
  <section class="section container" v-if="category">
    <div class="section-head">
      <div>
        <h2>{{ category.name }}</h2>
        <p class="muted">{{ category.description }}</p>
      </div>
    </div>
    <div class="article-list">
      <ArticleCard
        v-for="a in articles"
        :key="a.id"
        :article="a"
        :link-query="{ categoryId: String(category.id) }"
      />
      <p v-if="!articles.length" class="muted">该专题暂无文章。</p>
    </div>
  </section>
</template>
