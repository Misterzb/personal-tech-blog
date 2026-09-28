<script setup>
defineProps({
  article: { type: Object, required: true },
  /** 带入详情页的筛选上下文，如 { tagId } / { categoryId } */
  linkQuery: { type: Object, default: () => ({}) },
})

function formatDate(v) {
  if (!v) return ''
  return String(v).replace('T', ' ').slice(0, 16)
}
</script>

<template>
  <RouterLink
    class="article-item"
    :to="{ name: 'article', params: { slug: article.slug }, query: linkQuery }"
  >
    <div class="meta">
      <span v-if="article.categoryName" class="chip">{{ article.categoryName }}</span>
      <span>{{ formatDate(article.publishedAt) }}</span>
      <span>{{ article.viewCount || 0 }} 阅读</span>
    </div>
    <h3>{{ article.title }}</h3>
    <p class="muted" style="margin: 0">{{ article.summary }}</p>
  </RouterLink>
</template>
