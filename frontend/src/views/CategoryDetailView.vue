<script setup>
import { onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import {
  addSubscription,
  fetchArticles,
  fetchCategory,
  fetchReadingProgress,
  fetchSubscriptions,
  removeSubscription,
} from '../api'
import ArticleCard from '../components/ArticleCard.vue'
import { useAuthStore } from '../stores/auth'

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()
const category = ref(null)
const articles = ref([])
const lastReadId = ref(null)
const subscribed = ref(false)
const subBusy = ref(false)

async function load() {
  const c = await fetchCategory(route.params.slug)
  category.value = c.data
  if (!category.value) return
  document.title = `${category.value.name} - 技术实践笔记`
  const a = await fetchArticles({ page: 1, size: 50, categoryId: category.value.id })
  articles.value = a.data.records || []
  await loadProgress()
  await loadSubState()
}

async function loadProgress() {
  lastReadId.value = null
  if (!auth.isLoggedIn || !category.value?.id) return
  try {
    const res = await fetchReadingProgress({ categoryId: category.value.id })
    const data = res.data
    lastReadId.value = data?.articleId || data?.lastArticleId || null
  } catch {
    lastReadId.value = null
  }
}

async function loadSubState() {
  subscribed.value = false
  if (!auth.isLoggedIn || !category.value?.id) return
  try {
    const res = await fetchSubscriptions()
    const list = res.data?.records || res.data || []
    subscribed.value = list.some((x) => (x.categoryId || x.id) === category.value.id)
  } catch {
    subscribed.value = false
  }
}

async function toggleSubscribe() {
  if (!auth.isLoggedIn) {
    router.push({ path: '/login', query: { redirect: route.fullPath } })
    return
  }
  if (!category.value?.id || subBusy.value) return
  subBusy.value = true
  try {
    if (subscribed.value) {
      await removeSubscription(category.value.id)
      subscribed.value = false
    } else {
      await addSubscription(category.value.id)
      subscribed.value = true
    }
  } finally {
    subBusy.value = false
  }
}

onMounted(load)
watch(() => route.params.slug, load)
watch(() => auth.isLoggedIn, () => {
  loadProgress()
  loadSubState()
})
</script>

<template>
  <section class="section container" v-if="category">
    <div class="section-head">
      <div>
        <h2>{{ category.name }}</h2>
        <p class="muted">{{ category.description }}</p>
      </div>
      <button
        type="button"
        class="btn"
        :class="{ ghost: subscribed }"
        :disabled="subBusy"
        @click="toggleSubscribe"
      >
        {{ subscribed ? '已订阅' : '订阅专题' }}
      </button>
    </div>
    <div class="article-list">
      <div
        v-for="a in articles"
        :key="a.id"
        class="cat-article"
        :class="{ 'is-last-read': lastReadId === a.id }"
      >
        <span v-if="lastReadId === a.id" class="last-read-badge">上次读到</span>
        <ArticleCard
          :article="a"
          :link-query="{ categoryId: String(category.id) }"
        />
      </div>
      <p v-if="!articles.length" class="muted">该专题暂无文章。</p>
    </div>
  </section>
</template>

<style scoped>
.cat-article {
  position: relative;
}
.cat-article.is-last-read {
  background: var(--accent-soft);
  border-radius: 12px;
  padding: 0 0.75rem;
  margin: 0 -0.75rem;
}
.last-read-badge {
  display: inline-block;
  margin-top: 0.85rem;
  font-size: 0.78rem;
  color: var(--accent);
  font-weight: 600;
}
</style>
