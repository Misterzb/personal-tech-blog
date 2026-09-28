<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { fetchArticles, fetchTags } from '../api'
import ArticleCard from '../components/ArticleCard.vue'

const route = useRoute()
const router = useRouter()
const page = ref(1)
const total = ref(0)
const articles = ref([])
const tags = ref([])
const tagId = ref(route.query.tagId ? Number(route.query.tagId) : null)

const linkQuery = computed(() => (tagId.value ? { tagId: String(tagId.value) } : {}))

async function load() {
  const res = await fetchArticles({ page: page.value, size: 10, tagId: tagId.value || undefined })
  articles.value = res.data.records || []
  total.value = res.data.total || 0
  document.title = '文章 - 技术实践笔记'
}

function selectTag(id) {
  tagId.value = id
  page.value = 1
  router.replace({ query: id ? { tagId: String(id) } : {} })
  load()
}

onMounted(async () => {
  const t = await fetchTags()
  tags.value = t.data || []
  await load()
})

watch(
  () => route.query.tagId,
  (v) => {
    const next = v ? Number(v) : null
    if (next !== tagId.value) {
      tagId.value = next
      page.value = 1
      load()
    }
  },
)
</script>

<template>
  <section class="section container">
    <div class="section-head">
      <div>
        <h2>全部文章</h2>
        <p class="muted">支持按标签筛选，也能去搜索页关键词查找。</p>
      </div>
    </div>
    <div class="meta" style="margin-bottom: 1rem">
      <button class="chip" :style="{ opacity: tagId ? 0.6 : 1 }" @click="selectTag(null)">全部</button>
      <button
        v-for="t in tags"
        :key="t.id"
        class="chip"
        :style="{ opacity: tagId === t.id ? 1 : 0.65 }"
        @click="selectTag(t.id)"
      >
        {{ t.name }}
      </button>
    </div>
    <div class="article-list">
      <ArticleCard v-for="a in articles" :key="a.id" :article="a" :link-query="linkQuery" />
    </div>
    <div class="pager" v-if="total > 10">
      <button class="btn ghost" :disabled="page <= 1" @click="page--; load()">上一页</button>
      <button class="btn ghost" :disabled="page * 10 >= total" @click="page++; load()">下一页</button>
    </div>
  </section>
</template>
